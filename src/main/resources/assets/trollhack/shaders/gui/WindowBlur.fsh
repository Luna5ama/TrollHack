#version 140

uniform sampler2D background;

in vec2 coord0;
in vec4 coords[10];

out vec4 fragColor;

#define WEIGHT_0 0.11046790299233789
#define WEIGHT_1 0.10637649917780685
#define WEIGHT_2 0.09497901712304184
#define WEIGHT_3 0.07860332451562083
#define WEIGHT_4 0.060262548795309304
#define WEIGHT_5 0.04276697011280015
#define WEIGHT_6 0.0280658241365251
#define WEIGHT_7 0.017009590385772787
#define WEIGHT_8 0.009505359333225969
#define WEIGHT_9 0.004888470514230498
#define WEIGHT_10 0.0023084444094977354

void main() {
    fragColor = texture2D(background, coord0) * WEIGHT_0;

    fragColor += texture2D(background, coords[0].xy) * WEIGHT_1;
    fragColor += texture2D(background, coords[0].zw) * WEIGHT_1;

    fragColor += texture2D(background, coords[1].xy) * WEIGHT_2;
    fragColor += texture2D(background, coords[1].zw) * WEIGHT_2;

    fragColor += texture2D(background, coords[2].xy) * WEIGHT_3;
    fragColor += texture2D(background, coords[2].zw) * WEIGHT_3;

    fragColor += texture2D(background, coords[3].xy) * WEIGHT_4;
    fragColor += texture2D(background, coords[3].zw) * WEIGHT_4;

    fragColor += texture2D(background, coords[4].xy) * WEIGHT_5;
    fragColor += texture2D(background, coords[4].zw) * WEIGHT_5;

    fragColor += texture2D(background, coords[5].xy) * WEIGHT_6;
    fragColor += texture2D(background, coords[5].zw) * WEIGHT_6;

    fragColor += texture2D(background, coords[6].xy) * WEIGHT_7;
    fragColor += texture2D(background, coords[6].zw) * WEIGHT_7;

    fragColor += texture2D(background, coords[7].xy) * WEIGHT_8;
    fragColor += texture2D(background, coords[7].zw) * WEIGHT_8;

    fragColor += texture2D(background, coords[8].xy) * WEIGHT_9;
    fragColor += texture2D(background, coords[8].zw) * WEIGHT_9;

    fragColor += texture2D(background, coords[9].xy) * WEIGHT_10;
    fragColor += texture2D(background, coords[9].zw) * WEIGHT_10;
}