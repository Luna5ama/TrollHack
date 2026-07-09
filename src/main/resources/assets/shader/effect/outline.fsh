#version 450

layout(binding = 0) uniform sampler2D DiffuseSampler;

in vec2 texCoord;
in vec2 oneTexel;

out vec4 fragColor;

#define outlinecolor vec4(1, 0, 0, 1)
#define lineWidth 12
#define quality 2
#define alpha0 1.0

float getGray(vec4 color) {
    float R = color.r;
    float G = color.g;
    float B = color.b;
    return R*0.3+G*0.59+B*0.11;
}

void main() {
    vec4 centerCol = texture(DiffuseSampler, texCoord);

    if (centerCol.a != 0) {
        fragColor = vec4(1, 0, 0, 0.5);
    } else {
        float alphaOutline = 0;
        vec3 colorFinal = vec3(-1);
        for (int x = -quality; x < quality; x++) {
            for (int y = -quality; y < quality; y++) {
                vec2 offset = vec2(x, y);
                vec2 coord = texCoord + offset * oneTexel;
                vec4 t = texture(DiffuseSampler, coord);
                if (t.a != 0){
                    if (alpha0 == -1.0) {
                        if (colorFinal[0] == -1) {
                            colorFinal = outlinecolor.rgb;
                        }
                        alphaOutline += outlinecolor.a * 255.0 > 0 ? max(0, (lineWidth - distance(vec2(x, y), vec2(0))) / (outlinecolor.a * 255.0)) : 1;
                    }
                    else {
                        fragColor = vec4(outlinecolor.rgb, alpha0);
                        return;
                    }
                }
            }
        }
        fragColor = vec4(colorFinal, alphaOutline);
    }
//    vec4 center = texture(DiffuseSampler, texCoord);
//    vec4 left = texture(DiffuseSampler, texCoord - vec2(oneTexel.x, 0.0));
//    vec4 right = texture(DiffuseSampler, texCoord + vec2(oneTexel.x, 0.0));
//    vec4 up = texture(DiffuseSampler, texCoord - vec2(0.0, oneTexel.y));
//    vec4 down = texture(DiffuseSampler, texCoord + vec2(0.0, oneTexel.y));
//    float leftDiff  = abs(getGray(center) - getGray(left));
//    float rightDiff = abs(getGray(center) - getGray(right));
//    float upDiff    = abs(getGray(center) - getGray(up));
//    float downDiff  = abs(getGray(center) - getGray(down));
//    float total = clamp((leftDiff + rightDiff + upDiff + downDiff), 0.0, 1.0);
//    if (total > 0.000000000000001) discard;
//    vec3 outColor = center.rgb * center.a + left.rgb * left.a + right.rgb * right.a + up.rgb * up.a + down.rgb * down.a;
//    outColor = vec3(1, 0, 0);
//    discard;
//    fragColor = vec4(outColor, 1);
}
