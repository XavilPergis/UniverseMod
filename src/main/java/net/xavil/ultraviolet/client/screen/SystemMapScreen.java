package net.xavil.ultraviolet.client.screen;

import javax.annotation.Nullable;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.util.Mth;
import net.xavil.hawklib.Assert;
import net.xavil.hawklib.Units;
import net.xavil.hawklib.client.HawkDrawStates;
import net.xavil.hawklib.client.camera.CachedCamera;
import net.xavil.hawklib.client.camera.MotionSmoother;
import net.xavil.hawklib.client.camera.RenderMatricesSnapshot;
import net.xavil.hawklib.client.flexible.BufferLayout;
import net.xavil.hawklib.client.flexible.BufferRenderer;
import net.xavil.hawklib.client.flexible.PrimitiveType;
import net.xavil.hawklib.client.gl.DrawState;
import net.xavil.hawklib.client.screen.HawkScreen;
import net.xavil.hawklib.collections.impl.Vector;
import net.xavil.hawklib.collections.interfaces.MutableMap;
import net.xavil.hawklib.math.ColorRgba;
import net.xavil.hawklib.math.NumericOps;
import net.xavil.hawklib.math.Quat;
import net.xavil.hawklib.math.Rect;
import net.xavil.hawklib.math.TransformStack;
import net.xavil.hawklib.math.matrices.Mat4;
import net.xavil.hawklib.math.matrices.Vec2;
import net.xavil.hawklib.math.matrices.Vec3;
import net.xavil.hawklib.math.matrices.VecMath;
import net.xavil.ultraviolet.client.GalaxyRenderingContext;
import net.xavil.ultraviolet.client.PlanetRenderingContext;
import net.xavil.ultraviolet.client.StarRenderManager;
import net.xavil.ultraviolet.client.UltravioletShaders;
import net.xavil.ultraviolet.client.screen.layer.ScreenLayerBackground;
import net.xavil.ultraviolet.common.universe.WorldType;
import net.xavil.ultraviolet.common.universe.galaxy.Galaxy;
import net.xavil.ultraviolet.common.universe.galaxy.SectorTicketInfo;
import net.xavil.ultraviolet.common.universe.galaxy.SystemTicket;
import net.xavil.ultraviolet.common.universe.id.SystemId;
import net.xavil.ultraviolet.common.universe.id.SystemNodeId;
import net.xavil.ultraviolet.common.universe.system.BinaryCelestialNode;
import net.xavil.ultraviolet.common.universe.system.CelestialNode;
import net.xavil.ultraviolet.common.universe.system.StarSystem;
import net.xavil.ultraviolet.common.universe.system.UnaryCelestialNode;
// import net.xavil.ultraviolet.common.universe.system.realistic_generator.ProtoplanetaryDisc;
// import net.xavil.ultraviolet.common.universe.system.realistic_generator.RealisticStarSystemGenerator;
import net.xavil.ultraviolet.mixin.accessor.EntityAccessor;
import net.xavil.ultraviolet.networking.c2s.ServerboundStationJumpPacket;
import net.xavil.ultraviolet.networking.c2s.ServerboundTeleportToLocationPacket;

public class SystemMapScreen extends HawkScreen {

	private CachedCamera uiCamera = new CachedCamera();
	private CachedCamera backgroundCamera = new CachedCamera();
	private final GalaxyRenderingContext galaxyRenderer;
	private final StarRenderManager starRenderer;
	public final Galaxy galaxy;
	private final SystemTicket ticket;

	public MotionSmoother<Double> scale = new MotionSmoother<>(0.6, NumericOps.DOUBLE, 3.0);
	public MotionSmoother<Vec2> offset = new MotionSmoother<>(0.6, NumericOps.VEC2, Vec2.ZERO);
	// public double scaleTarget = 3, scale = scaleTarget;
	public double scaleMin = 0.3, scaleMax = 30;
	public double scrollMultiplier = 1.2;

	private double animationTimer = 0.0;
	private int selectedNode = -1;

	private PlanetRenderingContext renderContext = this.disposer.attach(new PlanetRenderingContext());

