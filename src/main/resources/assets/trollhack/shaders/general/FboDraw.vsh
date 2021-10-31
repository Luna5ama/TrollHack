#version 140
precision highp float;

in vec2 position;
in vec2 vertUV;

out vec2 uv;

void main() {
    gl_Position = vec4(position, 0.0, 1.0);
    uv = vertUV;
}