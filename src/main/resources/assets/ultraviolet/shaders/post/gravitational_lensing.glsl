// tonemapping, color grading, etc. stuff that can be combined into a single shader pass.

#stages fragment

#include [hawk:vertex/fullscreen.glsl]

#ifdef IS_FRAGMENT_STAGE
#include [ultraviolet:common_uniforms.glsl]
#include [ultraviolet:lib/constants.glsl]
#include [ultraviolet:lib/raymarch.glsl]

uniform vec3 uCameraFrustumNearNN;
uniform vec3 uCameraFrustumNearNP;
uniform vec3 uCameraFrustumNearPN;
uniform vec3 uCameraFrustumNearPP;
uniform vec3 uCameraFrustumFarNN;
uniform vec3 uCameraFrustumFarNP;
uniform vec3 uCameraFrustumFarPN;
uniform vec3 uCameraFrustumFarPP;

uniform vec3 uAccretionDiscColor;
uniform vec3 uAccretionDiscNormal;
uniform float uAccretionDiscDensityFalloffRadial;
uniform float uAccretionDiscDensityFalloffVerticalInner;
uniform float uAccretionDiscDensityFalloffVerticalOuter;
uniform float uAccretionDiscInnerPercent;
uniform float uAccretionDiscInnerFalloff;
uniform float uAccretionDiscBrightness;
uniform float uEffectLimitFactor;
uniform float uGravitationalConstant;

uniform sampler2D uColorTexture;
uniform sampler2D uDepthTexture;
uniform vec3 uPosition;
uniform float uMass;

out vec4 fColor;

const float sagAMass_Yg = 8.5484e15;

vec3 bendRay(in Ray ray, in vec3 center) {
	float swarzchildRadius = 1e21 * SCHWARZSCHILD_FACTOR_m_PER_kg * uMass / uMetersPerUnit;

	// if we don't divide by the mass, small black holes have no noticeable distortion.
	// We use the mass ratio to SagA* so `uGravitationalConstant` can be somewhat near 1.
	float g = uGravitationalConstant * sagAMass_Yg / uMass;

	// we divide by the radius, otherwise the inverse square for small radius black holes
	// shoots off to very large numbers.
	vec3 toCenter = (center - ray.origin) / swarzchildRadius;

	float strength = g / pow(length(toCenter), 2.0);
	return normalize(toCenter) * strength;
}

// adapted from https://www.youtube.com/watch?v=yhDxBt72PU4
void main() {
	// FIXME: black holes are rendered through objects in foreground
    vec3 color = texture(uColorTexture, texCoord0).rgb;

	vec3 camPosView = vec3(0.0);

	vec3 x0 = mix(uCameraFrustumNearNN, uCameraFrustumNearPN, texCoord0.x);
	vec3 x1 = mix(uCameraFrustumNearNP, uCameraFrustumNearPP, texCoord0.x);
	vec3 y = mix(x0, x1, texCoord0.y);
	Ray viewRayV = Ray(camPosView, normalize(y));

	float swarzchildRadius = 1e21 * SCHWARZSCHILD_FACTOR_m_PER_kg * uMass / uMetersPerUnit;


	vec3 centerV = (uViewMatrix * vec4(uPosition, 1.0)).xyz;

	float effectLimit = uEffectLimitFactor * swarzchildRadius;
	HitInfo hitInfo = raycast(viewRayV, Sphere(centerV, effectLimit));

	// don't raymarch if the influence is too weak to really notice
	if (hitInfo.far > 0.0) {
		Ray rayV = viewRayV;

		// march ray up until the start of the "effect sphere" if we are outside of it.
		rayV.origin += max(0.0, hitInfo.near) * rayV.dir;

		bool hitEventHorizon = false;

		int stepCount = 350;
		float stepSize = 2.0 * effectLimit / float(stepCount);

		// the amount of light the accretion disc has contributed for this ray
		vec3 discContrib = vec3(0.0);

		vec3 discNormal = normalize(uAccretionDiscNormal);
		vec3 discNormalView = (uViewMatrix * vec4(discNormal, 1.0)).xyz;
		Plane discPlane = Plane(centerV, discNormalView);

		int i = 0;
		for (; i < stepCount; ++i) {
			vec3 acceleration = bendRay(rayV, centerV);
			rayV.dir += acceleration * stepSize;
			rayV.dir = normalize(rayV.dir);
			rayV.origin += stepSize * rayV.dir;

			float distanceFromCenter = distance(rayV.origin, centerV);
			if (distanceFromCenter < swarzchildRadius) {
				hitEventHorizon = true;
				break;
			} else if (distanceFromCenter > effectLimit) {
				break;
			}

			if (length(uAccretionDiscColor) < 0.00001)
				continue;

			// TODO: relativistic jets
			// these should maybe be raymarched separately

			// accretion disc
			// TODO: large step sizes quickly cause noticeable banding in the accretion disc
			// TODO: noise texture to give the accretion disc a more broken up look
			float innerAccertionDistance = lerp(uAccretionDiscInnerPercent, swarzchildRadius, effectLimit);
			float t = clamp(invLerp(distanceFromCenter, innerAccertionDistance, effectLimit), 0.0, 1.0);
			t = 1.0 - t;
			t = pow(t, uAccretionDiscDensityFalloffRadial);

			// soft falloff for inner disc cutout
			float innerDiscT = clamp(distanceFromCenter / innerAccertionDistance, 0.0, 1.0);
			innerDiscT = pow(innerDiscT, uAccretionDiscInnerFalloff);
			t *= innerDiscT;

			float planeDistance = abs(sdf(rayV.origin, discPlane));
			
			float vert = mix(uAccretionDiscDensityFalloffVerticalInner, uAccretionDiscDensityFalloffVerticalOuter, t);
			float densityFactor = exp(-vert * planeDistance);

			vec3 col = uAccretionDiscColor;
			col =  t * uAccretionDiscBrightness * col;
			discContrib += col * densityFactor * stepSize;
		}

		// we're just using a simple 2d texture as input for this shader, so we unfortunately don't have any information for what's behind the camera, or off to the side, etc. So we just do something that looks decent, even if it isn't 100% correct.
		vec3 distortedDir = normalize(rayV.origin - viewRayV.origin);
		vec4 projDirC = viewToClip(vec4(distortedDir, 1.0));
		vec2 screenUv = vec2(projDirC.x * 0.5 + 0.5, projDirC.y * 0.5 + 0.5);

		color = texture(uColorTexture, screenUv).rgb;
		if (hitEventHorizon) color = vec3(0.0);
		color += discContrib;

		// float stepRatio = float(i) / float(stepCount);
		// color += 0.1 * stepRatio * vec3(1.0, 0.0, 1.0);
	}

	fColor = vec4(color, 1.0);
}

#endif
