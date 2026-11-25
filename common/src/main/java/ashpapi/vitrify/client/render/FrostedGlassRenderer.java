package ashpapi.vitrify.client.render;

import ashpapi.vitrify.client.anim.VitrifyAnimator;
import ashpapi.vitrify.client.config.VitrifyConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Items;
import org.joml.Matrix3x2fStack;

public final class FrostedGlassRenderer {
    private static final int MODE_SHADOW = 0;
    private static final int MODE_FILL = 1;
    private static final int MODE_RIM = 2;
    private static final int MODE_OUTER_CONTOUR = 3;
    private static final int MODE_CROSS = 4;
    private static final int MODE_CHECK = 5;
    private static final int MODE_MAGNIFIER = 6;
    private static final Identifier TOAST_RECIPE_BOOK_SPRITE = Identifier.withDefaultNamespace("toast/recipe_book");

    private static int internalDrawDepth;

    private FrostedGlassRenderer() {
    }

    public static boolean isInternalDraw() {
        return internalDrawDepth > 0;
    }

    private static int alpha8(float value, float max) {
        return Math.round(Mth.clamp(value, 0.0f, max) * 255.0f);
    }

    private static int liftTint(int baseTint, float dRgb, float dBlue, float dAlpha, int maxAlpha) {
        return ARGB.color(
                Mth.clamp(Math.round(ARGB.alpha(baseTint) + dAlpha), 0, maxAlpha),
                Mth.clamp(Math.round(ARGB.red(baseTint) + dRgb), 0, 255),
                Mth.clamp(Math.round(ARGB.green(baseTint) + dRgb), 0, 255),
                Mth.clamp(Math.round(ARGB.blue(baseTint) + dBlue), 0, 255)
        );
    }

    public static void drawFrostedPanel(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int width,
            int height,
            float radius,
            float alphaScale,
            float accentMix,
            boolean localizedBlur,
            boolean dropShadow
    ) {
        drawFrostedPanelWithTab(graphics, x, y, width, height, radius, alphaScale, accentMix, localizedBlur, dropShadow, 0, 0.0f, 0, 0);
    }

    public static void drawFrostedPanelWithTab(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int width,
            int height,
            float radius,
            float alphaScale,
            float accentMix,
            boolean localizedBlur,
            boolean dropShadow,
            int tabSide,
            float tabCenter,
            int tabHalfSpan,
            int tabProtrude
    ) {
        if (width <= 0 || height <= 0 || alphaScale <= 0.005f) {
            return;
        }
        drawLayeredSurfaceExt(
                graphics,
                x,
                y,
                width,
                height,
                Math.min(radius, Math.min(width, height) * 0.5f),
                VitrifyContrastAnalyzer.computeAdaptivePanelTint(alphaScale),
                VitrifyContrastAnalyzer.computeRimHighlightColor(alphaScale, accentMix),
                VitrifyContrastAnalyzer.computeOuterContourColor(alphaScale),
                VitrifyContrastAnalyzer.computeShadowColor(alphaScale),
                VitrifyConfig.get().noiseStrength,
                0.42f,
                localizedBlur,
                dropShadow,
                6.5f,
                alphaScale,
                0,
                tabSide,
                tabCenter,
                tabHalfSpan,
                tabProtrude
        );
    }

    public static void drawGlassSlot(
            GuiGraphicsExtractor graphics,
            int slotX,
            int slotY,
            float hoverProgress,
            boolean quickCraft,
            float alphaScale
    ) {
        VitrifyConfig cfg = VitrifyConfig.get();
        float r = Math.min(3.2f, cfg.cornerRadius * 0.48f);
        float bright = VitrifyContrastAnalyzer.brightSceneFactor();
        int baseAlpha = alpha8(Mth.lerp(hoverProgress, 0.36f + 0.12f * bright, 0.36f) * alphaScale, 0.78f);
        int baseR = Math.round(Mth.lerp(hoverProgress, 12.0f, 224.0f));
        int baseG = Math.round(Mth.lerp(hoverProgress, 13.0f, 226.0f));
        int baseB = Math.round(Mth.lerp(hoverProgress, 15.0f, 230.0f));
        if (quickCraft) {
            baseR = Math.max(baseR, 218);
            baseG = Math.max(baseG, 220);
            baseB = Math.max(baseB, 224);
            baseAlpha = Math.max(baseAlpha, Math.round(0.40f * alphaScale * 255.0f));
        }
        int rimShade = Math.round(Mth.lerp(hoverProgress, 236.0f, 255.0f));
        int rimAlpha = alpha8((0.32f + 0.08f * bright + 0.50f * hoverProgress + (quickCraft ? 0.32f : 0.0f)) * alphaScale, 0.96f);
        int rimColor = ARGB.color(rimAlpha, rimShade, rimShade, Math.min(255, rimShade + 2));
        int outerGroove = ARGB.color(alpha8(0.26f * alphaScale, 0.55f), 8, 9, 11);

        drawFlatSurface(graphics, slotX - 1, slotY - 1, 18, 18, r, ARGB.color(baseAlpha, baseR, baseG, baseB), rimColor, outerGroove, cfg.noiseStrength * 0.4f, -0.26f);
    }

