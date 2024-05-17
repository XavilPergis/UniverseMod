#stages compute

uniform sampler2D uSceneTexture;
layout (r16f, binding = 1) uniform image2D oLuminanceTexture;

layout (local_size_x = 16, local_size_y = 16, local_size_z = 1) in;
void main() {
	ivec2 size = imageSize(oLuminanceTexture);
	if (gl_GlobalInvocationID.x <= size.x && gl_GlobalInvocationID.y <= size.y) {
		vec2 uv = vec2(gl_GlobalInvocationID.xy) / vec2(size);
		vec4 color = texture(uSceneTexture, uv);
		float luminance = dot(color.rgb, vec3(0.2125, 0.7154, 0.0721));
		// luminance *= max(0.0, 1.0 - 2.0 * length(uv - 0.5));
		// luminance *= uv.x;
		imageStore(oLuminanceTexture, ivec2(gl_GlobalInvocationID.xy), vec4(luminance));
	}
}
