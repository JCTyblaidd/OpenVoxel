/**[Vertex]**/
#version 330 core
#include worldRender

layout(location = 0) in vec4 i_pos;//Position//
layout(location = 1) in vec2 i_uv;//UV Coord: TexAtlas//
layout(location = 2) in vec3 i_normal;//Normal of Face//
layout(location = 3) in vec3 i_tangent;//Tangent of Face//
layout(location = 4) in vec4 i_col;//Colour Mask//
layout(location = 5) in vec4 i_light;//Lighting Value//

//Precalculate normal
out mat3 tangentMat;
out vec3 tangentViewPos;
out vec3 tangentFragPos;
out vec3 fragPos;
out vec2 uv;
out vec3 normal;
out vec3 tangent;
out vec3 bitangent;
out vec4 colmask;
out vec4 light;


void main() {
    fragPos = (chunkdata.chunkPos * i_pos).xyz;
    gl_Position = frame.projMatrix * frame.camMatrix * vec4(fragPos,1);
    uv = i_uv;
    normal = i_normal;
    tangent = i_tangent;
    bitangent = cross(i_normal,i_tangent);
    colmask = i_col;
    light = i_light;
    tangentMat = mat3(normalize(tangent),normalize(bitangent),normalize(normal));
    tangentFragPos = tangentMat * fragPos;
    tangentViewPos = tangentMat * vec3(100,105,100);//TODO: fix properly
}



/**[Fragment]**/
#version 330 core
#include worldRender

in mat3 tangentMat;
in vec3 tangentViewPos;
in vec3 tangentFragPos;
in vec3 fragPos;
in vec2 uv;
in vec3 normal;
in vec3 tangent;
in vec3 bitangent;
in vec4 colmask;
in vec4 light;


layout(location = 0) out vec3 w_diffuse;
layout(location = 1) out vec4 w_pbr;
layout(location = 2) out vec3 w_normal;
layout(location = 3) out vec4 w_lighting;


/**
 * Execute Parallax : Returns UV Coordinate
**/
vec2 parallax() {
    const float height_scale = 0.1f;
    float height = texture(tPBR,uv).a;
    vec3 tangentViewDir = tangentViewPos - tangentFragPos;
    vec2 p = tangentViewDir.xy / tangentViewDir.z * (height * height_scale);
    return uv - p;
}

/**
 * Execute Parallax : Returns UV Coordinate
**/
vec2 parallax_occlusion() {
    const float height_scale = 0.1f;
    const float min_layers = 8;
    const float max_layers = 32;
    vec3 tangentViewDir = tangentViewPos - tangentFragPos;
    float num_layers = mix(max_layers, min_layers, abs(dot(vec3(0.0, 0.0, 1.0), tangentViewDir)));

    float layer_depth = 1.0 / num_layers;
    float current_layer_depth = 0.0;
    vec2 p = tangentViewDir.xy * height_scale;
    vec2 deltaTex = p / num_layers;

    vec2 current_uv = uv;
    float current_depth = texture(tPBR,uv).a;
    while(current_layer_depth < current_depth) {
        current_uv -= deltaTex;
        current_depth = texture(tPBR,current_uv).a;
        current_layer_depth += layer_depth;
    }

    vec2 prev_uv = current_uv + deltaTex;
    float after_depth = current_depth - current_layer_depth;
    float before_depth = texture(tPBR, prev_uv).a - current_layer_depth + layer_depth;

    float weight = after_depth / (after_depth - before_depth);
    vec2 final_uv = prev_uv * weight + current_uv * (1.0 - weight);
    return final_uv;
}


//TODO: edit depth value from parallax
void main() {
    vec2 parallax_uv = parallax();//_occlusion();
    vec4 sampleDiffuse = texture(tDiffuse,parallax_uv);
    vec4 sampleNormal = texture(tNormal,parallax_uv);
    vec4 samplePBR = texture(tPBR,parallax_uv);
    if(sampleDiffuse.a < 0.95) {
        discard;
    }
    vec2 smoothReflect = samplePBR.rg;
    float occlusion = samplePBR.b;
    w_diffuse = (colmask * sampleDiffuse).rgb;
    w_pbr = samplePBR;
    w_lighting = light * occlusion;
    w_normal = sampleNormal.rgb * tangentMat;
}


/**[End]**/
