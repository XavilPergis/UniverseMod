package net.xavil.hawklib.client.gl;

import java.time.Duration;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.lwjgl.opengl.GL45C;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mojang.blaze3d.vertex.BufferUploader;

import net.minecraft.client.renderer.ShaderInstance;
import net.xavil.hawklib.ErrorRatelimiter;
import net.xavil.hawklib.client.gl.texture.GlTexture;
import net.xavil.hawklib.collections.impl.Vector;
import net.xavil.hawklib.collections.interfaces.MutableList;
import net.xavil.hawklib.collections.interfaces.MutableMap;
import net.xavil.hawklib.hash.FastHasher;
import net.xavil.hawklib.hash.Hashable;
import net.xavil.hawklib.hash.Hasher;

public final class GlManager {

	public static final GlManager INSTANCE = new GlManager();

	private final MutableList<GlState> stack = new Vector<>();
	private final MutableList<GlState> freeStates = new Vector<>();

	// the OpenGL state that needs to be restored after we pop our final state.
	private GlState rootState = null;
	private GlState current = null;
	private GlStateSink currentSink = UnmanagedStateSink.INSTANCE;

	private GlManager() {
	}

	private GlState fetchNewState() {
		if (!this.freeStates.isEmpty()) {
			return this.freeStates.pop().unwrap();
		} else {
			return new GlState();
		}
	}

	private void push() {
		if (this.current == null) {
			// vanilla tracks some state in random places so that it can deduplicate binds,
			// which need to be reset to make them always re-bind after we do stuff.
			//
			// im not sure why the last pop() doesn't restore all the state correctly,
			// though...
			BufferUploader.reset();
			ShaderInstance.lastProgramId = -1;

			this.rootState = fetchNewState();
			this.rootState.reset(null);
			this.rootState.capture();
		}

		final var newState = fetchNewState();
		newState.reset(this.current == null ? this.rootState : this.stack.last().unwrap());
		this.stack.push(newState);
		this.current = newState;
		this.currentSink = this.current;
	}

	private void pop() {
		final var prev = this.stack.pop().unwrap();
		prev.restorePrevious();

		if (this.stack.isEmpty()) {
			this.current = null;
			this.currentSink = UnmanagedStateSink.INSTANCE;
			this.freeStates.push(this.rootState);
			this.rootState = null;
		} else {
			this.currentSink = this.current = this.stack.last().unwrap();
		}
		this.freeStates.push(prev);
	}

	public static GlState currentState() {
		if (INSTANCE.current == null) {
			throw new IllegalStateException("Cannot access the current OpenGL state because GlManager is not active!");
		}
		return INSTANCE.current;
	}

	public static boolean isManaged() {
		return INSTANCE.current != null;
	}

	public static void pushState() {
		INSTANCE.push();
	}

	public static void popState() {
		INSTANCE.pop();
	}

	public static enum DebugMessageSource {
		API(GL45C.GL_DEBUG_SOURCE_API, "API"),
		WINDOW_SYSTEM(GL45C.GL_DEBUG_SOURCE_WINDOW_SYSTEM, "Window System"),
		SHADER_COMPILER(GL45C.GL_DEBUG_SOURCE_SHADER_COMPILER, "Shader Compiler"),
		THIRD_PARTY(GL45C.GL_DEBUG_SOURCE_THIRD_PARTY, "Third Party"),
		APPLICATION(GL45C.GL_DEBUG_SOURCE_APPLICATION, "Application"),
		OTHER(GL45C.GL_DEBUG_SOURCE_OTHER, "Other");

		public final int id;
		public final String description;

		private DebugMessageSource(int id, String description) {
			this.id = id;
			this.description = description;
		}

		@Override
		public String toString() {
			return this.description;
		}

		public static DebugMessageSource from(int id) {
			return switch (id) {
				case GL45C.GL_DEBUG_SOURCE_API -> API;
				case GL45C.GL_DEBUG_SOURCE_WINDOW_SYSTEM -> WINDOW_SYSTEM;
				case GL45C.GL_DEBUG_SOURCE_SHADER_COMPILER -> SHADER_COMPILER;
				case GL45C.GL_DEBUG_SOURCE_THIRD_PARTY -> THIRD_PARTY;
				case GL45C.GL_DEBUG_SOURCE_APPLICATION -> APPLICATION;
				case GL45C.GL_DEBUG_SOURCE_OTHER -> OTHER;
				default -> null;
			};
		}
	}

