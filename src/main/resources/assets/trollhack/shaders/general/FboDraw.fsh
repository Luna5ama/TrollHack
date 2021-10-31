#version 140
precision highp float;

uniform sampler2D texture;
uniform sampler2D depthTexture;

in vec2 uv;

out vec4 fragColor;

void main() {
    fragColor = texture2D(texture, uv);
    if (fragColor.a == 0.0) discard;
    gl_FragDepth = texture2D(depthTexture, uv).r;
}