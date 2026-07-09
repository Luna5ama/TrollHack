#version 450

in vec2 Position;
in vec4 color;

layout(location = 0) uniform mat4 ProjMat;
layout(location = 1) uniform vec2 InSize;
layout(location = 2) uniform vec2 OutSize;

out vec2 texCoord;
out vec2 oneTexel;

void main(){
    vec4 outPos = ProjMat * vec4(Position, 0.0, 1.0);
    gl_Position = vec4(outPos.xy, 0.2, 1.0);

    oneTexel = 1.0 / InSize;

    texCoord = Position.xy / OutSize;
}
