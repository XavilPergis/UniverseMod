#ifndef ULTRAVIOLET_COMMON_UNIFORMS_H_
#define ULTRAVIOLET_COMMON_UNIFORMS_H_

#define CAMERA_TYPE_PERSPECTIVE  0
#define CAMERA_TYPE_ORTHOGRAPHIC 1

uniform  mat4 uModelMatrix;
uniform  mat4 uViewMatrix;
uniform  mat4 uProjectionMatrix;
uniform  mat4 uViewProjectionMatrix;
uniform float uCameraNear;
uniform float uCameraFar;
uniform float uCameraFov;
uniform float uMetersPerUnit;
uniform   int uCameraType;

#define uCameraPos  uViewMatrix[3].xyz
#define uCameraNegX (-uViewMatrix[0].xyz)
#define uCameraPosX ( uViewMatrix[0].xyz)
#define uCameraNegY (-uViewMatrix[1].xyz)
#define uCameraPosY ( uViewMatrix[1].xyz)
#define uCameraNegZ (-uViewMatrix[2].xyz)
#define uCameraPosZ ( uViewMatrix[2].xyz)

uniform vec2 uScreenSize;

uniform float uTime;

vec4 clipToView(in vec4 posC)  { vec4 res = inverse(uProjectionMatrix) * posC; return res / res.w; }
vec4 viewToWorld(in vec4 posV) { return inverse(uViewMatrix) * posV; }
vec4 viewToClip(in vec4 posV)  { vec4 res = uProjectionMatrix * posV; return res / res.w; }
vec4 worldToView(in vec4 posW) { return uViewMatrix * posW; }

float linearizeDepth(float d, float zNear, float zFar) {
    float z_n = 2.0 * d - 1.0;
    return 2.0 * zNear * zFar / (zFar + zNear - z_n * (zFar - zNear));
}

#endif