package net.xavil.universal.client.flexible;

import java.nio.ByteBuffer;

import org.lwjgl.opengl.GL32;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.renderer.ShaderInstance;
import net.xavil.util.Assert;
import net.xavil.util.Disposable;

public class FlexibleVertexBuffer implements Disposable {
	private int vertexBufferId = -1;
	private int indexBufferId = -1;
	private int vertexArrayId = -1;

	private int bufferUsage = GL32.GL_STATIC_DRAW;

	private FlexibleVertexMode mode;
	private VertexFormat format;

	private VertexFormat.IndexType indexType;
	private int indexCount;
	private boolean sequentialIndices;

	public void create(boolean createIndexBuffer, int usage) {
		if (this.vertexBufferId < 0)
			RenderSystem.glGenBuffers(id -> this.vertexBufferId = id);
		if (createIndexBuffer && this.indexBufferId < 0)
			RenderSystem.glGenBuffers(id -> this.indexBufferId = id);
		if (this.vertexArrayId < 0)
			RenderSystem.glGenVertexArrays(id -> this.vertexArrayId = id);
	}

	public boolean isCreated() {
		return this.vertexBufferId >= 0;
	}

	public void upload(FlexibleBufferBuilder builder) {
		if (builder.isBuilding())
			builder.end();
		final var finished = builder.popFinished();
		upload(finished.getSecond(), finished.getFirst(), GL32.GL_STATIC_DRAW);
	}

	public void upload(ByteBuffer buffer, FinishedBuffer info, int usage) {
		if (!isCreated())
			create(false, usage);

		buffer.clear();

		this.mode = info.mode();
		this.format = info.format();
		this.sequentialIndices = info.sequentialIndex();
		this.indexCount = info.indexCount();
		this.bufferUsage = usage;

		final var byteLimit = info.vertexCount() * info.format().getVertexSize();
		buffer.position(0);
		buffer.limit(byteLimit);
		setupForFormat(info.format());
		// GlStateManager._glBindVertexArray(this.vertexArrayId);
		// GlStateManager._glBindBuffer(GL32.GL_ARRAY_BUFFER, this.vertexBufferId);
		GlStateManager._glBufferData(GL32.GL_ARRAY_BUFFER, buffer, this.bufferUsage);

		if (info.sequentialIndex()) {
			final var sequentialIndexBuffer = info.mode().getSequentialBuffer(info.indexCount());
			this.indexBufferId = sequentialIndexBuffer.name();
			this.indexType = sequentialIndexBuffer.type();
			GlStateManager._glBindBuffer(GL32.GL_ELEMENT_ARRAY_BUFFER, this.indexBufferId);
		} else {
			create(true, usage);
			buffer.position(byteLimit);
			buffer.limit(byteLimit + info.indexCount() * info.indexType().bytes);
			GlStateManager._glBindBuffer(GL32.GL_ELEMENT_ARRAY_BUFFER, this.indexBufferId);
			GlStateManager._glBufferData(GL32.GL_ELEMENT_ARRAY_BUFFER, buffer, this.bufferUsage);
			this.indexType = info.indexType();
		}

		buffer.position(0);
		unbind();
	}

	public void draw(ShaderInstance shader) {
		if (this.indexCount <= 0)
			return;
		shader.apply();
		bind();
		GlStateManager._drawElements(this.mode.gl, this.indexCount, this.indexType.asGLType, 0L);
		unbind();
		shader.clear();
	}

	private void setupForFormat(VertexFormat format) {
		Assert.isTrue(isCreated());
		// if (this.format != null && this.format.equals(format))
		// 	return;
		BufferUploader.reset();
		if (this.format != null)
			this.format.clearBufferState();
		GlStateManager._glBindVertexArray(this.vertexArrayId);
		GlStateManager._glBindBuffer(GL32.GL_ARRAY_BUFFER, this.vertexBufferId);
		format.setupBufferState();
		this.format = format;
	}

	public void bind() {
		GlStateManager._glBindVertexArray(this.vertexArrayId);
		GlStateManager._glBindBuffer(GL32.GL_ARRAY_BUFFER, this.vertexBufferId);
		GlStateManager._glBindBuffer(GL32.GL_ELEMENT_ARRAY_BUFFER, this.indexBufferId);
	}

	public static void unbind() {
		GlStateManager._glBindVertexArray(0);
		GlStateManager._glBindBuffer(GL32.GL_ARRAY_BUFFER, 0);
		GlStateManager._glBindBuffer(GL32.GL_ELEMENT_ARRAY_BUFFER, 0);
	}

	@Override
	public void close() {
		if (this.vertexBufferId >= 0) {
			RenderSystem.glDeleteBuffers(this.vertexBufferId);
		}
		// sequential indices use a shared index buffer; we don't want to delete that!!
		if (!this.sequentialIndices && this.indexBufferId >= 0) {
			RenderSystem.glDeleteBuffers(this.indexBufferId);
		}
		if (this.vertexArrayId >= 0) {
			RenderSystem.glDeleteVertexArrays(this.vertexArrayId);
		}
		this.vertexBufferId = -1;
		this.indexBufferId = -1;
		this.vertexArrayId = -1;
	}

}
