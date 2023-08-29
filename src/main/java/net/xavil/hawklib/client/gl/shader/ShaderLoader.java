package net.xavil.hawklib.client.gl.shader;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;

import net.minecraft.ResourceLocationException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.xavil.hawklib.Disposable;
import net.xavil.hawklib.HawkLib;
import net.xavil.hawklib.client.gl.GlFragmentWrites;
import net.xavil.hawklib.collections.impl.Vector;
import net.xavil.hawklib.collections.interfaces.ImmutableList;
import net.xavil.hawklib.collections.interfaces.MutableList;
import net.xavil.hawklib.collections.interfaces.MutableSet;
import net.xavil.hawklib.collections.iterator.Iterator;

public final class ShaderLoader {

	public static final ResourceLocation STANDARD_LOCATION = new ResourceLocation("hawk", "standard.glsl");

	public static final class ShaderLoadException extends Exception {
		public final ResourceLocation shaderLocation;

		public ShaderLoadException(ResourceLocation shaderLocation) {
			this.shaderLocation = shaderLocation;
		}

		public ShaderLoadException(String message, ResourceLocation shaderLocation) {
			super(message);
			this.shaderLocation = shaderLocation;
		}

		public ShaderLoadException(Throwable cause, ResourceLocation shaderLocation) {
			super(cause);
			this.shaderLocation = shaderLocation;
		}
	}

	private static final class Parser {
		public final SharedContext shared;
		public final ResourceLocation location;
		public final String input;
		public int index = 0;

		@SuppressWarnings("unused")
		public int line = 1, col = 0;
		public boolean onlyWhitespaceOnLine = true;

		public Parser(SharedContext shared, ResourceLocation location, String input) {
			this.shared = shared;
			this.location = location;
			this.input = input;
		}

		public boolean hasMore() {
			return this.index < this.input.length();
		}

		public int peek() {
			return this.input.codePointAt(this.index);
		}

		private void applyCodepoint(int codepoint) {
			this.col += 1;
			if (codepoint == '\n') {
				this.line += 1;
				this.col = 0;
				this.onlyWhitespaceOnLine = true;
			} else if (!Character.isWhitespace(codepoint)) {
				this.onlyWhitespaceOnLine = false;
			}
		}

		public int advance(StringBuilder output) {
			final var codepoint = peek();
			this.index += Character.charCount(codepoint);
			if (output != null)
				output.appendCodePoint(codepoint);
			applyCodepoint(codepoint);
			return codepoint;
		}

		public boolean eat(int codepoint, StringBuilder output) {
			if (!hasMore())
				return false;
			final var actual = peek();
			if (codepoint == actual) {
				this.index += Character.charCount(actual);
				if (output != null)
					output.appendCodePoint(actual);
				applyCodepoint(actual);
				return true;
			}
			return false;
		}

		private void ensureNewLine(StringBuilder output) {
			if (!this.onlyWhitespaceOnLine)
				output.append('\n');
		}
	}

	// advances up to either the next non-whitespace codepoint, or the start of the
	// next line.
	private static void advanceWhitespace(Parser parser, StringBuilder output) {
		while (parser.hasMore()) {
			final var codepoint = parser.peek();
			if (!Character.isWhitespace(codepoint))
				return;
			parser.advance(output);
			if (codepoint == '\n')
				return;
		}
	}

	// assumes an initial `/` has already been consumed, and has not beed added to
	// the output.
	private static void advanceComment(Parser parser, StringBuilder output, boolean discardSingleSlash) {
		if (!parser.hasMore())
			return;
		if (parser.eat('/', null)) {
			output.append("//");
			while (parser.hasMore()) {
				if (parser.advance(output) == '\n')
					break;
			}
		} else if (parser.eat('*', null)) {
			output.append("/*");
			while (parser.hasMore()) {
				if (parser.eat('*', output) && parser.eat('/', output))
					break;
				parser.advance(output);
			}
		} else if (!discardSingleSlash) {
			output.append('/');
		}
	}