	public SystemMapScreen(@Nullable Screen previousScreen, Galaxy galaxy, SystemId systemId, StarSystem system) {
		super(new TranslatableComponent("narrator.screen.systemmap"), previousScreen);

		this.galaxy = galaxy;
		this.ticket = galaxy.sectorManager.createSystemTicket(this.disposer, systemId.galaxySector());

		this.layers.push(new ScreenLayerBackground(this, ColorRgba.BLACK));

		this.galaxyRenderer = this.disposer.attach(new GalaxyRenderingContext(galaxy.parameters));
		this.starRenderer = this.disposer.attach(new StarRenderManager(galaxy, SectorTicketInfo.visual(Vec3.ZERO)));
	}

	public SystemMapScreen(@Nullable Screen previousScreen, Galaxy galaxy, SystemNodeId id, StarSystem system) {
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

		public abstract CelestialNode getCelestialNode();
	}

	private static final class LayoutNodeUnary extends LayoutNode {
		public UnaryCelestialNode celestialNode;

		@Override
		public CelestialNode getCelestialNode() {
			return this.celestialNode;
		}
	}

	private static final class LayoutNodeBinary extends LayoutNode {
		public double baselineVA;
		public LayoutNode nodeA;
		public double baselineVB;
		public LayoutNode nodeB;
		public BinaryCelestialNode celestialNode;

		@Override
		public CelestialNode getCelestialNode() {
			return this.celestialNode;
		}
	}

	public static final double REFERENCE_RADIUS = Units.km_PER_Rjupiter;
	public static final double MOBILE_DIAGRAM_OFFSET = 0.5;
	public static final double BARYCENTER_MARKER_RADIUS = 0.1;
	public static final double CHILD_PADDING = 0.1;
	public static final double SIBLING_PADDING = 0.1;