    public static void drawDestroyItemSlot(
            GuiGraphicsExtractor graphics,
            int slotX,
            int slotY,
            float hoverProgress,
            boolean hasItem,
            float alphaScale
    ) {
        VitrifyConfig cfg = VitrifyConfig.get();
        int bx = slotX - 1;
        int by = slotY - 1;
        float r = Math.min(3.2f, cfg.cornerRadius * 0.48f);
        float bright = VitrifyContrastAnalyzer.brightSceneFactor();
        int fillColor = ARGB.color(
                alpha8((0.44f + 0.10f * bright + 0.16f * hoverProgress) * alphaScale, 0.84f),
                Math.round(Mth.lerp(hoverProgress, 56.0f, 168.0f)),
                Math.round(Mth.lerp(hoverProgress, 16.0f, 38.0f)),
                Math.round(Mth.lerp(hoverProgress, 18.0f, 42.0f))
        );
        int rimColor = ARGB.color(
                alpha8((0.42f + 0.46f * hoverProgress) * alphaScale, 0.96f),
                255,
                Math.round(Mth.lerp(hoverProgress, 152.0f, 206.0f)),
                Math.round(Mth.lerp(hoverProgress, 156.0f, 210.0f))
        );
        drawFlatSurface(graphics, bx, by, 18, 18, r, fillColor, rimColor, ARGB.color(alpha8(0.32f * alphaScale, 0.62f), 18, 6, 8), cfg.noiseStrength * 0.4f, -0.20f);

        if (!hasItem && alphaScale > 0.02f) {
            int iconColor = ARGB.color(
                    alpha8((0.92f + 0.08f * hoverProgress) * alphaScale, 1.0f),
                    255,
                    Math.round(Mth.lerp(hoverProgress, 88.0f, 136.0f)),
                    Math.round(Mth.lerp(hoverProgress, 94.0f, 142.0f))
            );
            drawSdfIcon(graphics, bx, by, 18, 18, MODE_CROSS, 4.4f, 1.10f, 0.85f, iconColor);
        }
    }

    public static void drawTabGlass(
            GuiGraphicsExtractor graphics,
            int gx,
            int gy,
            int gw,
            int gh,
            int openSide,
            float lift,
            float alphaScale
    ) {
        VitrifyConfig cfg = VitrifyConfig.get();
        int baseTint = VitrifyContrastAnalyzer.computeAdaptivePanelTint(alphaScale * 0.92f);
        int fillColor = ARGB.color(
                Mth.clamp(Math.round(ARGB.alpha(baseTint) * 0.94f + 16.0f * lift), 0, 240),
                Mth.clamp(Math.round(ARGB.red(baseTint) * 0.72f + 22.0f * lift), 0, 255),
                Mth.clamp(Math.round(ARGB.green(baseTint) * 0.72f + 22.0f * lift), 0, 255),
                Mth.clamp(Math.round(ARGB.blue(baseTint) * 0.74f + 24.0f * lift), 0, 255)
        );
        drawLayeredSurfaceExt(
                graphics,
                gx,
                gy,
                gw,
                gh,
                Math.min(cfg.cornerRadius * 0.72f, 5.0f),
                fillColor,
                VitrifyContrastAnalyzer.computeRimHighlightColor(alphaScale * (0.68f + 0.42f * lift), lift * 0.6f),
                VitrifyContrastAnalyzer.computeOuterContourColor(alphaScale * 0.80f),
                VitrifyContrastAnalyzer.computeShadowColor(alphaScale * 0.35f),
                cfg.noiseStrength * 0.7f,
                0.42f,
                true,
                true,
                3.5f,
                alphaScale,
                openSide,
                0,
                0.0f,
                0,
                0
        );
    }

