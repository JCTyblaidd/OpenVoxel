/**[Vertex]**/
#version 330 core
layout(location = 0) in vec3 position;
layout(location = 1) in vec2 uv_coord;
layout(location = 2) in vec4 colour;

uniform float Z_Offset = 0;

uniform mat4 matrix = mat4(1.0f);

out vec2 UV;
out vec4 Col;

void main() {
    UV = uv_coord;
    Col = colour;
    vec4 basePos = vec4(position,1) + vec4(0,0,Z_Offset,0);
    gl_Position = matrix * basePos;
}

/**[Fragment]**/
#version 330 core

in vec2 UV;
in vec4 Col;

uniform bool Enable_Tex = true;
uniform bool Enable_Col = true;

uniform sampler2D texture;

layout(location = 0) out vec4 colour;

void main() {
    vec4 res;
    if(Enable_Tex) {
        res = texture2D(texture,UV).rgba;
    }else {
        res = vec4(1,1,1,1);
    }
    if(Enable_Col) {
       res *= Col;
    }
    colour = res;
}
/**[End]**/