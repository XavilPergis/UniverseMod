#version 150

float gamma = 2.2;

vec3 tonemap(vec3 color){	
	mat3 m1 = mat3(
        0.59719, 0.07600, 0.02840,
        0.35458, 0.90834, 0.13383,
        0.04823, 0.01566, 0.83777
	);
	mat3 m2 = mat3(
        1.60475, -0.10208, -0.00327,
        -0.53108,  1.10813, -0.07276,
        -0.07367, -0.00605,  1.07602
	);
	vec3 v = m1 * color;    
	vec3 a = v * (v + 0.0245786) - 0.000090537;
	vec3 b = v * (0.983729 * v + 0.4329510) + 0.238081;
	return pow(clamp(m2 * (a / b), 0.0, 1.0), vec3(1.0 / 2.2));	
}

uniform sampler2D DiffuseSampler;

in vec2 texCoord;
in vec2 oneTexel;

out vec4 fragColor;

vec3 skyColor() {
	return vec3(0.3, 0.6, 1.0);
}

void main(){
    vec4 lightWithAlpha = texture(DiffuseSampler, texCoord);
	vec3 imageLight = lightWithAlpha.rgb;

	vec3 skyColor = skyColor();
	float skyIntensity = 0.0;
	float exposure = 1.5;

	vec3 lightOut = vec3(0.0);
	lightOut += imageLight;
	lightOut += skyIntensity * skyColor;

	// vec3 tonemapped = tonemap(lightOut);
	// tonemapped *= exposure;
	// float gamma = 2.2;
	// tonemapped = pow(tonemapped, vec3(1.0 / gamma));
	// lightOut *= 0.4;

	// vec3 outColor = vec3(0.0);


	// float blendFactor = min(0.0, length(light) - skyIntensity);

	// outColor = mix(light, skyColor, blendFactor);

    // fragColor = vec4(tonemapped, 1.0);
    fragColor = vec4(lightOut, 1.0);
}
