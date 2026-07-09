#version 450 core

uniform sampler2DArray tex;

in vec4 color;
in vec2 uv;
flat in int z;
out vec4 FragColor;

void main() {
    FragColor = color * texture(tex, vec3(uv, z));
}