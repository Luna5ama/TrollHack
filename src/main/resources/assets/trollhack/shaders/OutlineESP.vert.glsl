#version 460

layout(location = 0) in vec2 vertPos;

out vec2 uv;

void main(){
    gl_Position = vec4(vertPos, 0.0, 1.0);

    uv = vertPos * 0.5 + 0.5;
}
