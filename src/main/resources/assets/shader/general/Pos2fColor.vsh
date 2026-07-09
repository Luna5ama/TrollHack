#version 450 core

layout (location = 0) in vec2 position;
layout (location = 1) in vec4 vertColor;

uniform mat4 matrix;

out vec4 color;

void main() {
    gl_Position = matrix * vec4(position, 0.0, 1.0);
    color = vertColor.abgr;
}