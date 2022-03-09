#version 330
precision mediump float;

uniform mat4 projection;
uniform mat4 modelView;
uniform vec4 defaultColor;

layout(location = 0) in vec2 position;
layout(location = 1) in vec2 vertUV;
layout(location = 2) in int colorIndex;
layout(location = 3) in float overrideColor;
layout(location = 4) in float shadow;

out vec4 color;
out vec2 uv;

const vec4[17] COLORS = vec4[](
    vec4(0.0, 0.0, 0.0, 0.0),
    vec4(0.0, 0.0, 0.0, 1.0),
    vec4(0.0, 0.0, 0.6666, 1.0),
    vec4(0.0, 0.6666, 0.0, 1.0),
    vec4(0.0, 0.6666, 0.6666, 1.0),
    vec4(0.6666, 0.0, 0.0, 1.0),
    vec4(0.6666, 0.0, 0.6666, 1.0),
    vec4(1.0, 0.6666, 0.0, 1.0),
    vec4(0.6666, 0.6666, 0.6666, 1.0),
    vec4(0.3333, 0.3333, 0.3333, 1.0),
    vec4(0.3333, 0.3333, 1.0, 1.0),
    vec4(0.3333, 1.0, 0.3333, 1.0),
    vec4(0.3333, 1.0, 1.0, 1.0),
    vec4(1.0, 0.3333, 0.3333, 1.0),
    vec4(1.0, 0.3333, 1.0, 1.0),
    vec4(1.0, 1.0, 0.3333, 1.0),
    vec4(1.0, 1.0, 1.0, 1.0)
);

void main() {
    gl_Position = projection * modelView * vec4(position, 0.0, 1.0);
    uv = vertUV;

    color = COLORS[colorIndex];
    color.a *= defaultColor.a;
    color += overrideColor * defaultColor;
    color.rgb -= shadow * color.rgb * 0.75;
}