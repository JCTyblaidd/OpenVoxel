/**[Vertex]**/
#version 330 core

layout(location = 0) in vec4 pos;
layout(loctaion = 1) in vec2 UV;

out vec2 uv;

void main() {
    uv = UV;
    gl_Position = pos;
}


/**[Fragment]**/
#version 330 core

in vec2 uv;

uniform blockPrevData {
    sampler2D UV;
    sampler2D Normal;
    sampler2D ColourMask;
    sampler2D LightMask;
} blockDat;

uniform textureAtlas {
    sampler2D blockDiffuse;//rgba=diffuse
    sampler2D blockNormal;//rgb=normal, a=flags?
    sampler2D blockPBR;//r=smooth g=reflect b=ambient a=height
} atlas;

layout(location = 0) out vec3 diffuse;
layout(location = 1) out vec3 normal;
layout(location = 2) out vec3 pbr;
layout(location = 3) out vec3 lightdat;

vec2 getParallaxUV() {//Use the Height Value + parallax
    return vec2(0,0);
}

void main() {
    vec2 uvcoord = texture2D(blockDat.UV,uv).rg;
    vec3 normalval = texture2D(blockDat.Normal,uv).rgb;
    vec3 colourmask = texture2D(blockDat.ColourMask,uv).rgb;
    vec3 lightmask = textureD(blockDat.LightMask,uv).rgb;


    vec4 diffuse = texture2D(atlas.blockDiffuse,uvcoord);
    vec4 normal = texture2D(atlas.blockNormal,uvcoord);
    vec4 pbr = texture2D(atlas.blockPBR,uvcoord);



}

/**[End]**/