	private static ResourceLocation parseResourceLocation(Parser parser, String location) throws ShaderLoadException {
		try {
			return new ResourceLocation(location);
		} catch (ResourceLocationException ex) {
			return null;
		}
	}

	private static ShaderLoadException makeIncludeCycleException(SharedContext ctx, ResourceLocation cycleCause) {
		final var message = new StringBuilder(
				String.format("shader include cycle detected while loading shader root '%s':\n",
						ctx.rootLocation.toString()));

		final var provokingIndex = ctx.includeStack.iter().position(n -> n.include().equals(cycleCause));
		for (int i = 0; i < ctx.includeStack.size(); ++i) {
			final var info = ctx.includeStack.get(i);
			message.append("  ");
			if (i == provokingIndex)
				message.append("[FROM] ");
			if (i == ctx.includeStack.size() - 1)
				message.append("[CYCLE] ");

			message.append(info.source().toString());
			message.append(':');
			message.append(info.line());
			message.append(" => #include [");
			message.append(info.include().toString());
			message.append("]\n");
		}

		return new ShaderLoadException(message.toString(), ctx.rootLocation);
	}

	private static ShaderLoadException makeFileNotFoundError(SharedContext ctx, ResourceLocation missingLocation,
			FileNotFoundException cause) {
		if (ctx.includeStack.isEmpty()) {
			final var message = new StringBuilder(
					String.format("could not find shader root '%s'\n", ctx.rootLocation.toString()));
			final var ex = new ShaderLoadException(message.toString(), ctx.rootLocation);
			ex.initCause(cause);
			return ex;
		}

		final var message = new StringBuilder(
				String.format("could not find include '%s' while loading shader root '%s':\n",
						missingLocation.toString(), ctx.rootLocation.toString()));

		for (int i = 0; i < ctx.includeStack.size(); ++i) {
			final var info = ctx.includeStack.get(i);
			message.append("  ");
			message.append(info.source().toString());
			message.append(':');
			message.append(info.line());
			message.append(" => #include [");
			message.append(info.include().toString());
			message.append("]\n");
		}

		final var ex = new ShaderLoadException(message.toString(), ctx.rootLocation);
		ex.initCause(cause);
		return ex;
	}

	private static void appendErrorDirective(Parser parser, StringBuilder output) {
		parser.ensureNewLine(output);
		output.append(String.format("#line %d\n", parser.line));
		output.append(String.format("#error in shader '%s'", parser.location));
		if (!parser.location.equals(parser.shared.rootLocation))
			output.append(String.format(" (via shader root '%s')", parser.shared.rootLocation));
		output.append(": ");
	}

	private static void advanceStagesDirective(Parser parser, StringBuilder output) {
		final var startLine = parser.line;

		while (parser.hasMore()) {
			final var codepoint = parser.peek();
			if (codepoint == '/') {
				// NOTE: comments are forwarded to output instead of just being eaten
				advanceComment(parser, output, true);
			} else if (Character.isLetterOrDigit(codepoint)) {
				final var stageNameBuilder = new StringBuilder();
				while (parser.hasMore() && Character.isLetterOrDigit(parser.peek())) {
					parser.advance(stageNameBuilder);
				}
				switch (stageNameBuilder.toString()) {
					case "vertex" -> parser.shared.hasVertexStage |= true;
					case "tess_control" -> parser.shared.hasTessControlStage |= true;
					case "tess_eval" -> parser.shared.hasTessEvalStage |= true;
					case "geometry" -> parser.shared.hasGeometryStage |= true;
					case "fragment" -> parser.shared.hasFragmentStage |= true;
					case "compute" -> parser.shared.hasComputeStage |= true;
					default -> {
						appendErrorDirective(parser, output);
						output.append(String.format(
								"malformed stages directive: '%s' is not a valid stage\n",
								stageNameBuilder.toString()));
					}
				}
			} else {
				parser.advance(null);
			}

			// stop eating codepoints once the next line starts.
			if (startLine != parser.line)
				break;
		}

	}

