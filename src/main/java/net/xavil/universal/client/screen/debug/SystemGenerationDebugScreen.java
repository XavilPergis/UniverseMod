package net.xavil.universal.client.screen.debug;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.util.Mth;
import net.xavil.universal.client.screen.OrbitCamera;
import net.xavil.universal.client.screen.OrbitCamera.Cached;
import net.xavil.universal.client.screen.RenderHelper;
import net.xavil.universal.client.screen.Universal3dScreen;
import net.xavil.universal.common.universe.system.gen.AccreteContext;
import net.xavil.universal.common.universe.system.gen.AccreteDebugEvent;
import net.xavil.universal.common.universe.system.gen.DustBands;
import net.xavil.universal.common.universe.system.gen.ProtoplanetaryDisc;
import net.xavil.universal.common.universe.system.gen.SimulationParameters;
import net.xavil.universegen.system.StellarCelestialNode;
import net.xavil.util.Rng;
import net.xavil.util.Units;
import net.xavil.util.math.Color;
import net.xavil.util.math.Interval;
import net.xavil.util.math.Vec3;

public class SystemGenerationDebugScreen extends Universal3dScreen {

	private StellarCelestialNode node;
	private final List<AccreteDebugEvent> events = new ArrayList<>();
	private int currentEvent = -1;

	record PlanetesimalInfo(double distance, double mass, double radius, Interval sweepLimit) {
	}

	private DustBands currentDustBands;
	private Int2ObjectMap<PlanetesimalInfo> planetesimalInfos;

	private static final Color GAS_ONLY_COLOR = new Color(1, 0.8, 0, 1);
	private static final Color DUST_ONLY_COLOR = new Color(0.7, 0, 0.7, 1);
	private static final Color GAS_AND_DUST_COLOR = new Color(0.3, 0.8, 1, 1);

	public SystemGenerationDebugScreen(Screen previousScreen) {
		super(new TranslatableComponent("narrator.screen.debug.system_generation"), previousScreen,
				new OrbitCamera(1e12, 1), 1e-4, 4e3);
	}

	@Override
	public Cached setupCamera(float partialTick) {
		return this.camera.cached(partialTick);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (super.keyPressed(keyCode, scanCode, modifiers))
			return true;

		// if (keyCode == GLFW.GLFW_KEY_R && (modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
		if (keyCode == GLFW.GLFW_KEY_R) {
			generateSystem();
			this.currentEvent = this.events.size() - 1;
			this.currentEvent = Mth.clamp(this.currentEvent, 0, this.events.size() - 1);
			fastForward();
			return true;
		} else if (keyCode == GLFW.GLFW_KEY_COMMA) {
			seek(false, (modifiers & GLFW.GLFW_MOD_CONTROL) != 0, (modifiers & GLFW.GLFW_MOD_SHIFT) != 0);
			return true;
		} else if (keyCode == GLFW.GLFW_KEY_PERIOD) {
			seek(true, (modifiers & GLFW.GLFW_MOD_CONTROL) != 0, (modifiers & GLFW.GLFW_MOD_SHIFT) != 0);
			return true;
		} else if (keyCode == GLFW.GLFW_KEY_HOME) {
			this.currentEvent = 0;
			this.currentEvent = Mth.clamp(this.currentEvent, 0, this.events.size() - 1);
			fastForward();
			return true;
		} else if (keyCode == GLFW.GLFW_KEY_END) {
			this.currentEvent = this.events.size() - 1;
			this.currentEvent = Mth.clamp(this.currentEvent, 0, this.events.size() - 1);
			fastForward();
			return true;
		}

		return false;
	}

