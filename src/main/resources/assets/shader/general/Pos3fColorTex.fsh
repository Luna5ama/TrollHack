#version 450 core

uniform sampler2D texture;

in vec4 color;
in vec2 uv;

out vec4 FragColor;

void main() {
    FragColor = color * texture2D(texture, uv);
}