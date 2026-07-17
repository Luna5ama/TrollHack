#version 150

uniform sampler2D InputSampler;

layout(std140) uniform FilterColor {
    vec4 TintColor;
};

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec4 sceneColor = texture(InputSampler, texCoord);
    fragColor = vec4(mix(sceneColor.rgb, TintColor.rgb, clamp(TintColor.a, 0.0, 1.0)), sceneColor.a);
}