	private void fastForward() {
		if (this.currentEvent >= this.events.size())
			return;

		if (this.events.get(0) instanceof AccreteDebugEvent.Initialize event) {
			this.currentDustBands = new DustBands(event.dustBandInterval, AccreteDebugEvent.Consumer.DUMMY);
			this.planetesimalInfos = new Int2ObjectOpenHashMap<>();
		}

		for (var i = 1; i < this.currentEvent; ++i) {
			var eventUntyped = this.events.get(i);
			if (eventUntyped instanceof AccreteDebugEvent.AddPlanetesimal event) {
				this.planetesimalInfos.put(event.id,
						new PlanetesimalInfo(event.distance, event.mass, event.radius, event.sweepInterval));
			} else if (eventUntyped instanceof AccreteDebugEvent.UpdatePlanetesimal event) {
				if (this.planetesimalInfos.containsKey(event.id))
					this.planetesimalInfos.put(event.id,
							new PlanetesimalInfo(event.distance, event.mass, event.radius, event.sweepInterval));
			} else if (eventUntyped instanceof AccreteDebugEvent.CaptureMoon event) {
				this.planetesimalInfos.remove(event.moonId);
			} else if (eventUntyped instanceof AccreteDebugEvent.PlanetesimalCollision event) {
				this.planetesimalInfos.remove(event.collidedId);
			} else if (eventUntyped instanceof AccreteDebugEvent.RemovePlanetesimal event) {
				this.planetesimalInfos.remove(event.id);
			} else if (eventUntyped instanceof AccreteDebugEvent.Sweep event) {
				this.currentDustBands.removeMaterial(event.sweepInterval, event.sweptGas, event.sweptDust);
			}
		}
	}

	private void seek(boolean forwards, BiPredicate<Integer, AccreteDebugEvent> stopPredicate) {
		if (this.events.isEmpty())
			return;
		if (this.currentEvent >= this.events.size()) {
			this.currentEvent = this.events.size() - 1;
		} else if (this.currentEvent < 0) {
			this.currentEvent = 0;
		}
		if (forwards) {
			this.currentEvent += 1;
			for (; this.currentEvent < this.events.size(); ++this.currentEvent) {
				if (stopPredicate.test(this.currentEvent, this.events.get(this.currentEvent)))
					break;
			}
		} else {
			this.currentEvent -= 1;
			for (; this.currentEvent > 0; --this.currentEvent) {
				if (stopPredicate.test(this.currentEvent, this.events.get(this.currentEvent)))
					break;
			}
		}
		this.currentEvent = Mth.clamp(this.currentEvent, 0, this.events.size() - 1);
	}

	private void seek(boolean forwards, boolean control, boolean shift) {
		var prevSelected = this.currentEvent;
		if (control && shift) {
			seek(forwards, (id, event) -> true);
		} else if (control) {
			seek(forwards, (id, event) -> event instanceof AccreteDebugEvent.AddPlanetesimal);
		} else if (shift) {
			seek(forwards, (id, event) -> !isEventVerbose(event));
		} else {
			seek(forwards, (id, event) -> isEventSignificant(event));
		}
		if (prevSelected != this.currentEvent) {
			fastForward();
		}
	}

	private boolean isEventVerbose(AccreteDebugEvent event) {
		return event instanceof AccreteDebugEvent.UpdatePlanetesimal;
	}

	private boolean isEventSignificant(AccreteDebugEvent event) {
		return event instanceof AccreteDebugEvent.Initialize
				|| event instanceof AccreteDebugEvent.AddPlanetesimal
				|| event instanceof AccreteDebugEvent.CaptureMoon
				|| event instanceof AccreteDebugEvent.PlanetesimalCollision
				|| event instanceof AccreteDebugEvent.RemovePlanetesimal;
	}

	private void generateSystem() {
		this.events.clear();
		try {
			var rng = Rng.wrap(new Random());
			var starMass = rng.uniformDouble(Units.fromMsol(0.1), Units.fromMsol(100));
			this.node = StellarCelestialNode.fromMassAndAge(rng, starMass, 4600);
			var params = new SimulationParameters();
			var ctx = new AccreteContext(params, rng, this.node.luminosityLsol,
					this.node.massYg / Units.Yg_PER_Msol, new Interval(0, 1e20),
					AccreteDebugEvent.Consumer.wrap(this.events::add));
			var protoDisc = new ProtoplanetaryDisc(ctx);

			protoDisc.collapseDisc(this.node);
		} catch (Throwable throwable) {
			throwable.printStackTrace();
			this.events.clear();
		}
	}

	interface BandGeometryConsumer {
		void addLine(Vec3 start, Vec3 end, Color color);

		void addQuad(Vec3 innerStart, Vec3 innerEnd, Vec3 outerStart, Vec3 outerEnd, Color color);
	}