    public static void drawCreativeScrollbar(
            GuiGraphicsExtractor graphics,
            int trackX,
            int trackY,
            int trackW,
            int trackH,
            int thumbX,
            int thumbY,
            int thumbW,
            int thumbH,
            boolean canScroll,
            boolean hoveredOrDragging,
            float alphaScale
    ) {
        VitrifyConfig cfg = VitrifyConfig.get();
        float bright = VitrifyContrastAnalyzer.brightSceneFactor();
        int trackFill = ARGB.color(alpha8((0.42f + 0.10f * bright) * alphaScale, 0.80f), 10, 11, 13);
        int trackRim = ARGB.color(alpha8(0.32f * alphaScale, 0.75f), 232, 234, 238);
        int trackOuter = ARGB.color(alpha8(0.28f * alphaScale, 0.60f), 6, 7, 9);
        drawFlatSurface(graphics, trackX, trackY, trackW, trackH, Math.min(3.5f, trackW * 0.45f), trackFill, trackRim, trackOuter, cfg.noiseStrength * 0.35f, -0.26f);

        int tFill;
        int tRim;
        if (canScroll) {
            int shade = hoveredOrDragging ? 246 : 218;
            tFill = ARGB.color(Math.round((hoveredOrDragging ? 0.86f : 0.66f) * alphaScale * 255.0f), shade, shade, Math.min(255, shade + 3));
            tRim = ARGB.color(Math.round((hoveredOrDragging ? 0.90f : 0.68f) * alphaScale * 255.0f), 255, 255, 255);
        } else {
            tFill = ARGB.color(Math.round(0.34f * alphaScale * 255.0f), 118, 120, 126);
            tRim = ARGB.color(Math.round(0.28f * alphaScale * 255.0f), 196, 198, 204);
        }
        drawFlatSurface(graphics, thumbX, thumbY, thumbW, Math.max(8, thumbH), Math.min(3.0f, thumbW * 0.45f), tFill, tRim, 0, 0.0f, 0.30f);
    }

    public static void drawHotbarPill(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int width,
            int height,
            int slots,
            boolean localizedBlur
    ) {
        VitrifyConfig cfg = VitrifyConfig.get();
        drawFrostedPanel(graphics, x, y, width, height, Math.min(cfg.cornerRadius * 0.82f, 6.0f), 1.0f, 0.06f, localizedBlur, true);
        if (cfg.glassSlotsEnabled) {
            for (int i = 0; i < slots; i++) {
                drawGlassSlot(graphics, x + 3 + i * 20, y + 3, 0.0f, false, 0.92f);
            }
        }
    }

    public static void drawHotbarSelection(GuiGraphicsExtractor graphics, int centerX, int guiHeight, float animatedSlot) {
        float r = Math.min(VitrifyConfig.get().cornerRadius * 0.62f, 4.2f);
        int fillColor = ARGB.color(64, 240, 242, 246);
        int rimColor = ARGB.color(238, 252, 253, 255);
        int outerColor = ARGB.color(148, 10, 11, 13);
        if (animatedSlot > 8.0f) {
            graphics.enableScissor(centerX - 91, guiHeight - 22, centerX + 91, guiHeight);
            int rightSelX = Math.round(centerX - 88.0f + animatedSlot * 20.0f) - 2;
            int leftSelX = Math.round(centerX - 88.0f + (animatedSlot - 9.0f) * 20.0f) - 2;
            drawFlatSurface(graphics, rightSelX, guiHeight - 21, 20, 20, r, fillColor, rimColor, outerColor, 0.0f, 0.35f);
            drawFlatSurface(graphics, leftSelX, guiHeight - 21, 20, 20, r, fillColor, rimColor, outerColor, 0.0f, 0.35f);
            graphics.disableScissor();
            return;
        }
        int selX = Math.round(centerX - 88.0f + animatedSlot * 20.0f) - 2;
        drawFlatSurface(graphics, selX, guiHeight - 21, 20, 20, r, fillColor, rimColor, outerColor, 0.0f, 0.35f);
    }

    public static void drawSlotFrontSheen(GuiGraphicsExtractor graphics, int slotX, int slotY, float hoverProgress) {
        if (hoverProgress <= 0.02f) {
            return;
        }
        float r = Math.min(3.2f, VitrifyConfig.get().cornerRadius * 0.48f);
        drawFlatSurface(graphics, slotX - 1, slotY - 1, 18, 18, r, ARGB.color(alpha8(0.12f * hoverProgress, 0.18f), 250, 251, 253), ARGB.color(alpha8(0.58f * hoverProgress, 0.78f), 252, 253, 255), 0, 0.0f, 0.25f);
    }

