package net.xavil.ultraviolet.client.screen.layer;

import net.xavil.hawklib.client.screen.HawkScreen3d;
import net.xavil.hawklib.math.Color;
import net.xavil.hawklib.math.matrices.Vec3;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;

import net.minecraft.util.Mth;
import net.xavil.hawklib.client.HawkShaders;
import net.xavil.hawklib.client.camera.CameraConfig;
import net.xavil.hawklib.client.camera.OrbitCamera;
import net.xavil.hawklib.client.flexible.BufferRenderer;
import net.xavil.hawklib.client.flexible.FlexibleVertexConsumer;
import net.xavil.hawklib.client.flexible.PrimitiveType;
import net.xavil.hawklib.client.flexible.VertexBuilder;
import net.xavil.hawklib.client.gl.DrawState;
import net.xavil.hawklib.client.gl.GlState;
import net.xavil.ultraviolet.client.screen.RenderHelper;
import net.xavil.ultraviolet.debug.ClientConfig;
import net.xavil.ultraviolet.debug.ConfigKey;

public class ScreenLayerGrid extends HawkScreen3d.Layer3d {

	public ScreenLayerGrid(HawkScreen3d attachedScreen) {
		super(attachedScreen, new CameraConfig(0.01, true, 1e6, true));
	}

	public static double getGridScale(OrbitCamera.Cached camera, double tmPerUnit, double scaleFactor,
			float partialTick) {
		var currentThreshold = tmPerUnit / Math.pow(scaleFactor, 3);
		var scale = currentThreshold;
		for (var i = 0; i < 100; ++i) {
			currentThreshold *= scaleFactor;
			if (camera.uncached.scale.get(partialTick) > currentThreshold)
				scale = currentThreshold;
		}
		return scale;
	}

	public static void renderGrid(VertexBuilder builder,
			OrbitCamera.Cached camera, OrbitCamera.Cached cullingCamera,
			double tmPerUnit, double gridUnits, int scaleFactor, int gridLineCount, float partialTick) {
		var focusPos = camera.focus.div(tmPerUnit);
		var gridScale = getGridScale(camera, gridUnits, scaleFactor, partialTick);
		renderGrid(builder, camera, cullingCamera, focusPos, gridScale * gridLineCount, scaleFactor, gridLineCount);
	}

	public static final DrawState GRID_STATE = DrawState.builder()
			.depthMask(false)
			.enableCulling(false)
			.enableAdditiveBlending()
			.enableDepthTest(GlState.DepthFunc.LESS)
			.build();

