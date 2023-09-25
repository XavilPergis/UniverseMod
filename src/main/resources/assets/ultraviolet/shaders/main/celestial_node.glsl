#stages vertex fragment

#define USE_MODEL_MATRIX
#include [ultraviolet:vertex/world.glsl]

#ifdef IS_FRAGMENT_STAGE
#include [ultraviolet:common_uniforms.glsl]

uniform vec4 uLightPos0;
uniform vec4 uLightPos1;
uniform vec4 uLightPos2;
uniform vec4 uLightPos3;
uniform vec4 uLightColor0;
uniform vec4 uLightColor1;
uniform vec4 uLightColor2;
uniform vec4 uLightColor3;

uniform int uNodeType;
uniform int uRenderingSeed;

uniform vec4 uStarColor;
uniform sampler1D uGasGiantColorGradient;

out vec4 fColor;

#define NODE_TYPE_STAR 0
#define NODE_TYPE_STAR_GIANT 1
#define NODE_TYPE_STAR_WHITE_DWARF 2
#define NODE_TYPE_STAR_NEUTRON_STAR 3
#define NODE_TYPE_STAR_BLACK_HOLE 4
#define NODE_TYPE_BROWN_DWARF 5
#define NODE_TYPE_GAS_GIANT 6
#define NODE_TYPE_ICE_WORLD 7
#define NODE_TYPE_ROCKY_WORLD 8
#define NODE_TYPE_ROCKY_ICE_WORLD 9
#define NODE_TYPE_WATER_WORLD 10
#define NODE_TYPE_EARTH_LIKE_WORLD 11

#define ULTRAVIOLET_CELESTIAL_SHADING_IMPLS_
// shared includes
#include [ultraviolet:lib/lighting.glsl]
#include [ultraviolet:lib/noise.glsl]
#include [ultraviolet:lib/util.glsl]

struct FragmentInfo {
	vec3 posM;
	vec3 posW;
	vec3 posV;
	vec3 normalM;
	vec3 normalW;
	vec3 normalV;

	vec3 lightingPosV;
	vec3 lightingNormalV;
};

vec3 applyLighting(in Material material, in FragmentInfo frag) {
	LightingContext ctx = makeLightingContext(material, uMetersPerUnit, frag.lightingPosV, frag.lightingNormalV);

	vec3 res = vec3(0.0);
	res += lightContribution(ctx, makePointLight((uViewMatrix * vec4(uLightPos0.xyz, 1.0)).xyz, uLightColor0.rgb * uLightColor0.a));
	res += lightContribution(ctx, makePointLight((uViewMatrix * vec4(uLightPos1.xyz, 1.0)).xyz, uLightColor1.rgb * uLightColor1.a));
	res += lightContribution(ctx, makePointLight((uViewMatrix * vec4(uLightPos2.xyz, 1.0)).xyz, uLightColor2.rgb * uLightColor2.a));
	res += lightContribution(ctx, makePointLight((uViewMatrix * vec4(uLightPos3.xyz, 1.0)).xyz, uLightColor3.rgb * uLightColor3.a));
	res += material.emissiveFlux;

	if (length(res) > 30.0) {
		res /= length(res);
		res *= 30.0;
	}

	return res;
}

#include [ultraviolet:main/celestial/star.glsl]
#include [ultraviolet:main/celestial/gas_giant.glsl]
#include [ultraviolet:main/celestial/rocky.glsl]
// #include [ultraviolet:main/celestial/brown_dwarf.glsl]

#undef ULTRAVIOLET_CELESTIAL_SHADING_IMPLS_

void main() {
	vec3 vpM = vVertexPosM.xyz;
	vec3 vpW = vVertexPosW.xyz;
	vec3 vpV = vVertexPosV.xyz;
	vec3 vnM = normalize(vVertexNormalM.xyz);
	vec3 vnW = normalize(vVertexNormalW.xyz);
	vec3 vnV = normalize(vVertexNormalV.xyz);
	FragmentInfo fragInfo = FragmentInfo(vpM, vpW, vpV, vnM, vnW, vnV, vpV, vnV);

	vec3 color = vec3(1.0, 0.0, 1.0);
	switch (uNodeType) {
		case NODE_TYPE_STAR:
		case NODE_TYPE_STAR_GIANT:
		case NODE_TYPE_STAR_WHITE_DWARF:
		case NODE_TYPE_STAR_NEUTRON_STAR:
			color = shadeCelestialObject(fragInfo, Star(uRenderingSeed, uStarColor.rgb, uStarColor.a));
			break;
		case NODE_TYPE_STAR_BLACK_HOLE:
			break;
		case NODE_TYPE_BROWN_DWARF:
			break;
		case NODE_TYPE_GAS_GIANT:
			color = shadeCelestialObject(fragInfo, GasGiant(uRenderingSeed, uGasGiantColorGradient));
			break;
		case NODE_TYPE_ICE_WORLD:
			color = shadeCelestialObject(fragInfo, RockyWorld(uRenderingSeed), 0.5);
			break;
		case NODE_TYPE_ROCKY_WORLD:
			color = shadeCelestialObject(fragInfo, RockyWorld(uRenderingSeed), 1.0);
			break;
		case NODE_TYPE_ROCKY_ICE_WORLD:
			color = shadeCelestialObject(fragInfo, RockyWorld(uRenderingSeed), 0.5);
			break;
		case NODE_TYPE_WATER_WORLD:
			break;
		case NODE_TYPE_EARTH_LIKE_WORLD:
			break;
		default:
			break;
	}

    fColor = vec4(color, 1);
}

#endif
