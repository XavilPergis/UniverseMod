package net.xavil.hawklib.client.flexible.vertex;

import javax.annotation.Nullable;

import net.xavil.hawklib.client.flexible.BufferLayout;
import net.xavil.hawklib.client.flexible.IndexPattern;
import net.xavil.hawklib.client.flexible.Mesh;
import net.xavil.hawklib.client.gl.DrawState;
import net.xavil.hawklib.client.gl.GlBuffer;
import net.xavil.hawklib.client.gl.shader.ShaderProgram;

// NOTE: `FilledBuffer`s become invalid when the VertexBuilder they came from starts drawing again.
public final class FilledBuffer {

	public final VertexBuilder builder;
	public final GlBuffer.Slice vertexData;
	public final BufferLayout layout;
	public final int vertexCount;

	@Nullable
	public final IndexPattern indexPattern;

	private final Runnable syncPointEmitter;
	private boolean invalid = false;

	public FilledBuffer(
			VertexBuilder builder,
			GlBuffer.Slice vertexData,
			int vertexCount,
			BufferLayout layout,
			IndexPattern indexPattern,
			Runnable syncPointEmitter) {
		this.builder = builder;
		this.layout = layout;
		this.vertexCount = vertexCount;
		this.indexPattern = indexPattern;
		this.vertexData = vertexData;
		this.syncPointEmitter = syncPointEmitter;
	}

	/**
	 * Emit a synchronization barrier after the GL buffer is used, so that the
	 * VertexBuilder it came from does not clobber data before the GPU can read it.
	 */
	public void finishUsing() {
		if (!this.invalid)
			this.syncPointEmitter.run();
		this.invalid = true;
	}

	public void invalidate() {
		this.invalid = true;
	}

	public boolean isValid() {
		return !this.invalid;
	}

	public void draw(ShaderProgram shader, DrawState drawState) {
		Mesh.draw(shader, this, drawState);
	}

}