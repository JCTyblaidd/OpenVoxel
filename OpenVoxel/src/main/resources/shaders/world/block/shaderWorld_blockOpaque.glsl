/**[Vertex]**/
#version 330 core
#include worldRender

layout(location = 0) in vec4 i_pos;//Position//
layout(location = 1) in vec2 i_uv;//UV Coord: TexAtlas//
layout(location = 2) in vec3 i_normal;//Normal of Face//
layout(location = 3) in vec4 i_col;//Colour Mask//
layout(location = 4) in vec4 i_light;//Lighting Value//

void main() {
    attr_normal = frame.camNormMatrix * i_normal;
    attr_light = i_light;
    attr_col = i_col;
    attr_uv = i_uv;
    vec4 default_pos = frame.projMatrix * frame.camMatrix * chunkdata.chunkPos * i_pos;
    if(config.windAnim) {
        gl_Position = default_pos;//TODO: Read Texture + Add SRAND Diff
    }else{
        gl_Position = default_pos;
    }
}

out vec2 attr_uv;
out vec3 attr_normal;
out vec4 attr_col;
out vec4 attr_light;

/**[Fragment]**/
#version 330 core
#include worldRender

in vec2 attr_uv;
in vec3 attr_normal;
in vec4 attr_col;
in vec4 attr_light;

layout(location = 0) out vec3 o_diffuse;  //RGB {Opaque}
layout(location = 1) out vec3 o_normal;   //XYZ Normal
layout(location = 2) out vec3 o_lighting; //RGB Light
layout(location = 2) out vec2 o_pbr;      //Smooth,Reflect {Ambient=>Light,Height=>Used}


void parallax(inout vec2 uv,out vec4 diffuse,out vec3 normal, out vec3 pbr) {
    if(config.parallax) {

    }else{
        diffuse = texture2D(atlas.tDiffuse,uv).rgba;
        normal = texture2D(atlas.tNormal,uv).rgb;
        pbr = texture2D(atlas.tPBR,uv).rgb;
    }
}

vec3 genNormal(in vec3 texNormal) {
    return attr_normal;//TODO: apply calculation
}

void main() {
    //TODO: Apply UV Animation
    vec2 UV = attr_uv;
    vec4 diffuse;
    vec3 normal;
    vec3 pbr;
    parallax(UV,diffuse,normal,pbr);

    if(diffuse.a != 1.0F) {
        discard;
    }
    o_diffuse = diffuse.rgb;
    o_normal = genNormal(normal);
    o_lighting = pbr.b * getRealLight(attr_light);
    o_pbr = pbr.rg;
}


/**[End]**/