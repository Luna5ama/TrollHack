#version 460

layout(binding = 0) uniform sampler2D inputTexture;
layout(location = 0) uniform vec2 onePixel;
layout(location = 1) uniform float outlineAlpha;
layout(location = 2) uniform float filledAlpha;
layout(location = 3) uniform float outlineWidth;

in vec2 uv;

const vec2 offset1 = vec2(-1.5, -1.5);
const vec2 offset2 = vec2(-1.5, 1.5);
const vec2 offset3 = vec2(1.5, -1.5);
const vec2 offset4 = vec2(1.5, 1.5);

const vec2 offset5 = vec2(-1.5, 0.0);
const vec2 offset6 = vec2(1.5, 0.0);
const vec2 offset7 = vec2(0.0, -1.5);
const vec2 offset8 = vec2(0.0, 1.5);

out vec4 fragColor;

void main(){
    vec4 center = texture2D(inputTexture, uv);

    vec4 c1 = texture2D(inputTexture, onePixel * offset1 * outlineWidth + uv) * 2.0;
    vec4 c2 = texture2D(inputTexture, onePixel * offset2 * outlineWidth + uv) * 2.0;
    vec4 c3 = texture2D(inputTexture, onePixel * offset3 * outlineWidth + uv) * 2.0;
    vec4 c4 = texture2D(inputTexture, onePixel * offset4 * outlineWidth + uv) * 2.0;

    vec4 c5 = texture2D(inputTexture, onePixel * offset5 * outlineWidth + uv);
    vec4 c6 = texture2D(inputTexture, onePixel * offset6 * outlineWidth + uv);
    vec4 c7 = texture2D(inputTexture, onePixel * offset7 * outlineWidth + uv);
    vec4 c8 = texture2D(inputTexture, onePixel * offset8 * outlineWidth + uv);

    float alphaSumOutline = c1.a + c2.a + c3.a + c4.a + c5.a + c6.a + c7.a + c8.a;
    float alphaSum = alphaSumOutline + center.a;

    float outlineSum = clamp(alphaSumOutline * 0.08333 - center.a, 0.0, 1.0);
    vec3 averageColor =
        center.rgb * center.a +
        c1.rgb * c1.a +
        c2.rgb * c2.a +
        c3.rgb * c3.a +
        c4.rgb * c4.a +
        c5.rgb * c5.a +
        c6.rgb * c6.a +
        c7.rgb * c7.a +
        c8.rgb * c8.a;

    averageColor /= alphaSum;

    fragColor = vec4(averageColor, step(0.9, alphaSum) * mix(filledAlpha, outlineAlpha, outlineSum * 2.0));
}
