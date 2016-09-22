/**[Vertex]**/
#version 330 core

layout(location = 0) in vec4 i_pos;//Position//
layout(location = 1) in vec2 i_uv;//UV Coord: TexAtlas//
layout(location = 2) in vec3 i_normal;//Normal of Face//
layout(location = 3) in vec4 i_col;//Colour Mask//
layout(location = 4) in vec4 i_light;//Lighting Value//

out vec2 UV;

void main() {
    UV = i_uv;
    gl_Position = frame.projMatrix * frame.camMatrix * chunkdata.chunkPos * i_pos;
}

/**[Geometry]**/
#version 330 core
//Triple Geomtetry//




/**[Fragment]**/

in vec2 UV;

void main() {
    vec4 val = texture2D(atlas.tDiffuse,uv);
    if(val.a <= 0.5F) {//Deny Shadow Blocking
        discard;
    }
}


/**[End]**/