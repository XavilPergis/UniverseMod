package net.xavil.hawklib.client.flexible;

import javax.annotation.Nullable;

import org.lwjgl.opengl.GL45C;

import com.mojang.blaze3d.vertex.VertexFormat;

public enum PrimitiveType {

	// @formatter:off
	POINT         (GL45C.GL_POINTS,         1, 1, null,                               null),
	LINE          (GL45C.GL_LINES,          2, 2, VertexFormat.Mode.DEBUG_LINES,      null),
	LINE_STRIP    (GL45C.GL_LINE_STRIP,     2, 1, VertexFormat.Mode.DEBUG_LINE_STRIP, null),
	TRIANGLE      (GL45C.GL_TRIANGLES,      3, 3, VertexFormat.Mode.TRIANGLES,        null),
	TRIANGLE_STRIP(GL45C.GL_TRIANGLE_STRIP, 3, 1, VertexFormat.Mode.TRIANGLE_STRIP,   null),
	TRIANGLE_FAN  (GL45C.GL_TRIANGLE_FAN,   3, 1, VertexFormat.Mode.TRIANGLE_FAN,     null),
	;
	// @formatter:on

	public static enum IndexPattern {
		LINES, QUADS;
	}

	public final int gl;
	// how many logical vertices make up this primitive?
	public final int primitiveSize;
	// how many logical vertices do we need to advance to get the next primitive?
	public final int primitiveStride;
	@Nullable
	public final VertexFormat.Mode vanilla;
	@Nullable
	public final IndexPattern indexPattern;

	private PrimitiveType(int gl, int primitiveSize, int primitiveStride,
			VertexFormat.Mode vanilla, IndexPattern indexPattern) {
		this.gl = gl;
		this.primitiveSize = primitiveSize;
		this.primitiveStride = primitiveStride;
		this.vanilla = vanilla;
		this.indexPattern = indexPattern;
	}

}
