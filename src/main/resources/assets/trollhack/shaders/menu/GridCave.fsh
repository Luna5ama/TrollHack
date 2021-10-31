/*
 * Original shader from: https://www.shadertoy.com/view/fd23zz
 */
#version 140
precision mediump float;

// glslsandbox uniforms
uniform float time;
uniform vec2 resolution;

// shadertoy emulation
#define iTime time
#define iResolution resolution

// --------[ Original ShaderToy begins here ]---------- //
#define MAX_STEPS 64
#define EPS 0.001
#define ITR 20


mat2 rotate(float r){
    float c=cos(r);
    float s=sin(r);
    return mat2(c, s, -s, c);
}

float maxcomp(vec2 p){
    return max(p.x, p.y);
}


float sdCross(vec3 p){
    p=abs(p);
    vec3 d=vec3(max(p.x, p.y),
    max(p.y, p.z),
    max(p.z, p.x));
    return min(d.x, min(d.y, d.z))-(1.0/3.0);
}


float sdCrossRep(vec3 p){
    vec3 q=mod(p+1.0, 2.0)-1.0;
    return sdCross(q);
}


float sdCrossRepScale(vec3 p, float s){
    return sdCrossRep(p*s)/s;
}


float scene(vec3 p){

    float scale=1.0;
    float dist=0.0;
    for (int i=0;i<ITR;i++){
        dist=max(dist, -sdCrossRepScale(p, scale));
        scale*=3.0;
    }
    return dist;
}

vec3 genNormal(vec3 p){
    return normalize(vec3(
    scene(p+vec3(EPS, 0.0, 0.0))-scene(p-vec3(EPS, 0.0, 0.0)),
    scene(p+vec3(0.0, EPS, 0.0))-scene(p-vec3(0.0, EPS, 0.0)),
    scene(p+vec3(0.0, 0.0, EPS))-scene(p-vec3(0.0, 0.0, EPS))
    ));
}

vec3 hsv2rgb(vec3 c){
    vec4 K=vec4(1.0, 2.0/3.0, 1.0/3.0, 3.0);
    vec3 p=abs(fract(c.xxx+K.xyz)*6.0-K.www);
    return c.z*mix(K.xxx, clamp(p-K.xxx, 0.0, 1.0), c.y);
}

vec4 colorize(float c){
    float hue=mix(0.6, 1.15, min(c*1.2-0.05, 1.0));
    float sat=1.0-pow(c, 4.0);
    float lum=c;
    vec3 hsv=vec3(hue, sat, lum);
    vec3 rgb=hsv2rgb(hsv);
    return vec4(rgb, 1.0);
}

void mainImage(out vec4 fragColor, in vec2 fragCoord){
    vec2 uv=fragCoord.xy/iResolution.xy*2.0-1.0;

    vec3 cameraPos=vec3(0.0, 0.0, iTime*0.4);
    vec3 cameraDir=vec3(0.0, 0.0, 1.0);
    vec3 cameraPlaneU=vec3(1.0, 0.0, 0.0);
    vec3 cameraPlaneV=vec3(0.0, 1.0, 0.0)*(iResolution.y/iResolution.x);

    vec3 rayPos=cameraPos;

    vec3 rayDir=cameraDir+uv.x*cameraPlaneU+uv.y*cameraPlaneV;

    rayDir=normalize(rayDir);

    rayDir.xy*=rotate(iTime*0.4);
    rayDir.yz*=rotate(iTime*0.4);

    float dist=scene(rayPos);

    float emission=0.0;


    int stapsTaken;
    for (int i=0;i<MAX_STEPS;i++){
        if (dist<EPS)break;
        rayPos+=rayDir*dist;
        dist=scene(rayPos);

        emission+=exp(dist*-0.4);

        stapsTaken=i;
    }


    vec4 color=0.05*emission*vec4(0.3, 0.3, 0.9, 1.0);
    fragColor=color;
}
// --------[ Original ShaderToy ends here ]---------- //

void main(void)
{
    mainImage(gl_FragColor, gl_FragCoord.xy);
}