/**[Vertex]**/

#version 330 core
#include worldRender

layout(location = 0) in vec4 i_pos;//Position//
layout(location = 1) in vec2 i_uv;//UV Coord: TexAtlas//
layout(location = 2) in vec3 i_normal;//Normal of Face//
layout(location = 3) in vec3 i_tangent;//Tangent of Face//
layout(location = 4) in vec3 i_bitangent;//BiTangent of Face//
layout(location = 3) in vec4 i_col;//Colour Mask//
layout(location = 4) in vec4 i_light;//Lighting Value//

out vec2 uv;
//out vec3 debug;

void main() {
    uv = i_uv;
    //debug = i_pos.xyz;
    gl_Position = frame.projMatrix * frame.camMatrix * chunkdata.chunkPos * i_pos;
}

/**[Fragment]**/

#version 330 core
#include worldRender

in vec2 uv;
//in vec3 debug;

layout(location = 0) out vec4 diffuse;

void main() {
    //vec4 debugK = vec4(debug,1) / 50.0F;
    diffuse = texture(tDiffuse,uv).rgba;// + debugK;
}


/**[End]**/
