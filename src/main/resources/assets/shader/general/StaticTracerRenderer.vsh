#version 140

uniform mat4 mvp;
uniform float alpha;

in vec3 position;
in vec4 vertColor;

out vec4 color;

void main() {
    gl_Position = gl_VertexID % 2 == 0 ? vec4(0.0, 0.0, 0.0, 1.0) : mvp * vec4(position, 1.0);
    color = vec4(vertColor.abg, vertColor.r * alpha);
}