#version 450 core

layout (binding = 0) uniform sampler2D texture;

in vec4 color;
in vec2 uv;

out vec4 FragColor;

void main() {
    if (uv.x <= -200000.0) discard;
    else if (uv.x >= -100000.0) FragColor = color * texture2D(texture, uv);
    else FragColor = color;
}