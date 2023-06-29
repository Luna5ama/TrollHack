#version 460

uniform mat4 projection;
uniform mat4 modelView;
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

void main() {
    vec2 uv = 1.0 / resolution;

    gl_Position = projection * modelView * vec4(vertexPos.xy, 2000.0, 1.0);
    gl_Position.xy += extend * 12.0 * uv * vertexPos.zw;

    coord0 = (gl_Position * reverseProjection).xy * uv + 0.5;

    #define CALC_OFFSET(i) float offset##i## = OFFSET_##i## * uv.x;\
        coords[##i##] = vec4(coord0.x - offset##i##, coord0.y, coord0.x + offset##i##, coord0.y);

    CALC_OFFSET(0)
    CALC_OFFSET(1)
    CALC_OFFSET(2)
    CALC_OFFSET(3)
    CALC_OFFSET(4)
    CALC_OFFSET(5)
}