#version 140

uniform sampler2D DiffuseSampler;

uniform float outlineAlpha;
uniform float filledAlpha;
uniform float width;
uniform float widthSq;
uniform float ratio;

varying vec2 fragCoord;
varying vec2 uv;

void main(){
    vec4 center = texture2D(DiffuseSampler, fragCoord);

    if (center.a == 0.0) {
        // Scan pixels nearby
        int intWidth = int(width);
        int closestDist = 114514;
        vec4 closestColor = center;

        for (int sampleX = -intWidth; sampleX <= intWidth; sampleX++) {
            for (int sampleY = -intWidth; sampleY <= intWidth; sampleY++) {
                int dist = sampleX * sampleX + sampleY * sampleY;

                if (dist > widthSq || dist > closestDist) {
                    continue;
                }

                vec2 sampleCoord = fragCoord + vec2(float(sampleX), float(sampleY)) * uv;
                vec4 result = texture2D(DiffuseSampler, sampleCoord);

                if (result.a > 0.0) {
                    closestDist = dist;
                    closestColor = result;
                }
            }
        }

        if (closestColor.a > 0.0) {
            float scale = 1.0 - sqrt(float(closestDist - 1)) / width;
            center = vec4(closestColor.rgb, scale * ratio * outlineAlpha);
        }
    } else {
        // Replace the color
        center = vec4(center.rgb, filledAlpha);
    }

    gl_FragColor = center;
}
