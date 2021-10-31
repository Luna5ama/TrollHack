// http://glslsandbox.com/e#25857.0
// MORE NEON HACK TIME FROM THE 80'S

// YOU'RE ABOUT
// TO HACK TIME,
// ARE YOU SURE?
//  >YES   NO
#version 140
precision mediump float;

uniform float time;
uniform vec2 mouse;
uniform vec2 resolution;

void glow(float d) {
    float br = 0.005 * resolution.y;
    gl_FragColor.rgb += vec3(0.3, 0.15, 0.45) * br / d;
}

void line(vec2 a, vec2 l) {
    l.x *= resolution.y/resolution.x;
    l += 0.5;
    l *= resolution;

    vec2 P = gl_FragCoord.xy;

    float angle = length(mouse)/10.0;
    mat2 rot = mat2(cos(angle), -sin(angle),
    sin(angle), cos(angle));
    P = rot * P;

    a.x *= resolution.y/resolution.x;
    a += 0.5;
    a *= resolution;

    vec2 aP = P-a;
    vec2 al = l-a;
    vec3 al3 = vec3(al, 0.0);
    vec3 aP3 = vec3(aP, 0.0);
    //float q = length(dot(aP,al))/length(al);
    float q = length(cross(aP3, al3))/length(al3);

    float d = q;
    if (dot(al, aP) <= 0.0) { // before start
        d = distance(P, a);
    }
    else if (dot(al, al) <= dot(al, aP)) { // after end
        d = distance(P, l);
    }
    glow(d);
}

void point(vec2 a) {
    a.x *= resolution.y/resolution.x;
    a += 0.5;
    a *= resolution;

    vec2 P = gl_FragCoord.xy;
    float d = distance(P, a);
    glow(d);
}

float rand(int seed) {
    return fract(sin(float(seed)*15.234234) + sin(float(seed)*4.3456342) * 372.4532);
}

void main(void) {
    gl_FragColor = vec4(0.0, 0.0, 0.0, 1.0);

    // Horizontal grid lines
    float y = 0.0;
    for (int l=1; l<13; l++) {
        y = -1.0/(0.6 * sin(time * 0.73) + float(l)*1.2) + 0.25;
        line(vec2(-2.0, y), vec2(2.0, y));
    }

    // Perpendicular grid lines
    for (int l=-30; l<31; l++) {
        float x = float(l) + fract(time * 3.25);
        line(vec2(x * 0.025, y), vec2(x, -1.0));
    }

    // Starfield
    /*
    for (int l=1; l<70; l++) {
        float sx = (fract(rand(l+342) + time * (0.002 + 0.01*rand(l)))-0.5) * 3.0;
        float sy = y + 0.4 * rand(l+8324);
        point(vec2(sx,sy));
    }
    */
}