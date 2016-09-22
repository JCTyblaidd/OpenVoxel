/**[Vertex]**/
#version 330 core

layout(location = 0) in vec4 pos;
layout(location = 1) in vec2 uv;

out UV;

void main() {
    UV = uv;
    gl_Position = pos;
}


/**[Fragment]**/
#version 330 core
#include worldRender

in UV;

layout(location = 0) out vec3 post_color;

void main() {
    
}


/**[End]**/