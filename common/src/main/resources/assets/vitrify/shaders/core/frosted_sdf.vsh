#version 330
#extension GL_ARB_separate_shader_objects : require

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    mat4 TextureMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
};
layout(std140) uniform Projection {
    mat4 ProjMat;
};

layout(location = 0) in vec3 Position;
layout(location = 1) in vec2 UV0;
layout(location = 2) in vec2 UV3;
layout(location = 3) in ivec2 UV1;
layout(location = 4) in ivec2 UV2;
layout(location = 5) in vec4 Color;

layout(location = 0) out vec2 vLocalPos;
layout(location = 1) out vec2 vHalfSize;
layout(location = 2) flat out vec2 vShapeParams;
layout(location = 3) flat out ivec2 vModeFlags;
layout(location = 4) out vec4 vColor;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vLocalPos = UV0;
    vHalfSize = UV3;
    vShapeParams = vec2(UV1) / 64.0;
    vModeFlags = UV2;
    vColor = Color;
}