    public static void drawGlassButton(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int width,
            int height,
            boolean active,
            float widgetAlpha,
            VitrifyAnimator.WidgetState animState
    ) {
        VitrifyConfig cfg = VitrifyConfig.get();
        float alphaScale = widgetAlpha * VitrifyAnimator.screenAlphaFactor();
        float press = active ? animState.press : 0.0f;
        float highlight = active ? Math.max(animState.hover, animState.focus * 0.85f) : 0.0f;
        float r = Math.min(cfg.cornerRadius * 0.72f, Math.min(width, height) * 0.45f);
        int inset = press > 0.35f ? 1 : 0;

        int baseTint = VitrifyContrastAnalyzer.computeAdaptivePanelTint(alphaScale * (active ? 0.92f : 0.60f));
        int fillColor = liftTint(baseTint, 22.0f * highlight + 12.0f * press, 24.0f * highlight + 13.0f * press, 26.0f * highlight, 235);
        int rimColor = VitrifyContrastAnalyzer.computeRimHighlightColor(alphaScale * (active ? (1.0f + 0.45f * highlight) : 0.45f), highlight);
        int shadowColor = active ? VitrifyContrastAnalyzer.computeShadowColor(alphaScale * 0.55f * (1.0f - 0.5f * press)) : 0;
        int outerColor = VitrifyContrastAnalyzer.computeOuterContourColor(alphaScale * 0.75f);

        drawLayeredSurface(
                graphics,
                x + inset,
                y + inset,
                Math.max(2, width - inset * 2),
                Math.max(2, height - inset * 2),
                r,
                fillColor,
                rimColor,
                outerColor,
                shadowColor,
                cfg.noiseStrength * 0.8f,
                0.38f,
                width >= 36 && height >= 14,
                active,
                3.5f,
                alphaScale
        );
    }

    public static void drawGlassSlider(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int width,
            int height,
            double normalizedValue,
            boolean active,
            float widgetAlpha,
            VitrifyAnimator.WidgetState animState
    ) {
        VitrifyConfig cfg = VitrifyConfig.get();
        float alphaScale = widgetAlpha * VitrifyAnimator.screenAlphaFactor();
        float highlight = active ? Math.max(animState.hover, animState.focus * 0.85f) : 0.0f;
        float r = Math.min(cfg.cornerRadius * 0.72f, height * 0.45f);

        drawLayeredSurface(
                graphics,
                x,
                y,
                width,
                height,
                r,
                VitrifyContrastAnalyzer.computeAdaptivePanelTint(alphaScale * 0.88f),
                VitrifyContrastAnalyzer.computeRimHighlightColor(alphaScale * 0.85f, highlight * 0.6f),
                VitrifyContrastAnalyzer.computeOuterContourColor(alphaScale * 0.70f),
                VitrifyContrastAnalyzer.computeShadowColor(alphaScale * 0.45f),
                cfg.noiseStrength * 0.6f,
                -0.25f,
                true,
                true,
                3.0f,
                alphaScale
        );

        int handleW = 8;
        int handleX = x + (int) Math.round(Mth.clamp(normalizedValue, 0.0, 1.0) * Math.max(0, width - handleW));
        int fillW = handleX - x + handleW / 2;
        int innerH = Math.max(2, height - 2);
        if (fillW > 3) {
            drawFlatSurface(graphics, x + 1, y + 1, Math.min(width - 2, fillW), innerH, Math.max(1.0f, r - 1.0f), ARGB.color(alpha8((0.26f + 0.16f * highlight) * alphaScale, 0.68f), 236, 238, 242), 0, 0, 0.0f, 0.0f);
        }
        drawFlatSurface(graphics, handleX, y + 1, handleW, innerH, Math.min(3.5f, r), ARGB.color(alpha8((0.78f + 0.16f * highlight) * alphaScale, 0.98f), 242, 244, 247), ARGB.color(alpha8(0.85f * alphaScale, 1.0f), 255, 255, 255), 0, 0.0f, 0.35f);
    }

    public static void drawGlassCheckboxBox(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int boxSize,
            boolean selected,
            boolean active,
            float widgetAlpha,
            VitrifyAnimator.WidgetState animState
    ) {
        float alphaScale = widgetAlpha * VitrifyAnimator.screenAlphaFactor();
        float highlight = active ? Math.max(animState.hover, animState.focus) : 0.0f;
        float r = Math.min(4.0f, VitrifyConfig.get().cornerRadius * 0.55f);
        int fill = selected
                ? ARGB.color(Math.round(0.55f * alphaScale * 255.0f), 226, 228, 234)
                : VitrifyContrastAnalyzer.computeAdaptivePanelTint(alphaScale * 0.88f);
        int rim = VitrifyContrastAnalyzer.computeRimHighlightColor(alphaScale * (1.0f + 0.4f * highlight), selected ? 0.9f : highlight);

        drawLayeredSurface(graphics, x, y, boxSize, boxSize, r, fill, rim, VitrifyContrastAnalyzer.computeOuterContourColor(alphaScale * 0.65f), 0, 0.01f, 0.30f, true, false, 0.0f, alphaScale);

        if (selected && boxSize >= 8) {
            int pad = Math.max(3, boxSize / 4);
            drawFlatSurface(graphics, x + pad, y + pad, boxSize - pad * 2, boxSize - pad * 2, Math.max(1.2f, r * 0.5f), ARGB.color(Math.round(0.95f * alphaScale * 255.0f), 252, 253, 255), 0, 0, 0.0f, 0.0f);
        }
    }

