/**[Vertex]**/
#version 330 core
#include worldRender

layout(location = 0) in vec4 i_pos;     //Position//
layout(location = 1) in vec2 i_uv;      //UV Coord: TexAtlas//
layout(location = 2) in vec3 i_normal;  //Normal of Face//
layout(location = 3) in vec3 i_tangent; //Tangent of Face//
layout(location = 4) in vec4 i_col;     //Colour Mask//
layout(location = 5) in vec4 i_light;   //Lighting Value//

out vec2 uv;
out vec4 col;

void main() {
    gl_Position = shadowdata.ShadowMapMatrices[gl_InstanceID] * chunkData.chunkPos * i_pos;
    uv = i_uv;
    col = i_col;
}


/**[Geometry]**/
#version 330 core
#define GL_NV_geometry_shader_passthrough 1
#include worldRender

layout(passthrough) in;

layout(passthrough) in gl_PerVertex {
    vec4 gl_Position;
};
layout(passthrough) in inputs {
    vec2 uv;
    vec4 col;
};

void main() {
    gl_Layer = gl_InstanceID;
}

/**[Fragment]**/
#version 330 core
#include worldRender

in vec2 uv;
in vec4 col;


void main() {

}
/**[End]**/