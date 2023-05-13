#ifndef ULTRAVIOLET_COMMON_UNIFORMS_H_
#define ULTRAVIOLET_COMMON_UNIFORMS_H_

#define CAMERA_TYPE_PERSPECTIVE  0
#define CAMERA_TYPE_ORTHOGRAPHIC 1

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

uniform float uTime;

#endif