    public static void drawGlassEditBox(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int width,
            int height,
            boolean active,
            boolean focused,
            VitrifyAnimator.WidgetState animState
    ) {
        VitrifyConfig cfg = VitrifyConfig.get();
        float alphaScale = VitrifyAnimator.screenAlphaFactor();
        float focusVal = cfg.animationsEnabled ? animState.focus : (focused ? 1.0f : 0.0f);
        float highlight = Math.max(focusVal, animState.hover * 0.6f);
        float r = Math.min(cfg.cornerRadius * 0.5f, Math.min(height * 0.30f, 4.5f));

        drawLayeredSurface(
                graphics,
                x,
                y,
                width,
                height,
                r,
                VitrifyContrastAnalyzer.computeAdaptivePanelTint(alphaScale * (active ? 0.96f : 0.65f)),
                VitrifyContrastAnalyzer.computeRimHighlightColor(alphaScale * (0.95f + 0.65f * highlight), highlight),
                VitrifyContrastAnalyzer.computeOuterContourColor(alphaScale * 0.75f),
                0,
                cfg.noiseStrength * 0.5f,
                -0.20f,
                width >= 30 && height >= 12,
                false,
                0.0f,
                alphaScale
        );
    }

    public static void drawGlassScrollbar(
            GuiGraphicsExtractor graphics,
            int barX,
            int trackY,
            int width,
            int trackHeight,
            int thumbY,
            int thumbHeight,
            boolean hovered
    ) {
        float alphaScale = VitrifyAnimator.screenAlphaFactor();
        float r = Math.min(3.0f, width * 0.5f);
        drawFlatSurface(graphics, barX, trackY, width, trackHeight, r, ARGB.color(Math.round(0.40f * alphaScale * 255.0f), 12, 13, 15), ARGB.color(Math.round(0.28f * alphaScale * 255.0f), 228, 230, 234), 0, 0.0f, -0.22f);

        int shade = hovered ? 248 : 224;
        int thumbFill = ARGB.color(Math.round((hovered ? 0.86f : 0.64f) * alphaScale * 255.0f), shade, shade, Math.min(255, shade + 2));
        drawFlatSurface(graphics, barX, thumbY, width, Math.max(6, thumbHeight), r, thumbFill, ARGB.color(Math.round(0.64f * alphaScale * 255.0f), 255, 255, 255), 0, 0.0f, 0.25f);
    }

    public static boolean replaceRecipeBookButtonSprite(
            AbstractWidget widget,
            float widgetAlpha,
            GuiGraphicsExtractor graphics,
            Identifier sprite,
            int x,
            int y,
            int width,
            int height
    ) {
        if (!VitrifyScreenContext.shouldStyleWidgets() || sprite == null || !"minecraft".equals(sprite.getNamespace())) {
            return false;
        }
        String path = sprite.getPath();
        if (path.equals("recipe_book/button") || path.equals("recipe_book/button_highlighted")) {
            drawRecipeBookToggleButton(graphics, x, y, width, height, widget.active, widgetAlpha, VitrifyAnimator.updateWidget(widget));
            return true;
        }
        if (path.startsWith("recipe_book/page_forward") || path.startsWith("recipe_book/page_backward")) {
            drawGlassButton(graphics, x - 2, y - 1, width + 4, height + 2, widget.active, widgetAlpha, VitrifyAnimator.updateWidget(widget));
            return false;
        }
        if (path.startsWith("recipe_book/") && path.contains("filter_")) {
            drawRecipeFilterButton(
                    graphics,
                    x,
                    y,
                    width,
                    height,
                    path.contains("filter_enabled"),
                    path.contains("furnace_"),
                    widget.active,
                    widgetAlpha,
                    VitrifyAnimator.updateWidget(widget)
            );
            return true;
        }
        return false;
    }

    private static void drawRecipeBookToggleButton(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int width,
            int height,
            boolean active,
            float widgetAlpha,
            VitrifyAnimator.WidgetState animState
    ) {
        drawGlassButton(graphics, x, y, width, height, active, widgetAlpha, animState);
        float alphaScale = widgetAlpha * VitrifyAnimator.screenAlphaFactor();
        if (alphaScale <= 0.02f) {
            return;
        }
        int inset = (active && animState.press > 0.35f) ? 1 : 0;
        internalDrawDepth++;
        try {
            graphics.blitSprite(
                    RenderPipelines.GUI_TEXTURED,
                    TOAST_RECIPE_BOOK_SPRITE,
                    20,
                    20,
                    0,
                    1,
                    x + inset,
                    y + inset,
                    width,
                    height,
                    ARGB.color(alpha8(alphaScale, 1.0f), 255, 255, 255)
            );
        } finally {
            internalDrawDepth--;
        }
    }

