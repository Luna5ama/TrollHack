#version 450 core

layout (location = 0) in vec3 position;
layout (location = 1) in vec2 texCoords;
layout (location = 2) in vec3 normal;

uniform mat4 matrix;
uniform vec3 viewRotation;

out vec2 uv;
out vec3 fragPosVec;
out vec3 normaVec;

//float calcAngleDiff(vec3 a, vec3 b) {
//    float radiansDelta = acos(dot(a, b) / (a.length() * b.length()));
//    return radiansDelta / 3.14159 * 180;
//}

void main() {
//    if (calcAngleDiff(normal, viewRotation) > 90) {
//        gl_ClipDistance = -1.0;
//    } else gl_ClipDistance = 1.0;
//    gl_CullDistance[0] = -dot(viewRotation, normal);
    gl_Position = matrix * vec4(position + vec3(1.5 * floor(gl_InstanceID / 100), 0, 1.5 * (gl_InstanceID % 100)), 1.0);
    uv = texCoords;
    fragPosVec = position;
    normaVec = normal;
}