	private static void advanceIncludeDirective(Parser parser, StringBuilder output) throws ShaderLoadException {
		final var startLine = parser.line;

		// find the starting [ delimiter
		while (parser.hasMore()) {
			final var codepoint = parser.advance(null);
			if (codepoint == '[') {
				break;
			} else if (codepoint == '/') {
				// NOTE: comments are forwarded to output instead of just being eaten
				advanceComment(parser, output, true);
			}
			if (startLine != parser.line) {
				appendErrorDirective(parser, output);
				output.append("malformed include directive: no resource location found\n");
				return;
			}
			advanceWhitespace(parser, output);
		}

		// find the closing ] delimiter, accumulating everything in between
		final var locationBuilder = new StringBuilder();
		while (parser.hasMore()) {
			final var codepoint = parser.advance(null);
			if (codepoint == ']')
				break;
			if (startLine != parser.line) {
				appendErrorDirective(parser, output);
				output.append("malformed include directive: no closing delimiter found\n");
				return;
			}
			locationBuilder.appendCodePoint(codepoint);
		}

		// advance to the end of the line, discarding any gunk we find along the way.
		// (except for comments)
		while (parser.hasMore()) {
			final var codepoint = parser.advance(null);
			if (codepoint == '\n') {
				break;
			} else if (codepoint == '/') {
				// NOTE: comments are forwarded to output instead of just being eaten
				advanceComment(parser, output, true);
			}
			// stop eating codepoints if a comment spills us over onto the next line
			if (startLine != parser.line)
				break;
		}

		final var locatingString = locationBuilder.toString().trim();
		final var location = parseResourceLocation(parser, locatingString);
		if (location == null) {
			appendErrorDirective(parser, output);
			output.append(String.format(
					"malformed include directive: '%s' is not a valid resource location\n",
					locatingString));
			return;
		}

		appendInclude(parser, output, location, startLine);
	}

	private static void appendInclude(Parser parser, StringBuilder output, ResourceLocation location, int line)
			throws ShaderLoadException {
		HawkLib.LOGGER.debug("applying include directive for shader '{}': #include [{}]",
				parser.location, location);
		if (!parser.shared.pushInclude(new IncludeInfo(parser.location, line, location))) {
			throw makeIncludeCycleException(parser.shared, location);
		}
		final var includedSource = doLoadSource(parser.shared, location);
		if (!parser.onlyWhitespaceOnLine)
			output.append('\n');
		// NOTE: if `location` contained a newline, it could escape the influence of the
		// comment, but it has been verified to be a valid ResourceLocation, so it can't
		// contain a newline.
		output.append(String.format("// ===>> BEGIN INCLUDE '%s' <<===\n", location.toString()));
		output.append("#line 1\n");
		output.append(includedSource);
		// we don't know whether the string builder is on a new line or not, se we have
		// to do something to make sure that it is!
		output.append('\n');
		output.append(String.format("// ===<< END INCLUDE '%s' >>===\n", location.toString()));
		output.append(String.format("#line %d\n", parser.line));
		parser.shared.popInclude();
	}

	// assumes the initial `#` has already been consumed
	private static void advanceDirective(Parser parser, StringBuilder output) throws ShaderLoadException {
		final var tempBuilder = new StringBuilder("#");
		final var startLine = parser.line;
		advanceWhitespace(parser, tempBuilder);

		// handles the case where there's only a single # on a line, possibly surrounded
		// with whitespace.
		if (startLine != parser.line) {
			output.append(tempBuilder);
			return;
		}

		// the cursor is now pointing at the first non-whitespace character after the
		// initial #. We push the longest sequence of alphanumeric characters we can
		// find to the directive name buffer.
		final var directiveName = new StringBuilder();
		while (parser.hasMore()) {
			final var codepoint = parser.peek();
			if (!Character.isLetterOrDigit(codepoint))
				break;
			parser.advance(directiveName);
		}

		// the cursor is now pointing to the character immediately after the directive
		// name we just parsed.
		switch (directiveName.toString()) {
			case "include" -> advanceIncludeDirective(parser, output);
			case "stages" -> advanceStagesDirective(parser, output);
			default -> {
				// we didn't handle this directive ourselves, so we just forward everything
				// we've found so far to the actual output. `advanceAll` will take care of
				// emitting the rest of the directive.
				output.append(tempBuilder);
				output.append(directiveName);
			}
		}
	}

