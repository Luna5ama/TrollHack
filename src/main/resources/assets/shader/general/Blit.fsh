#version 450 core

layout (binding = 0) uniform sampler2DMS texture;
uniform int msaaLevel;
//layout (binding = 2) uniform ivec2 resolution;

in vec2 uv;

out vec4 FragColor;

vec4 calcAverageColor(ivec2 uv) {
    vec4 temp = texelFetch(texture, uv, 0);
    for (int i = 1; i < msaaLevel; i++) {
        temp += texelFetch(texture, uv, i);
    }
    return temp / float(msaaLevel);
}

void main() {
    ivec2 texturePosition = ivec2(gl_FragCoord.x, gl_FragCoord.y);
    FragColor = calcAverageColor(texturePosition);
}