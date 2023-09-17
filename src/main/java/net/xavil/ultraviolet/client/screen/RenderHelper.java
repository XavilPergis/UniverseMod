package net.xavil.ultraviolet.client.screen;

import net.minecraft.resources.ResourceLocation;
import net.xavil.ultraviolet.Mod;
import net.xavil.hawklib.client.camera.CachedCamera;
import net.xavil.hawklib.client.flexible.FlexibleVertexConsumer;
import net.xavil.hawklib.math.Color;
import net.xavil.hawklib.math.matrices.Vec3;

public final class RenderHelper {

	public static final ResourceLocation STAR_ICON_LOCATION = Mod.namespaced("textures/misc/star_icon.png");
	public static final ResourceLocation GALAXY_GLOW_LOCATION = Mod.namespaced("textures/misc/galaxyglow.png");
	public static final ResourceLocation SELECTION_CIRCLE_ICON_LOCATION = Mod
			.namespaced("textures/misc/selection_circle.png");

	public static void addLine(FlexibleVertexConsumer builder, CachedCamera camera, Vec3 start, Vec3 end,
			Color color) {
		addLine(builder, camera, start, end, color, color);
	}

	public static void addLine(FlexibleVertexConsumer builder, Vec3 start, Vec3 end, Color color) {
		addLine(builder, start, end, color, color);
	}

	public static void addLine(FlexibleVertexConsumer builder, CachedCamera camera, Vec3 start, Vec3 end,
			Color startColor, Color endColor) {
		addLine(builder, camera.toCameraSpace(start), camera.toCameraSpace(end), startColor, endColor);
	}

	public static void addLine(FlexibleVertexConsumer builder, Vec3 start, Vec3 end, Color startColor, Color endColor) {
		var normal = end.sub(start).normalize();
		builder.vertex(start.x, start.y, start.z)
				.color(startColor.r(), startColor.g(), startColor.b(), startColor.a())
				.normal((float) normal.x, (float) normal.y, (float) normal.z)
				.endVertex();
		builder.vertex(end.x, end.y, end.z)
				.color(endColor.r(), endColor.g(), endColor.b(), endColor.a())
				.normal((float) normal.x, (float) normal.y, (float) normal.z)
				.endVertex();
	}

}
