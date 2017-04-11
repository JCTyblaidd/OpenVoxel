/**[Vertex]**/
#version 330 core
#include worldRender

layout(location = 0) in vec4 i_pos;//Position//
layout(location = 1) in vec2 i_uv;//UV Coord: TexAtlas//
layout(location = 2) in vec3 i_normal;//Normal of Face//
layout(location = 3) in vec3 i_tangent;//Tangent of Face//
layout(location = 4) in vec4 i_col;//Colour Mask//
layout(location = 5) in vec4 i_light;//Lighting Value//


out vec2 uv;
out vec3 normal;
out vec3 tangent;
out vec3 bitangent;
out vec4 colmask;
out vec4 light;


void main() {
    gl_Position = frame.projMatrix * frame.camMatrix * chunkdata.chunkPos * i_pos;
    uv = i_uv;
    normal = i_normal;
    tangent = i_tangent;
    bitangent = cross(i_normal,i_tangent);
    colmask = i_col;
    light = i_light;
}



/**[Fragment]**/
#version 330 core
#include worldRender


in vec2 uv;
in vec3 normal;
in vec3 tangent;
in vec3 bitangent;
in vec4 colmask;
in vec4 light;


layout(location = 0) out vec3 w_diffuse;
layout(location = 1) out vec4 w_pbr;
layout(location = 2) out vec3 w_normal;
layout(location = 3) out vec4 w_lighting;

void main() {
    vec4 sampleDiffuse = texture(tDiffuse,uv);
    vec4 sampleNormal = texture(tNormal,uv);
    vec4 samplePBR = texture(tPBR,uv);
    mat3 normalMat = mat3(normalize(tangent),normalize(bitangent),normalize(normal));
    //Write Information//
    w_diffuse = sampleDiffuse.rgb;//(colmask * sampleDiffuse).rgb;
    w_pbr = samplePBR;
    w_lighting = light;
    w_normal = sampleNormal.rgb * normalMat;
}


/**[End]**/
