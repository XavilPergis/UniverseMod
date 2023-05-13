package net.xavil.universal.client.flexible;

import com.mojang.blaze3d.systems.RenderSystem;

public final class Buffer extends GlObject {

	public Buffer(int id, boolean owned) {
		super(id, owned);
	}

	public Buffer() {
		super(genBuffer(), true);
	}

	private static int genBuffer() {
		var holder = new int[1];
		RenderSystem.glGenBuffers(id -> holder[0] = id);
		return holder[0];
	}

	@Override
	public ObjectType objectType() {
		return ObjectType.BUFFER;
	}

	@Override
	protected void release(int id) {
		RenderSystem.glDeleteBuffers(this.id);
	}

	public static Buffer importFromId(int id) {
		return new Buffer(id, false);
	}
	
}
