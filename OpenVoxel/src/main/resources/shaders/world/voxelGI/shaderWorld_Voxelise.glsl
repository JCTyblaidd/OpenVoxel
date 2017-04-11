/**[Vertex]**/
#version 330 core
#include worldRender

layout(location = 0) in vec4 i_pos;//Position//
layout(location = 1) in vec2 i_uv;//UV Coord: TexAtlas//
layout(location = 2) in vec3 i_normal;//Normal of Face//
layout(location = 3) in vec3 i_tangent;//Tangent of Face//
layout(location = 4) in vec4 i_col;//Colour Mask//
layout(location = 5) in vec4 i_light;//Lighting Value//


/**[Geometry]**/





/**[Fragment]**/


layout(RGBA8) uniform image3D tex3D;


/**[End]**/
