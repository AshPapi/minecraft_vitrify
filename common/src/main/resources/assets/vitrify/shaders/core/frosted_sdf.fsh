#version 330
#extension GL_ARB_separate_shader_objects : require

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    mat4 TextureMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
};

layout(location = 0) in vec2 vLocalPos;
layout(location = 1) in vec2 vHalfSize;
layout(location = 2) flat in vec2 vShapeParams;
layout(location = 3) flat in ivec2 vModeFlags;
layout(location = 4) in vec4 vColor;

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
    float tabProtrude,
    out vec2 blendNormDir
) {
    float dPanel = sdRoundBox(pos, halfSize, radius);
    vec2 panelNorm = pos / max(halfSize, vec2(1.0));
    if (tabSide == 0) {
        blendNormDir = panelNorm;
        return dPanel;
    }
    float halfDepth = (tabProtrude + 10.0) * 0.5;
    float shift = (tabProtrude - 10.0) * 0.5;
    vec2 tabPos = tabSide == 3
        ? pos - vec2(-halfSize.x - shift, tabCenter)
        : pos - vec2(tabCenter, tabSide == 1 ? -halfSize.y - shift : halfSize.y + shift);
    vec2 tabHalf = tabSide == 3 ? vec2(halfDepth, tabHalfSpan) : vec2(tabHalfSpan, halfDepth);
    float dTab = sdRoundBox(tabPos, tabHalf, min(radius, 5.0));
    vec2 tabNorm = tabPos / max(tabHalf, vec2(1.0));
    blendNormDir = mix(panelNorm, tabNorm, clamp(0.5 + (dPanel - dTab) / 3.7, 0.0, 1.0));
    vec2 u = max(vec2(1.85 - dPanel, 1.85 - dTab), vec2(0.0));
    return max(1.85, min(dPanel, dTab)) - length(u);
}

float sdSegment(vec2 p, vec2 a, vec2 b) {
    vec2 pa = p - a;
    vec2 ba = b - a;
    return length(pa - ba * clamp(dot(pa, ba) / max(dot(ba, ba), 0.0001), 0.0, 1.0));
}

float interleavedGradientNoise(vec2 screenPos) {
    vec3 magic = vec3(0.06711056, 0.00583715, 52.9829189);
    return fract(magic.z * fract(dot(screenPos, magic.xy)));
}

void main() {
    float radius = vShapeParams.x;
    float param = vShapeParams.y;
    int mode = vModeFlags.x & 7;
    int openSide = (vModeFlags.x >> 3) & 3;
    int tabSide = (vModeFlags.x >> 5) & 3;
    float tabHalfSpan = float(((vModeFlags.x >> 7) & 15) + 6);
    float tabProtrude = float(((vModeFlags.x >> 11) & 15) + 20);
    float extra = float(vModeFlags.y) / 1024.0;

    if (mode >= 4 && mode <= 6) {
        float s = min(vHalfSize.x, vHalfSize.y);
        float d;
        if (mode == 4) {
            vec2 rot = abs(vec2(vLocalPos.x + vLocalPos.y, vLocalPos.y - vLocalPos.x) * 0.70710678);
            d = length(vec2(max(max(rot.x, rot.y) - radius, 0.0), min(rot.x, rot.y))) - param;
        } else if (mode == 5) {
            vec2 p1 = vec2(-0.56, 0.04) * s;
            vec2 p2 = vec2(-0.12, 0.48) * s;
            vec2 p3 = vec2(0.60, -0.44) * s;
            d = min(sdSegment(vLocalPos, p1, p2), sdSegment(vLocalPos, p2, p3)) - param;
        } else {
            vec2 c = vec2(-0.16, -0.16) * s;
            float lr = 0.44 * s;
            float ring = abs(length(vLocalPos - c) - lr) - param;
            vec2 h1 = c + vec2(0.70710678, 0.70710678) * (lr + param * 0.25);
            d = min(ring, sdSegment(vLocalPos, h1, vec2(0.66, 0.66) * s) - param * 1.12);
        }
        float iconAa = clamp(fwidth(d), 0.25, 0.65);
        float fillMask = 1.0 - smoothstep(-iconAa * 0.5, iconAa * 0.5, d);
        float outMask = (1.0 - smoothstep(-iconAa * 0.5, iconAa * 0.5, d - 0.75)) * clamp(extra, 0.0, 1.0);
        float alpha = clamp(max(fillMask, outMask * 0.55), 0.0, 1.0) * vColor.a * ColorModulator.a;
        if (alpha <= 0.002) {
            discard;
        }
        fragColor = vec4(mix(vec3(0.06, 0.06, 0.08), vColor.rgb, fillMask) * ColorModulator.rgb, alpha);
        return;
    }

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

    if (mode == 0) {
        vec2 dummyNorm;
        float innerDist = evalPanelWithTab(p, b, radius, tabSide, param, tabHalfSpan, tabProtrude, dummyNorm);
        if (innerDist < -0.5) {
            discard;
        }
        float shiftedDist = evalPanelWithTab(p - vec2(0.0, 1.35), b, radius, tabSide, param, tabHalfSpan, tabProtrude, dummyNorm);
        float t = clamp(shiftedDist / max(tabSide != 0 ? 6.5 : param, 1.0), 0.0, 1.0);
        float alpha = vColor.a * (1.0 - t) * (1.0 - t) * smoothstep(-0.6, 0.35, innerDist) * ColorModulator.a;
        if (alpha <= 0.002) {
            discard;
        }
        fragColor = vec4(vColor.rgb * ColorModulator.rgb, alpha);
        return;
    }

    vec2 normDir;
    float dist = evalPanelWithTab(p, b, radius, tabSide, param, tabHalfSpan, tabProtrude, normDir);
    float aa = max(fwidth(dist), 0.55);

    if (mode == 1) {
        float alpha = (1.0 - smoothstep(-aa * 0.5, aa * 0.5, dist)) * vColor.a * ColorModulator.a;
        if (alpha <= 0.002) {
            discard;
        }
        float normY = clamp((vLocalPos.y / max(vHalfSize.y, 1.0)) * 0.5 + 0.5, 0.0, 1.0);
        float sheen = openSide == 1 ? 1.06 : (openSide == 2 ? 0.94 : mix(1.06, 0.94, normY));
        float grain = (interleavedGradientNoise(gl_FragCoord.xy) - 0.5) * extra;
        fragColor = vec4(clamp(vColor.rgb * sheen + vec3(grain), 0.0, 1.0) * ColorModulator.rgb, alpha);
        return;
    }

    float strokeWidth = tabSide != 0 ? 1.0 : max(param, 0.85);

    if (mode == 2) {
        float ringMask = 1.0 - smoothstep(-aa * 0.45, aa * 0.55, abs(dist + strokeWidth * 0.5) - strokeWidth * 0.5);
        float directional = clamp(1.0 + dot(normDir / max(length(normDir), 0.001), vec2(-0.62, -0.78)) * extra, 0.42, 1.48);
        float alpha = ringMask * vColor.a * directional * ColorModulator.a;
        if (alpha <= 0.002) {
            discard;
        }
        fragColor = vec4(vColor.rgb * ColorModulator.rgb, clamp(alpha, 0.0, 1.0));
        return;
    }

    float outerMask = 1.0 - smoothstep(-aa * 0.45, aa * 0.55, abs(dist - strokeWidth * 0.5) - strokeWidth * 0.5);
    float alpha = outerMask * vColor.a * ColorModulator.a;
    if (alpha <= 0.002) {
        discard;
    }
    fragColor = vec4(vColor.rgb * ColorModulator.rgb, alpha);
}