	public static enum DebugMessageType {
		ERROR(GL45C.GL_DEBUG_TYPE_ERROR, "Error"),
		DEPRECATED_BEHAVIOR(GL45C.GL_DEBUG_TYPE_DEPRECATED_BEHAVIOR, "Deprecated"),
		UNDEFINED_BEHAVIOR(GL45C.GL_DEBUG_TYPE_UNDEFINED_BEHAVIOR, "UB"),
		PORTABILITY(GL45C.GL_DEBUG_TYPE_PORTABILITY, "Portability"),
		PERFORMANCE(GL45C.GL_DEBUG_TYPE_PERFORMANCE, "Performance"),
		MARKER(GL45C.GL_DEBUG_TYPE_MARKER, "Marker"),
		PUSH_GROUP(GL45C.GL_DEBUG_TYPE_PUSH_GROUP, "Group Push"),
		POP_GROUP(GL45C.GL_DEBUG_TYPE_POP_GROUP, "Group Pop"),
		OTHER(GL45C.GL_DEBUG_TYPE_OTHER, "Other");

		public final int id;
		public final String description;

		private DebugMessageType(int id, String description) {
			this.id = id;
			this.description = description;
		}

		@Override
		public String toString() {
			return this.description;
		}

		public static DebugMessageType from(int id) {
			return switch (id) {
				case GL45C.GL_DEBUG_TYPE_ERROR -> ERROR;
				case GL45C.GL_DEBUG_TYPE_DEPRECATED_BEHAVIOR -> DEPRECATED_BEHAVIOR;
				case GL45C.GL_DEBUG_TYPE_UNDEFINED_BEHAVIOR -> UNDEFINED_BEHAVIOR;
				case GL45C.GL_DEBUG_TYPE_PORTABILITY -> PORTABILITY;
				case GL45C.GL_DEBUG_TYPE_PERFORMANCE -> PERFORMANCE;
				case GL45C.GL_DEBUG_TYPE_MARKER -> MARKER;
				case GL45C.GL_DEBUG_TYPE_PUSH_GROUP -> PUSH_GROUP;
				case GL45C.GL_DEBUG_TYPE_POP_GROUP -> POP_GROUP;
				case GL45C.GL_DEBUG_TYPE_OTHER -> OTHER;
				default -> null;
			};
		}
	}

	public static enum DebugMessageSeverity {
		HIGH(GL45C.GL_DEBUG_SEVERITY_HIGH, "High",
				(logger, fmt, param) -> logger.error(fmt, param)),
		MEDIUM(GL45C.GL_DEBUG_SEVERITY_MEDIUM, "Medium",
				(logger, fmt, param) -> logger.warn(fmt, param)),
		LOW(GL45C.GL_DEBUG_SEVERITY_LOW, "Low",
				(logger, fmt, param) -> logger.info(fmt, param)),
		NOTIFICATION(GL45C.GL_DEBUG_SEVERITY_NOTIFICATION, "Notification",
				(logger, fmt, param) -> logger.info(fmt, param));

		public static interface LogEmitter {
			void log(Logger logger, String format, Object... params);
		}

		public final int id;
		public final String description;
		public final LogEmitter logger;

		private DebugMessageSeverity(int id, String description, LogEmitter logger) {
			this.id = id;
			this.description = description;
			this.logger = logger;
		}

		@Override
		public String toString() {
			return this.description;
		}

		public void log(String format, Object... params) {
			this.logger.log(LOGGER, format, params);
		}

		public static DebugMessageSeverity from(int id) {
			return switch (id) {
				case GL45C.GL_DEBUG_SEVERITY_HIGH -> HIGH;
				case GL45C.GL_DEBUG_SEVERITY_MEDIUM -> MEDIUM;
				case GL45C.GL_DEBUG_SEVERITY_LOW -> LOW;
				case GL45C.GL_DEBUG_SEVERITY_NOTIFICATION -> NOTIFICATION;
				default -> null;
			};
		}
	}

	public static enum ErrorType {
		INVALID_ENUM(GL45C.GL_INVALID_ENUM, "Invalid Enum"),
		INVALID_VALUE(GL45C.GL_INVALID_VALUE, "Invalid Value"),
		INVALID_OPERATION(GL45C.GL_INVALID_OPERATION, "Invalid Operation"),
		STACK_OVERFLOW(GL45C.GL_STACK_OVERFLOW, "Stack Overflow"),
		STACK_UNDERFLOW(GL45C.GL_STACK_UNDERFLOW, "Stack Underflow"),
		OUT_OF_MEMORY(GL45C.GL_OUT_OF_MEMORY, "Out Of Memory"),
		INVALID_FRAMEBUFFER_OPERATION(GL45C.GL_INVALID_FRAMEBUFFER_OPERATION, "Invalid Framebuffer Operation"),
		CONTEXT_LOST(GL45C.GL_CONTEXT_LOST, "Context Lost");

