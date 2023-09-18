package net.xavil.ultraviolet.client.screen;

import javax.annotation.Nullable;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TranslatableComponent;
import net.xavil.hawklib.client.screen.HawkScreen;
import net.xavil.hawklib.client.screen.HawkScreen3d;
import net.xavil.hawklib.collections.impl.Vector;
import net.xavil.hawklib.Assert;
import net.xavil.hawklib.Units;
import net.xavil.hawklib.client.HawkDrawStates;
import net.xavil.hawklib.client.camera.CachedCamera;
import net.xavil.hawklib.client.camera.CameraConfig;
import net.xavil.hawklib.client.camera.OrbitCamera;
import net.xavil.hawklib.client.camera.OrbitCamera.Cached;
import net.xavil.hawklib.client.flexible.BufferLayout;
import net.xavil.hawklib.client.flexible.BufferRenderer;
import net.xavil.hawklib.client.flexible.PrimitiveType;
import net.xavil.hawklib.client.flexible.VertexBuilder;
import net.xavil.ultraviolet.client.PlanetRenderingContext;
import net.xavil.ultraviolet.client.UltravioletShaders;
import net.xavil.ultraviolet.client.screen.layer.ScreenLayerBackground;
import net.xavil.ultraviolet.client.screen.layer.ScreenLayerGalaxy;
import net.xavil.ultraviolet.client.screen.layer.ScreenLayerGrid;
import net.xavil.ultraviolet.client.screen.layer.ScreenLayerStars;
import net.xavil.ultraviolet.client.screen.layer.ScreenLayerSystem;
import net.xavil.ultraviolet.common.universe.galaxy.Galaxy;
import net.xavil.ultraviolet.common.universe.galaxy.SystemTicket;
import net.xavil.ultraviolet.common.universe.id.SystemId;
import net.xavil.ultraviolet.common.universe.id.SystemNodeId;
import net.xavil.ultraviolet.common.universe.system.StarSystem;
import net.xavil.universegen.system.BinaryCelestialNode;
import net.xavil.universegen.system.CelestialNode;
import net.xavil.universegen.system.UnaryCelestialNode;
import net.xavil.hawklib.math.Color;
import net.xavil.hawklib.math.Quat;
import net.xavil.hawklib.math.matrices.Mat4;
import net.xavil.hawklib.math.matrices.Vec2;
import net.xavil.hawklib.math.matrices.Vec2i;
import net.xavil.hawklib.math.matrices.Vec3;

public class NewSystemMapScreen extends HawkScreen {

	private CachedCamera camera = new CachedCamera();
	public final Galaxy galaxy;
	private final SystemTicket ticket;
	// private CelestialNode systemRoot;

	public boolean showGuides = true;

	public double scale = 3;
	public double scaleMin = 0.3, scaleMax = 30;
	public double scrollMultiplier = 1.2;
	public Vec2 offset = Vec2.ZERO;

	private PlanetRenderingContext renderContext = new PlanetRenderingContext();

	public NewSystemMapScreen(@Nullable Screen previousScreen, Galaxy galaxy, SystemId systemId, StarSystem system) {
		super(new TranslatableComponent("narrator.screen.systemmap"), previousScreen);

		this.galaxy = galaxy;
		this.ticket = galaxy.sectorManager.createSystemTicket(this.disposer, systemId.galaxySector());

		this.layers.push(new ScreenLayerBackground(this, Color.BLACK));
		// this.layers.push(new ScreenLayerGrid(this));
		// this.layers.push(new ScreenLayerGalaxy(this, galaxy, system.pos));
		// this.layers.push(new ScreenLayerStars(this, galaxy, system.pos));
		// this.layers.push(new ScreenLayerSystem(this, galaxy,
		// systemId.galaxySector()));
	}

	public NewSystemMapScreen(@Nullable Screen previousScreen, Galaxy galaxy, SystemNodeId id, StarSystem system) {
		this(previousScreen, galaxy, id.system(), system);
	}

	private static final class LayoutNodeElement {
		public final double nodeOffset;
		public final LayoutNode node;

		public LayoutNodeElement(double nodeOffset, LayoutNode node) {
			this.nodeOffset = nodeOffset;
			this.node = node;
		}
	}

	private static sealed abstract class LayoutNode {
		public double nodeSize;
		public final Vector<LayoutNodeElement> elements = new Vector<>();
		public double rectOffsetUN, rectOffsetUP;
		public double rectOffsetVN, rectOffsetVP;
	}

