/**[Vertex]**/
#version 300 core

layout(location = 0) in vec4 pos;
layout(location = 1) in vec2 UV;
layout(location = 2) in vec3 ColourMask;
layout(location = 3) in vec3 LightData;
layout(location = 4) in vec3 Normal;

uniform mat4 chunkTransformation;

uniform frameData {
    mat4 projMatrix;
    mat4 inveseProjMatrix;
    mat4 cameraMatrix;
    vec2 depthRange;
    vec2 halfSizeNearPlane;//{tan(fovy/2.0) * aspect, tan(fovy/2.0) }//
} frame;

void main() {
    gl_Position = frame.projMatrix * frame.cameraMatrix * chunkTransformation * pos;
    uv = UV;
    Normal = normalize(cameraMatrix * chunkTransformation * vec4(Normal,0)).xyz;//TODO: check
    colour = ColourMask;
    lightData = LightData;
}

out vec2 uv;
out vec3 normal;
out vec3 colour;
out vec3 lightData;

/**[Fragment]**/
#version 330 core

in vec2 uv;
in vec3 normal;
in vec3 colour;
in vec3 lightData;

uniform bool UsesTransparent;

uniform textureAtlas {
    sampler2D blockDiffuse;
    sampler2D blockNormal;
    sampler2D blockPBR;
} atlas;

layout(location = 0) out vec2 uvTex;
layout(location = 1) out vec3 normalTex;
layout(location = 2) out vec3 colourTex;
layout(location = 3) out vec3 lightTex;

void main() {
    vec4 colV = texture2D(blockDiffuse,uv);
    if((colV.a == 1.0F) == UsesTransparent) {
        discard;//Fragment Denied//
    }
    uvTex = uv;
    normalTex = normal;
    colourTex = colour;
    lightTex = lightData;
}

/**[End]**/
