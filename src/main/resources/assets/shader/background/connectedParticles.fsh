#version 450

// ideas borrowed heavily from https://www.shadertoy.com/view/lsy3R1

// Random function from https://www.shadertoy.com/view/4ssXRX
// note: uniformly distributed, normalized rand, [0;1[

layout(binding = 0) uniform sampler2DMS background;
layout(binding = 1) uniform sampler2D iChannel0;
uniform float backgroundAlpha;
uniform vec2 iResolution;
uniform int iFrame;
uniform vec2 iMouse;
uniform float iTime;

out vec4 fragColor;

float distanceSqTo(vec2 p1, vec2 p2) {
    return pow(p1.x - p2.x, 2) + pow(p1.y - p2.y, 2);
}

bool nearToMouse(vec2 p1, vec2 p2) {
    float maxDistSq = 0.4 * 0.4;
    return distanceSqTo(p1, iMouse) <= maxDistSq && distanceSqTo(p2, iMouse) <= maxDistSq;
}

//ideas borrowed heavily from https://www.shadertoy.com/view/lsy3R1
void main()
{
    vec2 fragCoord = gl_FragCoord.xy;
    fragColor = vec4(0.0,0.0,0.0,0.0);
    //root of the number of particles, adjust for complexity
    int numParticles = 20;

    float maxDistance = .6;
    float minDistance = 0.0;

    //these two loops determine how many particles render.
    for (int x = 0; x<=numParticles; x++) {
        //fetch particle
        vec4 particle = texture(iChannel0, vec2(x, 0)/iResolution.xy);
        //get pixel coords for particles
        vec2 nearestP = vec2(floor(iResolution.x*particle.x), floor(iResolution.y*particle.y));
        //draw point

        for (int a = 0; a<=numParticles; a++) {
            //remove 1/numParticles of the work
            if (x != a) {
                //fetch particle
                vec4 particle2 = texture(iChannel0, vec2(a, 0)/iResolution.xy);
                //get pixel coords for particles
                vec2 nearestP2 = vec2(floor(iResolution.x*particle2.x), floor(iResolution.y*particle2.y));

                //diffs used for distance and slope
                float diffy = nearestP.y-nearestP2.y;
                float diffx = nearestP.x-nearestP2.x;

                //distance between points
                float addExp = pow(diffx/iResolution.x, 2.0) + pow(diffy/iResolution.x,2.0);
                float ldistance = pow(addExp, .5);

                if (ldistance < maxDistance && ldistance > minDistance) {
                    //line equation
                    float slope = (diffy)/(diffx);
                    float intercept = nearestP.y-(slope*nearestP.x);

                    //find out what pixel would be on the line for these two points
                    vec2 intended = vec2(floor(fragCoord.x), floor((fragCoord.x*slope)+intercept));

                    //throw away the points that are not between the two points
                    vec2 mins = min(nearestP, nearestP2);
                    vec2 maxs = max(nearestP, nearestP2);
                    intended = clamp(intended, mins, maxs);

                    //draw Lines
                    if (floor(fragCoord) == intended) {
                        //variation by length, adjusts for max Distance
                        float col = pow(1.0-ldistance, 8.0/maxDistance);
                        //always draw brightest line on top
                        if (col > fragColor.x) {
                            fragColor = vec4(vec3(col), 1.0);
                        }
                    }
                }
            }
        }
        //draw points;
        if (floor(fragCoord) == (floor(nearestP))) {
            fragColor = vec4(1.0, 0.0, 0.0, 1.0);
        }
    }
}