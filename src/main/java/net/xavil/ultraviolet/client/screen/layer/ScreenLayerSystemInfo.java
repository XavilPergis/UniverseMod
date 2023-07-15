package net.xavil.ultraviolet.client.screen.layer;

import javax.annotation.Nonnull;

import com.mojang.blaze3d.vertex.PoseStack;

import net.xavil.ultraviolet.client.screen.BlackboardKeys;
import net.xavil.ultraviolet.common.universe.galaxy.Galaxy;
import net.xavil.ultraviolet.common.universe.galaxy.SystemTicket;
import net.xavil.ultraviolet.common.universe.id.GalaxySectorId;
import net.xavil.universegen.system.StellarCelestialNode;
import net.xavil.hawklib.client.gl.GlManager;
import net.xavil.hawklib.client.screen.HawkScreen;
import net.xavil.hawklib.math.matrices.Vec2i;

public class ScreenLayerSystemInfo extends HawkScreen.Layer2d {

	private final Galaxy galaxy;
	private final SystemTicket selectedSystemTicket;

	public ScreenLayerSystemInfo(HawkScreen attachedScreen, Galaxy galaxy) {
		super(attachedScreen);
		this.galaxy = galaxy;
		this.selectedSystemTicket = galaxy.sectorManager.createSystemTicket(disposer, null);
	}

	private String describeStar(StellarCelestialNode starNode) {
		String starKind = "";
		var starClass = starNode.starClass();
		if (starClass != null) {
			starKind += "Class " + starClass.name + " ";
		}
		if (starNode.type == StellarCelestialNode.Type.BLACK_HOLE) {
			starKind += "Black Hole ";
		} else if (starNode.type == StellarCelestialNode.Type.NEUTRON_STAR) {
			starKind += "Neutron Star ";
		} else if (starNode.type == StellarCelestialNode.Type.WHITE_DWARF) {
			starKind += "White Dwarf ";
		} else if (starNode.type == StellarCelestialNode.Type.GIANT) {
			starKind += "Giant ";
		}

		return starKind;
	}

	private void drawInfo(PoseStack poseStack, @Nonnull GalaxySectorId selected) {

		// fillGradient(poseStack, 0, 0, 200, this.height, 0xff0a0a0a, 0xff0a0a0a);

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
		this.galaxy.sectorManager.getInitial(selected).ifSome(info -> {
			final var system = info.info();
			holder.emit(String.format("§9§l§n%s§r", system.name), 0xffffffff);
			holder.height += 10;
			system.primaryStar.describe((prop, value) -> {
				holder.emit(String.format("§9%s§r: %s", prop, value), 0xffffffff);
			});
		});
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
		GlManager.currentState().sync();
	}

}
