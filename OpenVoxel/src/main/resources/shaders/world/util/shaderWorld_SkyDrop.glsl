/**[Vertex]**/
#version 330 core

layout(location = 0) in vec3 i_pos;

out vec3 uv;

void main() {
    gl_Position = i_pos;
    uv = normalize(i_pos);
}

/**[Fragment]**/
#version 330 core
#include worldRender

in vec3 uv;

layout(location = 0) out vec3 colour;

void main() {
    vec3 newUV = frame.dayProgressMatrix * uv;
    colour = texture(skyMap,newUV);
}

/**[End]**/