    public static void drawRecipeBookMagnifier(GuiGraphicsExtractor graphics, int x, int y, int size, float alphaScale) {
        if (alphaScale <= 0.02f) {
            return;
        }
        drawSdfIcon(graphics, x, y, size, size, MODE_MAGNIFIER, 0.0f, 0.85f, 0.70f, ARGB.color(alpha8(0.84f * alphaScale, 1.0f), 228, 231, 238));
    }

    private static void drawRecipeFilterButton(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int width,
            int height,
            boolean filtering,
            boolean furnace,
            boolean active,
            float widgetAlpha,
            VitrifyAnimator.WidgetState animState
    ) {
        VitrifyConfig cfg = VitrifyConfig.get();
        float alphaScale = widgetAlpha * VitrifyAnimator.screenAlphaFactor();
        if (alphaScale <= 0.02f) {
            return;
        }
        float hover = active ? Math.max(animState.hover, animState.focus * 0.85f) : 0.0f;
        int inset = (active && animState.press > 0.35f) ? 1 : 0;
        float r = Math.min(cfg.cornerRadius * 0.68f, height * 0.45f);

        int fillColor;
        int rimColor;
        if (filtering) {
            fillColor = ARGB.color(
                    alpha8((0.64f + 0.14f * hover) * alphaScale, 0.90f),
                    Math.round(Mth.lerp(hover, 24.0f, 38.0f)),
                    Math.round(Mth.lerp(hover, 96.0f, 128.0f)),
                    Math.round(Mth.lerp(hover, 56.0f, 76.0f))
            );
            rimColor = ARGB.color(alpha8((0.55f + 0.32f * hover) * alphaScale, 0.96f), 186, 255, 214);
        } else {
            int baseTint = VitrifyContrastAnalyzer.computeAdaptivePanelTint(alphaScale * 0.92f);
            fillColor = liftTint(baseTint, 20.0f * hover, 22.0f * hover, 22.0f * hover, 238);
            rimColor = VitrifyContrastAnalyzer.computeRimHighlightColor(alphaScale * (1.0f + 0.45f * hover), hover);
        }

        drawLayeredSurface(
                graphics,
                x + inset,
                y + inset,
                Math.max(2, width - inset * 2),
                Math.max(2, height - inset * 2),
                r,
                fillColor,
                rimColor,
                VitrifyContrastAnalyzer.computeOuterContourColor(alphaScale * 0.78f),
                VitrifyContrastAnalyzer.computeShadowColor(alphaScale * 0.45f),
                cfg.noiseStrength * 0.6f,
                0.35f,
                true,
                true,
                3.0f,
                alphaScale
        );

        int iconAlpha = alpha8(0.95f * alphaScale, 1.0f);
        if (filtering) {
            drawSdfIcon(graphics, x + 3 + inset, y + 4 + inset, 8, 8, MODE_CHECK, 0.0f, 0.88f, 0.75f, ARGB.color(iconAlpha, 224, 255, 234));
        } else {
            drawSdfIcon(graphics, x + 3 + inset, y + 4 + inset, 8, 8, MODE_CROSS, 2.3f, 0.85f, 0.75f, ARGB.color(iconAlpha, 255, 118, 124));
        }

        internalDrawDepth++;
        try {
            Matrix3x2fStack pose = graphics.pose();
            pose.pushMatrix();
            try {
                pose.translate(x + 18.5f + inset, y + 8.0f + inset);
                pose.scale(0.6875f, 0.6875f);
                pose.translate(-8.0f, -8.0f);
                graphics.fakeItem((furnace ? Items.FURNACE : Items.CRAFTING_TABLE).getDefaultInstance(), 0, 0);
            } finally {
                pose.popMatrix();
            }
        } finally {
            internalDrawDepth--;
        }
    }

