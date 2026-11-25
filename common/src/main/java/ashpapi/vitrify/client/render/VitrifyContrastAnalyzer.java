package ashpapi.vitrify.client.render;

import ashpapi.vitrify.client.anim.VitrifyAnimator;
import ashpapi.vitrify.client.config.VitrifyConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class VitrifyContrastAnalyzer {
    private static float smoothedLuminance = 0.35f;
    private static float previewLuminanceOverride = -1.0f;

    private VitrifyContrastAnalyzer() {
    }

    public static void setPreviewLuminanceOverride(float luminanceOrNegative) {
        previewLuminanceOverride = luminanceOrNegative;
    }

    public static void updatePerFrame(Minecraft mc) {
        float factor = 1.0f - (float) Math.exp(-7.5f * VitrifyAnimator.frameDelta());
        smoothedLuminance = Mth.clamp(Mth.lerp(factor, smoothedLuminance, estimateRawSceneLuminance(mc)), 0.0f, 1.0f);
    }

    private static float currentSceneLuminance() {
        return previewLuminanceOverride >= 0.0f ? Mth.clamp(previewLuminanceOverride, 0.0f, 1.0f) : smoothedLuminance;
    }

    public static float brightSceneFactor() {
        VitrifyConfig cfg = VitrifyConfig.get();
        if (!cfg.autoContrast) {
            return 0.0f;
        }
        float start = Math.max(0.18f, cfg.autoContrastThreshold * 0.72f);
        return Mth.clamp((currentSceneLuminance() - start) / Math.max(0.15f, 0.96f - start), 0.0f, 1.0f);
    }

    private static float darkSceneFactor() {
        VitrifyConfig cfg = VitrifyConfig.get();
        if (!cfg.autoContrast) {
            return 0.0f;
        }
        float ceiling = Math.max(0.18f, cfg.autoContrastThreshold * 0.50f);
        return Mth.clamp((ceiling - currentSceneLuminance()) / ceiling, 0.0f, 1.0f);
    }

    private static int alpha8(float value, float max) {
        return Math.round(Mth.clamp(value, 0.0f, max) * 255.0f);
    }

    public static int computeAdaptivePanelTint(float alphaScale) {
        VitrifyConfig cfg = VitrifyConfig.get();
        float bright = brightSceneFactor();
        float dark = darkSceneFactor();
        float baseAlpha = Mth.clamp(cfg.panelOpacity * 0.65f + cfg.tintDensity * 0.35f, 0.10f, 0.92f);
        int r = ARGB.red(cfg.tintColorRgb);
        int g = ARGB.green(cfg.tintColorRgb);
        int b = ARGB.blue(cfg.tintColorRgb);

        if (bright > 0.01f) {
            float shift = 0.24f * bright;
            r = Math.round(Mth.lerp(shift, r, Math.min(r, 20)));
            g = Math.round(Mth.lerp(shift, g, Math.min(g, 21)));
            b = Math.round(Mth.lerp(shift, b, Math.min(b, 23)));
        } else if (dark > 0.01f) {
            float lift = 0.12f * dark;
            r = Math.round(Mth.lerp(lift, r, Math.max(r, 48)));
            g = Math.round(Mth.lerp(lift, g, Math.max(g, 49)));
            b = Math.round(Mth.lerp(lift, b, Math.max(b, 53)));
        }
        return ARGB.color(alpha8((baseAlpha + 0.14f * bright + 0.04f * dark) * alphaScale, 0.94f), r, g, b);
    }

    public static int computeRimHighlightColor(float alphaScale, float accentMix) {
        VitrifyConfig cfg = VitrifyConfig.get();
        float rimAlpha = cfg.rimBrightness * (1.0f + 0.32f * darkSceneFactor() + 0.24f * brightSceneFactor()) * (1.0f + 0.45f * accentMix);
        int channel = Math.round(Mth.lerp(accentMix, 240.0f, 255.0f));
        return ARGB.color(alpha8(rimAlpha * alphaScale, 0.95f), channel, channel, Math.min(255, channel + 2));
    }

    public static int computeOuterContourColor(float alphaScale) {
        VitrifyConfig cfg = VitrifyConfig.get();
        float contourAlpha = (0.24f + 0.38f * brightSceneFactor()) * Mth.clamp(cfg.shadowStrength + 0.25f, 0.20f, 0.92f) * alphaScale;
        return ARGB.color(alpha8(contourAlpha, 0.80f), 12, 13, 15);
    }

    public static int computeShadowColor(float alphaScale) {
        VitrifyConfig cfg = VitrifyConfig.get();
        return ARGB.color(alpha8(cfg.shadowStrength * (1.0f + 0.32f * brightSceneFactor()) * alphaScale, 0.84f), 10, 11, 13);
    }

    public static int computeContainerLabelColor(float alphaScale) {
        int c = Math.round(Mth.lerp(brightSceneFactor(), 246.0f, 252.0f));
        return ARGB.color(alpha8(alphaScale, 1.0f), c, c, Math.min(255, c + 1));
    }

    private static float estimateRawSceneLuminance(Minecraft mc) {
        if (mc == null) {
            return 0.45f;
        }
        ClientLevel level = mc.level;
        LocalPlayer player = mc.player;
        if (level == null || player == null) {
            return 0.56f;
        }
        if (player.isInLava()) {
            return 0.88f;
        }

        BlockPos eyePos = BlockPos.containing(player.getEyePosition());
        int skyLight = level.getBrightness(LightLayer.SKY, eyePos);
        int blockLight = level.getBrightness(LightLayer.BLOCK, eyePos);
        float dayFactor = Mth.clamp(1.0f - (level.getSkyDarken() / 11.0f), 0.08f, 1.0f);
        float baseLight = Math.max((skyLight / 15.0f) * dayFactor, (blockLight / 15.0f) * 0.82f);

        float pitch = player.getXRot();
        if (pitch < -8.0f && skyLight >= 11) {
            baseLight = Math.max(baseLight, Mth.lerp(Mth.clamp((-pitch - 8.0f) / 45.0f, 0.0f, 1.0f), baseLight, 0.92f * dayFactor));
        }

        return Mth.clamp(baseLight * (0.72f + 0.52f * sampleNearbyAlbedoBoost(mc, level, player, eyePos)), 0.03f, 0.98f);
    }

    private static float sampleNearbyAlbedoBoost(Minecraft mc, ClientLevel level, LocalPlayer player, BlockPos eyePos) {
        float boost = 0.25f;
        HitResult hit = mc.hitResult;
        if (hit instanceof BlockHitResult bhr && hit.getType() == HitResult.Type.BLOCK) {
            boost = Math.max(boost, blockBrightnessScore(level, bhr.getBlockPos()));
        }
        Vec3 look = player.getViewVector(1.0f);
        BlockPos ahead = BlockPos.containing(player.getEyePosition().add(look.scale(3.0)));
        boost = Math.max(boost, blockBrightnessScore(level, ahead));
        boost = Math.max(boost, blockBrightnessScore(level, player.blockPosition().below()));
        return Math.max(boost, blockBrightnessScore(level, eyePos));
    }

    private static float blockBrightnessScore(ClientLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return 0.20f;
        }
        if (state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.POWDER_SNOW)
                || state.is(Blocks.QUARTZ_BLOCK) || state.is(Blocks.CALCITE)) {
            return 1.0f;
        }
        if (state.is(Blocks.ICE) || state.is(Blocks.PACKED_ICE) || state.is(Blocks.BLUE_ICE)
                || state.is(Blocks.SAND) || state.is(Blocks.SANDSTONE) || state.is(Blocks.LAVA)) {
            return 0.86f;
        }
        int rgb = state.getMapColor(level, pos).col;
        return (0.2126f * ARGB.red(rgb) + 0.7152f * ARGB.green(rgb) + 0.0722f * ARGB.blue(rgb)) / 255.0f;
    }
}
