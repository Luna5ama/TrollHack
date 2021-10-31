#version 140
precision highp float;

uniform mat4 projection;
uniform mat4 modelView;
uniform float alpha;

in vec3 position;
in vec4 vertColor;

out vec4 color;

void main() {
    gl_Position = projection * modelView * vec4(position, 1.0);
    color = vec4(vertColor.abg, vertColor.r * alpha);
}