package net.xavil.ultraviolet.client.screen.layer;

import javax.annotation.Nonnull;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.gui.font.glyphs.EmptyGlyph;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSink;
import net.xavil.hawklib.client.flexible.BufferLayout;
import net.xavil.hawklib.client.flexible.IndexPattern;
import net.xavil.hawklib.client.flexible.Mesh;
import net.xavil.hawklib.client.flexible.vertex.ElementInfo;
import net.xavil.hawklib.client.flexible.vertex.VertexBuilder;
import net.xavil.hawklib.client.flexible.vertex.VertexDispatcher;
import net.xavil.hawklib.client.gl.DrawState;
import net.xavil.hawklib.client.gl.GlBuffer;
import net.xavil.hawklib.client.gl.texture.GlTexture2d;
import net.xavil.hawklib.collections.impl.Vector;
import net.xavil.hawklib.collections.impl.VectorFloat;
import net.xavil.hawklib.collections.interfaces.MutableMap;
import net.xavil.hawklib.math.ColorRgba;
import net.xavil.hawklib.math.matrices.Mat4;
import net.xavil.hawklib.math.matrices.interfaces.Mat4Access;
import net.xavil.ultraviolet.client.UltravioletShaders;
import net.xavil.ultraviolet.mixin.accessor.FontAccessor;

public final class TextBuilder implements FormattedCharSink {

	public static final VertexBuilder SSBO_BUILDER = new VertexBuilder(0x8000);
	public static final GlBuffer SSBO_BATCH_BUFFER = new GlBuffer();
	private static final Mesh TEXT_MESH = new Mesh();
	public static final BufferLayout.Attribute USAGE_BATCH_ID = new BufferLayout.Attribute("Batch ID");

	private static final BufferLayout TEXT_LAYOUT = BufferLayout.builder()
			.element(BufferLayout.ELEMENT_FLOAT3, BufferLayout.Attribute.POSITION)
			.element(BufferLayout.ELEMENT_FLOAT_UBYTE_NORM4, BufferLayout.Attribute.COLOR)
			.element(BufferLayout.ELEMENT_FLOAT_SHORT_NORM2, BufferLayout.Attribute.UV0)
			.element(BufferLayout.ELEMENT_SHORT1, USAGE_BATCH_ID)
			.build();

	private static final BufferLayout SSBO_LAYOUT = BufferLayout.builder()
			.element(BufferLayout.ELEMENT_MAT4X4, BufferLayout.Attribute.MODEL_MATRIX)
			.element(BufferLayout.ELEMENT_FLOAT_UBYTE_NORM4, BufferLayout.Attribute.COLOR)
			.build(BufferLayout.FINALIZER_STD430);

	public enum TextOrigin {
		TOP_LEFT(0, 0), TOP(0.5, 0), TOP_RIGHT(1, 0),
		LEFT(0, 0.5), CENTER(0.5, 0.5), RIGHT(1, 0.5),
		BOTTOM_LEFT(0, 1), BOTTOM(0.5, 1), BOTTOM_RIGHT(1, 1);

		public final double xFactor, yFactor;

		private TextOrigin(double xFactor, double yFactor) {
			this.xFactor = xFactor;
			this.yFactor = yFactor;
		}
	}

	public TextOrigin textOrigin;
	public double baseScale, scale;
	public double x, y, z;
	public double cursorX, cursorY;
	public double lastPosX, lastPosY;
	public double lastSizeX, lastSizeY;

	@Nonnull
	public ColorRgba baseColor = ColorRgba.WHITE;

	private final Font font = Minecraft.getInstance().font;

	private final MutableMap<ResourceLocation, Vector<TextBatch>> batches = MutableMap.hashMap();
	private final MutableMap<ResourceLocation, TextBatch> currentBatches = MutableMap.hashMap();

	public Mat4 currentBatchModelMatrix;
	public ColorRgba currentBatchColor;

	public TextBuilder() {
		reset();
	}

	public void reset() {
		this.textOrigin = TextOrigin.BOTTOM_LEFT;
		this.baseScale = 0.005f;
		this.scale = 1;
		this.x = this.y = this.z = 0;
		this.cursorX = this.cursorY = 0;
		this.lastPosX = this.lastPosY = 0;
		this.lastSizeX = this.lastSizeY = 0;
	}

	private static final class TextBatch {
		private static final int PARAM_COUNT = 6;

		public final Mat4 modelMatrix;
		public final ColorRgba color;
		public final float scale;

		public final Vector<BakedGlyph> glyphs = new Vector<>();
		// [x y z r g b a]
		public final VectorFloat params = new VectorFloat();

		public TextBatch(Mat4 modelMatrix, ColorRgba color, double scale) {
			this.modelMatrix = modelMatrix;
			this.color = color;
			this.scale = (float) scale;
		}

