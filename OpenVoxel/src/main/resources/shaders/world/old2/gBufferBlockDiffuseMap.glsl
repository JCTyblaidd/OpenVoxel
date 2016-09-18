/**[Vertex]**/
#version 300 core

layout(location = 0) in vec4 pos;
layout(location = 1) in vec2 UV;
layout(location = 2) in vec3 ColourMask;
layout(location = 3) in vec3 LightData;

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
    colour = ColourMask;
    lightData = LightData;
}

out vec2 uv;
out vec3 colour;
out vec3 lightData;

/**[Fragment]**/
#version 330 core

in vec2 uv;
in vec3 colour;
in vec3 lightData;

uniform textureAtlas {
    sampler2D blockDiffuse;
    sampler2D blockNormal;
    sampler2D blockPBR;
} atlas;

layout(location = 0) out vec4 texDiffuse;

void main() {
    vec4 colV = texture2D(blockDiffuse,uv);
    texDiffuse = colV * vec4(colour,1) * vec4(lightData,1);
}

/**[End]**/