	private void addCircle(Cached camera, BandGeometryConsumer consumer, double radius, Color color) {
		int segmentCount = 25;
		for (var i = 0; i < segmentCount; ++i) {
			var angleL = 2 * Math.PI * (i / (double) segmentCount);
			var angleH = 2 * Math.PI * ((i + 1) / (double) segmentCount);
			var llx = radius * Math.cos(angleL);
			var lly = radius * Math.sin(angleL);
			var lhx = radius * Math.cos(angleH);
			var lhy = radius * Math.sin(angleH);
			var ll = Vec3.from(llx, 0, lly);
			var lh = Vec3.from(lhx, 0, lhy);
			consumer.addLine(ll, lh, color);
		}
	}

	private void addCircleInterval(Cached camera, BandGeometryConsumer consumer, Interval interval, Color color,
			double fillAlpha) {
		final var quadColor = color.withA(fillAlpha);
		int segmentCount = 25;
		for (var i = 0; i < segmentCount; ++i) {
			var angleL = 2 * Math.PI * (i / (double) segmentCount);
			var angleH = 2 * Math.PI * ((i + 1) / (double) segmentCount);
			var llx = interval.lower() * Math.cos(angleL);
			var lly = interval.lower() * Math.sin(angleL);
			var lhx = interval.lower() * Math.cos(angleH);
			var lhy = interval.lower() * Math.sin(angleH);
			var hlx = interval.higher() * Math.cos(angleL);
			var hly = interval.higher() * Math.sin(angleL);
			var hhx = interval.higher() * Math.cos(angleH);
			var hhy = interval.higher() * Math.sin(angleH);
			var ll = Vec3.from(llx, 0, lly);
			var lh = Vec3.from(lhx, 0, lhy);
			var hl = Vec3.from(hlx, 0, hly);
			var hh = Vec3.from(hhx, 0, hhy);
			consumer.addLine(ll, lh, color);
			consumer.addLine(hl, hh, color);
			consumer.addQuad(ll, lh, hl, hh, quadColor);
		}
	}

	private void addDustBand(Cached camera, BandGeometryConsumer consumer, DustBands.Band band, float partialTick) {
		var color = Color.WHITE;
		if (band.hasGas() && band.hasDust())
			color = GAS_AND_DUST_COLOR;
		else if (band.hasGas())
			color = GAS_ONLY_COLOR;
		else if (band.hasDust())
			color = DUST_ONLY_COLOR;
		addCircleInterval(camera, consumer, band.interval(), color, 0.4);
	}

	record Line(Vec3 start, Vec3 end, Color color) {
	}

	record Quad(Vec3 innerStart, Vec3 innerEnd, Vec3 outerStart, Vec3 outerEnd, Color color) {
	}

