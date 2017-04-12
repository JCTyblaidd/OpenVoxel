/**[Vertex]**/
#version 330 core
#include worldRender

layout(location = 0) in vec4 i_pos;//Position//
layout(location = 1) in vec2 i_uv;//UV Coord: TexAtlas//
layout(location = 2) in vec3 i_normal;//Normal of Face//
layout(location = 3) in vec3 i_tangent;//Tangent of Face//
layout(location = 4) in vec4 i_col;//Colour Mask//
layout(location = 5) in vec4 i_light;//Lighting Value//

//Precalculate normal
out mat3 tangentMat;
out vec3 tangentViewPos;
out vec3 tangentFragPos;
out vec3 fragPos;
out vec2 uv;
out vec3 normal;
out vec3 tangent;
out vec3 bitangent;
out vec4 colmask;
out vec4 light;


void main() {
    fragPos = (chunkdata.chunkPos * i_pos).xyz;
    gl_Position = frame.projMatrix * frame.camMatrix * vec4(fragPos,1);
    uv = i_uv;
    normal = i_normal;
    tangent = i_tangent;
    bitangent = cross(i_normal,i_tangent);
    colmask = i_col;
    light = i_light;
    tangentMat = mat3(normalize(tangent),normalize(bitangent),normalize(normal));
    tangentFragPos = tangentMat * fragPos;
    tangentViewPos = tangentMat * vec3(100,105,100);//TODO: fix properly
}



/**[Fragment]**/
#version 330 core
#include worldRender

in mat3 tangentMat;
in vec3 tangentViewPos;
in vec3 tangentFragPos;
in vec3 fragPos;
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


/**
 * Execute Parallax : Returns UV Coordinate
**/
vec2 parallax() {
    float height = texture(tPBR,uv).a;
    vec3 tangentViewDir = tangentViewPos - tangentFragPos;
    vec2 p = tangentViewDir.xy / tangentViewDir.z * (height * 0.1);
    return uv - p;
}


void main() {
    vec2 parallax_uv = parallax();
    vec4 sampleDiffuse = texture(tDiffuse,parallax_uv);
    vec4 sampleNormal = texture(tNormal,parallax_uv);
    vec4 samplePBR = texture(tPBR,parallax_uv);
    //Write Information//
    if(sampleDiffuse.a < 0.95) {
        discard;//discard transparent pixels//
    }
    w_diffuse = sampleDiffuse.rgb;//(colmask * sampleDiffuse).rgb;
    w_pbr = samplePBR;
    w_lighting = light;
    w_normal = sampleNormal.rgb * tangentMat;
}


/**[End]**/
