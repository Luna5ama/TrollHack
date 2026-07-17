#version 410 core

uniform sampler2D InputSampler;

layout(std140) uniform ShaderParams {
    vec2 TargetSize;
    vec2 TexelSize;
    float Quality;
    float LineWidth;
    float OutlineAlpha;
    float FillAlpha;
    float GradientAlpha;
    float Time;
    float GradientFactor;
    float GradientScale;
    float Octaves;
    vec2 Resolution;
};

layout(std140) uniform ShaderColors {
    vec4 Outline;
    vec4 SmokeOutline1;
    vec4 SmokeOutline2;
    vec4 Fill;
    vec4 SmokeFill1;
    vec4 SmokeFill2;
};
in vec2 texCoord;

layout(location = 0) out vec4 fragColor;

vec3 wave(vec2 pos) {
    return mix(Fill.rgb, SmokeFill1.rgb, sin((distance(vec2(0.0), pos) - Time * 60.0) / 60.0) * 0.5 + 0.5);
}

float alphaMask(float alpha) {
    return step(1.0e-6, alpha);
}

float modeMask(float value, float target) {
    return 1.0 - step(1.0e-4, abs(value - target));
}

float outlineFalloff(vec2 offset, float lineWidth, float alpha) {
    float alphaScale = alpha * 255.0;
    float faded = max(0.0, (lineWidth - length(offset)) / max(alphaScale, 1.0e-6));
    return mix(1.0, faded, step(1.0e-6, alphaScale));
}

void main() {
    vec4 centerCol = texture(InputSampler, texCoord);
    int quality = int(Quality);
    int lineWidth = int(LineWidth);
    float alpha0 = OutlineAlpha;
    float fillAlpha = FillAlpha;
    vec2 oneTexel = TexelSize;
    float softMode = modeMask(alpha0, -1.0);

    if (centerCol.a != 0.0) {
        fragColor = vec4(wave(gl_FragCoord.xy), fillAlpha);
    } else {
        float alphaOutline = 0.0;
        float outlineHit = 0.0;
        for (int x = -quality; x < quality; x++) {
            for (int y = -quality; y < quality; y++) {
                vec2 offset = vec2(float(x) + 0.5, float(y) + 0.5);
                vec2 coord = texCoord + offset * oneTexel;
                float sampleHit = alphaMask(texture(InputSampler, coord).a);
                outlineHit += sampleHit;
                alphaOutline += sampleHit * softMode * outlineFalloff(offset, float(lineWidth), Outline.a);
            }
        }

        float hitMask = alphaMask(outlineHit);
        float hardMode = 1.0 - softMode;
        float finalAlpha = alphaOutline + hardMode * alpha0 * hitMask;
        fragColor = vec4(mix(vec3(-1.0), Outline.rgb, hitMask), finalAlpha);
    }
}
