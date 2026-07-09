#version 450 core

uniform sampler2D texture;

in vec2 uv;

out vec4 FragColor;

void main() {
    FragColor = vec4(1.0,0.0,0.0,1.0);//texture2D(texture, uv);
}