#version 450 core

float Bayer2(vec2 a) {
    a = floor(a);
    return fract(a.x * 0.5 + a.y * 0.75);
}

#define Bayer4(a) (Bayer2(a * 0.5) * 0.25 + Bayer2(a))
#define Bayer8(a) (Bayer4(a * 0.5) * 0.25 + Bayer2(a))

uniform float viewWidth, viewHeight, aspectRatio, strength;
uniform vec3 cameraPosition, previousCameraPosition;
uniform mat4 gbufferPreviousProjection, gbufferProjectionInverse;
uniform mat4 gbufferModelView, gbufferPreviousModelView, gbufferModelViewInverse;

uniform sampler2D colortex0;
uniform sampler2D depthtex1;

in vec2 texCoord;
out vec4 fragColor;

#define MOTION_BLUR_STRENGTH strength

vec3 MotionBlur(vec3 color, float z, float dither) {

    float hand = float(z < 0.56);

    if (hand < 0.5) {
        float mbwg = 0.0;
        vec2 doublePixel = 2.0 / vec2(viewWidth, viewHeight);
        vec3 mblur = vec3(0.0);

        vec4 currentPosition = vec4(texCoord, z, 1.0) * 2.0 - 1.0;

        vec4 viewPos = gbufferProjectionInverse * currentPosition;
        viewPos = gbufferModelViewInverse * viewPos;
        viewPos /= viewPos.w;

        vec3 cameraOffset = cameraPosition - previousCameraPosition;

        vec4 previousPosition = viewPos + vec4(cameraOffset, 0.0);
        previousPosition = gbufferPreviousModelView * previousPosition;
        previousPosition = gbufferPreviousProjection * previousPosition;
        previousPosition /= previousPosition.w;

        vec2 velocity = (currentPosition - previousPosition).xy;
        velocity = velocity / (1.0 + length(velocity)) * MOTION_BLUR_STRENGTH * 0.02;

        vec2 coord = texCoord.st - velocity * (1.5 + dither);
        for (int i = 0; i < 5; i++, coord += velocity) {
            vec2 sampleCoord = clamp(coord, doublePixel, 1.0 - doublePixel);
            float mask = float(texture2D(depthtex1, sampleCoord).r > 0.56);
            mblur += texture2D(colortex0, sampleCoord).rgb * mask;
            mbwg += mask;
        }
        mblur /= max(mbwg, 1.0);

        return mblur;
    }
    else return color;
}

void main() {
    vec3 color = texture2D(colortex0, texCoord).rgb;
    float z = texture2D(depthtex1, texCoord).x;
    float dither = Bayer8(gl_FragCoord.xy);
    color = MotionBlur(color, z, dither);
    fragColor = vec4(color, 1.0f);
//    fragColor = vec4(z, z, z, 1.0f);
}