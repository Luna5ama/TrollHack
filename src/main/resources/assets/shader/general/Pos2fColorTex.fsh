#version 450 core

uniform sampler2D texture;

in vec4 color;
in vec2 uv;

out vec4 FragColor;

void main() {
    vec4 texColor = texture2D(texture, uv);
    float alpha = texColor.a;
    if (alpha < 0.01) discard;
    FragColor = color * texColor;
}