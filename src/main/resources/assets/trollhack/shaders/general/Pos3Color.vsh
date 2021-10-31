#version 120

attribute vec3 position;
attribute vec4 vertColor;

varying vec4 color;

void main() {
    gl_Position = gl_ModelViewProjectionMatrix * vec4(position, 1.0);
    color = vertColor.abgr;
}