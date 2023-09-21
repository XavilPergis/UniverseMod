package net.xavil.hawklib.client.gl;

import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL45C;

public final class GlLimits {

	public static final int MAX_3D_TEXTURE_SIZE;
	public static final int MAX_CLIP_DISTANCES;
	public static final int MAX_COMBINED_FRAGMENT_UNIFORM_COMPONENTS;
	public static final int MAX_COMBINED_TEXTURE_IMAGE_UNITS;
	public static final int MAX_COMBINED_VERTEX_UNIFORM_COMPONENTS;
	public static final int MAX_COMBINED_GEOMETRY_UNIFORM_COMPONENTS;
	// public static final int MAX_VARYING_COMPONENTS;
	public static final int MAX_COMBINED_UNIFORM_BLOCKS;
	public static final int MAX_CUBE_MAP_TEXTURE_SIZE;
	public static final int MAX_DRAW_BUFFERS;
	public static final int MAX_ELEMENTS_INDICES;
	public static final int MAX_ELEMENTS_VERTICES;
	public static final int MAX_FRAGMENT_UNIFORM_COMPONENTS;
	public static final int MAX_FRAGMENT_UNIFORM_BLOCKS;
	public static final int MAX_FRAGMENT_INPUT_COMPONENTS;
	public static final int MIN_PROGRAM_TEXEL_OFFSET;
	public static final int MAX_PROGRAM_TEXEL_OFFSET;
	public static final int MAX_RECTANGLE_TEXTURE_SIZE;
	public static final int MAX_TEXTURE_IMAGE_UNITS;
	public static final float MAX_TEXTURE_LOD_BIAS;
	public static final int MAX_TEXTURE_SIZE;
	public static final int MAX_RENDERBUFFER_SIZE;
	public static final int MAX_ARRAY_TEXTURE_LAYERS;
	public static final int MAX_TEXTURE_BUFFER_SIZE;
	public static final int MAX_UNIFORM_BLOCK_SIZE;
	// public static final int MAX_VARYING_FLOATS;
	public static final int MAX_VERTEX_ATTRIBS;
	public static final int MAX_VERTEX_TEXTURE_IMAGE_UNITS;
	public static final int MAX_GEOMETRY_TEXTURE_IMAGE_UNITS;
	public static final int MAX_VERTEX_UNIFORM_COMPONENTS;
	public static final int MAX_VERTEX_OUTPUT_COMPONENTS;
	public static final int MAX_GEOMETRY_UNIFORM_COMPONENTS;
	public static final int MAX_SAMPLE_MASK_WORDS;
	public static final int MAX_COLOR_TEXTURE_SAMPLES;
	public static final int MAX_DEPTH_TEXTURE_SAMPLES;
	public static final int MAX_INTEGER_SAMPLES;
	public static final long MAX_SERVER_WAIT_TIMEOUT;
	public static final int MAX_UNIFORM_BUFFER_BINDINGS;
	public static final int UNIFORM_BUFFER_OFFSET_ALIGNMENT;
	public static final int MAX_VERTEX_UNIFORM_BLOCKS;
	public static final int MAX_GEOMETRY_UNIFORM_BLOCKS;
	public static final int MAX_GEOMETRY_INPUT_COMPONENTS;
	public static final int MAX_GEOMETRY_OUTPUT_COMPONENTS;
	public static final int MAX_VIEWPORT_DIMS;

	public static final boolean HAS_DIRECT_STATE_ACCESS = GL.getCapabilities().GL_ARB_direct_state_access;

