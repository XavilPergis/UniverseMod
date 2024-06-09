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
import net.xavil.hawklib.client.flexible.PrimitiveType;
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
	public static final BufferLayout.Attribute USAGE_BATCH_ID = new BufferLayout.Attribute("Batch ID");

	private static final BufferLayout TEXT_LAYOUT = BufferLayout.builder()
			.element(BufferLayout.ELEMENT_FLOAT3, BufferLayout.Attribute.POSITION)
			.element(BufferLayout.ELEMENT_FLOAT_UBYTE_NORM4, BufferLayout.Attribute.COLOR)
			.element(BufferLayout.ELEMENT_FLOAT_SHORT_NORM2, BufferLayout.Attribute.UV0)
			.element(BufferLayout.ELEMENT_SHORT1, USAGE_BATCH_ID)
			.build();

	private static final BufferLayout SSBO_LAYOUT = BufferLayout.builder()
			.element(BufferLayout.ELEMENT_MAT4, BufferLayout.Attribute.MODEL_MATRIX)
			.element(BufferLayout.ELEMENT_FLOAT_UBYTE_NORM4, BufferLayout.Attribute.COLOR)
			.build();

	public double x = 0, y = 0;
	public double cursorX = 0, cursorY = 0;
	public double lastPosX = 0, lastPosY = 0;
	public double lastSizeX = 0, lastSizeY = 0;

	@Nonnull
	public ColorRgba baseColor = ColorRgba.WHITE;

	private final Font font = Minecraft.getInstance().font;

	private final MutableMap<ResourceLocation, Vector<TextBatch>> batches = MutableMap.hashMap();
	private final MutableMap<ResourceLocation, TextBatch> currentBatches = MutableMap.hashMap();

	public Mat4 currentBatchModelMatrix;
	public ColorRgba currentBatchColor;

	public TextBuilder() {
	}

	private static final class TextBatch {
		private static final int PARAM_COUNT = 6;

		public final Mat4 modelMatrix;
		public final ColorRgba color;

		public TextBatch(Mat4 modelMatrix, ColorRgba color) {
			this.modelMatrix = modelMatrix;
			this.color = color;
		}

		public final Vector<BakedGlyph> glyphs = new Vector<>();
		// [x y r g b a]
		public final VectorFloat params = new VectorFloat();

		public void push(BakedGlyph glyph, float x, float y, float r, float g, float b, float a) {
			this.glyphs.push(glyph);
			this.params.push(x);
			this.params.push(y);
			this.params.push(r);
			this.params.push(g);
			this.params.push(b);
			this.params.push(a);
		}

		public void draw(TextDispatcher builder, int batchId) {
			for (int i = 0; i < this.glyphs.size(); ++i) {
				final var glyph = this.glyphs.get(i);
				int j = PARAM_COUNT * i;
				final var x = this.params.get(j++);
				final var y = this.params.get(j++);
				final var r = this.params.get(j++);
				final var g = this.params.get(j++);
				final var b = this.params.get(j++);
				final var a = this.params.get(j++);

				boolean italic = false;

				float hh = glyph.up - 3;
				float hl = glyph.down - 3;
				float yh = -y + hh;
				float yl = -y + hl;

				float xoh = italic ? 1 - 0.25f * hh : 0.0f;
				float xol = italic ? 1 - 0.25f * hl : 0.0f;
				float xl = x + glyph.left;
				float xh = x + glyph.right;
				builder.batchId(batchId)
						.vertex(xl + xoh, yh, 0)
						.color(r, g, b, a)
						.uv0(glyph.u0, glyph.v0)
						.endVertex();
				builder.batchId(batchId)
						.vertex(xl + xol, yl, 0)
						.color(r, g, b, a)
						.uv0(glyph.u0, glyph.v1)
						.endVertex();
				builder.batchId(batchId)
						.vertex(xh + xol, yl, 0)
						.color(r, g, b, a)
						.uv0(glyph.u1, glyph.v1)
						.endVertex();
				builder.batchId(batchId)
						.vertex(xh + xoh, yh, 0)
						.color(r, g, b, a)
						.uv0(glyph.u1, glyph.v0)
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
						this.currentBatchColor));
			}

			batch.push(bakedGlyph, (float) this.x, (float) this.y, r, g, b, a);
		}

		this.x += glyphInfo.getAdvance(style.isBold());
		return true;
	}

	public void emitNewline(Mat4Access tfm, String text) {
		emit(tfm, ColorRgba.WHITE, FormattedText.of(text));
		cursorNewline();
	}

	public void emitNewline(Mat4Access tfm, FormattedText text) {
		emit(tfm, ColorRgba.WHITE, text);
		cursorNewline();
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

		final var lines = this.font.split(text, Integer.MAX_VALUE);
		// final var height = this.font.lineHeight * lines.size();
		this.y = this.cursorY;
		for (final var cs : lines) {
			// final var width = this.font.width(cs);
			this.x = this.cursorX;
			// FIXME: right-to-left text?
			cs.accept(this);
			this.y -= this.font.lineHeight;
		}

		this.lastPosX = this.cursorX;
		this.lastPosY = this.cursorY;
		this.lastSizeX = Math.abs(this.x - this.cursorX);
		this.lastSizeY = Math.abs(this.y - this.cursorY);
	}

	public void cursorNewline() {
		this.cursorY -= this.lastSizeY;
	}

	public void cursorAppend() {
		this.cursorX += this.lastSizeX;
	}

	public void cursorIndent(float amount) {
		this.cursorX += amount;
	}

	public void draw(VertexBuilder builder, DrawState drawState, float alpha) {

		this.batches.clear();
		this.currentBatches.clear();

		// final var shader = UltravioletShaders.SHADER_TEXT.get();

		// for (final var entry : this.batches.entries().iterable()) {
		// 	final var batches = entry.getOrThrow();

		// 	final var glyphBuilder = builder.begin(new TextDispatcher(),
		// 			PrimitiveType.QUAD_DUPLICATED, BufferLayout.POSITION_COLOR_TEX_LIGHTMAP);
		// 	final var ssboBuilder = SSBO_BUILDER.begin(new TextBatchSsboDispatcher(),
		// 			null, SSBO_LAYOUT);

		// 	for (int i = 0; i < batches.size(); ++i) {
		// 		final var batch = batches.get(i);
		// 		ssboBuilder.modelMatrix(batch.modelMatrix).color(batch.color).endVertex();
		// 		batch.draw(glyphBuilder, i);
		// 	}

		// 	final var fontTexture = GlTexture2d.importTexture(entry.key);
		// 	shader.setupDefaultShaderUniforms();
		// 	shader.setUniformf("uColor", ColorRgba.WHITE.withA(alpha));
		// 	shader.setUniformSampler("uFontAtlas", fontTexture);
		// 	shader.setStorageBuffer("bBatchingInfos", null);

		// 	ssboBuilder.end();
		// 	glyphBuilder.end().draw(shader, drawState);

		// }

		// final var scale = 1f;
		// final var mat = Matrix4f.createScaleMatrix(scale, -scale, scale);
		// this.cachedMatrix.multiply(mat);

		// for (final var entry : this.builtGlyphs.entries().iterable()) {
		// final var fontTexture = GlTexture2d.importTexture(entry.key);
		// // fontTexture.setMinFilter(GlTexture.MinFilter.NEAREST);
		// // fontTexture.setMagFilter(GlTexture.MagFilter.NEAREST);

		// final var shader = UltravioletShaders.SHADER_TEXT.get();
		// shader.setupDefaultShaderUniforms();
		// shader.setUniformf("uColor", ColorRgba.WHITE.withA(alpha));
		// shader.setUniformSampler("uFontAtlas", fontTexture);
		// shader.setStorageBuffer("bBatchingInfos", null);

		// final var glyphBuilder = builder.beginGeneric(PrimitiveType.QUAD_DUPLICATED,
		// BufferLayout.POSITION_COLOR_TEX_LIGHTMAP);
		// // final var consumer = glyphBuilder.asVanilla();
		// final var consumer = VertexAttributeConsumer.asVanilla(glyphBuilder);
		// for (final var glyph : entry.getOrThrow().iterable()) {
		// glyph.draw(this.cachedMatrix, consumer);
		// }
		// glyphBuilder.end().draw(shader, drawState);
		// }

		// this.builtGlyphs.clear();
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
				this.batchId.setInt(this.builder.currentOffset(), 0, batchId);
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
				this.modelMatrix.setFloats(this.builder.currentOffset(), matrix);
			}
			return this;
		}

		public TextBatchSsboDispatcher color(float r, float g, float b, float a) {
			if (this.color != null) {
				this.color.setFloats(this.builder.currentOffset(), r, g, b, a);
			}
			return this;
		}

		public TextBatchSsboDispatcher color(ColorRgba color) {
			return color(color.r, color.g, color.b, color.a);
		}

	}

}