	private static final class LayoutNodeUnary extends LayoutNode {
		public UnaryCelestialNode celestialNode;
	}

	private static final class LayoutNodeBinary extends LayoutNode {
		public BinaryCelestialNode celestialNode;
		public double baselineVA;
		public LayoutNode nodeA;
		public double baselineVB;
		public LayoutNode nodeB;
	}

	public static final double REFERENCE_RADIUS = Units.km_PER_Rjupiter;
	public static final double MOBILE_DIAGRAM_OFFSET = 0.5;
	public static final double BARYCENTER_MARKER_RADIUS = 0.05;
	public static final double CHILD_PADDING = 0.1;
	public static final double SIBLING_PADDING = 0.1;

	private double nodeRadius(UnaryCelestialNode node) {
		return Math.max(0.025, 2 * node.radius / (node.radius + REFERENCE_RADIUS));
	}

	private void layoutChildren(LayoutNode res, CelestialNode node) {
		for (final var child : node.childNodes.iterable()) {
			final var childLayout = layout(child.node);

			res.rectOffsetUP += SIBLING_PADDING;
			final var posU = res.rectOffsetUP + childLayout.rectOffsetVN;
			res.rectOffsetUP += childLayout.rectOffsetVN + childLayout.rectOffsetVP;

			res.rectOffsetVN = Math.max(res.rectOffsetVN, childLayout.rectOffsetUN);
			res.rectOffsetVP = Math.max(res.rectOffsetVP, childLayout.rectOffsetUP);

			res.elements.push(new LayoutNodeElement(posU, childLayout));
		}
	}

	private LayoutNodeUnary layoutUnary(UnaryCelestialNode node) {
		final var res = new LayoutNodeUnary();
		final var nodeRadius = nodeRadius(node);
		res.rectOffsetUN = res.rectOffsetUP = nodeRadius;
		res.rectOffsetVN = res.rectOffsetVP = nodeRadius;
		res.nodeSize = nodeRadius;
		layoutChildren(res, node);
		return res;
	}

	private LayoutNodeBinary layoutBinary(BinaryCelestialNode node) {
		final var res = new LayoutNodeBinary();
		res.celestialNode = node;

		if (!node.childNodes.isEmpty()) {
			res.rectOffsetUN = res.rectOffsetUP = BARYCENTER_MARKER_RADIUS;
			res.rectOffsetVN = res.rectOffsetVP = BARYCENTER_MARKER_RADIUS;
			res.nodeSize = BARYCENTER_MARKER_RADIUS;
		}
		layoutChildren(res, node);

		res.nodeA = layout(node.inner);
		res.nodeB = layout(node.outer);
		// set binary baselines
		res.baselineVA = res.rectOffsetVN + res.nodeA.rectOffsetVP + SIBLING_PADDING;
		res.baselineVB = res.rectOffsetVP + res.nodeB.rectOffsetVN + SIBLING_PADDING;
		// expand layout rect to include binary children
		res.rectOffsetVN = res.baselineVA + res.nodeA.rectOffsetVN;
		res.rectOffsetVP = res.baselineVB + res.nodeB.rectOffsetVP;
		// allocate space for mobile diagram lines
		res.rectOffsetUN = Math.max(res.rectOffsetUN, res.nodeA.rectOffsetUN);
		res.rectOffsetUN = Math.max(res.rectOffsetUN, res.nodeB.rectOffsetUN);
		res.rectOffsetUN += MOBILE_DIAGRAM_OFFSET;
		res.rectOffsetUP = Math.max(res.rectOffsetUP, res.nodeA.rectOffsetUP);
		res.rectOffsetUP = Math.max(res.rectOffsetUP, res.nodeB.rectOffsetUP);
		return res;
	}

	private LayoutNode layout(CelestialNode node) {
		if (node instanceof UnaryCelestialNode n)
			return layoutUnary(n);
		if (node instanceof BinaryCelestialNode n)
			return layoutBinary(n);
		throw Assert.isUnreachable();
	}

	private Vec2 transpose(Vec2 pos, boolean transpose) {
		return transpose ? new Vec2(pos.y, pos.x) : pos;
	}

