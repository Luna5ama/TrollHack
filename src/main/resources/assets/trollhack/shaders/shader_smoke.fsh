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

float random(vec2 st) {
    return fract(sin(dot(st.xy, vec2(12.9898, 78.233))) * 43758.5453123);
}

float noise(vec2 st) {
    vec2 i = floor(st);
    vec2 f = fract(st);
    float a = random(i);
    float b = random(i + vec2(1.0, 0.0));
    float c = random(i + vec2(0.0, 1.0));
    float d = random(i + vec2(1.0, 1.0));
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(a, b, u.x) + (c - a) * u.y * (1.0 - u.x) + (d - b) * u.x * u.y;
}

float fbm(vec2 st) {
    float v = 0.0;
    float a = 0.5;
    vec2 shift = vec2(100.0);
    mat2 rot = mat2(cos(0.5), sin(0.5), -sin(0.5), cos(0.50));
    for (int i = 0; i < int(Octaves); ++i) {
        v += a * noise(st);
        st = rot * st * 2.0 + shift;
        a *= 0.5;
    }
    return v;
}

vec3 getColor() {
    vec2 resolution = Resolution;
    vec2 st = gl_FragCoord.xy / resolution.xy * 3.0;
    vec3 color = vec3(0.0);
    vec2 q = vec2(0.0);
    q.x = fbm(st);
    q.y = fbm(st + vec2(1.0));
    vec2 r = vec2(0.0);
    r.x = fbm(st + 1.0 * q + vec2(1.7, 9.2) + 0.15 * Time);
    r.y = fbm(st + 1.0 * q + vec2(8.3, 2.8) + 0.126 * Time);
    float f = fbm(st + r);
    color = Outline.rgb;
    color = mix(color, SmokeOutline1.rgb, clamp(length(q), 0.0, 1.0));
    color = mix(color, SmokeOutline2.rgb, clamp(length(r.x), 0.0, 1.0));
    vec4 outputColor = vec4((f * f * f + 0.6 * f * f + 0.5 * f) * color, Outline.a);
    return outputColor.rgb;
}

vec3 getFillColor() {
    vec2 resolution = Resolution;
    vec2 st = gl_FragCoord.xy / resolution.xy * 3.0;
    vec3 color = vec3(0.0);
    vec2 q = vec2(0.0);
    q.x = fbm(st);
    q.y = fbm(st + vec2(1.0));
    vec2 r = vec2(0.0);
    r.x = fbm(st + 1.0 * q + vec2(1.7, 9.2) + 0.15 * Time);
    r.y = fbm(st + 1.0 * q + vec2(8.3, 2.8) + 0.126 * Time);
    float f = fbm(st + r);
    color = Fill.rgb;
    color = mix(color, SmokeFill1.rgb, clamp(length(q), 0.0, 1.0));
    color = mix(color, SmokeFill2.rgb, clamp(length(r.x), 0.0, 1.0));
    vec4 outputColor = vec4((f * f * f + 0.6 * f * f + 0.5 * f) * color, Fill.a);
    return outputColor.rgb;
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
    float alpha1 = FillAlpha;
    vec2 oneTexel = TexelSize;
    float softMode = modeMask(alpha0, -1.0);

    if (centerCol.a != 0.0) {
        fragColor = vec4(getFillColor(), alpha1);
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

        if (outlineHit != 0.0) {
            fragColor = vec4(getColor(), finalAlpha);
        } else {
            fragColor = vec4(vec3(-1.0), 0.0);
        }
    }
}
