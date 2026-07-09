#version 450

layout(binding = 0) uniform sampler2D iChannel1;
layout(binding = 1) uniform sampler2D iChannel0;
uniform vec2 iResolution;
uniform int iFrame;
uniform float iTime;

out vec4 fragColor;

float nrand(vec2 n)
{
    return fract(sin(dot(n.xy, vec2(12.9898, 78.233))) * 43758.5453);
}

void mainImage(vec2 fragCoord)
{
    //we'll use this normalized uv to generate our random starting position and grab textures
    vec2 uv = fragCoord.xy / iResolution.xy;

    if (iFrame == 1)
    {
        //randomize positions on first frame
        fragColor = vec4(nrand(uv), nrand(uv.yx), 0.0, 0.0);
        return;
    }
    //dont want to do additional work for non-used pixels

    //grab particle data from last frame
    vec4 previousFrameValues = texture(iChannel0, uv);
    vec4 noise = texture(iChannel1, uv);

    //position and velocity
    vec2 pos = previousFrameValues.xy;
    vec2 vel = previousFrameValues.zw;

    //random variable to change velocities, hence "wandering"
    //I might take the noise back out, I was having trouble with random movement.
    float rand = nrand(vec2(sin(iTime)+noise.x, sin(iTime)+noise.y))-.499;
    float rand2 = nrand(vec2(sin(iTime)+noise.y, sin(iTime)+noise.x))-.499;

    //accelerate particles
    vel.x = vel.x + (rand/5000.0);
    vel.y = vel.y + (rand2/5000.0);

    //cancel particle velocity  on mouse click
    //    if (iMouse.w>0.01)
    //    {
    //        vel = vec2(0.0);
    //    }

    //limit particle velocity for smooth movement
    vel.x = clamp(vel.x, -0.004, 0.004);
    vel.y = clamp(vel.y, -0.004, 0.004);

    //calculate new position of particles
    pos.x = pos.x + vel.x;
    pos.y = pos.y + vel.y;
    //if particles drift outside the line, stop em in their tracks!
    if (pos.x < -0.2) {
        pos.x = 1.0;
    } else if (pos.x > 1.2) {
        pos.x = 0.0;
    }
    if (pos.y < -0.2) {
        pos.y = 1.0;
    } else if (pos.y > 1.2) {
        pos.y = 0.0;
    }

    //send particle information to buffer!
    fragColor = vec4(pos.x, pos.y, vel.x, vel.y) * 4;
}


void main() {
    mainImage(gl_FragCoord.xy);
}