	private void renderDustBands(Cached camera, DustBands dustBands, float partialTick) {

		final var lines = new ArrayList<Line>();
		final var quads = new ArrayList<Quad>();
		final var consumer = new BandGeometryConsumer() {
			@Override
			public void addLine(Vec3 start, Vec3 end, Color color) {
				lines.add(new Line(start, end, color));
			}

			@Override
			public void addQuad(Vec3 innerStart, Vec3 innerEnd, Vec3 outerStart, Vec3 outerEnd, Color color) {
				quads.add(new Quad(innerStart, innerEnd, outerStart, outerEnd, color));
			}
		};
		for (var band : dustBands.bands) {
			addDustBand(camera, consumer, band, partialTick);
		}

		for (var entry : this.planetesimalInfos.int2ObjectEntrySet()) {
			addCircle(camera, consumer, entry.getValue().distance, Color.WHITE);
			addCircleInterval(camera, consumer, entry.getValue().sweepLimit, Color.GREEN, 0.1);
		}

		if (this.currentEvent != -1 && this.currentEvent < this.events.size()) {
			var initEvent = (AccreteDebugEvent.Initialize) this.events.get(0);
			addCircle(camera, consumer, initEvent.planetesimalPlacementInterval.lower(), Color.RED);
			addCircle(camera, consumer, initEvent.planetesimalPlacementInterval.higher(), Color.RED);

			var currentEvent = this.events.get(this.currentEvent);
			if (currentEvent instanceof AccreteDebugEvent.Initialize event) {
			} else if (currentEvent instanceof AccreteDebugEvent.AddPlanetesimal event) {
				addCircle(camera, consumer, event.distance, Color.MAGENTA);
				addCircleInterval(camera, consumer, event.sweepInterval, Color.MAGENTA, 0.2);
			} else if (currentEvent instanceof AccreteDebugEvent.Sweep event) {
				addCircleInterval(camera, consumer, event.sweepInterval, Color.MAGENTA, 0.2);
			} else if (currentEvent instanceof AccreteDebugEvent.CaptureMoon event) {
				var parent = this.planetesimalInfos.get(event.parentId);
				var moon = this.planetesimalInfos.get(event.moonId);
				if (parent != null)
					addCircle(camera, consumer, parent.distance, Color.MAGENTA);
				if (moon != null)
					addCircle(camera, consumer, moon.distance, Color.CYAN);
			} else if (currentEvent instanceof AccreteDebugEvent.PlanetesimalCollision event) {
				var parent = this.planetesimalInfos.get(event.parentId);
				var collided = this.planetesimalInfos.get(event.collidedId);
				if (parent != null)
					addCircle(camera, consumer, parent.distance, Color.MAGENTA);
				if (collided != null)
					addCircle(camera, consumer, collided.distance, Color.CYAN);
			}
		}

		final var builder = Tesselator.getInstance().getBuilder();
		builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		for (var quad : quads) {
			RenderHelper.addQuad(builder, camera, quad.innerEnd, quad.innerStart, quad.outerStart, quad.outerEnd,
					quad.color);
		}
		builder.end();
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
		RenderSystem.depthMask(false);
		RenderSystem.enableDepthTest();
		RenderSystem.disableCull();
		RenderSystem.enableBlend();
		BufferUploader.end(builder);

		builder.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
		for (var line : lines) {
			RenderHelper.addLine(builder, camera, line.start, line.end, line.color);
		}
		builder.end();
		RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);
		RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
		RenderSystem.depthMask(false);
		RenderSystem.enableDepthTest();
		RenderSystem.disableCull();
		RenderSystem.enableBlend();
		BufferUploader.end(builder);
	}

	@Override
	public void render3d(Cached camera, float partialTick) {
		final var builder = Tesselator.getInstance().getBuilder();

		RenderHelper.renderGrid(builder, camera, 1, 1, 10, 40, partialTick);

		if (this.currentDustBands != null)
			renderDustBands(camera, this.currentDustBands, partialTick);
	}

	private void addPlanetesimalLines(BiConsumer<String, String> consumer) {
		if (this.planetesimalInfos == null)
			return;

		consumer.accept("star", String.format("%.2f M☉", this.node.massYg / Units.Yg_PER_Msol));

		var stream = this.planetesimalInfos.int2ObjectEntrySet().stream()
				.sorted(Comparator.comparingInt(entry -> entry.getIntKey()));
		stream.forEach(entry -> {
			final var id = entry.getIntKey();
			final var infoWriter = new StringBuilder();

			var info = entry.getValue();
			infoWriter.append(String.format("%.2f au", info.distance));
			infoWriter.append(" | ");
			infoWriter.append(String.format("%.2f M☉, %.2f M♃, %.2f Mⴲ",
					info.mass,
					info.mass * Units.Yg_PER_Msol / Units.Yg_PER_Mjupiter,
					info.mass * Units.Yg_PER_Msol / Units.Yg_PER_Mearth));

			consumer.accept("" + id, infoWriter.toString());
		});
	}

	@Override
	public void render2d(PoseStack poseStack, float partialTick) {
		if (this.currentEvent != -1 && this.currentEvent < this.events.size()) {
			var currentEvent = this.events.get(this.currentEvent);
			this.client.font.draw(poseStack,
					String.format("(%s/%s) §9§l§n%s§r",
							"" + this.currentEvent,
							"" + (this.events.size() - 1),
							currentEvent.kind()),
					0, 0, 0xff777777);
			var obj = new Object() {
				int currentHeight = client.font.lineHeight;
			};
			currentEvent.addInfoLines((property, value) -> {
				this.client.font.draw(poseStack, "§9" + property + "§r: " + value, 0, obj.currentHeight, 0xff777777);
				obj.currentHeight += this.client.font.lineHeight + 1;
			});
			obj.currentHeight = this.height - (font.lineHeight + 1);
			addPlanetesimalLines((property, value) -> {
				this.client.font.draw(poseStack, "§9" + property + "§r: " + value, 0, obj.currentHeight, 0xff777777);
				obj.currentHeight -= this.client.font.lineHeight + 1;
			});
		}
	}

}
