package net.xavil.ultraviolet.client;

import net.xavil.hawklib.client.camera.CachedCamera;
import net.xavil.hawklib.client.flexible.vertex.VertexBuilder;
import net.xavil.hawklib.collections.impl.Vector;
import net.xavil.hawklib.math.TransformStack;
import net.xavil.ultraviolet.common.universe.system.UnaryCelestialNode;

public final class CelestialRenderer {

	private final Vector<UnaryCelestialNode> nodesToRender = new Vector<>();

	public void addCelestialNode(UnaryCelestialNode node, boolean shouldRenderSurface) {
		this.nodesToRender.push(node);
	}

	public void render(VertexBuilder builder, CachedCamera camera, UnaryCelestialNode node, TransformStack transform) {
		this.nodesToRender.clear();
	}

}