	private static void advanceAll(Parser parser, StringBuilder output) throws ShaderLoadException {
		final var lastIncludeInfo = parser.shared.includeStack.last().unwrapOrNull();
		if (lastIncludeInfo != null) {
			output.append(String.format("// loaded from '%s':%d\n", lastIncludeInfo.source().toString(),
					lastIncludeInfo.line()));
		}
		if (parser.location.equals(parser.shared.rootLocation)) {
			output.append("// ===== user-provided defines =====\n");
			for (final var def : parser.shared.shaderDefines.iterable()) {
				output.append(String.format("#define %s\n", def));
			}

			output.append("\n// ===== standard library =====\n");
			appendInclude(parser, output, STANDARD_LOCATION, parser.line);
			output.append("\n// ===== shader root =====\n");
		}
		output.append("#line 1\n");
		while (parser.hasMore()) {
			if (parser.eat('/', null)) {
				advanceComment(parser, output, false);
			} else if (parser.onlyWhitespaceOnLine && parser.eat('#', null)) {
				advanceDirective(parser, output);
			} else {
				parser.advance(output);
			}
			advanceWhitespace(parser, output);
		}
	}

	private static final String doLoadSource(SharedContext shared, ResourceLocation location)
			throws ShaderLoadException {
		try {
			final var resource = shared.provider.getResource(getShaderLocation(location));
			final var raw = IOUtils.toString(resource.getInputStream(), StandardCharsets.UTF_8);

			final var builder = new StringBuilder();
			final var parser = new Parser(shared, location, raw);
			advanceAll(parser, builder);

			return builder.toString();
		} catch (FileNotFoundException ex) {
			throw makeFileNotFoundError(shared, location, ex);
		} catch (IOException ex) {
			throw new ShaderLoadException(ex, location);
		}
	}

	private record IncludeInfo(ResourceLocation source, int line, ResourceLocation include) {
	}

	private static final class SharedContext {
		public final ResourceProvider provider;
		public final ResourceLocation rootLocation;
		public final MutableSet<ResourceLocation> visitingFiles = MutableSet.hashSet();
		public final MutableList<IncludeInfo> includeStack = new Vector<>();
		public final ImmutableList<String> shaderDefines;

		public boolean hasVertexStage = false;
		public boolean hasTessControlStage = false;
		public boolean hasTessEvalStage = false;
		public boolean hasGeometryStage = false;
		public boolean hasFragmentStage = false;
		public boolean hasComputeStage = false;

		public SharedContext(ResourceProvider provider, ResourceLocation rootLocation,
				ImmutableList<String> shaderDefines) {
			this.provider = provider;
			this.rootLocation = rootLocation;
			this.shaderDefines = shaderDefines;
			this.visitingFiles.insert(rootLocation);
		}

		public boolean pushInclude(IncludeInfo info) {
			if (!this.visitingFiles.insert(info.include()))
				return false;
			this.includeStack.push(info);
			return true;
		}

		public void popInclude() {
			final var popped = this.includeStack.pop().unwrap();
			this.visitingFiles.remove(popped.include());
		}
	}

	private static final class UnspecializedSource {
		public final String unspecialized;
		public final boolean hasVertexStage;
		public final boolean hasTessControlStage;
		public final boolean hasTessEvalStage;
		public final boolean hasGeometryStage;
		public final boolean hasFragmentStage;
		public final boolean hasComputeStage;

