#version 140
precision highp float;

uniform mat4 projection;
uniform mat4 modelView;
uniform vec4 defaultColor;

in vec2 position;
in vec4 vertColor;
in vec2 vertUV;

out vec4 color;
out vec2 uv;

const vec4 WHITE = vec4(1.0, 1.0, 1.0, 1.0);

void main() {
    vec4 adjustedVectColor = vertColor.abgr;
    vec4 overriedColor = adjustedVectColor == WHITE ? vec4(defaultColor.rgb, defaultColor.a * adjustedVectColor.a) : vec4(adjustedVectColor.rgb, adjustedVectColor.a * defaultColor.a);

    gl_Position = projection * modelView * vec4(position, 0.0, 1.0);
    uv = vertUV;
    color = overriedColor;
}