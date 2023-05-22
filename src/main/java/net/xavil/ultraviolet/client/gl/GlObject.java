package net.xavil.ultraviolet.client.gl;

import net.xavil.ultraviolet.Mod;
import net.xavil.util.Disposable;

public abstract class GlObject implements Disposable {

	public static enum ObjectType {
		TEXTURE("Texture"),
		BUFFER("Buffer"),
		VERTEX_ARRAY("Vertex Array"),
		FRAMEBUFFER("Framebuffer"),
		RENDERBUFFER("Renderbuffer"),
		SHADER("Shader Stage"),
		PROGRAM("Shader");

		public final String description;

		private ObjectType(String description) {
			this.description = description;
		}
	}

	public final int id;
	// does this object have ownership of the associated GL resource? ie, is it
	// responsible for deleting the resource when done?
	protected final boolean owned;
	protected boolean destroyed = false;
	protected String debugName;

	public GlObject(int id, boolean owned) {
		this.id = id;
		this.owned = owned;
	}

	@Override
	public void close() {
		if (this.destroyed) {
			Mod.LOGGER.warn(toString() + ": Tried to destroy GL object more than once!");
			return;
		}
		if (this.owned)
			GlManager.deleteObject(objectType(), this.id);
		this.destroyed = true;
	}

	public String getDebugName() {
		return this.debugName;
	}

	public void setDebugName(String debugName) {
		this.debugName = debugName;
	}

	public abstract ObjectType objectType();

	// protected abstract void release(int id);

	public String debugDescription() {
		var desc = objectType().description + " " + this.id;
		if (this.debugName != null)
			desc += " " + this.debugName;
		return desc;
	}

	@Override
	public String toString() {
		return "[" + debugDescription() + "]@" + Integer.toHexString(hashCode());
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof GlObject other
				&& objectType() == other.objectType()
				&& this.id == other.id;
	}

}
