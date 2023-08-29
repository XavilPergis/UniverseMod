package net.xavil.hawklib.client.gl.shader;

import java.util.List;

import org.lwjgl.opengl.GL45C;

import com.mojang.blaze3d.platform.GlStateManager;

import net.xavil.hawklib.client.gl.GlObject;

public final class ShaderStage extends GlObject {

	public static enum Stage {
		VERTEX(GL45C.GL_VERTEX_SHADER, "Vertex Stage", "VERTEX"),
		TESSELATION_CONTROL(GL45C.GL_TESS_CONTROL_SHADER, "Tesselation Control Stage", "TESSELATION_CONTROL"),
		TESSELATION_EVALUATION(GL45C.GL_TESS_EVALUATION_SHADER, "Tesselation Evaluation Stage", "TESSELATION_EVALUATION"),
		GEOMETRY(GL45C.GL_GEOMETRY_SHADER, "Geometry Stage", "GEOMETRY"),
		FRAGMENT(GL45C.GL_FRAGMENT_SHADER, "Fragment Stage", "FRAGMENT"),
		COMPUTE(GL45C.GL_COMPUTE_SHADER, "Fragment Stage", "COMPUTE");

		public final int id;
		public final String description;
		public final String specializationDefine;

		private Stage(int id, String description, String specializationDefine) {
			this.id = id;
			this.description = description;
			this.specializationDefine = specializationDefine;
		}

		public static Stage from(int id) {
			return switch (id) {
				case GL45C.GL_VERTEX_SHADER -> VERTEX;
				case GL45C.GL_TESS_CONTROL_SHADER -> TESSELATION_CONTROL;
				case GL45C.GL_TESS_EVALUATION_SHADER -> TESSELATION_EVALUATION;
				case GL45C.GL_GEOMETRY_SHADER -> GEOMETRY;
				case GL45C.GL_FRAGMENT_SHADER -> FRAGMENT;
				case GL45C.GL_COMPUTE_SHADER -> COMPUTE;
				default -> null;
			};
		}
	}

	public final Stage type;

	public ShaderStage(int id, boolean owned) {
		super(id, owned);
		this.type = Stage.from(GL45C.glGetShaderi(id, GL45C.GL_SHADER_TYPE));
	}

	public ShaderStage(Stage type) {
		super(GL45C.glCreateShader(type.id), true);
		this.type = type;
	}

	@Override
	protected void destroy() {
		GL45C.glDeleteShader(this.id);
	}

	@Override
	public ObjectType objectType() {
		return ObjectType.SHADER;
	}

	public void setSource(String source) {
		GlStateManager.glShaderSource(this.id, List.of(source));
	}

	public String getSource() {
		return GL45C.glGetShaderSource(this.id);
	}

	public boolean compile() {
		GlStateManager.glCompileShader(this.id);
		return GL45C.glGetShaderi(this.id, GL45C.GL_COMPILE_STATUS) != GL45C.GL_FALSE;
	}

	public String infoLog() {
		return GL45C.glGetShaderInfoLog(this.id);
	}

}
