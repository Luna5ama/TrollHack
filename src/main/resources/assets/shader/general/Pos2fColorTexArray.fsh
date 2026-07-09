#version 450
#extension GL_EXT_texture_array : enable

uniform sampler2DArray texture;

in vec4 color;
in vec3 uv;

out vec4 FragColor;

void main() {
    vec4 texColor = texture2DArray(texture, vec3(uv.xy, floor(uv.z)));
    FragColor = color * texColor;
}