    public static void drawRecipeTile(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int width,
            int height,
            boolean craftable,
            boolean hasMultiple,
            VitrifyAnimator.WidgetState animState,
            float alphaScale
    ) {
        VitrifyConfig cfg = VitrifyConfig.get();
        float hover = Math.max(animState.hover, animState.focus * 0.85f);
        float bright = VitrifyContrastAnalyzer.brightSceneFactor();
        float r = Math.min(3.8f, cfg.cornerRadius * 0.55f);

        int fillColor;
        int rimColor;
        int outerGroove;
        if (craftable) {
            fillColor = ARGB.color(
                    alpha8(Mth.lerp(hover, 0.38f + 0.10f * bright, 0.40f) * alphaScale, 0.80f),
                    Math.round(Mth.lerp(hover, 14.0f, 222.0f)),
                    Math.round(Mth.lerp(hover, 15.0f, 224.0f)),
                    Math.round(Mth.lerp(hover, 18.0f, 228.0f))
            );
            int shade = Math.round(Mth.lerp(hover, hasMultiple ? 248.0f : 236.0f, 255.0f));
            rimColor = ARGB.color(alpha8((0.36f + (hasMultiple ? 0.22f : 0.0f) + 0.46f * hover) * alphaScale, 0.96f), shade, shade, Math.min(255, shade + 2));
            outerGroove = ARGB.color(alpha8(0.28f * alphaScale, 0.58f), 8, 9, 11);
        } else {
            fillColor = ARGB.color(
                    alpha8((0.46f + 0.10f * bright + 0.14f * hover) * alphaScale, 0.84f),
                    Math.round(Mth.lerp(hover, 54.0f, 128.0f)),
                    Math.round(Mth.lerp(hover, 16.0f, 32.0f)),
                    Math.round(Mth.lerp(hover, 18.0f, 36.0f))
            );
            rimColor = ARGB.color(
                    alpha8((0.40f + (hasMultiple ? 0.18f : 0.0f) + 0.40f * hover) * alphaScale, 0.94f),
                    242,
                    Math.round(Mth.lerp(hover, 128.0f, 178.0f)),
                    Math.round(Mth.lerp(hover, 132.0f, 182.0f))
            );
            outerGroove = ARGB.color(alpha8(0.32f * alphaScale, 0.62f), 18, 6, 8);
        }
        drawFlatSurface(graphics, x, y, width, height, r, fillColor, rimColor, outerGroove, cfg.noiseStrength * 0.4f, -0.22f);
    }

    public static void drawRecipeOverlayPanel(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int width,
            int height,
            float alphaScale
    ) {
        VitrifyConfig cfg = VitrifyConfig.get();
        int denseAlpha = alpha8((0.82f + 0.08f * VitrifyContrastAnalyzer.brightSceneFactor()) * alphaScale, 0.94f);
        drawLayeredSurface(
                graphics,
                x,
                y,
                width,
                height,
                Math.min(cfg.cornerRadius + 0.5f, 6.5f),
                ARGB.color(denseAlpha, 26, 27, 30),
                VitrifyContrastAnalyzer.computeRimHighlightColor(alphaScale * 1.25f, 0.45f),
                VitrifyContrastAnalyzer.computeOuterContourColor(alphaScale * 1.10f),
                VitrifyContrastAnalyzer.computeShadowColor(alphaScale * 1.15f),
                cfg.noiseStrength,
                0.42f,
                true,
                true,
                6.5f,
                alphaScale
        );
    }

    private static void drawSdfIcon(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int width,
            int height,
            int mode,
            float shapeParam0,
            float shapeParam1,
            float outlineStrength,
            int color
    ) {
        if (width <= 0 || height <= 0 || ARGB.alpha(color) <= 2 || !(graphics instanceof GuiGraphicsAccess access)) {
            return;
        }
        access.vitrify$guiRenderState().addGuiElement(FrostedSdfQuadRenderState.createSdf(
                graphics.pose(),
                x,
                y,
                width,
                height,
                1.0f,
                shapeParam0,
                shapeParam1,
                mode,
                Mth.clamp(Math.round(outlineStrength * 1024.0f), 0, 1024),
                color,
                access.vitrify$currentScissor(),
                0,
                0
        ));
    }

    public static void drawGlassTooltip(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        VitrifyConfig cfg = VitrifyConfig.get();
        int denseAlpha = Math.round(Mth.clamp(0.66f + 0.14f * VitrifyContrastAnalyzer.brightSceneFactor(), 0.55f, 0.84f) * 255.0f);
        drawLayeredSurface(
                graphics,
                x - 4,
                y - 4,
                width + 8,
                height + 8,
                Math.min(cfg.cornerRadius * 0.80f, 6.5f),
                ARGB.color(denseAlpha, 44, 46, 50),
                VitrifyContrastAnalyzer.computeRimHighlightColor(1.20f, 0.45f),
                VitrifyContrastAnalyzer.computeOuterContourColor(1.05f),
                VitrifyContrastAnalyzer.computeShadowColor(1.10f),
                cfg.noiseStrength,
                0.45f,
                true,
                true,
                6.0f,
                1.0f
        );
    }

    public static void drawSmoothFocusRing(GuiGraphicsExtractor graphics) {
        float focusAlpha = VitrifyAnimator.focusAlpha();
        if (focusAlpha <= 0.03f || !VitrifyConfig.get().animationsEnabled) {
            return;
        }
        int x = Math.round(VitrifyAnimator.focusX()) - 1;
        int y = Math.round(VitrifyAnimator.focusY()) - 1;
        int w = Math.round(VitrifyAnimator.focusW()) + 2;
        int h = Math.round(VitrifyAnimator.focusH()) + 2;
        if (w <= 2 || h <= 2) {
            return;
        }
        float r = Math.min(VitrifyConfig.get().cornerRadius * 0.78f, Math.min(w, h) * 0.48f);
        drawFlatSurface(graphics, x, y, w, h, r, 0, ARGB.color(alpha8(0.50f * focusAlpha, 0.75f), 248, 249, 252), 0, 0.0f, 0.20f);
    }

