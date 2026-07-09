#version 450 core

uniform sampler2D diffuseTex;
uniform sampler2D normalTex;
uniform sampler2D specularTex;
uniform sampler2D heightTex;
uniform vec3 viewPos;
uniform vec3 viewRotation;
uniform vec3 lightColor;
uniform vec3 lightPos;

in vec2 uv;
in vec3 fragPosVec;
in vec3 normaVec;

out vec4 FragColor;

void main() {

    vec2 revUV = vec2(uv.x, (1.0-uv.y));
    vec4 color = texture2D(diffuseTex, revUV);
    vec4 color1 = texture2D(specularTex, revUV);
    float alpha = color.a;
    if (color1.a > color.a) alpha = color1.a;
    float red = color.r;
    float green = color.g;
    float blue = color.b;
    if (color.a != 1.0) {
        red += color1.r / 2.0;
        green +=  color1.g / 2.0;
        blue += color1.b / 2.0;
    }

    vec3 origin = vec3(red, green, blue);

    // Ambient
    float ambientStrength = 0.2f;
    vec3 ambientColor = ambientStrength * lightColor;

    // Diffuse
    vec3 norm = normalize(normaVec);
    vec3 lightDir = normalize(lightPos - fragPosVec);
    float diff = max(dot(norm, lightDir), 0.0);
    vec3 diffColor = diff * lightColor;;

    // Specular
    float specularStrength = 0.5f;
    vec3 viewDir = normalize(viewPos - fragPosVec);
    vec3 reflectDir = reflect(-lightDir, norm);
    float spec = pow(max(dot(viewDir, reflectDir), 0.0), 32);
    vec3 specularColor = specularStrength * spec * lightColor;;

    // Result
    vec3 result = (ambientColor + diffColor + specularColor) * origin;
    if (color.a == 0.0) FragColor = vec4(1.0, 0.0, 0.0, 0.01);
    else FragColor = vec4(result, 1.0);
}