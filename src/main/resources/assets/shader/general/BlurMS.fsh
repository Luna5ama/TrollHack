#version 450 core

uniform sampler2DMS background;
uniform vec2 resolution;
uniform int msaaSample;

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

const float WEIGHTS[6] = float[6](
WEIGHT_0,
WEIGHT_1,
WEIGHT_2,
WEIGHT_3,
WEIGHT_4,
WEIGHT_5
);

ivec2 calcPixelUV(vec2 uv) {
    vec2 temp = uv.xy * resolution.xy;
    return ivec2(int(temp.x), int(temp.y));
}

vec4 calcAverageColor(ivec2 uv) {
    vec4 temp = texelFetch(background, uv, 0);
    for (int i = 1; i < msaaSample; i++) {
        temp += texelFetch(background, uv, i);
    }
    return temp / float(msaaSample);
}

void calcSample(int index) {
    fragColor += calcAverageColor(calcPixelUV(coords[index].xy)) * WEIGHTS[index];
    fragColor += calcAverageColor(calcPixelUV(coords[index].xw)) * WEIGHTS[index];
}

void main() {
    fragColor = calcAverageColor(calcPixelUV(coord0)) * WEIGHT_C;

    calcSample(0);
    calcSample(1);
    calcSample(2);
    calcSample(3);
    calcSample(4);
    calcSample(5);
}