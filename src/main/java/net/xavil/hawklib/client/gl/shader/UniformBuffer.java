package net.xavil.hawklib.client.gl.shader;

import net.xavil.hawklib.client.gl.GlBuffer;

public final class UniformBuffer extends GlBuffer implements UniformHolder {

	@Override
	public void markDirty(boolean dirty) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'markDirty'");
	}

	@Override
	public UniformSlot getSlot(String uniformName) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'getSlot'");
	}

	public static final class UniformInterface {
	}

	public static abstract sealed class GlType {
		public static final class Primitive extends GlType {
		}

		public static final class Array extends GlType {
			public int length;

			public GlType get(int index) {
				return null;
			}
		}

		public static final class Struct extends GlType {
			public GlType get(String member) {
				return null;
			}
		}
	}

	// foo:int (norm:vec3 color:vec4){4} (pos:vec3 color:vec4){32}

	// void foo() {
	// 	buffer.set("foo", foo);
	// 	var dirLights = buffer.get("dirLights");
	// 	for (int i = 0; i < 4; ++i) {
	// 		dirLights.get(i)
	// 		.set("norm", directionalLights.get(i).normal)
	// 		.set("color", directionalLights.get(i).color);
	// 	}
	// 	var pointLights = buffer.get("lights");
	// 	for (int i = 0; i < 32; ++i) {
	// 		pointLights.get(i)
	// 			.set("norm", lights.get(i).normal)
	// 			.set("color", lights.get(i).color);
	// 	}
	// }

	// buf.

	// struct DirectionalLight { vec3 norm; vec4 color; };
	// struct Light { vec3 pos; vec4 color; };

	// uniform Block1 {
	// 	int foo;
	// 	DirectionalLight dirLights[4];
	// 	PointLight lights[32];
	// } block1;
	
}