	static {
		MAX_3D_TEXTURE_SIZE = GL45C.glGetInteger(GL45C.GL_MAX_3D_TEXTURE_SIZE);
		MAX_CLIP_DISTANCES = GL45C.glGetInteger(GL45C.GL_MAX_CLIP_DISTANCES);
		MAX_COMBINED_FRAGMENT_UNIFORM_COMPONENTS = GL45C
				.glGetInteger(GL45C.GL_MAX_COMBINED_FRAGMENT_UNIFORM_COMPONENTS);
		MAX_COMBINED_TEXTURE_IMAGE_UNITS = GL45C.glGetInteger(GL45C.GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS);
		MAX_COMBINED_VERTEX_UNIFORM_COMPONENTS = GL45C.glGetInteger(GL45C.GL_MAX_COMBINED_VERTEX_UNIFORM_COMPONENTS);
		MAX_COMBINED_GEOMETRY_UNIFORM_COMPONENTS = GL45C
				.glGetInteger(GL45C.GL_MAX_COMBINED_GEOMETRY_UNIFORM_COMPONENTS);
		// MAX_VARYING_COMPONENTS = GL45C.glGetInteger(GL45C.GL_MAX_VARYING_COMPONENTS);
		MAX_COMBINED_UNIFORM_BLOCKS = GL45C.glGetInteger(GL45C.GL_MAX_COMBINED_UNIFORM_BLOCKS);
		MAX_CUBE_MAP_TEXTURE_SIZE = GL45C.glGetInteger(GL45C.GL_MAX_CUBE_MAP_TEXTURE_SIZE);
		MAX_DRAW_BUFFERS = GL45C.glGetInteger(GL45C.GL_MAX_DRAW_BUFFERS);
		MAX_ELEMENTS_INDICES = GL45C.glGetInteger(GL45C.GL_MAX_ELEMENTS_INDICES);
		MAX_ELEMENTS_VERTICES = GL45C.glGetInteger(GL45C.GL_MAX_ELEMENTS_VERTICES);
		MAX_FRAGMENT_UNIFORM_COMPONENTS = GL45C.glGetInteger(GL45C.GL_MAX_FRAGMENT_UNIFORM_COMPONENTS);
		MAX_FRAGMENT_UNIFORM_BLOCKS = GL45C.glGetInteger(GL45C.GL_MAX_FRAGMENT_UNIFORM_BLOCKS);
		MAX_FRAGMENT_INPUT_COMPONENTS = GL45C.glGetInteger(GL45C.GL_MAX_FRAGMENT_INPUT_COMPONENTS);
		MIN_PROGRAM_TEXEL_OFFSET = GL45C.glGetInteger(GL45C.GL_MIN_PROGRAM_TEXEL_OFFSET);
		MAX_PROGRAM_TEXEL_OFFSET = GL45C.glGetInteger(GL45C.GL_MAX_PROGRAM_TEXEL_OFFSET);
		MAX_RECTANGLE_TEXTURE_SIZE = GL45C.glGetInteger(GL45C.GL_MAX_RECTANGLE_TEXTURE_SIZE);
		MAX_TEXTURE_IMAGE_UNITS = GL45C.glGetInteger(GL45C.GL_MAX_TEXTURE_IMAGE_UNITS);
		MAX_TEXTURE_LOD_BIAS = GL45C.glGetFloat(GL45C.GL_MAX_TEXTURE_LOD_BIAS);
		MAX_TEXTURE_SIZE = GL45C.glGetInteger(GL45C.GL_MAX_TEXTURE_SIZE);
		MAX_RENDERBUFFER_SIZE = GL45C.glGetInteger(GL45C.GL_MAX_RENDERBUFFER_SIZE);
		MAX_ARRAY_TEXTURE_LAYERS = GL45C.glGetInteger(GL45C.GL_MAX_ARRAY_TEXTURE_LAYERS);
		MAX_TEXTURE_BUFFER_SIZE = GL45C.glGetInteger(GL45C.GL_MAX_TEXTURE_BUFFER_SIZE);
		MAX_UNIFORM_BLOCK_SIZE = GL45C.glGetInteger(GL45C.GL_MAX_UNIFORM_BLOCK_SIZE);
		// MAX_VARYING_FLOATS = GL45C.glGetInteger(GL45C.GL_MAX_VARYING_FLOATS);
		MAX_VERTEX_ATTRIBS = GL45C.glGetInteger(GL45C.GL_MAX_VERTEX_ATTRIBS);
		MAX_VERTEX_TEXTURE_IMAGE_UNITS = GL45C.glGetInteger(GL45C.GL_MAX_VERTEX_TEXTURE_IMAGE_UNITS);
		MAX_GEOMETRY_TEXTURE_IMAGE_UNITS = GL45C.glGetInteger(GL45C.GL_MAX_GEOMETRY_TEXTURE_IMAGE_UNITS);
		MAX_VERTEX_UNIFORM_COMPONENTS = GL45C.glGetInteger(GL45C.GL_MAX_VERTEX_UNIFORM_COMPONENTS);
		MAX_VERTEX_OUTPUT_COMPONENTS = GL45C.glGetInteger(GL45C.GL_MAX_VERTEX_OUTPUT_COMPONENTS);
		MAX_GEOMETRY_UNIFORM_COMPONENTS = GL45C.glGetInteger(GL45C.GL_MAX_GEOMETRY_UNIFORM_COMPONENTS);
		MAX_SAMPLE_MASK_WORDS = GL45C.glGetInteger(GL45C.GL_MAX_SAMPLE_MASK_WORDS);
		MAX_COLOR_TEXTURE_SAMPLES = GL45C.glGetInteger(GL45C.GL_MAX_COLOR_TEXTURE_SAMPLES);
		MAX_DEPTH_TEXTURE_SAMPLES = GL45C.glGetInteger(GL45C.GL_MAX_DEPTH_TEXTURE_SAMPLES);
		MAX_INTEGER_SAMPLES = GL45C.glGetInteger(GL45C.GL_MAX_INTEGER_SAMPLES);
		MAX_SERVER_WAIT_TIMEOUT = GL45C.glGetInteger64(GL45C.GL_MAX_SERVER_WAIT_TIMEOUT);
		MAX_UNIFORM_BUFFER_BINDINGS = GL45C.glGetInteger(GL45C.GL_MAX_UNIFORM_BUFFER_BINDINGS);
		UNIFORM_BUFFER_OFFSET_ALIGNMENT = GL45C.glGetInteger(GL45C.GL_UNIFORM_BUFFER_OFFSET_ALIGNMENT);
		MAX_VERTEX_UNIFORM_BLOCKS = GL45C.glGetInteger(GL45C.GL_MAX_VERTEX_UNIFORM_BLOCKS);
		MAX_GEOMETRY_UNIFORM_BLOCKS = GL45C.glGetInteger(GL45C.GL_MAX_GEOMETRY_UNIFORM_BLOCKS);
		MAX_GEOMETRY_INPUT_COMPONENTS = GL45C.glGetInteger(GL45C.GL_MAX_GEOMETRY_INPUT_COMPONENTS);
		MAX_GEOMETRY_OUTPUT_COMPONENTS = GL45C.glGetInteger(GL45C.GL_MAX_GEOMETRY_OUTPUT_COMPONENTS);
		MAX_VIEWPORT_DIMS = GL45C.glGetInteger(GL45C.GL_MAX_VIEWPORT_DIMS);
	}

