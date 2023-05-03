package net.xavil.universal.client.flexible;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;

public enum FlexibleVertexMode {
	POINTS(4, 4, 4, 4),
	LINES(4, 2, 2, 2),
	LINE_STRIP(5, 2, 1, 2),
	DEBUG_LINES(1, 2, 2, 1),
	DEBUG_LINE_STRIP(3, 2, 1, 1),
	TRIANGLES(4, 3, 3, 1),
	TRIANGLE_STRIP(5, 3, 1, 1),
	TRIANGLE_FAN(6, 3, 1, 1),
	QUADS(4, 4, 4, 1);

	public final int gl;
	// how many vertices make up this primitive?
	public final int primitiveSize;
	// how many vertices do we need to advance to get the next primitive?
	public final int primitiveStride;
	// used while building a buffer
	public final int duplicationCount;

	private FlexibleVertexMode(int gl, int primitiveSize, int primitiveStride, int duplicationCount) {
		this.gl = gl;
		this.primitiveSize = primitiveSize;
		this.primitiveStride = primitiveStride;
		this.duplicationCount = duplicationCount;
	}

	public static FlexibleVertexMode from(VertexFormat.Mode vanilla) {
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

	public VertexFormat.Mode asVanilla() {
		return switch (this) {
			case POINTS -> null;
			case LINES -> VertexFormat.Mode.LINES;
			case LINE_STRIP -> VertexFormat.Mode.LINE_STRIP;
			case DEBUG_LINES -> VertexFormat.Mode.DEBUG_LINES;
			case DEBUG_LINE_STRIP -> VertexFormat.Mode.DEBUG_LINE_STRIP;
			case TRIANGLES -> VertexFormat.Mode.TRIANGLES;
			case TRIANGLE_STRIP -> VertexFormat.Mode.TRIANGLE_STRIP;
			case TRIANGLE_FAN -> VertexFormat.Mode.TRIANGLE_FAN;
			case QUADS -> VertexFormat.Mode.QUADS;
		};
	}

	public int indexCount(int vertices) {
		return switch (this) {
			case LINE_STRIP, DEBUG_LINES, DEBUG_LINE_STRIP, TRIANGLES, TRIANGLE_STRIP, TRIANGLE_FAN -> vertices;
			case LINES, QUADS -> vertices / 4 * 6;
			default -> 0;
		};
	}

	public RenderSystem.AutoStorageIndexBuffer getSequentialBuffer(int indexCount) {
		final var vanilla = asVanilla();
		if (vanilla != null)
			return RenderSystem.getSequentialBuffer(vanilla, indexCount);
		return RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS, indexCount);
	}
}
