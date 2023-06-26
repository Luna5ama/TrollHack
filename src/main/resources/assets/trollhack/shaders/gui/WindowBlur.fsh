#version 460

uniform sampler2D background;

in vec2 coord0;
in vec4 coords[6];

out vec4 fragColor;

const float WEIGHT_C = 0.09950225481157278;
const float WEIGHT_0 = 0.18446050802858416;
const float WEIGHT_1 = 0.13614942259252638;
const float WEIGHT_2 = 0.07862968075756147;
const float WEIGHT_3 = 0.03538335634090266;
const float WEIGHT_4 = 0.01232869558916469;
const float WEIGHT_5 = 0.00329720928547428;

void main() {
    fragColor = texture2D(background, coord0) * WEIGHT_C;

    #define CALC_SAMPLE(i) fragColor += texture2D(background, coords[##i].xy) * WEIGHT_##i;\
        fragColor += texture2D(background, coords[##i].zw) * WEIGHT_##i;

    CALC_SAMPLE(0)
    CALC_SAMPLE(1)
    CALC_SAMPLE(2)
    CALC_SAMPLE(3)
    CALC_SAMPLE(4)
    CALC_SAMPLE(5)
}