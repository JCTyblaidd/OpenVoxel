/**[Vertex]**/
#version 330 core
#include worldRender

layout(location = 0) in vec4 i_pos;//Position//
layout(location = 1) in vec2 i_uv;//UV Coord: TexAtlas//
layout(location = 2) in vec3 i_normal;//Normal of Face//
layout(location = 3) in vec4 i_col;//Colour Mask//
layout(location = 4) in vec4 i_light;//Lighting Value//

out vec2 uv;
out vec3 normal;
out vec4 col;
out vec4 light;


void main() {
    normal = frame.camNormMatrix * i_normal;
    gl_Position = frame.projMatrix * frame.camMatrix * chunkdata.chunkPos * i_pos;
    light = i_light;
    col = i_col;
    uv = i_uv;
}

/**[Fragment]**/
#version 330 core
#include worldRender

in vec2 uv;
in vec3 normal;
in vec4 col;
in vec4 light;

layout(location = 0) out vec4 forwardColour;

void main() {
    vec3 lightReal = getRealLight(light);
    vec4 diffuse = texture2D(atlas.tDiffuse,uv);

    forwardColour = (diffuse * col) + vec4(lightReal,0);
}