	public static void renderGrid(VertexBuilder builder,
			OrbitCamera.Cached camera, OrbitCamera.Cached cullingCamera,
			Vec3 focusPos,
			double gridDiameter, int subcellsPerCell, int gridLineCount) {
		builder.begin(PrimitiveType.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
		addGrid(builder, camera, cullingCamera, focusPos, gridDiameter, subcellsPerCell, gridLineCount);
		RenderSystem.lineWidth(2);
		builder.end().draw(HawkShaders.getVanillaShader(HawkShaders.SHADER_VANILLA_RENDERTYPE_LINES), GRID_STATE);
	}

	private static void addGridSegment(FlexibleVertexConsumer builder,
			OrbitCamera.Cached camera, OrbitCamera.Cached cullingCamera,
			int maxDepth, Color color,
			Vec3 startPos, Vec3 endPos) {

		// final var camPos = cullingCamera.pos.mul(camera.metersPerUnit / 1e12);

		var midpointSegment = startPos.div(2).add(endPos.div(2));
		var segmentLength = startPos.distanceTo(endPos);
		// var divisionFactor = 1;
		// var insideDivisionRadius = camPos.distanceTo(midpointSegment) <
		// divisionFactor * segmentLength;
		var insideDivisionRadius = midpointSegment.length() < segmentLength / 2.0;

		var subvisions = ClientConfig.get(ConfigKey.GRID_LINE_SUBDIVISIONS);
		subvisions = Mth.clamp(subvisions, 1, 100);

		if (maxDepth > 0 && insideDivisionRadius) {
			for (int i = 0; i < subvisions; ++i) {
				final double ds = i / (double) subvisions;
				final double de = (i + 1) / (double) subvisions;
				final var start = Vec3.lerp(ds, startPos, endPos);
				final var end = Vec3.lerp(de, startPos, endPos);
				addGridSegment(builder, camera, cullingCamera, maxDepth - 1, color, start, end);
			}
		} else {
			if (ClientConfig.get(ConfigKey.SHOW_LINE_LODS)) {
				color = ClientConfig.getDebugColor(maxDepth);
			}
			RenderHelper.addLine(builder, startPos, endPos, color);
		}
	}

	private static void addSubdividedLine(FlexibleVertexConsumer builder,
			OrbitCamera.Cached camera, OrbitCamera.Cached cullingCamera,
			Color color, Vec3 startPos, Vec3 endPos) {
		addGridSegment(builder, camera, cullingCamera, 20, color, startPos, endPos);
	}

	public static void addGrid(FlexibleVertexConsumer builder,
			OrbitCamera.Cached camera, OrbitCamera.Cached cullingCamera,
			Vec3 focusPos,
			double gridDiameter, int subcellsPerCell, int gridLineCount) {

		var gridCellResolution = gridDiameter / gridLineCount;

		var gridMinX = gridCellResolution * Math.floor(focusPos.x / gridCellResolution);
		var gridMinZ = gridCellResolution * Math.floor(focusPos.z / gridCellResolution);

		float r = 0.5f, g = 0.5f, b = 0.5f, a1 = 0.1f, a2 = 0.33f;
		var color = new Color(r, g, b, 0.1f);
		final double gridFadeFactor = 2.3;

		var gridOffset = gridCellResolution * gridLineCount / 2;

		// NOTE: each line needs to be divided into sections, because the lines will
		// become distorted if they are too long.
		// X
		for (var i = 1; i < gridLineCount; ++i) {
			var z = gridMinZ + i * gridCellResolution - gridOffset;
			double lx = gridMinX - gridOffset, hx = lx + gridDiameter;

			var zMark = (int) Math.floor(gridMinZ / gridCellResolution + i - gridLineCount / 2);
			var la = zMark % subcellsPerCell == 0 ? a2 : a1;

			for (var j = 0; j < gridLineCount; ++j) {
				var lt = j / (double) gridLineCount;
				var ht = (j + 1) / (double) gridLineCount;
				var lp = new Vec3(Mth.lerp(lt, lx, hx), focusPos.y, z);
				// var hp = new Vec3(Mth.lerp(ht, lx, hx), focusPos.y, z);

				var ld = lp.distanceTo(focusPos);
				// var hd = hp.distanceTo(focusPos);
				if (ld <= gridDiameter / gridFadeFactor) {
					// var rla = la * Mth.clamp(5 * Mth.inverseLerp(ld, gridDiameter /
					// gridFadeFactor, 0), 0, 1);
					// var rha = la * Mth.clamp(5 * Mth.inverseLerp(hd, gridDiameter /
					// gridFadeFactor, 0), 0, 1);
					var start = camera.toCameraSpace(new Vec3(Mth.lerp(lt, lx, hx), focusPos.y, z));
					var end = camera.toCameraSpace(new Vec3(Mth.lerp(ht, lx, hx), focusPos.y, z));
					// RenderHelper.addLine(builder, start, end, color.withA(rla),
					// color.withA(rha));
					// addSubdividedLine(builder, start, end, color.withA(rla), color.withA(rha));
					addSubdividedLine(builder, camera, cullingCamera, color.withA(la), start, end);
				}
			}
		}
		// Z
		for (var i = 1; i < gridLineCount; ++i) {
			var x = gridMinX + i * gridCellResolution - gridOffset;
			double lz = gridMinZ - gridOffset, hz = lz + gridDiameter;

			var xMark = (int) Math.floor(gridMinX / gridCellResolution + i - gridLineCount / 2);
			var la = xMark % subcellsPerCell == 0 ? a2 : a1;

			for (var j = 0; j < gridLineCount; ++j) {
				var lt = j / (double) gridLineCount;
				var ht = (j + 1) / (double) gridLineCount;
				var lp = new Vec3(x, focusPos.y, Mth.lerp(lt, lz, hz));
				// var hp = new Vec3(x, focusPos.y, Mth.lerp(ht, lz, hz));

				var ld = lp.distanceTo(focusPos);
				// var hd = hp.distanceTo(focusPos);
				if (ld <= gridDiameter / gridFadeFactor) {
					// var rla = la * Mth.clamp(5 * Mth.inverseLerp(ld, gridDiameter /
					// gridFadeFactor, 0), 0, 1);
					// var rha = la * Mth.clamp(5 * Mth.inverseLerp(hd, gridDiameter /
					// gridFadeFactor, 0), 0, 1);
					var start = camera.toCameraSpace(new Vec3(x, focusPos.y, Mth.lerp(lt, lz, hz)));
					var end = camera.toCameraSpace(new Vec3(x, focusPos.y, Mth.lerp(ht, lz, hz)));
					// RenderHelper.addLine(builder, start, end, color.withA(rla),
					// color.withA(rha));
					addSubdividedLine(builder, camera, cullingCamera, color.withA(la), start, end);
				}
			}
		}

	}

	@Override
	public void render3d(OrbitCamera.Cached camera, float partialTick) {
		final var cullingCamera = getCullingCamera();
		final var builder = BufferRenderer.IMMEDIATE_BUILDER;

		// TODO: configurable grid
		renderGrid(builder, camera, cullingCamera, camera.metersPerUnit / 1e12, 1, 10, 40, partialTick);
	}

}
