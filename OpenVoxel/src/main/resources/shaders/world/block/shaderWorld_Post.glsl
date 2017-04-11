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

layout(location = 0) out vec3 resultColour;

/**
    Run Sampling
**/
void main() {
    resultColour = texture(tMerged,uv).rgb;
}

/**[End]**/
