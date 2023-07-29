package net.xavil.hawklib.client.flexible;

import javax.annotation.Nullable;

import org.lwjgl.opengl.GL32C;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.xavil.hawklib.client.gl.GlBuffer;
import net.xavil.hawklib.client.gl.GlManager;
import net.xavil.hawklib.client.gl.UnmanagedStateSink;

public enum PrimitiveType {
	// @formatter:off
	// points do not actually use the GL_POINTS primitive type, and are instead expanded into quads.
	POINT_QUADS     (GL32C.GL_TRIANGLES,      4, 4, 3, null,                               VertexFormat.Mode.QUADS),
	POINTS          (GL32C.GL_POINTS,         1, 1, 0, null,                               VertexFormat.Mode.TRIANGLES),

	LINES           (GL32C.GL_TRIANGLES,      2, 2, 1, VertexFormat.Mode.LINES,            VertexFormat.Mode.LINES),
	LINE_STRIP      (GL32C.GL_TRIANGLE_STRIP, 2, 1, 1, VertexFormat.Mode.LINE_STRIP,       VertexFormat.Mode.LINE_STRIP),
	DEBUG_LINES     (GL32C.GL_LINES,          2, 2, 0, VertexFormat.Mode.DEBUG_LINES,      VertexFormat.Mode.DEBUG_LINES),
	DEBUG_LINE_STRIP(GL32C.GL_LINE_STRIP,     2, 1, 0, VertexFormat.Mode.DEBUG_LINE_STRIP, VertexFormat.Mode.DEBUG_LINE_STRIP),
	TRIANGLES       (GL32C.GL_TRIANGLES,      3, 3, 0, VertexFormat.Mode.TRIANGLES,        VertexFormat.Mode.TRIANGLES),
	TRIANGLE_STRIP  (GL32C.GL_TRIANGLE_STRIP, 3, 1, 0, VertexFormat.Mode.TRIANGLE_STRIP,   VertexFormat.Mode.TRIANGLE_STRIP),
	TRIANGLE_FAN    (GL32C.GL_TRIANGLE_FAN,   3, 1, 0, VertexFormat.Mode.TRIANGLE_FAN,     VertexFormat.Mode.TRIANGLE_FAN),
	QUADS           (GL32C.GL_TRIANGLES,      4, 4, 0, VertexFormat.Mode.QUADS,            VertexFormat.Mode.QUADS);
	// @formatter:on

	public final int gl;
	// how many vertices make up this primitive?
	public final int primitiveSize;
	// how many vertices do we need to advance to get the next primitive?
	public final int primitiveStride;
	// the amount of times that a vertex should be duplicated when building a
	// primitive of this type. A value of 0 means that the vertex will only be
	// emitted once.
	public final int duplicationCount;
	@Nullable
	public final VertexFormat.Mode vanilla;
	public final VertexFormat.Mode indexBufferType;

	private PrimitiveType(int gl, int primitiveSize, int primitiveStride, int duplicationCount,
			VertexFormat.Mode vanilla, VertexFormat.Mode indexBufferType) {
		this.gl = gl;
		this.primitiveSize = primitiveSize;
		this.primitiveStride = primitiveStride;
		this.duplicationCount = duplicationCount;
		this.vanilla = vanilla;
		this.indexBufferType = indexBufferType;
	}

	public static PrimitiveType from(VertexFormat.Mode vanilla) {
		return switch (vanilla) {
			case LINES -> LINES;
			case LINE_STRIP -> LINE_STRIP;
			case DEBUG_LINES -> DEBUG_LINES;
			case DEBUG_LINE_STRIP -> DEBUG_LINE_STRIP;
			case TRIANGLES -> TRIANGLES;
			case TRIANGLE_STRIP -> TRIANGLE_STRIP;
			case TRIANGLE_FAN -> TRIANGLE_FAN;
			case QUADS -> QUADS;
		};
	}

	public int indexCount(int vertices) {
		return switch (this) {
			case LINE_STRIP, DEBUG_LINES, DEBUG_LINE_STRIP, TRIANGLES, TRIANGLE_STRIP, TRIANGLE_FAN -> vertices;
			case LINES, QUADS, POINT_QUADS -> vertices / 4 * 6;
			default -> 0;
		};
	}

	public RenderSystem.AutoStorageIndexBuffer getSequentialBuffer(int indexCount) {
		final var previouslyBound = GlManager.isManaged()
				? GlManager.currentState().boundBuffers[GlBuffer.Type.ELEMENT.ordinal()]
				: 0;
		RenderSystem.AutoStorageIndexBuffer buf;
		buf = RenderSystem.getSequentialBuffer(this.indexBufferType, indexCount);
		if (GlManager.isManaged())
			UnmanagedStateSink.INSTANCE.bindBuffer(GlBuffer.Type.ELEMENT, previouslyBound);
		return buf;
	}
}
