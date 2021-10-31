#version 330
precision highp float;

uniform mat4 projection;
uniform mat4 modelView;
uniform float alpha;
uniform float partialTicks;

layout(location = 0) in vec3 modelPosition;
layout(location = 1) in vec2 vertUV;
layout(location = 2) in vec3 vertNormal;
layout(location = 3) in int vertGroupID;


layout(location = 4) in vec3 renderPosition;
layout(location = 5) in vec2 vertLightMapUV;

layout(location = 6) in int rotationY;
layout(location = 7) in float prevLidAngle;
layout(location = 8) in float lidAngle;

out vec2 uv;
flat out vec3 normal;
out vec2 lightMapUV;

const vec3 rotationPointOffset = vec3(0.0, 0.5625, -0.4375);
const float angleMultiplier = 1.57079637050628662109375;

mat3 rotateX(mat3 matrix, float angle) {
    float sin = sin(angle);
    float cos = cos(angle);

    float rm11 = cos;
    float rm21 = -sin;
    float rm12 = sin;
    float rm22 = cos;

    float nm10 = matrix[1][0] * rm11 + matrix[2][0] * rm12;
    float nm11 = matrix[1][1] * rm11 + matrix[2][1] * rm12;
    float nm12 = matrix[1][2] * rm11 + matrix[2][2] * rm12;

    matrix[2][0] = matrix[1][0] * rm21 + matrix[2][0] * rm22;
    matrix[2][1] = matrix[1][1] * rm21 + matrix[2][1] * rm22;
    matrix[2][2] = matrix[1][2] * rm21 + matrix[2][2] * rm22;

    matrix[1][0] = nm10;
    matrix[1][1] = nm11;
    matrix[1][2] = nm12;

    return matrix;
}

mat3 rotateY(mat3 matrix, float angle) {
    float sin = sin(angle);
    float cos = cos(angle);

    float rm00 = cos;
    float rm20 = sin;
    float rm02 = -sin;
    float rm22 = cos;

    float nm00 = matrix[0][0] * rm00 + matrix[2][0] * rm02;
    float nm01 = matrix[0][1] * rm00 + matrix[2][1] * rm02;
    float nm02 = matrix[0][2] * rm00 + matrix[2][2] * rm02;

    matrix[2][0] = matrix[0][0] * rm20 + matrix[2][0] * rm22;
    matrix[2][1] = matrix[0][1] * rm20 + matrix[2][1] * rm22;
    matrix[2][2] = matrix[0][2] * rm20 + matrix[2][2] * rm22;

    matrix[0][0] = nm00;
    matrix[0][1] = nm01;
    matrix[0][2] = nm02;

    return matrix;
}

mat3 rotateZ(mat3 matrix, float angle) {
    float sin = sin(angle);
    float cos = cos(angle);

    float rm00 = cos;
    float rm10 = -sin;
    float rm01 = sin;
    float rm11 = cos;

    float nm00 = matrix[0][0] * rm00 + matrix[1][0] * rm01;
    float nm01 = matrix[0][1] * rm00 + matrix[1][1] * rm01;
    float nm02 = matrix[0][2] * rm00 + matrix[1][2] * rm01;

    matrix[1][0] = matrix[0][0] * rm10 + matrix[1][0] * rm11;
    matrix[1][1] = matrix[0][1] * rm10 + matrix[1][1] * rm11;
    matrix[1][2] = matrix[0][2] * rm10 + matrix[1][2] * rm11;

    matrix[0][0] = nm00;
    matrix[0][1] = nm01;
    matrix[0][2] = nm02;

    return matrix;
}

mat3 rotateX90(mat3 matrix, int angle) {
    return rotateX(matrix, angle * angleMultiplier);
}

mat3 rotateY90(mat3 matrix, int angle) {
    return rotateY(matrix, angle * angleMultiplier);
}

mat3 rotateZ90(mat3 matrix, int angle) {
    return rotateZ(matrix, angle * angleMultiplier);
}

mat3 rotateX90(mat3 matrix, float angle) {
    return rotateX(matrix, angle * angleMultiplier);
}

mat3 rotateY90(mat3 matrix, float angle) {
    return rotateY(matrix, angle * angleMultiplier);
}

mat3 rotateZ90(mat3 matrix, float angle) {
    return rotateZ(matrix, angle * angleMultiplier);
}

void main() {
    vec3 position = modelPosition;
    normal = vertNormal;

    if (vertGroupID != 0) {
        float renderLidAngle = mix(prevLidAngle, lidAngle, partialTicks);
        renderLidAngle = 1.0 - renderLidAngle;
        renderLidAngle = 1.0 - renderLidAngle * renderLidAngle * renderLidAngle;

        mat3 lidMatrix = mat3(1.0);
        lidMatrix = rotateX90(lidMatrix, renderLidAngle);

        position -= rotationPointOffset;
        position = position * lidMatrix;
        position += rotationPointOffset;

        normal = normal * lidMatrix;
    }

    mat3 rotationMatrix = mat3(1.0);
    rotationMatrix = rotateY90(rotationMatrix, rotationY);

    gl_Position = projection * modelView * vec4(position * rotationMatrix + renderPosition, 1.0);
    uv = vertUV;
    normal = normal * rotationMatrix;
    lightMapUV = vertLightMapUV * 0.99609375 + 0.03125;
}