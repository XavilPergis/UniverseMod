package net.xavil.hawklib.client.flexible;

import javax.annotation.Nullable;

import org.lwjgl.opengl.GL45C;

import com.mojang.blaze3d.vertex.VertexFormat;

public enum PrimitiveType {

	// @formatter:off
	POINT         (GL45C.GL_POINTS,         1, 1, 0, null,                               null),
	LINE          (GL45C.GL_LINES,          2, 2, 0, VertexFormat.Mode.DEBUG_LINES,      null),
	LINE_STRIP    (GL45C.GL_LINE_STRIP,     2, 1, 0, VertexFormat.Mode.DEBUG_LINE_STRIP, null),
	TRIANGLE      (GL45C.GL_TRIANGLES,      3, 3, 0, VertexFormat.Mode.TRIANGLES,        null),
	TRIANGLE_STRIP(GL45C.GL_TRIANGLE_STRIP, 3, 1, 0, VertexFormat.Mode.TRIANGLE_STRIP,   null),
	TRIANGLE_FAN  (GL45C.GL_TRIANGLE_FAN,   3, 1, 0, VertexFormat.Mode.TRIANGLE_FAN,     null),

	LINE_STRIP_VANILLA(GL45C.GL_TRIANGLE_STRIP, 2, 1, 1, VertexFormat.Mode.LINE_STRIP, null),
	LINE_DUPLICATED   (GL45C.GL_TRIANGLES,      2, 2, 1, VertexFormat.Mode.LINES,      IndexPattern.LINES),
	// points do not actually use the GL_POINTS primitive type, and are instead expanded into quads.
	POINT_DUPLICATED  (GL45C.GL_TRIANGLES,      4, 4, 3, null,                         IndexPattern.QUADS),
	// start at bottom right, emit vertices clockwise
	QUAD_DUPLICATED   (GL45C.GL_TRIANGLES,      4, 4, 0, VertexFormat.Mode.QUADS,      IndexPattern.QUADS);
	// @formatter:on

	public static enum IndexPattern {
		LINES, QUADS;
	}

	public final int gl;
	// how many logical vertices make up this primitive?
	public final int primitiveSize;
	// how many logical vertices do we need to advance to get the next primitive?
	public final int primitiveStride;
	// the amount of times that a vertex should be duplicated when building a
	// primitive of this type. A value of 0 means that the vertex will only be
	// emitted once.
	public final int duplicationCount;
	@Nullable
	public final VertexFormat.Mode vanilla;
	@Nullable
	public final IndexPattern indexPattern;

	private PrimitiveType(int gl, int primitiveSize, int primitiveStride, int duplicationCount,
			VertexFormat.Mode vanilla, IndexPattern indexPattern) {
		this.gl = gl;
		this.primitiveSize = primitiveSize;
		this.primitiveStride = primitiveStride;
		this.duplicationCount = duplicationCount;
		this.vanilla = vanilla;
		this.indexPattern = indexPattern;
	}

	public static PrimitiveType from(VertexFormat.Mode vanilla) {
		return switch (vanilla) {
			case LINES -> LINE_DUPLICATED;
			case LINE_STRIP -> LINE_STRIP_VANILLA;
			case DEBUG_LINES -> LINE;
			case DEBUG_LINE_STRIP -> LINE_STRIP;
			case TRIANGLES -> TRIANGLE;
			case TRIANGLE_STRIP -> TRIANGLE_STRIP;
			case TRIANGLE_FAN -> TRIANGLE_FAN;
			case QUADS -> QUAD_DUPLICATED;
		};
	}

	public int physicalIndexCount(int vertexCount) {
		return switch (this) {
			case POINT, LINE_STRIP_VANILLA, LINE, LINE_STRIP, TRIANGLE, TRIANGLE_STRIP, TRIANGLE_FAN -> vertexCount;
			case LINE_DUPLICATED, QUAD_DUPLICATED, POINT_DUPLICATED -> vertexCount / 4 * 6;
			default -> 0;
		};
	}

}
