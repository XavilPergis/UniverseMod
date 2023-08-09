package net.xavil.ultraviolet.client.screen.layer;

import javax.annotation.Nonnull;

import com.mojang.blaze3d.vertex.PoseStack;

import net.xavil.ultraviolet.client.screen.BlackboardKeys;
import net.xavil.ultraviolet.common.universe.galaxy.Galaxy;
import net.xavil.ultraviolet.common.universe.galaxy.SystemTicket;
import net.xavil.ultraviolet.common.universe.id.GalaxySectorId;
import net.xavil.universegen.system.PlanetaryCelestialNode;
import net.xavil.universegen.system.StellarCelestialNode;
import net.xavil.hawklib.Units;
import net.xavil.hawklib.client.gl.GlManager;
import net.xavil.hawklib.client.screen.HawkScreen;
import net.xavil.hawklib.math.matrices.Vec2i;

public class ScreenLayerSystemInfo extends HawkScreen.Layer2d {

	private final SystemTicket selectedSystemTicket;

	public ScreenLayerSystemInfo(HawkScreen attachedScreen, Galaxy galaxy) {
		super(attachedScreen);
		this.selectedSystemTicket = galaxy.sectorManager.createSystemTicket(disposer, null);
	}

	private void drawInfo(PoseStack poseStack, @Nonnull GalaxySectorId selected) {

		final var sectorPos = selected.levelCoords();

		// @formatter:off
		var systemId = "";
		if (sectorPos.x < 0) systemId += "M";
		systemId += Math.abs(sectorPos.x) + ".";
		if (sectorPos.y < 0) systemId += "M";
		systemId += Math.abs(sectorPos.y) + ".";
		if (sectorPos.z < 0) systemId += "M";
		systemId += Math.abs(sectorPos.z);
		systemId += ":";
		systemId += selected.level();
		systemId += "#";
		systemId += selected.elementIndex();
		// @formatter:on

		final var holder = new Object() {
			int height = 10;

			void emit(String text, int color) {
				client.font.draw(poseStack, text, 10, this.height, 0xffffffff);
				this.height += client.font.lineHeight;
			}
		};

		holder.emit("System " + systemId, 0xffffffff);
		final var system = this.selectedSystemTicket.get();
		if (system == null) {
			holder.emit("§c[generating]§r", 0xffffffff);
			holder.height += 10;
		} else {
			holder.emit(String.format("§9§l§n%s§r", system.name), 0xffffffff);
			holder.height += 10;

			final var res = new StringBuilder("§l");
			for (final var node : system.rootNode.iterable()) {
				if (node instanceof StellarCelestialNode starNode) {
					res.append(starNode.getSpectralClassification());
					res.append(' ');
				}
			}
			res.append("§r");
			holder.emit(res.toString(), 0xffffffff);
			holder.height += 10;

			for (final var node : system.rootNode.iterable()) {
				if (node instanceof StellarCelestialNode) {
					final var massLine = String.format("- * §9§lMass§r: %.4e Yg (%.2f M☉)", node.massYg, node.massYg / Units.Yg_PER_Msol);
					holder.emit(massLine, 0xffffffff);
				} else if (node instanceof PlanetaryCelestialNode) {
					var massLine = String.format("-   §9§lMass§r: %.4e Yg (%.2f Mⴲ)", node.massYg, node.massYg / Units.Yg_PER_Mearth);
					if (node.massYg > 0.01 * Units.Yg_PER_Msol) {
						massLine += String.format(" (%.2f M☉)", node.massYg / Units.Yg_PER_Msol);
					}
					holder.emit(massLine, 0xffffffff);
				}
			}
		}
	}

	@Override
	public void render(PoseStack poseStack, Vec2i mousePos, float partialTick) {
		// sidebar

		final var selected = getBlackboard(BlackboardKeys.SELECTED_STAR_SYSTEM).unwrapOrNull();
		this.selectedSystemTicket.id = selected;
		if (selected == null)
			return;

		GlManager.pushState();
		drawInfo(poseStack, selected);
		GlManager.popState();
		// GlManager.currentState().invalidate();
	}

}
