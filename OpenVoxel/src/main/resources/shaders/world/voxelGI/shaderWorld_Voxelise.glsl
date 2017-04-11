/**[Vertex]**/
#version 330 core
#include worldRender

layout(location = 0) in vec4 i_pos;     //Position//
layout(location = 1) in vec2 i_uv;      //UV Coord: TexAtlas//
layout(location = 2) in vec3 i_normal;  //Normal of Face//
layout(location = 3) in vec3 i_tangent; //Tangent of Face//
layout(location = 4) in vec4 i_col;     //Colour Mask//
layout(location = 5) in vec4 i_light;   //Lighting Value//


out vec2 g_uv;
out vec3 g_pos;
out vec3 g_normal;
out vec4 g_col;
out vec4 g_light;

void main() {
    vec4 temp_pos = chunkdata.chunkPos * i_pos;
    g_pos = temp_pos.xyz;
    g_uv = i_uv;
    g_normal = i_normal;
    g_col = i_col;
    g_light = i_light;
    gl_Position = frame.projMatrix * frame.camMatrix * temp_pos;
}

/**[Geometry]**/
#version 330 core
#include worldRender


layout(triangles) in;
layout(triangle_strip, max_vertices = 3) out;

in vec2 g_uv[3];
in vec3 g_pos[3];
in vec3 g_normal[3];
in vec4 g_col[3];
in vec4 g_light[3];

out vec2 uv;
out vec3 pos;
out vec3 normal;
out vec4 col;
out vec4 light;

void main() {
    vec3 delta1 = g_pos[1] - g_pos[0];
    vec3 delta2 = g_pos[2] - g_pos[0];
    vec3 pval = abs(cross(delta1,delta2));
    for(uint i = 0; i < 3; i++) {
        uv = g_uv[i];
        pos = g_pos[i];
        normal = g_normal[i];
        col = g_col[i];
        light = g_light[i];
        if(pval.z > pval.x && pval.z > pval.y){
            gl_Position = vec4(pos.x, pos.y, 0, 1);
        } else if (pval.x > pval.y && pval.x > pval.z){
            gl_Position = vec4(pos.y, pos.z, 0, 1);
        } else {
            gl_Position = vec4(pos.x, pos.z, 0, 1);
        }
        EmitVertex();
    }
    EmitPrimitive();
}


/**[Fragment]**/
#version 330 core
#include worldRender

in vec2 uv;
in vec3 pos;
in vec3 normal;
in vec4 col;
in vec4 light;


/**
    Voxelised Output : Stores voxel information for the nearest ?x?x? cube area from the player
**/
layout(RGBA8) uniform image3D voxelOut;

bool isValidLocation(vec3 pos) {
    return abs(pos.x) < 1 && abs(pos.y) < 1 && abs(pos.z) < 1;
}

vec3 voxelMapPosition() {
    return (pos - voxeldata.minVoxels) / voxeldata.sizeVoxels;
}

void main() {
    vec3 voxelPos = voxelMapPosition();
    if(!isValidLocation(voxelPos)) return;
    vec3 voxelColour = texture(tDiffuse,uv) * col * light;
    imageStore(voxelPos,voxelColour);
}


/**[End]**/
