package net.xavil.hawklib.client.gl;

import javax.annotation.OverridingMethodsMustInvokeSuper;

import net.xavil.hawklib.Disposable;
import net.xavil.hawklib.HawkLib;

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
	@OverridingMethodsMustInvokeSuper
	public void close() {
		if (this.destroyed) {
			HawkLib.LOGGER.warn(toString() + ": Tried to destroy GL object more than once!");
			return;
		}
		if (this.owned)
			destroy();
		this.destroyed = true;
	}

	public String getDebugName() {
		return this.debugName;
	}

	public void setDebugName(String debugName) {
		this.debugName = debugName;
	}

	protected abstract void destroy();

	public abstract ObjectType objectType();

	public void writeDebugInfo(StringBuilder output) {
		output.append(objectType().description);
		if (this.debugName != null) {
			output.append(" '");
			output.append(this.debugName);
			output.append("'");
		}
		output.append(" [id:");
		output.append(this.id);
		output.append("]");
		if (this.owned) {
			output.append(" [owned]");
		} else {
			output.append(" [unowned]");
		}
		output.append('\n');
	}

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

	public final boolean isDestroyed() {
		return this.destroyed;
	}

}
