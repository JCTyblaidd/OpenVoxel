/**[Vertex]**/
#version 330 core

layout(location = 0) in vec4 pos;
layout(location = 1) in vec2 UV;
layout(location = 2) in vec3 Light_Data;

uniform frameData {
    mat4 projMatrix;
    mat4 inveseProjMatrix;
    mat4 cameraMatrix;
    vec2 depthRange;
    vec2 halfSizeNearPlane;//{tan(fovy/2.0) * aspect, tan(fovy/2.0) }//
} frame;

uniform mat4 EntityMatrix;

void main() {
    gl_Position = frame.projMatrix * frame.cameraMatrix * EntityMatrix * pos;
    uv = UV;
    light = Light_Data;
}

out vec2 uv;
out vec3 light;

/**[Fragment]**/

in vec2 uv;
in vec3 light;

layout(location = 0) out vec3 colour;

uniform sampler2D texture;

void main() {
    vec4 tex = texture2D(texture,uv);
    if(tex.a <= 0.5) {
        discard;
    }
    colour = tex.rgb * light;
}

/**[End]**/
