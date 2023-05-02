package net.xavil.universal.client.flexible;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.xavil.util.Disposable;

public class FlexibleVertexBuffer implements Disposable {
	private int vboId = -1;
	private int iboId = -1;
	private int vaoId = -1;
	private VertexFormat.Mode mode;
	private VertexFormat format;
	private VertexFormat.IndexType indexType;
	private int indexCount;
	private boolean sequentialIndices;

	public void create() {
		if (this.vboId < 0)
		RenderSystem.glGenBuffers(id -> this.vboId = id);
		if (this.iboId < 0)
		RenderSystem.glGenBuffers(id -> this.iboId = id);
		if (this.vaoId < 0)RenderSystem.glGenVertexArrays(null);
	}

	@Override
	public void dispose() {
		if (this.vboId >= 0) {
			RenderSystem.glDeleteBuffers(this.vboId);
			this.vboId = -1;
		}
		if (this.iboId >= 0) {
			RenderSystem.glDeleteBuffers(this.iboId);
			this.iboId = -1;
		}
		if (this.vaoId >= 0) {
			RenderSystem.glDeleteVertexArrays(this.vaoId);
			this.vaoId = -1;
		}
	}

}