		public UnspecializedSource(SharedContext ctx, String unspecialized) {
			this.unspecialized = unspecialized;
			this.hasVertexStage = ctx.hasVertexStage;
			this.hasTessControlStage = ctx.hasTessControlStage;
			this.hasTessEvalStage = ctx.hasTessEvalStage;
			this.hasGeometryStage = ctx.hasGeometryStage;
			this.hasFragmentStage = ctx.hasFragmentStage;
			this.hasComputeStage = ctx.hasComputeStage;
		}

		public ImmutableList<ShaderStage.Stage> getStages() {
			// @formatter:off
			final var stages = new Vector<ShaderStage.Stage>();
			if (this.hasVertexStage)      stages.push(ShaderStage.Stage.VERTEX);
			if (this.hasTessControlStage) stages.push(ShaderStage.Stage.TESSELATION_CONTROL);
			if (this.hasTessEvalStage)    stages.push(ShaderStage.Stage.TESSELATION_EVALUATION);
			if (this.hasGeometryStage)    stages.push(ShaderStage.Stage.GEOMETRY);
			if (this.hasFragmentStage)    stages.push(ShaderStage.Stage.FRAGMENT);
			if (this.hasComputeStage)     stages.push(ShaderStage.Stage.COMPUTE);
			// @formatter:on
			return stages;
		}
	}

	private static final UnspecializedSource loadSource(ResourceProvider provider, ResourceLocation location,
			Iterator<String> shaderDefines) throws ShaderLoadException {
		final var defines = new Vector<String>();
		shaderDefines.forEach(defines::push);
		final var shared = new SharedContext(provider, location, defines);
		final var source = doLoadSource(shared, location);
		return new UnspecializedSource(shared, source);
	}

	private static ResourceLocation getShaderLocation(ResourceLocation location) {
		return new ResourceLocation(location.getNamespace(), "shaders/" + location.getPath());
	}

	public static final ShaderProgram load(ResourceProvider provider, ResourceLocation location,
			AttributeSet attributeSet, GlFragmentWrites fragmentWrites, Iterator<String> shaderDefines)
			throws ShaderLoadException {
		final var source = loadSource(provider, location, shaderDefines);

		final var disposer = Disposable.scope();
		try {
			final var program = disposer.attach(new ShaderProgram(location));
			for (final var stageType : source.getStages().iterable()) {
				final var stage = disposer.attach(new ShaderStage(stageType));
				final var specializedSource = new StringBuilder();
				specializedSource.append("#version 450\n\n");
				specializedSource.append("// ===== shader stage specialization =====\n");
				specializedSource.append(String.format("#define IS_%s_STAGE\n", stageType.specializationDefine));
				for (final var stageType2 : source.getStages().iterable()) {
					specializedSource.append(String.format("#define HAS_%s_STAGE\n", stageType2.specializationDefine));
				}
				specializedSource.append("\n// ===== unspecialized shader source =====\n");
				specializedSource.append(source.unspecialized);
				stage.setSource(specializedSource.toString());
				if (!stage.compile()) {
					// HawkLib.LOGGER.warn("SHADER SOURCE '{}' ({}):\n\n{}\n\n", location.toString(),
					// 		stageType.description, specializedSource.toString());
					final var message = String.format("Failed to compile shader '%s' (%s):\n\n%s\n\n",
							location.toString(), stageType.description, stage.infoLog());
					throw new ShaderLoadException(message, location);
				}
				program.attachShader(stage);
			}
			if (!program.link(attributeSet, fragmentWrites)) {
				final var message = String.format("Failed to link shader program '%s':\n\n%s\n\n",
						location.toString(), program.infoLog());
				throw new ShaderLoadException(message, location);
			}

			disposer.detach(program);
			return program;
		} finally {
			// try-with-resources was giving me a self-suppression exception for some rason
			// so im doing this manually.
			disposer.close();
		}
	}

}
