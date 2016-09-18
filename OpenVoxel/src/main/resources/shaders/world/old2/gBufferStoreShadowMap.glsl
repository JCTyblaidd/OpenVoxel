/**[Vertex]**/
#version 330 core
layout(location = 0) in vec4 pos;

uniform mat4 chunkTransformation;

uniform mat4 shadowMapCamera;

void main() {
    gl_Position = shadowMapCamera * chunkTransformation * pos;
}


/**[Fragment]**/
#version 330 core


void main() {

}



/**[End]**/