	private void renderCelestialNode(RenderContext ctx, VertexBuilder vertexBuilder, Vec2 pos, LayoutNode layout) {
		final var builder = vertexBuilder.beginGeneric(PrimitiveType.QUADS, BufferLayout.POSITION_COLOR);
		final var s = layout.nodeSize;
		builder.vertex(pos.x + s, pos.y - s, 0).color(Color.GREEN).endVertex();
		builder.vertex(pos.x - s, pos.y - s, 0).color(Color.GREEN).endVertex();
		builder.vertex(pos.x - s, pos.y + s, 0).color(Color.MAGENTA).endVertex();
		builder.vertex(pos.x + s, pos.y + s, 0).color(Color.MAGENTA).endVertex();
		final var shader = UltravioletShaders.SHADER_VANILLA_POSITION_COLOR.get();
		builder.end().draw(shader, HawkDrawStates.DRAW_STATE_DIRECT_ALPHA_BLENDING);
	}

	private void renderNode(RenderContext ctx, LayoutNode layout, Vec2 pos, boolean isVertical) {
		// renderCelestialNode(ctx, BufferRenderer.IMMEDIATE_BUILDER, pos, layout);

		if (shouldRenderDebugInfo()) {
			// final var builder = BufferRenderer.IMMEDIATE_BUILDER.beginGeneric(
			// PrimitiveType.QUADS,
			// BufferLayout.POSITION_COLOR);
			// final var s = layout.nodeSize;
			// builder.vertex(pos.x + s, pos.y - s, 0).color(Color.GREEN).endVertex();
			// builder.vertex(pos.x - s, pos.y - s, 0).color(Color.GREEN).endVertex();
			// builder.vertex(pos.x - s, pos.y + s, 0).color(Color.MAGENTA).endVertex();
			// builder.vertex(pos.x + s, pos.y + s, 0).color(Color.MAGENTA).endVertex();
			// final var shader = UltravioletShaders.SHADER_VANILLA_POSITION_COLOR.get();
			// builder.end().draw(shader, HawkDrawStates.DRAW_STATE_DIRECT_ALPHA_BLENDING);
		}

		for (final var elem : layout.elements.iterable()) {
			final var offsetUV = new Vec2(elem.nodeOffset, 0);
			final var elemPos = pos.add(transpose(offsetUV, !isVertical));
			renderNode(ctx, elem.node, elemPos, !isVertical);
		}

		if (layout instanceof LayoutNodeBinary bnode) {
			final var offsetAUV = new Vec2(0, -bnode.baselineVA);
			final var elemPosA = pos.add(transpose(offsetAUV, !isVertical));
			renderNode(ctx, bnode.nodeA, elemPosA, isVertical);

			final var offsetBUV = new Vec2(0, bnode.baselineVB);
			final var elemPosB = pos.add(transpose(offsetBUV, !isVertical));
			renderNode(ctx, bnode.nodeB, elemPosB, isVertical);

			final var builder = BufferRenderer.IMMEDIATE_BUILDER.beginGeneric(
					PrimitiveType.LINES,
					BufferLayout.POSITION_COLOR_NORMAL);
			final var da = layout.rectOffsetUN - bnode.nodeA.rectOffsetUN;
			final var db = layout.rectOffsetUN - bnode.nodeB.rectOffsetUN;
			final var aNNu = new Vec2(-layout.rectOffsetUN, -bnode.baselineVA);
			final var bNNu = new Vec2(-layout.rectOffsetUN, bnode.baselineVB);
			final var aPNu = new Vec2(aNNu.x + da, -bnode.baselineVA);
			final var bPNu = new Vec2(bNNu.x + db, bnode.baselineVB);
			final var aNN = pos.add(transpose(aNNu, !isVertical)).xy0();
			final var bNN = pos.add(transpose(bNNu, !isVertical)).xy0();
			final var aPN = pos.add(transpose(aPNu, !isVertical)).xy0();
			final var bPN = pos.add(transpose(bPNu, !isVertical)).xy0();
			RenderHelper.addLine(builder, aNN, bNN, Color.WHITE);
			RenderHelper.addLine(builder, aNN, aPN, Color.WHITE);
			RenderHelper.addLine(builder, bNN, bPN, Color.WHITE);
			RenderSystem.lineWidth(1f);
			builder.end().draw(
					UltravioletShaders.SHADER_VANILLA_RENDERTYPE_LINES.get(),
					HawkDrawStates.DRAW_STATE_LINES);	
		}

		if (shouldRenderDebugInfo()) {
			final var builder = BufferRenderer.IMMEDIATE_BUILDER.beginGeneric(
					PrimitiveType.LINES,
					BufferLayout.POSITION_COLOR_NORMAL);
			final var s = layout.nodeSize;
			// @formatter:off
			final var sNN = pos.add(transpose(new Vec2(-s, -s), !isVertical)).xy0();
			final var sNP = pos.add(transpose(new Vec2(-s, s), !isVertical)).xy0();
			final var sPN = pos.add(transpose(new Vec2(s, -s), !isVertical)).xy0();
			final var sPP = pos.add(transpose(new Vec2(s, s), !isVertical)).xy0();
			RenderHelper.addLine(builder, sNN, sNP, Color.GREEN);
			RenderHelper.addLine(builder, sNP, sPP, Color.GREEN);
			RenderHelper.addLine(builder, sPP, sPN, Color.GREEN);
			RenderHelper.addLine(builder, sPN, sNN, Color.GREEN);
			final var rNN = pos.add(transpose(new Vec2(-layout.rectOffsetUN, -layout.rectOffsetVN), !isVertical)).xy0();
			final var rNP = pos.add(transpose(new Vec2(-layout.rectOffsetUN, layout.rectOffsetVP), !isVertical)).xy0();
			final var rPN = pos.add(transpose(new Vec2(layout.rectOffsetUP, -layout.rectOffsetVN), !isVertical)).xy0();
			final var rPP = pos.add(transpose(new Vec2(layout.rectOffsetUP, layout.rectOffsetVP), !isVertical)).xy0();
			// RenderHelper.addLine(builder, rNN, rNP, Color.MAGENTA);
			// RenderHelper.addLine(builder, rNP, rPP, Color.MAGENTA);
			// RenderHelper.addLine(builder, rPP, rPN, Color.MAGENTA);
			// RenderHelper.addLine(builder, rPN, rNN, Color.MAGENTA);
			// @formatter:on
			RenderSystem.lineWidth(1f);
			builder.end().draw(
					UltravioletShaders.SHADER_VANILLA_RENDERTYPE_LINES.get(),
					HawkDrawStates.DRAW_STATE_LINES);
		}

	}

