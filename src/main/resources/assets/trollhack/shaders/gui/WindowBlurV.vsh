#version 140

uniform mat4 projection;
uniform mat4 modelView;
uniform mat4 reverseProjection;
uniform vec2 resolution;

in vec3 vertexPos;

out vec2 coord0;
out vec4 coords[10];

void main() {
    vec2 uv = 1.0 / resolution;
    float offset1 = 1.0 * uv.y;
    float offset2 = 2.0 * uv.y;
    float offset3 = 3.0 * uv.y;
    float offset4 = 4.0 * uv.y;
    float offset5 = 5.0 * uv.y;
    float offset6 = 6.0 * uv.y;
    float offset7 = 7.0 * uv.y;
    float offset8 = 8.0 * uv.y;
    float offset9 = 9.0 * uv.y;
    float offset10 = 10.0 * uv.y;

    gl_Position = projection * modelView * vec4(vertexPos.xy, 2000.0, 1.0);

    coord0 = (gl_Position * reverseProjection).xy * uv + 0.5;
    coords[0] = vec4(coord0.x, coord0.y - offset1, coord0.x, coord0.y + offset1);
    coords[1] = vec4(coord0.x, coord0.y - offset2, coord0.x, coord0.y + offset2);
    coords[2] = vec4(coord0.x, coord0.y - offset3, coord0.x, coord0.y + offset3);
    coords[3] = vec4(coord0.x, coord0.y - offset4, coord0.x, coord0.y + offset4);
    coords[4] = vec4(coord0.x, coord0.y - offset5, coord0.x, coord0.y + offset5);
    coords[5] = vec4(coord0.x, coord0.y - offset6, coord0.x, coord0.y + offset6);
    coords[6] = vec4(coord0.x, coord0.y - offset7, coord0.x, coord0.y + offset7);
    coords[7] = vec4(coord0.x, coord0.y - offset8, coord0.x, coord0.y + offset8);
    coords[8] = vec4(coord0.x, coord0.y - offset9, coord0.x, coord0.y + offset9);
    coords[9] = vec4(coord0.x, coord0.y - offset10, coord0.x, coord0.y + offset10);
}