	private double nodeRadius(UnaryCelestialNode node) {
		// return Math.max(0.025, 2 * node.radius / (node.radius + REFERENCE_RADIUS));
		return Math.max(0.025, Math.pow(node.radius / REFERENCE_RADIUS, 1.0 / 2.0));
		// return node.radius / REFERENCE_RADIUS;
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
		res.baselineVA = res.rectOffsetVN + res.nodeA.rectOffsetVP;
		res.baselineVB = res.rectOffsetVP + res.nodeB.rectOffsetVN;
		if (!node.childNodes.isEmpty()) {
			res.baselineVA += SIBLING_PADDING;
			res.baselineVB += SIBLING_PADDING;
		} else {
			res.baselineVA += SIBLING_PADDING / 2;
			res.baselineVB += SIBLING_PADDING / 2;
		}
		// expand layout rect to include binary children
		res.rectOffsetVN = res.baselineVA + res.nodeA.rectOffsetVN;
		res.rectOffsetVP = res.baselineVB + res.nodeB.rectOffsetVP;
		// allocate space for mobile diagram lines

		// res.rectOffsetUN = Math.max(res.rectOffsetUN, res.nodeA.rectOffsetUN);
		// res.rectOffsetUN = Math.max(res.rectOffsetUN, res.nodeB.rectOffsetUN);
		// // res.rectOffsetUN += Math.min(0.2, Math.max(res.nodeA.rectOffsetUN,
		// res.nodeB.rectOffsetUN));

		res.rectOffsetUN = Math.max(res.nodeA.rectOffsetUN, res.nodeB.rectOffsetUN);
		res.rectOffsetUN += Mth.clamp(0.5 * Math.max(res.nodeA.rectOffsetUN, res.nodeB.rectOffsetUN), 0.02, 0.2);

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

	private boolean isSelected(CelestialNode node) {
		return this.selectedNode != -1 && node.id == this.selectedNode;
	}

	private void renderNode(RenderContext ctx, LayoutNode layout, Vec2 pos, boolean isVertical) {
		double currentLineStart = 0;
		final var lineBuilder = BufferRenderer.IMMEDIATE_BUILDER.beginGeneric(
				PrimitiveType.LINE_DUPLICATED,
				BufferLayout.POSITION_COLOR_NORMAL);
		for (final var elem : layout.elements.iterable()) {
			if (elem.node instanceof LayoutNodeBinary) {
				final var startU = new Vec2(currentLineStart, 0);
				final var endU = new Vec2(elem.nodeOffset - elem.node.rectOffsetVN, 0);
				currentLineStart = elem.nodeOffset + elem.node.rectOffsetVP;
				final var start = pos.add(transpose(startU, !isVertical)).withZ(-9);
				final var end = pos.add(transpose(endU, !isVertical)).withZ(-9);
				RenderHelper.addLine(lineBuilder, start, end, ColorRgba.WHITE);
			} else {
				final var startU = new Vec2(currentLineStart, 0);
				final var endU = new Vec2(elem.nodeOffset, 0);
				currentLineStart = elem.nodeOffset;
				final var start = pos.add(transpose(startU, !isVertical)).withZ(-9);
				final var end = pos.add(transpose(endU, !isVertical)).withZ(-9);
				RenderHelper.addLine(lineBuilder, start, end, ColorRgba.WHITE);
			}
		}
		RenderSystem.lineWidth(2f);
		lineBuilder.end().draw(
				UltravioletShaders.SHADER_VANILLA_RENDERTYPE_LINES.get(),
				HawkDrawStates.DRAW_STATE_LINES);

		if (layout instanceof LayoutNodeUnary unaryNode) {
			this.renderContext.setupLight(0,
					pos.xy0().add(new Vec3(-10, -20, -20).mul(layout.nodeSize)),
					new ColorRgba(1f, 1f, 1f, 0.08f));
			this.renderContext.setupLight(1,
					pos.xy0().add(new Vec3(0, 0, -40).mul(layout.nodeSize)),
					new ColorRgba(1f, 1f, 1f, 0.02f));

			final var modelTfm = new TransformStack();
			modelTfm.appendRotation(
					Quat.axisAngle(Vec3.YP, -unaryNode.celestialNode.rotationalRate * this.animationTimer));
			// modelTfm.appendRotation(Quat.axisAngle(Vec3.ZP,
			// unaryNode.celestialNode.obliquityAngle));
			// modelTfm.appendRotation(Quat.axisAngle(Vec3.XP, -Math.PI / 16));
			modelTfm.appendRotation(Quat.axisAngle(Vec3.ZP, Math.PI / 8));
			modelTfm.appendTransform(Mat4.scale(0.9 * layout.nodeSize));
			if (isSelected(layout.getCelestialNode())) {
				modelTfm.appendTransform(Mat4.scale(1.1));
			}
			modelTfm.appendTranslation(pos.xy0());

			final var quadBuilder = BufferRenderer.IMMEDIATE_BUILDER.beginGeneric(
					PrimitiveType.QUAD_DUPLICATED,
					BufferLayout.POSITION_COLOR_TEX);

			final var s = 1.0 * layout.nodeSize;
			final var sNN = pos.add(transpose(new Vec2(-s, -s), !isVertical)).withZ(-0.2);
			final var sNP = pos.add(transpose(new Vec2(-s, s), !isVertical)).withZ(-0.2);
			final var sPN = pos.add(transpose(new Vec2(s, -s), !isVertical)).withZ(-0.2);
			final var sPP = pos.add(transpose(new Vec2(s, s), !isVertical)).withZ(-0.2);

			ColorRgba color = new ColorRgba(0.0f, 0.0f, 0.0f, 0.2f);
			if (isSelected(layout.getCelestialNode())) {
				color = new ColorRgba(0.0f, 0.0f, 0.0f, 0.5f);
			}

			quadBuilder.vertex(sPN).color(color).uv0(1, 0).endVertex();
			quadBuilder.vertex(sNN).color(color.withA(0)).uv0(0, 0).endVertex();
			quadBuilder.vertex(sNP).color(color.withA(0)).uv0(0, 1).endVertex();
			quadBuilder.vertex(sPP).color(color).uv0(1, 1).endVertex();

			quadBuilder.end().draw(UltravioletShaders.SHADER_UI_QUADS.get(), new DrawState.Builder()
					// .enableDepthTest(GlState.DepthFunc.LESS)
					// .enableDepthWrite(true)
					.enableCulling(false)
					.enableAlphaBlending()
					.build());

			this.renderContext.render(BufferRenderer.IMMEDIATE_BUILDER, this.uiCamera, unaryNode.celestialNode,
					modelTfm, false);
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
					PrimitiveType.LINE_DUPLICATED,
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
			RenderHelper.addLine(builder, aNN, bNN, ColorRgba.WHITE);
			RenderHelper.addLine(builder, aNN, aPN, ColorRgba.WHITE);
			RenderHelper.addLine(builder, bNN, bPN, ColorRgba.WHITE);
			if (!bnode.elements.isEmpty()) {
				final var mNN = pos.add(-BARYCENTER_MARKER_RADIUS, -BARYCENTER_MARKER_RADIUS).xy0();
				final var mNP = pos.add(-BARYCENTER_MARKER_RADIUS, BARYCENTER_MARKER_RADIUS).xy0();
				final var mPN = pos.add(BARYCENTER_MARKER_RADIUS, -BARYCENTER_MARKER_RADIUS).xy0();
				final var mPP = pos.add(BARYCENTER_MARKER_RADIUS, BARYCENTER_MARKER_RADIUS).xy0();
				RenderHelper.addLine(builder, mNN, mPP, ColorRgba.WHITE);
				RenderHelper.addLine(builder, mNP, mPN, ColorRgba.WHITE);
			}
			RenderSystem.lineWidth(2f);
			builder.end().draw(
					UltravioletShaders.SHADER_VANILLA_RENDERTYPE_LINES.get(),
					HawkDrawStates.DRAW_STATE_LINES);
		}

		if (shouldRenderDebugInfo()) {
			final var builder = BufferRenderer.IMMEDIATE_BUILDER.beginGeneric(
					PrimitiveType.LINE_DUPLICATED,
					BufferLayout.POSITION_COLOR_NORMAL);
			final var s = layout.nodeSize;
			// @formatter:off
			final var sNN = pos.add(transpose(new Vec2(-s, -s), !isVertical)).xy0();
			final var sNP = pos.add(transpose(new Vec2(-s, s), !isVertical)).xy0();
			final var sPN = pos.add(transpose(new Vec2(s, -s), !isVertical)).xy0();
			final var sPP = pos.add(transpose(new Vec2(s, s), !isVertical)).xy0();
			RenderHelper.addLine(builder, sNN, sNP, ColorRgba.GREEN);
			RenderHelper.addLine(builder, sNP, sPP, ColorRgba.GREEN);
			RenderHelper.addLine(builder, sPP, sPN, ColorRgba.GREEN);
			RenderHelper.addLine(builder, sPN, sNN, ColorRgba.GREEN);
			final var rNN = pos.add(transpose(new Vec2(-layout.rectOffsetUN, -layout.rectOffsetVN), !isVertical)).xy0();
			final var rNP = pos.add(transpose(new Vec2(-layout.rectOffsetUN, layout.rectOffsetVP), !isVertical)).xy0();
			final var rPN = pos.add(transpose(new Vec2(layout.rectOffsetUP, -layout.rectOffsetVN), !isVertical)).xy0();
			final var rPP = pos.add(transpose(new Vec2(layout.rectOffsetUP, layout.rectOffsetVP), !isVertical)).xy0();
			RenderHelper.addLine(builder, rNN, rNP, ColorRgba.MAGENTA);
			RenderHelper.addLine(builder, rNP, rPP, ColorRgba.MAGENTA);
			RenderHelper.addLine(builder, rPP, rPN, ColorRgba.MAGENTA);
			RenderHelper.addLine(builder, rPN, rNN, ColorRgba.MAGENTA);
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

	private void renderBackground(StarSystem system, RenderContext ctx) {
		final var up = new Vec3(1.0, 2.0, 0.0).normalize();

		if (Vec3.ZERO.equals(system.pos)) {
			Mat4.setLookAt(this.backgroundCamera.viewMatrix, Vec3.ZERO, Vec3.ZP, up);
		} else {
			Mat4.setLookAt(this.backgroundCamera.viewMatrix, Vec3.ZERO, system.pos, up);
		}

		this.backgroundCamera.metersPerUnit = 1e12;

		final var interpolatedOffset = this.offset.get(ctx.partialTick);
		final var offset = new Vec3(100 * interpolatedOffset.x, -100 * interpolatedOffset.y, 0);
		Mat4.mulTranslation(this.backgroundCamera.viewMatrix, this.backgroundCamera.viewMatrix, offset);

		final var window = Minecraft.getInstance().getWindow();
		final var aspect = (float) window.getWidth() / (float) window.getHeight();

		Mat4.setPerspectiveProjection(this.backgroundCamera.projectionMatrix, Math.toRadians(60), aspect, 1e4, 1e12);
		this.backgroundCamera.recalculateCached();

		ctx.currentTexture.framebuffer.bind();

		this.galaxyRenderer.draw(this.backgroundCamera, Vec3.ZERO);
		this.starRenderer.setOriginOffset(Vec3.ZERO);
		this.starRenderer.draw(this.backgroundCamera, this.backgroundCamera.posTm.xyz());

		// it might be cool to blur the background or something
		// or like, do some sort of post processing effect
	}

	private void setupUiCamera(CachedCamera camera, LayoutNode rootLayout, float partialTick) {

		// TODO: only apply offset when first loading this screen, instead of
		// calculating it every time (might look weird if layout changes)
		final var rootWidth = rootLayout.rectOffsetUN + rootLayout.rectOffsetUP;
		final var offset = rootWidth / 2 - rootLayout.rectOffsetUN;

		// setup camera for ortho UI
		final var window = Minecraft.getInstance().getWindow();
		final var aspectRatio = (float) window.getWidth() / (float) window.getHeight();

		final double frustumDepth = 400;

		final var projMat = new Mat4.Mutable();
		final var projLR = aspectRatio * this.scale.get(partialTick);
		final var projTB = this.scale.get(partialTick);
		Mat4.setOrthographicProjection(projMat, -projLR, projLR, -projTB, projTB, -frustumDepth, 0);

		final var viewMat = new Mat4.Mutable();
		viewMat.loadIdentity();
		viewMat.appendTranslation(this.offset.get(partialTick).add(offset, 0).withZ(-0.5 * frustumDepth));
		Mat4.invert(viewMat, viewMat);

		camera.load(viewMat, projMat, 1);
	}

	@Override
	public void renderScreenPostLayers(RenderContext ctx) {
		this.animationTimer += 10.0 * this.client.getDeltaFrameTime();

		CelestialNode rootNode = null;
		final var system = this.galaxy.getSystem(this.ticket.id).unwrapOrNull();
		if (system == null)
			return;

		renderBackground(system, ctx);

		rootNode = rootNode == null ? system.rootNode : rootNode;
		final var rootLayout = layout(rootNode);

		setupUiCamera(this.uiCamera, rootLayout, ctx.partialTick);

		// draw
		ctx.currentTexture.framebuffer.bind();

		final var snapshot = RenderMatricesSnapshot.capture();
		this.uiCamera.applyProjection();
		this.uiCamera.applyView();

		this.renderContext.begin(0.0);
		renderNode(ctx, rootLayout, Vec2.ZERO, true);
		this.renderContext.end();
		snapshot.restore();
	}

	@Override
	public void tick() {
		super.tick();
		this.scale.tick();
		this.offset.tick();
	}

	private static final class LayoutPositions {
		// node ID to rect
		public final MutableMap<Integer, Rect> nodeRects = MutableMap.hashMap();
	}

	private void computeLayoutPositions(LayoutPositions output, LayoutNode layout, Vec2 pos, boolean isVertical) {
		final var min = pos.sub(Vec2.broadcast(layout.nodeSize));
		final var max = pos.add(Vec2.broadcast(layout.nodeSize));
		final var rect = Rect.fromCorners(min, max);

		output.nodeRects.insert(layout.getCelestialNode().id, rect);

		for (final var elem : layout.elements.iterable()) {
			final var offsetUV = new Vec2(elem.nodeOffset, 0);
			final var elemPos = pos.add(transpose(offsetUV, !isVertical));
			computeLayoutPositions(output, elem.node, elemPos, !isVertical);
		}

		if (layout instanceof LayoutNodeBinary bnode) {
			final var offsetAUV = new Vec2(0, -bnode.baselineVA);
			final var elemPosA = pos.add(transpose(offsetAUV, !isVertical));
			computeLayoutPositions(output, bnode.nodeA, elemPosA, isVertical);

			final var offsetBUV = new Vec2(0, bnode.baselineVB);
			final var elemPosB = pos.add(transpose(offsetBUV, !isVertical));
			computeLayoutPositions(output, bnode.nodeB, elemPosB, isVertical);
		}

	}

	@Override
	public boolean mouseReleased(Vec2 mousePos, int button, boolean wasDragging) {
		if (wasDragging)
			return false;

		CelestialNode rootNode = null;
		final var system = this.galaxy.getSystem(this.ticket.id).unwrapOrNull();
		if (system == null)
			return false;

		final var positions = new LayoutPositions();
		rootNode = rootNode == null ? system.rootNode : rootNode;
		final var rootLayout = layout(rootNode);
		computeLayoutPositions(positions, rootLayout, Vec2.ZERO, true);

		final var window = Minecraft.getInstance().getWindow();
		final var mouseNormX = (2.0 * mousePos.x / window.getGuiScaledWidth()) - 1;
		final var mouseNormY = 4.0 * (window.getGuiScaledHeight() - mousePos.y) / window.getHeight() - 1;
		final var mouseNormalized = new Vec2(mouseNormX, mouseNormY);
		final var mouseWorld = VecMath
				.transformPerspective(this.uiCamera.inverseViewProjectionMatrix, mouseNormalized.xy1(), 1).xy();

		this.selectedNode = -1;
		for (final var entry : positions.nodeRects.entries().iterable()) {
			if (entry.get().unwrap().contains(mouseWorld)) {
				this.selectedNode = entry.key;
			}
		}

		return true;

	}

	@Override
	public boolean mouseClicked(Vec2 mousePos, int button) {
		return false;
	}

	@Override
	public boolean mouseScrolled(Vec2 mousePos, double scrollDelta) {
		if (scrollDelta > 0) {
			this.scale.target = Math.max(this.scale.target / scrollMultiplier, this.scaleMin);
			return true;
		} else if (scrollDelta < 0) {
			this.scale.target = Math.min(this.scale.target * scrollMultiplier, this.scaleMax);
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
		final var sizeXu = 4.0 * aspectRatio * this.scale.current;
		final var sizeYu = 4.0 * this.scale.current;

		final var dx = delta.x * (sizeXu / sizeXp);
		final var dy = delta.y * (sizeYu / sizeYp);

		this.setDragging(true);
		this.offset.target = this.offset.target.add(new Vec2(-dx, -dy));
		return true;
	}

	@Override
	public boolean keyPressed(Keypress keypress) {
		if (keypress.keyCode == GLFW.GLFW_KEY_R) {
			if (this.selectedNode != -1) {
				final var packet = new ServerboundTeleportToLocationPacket();
				final var systemId = new SystemId(this.galaxy.galaxyId, this.ticket.id);
				final var id = new SystemNodeId(systemId, this.selectedNode);
				packet.location = new WorldType.SystemNode(id);
				this.client.player.connection.send(packet);
			}
			return true;
		} else if (keypress.keyCode == GLFW.GLFW_KEY_J) {
			if (this.selectedNode != -1) {
				final var stationId = EntityAccessor.getStation(this.client.player);
				if (stationId == -1)
					return true;
				final var systemId = new SystemId(this.galaxy.galaxyId, this.ticket.id);
				final var id = new SystemNodeId(systemId, this.selectedNode);
				final var packet = new ServerboundStationJumpPacket(stationId, id, false);
				this.client.player.connection.send(packet);
			}
			return true;
		}

		return false;
	}

}