		public void push(BakedGlyph glyph, float x, float y, float z, float r, float g, float b, float a) {
			this.glyphs.push(glyph);
			this.params.push(x);
			this.params.push(y);
			this.params.push(z);
			this.params.push(r);
			this.params.push(g);
			this.params.push(b);
			this.params.push(a);
		}

		public void draw(TextDispatcher builder, int batchId) {
			for (int i = 0; i < this.glyphs.size(); ++i) {
				final var glyph = this.glyphs.get(i);
				int j = 7 * i;
				final var x = this.params.get(j++);
				final var y = this.params.get(j++);
				final var z = this.params.get(j++);
				final var r = this.params.get(j++);
				final var g = this.params.get(j++);
				final var b = this.params.get(j++);
				final var a = this.params.get(j++);

				boolean italic = false;

				float hh = glyph.up - 3;
				float hl = glyph.down - 3;
				float yh = y + hh * this.scale;
				float yl = y + hl * this.scale;

				float xoh = this.scale * (italic ? 1 - 0.25f * hh : 0.0f);
				float xol = this.scale * (italic ? 1 - 0.25f * hl : 0.0f);
				float xl = x + glyph.left * this.scale;
				float xh = x + glyph.right * this.scale;
				builder.batchId(batchId)
						.vertex(xh + xol, yh, z)
						.color(r, g, b, a)
						.uv0(glyph.u1, glyph.v1)
						.endVertex();
				builder.batchId(batchId)
						.vertex(xh + xoh, yl, z)
						.color(r, g, b, a)
						.uv0(glyph.u1, glyph.v0)
						.endVertex();
				builder.batchId(batchId)
						.vertex(xl + xoh, yl, z)
						.color(r, g, b, a)
						.uv0(glyph.u0, glyph.v0)
						.endVertex();
				builder.batchId(batchId)
						.vertex(xl + xol, yh, z)
						.color(r, g, b, a)
						.uv0(glyph.u0, glyph.v1)
						.endVertex();

			}
		}
	}

	// lmfao
	private static ResourceLocation extractTextureFromRenderType(RenderType renderType) {
		if (renderType instanceof RenderType.CompositeRenderType composite) {
			if (composite.state.textureState instanceof RenderStateShard.TextureStateShard shard) {
				if (shard.texture.isPresent()) {
					return shard.texture.get();
				}
			}
		}
		throw new IllegalArgumentException(String.format(
				"RenderType {} has no TextureStateShard",
				renderType.toString()));
	}

	@Override
	public boolean accept(int var1, Style style, int character) {
		final var fontSet = FontAccessor.getFontSet(this.font, style.getFont());
		final var glyphInfo = fontSet.getGlyphInfo(character);
		final var bakedGlyph = style.isObfuscated() && !Character.isSpaceChar(character)
				? fontSet.getRandomGlyph(glyphInfo)
				: fontSet.getGlyph(character);

		float r = this.baseColor.r(), g = this.baseColor.g(), b = this.baseColor.b(), a = this.baseColor.a();
		final var textColor = style.getColor();
		if (textColor != null) {
			final var k = textColor.getValue();
			r = (float) (k >> 16 & 0xFF) / 255f;
			g = (float) (k >> 8 & 0xFF) / 255f;
			b = (float) (k & 0xFF) / 255f;
		}

		if (!(bakedGlyph instanceof EmptyGlyph)) {
			final var renderType = bakedGlyph.renderType(Font.DisplayMode.NORMAL);
			final var texture = extractTextureFromRenderType(renderType);

			TextBatch batch = this.currentBatches.getOrNull(texture);
			if (batch == null) {
				this.currentBatches.insert(texture, batch = new TextBatch(
						this.currentBatchModelMatrix,
						this.currentBatchColor,
						this.baseScale * this.scale));
			}

			batch.push(bakedGlyph, (float) this.x, (float) this.y, (float) this.z, r, g, b, a);
		}

		this.x += glyphInfo.getAdvance(style.isBold()) * this.baseScale * this.scale;
		return true;
	}

	public void emitNewline(Mat4Access tfm, String text) {
		emit(tfm, ColorRgba.WHITE, FormattedText.of(text));
		cursorAppendUnder();
	}

	public void emitNewline(Mat4Access tfm, FormattedText text) {
		emit(tfm, ColorRgba.WHITE, text);
		cursorAppendUnder();
	}

	public void emit(Mat4Access tfm, String text) {
		emit(tfm, ColorRgba.WHITE, FormattedText.of(text));
	}

	public void emit(Mat4Access tfm, FormattedText text) {
		emit(tfm, ColorRgba.WHITE, text);
	}

