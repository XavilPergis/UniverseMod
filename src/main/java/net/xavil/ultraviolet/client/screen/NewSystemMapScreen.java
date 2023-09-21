package net.xavil.ultraviolet.client.screen;

import javax.annotation.Nullable;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TranslatableComponent;
import net.xavil.hawklib.client.screen.HawkScreen;
import net.xavil.hawklib.collections.impl.Vector;
import net.xavil.hawklib.Assert;
import net.xavil.hawklib.Units;
import net.xavil.hawklib.client.HawkDrawStates;
import net.xavil.hawklib.client.camera.CachedCamera;
import net.xavil.hawklib.client.camera.RenderMatricesSnapshot;
import net.xavil.hawklib.client.flexible.BufferLayout;
import net.xavil.hawklib.client.flexible.BufferRenderer;
import net.xavil.hawklib.client.flexible.Mesh;
import net.xavil.hawklib.client.flexible.PrimitiveType;
import net.xavil.ultraviolet.client.PlanetRenderingContext;
import net.xavil.ultraviolet.client.UltravioletShaders;
import net.xavil.ultraviolet.client.screen.layer.ScreenLayerBackground;
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
import net.xavil.hawklib.math.TransformStack;
import net.xavil.hawklib.math.matrices.Mat4;
import net.xavil.hawklib.math.matrices.Vec2;
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

	private PlanetRenderingContext renderContext = this.disposer.attach(new PlanetRenderingContext());

	private final Mesh sphereMesh = this.disposer.attach(new Mesh());

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

		final var builder = BufferRenderer.IMMEDIATE_BUILDER.beginGeneric(PrimitiveType.QUADS,
				BufferLayout.POSITION_TEX_COLOR_NORMAL);
		RenderHelper.addCubeSphere(builder, Vec3.ZERO, 1, 10);
		this.sphereMesh.upload(builder.end());
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
		public double baselineVA;
		public LayoutNode nodeA;
		public double baselineVB;
		public LayoutNode nodeB;
	}

	public static final double REFERENCE_RADIUS = Units.km_PER_Rjupiter;
	public static final double MOBILE_DIAGRAM_OFFSET = 0.5;
	public static final double BARYCENTER_MARKER_RADIUS = 0.1;
	public static final double CHILD_PADDING = 0.1;
	public static final double SIBLING_PADDING = 0.1;

	private double nodeRadius(UnaryCelestialNode node) {
		// return Math.max(0.025, 2 * node.radius / (node.radius + REFERENCE_RADIUS));
		return Math.max(0.025, Math.pow(node.radius / REFERENCE_RADIUS, 1.0 / 2.0));
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
		res.celestialNode = node;
		final var nodeRadius = nodeRadius(node);
		res.rectOffsetUN = res.rectOffsetUP = nodeRadius;
		res.rectOffsetVN = res.rectOffsetVP = nodeRadius;
		res.nodeSize = nodeRadius;
		layoutChildren(res, node);
		return res;
	}

	private LayoutNodeBinary layoutBinary(BinaryCelestialNode node) {
		final var res = new LayoutNodeBinary();

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

	private void renderNode(RenderContext ctx, LayoutNode layout, Vec2 pos, boolean isVertical) {
		double currentLineStart = 0;
		final var lineBuilder = BufferRenderer.IMMEDIATE_BUILDER.beginGeneric(
				PrimitiveType.LINES,
				BufferLayout.POSITION_COLOR_NORMAL);
		for (final var elem : layout.elements.iterable()) {
			if (elem.node instanceof LayoutNodeBinary) {
				final var startU = new Vec2(currentLineStart, 0);
				final var endU = new Vec2(elem.nodeOffset - elem.node.rectOffsetUN, 0);
				currentLineStart = elem.nodeOffset + elem.node.rectOffsetUP;
				final var start = pos.add(transpose(startU, !isVertical)).withZ(-9);
				final var end = pos.add(transpose(endU, !isVertical)).withZ(-9);
				RenderHelper.addLine(lineBuilder, start, end, Color.WHITE);
			} else {
				final var startU = new Vec2(currentLineStart, 0);
				final var endU = new Vec2(elem.nodeOffset, 0);
				currentLineStart = elem.nodeOffset;
				final var start = pos.add(transpose(startU, !isVertical)).withZ(-9);
				final var end = pos.add(transpose(endU, !isVertical)).withZ(-9);
				RenderHelper.addLine(lineBuilder, start, end, Color.WHITE);
			}
		}
		RenderSystem.lineWidth(2f);
		lineBuilder.end().draw(
				UltravioletShaders.SHADER_VANILLA_RENDERTYPE_LINES.get(),
				HawkDrawStates.DRAW_STATE_LINES);

		if (layout instanceof LayoutNodeUnary unaryNode) {
			this.renderContext.setupLight(0,
					pos.xy0().add(new Vec3(-10, -20, -20).mul(layout.nodeSize)),
					new Color(1f, 1f, 1f, 0.08f));
			this.renderContext.setupLight(1,
					pos.xy0().add(new Vec3(0, 0, -40).mul(layout.nodeSize)),
					new Color(1f, 1f, 1f, 0.02f));

			final var modelTfm = new TransformStack();
			// modelTfm.appendRotation(Quat.axisAngle(Vec3.ZP,
			// unaryNode.celestialNode.obliquityAngle));
			modelTfm.appendRotation(Quat.axisAngle(Vec3.XP, Math.PI / 8));
			// modelTfm.appendRotation(Quat.axisAngle(Vec3.YP, -node.rotationalRate *
			// this.celestialTime));
			modelTfm.appendTransform(Mat4.scale(0.9 * layout.nodeSize));
			modelTfm.appendTranslation(pos.xy0());

			this.renderContext.render(BufferRenderer.IMMEDIATE_BUILDER, this.camera, unaryNode.celestialNode, modelTfm,
					false);
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
			if (!bnode.elements.isEmpty()) {
				final var mNN = pos.add(-BARYCENTER_MARKER_RADIUS, -BARYCENTER_MARKER_RADIUS).xy0();
				final var mNP = pos.add(-BARYCENTER_MARKER_RADIUS, BARYCENTER_MARKER_RADIUS).xy0();
				final var mPN = pos.add(BARYCENTER_MARKER_RADIUS, -BARYCENTER_MARKER_RADIUS).xy0();
				final var mPP = pos.add(BARYCENTER_MARKER_RADIUS, BARYCENTER_MARKER_RADIUS).xy0();
				RenderHelper.addLine(builder, mNN, mPP, Color.WHITE);
				RenderHelper.addLine(builder, mNP, mPN, Color.WHITE);
			}
			RenderSystem.lineWidth(2f);
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
			RenderHelper.addLine(builder, rNN, rNP, Color.MAGENTA);
			RenderHelper.addLine(builder, rNP, rPP, Color.MAGENTA);
			RenderHelper.addLine(builder, rPP, rPN, Color.MAGENTA);
			RenderHelper.addLine(builder, rPN, rNN, Color.MAGENTA);
			// @formatter:on
			RenderSystem.lineWidth(1f);
			builder.end().draw(
					UltravioletShaders.SHADER_VANILLA_RENDERTYPE_LINES.get(),
					HawkDrawStates.DRAW_STATE_LINES);
		}

	}

	private boolean shouldRenderDebugInfo() {
		return false;
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

		final double k = this.scale;
		final double l = 200;
		// final var projMat = Mat4.orthographicProjection(aspectRatio * -k, aspectRatio
		// * k, -k, k, -100, 100);
		final var projMat = new Mat4.Mutable();
		// Mat4.setPerspectiveProjection(projMat, Math.toRadians(30 * this.scale), aspectRatio, 0.01, 1000);
		Mat4.setOrthographicProjection(projMat, aspectRatio * -k, aspectRatio * k, -k, k, -2 * l, 0);
		// projMat.prependTranslation(this.offset.neg().add(-offset, 0).xy0());
		final var viewMat = new Mat4.Mutable();
		// FIXME: the fucking view matrix is broken!!! why!! the translation should be
		// applied here instead of in the projection matrix, but it just. refuses to do
		// anything.
		viewMat.loadIdentity();
		viewMat.appendTranslation(this.offset.add(offset, 0).withZ(-l));
		// Mat4.setScale(viewMat, this.scale);
		Mat4.invert(viewMat, viewMat);
		this.camera.load(viewMat, projMat, 1);
		// this.camera.load(this.offset.neg().add(-offset, 0).withZ(-50), Quat.axisAngle(Vec3.YP, Math.PI / 5), projMat.asImmutable(), 1);

		ctx.currentTexture.framebuffer.bind();

		// final var snapshot = this.camera.setupRenderMatrices();

		final var snapshot = RenderMatricesSnapshot.capture();
		RenderSystem.setProjectionMatrix(this.camera.projectionMatrix.asMinecraft());

		final var poseStack = RenderSystem.getModelViewStack();
		poseStack.setIdentity();

		poseStack.mulPoseMatrix(this.camera.viewMatrix.asMinecraft());
		final var inverseViewRotationMatrix = poseStack.last().normal().copy();
		if (inverseViewRotationMatrix.invert()) {
			RenderSystem.setInverseViewRotationMatrix(inverseViewRotationMatrix);
		}

		// it would be very nice for simplicity's sake to apply the camera's translation
		// here, but unfortunately, that can cause stuff to melt into floating point
		// soup at the scales we're dealing with. So instead, vertices are specified in
		// a space where the camera is at the origin (like in view space), but the
		// camera's rotation is not taken into account (like in world space).
		// Essentially a weird hybrid between the two. This is the same behavior as the
		// vanilla camera.
		RenderSystem.applyModelViewMatrix();
		// return snapshot;

		this.renderContext.begin(0.0);
		renderNode(ctx, rootLayout, Vec2.ZERO, true);
		this.renderContext.end();
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
		final var window = Minecraft.getInstance().getWindow();
		final var aspectRatio = (float) window.getWidth() / (float) window.getHeight();

		final var sizeXp = (double) window.getWidth();
		final var sizeYp = (double) window.getHeight();
		final var sizeXu = 4.0 * aspectRatio * this.scale;
		final var sizeYu = 4.0 * this.scale;

		final var dx = delta.x * (sizeXu / sizeXp);
		final var dy = delta.y * (sizeYu / sizeYp);

		// this.setDragging(true);
		this.offset = this.offset.add(new Vec2(-dx, -dy));
		return true;
	}

}