		public final int id;
		public final String description;

		private ErrorType(int id, String description) {
			this.id = id;
			this.description = description;
		}

		@Override
		public String toString() {
			return this.description;
		}

		public static ErrorType from(int id) {
			return switch (id) {
				case GL45C.GL_INVALID_ENUM -> INVALID_ENUM;
				case GL45C.GL_INVALID_VALUE -> INVALID_VALUE;
				case GL45C.GL_INVALID_OPERATION -> INVALID_OPERATION;
				case GL45C.GL_STACK_OVERFLOW -> STACK_OVERFLOW;
				case GL45C.GL_STACK_UNDERFLOW -> STACK_UNDERFLOW;
				case GL45C.GL_OUT_OF_MEMORY -> OUT_OF_MEMORY;
				case GL45C.GL_INVALID_FRAMEBUFFER_OPERATION -> INVALID_FRAMEBUFFER_OPERATION;
				case GL45C.GL_CONTEXT_LOST -> CONTEXT_LOST;
				default -> null;
			};
		}
	}

	public static final Logger LOGGER = LoggerFactory.getLogger("OpenGL Debug");
	public static final boolean ENABLE_DEBUG = Boolean.valueOf(System.getProperty("net.xavil.gl.enable_debug"));

	private static void debugMessageControl(
			@Nullable DebugMessageSource source,
			@Nullable DebugMessageType type,
			@Nullable DebugMessageSeverity severity,
			boolean enabled) {
		GL45C.glDebugMessageControl(
				source == null ? GL45C.GL_DONT_CARE : source.id,
				type == null ? GL45C.GL_DONT_CARE : type.id,
				severity == null ? GL45C.GL_DONT_CARE : severity.id,
				(int[]) null, enabled);
	}

	private static void debugMessageControl(
			@Nonnull DebugMessageSource source,
			@Nonnull DebugMessageType type,
			int id,
			boolean enabled) {
		GL45C.glDebugMessageControl(source.id, type.id, GL45C.GL_DONT_CARE, new int[] { id }, enabled);
	}

	private static void debugMessageControl(
			@Nonnull DebugMessageSource source,
			@Nonnull DebugMessageType type,
			int id,
			Duration ratelimitDuration,
			int ratelimitCount) {
		debugMessageControl(source, type, id, true);
		final var filter = new DebugMessageFilter(source, type, id);
		ERROR_RATELIMITERS.insert(filter, new ErrorRatelimiter(ratelimitDuration, ratelimitCount));
	}

	private static final class DebugMessageFilter implements Hashable {
		public final DebugMessageSource source;
		public final DebugMessageType type;
		public final int id;

		public DebugMessageFilter(DebugMessageSource source, DebugMessageType type, int id) {
			this.source = source;
			this.type = type;
			this.id = id;
		}

		@Override
		public void appendHash(Hasher hasher) {
			hasher.appendEnum(source).appendEnum(type).appendInt(id);
		}

		@Override
		public final int hashCode() {
			return FastHasher.hashToInt(this);
		}
	}

	private static final MutableMap<DebugMessageFilter, ErrorRatelimiter> ERROR_RATELIMITERS = MutableMap
			.hashMap();

	private static void debugMessageCallback(int sourceRaw, int typeRaw, int id, int severityRaw,
			int messageLength, long messagePointer, long userParam) {
		final var source = DebugMessageSource.from(sourceRaw);
		final var type = DebugMessageType.from(typeRaw);
		final var severity = DebugMessageSeverity.from(severityRaw);

		final var filter = new DebugMessageFilter(source, type, id);
		final var limiter = ERROR_RATELIMITERS.getOrNull(filter);
		if (limiter != null && !limiter.throttle()) {
			final var message = MemoryUtil.memASCII(messagePointer, messageLength);
			severity.log("[{}] [{}/{}]: {}", id, source, type, message);
		}
	}

	public static void setupDebugMessageCallback() {
		if (!ENABLE_DEBUG)
			return;
		GL45C.glEnable(GL45C.GL_DEBUG_OUTPUT_SYNCHRONOUS);
		GL45C.glEnable(GL45C.GL_DEBUG_OUTPUT);
		GL45C.glDebugMessageCallback(GlManager::debugMessageCallback, 0);
		// enable all messages
		debugMessageControl(null, null, null, true);
		// ...except for buffer source info, which is really really really spammy for
		// minecraft
		debugMessageControl(DebugMessageSource.API, DebugMessageType.OTHER, 131185, false);
		debugMessageControl(DebugMessageSource.API, DebugMessageType.PERFORMANCE, 131186, Duration.ofMillis(5000), 1);
	}

