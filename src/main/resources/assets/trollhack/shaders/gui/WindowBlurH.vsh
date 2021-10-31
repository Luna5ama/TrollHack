#version 140
precision highp float;

uniform mat4 projection;
uniform mat4 modelView;
uniform mat4 reverseProjection;
uniform vec2 resolution;

in vec3 vertexPos;

out vec2 coord0;
out vec4 coord1;
out vec4 coord2;
out vec4 coord3;
out vec4 coord4;
out vec4 coord5;
out vec4 coord6;
out vec4 coord7;
out vec4 coord8;
out vec4 coord9;
out vec4 coord10;

void main() {
    vec2 uv = 1.0 / resolution;
    float offset1 = 1.0 * uv.x;
    float offset2 = 2.0 * uv.x;
    float offset3 = 3.0 * uv.x;
    float offset4 = 4.0 * uv.x;
    float offset5 = 5.0 * uv.x;
    float offset6 = 6.0 * uv.x;
    float offset7 = 7.0 * uv.x;
    float offset8 = 8.0 * uv.x;
    float offset9 = 9.0 * uv.x;
    float offset10 = 10.0 * uv.x;

    gl_Position = projection * modelView * vec4(vertexPos.xy, 2000.0, 1.0);
    gl_Position.y += 10.0 * uv.y * vertexPos.z;

    coord0 = (gl_Position * reverseProjection).xy * uv + 0.5;
    coord1 = vec4(coord0.x - offset1, coord0.y, coord0.x + offset1, coord0.y);
    coord2 = vec4(coord0.x - offset2, coord0.y, coord0.x + offset2, coord0.y);
    coord3 = vec4(coord0.x - offset3, coord0.y, coord0.x + offset3, coord0.y);
    coord4 = vec4(coord0.x - offset4, coord0.y, coord0.x + offset4, coord0.y);
    coord5 = vec4(coord0.x - offset5, coord0.y, coord0.x + offset5, coord0.y);
    coord6 = vec4(coord0.x - offset6, coord0.y, coord0.x + offset6, coord0.y);
    coord7 = vec4(coord0.x - offset7, coord0.y, coord0.x + offset7, coord0.y);
    coord7 = vec4(coord0.x - offset8, coord0.y, coord0.x + offset8, coord0.y);
    coord7 = vec4(coord0.x - offset9, coord0.y, coord0.x + offset9, coord0.y);
    coord7 = vec4(coord0.x - offset10, coord0.y, coord0.x + offset10, coord0.y);
}