#version 140

attribute vec4 Position;

uniform mat4 ProjMat;
uniform vec2 InSize;
uniform vec2 OutSize;

varying vec2 fragCoord;
varying vec2 uv;

void main(){
    vec4 outPos = ProjMat * vec4(Position.xy, 0.0, 1.0);
    gl_Position = vec4(outPos.xy, 0.2, 1.0);

    fragCoord = Position.xy / OutSize;
    fragCoord.y = 1.0 - fragCoord.y;

    uv = 1.0 / InSize;
}
