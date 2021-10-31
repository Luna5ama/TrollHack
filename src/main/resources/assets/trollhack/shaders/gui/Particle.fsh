#version 140
precision lowp float;

uniform float time;
uniform vec2 mouse;
uniform vec2 resolution;

float lengthSq(vec2 diff) {
    return diff.x * diff.x + diff.y * diff.y;
}

float distSq(vec2 a, vec2 b) {
    vec2 diff = a - b;
    return diff.x * diff.x + diff.y * diff.y;
}

float distLine(vec2 p, vec2 a, vec2 b) {
    vec2 pa = p - a;
    vec2 ba = b - a;
    float t = clamp(dot(pa, ba) / dot(ba, ba), 0.0, 1.0);

    return length(pa - ba * t);
}

float n21(vec2 p) {
    p = fract(p * vec2(383.12, 154.53));
    p += dot(p, p + 43.53);

    return fract(p.x * p.y);
}

vec2 n22(vec2 p) {
    float n = n21(p);
    return vec2(n, n21(p + n));
}

vec2 getPos(vec2 id, vec2 offset) {
    vec2 n = n22(id + offset) * time;
    return offset + sin(n) * 0.5;
}

float line(vec2 id, vec2 m, vec2 p, vec2 a, vec2 b) {
    float d3 = lengthSq(a - b);

    if (d3 > 1.0) {
        return 0.0;
    }

    float distA = distSq(id + 0.5 + a, m);
    float distB = distSq(id + 0.5 + b, m);
    float distM = min(distA, distB);

    float d2 = distLine(p, a, b);

    float lol = smoothstep(0.0025, 0.0, d2);
    lol *= smoothstep(1.6, 0.9, d3) + smoothstep(0.05, 0.03, abs(d3 - 0.75));

    if (distM < 0.07) {
        float sus = distM * 4.0;
        return lol * 0.5 * (1.0 - sus);
    } else {
        float sus = min(distM, 1.0);
        float amogus = max(1.0 - sus * 0.35, 0.0);
        return min(lol * 0.15, 0.1) * sus * amogus;
    }
}

float layer(vec2 uv, vec2 m) {
    vec2 gv = fract(uv) - 0.5;
    vec2 id = floor(uv);
    vec2 center = getPos(id, vec2(0.0));

    float result;
    float fuck = distance(id + 0.5 + center, m);
    bool a = fuck < 1.0;

    for (int x = -1; x <= 1; x++) {
        for (int y = -1; y <= 1; y++) {
            vec2 point = getPos(id, vec2(x, y));
            result += line(id, m, gv, center, point);

            vec2 d = (point - gv);
            float sparkle = 0.000025 / dot(d, d);

            result += min(pow(sparkle, 4.0), 1.0);
        }
    }

    result += line(id, m, gv, getPos(id, vec2(-1, 0)), getPos(id, vec2(0, 1)));
    result += line(id, m, gv, getPos(id, vec2(0, 1)), getPos(id, vec2(1, 0)));
    result += line(id, m, gv, getPos(id, vec2(1, 0)), getPos(id, vec2(0, -1)));
    result += line(id, m, gv, getPos(id, vec2(0, -1)), getPos(id, vec2(-1, 0)));

    return min(result, 1.0);
}

mat2 rotate2d(float angle) {
    float s = sin(angle);
    float c = cos(angle);

    return mat2(c, -s, s, c);
}

vec2 transform(vec2 a) {
    vec2 scaled = (a - resolution.xy * 0.4) / resolution.y;
    return scaled * 0.25;
}

void main() {
    vec2 uv = transform(gl_FragCoord.xy);
    vec2 m = transform(mouse * resolution);

    float sum = 0.0;

    for (float i = 0.0; i <= 1.2; i += 0.3) {
        float z = fract(i + time * 0.2);
        float size = mix(16.0, 4.0, z);
        float fade = smoothstep(0.0, 0.5, z) * smoothstep(1.0, 0.5, z);

        mat2 mat = rotate2d(i * 2.618);
        uv = uv * mat;
        m = m * mat;

        vec2 lol = mouse * 0.25 * mat;
        vec2 uv2 = uv * size - lol;
        vec2 m2 = m * size - lol;

        sum += layer(uv2, m2) * fade;
    }

    //gl_FragColor = vec4(sum, sum, sum, 1.0);
    gl_FragColor = vec4(1.0, 1.0, 1.0, sum);
}
