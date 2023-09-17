package net.xavil.ultraviolet.client.screen.layer;

import java.util.Comparator;

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
import net.xavil.hawklib.client.screen.HawkScreen.RenderContext;

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
				String line = node.explicitName != null ? node.explicitName : " §a§o" + system.name + "§r §a§l" + node.suffix + "§r";

				if (node.parentUnaryNode != null) {
					final var semiMajor = node.parentUnaryNode.orbitalShape.semiMajor();
					line += String.format(" §7%.2f au§r", semiMajor / Units.Tm_PER_au);
				}

				if (node instanceof StellarCelestialNode) {
					line += String.format(" §7%.2f M☉§r", node.massYg / Units.Yg_PER_Msol);
				} else if (node instanceof PlanetaryCelestialNode) {
					line += String.format(" §7%.2f Mⴲ§r", node.massYg / Units.Yg_PER_Mearth);
					if (node.massYg > 0.01 * Units.Yg_PER_Msol) {
						line += String.format(" §7(%.2f M☉)§r", node.massYg / Units.Yg_PER_Msol);
					}
				}

				holder.emit(line, 0xffffffff);
			}
		}
	}

	@Override
	public void render(RenderContext ctx) {
		// sidebar

		final var selected = getBlackboard(BlackboardKeys.SELECTED_STAR_SYSTEM).unwrapOrNull();
		this.selectedSystemTicket.id = selected;
		if (selected == null)
			return;

		GlManager.pushState();
		drawInfo(ctx.poseStack, selected);
		GlManager.popState();
		// GlManager.currentState().invalidate();
	}

}