	// does not change the location of the cursor.
	public void emit(Mat4Access tfm, ColorRgba tint, FormattedText text) {
		this.currentBatchModelMatrix = tfm.asImmutable();
		this.currentBatchColor = tint;

		final var scale = this.baseScale * this.scale;

		final var lines = this.font.split(text, Integer.MAX_VALUE);
		final var height = scale * this.font.lineHeight * lines.size();
		this.y = (-scale * this.font.lineHeight) + this.cursorY + this.textOrigin.yFactor * height;
		for (final var cs : lines) {
			final var width = scale * this.font.width(cs);
			this.x = this.cursorX - this.textOrigin.xFactor * width;
			// FIXME: right-to-left text?
			cs.accept(this);
			this.y -= this.font.lineHeight * scale;
		}

		this.lastPosX = this.cursorX;
		this.lastPosY = this.cursorY;
		this.lastSizeX = Math.abs(this.x - this.cursorX);
		this.lastSizeY = Math.abs(this.y - this.cursorY);

		finishBatch();
	}

	public void cursorAppendUnder() {
		this.cursorY -= this.lastSizeY;
	}

	public void cursorAppendAfter() {
		this.cursorX += this.lastSizeX;
	}

	public void cursorNewline() {
		this.cursorY -= Minecraft.getInstance().font.lineHeight;
	}

	public void cursorIndent(float amount) {
		this.cursorX += amount;
	}

	private void finishBatch() {
		for (final var key : this.currentBatches.keys().iterable()) {
			if (!this.batches.containsKey(key))
				this.batches.insert(key, new Vector<>());
			this.batches.getOrThrow(key).push(this.currentBatches.getOrThrow(key));
		}
		this.currentBatches.clear();
	}

	public void draw(VertexBuilder builder, DrawState drawState, float alpha) {

		final var shader = UltravioletShaders.SHADER_TEXT.get();

		for (final var entry : this.batches.entries().iterable()) {
			final var batches = entry.getOrThrow();

			final var glyphBuilder = builder.begin(new TextDispatcher(),
					IndexPattern.QUADS, TEXT_LAYOUT);
			final var ssboBuilder = SSBO_BUILDER.begin(new TextBatchSsboDispatcher(),
					null, SSBO_LAYOUT);
			// final var ssboBuilder = SSBO_BUILDER.begin(new TextBatchSsboDispatcher(),
			// null, BufferLayout.builder()
			// .element(BufferLayout.ELEMENT_MAT4X4, BufferLayout.Attribute.MODEL_MATRIX)
			// .element(BufferLayout.ELEMENT_FLOAT_UBYTE_NORM4,
			// BufferLayout.Attribute.COLOR)
			// .build(BufferLayout.FINALIZER_STD430));

			for (int i = 0; i < batches.size(); ++i) {
				final var batch = batches.get(i);
				ssboBuilder.modelMatrix(batch.modelMatrix).color(batch.color).endVertex();
				batch.draw(glyphBuilder, i);
			}

			final var fontTexture = GlTexture2d.importTexture(entry.key);
			shader.setupDefaultShaderUniforms();
			shader.setUniformf("uColor", ColorRgba.WHITE.withA(alpha));
			shader.setUniformSampler("uFontAtlas", fontTexture);
			// final var scale = 1f;
			// final var mat = Matrix4f.createScaleMatrix(scale, -scale, scale);

			TEXT_MESH.setupAndUpload(glyphBuilder.end());
			TEXT_MESH.uploadSsbo("bBatchingInfos", ssboBuilder.end());
			TEXT_MESH.draw(shader, drawState);
		}

		this.batches.clear();
		this.currentBatches.clear();
	}

	private static final class TextDispatcher extends VertexDispatcher.Generic {

		private ElementInfo.Int batchId;

		@Override
		protected void registerAttributes(AttributeRegistrationContext ctx) {
			super.registerAttributes(ctx);
			ctx.registerInt(USAGE_BATCH_ID, info -> this.batchId = info);
		}

		public TextDispatcher batchId(int batchId) {
			if (this.batchId != null) {
				this.batchId.setInt(0, 0, batchId);
			}
			return this;
		}

	}

	private static final class TextBatchSsboDispatcher extends VertexDispatcher {

		private ElementInfo.Float modelMatrix;
		private ElementInfo.Float color;

		@Override
		protected void registerAttributes(AttributeRegistrationContext ctx) {
			ctx.registerFloat(BufferLayout.Attribute.MODEL_MATRIX, info -> this.modelMatrix = info);
			ctx.registerFloat(BufferLayout.Attribute.COLOR, info -> this.color = info);
		}

		public TextBatchSsboDispatcher modelMatrix(Mat4Access matrix) {
			if (this.modelMatrix != null) {
				this.modelMatrix.setFloats(0, matrix);
			}
			return this;
		}

		public TextBatchSsboDispatcher color(float r, float g, float b, float a) {
			if (this.color != null) {
				this.color.setFloats(0, r, g, b, a);
			}
			return this;
		}

		public TextBatchSsboDispatcher color(ColorRgba color) {
			return color(color.r, color.g, color.b, color.a);
		}

	}

}