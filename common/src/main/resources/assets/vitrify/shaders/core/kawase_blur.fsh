#version 330
#extension GL_ARB_separate_shader_objects : require

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    mat4 TextureMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
};

uniform sampler2D InSampler;

layout(location = 0) in vec2 texCoord;
layout(location = 0) out vec4 fragColor;

void main() {
    vec2 stepUv = ColorModulator.xy * ColorModulator.z;
    vec2 halfStep = stepUv * 0.5;

    vec4 sum = texture(InSampler, clamp(texCoord, vec2(0.001), vec2(0.999))) * 4.0;
    sum += texture(InSampler, clamp(texCoord + vec2(-halfStep.x, -halfStep.y), vec2(0.001), vec2(0.999))) * 2.0;
    sum += texture(InSampler, clamp(texCoord + vec2(halfStep.x, -halfStep.y), vec2(0.001), vec2(0.999))) * 2.0;
    sum += texture(InSampler, clamp(texCoord + vec2(-halfStep.x, halfStep.y), vec2(0.001), vec2(0.999))) * 2.0;
    sum += texture(InSampler, clamp(texCoord + vec2(halfStep.x, halfStep.y), vec2(0.001), vec2(0.999))) * 2.0;
    sum += texture(InSampler, clamp(texCoord + vec2(-stepUv.x, 0.0), vec2(0.001), vec2(0.999)));
    sum += texture(InSampler, clamp(texCoord + vec2(stepUv.x, 0.0), vec2(0.001), vec2(0.999)));
    sum += texture(InSampler, clamp(texCoord + vec2(0.0, -stepUv.y), vec2(0.001), vec2(0.999)));
    sum += texture(InSampler, clamp(texCoord + vec2(0.0, stepUv.y), vec2(0.001), vec2(0.999)));

    fragColor = vec4(sum.rgb / 16.0, 1.0);
}