	public static void validateRenderbufferSize(int width, int height) {
		if (width > MAX_RENDERBUFFER_SIZE || height > MAX_RENDERBUFFER_SIZE) {
			throw new IllegalArgumentException(String.format(
					"The maximum renderbuffer size is %d, but the requested size was (%d, %d)",
					MAX_RENDERBUFFER_SIZE, width, height));
		}
	}

	public static void validateTextureSize(int width) {
		if (width > MAX_TEXTURE_SIZE) {
			throw new IllegalArgumentException(String.format(
					"The maximum texture size is %d, but the requested size was (%d)",
					MAX_TEXTURE_SIZE));
		}
	}

	public static void validateTextureSize(int width, int height) {
		if (width > MAX_TEXTURE_SIZE || height > MAX_TEXTURE_SIZE) {
			throw new IllegalArgumentException(String.format(
					"The maximum texture size is %d, but the requested size was (%d, %d)",
					MAX_TEXTURE_SIZE, width, height));
		}
	}

	public static void validateTextureSize(int width, int height, int depth) {
		if (width > MAX_3D_TEXTURE_SIZE || height > MAX_3D_TEXTURE_SIZE || depth > MAX_3D_TEXTURE_SIZE) {
			throw new IllegalArgumentException(String.format(
					"The maximum 3d texture size is %d, but the requested size was (%d, %d, %d)",
					MAX_3D_TEXTURE_SIZE, width, height, depth));
		}
	}

	public static void validateTextureSizeCubemap(int size) {
		if (size > MAX_CUBE_MAP_TEXTURE_SIZE) {
			throw new IllegalArgumentException(String.format(
					"The maximum cubemap texture size is %d, but the requested size was %d",
					MAX_CUBE_MAP_TEXTURE_SIZE, size));
		}
	}
}
