#version 150

uniform sampler2D InputSampler;

layout(std140) uniform FxaaInfo {
    vec4 ScreenSize;
};

in vec2 texCoord;
out vec4 fragColor;

float luma(vec3 color) { return dot(color, vec3(0.299, 0.587, 0.114)); }

void main() {
    vec2 inv = ScreenSize.zw;
    vec3 nw = texture(InputSampler, texCoord + vec2(-1.0, -1.0) * inv).rgb;
    vec3 ne = texture(InputSampler, texCoord + vec2(1.0, -1.0) * inv).rgb;
    vec3 sw = texture(InputSampler, texCoord + vec2(-1.0, 1.0) * inv).rgb;
    vec3 se = texture(InputSampler, texCoord + vec2(1.0, 1.0) * inv).rgb;
    vec4 center = texture(InputSampler, texCoord);
    float minL = min(luma(center.rgb), min(min(luma(nw), luma(ne)), min(luma(sw), luma(se))));
    float maxL = max(luma(center.rgb), max(max(luma(nw), luma(ne)), max(luma(sw), luma(se))));
    vec2 dir = vec2(-((luma(nw) + luma(ne)) - (luma(sw) + luma(se))), (luma(nw) + luma(sw)) - (luma(ne) + luma(se)));
    float reduce = max((luma(nw) + luma(ne) + luma(sw) + luma(se)) * (0.25 / 8.0), 1.0 / 128.0);
    dir = clamp(dir / (min(abs(dir.x), abs(dir.y)) + reduce), vec2(-8.0), vec2(8.0)) * inv;
    vec3 a = 0.5 * (texture(InputSampler, texCoord + dir * (-1.0 / 6.0)).rgb + texture(InputSampler, texCoord + dir * (1.0 / 6.0)).rgb);
    vec3 b = a * 0.5 + 0.25 * (texture(InputSampler, texCoord - dir * 0.5).rgb + texture(InputSampler, texCoord + dir * 0.5).rgb);
    fragColor = vec4((luma(b) < minL || luma(b) > maxL) ? a : b, center.a);
}
