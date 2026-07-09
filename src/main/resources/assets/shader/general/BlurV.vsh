#version 450 core

uniform mat4 matrix;
uniform mat4 reverseProjection;
uniform vec2 resolution;
uniform vec2 extend;

layout(location = 0) in vec4 vertexPos;

out vec2 coord0;
out vec4 coords[6];

const float OFFSET_0 = 1.4769230769230768;
const float OFFSET_1 = 3.4461538461538463;
const float OFFSET_2 = 5.415384615384616;
const float OFFSET_3 = 7.384615384615384;
const float OFFSET_4 = 9.353846153846154;
const float OFFSET_5 = 11.323076923076922;

const float OFFSETS[6] = float[](
OFFSET_0,
OFFSET_1,
OFFSET_2,
OFFSET_3,
OFFSET_4,
OFFSET_5
);

void calcOffset(int index, vec2 texelSize) {
    float offset = OFFSETS[index] * texelSize.y;
    coords[index] = vec4(coord0.x, coord0.y - offset, coord0.x, coord0.y + offset);
}

void main() {
    vec2 texelSize = 1.0 / resolution;

    gl_Position = matrix * vec4(vertexPos.xy, 2000.0, 1.0);
    gl_Position.xy += extend * 12.0 * texelSize * vertexPos.zw;

    coord0 = (gl_Position * reverseProjection).xy * texelSize + 0.5;

    calcOffset(0, texelSize);
    calcOffset(1, texelSize);
    calcOffset(2, texelSize);
    calcOffset(3, texelSize);
    calcOffset(4, texelSize);
    calcOffset(5, texelSize);
}