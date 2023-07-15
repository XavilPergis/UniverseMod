package net.xavil.ultraviolet.client;

import net.xavil.hawklib.Disposable;
import net.xavil.hawklib.client.flexible.FlexibleVertexBuffer;

public final class PlanetGeometryManager implements Disposable {

	static final class Node {
		public FlexibleVertexBuffer patchBuffer;
		private Node nn, np, pn, pp;
	}

	private Node xn, xp;
	private Node yn, yp;
	private Node zn, zp;

	@Override
	public void close() {
		// TODO Auto-generated method stub
	}

}