	private boolean shouldRenderDebugInfo() {
		return true;
	}

	@Override
	public void renderScreenPostLayers(RenderContext ctx) {
		final var system = this.galaxy.getSystem(this.ticket.id).unwrapOrNull();
		if (system == null)
			return;

		final var rootLayout = layout(system.rootNode);

		final var rootWidth = rootLayout.rectOffsetUN + rootLayout.rectOffsetUP;
		final var offset = rootWidth / 2 - rootLayout.rectOffsetUN;

		final var window = Minecraft.getInstance().getWindow();
		final var aspectRatio = (float) window.getWidth() / (float) window.getHeight();
		// final var proj = Mat4.perspectiveProjection(Math.toRadians(90), aspectRatio,
		// 0.01, 100.0);
		final double k = this.scale;
		final var proj = Mat4.orthographicProjection(aspectRatio * -k, aspectRatio * k, -k, k, -1, 1);
		// final var proj = Mat4.orthographicProjection(
		// -aspectRatio * rootLayout.rectOffsetUN, aspectRatio *
		// rootLayout.rectOffsetUP,
		// -rootLayout.rectOffsetVN, rootLayout.rectOffsetVP,
		// -1, 1);

		// final var pos = new Vec3(0, 0, 2);
		// // final var orientation = Quat.axisAngle(Vec3.XP, Math.PI / 2);
		// final var orientation = Quat.IDENTITY;
		final var viewMat = new Mat4.Mutable();
		// this.camera.load(pos, orientation, proj, 1);
		this.camera.load(viewMat, proj, 1);

		ctx.currentTexture.framebuffer.bind();
		final var snapshot = this.camera.setupRenderMatrices();
		renderNode(ctx, rootLayout, this.offset.neg().add(-offset, 0), true);

		snapshot.restore();
	}

	@Override
	public boolean mouseScrolled(Vec2 mousePos, double scrollDelta) {
		if (scrollDelta > 0) {
			this.scale = Math.max(this.scale / scrollMultiplier, this.scaleMin);
			return true;
		} else if (scrollDelta < 0) {
			this.scale = Math.min(this.scale * scrollMultiplier, this.scaleMax);
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseDragged(Vec2 mousePos, Vec2 delta, int button) {
		// this.setDragging(true);
		this.offset = this.offset.add(delta.neg().mul(0.01 * this.scale));
		return true;
	}

}
