//Monolithic Utility Class//

uniform Settings {//Uniform Based Shader Configuration//
    bool parallax;
    bool cloudVolumetric;
    bool drawCloud;
    bool cloudShadows;
    bool enableFog;
    bool godRays;
    bool accurateNormal;
    bool fastTransparency;
    bool highQualityLight;
    bool cascadeShadows;
    bool enableShadows;
    bool reflections;
    bool bonusReflections;
    bool depthOfField;
    bool waveAnim;
    bool windAnim;
} config;

uniform FinalFrame {
    //Animation Information//
    int animIndex;//Current Animation Index//
    //Draw Information//
    mat4 projMatrix;//Perspective Matrix
    mat4 invProjMatrix;//Inverse
    vec2 zLimits;
    mat4 camMatrix;
    mat3 camNormMatrix;//Similar to camMatrix//
    mat3 invCamNormMatrix;//Inverse

    //Per Dimension Information//
    float dayProgress;//Current Day Progress//
    mat3 dayProgressMatrix;
    float skyLightPower;//SkyLight Strength: 0->1//
    vec3 dirSun;//Direction of light from the sun or the moon
    bool skyEnabled;//Sky Is Enabled//
    vec3 fogColour;//Distance Fog Colour//
    vec3 skyLightColour;//Colour of skyLight//
    bool isRaining;
    bool isThunder;
} frame;

uniform chunkConstants {
    mat4 chunkPos;
} chunkdata;

uniform TextureAtlas {
    sampler2D tDiffuse;//Block Diffuse//
    sampler2D tNormal;//Normal Diffuse//
    sampler2D tPBR;//Physically Based Rendering Information//
    sampler2D itemDiffuse;//Item Diffuse Texture//
    samplerCube skyMap;
    vec2 tileSize;
} atlas;

uniform ShadowMap {
    sampler2D shadow1;
    sampler2D shadow2;
    sampler2D shadow3;
} shadowmap;

vec3 sampleSky(in vec3 camSpaceDir) {
    vec3 worldSpaceDir = frame.dayProgressMatrix * frame.invCamNormMatrix * camSpaceDir;
    return textureCube(atlas.skyMap,worldSpaceDir);
}

/**Get The Real Block Light Value (taking into account world lighting)*/
vec3 getRealLight(in vec4 lightDat) {
    float val = lightDat.a + frame.skyLightPower - 1.0F;
    return lightDat.rgb + (val * frame.skyLightColour);
}