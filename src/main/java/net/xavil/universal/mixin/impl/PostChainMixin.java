package net.xavil.universal.mixin.impl;

import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

import org.lwjgl.opengl.GL31;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.google.gson.JsonElement;
import com.mojang.blaze3d.pipeline.RenderTarget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.server.ChainedJsonException;
import net.minecraft.util.GsonHelper;
import net.xavil.universal.client.SkyRenderer;
import net.xavil.universal.client.flexible.FlexibleRenderTarget;

@Mixin(PostChain.class)
public abstract class PostChainMixin {

	// @formatter:off
	@Shadow @Final private Map<String, RenderTarget> customRenderTargets;
    @Shadow @Final private List<RenderTarget> fullSizedTargets;
	@Shadow private int screenWidth;
    @Shadow private int screenHeight;
	// @formatter:on

	// @Inject(method = "getRenderTarget", at = @At("RETURN"), cancellable = true)
	// private void addSkyTarget(String target, CallbackInfoReturnable<RenderTarget> info) {
	// 	if (target.equals("universal:sky")) {
	// 		info.setReturnValue(SkyRenderer.INSTANCE.skyTarget);
	// 	}
	// }

	private void addManagedTargetWithFormat(String name, int width, int height, FlexibleRenderTarget.FormatPair formatPair) {
		final var renderTarget = new FlexibleRenderTarget(width, height, formatPair);
		renderTarget.setClearColor(0f, 0f, 0f, 0f);
		this.customRenderTargets.put(name, renderTarget);
		if (width == this.screenWidth && height == this.screenHeight) {
			this.fullSizedTargets.add(renderTarget);
		}
	}

	private int convertFormatName(String name) {
		return switch (name) {
			// 8 bits per channel
			case "r8snorm" -> GL31.GL_R8_SNORM;
			case "rg8snorm" -> GL31.GL_RG8_SNORM;
			case "rgb8snorm" -> GL31.GL_RGB8_SNORM;
			case "rgba8snorm" -> GL31.GL_RGBA8_SNORM;
			case "r8" -> GL31.GL_R8;
			case "rg8" -> GL31.GL_RG8;
			case "rgb8" -> GL31.GL_RGB8;
			case "rgba8" -> GL31.GL_RGBA8;
			// 16 bits per channel
			case "r16snorm" -> GL31.GL_R16_SNORM;
			case "rg16snorm" -> GL31.GL_RG16_SNORM;
			case "rgb16snorm" -> GL31.GL_RGB16_SNORM;
			case "rgba16snorm" -> GL31.GL_RGBA16_SNORM;
			case "r16" -> GL31.GL_R16;
			case "rg16" -> GL31.GL_RG16;
			case "rgb16" -> GL31.GL_RGB16;
			case "rgba16" -> GL31.GL_RGBA16;
			case "r16i" -> GL31.GL_R16I;
			case "rg16i" -> GL31.GL_RG16I;
			case "rgb16i" -> GL31.GL_RGB16I;
			case "rgba16i" -> GL31.GL_RGBA16I;
			case "r16ui" -> GL31.GL_R16UI;
			case "rg16ui" -> GL31.GL_RG16UI;
			case "rgb16ui" -> GL31.GL_RGB16UI;
			case "rgba16ui" -> GL31.GL_RGBA16UI;
			case "r16f" -> GL31.GL_R16F;
			case "rg16f" -> GL31.GL_RG16F;
			case "rgb16f" -> GL31.GL_RGB16F;
			case "rgba16f" -> GL31.GL_RGBA16F;
			// 32 bits per channel
			case "r32i" -> GL31.GL_R32I;
			case "rg32i" -> GL31.GL_RG32I;
			case "rgb32i" -> GL31.GL_RGB32I;
			case "rgba32i" -> GL31.GL_RGBA32I;
			case "r32ui" -> GL31.GL_R32UI;
			case "rg32ui" -> GL31.GL_RG32UI;
			case "rgb32ui" -> GL31.GL_RGB32UI;
			case "rgba32ui" -> GL31.GL_RGBA32UI;
			case "r32f" -> GL31.GL_R32F;
			case "rg32f" -> GL31.GL_RG32F;
			case "rgb32f" -> GL31.GL_RGB32F;
			case "rgba32f" -> GL31.GL_RGBA32F;
			// depth-stencil
			case "depth16" -> GL31.GL_DEPTH_COMPONENT16;
			case "depth24" -> GL31.GL_DEPTH_COMPONENT24;
			case "depth32" -> GL31.GL_DEPTH_COMPONENT32F;
			case "depth24stencil8" -> GL31.GL_DEPTH24_STENCIL8;
			case "depth32stencil8" -> GL31.GL_DEPTH32F_STENCIL8;
			// exotic
			case "r11g11b10" -> GL31.GL_R11F_G11F_B10F;
			case "rgb9e5" -> GL31.GL_RGB9_E5;
			case "rgb5a1" -> GL31.GL_RGB5_A1;
			case "rgba4" -> GL31.GL_RGBA4;
			case "rgb10a2" -> GL31.GL_RGB10_A2;
			default -> -1;
		};
	}

	private OptionalInt parseFormat(String str, OptionalInt defaultValue) throws ChainedJsonException {
		if (str.equals("none"))
			return OptionalInt.empty();
		if (str.equals("default"))
			return defaultValue;
		final var format = convertFormatName(str);
		if (format == -1)
			throw new ChainedJsonException(String.format("unknown framebuffer format type %s", str));
		return OptionalInt.of(format);
	}

	private FlexibleRenderTarget.FormatPair parseFormatPair(String str) throws ChainedJsonException {
		int colorFormat0 = GL31.GL_RGBA8;
		var depthFormat = OptionalInt.of(GL31.GL_DEPTH_COMPONENT);
		if (str.contains("+")) {
			var segments = str.split("\\+");
			if (segments.length != 2)
				throw new ChainedJsonException(
						String.format("format string must only have two components (got %d)", segments.length));
			colorFormat0 = parseFormat(segments[0], OptionalInt.of(GL31.GL_RGBA8))
					.orElseThrow(() -> new ChainedJsonException(String.format("a color format must be provided")));
			depthFormat = parseFormat(segments[1], OptionalInt.of(GL31.GL_DEPTH_COMPONENT));
		}
		colorFormat0 = parseFormat(str, OptionalInt.of(GL31.GL_RGBA8))
				.orElseThrow(() -> new ChainedJsonException(String.format("a color format must be provided")));
		return new FlexibleRenderTarget.FormatPair(colorFormat0, depthFormat);
	}

	@Inject(method = "parseTargetNode", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/PostChain;addTempTarget(Ljava/lang/String;II)V", ordinal = 1), cancellable = true)
	private void floatingPointTargetSupport(JsonElement json, CallbackInfo info) throws ChainedJsonException {
		final var obj = GsonHelper.convertToJsonObject(json, "target");

		if (!obj.has("format"))
			return;

		final var formatString = GsonHelper.getAsString(obj, "format");
		final var formatPair = parseFormatPair(formatString);
		if (!formatPair.isDefault()) {
			final var name = GsonHelper.getAsString(obj, "name");
			int width = GsonHelper.getAsInt(obj, "width", this.screenWidth);
			int height = GsonHelper.getAsInt(obj, "height", this.screenHeight);
			addManagedTargetWithFormat(name, width, height, formatPair);
			info.cancel();
		}
	}

}
