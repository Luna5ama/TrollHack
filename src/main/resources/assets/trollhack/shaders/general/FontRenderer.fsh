#version 140
precision highp float;

uniform sampler2D texture;

in vec4 color;
in vec2 uv;

out vec4 fragColor;

void main() {
    float alpha = texture2D(texture, uv).r;
    if (alpha <= 0.002) discard;
    fragColor = vec4(color.rgb, alpha * color.a);
}