	public static ErrorType getError() {
		if (!ENABLE_DEBUG)
			return null;
		final var error = GL45C.glGetError();
		if (error == GL45C.GL_NO_ERROR)
			return null;
		return ErrorType.from(error);
	}

	public static void checkErrors() {
		if (!ENABLE_DEBUG)
			return;
		ErrorType error = getError();
		while (error != null) {
			LOGGER.error("OpenGL Error: {}", error);
			error = getError();
		}
	}

	public static void assertNoErrors() {
		if (!ENABLE_DEBUG)
			return;
		ErrorType error = getError();
		if (error != null) {
			LOGGER.error("OpenGL Error: {}", error);
			throw new IllegalStateException(error.description);
		}
	}

	public static void bindFramebuffer(int target, int id) {
		INSTANCE.currentSink.bindFramebuffer(target, id);
	}

	public static void bindBuffer(GlBuffer.Type target, int id) {
		INSTANCE.currentSink.bindBuffer(target, id);
	}

	public static void bindTexture(GlTexture.Type target, int id) {
		INSTANCE.currentSink.bindTexture(target, id);
	}

	public static void bindVertexArray(int id) {
		INSTANCE.currentSink.bindVertexArray(id);
	}

	public static void useProgram(int id) {
		INSTANCE.currentSink.bindProgram(id);
	}

	public static void bindRenderbuffer(int id) {
		INSTANCE.currentSink.bindRenderbuffer(id);
	}

	public static void activeTexture(int unit) {
		INSTANCE.currentSink.bindTextureUnit(unit);
	}

	public static void enableCull(boolean enable) {
		INSTANCE.currentSink.enableCull(enable);
	}

	public static void enableBlend(boolean enable) {
		INSTANCE.currentSink.enableBlend(enable);
	}

	public static void enableDepthTest(boolean enable) {
		INSTANCE.currentSink.enableDepthTest(enable);
	}

	public static void enableLogicOp(boolean enable) {
		INSTANCE.currentSink.enableLogicOp(enable);
	}

	public static void polygonMode(GlState.PolygonMode mode) {
		INSTANCE.currentSink.polygonMode(mode);
	}

	public static void cullFace(GlState.CullFace cullFace) {
		INSTANCE.currentSink.cullFace(cullFace);
	}

	public static void frontFace(GlState.FrontFace frontFace) {
		INSTANCE.currentSink.frontFace(frontFace);
	}

	public static void depthMask(boolean depthMask) {
		INSTANCE.currentSink.depthMask(depthMask);
	}

	public static void depthFunc(GlState.DepthFunc depthFunc) {
		INSTANCE.currentSink.depthFunc(depthFunc);
	}

	public static void logicOp(GlState.LogicOp logicOp) {
		INSTANCE.currentSink.logicOp(logicOp);
	}

	public static void blendEquation(GlState.BlendEquation blendEquationRgb, GlState.BlendEquation blendEquationAlpha) {
		INSTANCE.currentSink.blendEquation(blendEquationRgb, blendEquationAlpha);
	}

	public static void blendFunc(GlState.BlendFactor blendFactorSrc, GlState.BlendFactor blendFactorDst) {
		INSTANCE.currentSink.blendFunc(blendFactorSrc, blendFactorDst, blendFactorSrc, blendFactorDst);
	}

	public static void blendFunc(GlState.BlendFactor blendFactorSrcRgb, GlState.BlendFactor blendFactorDstRgb,
			GlState.BlendFactor blendFactorSrcAlpha, GlState.BlendFactor blendFactorDstAlpha) {
		INSTANCE.currentSink.blendFunc(blendFactorSrcRgb, blendFactorDstRgb, blendFactorSrcAlpha, blendFactorDstAlpha);
	}

	public static void colorMask(boolean colorMaskR, boolean colorMaskG, boolean colorMaskB, boolean colorMaskA) {
		INSTANCE.currentSink.colorMask(colorMaskR, colorMaskG, colorMaskB, colorMaskA);
	}

	public static void setViewport(int x, int y, int w, int h) {
		INSTANCE.currentSink.setViewport(x, y, w, h);
	}

	public static void enableProgramPointSize(boolean enable) {
		INSTANCE.currentSink.enableProgramPointSize(enable);
	}
}
