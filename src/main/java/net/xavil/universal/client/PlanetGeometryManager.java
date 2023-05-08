package net.xavil.universal.client;

import net.xavil.universal.client.flexible.FlexibleVertexBuffer;
import net.xavil.util.Disposable;

public final class PlanetGeometryManager implements Disposable {

	static final class Node {
		public FlexibleVertexBuffer patchBuffer;
		private Node nn, np, pn, pp;
	}

	private Node xn, xp;
	private Node yn, yp;
	private Node zn, zp;

	@Override
	public void dispose() {
		// TODO Auto-generated method stub
	}

}