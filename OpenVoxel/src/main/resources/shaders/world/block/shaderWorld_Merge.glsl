/**[Vertex]**/
#version 330 core
#include worldRender

layout(location = 0) in vec4 i_pos;
layout(location = 1) in vec2 i_uv;

out vec2 uv;

void main() {
    uv = i_uv;
    gl_Position = i_pos;
}


/**[Fragment]**/
#version 330 core
#include worldRender

in vec2 uv;

layout(location = 0) out vec4 finalColour;

/**
    Run Sampling
**/
void main() {
    vec3 diffuse = texture(gDiffuse,uv).rgb;
    vec4 pbr = texture(gDiffuse,uv).rgba;
    vec3 normal = texture(gNormal,uv).rgb;
    vec4 light = texture(gLighting,uv).rgba;
    //float depth = textureShadow(gDepth,uv).z;

    float pow = dot(normalize(normal),normalize(vec3(1,0.2,0.2)));
    pow = clamp(pow,0.1,1);

    finalColour = vec4(diffuse * pow,1);
}

/**[End]**/
