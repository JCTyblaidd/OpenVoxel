/**[Vertex]**/
#version 330 core

layout(location = 0) in vec4 pos;
layout(location = 1) in vec2 uv;

uniform mat4 mat = mat4(1.0);
out vec2 uvCoord;

void main() {
    gl_Position = mat * pos;
    uvCoord = uv;
}

/**[Fragment]**/
#version 330 core
in vec2 uvCoord;

uniform vec4 colour = vec4(1,0,1,1);
uniform vec4 outline_colour = vec4(1,0,1,1);//Outline NYI
uniform float shadow;

uniform sampler2D texture;

layout(location = 0) out vec4 Frag;

void main() {
    /**TODO: IMPROVE**/
    //Frag = texture2D(texture,uvCoord) + vec4(1,0,0,0);
    float res = texture2D(texture,uvCoord).r;
    if(res < 0.5F) {
        discard;
    }else{
        Frag = colour;
    }
}

/**[End]**/