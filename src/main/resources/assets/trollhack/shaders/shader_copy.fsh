#version 410 core

uniform sampler2D InputSampler;

in vec2 texCoord;

layout(location = 0) out vec4 fragColor;

void main() {
    // This pass is intentionally kept: testing showed higher FPS than using a direct texture copy command.
    fragColor = texture(InputSampler, texCoord);
}
