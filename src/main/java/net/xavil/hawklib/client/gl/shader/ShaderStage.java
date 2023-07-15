package net.xavil.hawklib.client.gl.shader;

import java.util.List;

import org.lwjgl.opengl.GL32C;

import com.mojang.blaze3d.platform.GlStateManager;

import net.xavil.hawklib.client.gl.GlManager;
import net.xavil.hawklib.client.gl.GlObject;

public final class ShaderStage extends GlObject {

	public static enum Stage {
		VERTEX(GL32C.GL_VERTEX_SHADER, "Vertex Stage", "VERTEX"),
		GEOMETRY(GL32C.GL_GEOMETRY_SHADER, "Geometry Stage", "GEOMETRY"),
		FRAGMENT(GL32C.GL_FRAGMENT_SHADER, "Fragment Stage", "FRAGMENT");

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
				case GL32C.GL_VERTEX_SHADER -> VERTEX;
				case GL32C.GL_GEOMETRY_SHADER -> GEOMETRY;
				case GL32C.GL_FRAGMENT_SHADER -> FRAGMENT;
				default -> null;
			};
		}
	}

	public final Stage type;

	public ShaderStage(int id, boolean owned) {
		super(id, owned);
		this.type = Stage.from(GL32C.glGetShaderi(id, GL32C.GL_SHADER_TYPE));
	}

	public ShaderStage(Stage type) {
		super(GlManager.createShader(type), true);
		this.type = type;
	}

	@Override
	public ObjectType objectType() {
		return ObjectType.SHADER;
	}

	public void setSource(String source) {
		GlStateManager.glShaderSource(this.id, List.of(source));
	}

	public String getSource() {
		return GL32C.glGetShaderSource(this.id);
	}

	public boolean compile() {
		GlStateManager.glCompileShader(this.id);
		return GL32C.glGetShaderi(this.id, GL32C.GL_COMPILE_STATUS) != GL32C.GL_FALSE;
	}

	public String infoLog() {
		return GL32C.glGetShaderInfoLog(this.id);
	}

}
