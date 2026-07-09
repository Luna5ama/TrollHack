#version 450 core

uniform sampler2D tex_y;
uniform sampler2D tex_uv;

in vec2 texCoord;

out vec4 FragColor;

void main()
{
	vec3 yuv = vec3(0.f);

    yuv.x = texture2D(tex_y, texCoord).r;
    yuv.y = texture2D(tex_uv, texCoord).r - 0.5;
    yuv.z = texture2D(tex_uv, texCoord).g - 0.5;
//    yuv = clamp(yuv, 0.f, 1.f);
//    mat3 yuv_to_rgb = mat3(1.164f, 1.164f,  1.164f,
//                           0.f,    -0.213f, 2.112f,
//                           1.793f, -0.533f, 0.f);


    vec3 rgb = mat3(1,       1,        1,
                    0,       -0.39465, 2.03211,
                    1.13983, -0.58060, 0       ) * yuv;
    FragColor = vec4(rgb, 1.f);
}