    private static void drawFlatSurface(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int width,
            int height,
            float radius,
            int fillColor,
            int rimColor,
            int outerContourColor,
            float noiseStrength,
            float directionalBias
    ) {
        drawLayeredSurfaceExt(graphics, x, y, width, height, radius, fillColor, rimColor, outerContourColor, 0, noiseStrength, directionalBias, false, false, 0.0f, 1.0f, 0, 0, 0.0f, 0, 0);
    }

    private static void drawLayeredSurface(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int width,
            int height,
            float radius,
            int fillColor,
            int rimColor,
            int outerContourColor,
            int shadowColor,
            float noiseStrength,
            float directionalBias,
            boolean localizedBlur,
            boolean dropShadow,
            float shadowSpread,
            float alphaScale
    ) {
        drawLayeredSurfaceExt(graphics, x, y, width, height, radius, fillColor, rimColor, outerContourColor, shadowColor, noiseStrength, directionalBias, localizedBlur, dropShadow, shadowSpread, alphaScale, 0, 0, 0.0f, 0, 0);
    }

    private static void drawLayeredSurfaceExt(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int width,
            int height,
            float radius,
            int fillColor,
            int rimColor,
            int outerContourColor,
            int shadowColor,
            float noiseStrength,
            float directionalBias,
            boolean localizedBlur,
            boolean dropShadow,
            float shadowSpread,
            float alphaScale,
            int openSide,
            int tabSide,
            float tabCenter,
            int tabHalfSpan,
            int tabProtrude
    ) {
        if (width <= 0 || height <= 0 || !(graphics instanceof GuiGraphicsAccess access)) {
            return;
        }
        GuiRenderState state = access.vitrify$guiRenderState();
        ScreenRectangle scissor = access.vitrify$currentScissor();
        Matrix3x2fStack pose = graphics.pose();
        int spanEnc = tabSide != 0 ? Mth.clamp(tabHalfSpan - 6, 0, 15) : 0;
        int protEnc = tabSide != 0 ? Mth.clamp(tabProtrude - 20, 0, 15) : 0;
        int flagsPrefix = ((openSide & 3) << 3) | ((tabSide & 3) << 5) | (spanEnc << 7) | (protEnc << 11);
        float strokeParam = tabSide != 0 ? tabCenter : 1.0f;

        if (dropShadow && ARGB.alpha(shadowColor) > 2 && shadowSpread > 0.5f) {
            state.addGuiElement(FrostedSdfQuadRenderState.createSdf(
                    pose, x, y, width, height, shadowSpread + 2.0f, radius,
                    tabSide != 0 ? tabCenter : shadowSpread, MODE_SHADOW | flagsPrefix, 0,
                    shadowColor, scissor, tabSide, tabProtrude
            ));
        }

        if (localizedBlur && VitrifyBlurController.canQueueLocalizedBlurQuad()) {
            int blurAlpha = alpha8(VitrifyConfig.get().blurStrength * alphaScale, 1.0f);
            if (blurAlpha > 2) {
                state.addGuiElement(FrostedSdfQuadRenderState.createBlur(
                        VitrifyBlurController.markAndGetBlurTextureSetup(),
                        pose, x, y, width, height, radius, tabCenter, flagsPrefix,
                        ARGB.color(blurAlpha, 255, 255, 255), scissor, tabSide, tabProtrude
                ));
            }
        }

        if (ARGB.alpha(fillColor) > 1) {
            state.addGuiElement(FrostedSdfQuadRenderState.createSdf(
                    pose, x, y, width, height, 1.0f, radius, strokeParam, MODE_FILL | flagsPrefix,
                    Mth.clamp(Math.round(noiseStrength * 1024.0f), 0, 256), fillColor, scissor, tabSide, tabProtrude
            ));
        }

        if (ARGB.alpha(outerContourColor) > 2) {
            state.addGuiElement(FrostedSdfQuadRenderState.createSdf(
                    pose, x, y, width, height, 2.0f, radius, strokeParam, MODE_OUTER_CONTOUR | flagsPrefix,
                    0, outerContourColor, scissor, tabSide, tabProtrude
            ));
        }

        if (ARGB.alpha(rimColor) > 2) {
            state.addGuiElement(FrostedSdfQuadRenderState.createSdf(
                    pose, x, y, width, height, 1.0f, radius, strokeParam, MODE_RIM | flagsPrefix,
                    Mth.clamp(Math.round(directionalBias * 1024.0f), -1024, 1024), rimColor, scissor, tabSide, tabProtrude
            ));
        }
    }
}
