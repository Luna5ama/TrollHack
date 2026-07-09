#version 450 core

layout (location = 0) in vec3 vertex_pos;
layout (location = 1) in vec2 tex_pos;

uniform mat4 matrix;

out vec2 texCoord;

void main()
{
    gl_Position = matrix * vec4(vertex_pos, 1.f);
    texCoord = vec2(tex_pos);
}