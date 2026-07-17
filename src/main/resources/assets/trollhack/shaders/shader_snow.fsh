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

float snow(vec2 uv, float scale) {
    float w = smoothstep(1.0, 0.0, -uv.y * (scale / 10.0));
    uv += Time / scale;
    uv.y += Time * 2.0 / scale;
    uv.x += sin(uv.y + Time * 0.5) / scale;
    uv *= scale;
    vec2 s = floor(uv);
    vec2 f = fract(uv);
    vec2 p = vec2(0.0);
    float k = 3.0;
    float d;
    p = 0.5 + 0.35 * sin(11.0 * fract(sin((s + p + scale) * mat2(7, 3, 6, 5)) * 5.0)) - f;
    d = length(p);
    k = min(d, k);
    k = smoothstep(0.0, k, sin(f.x + f.y) * 0.01);
    return k * w * smoothstep(0.08, 0.1, w);
}

float alphaMask(float alpha) {
    return step(1.0e-6, alpha);
}

float glowFalloff(vec2 offset, float maxSample, float divider) {
    float faded = max(0.0, (maxSample - length(offset)) / max(divider, 1.0e-6));
    return mix(1.0, faded, step(1.0e-6, divider));
}

float glowShader() {
    float divider = 158.0;
    float maxSample = 10.0;
    vec2 resolution = Resolution;
    float quality = Quality;
    vec2 texelSize = vec2(1.0 / resolution.x * quality, 1.0 / resolution.y * quality);
    float alpha = 0.0;

    for (float x = -quality; x < quality; x++) {
        for (float y = -quality; y < quality; y++) {
            vec2 offset = vec2(x + 0.5, y + 0.5);
            vec4 currentColor = texture(InputSampler, texCoord + vec2(texelSize.x * offset.x, texelSize.y * offset.y));
            alpha += alphaMask(currentColor.a) * glowFalloff(offset, maxSample, divider);
        }
    }
    return alpha;
}

void main() {
    vec4 centerCol = texture(InputSampler, texCoord);
    vec2 resolution = Resolution;
    vec2 uv = (gl_FragCoord.xy * 2.0 - resolution.xy) / min(resolution.x, resolution.y);
    vec3 finalColor = vec3(0.0);
    float c = smoothstep(1.0, 0.3, clamp(uv.y * 0.3 + 0.8, 0.0, 0.75));
    c += snow(uv, 10.0);
    c += snow(uv, 8.0);
    c += snow(uv, 6.0);
    c += snow(uv, 5.0);

    float alpha = centerCol.a != 0.0 ? Fill.a : glowShader();
    fragColor = vec4(vec3(c) * Fill.rgb, alpha);
}
