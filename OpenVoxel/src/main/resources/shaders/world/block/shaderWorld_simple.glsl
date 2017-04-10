/**[Vertex]**/

#version 330 core
#include worldRender

layout(location = 0) in vec4 i_pos;//Position//
layout(location = 1) in vec2 i_uv;//UV Coord: TexAtlas//
layout(location = 2) in vec3 i_normal;//Normal of Face//
layout(location = 3) in vec4 i_col;//Colour Mask//
layout(location = 4) in vec4 i_light;//Lighting Value//

out vec2 uv;

void main() {
    uv = i_uv;
    gl_Position = frame.projMatrix * frame.camMatrix * chunkdata.chunkPos * i_pos;
}

/**[Fragment]**/

#version 330 core
#include worldRender

in vec2 uv;

layout(location = 0) out vec4 diffuse;

void main() {
    diffuse = texture(tDiffuse,uv).rgba;
}


/**[End]**/
