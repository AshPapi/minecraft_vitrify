#version 330
#extension GL_ARB_separate_shader_objects : require

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    mat4 TextureMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
};

uniform sampler2D Sampler0;

layout(location = 0) in vec2 vLocalPos;
layout(location = 1) in vec2 vHalfSize;
layout(location = 2) flat in vec2 vShapeParams;
layout(location = 3) flat in ivec2 vModeFlags;
layout(location = 4) in vec4 vColor;
layout(location = 5) in vec2 vScreenUv;

layout(location = 0) out vec4 fragColor;

float sdRoundBox(vec2 p, vec2 b, float r) {
    float cr = min(r, min(b.x, b.y));
    vec2 q = abs(p) - b + vec2(cr);
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - cr;
}

float evalPanelWithTab(
    vec2 pos,
    vec2 halfSize,
    float radius,
    int tabSide,
    float tabCenter,
    float tabHalfSpan,
    float tabProtrude
) {
    float dPanel = sdRoundBox(pos, halfSize, radius);
    if (tabSide == 0) {
        return dPanel;
    }
    float halfDepth = (tabProtrude + 10.0) * 0.5;
    float shift = (tabProtrude - 10.0) * 0.5;
    vec2 tabPos = tabSide == 3
        ? pos - vec2(-halfSize.x - shift, tabCenter)
        : pos - vec2(tabCenter, tabSide == 1 ? -halfSize.y - shift : halfSize.y + shift);
    vec2 tabHalf = tabSide == 3 ? vec2(halfDepth, tabHalfSpan) : vec2(tabHalfSpan, halfDepth);
    float dTab = sdRoundBox(tabPos, tabHalf, min(radius, 5.0));
    vec2 u = max(vec2(1.85 - dPanel, 1.85 - dTab), vec2(0.0));
    return max(1.85, min(dPanel, dTab)) - length(u);
}

void main() {
    int openSide = (vModeFlags.x >> 3) & 3;
    int tabSide = (vModeFlags.x >> 5) & 3;
    float tabHalfSpan = float(((vModeFlags.x >> 7) & 15) + 6);
    float tabProtrude = float(((vModeFlags.x >> 11) & 15) + 20);

    vec2 p = vLocalPos;
    vec2 b = vHalfSize;
    if (openSide == 1) {
        if (p.y > b.y + 0.5) discard;
        p.y -= 6.0;
        b.y += 6.0;
    } else if (openSide == 2) {
        if (p.y < -b.y - 0.5) discard;
        p.y += 6.0;
        b.y += 6.0;
    } else if (openSide == 3) {
        if (p.x > b.x + 0.5) discard;
        p.x -= 6.0;
        b.x += 6.0;
    }

    float dist = evalPanelWithTab(p, b, vShapeParams.x, tabSide, vShapeParams.y, tabHalfSpan, tabProtrude);
    float aa = max(fwidth(dist), 0.55);
    float mask = 1.0 - smoothstep(-aa * 0.5, aa * 0.5, dist);
    float alpha = mask * vColor.a * ColorModulator.a;
    if (alpha <= 0.002) {
        discard;
    }
    vec3 blurred = texture(Sampler0, clamp(vScreenUv, vec2(0.001), vec2(0.999))).rgb;
    float desat = dot(blurred, vec3(0.2126, 0.7152, 0.0722));
    vec3 frosted = clamp(mix(vec3(desat), blurred, 0.88) * 0.80, 0.0, 1.0);
    fragColor = vec4(frosted * vColor.rgb * ColorModulator.rgb, alpha);
}
