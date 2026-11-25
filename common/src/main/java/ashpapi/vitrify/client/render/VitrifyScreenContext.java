package ashpapi.vitrify.client.render;

import ashpapi.vitrify.client.anim.VitrifyAnimator;
import ashpapi.vitrify.client.config.VitrifyConfig;
import ashpapi.vitrify.client.gui.VitrifyConfigScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import net.minecraft.client.gui.screens.inventory.BookSignScreen;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2fStack;

public final class VitrifyScreenContext {
    private static Screen activeScreen;
    private static int screenPassDepth;
    private static int convertContainerLabelDepth;
    private static boolean inTopLevelBackgroundPass;
    private static boolean backgroundTransformPushed;
    private static boolean contentTransformPushed;

    private VitrifyScreenContext() {
    }

    public static void resetTopLevelFrame() {
        screenPassDepth = 0;
        convertContainerLabelDepth = 0;
        inTopLevelBackgroundPass = false;
        backgroundTransformPushed = false;
        contentTransformPushed = false;
        activeScreen = null;
    }

    public static void beginTopLevelFrame(Minecraft mc, Screen expectedScreen) {
        if (mc != null && mc.gui != null && mc.gui.screen() == expectedScreen) {
            resetTopLevelFrame();
            VitrifyAnimator.beginFrame(expectedScreen);
            VitrifyContrastAnalyzer.updatePerFrame(mc);
            VitrifyBlurController.onExtractionFrameStart();
        }
    }

    public static void enterTopLevelBackgroundPass() {
        inTopLevelBackgroundPass = true;
    }

    public static void exitTopLevelBackgroundPass(GuiGraphicsExtractor graphics) {
        inTopLevelBackgroundPass = false;
        if (backgroundTransformPushed) {
            backgroundTransformPushed = false;
            graphics.pose().popMatrix();
        }
    }

    public static boolean pushContentTransformIfNeeded(Screen screen, GuiGraphicsExtractor graphics) {
        if (shouldApplyEntranceTransform(screen) && pushEntranceMatrix(screen, graphics)) {
            contentTransformPushed = true;
            return true;
        }
        return false;
    }

    public static void popContentTransform(GuiGraphicsExtractor graphics) {
        contentTransformPushed = false;
        graphics.pose().popMatrix();
    }

    private static boolean pushEntranceMatrix(Screen screen, GuiGraphicsExtractor graphics) {
        float scale = VitrifyAnimator.screenMicroScale();
        float slideY = VitrifyAnimator.screenSlideOffsetY();
        if (Math.abs(1.0f - scale) <= 0.0005f && Math.abs(slideY) <= 0.02f) {
            return false;
        }
        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        float cx = screen.width * 0.5f;
        float cy = screen.height * 0.5f;
        pose.translate(cx, cy + slideY);
        pose.scale(scale, scale);
        pose.translate(-cx, -cy);
        return true;
    }

    public static void enterScreenPass(Screen screen) {
        activeScreen = screen;
        screenPassDepth++;
    }

    public static void exitScreenPass() {
        screenPassDepth = Math.max(0, screenPassDepth - 1);
        if (screenPassDepth == 0) {
            activeScreen = null;
            convertContainerLabelDepth = 0;
        }
    }

    private static Screen currentScreen() {
        if (activeScreen != null) {
            return activeScreen;
        }
        Minecraft mc = Minecraft.getInstance();
        return (mc != null && mc.gui != null) ? mc.gui.screen() : null;
    }

    private static boolean isFullChromeTarget(Screen screen, VitrifyConfig cfg) {
        return !cfg.containersOnly || screen instanceof AbstractContainerScreen<?> || screen instanceof VitrifyConfigScreen;
    }

    public static boolean isStylingActiveScreen() {
        return screenPassDepth > 0
                && activeScreen != null
                && !FrostedGlassRenderer.isInternalDraw()
                && VitrifyConfig.get().isScreenStyled(activeScreen);
    }

    public static boolean isFullChromeStyledScreen() {
        return isStylingActiveScreen() && isFullChromeTarget(activeScreen, VitrifyConfig.get());
    }

    public static boolean shouldStyleWidgets() {
        return isFullChromeStyledScreen() && VitrifyConfig.get().styleWidgets;
    }

    public static boolean isWidgetStylingEnabledNow() {
        VitrifyConfig cfg = VitrifyConfig.get();
        Screen screen = currentScreen();
        return cfg.enabled && cfg.styleWidgets && screen != null && cfg.isScreenStyled(screen) && isFullChromeTarget(screen, cfg);
    }

    public static boolean shouldStyleTooltips() {
        return isFullChromeStyledScreen() && VitrifyConfig.get().styleTooltips;
    }

    public static boolean isCreativeScreenActive() {
        return isFullChromeStyledScreen() && activeScreen instanceof CreativeModeInventoryScreen;
    }

    public static boolean isCreativeScreenActiveNow() {
        return VitrifyConfig.get().enabled
                && currentScreen() instanceof CreativeModeInventoryScreen creative
                && isSupportedContainer(creative);
    }

    private static boolean isBookScreen(Screen screen) {
        return screen instanceof BookViewScreen || screen instanceof BookEditScreen || screen instanceof BookSignScreen;
    }

    public static boolean isVanillaContainer(AbstractContainerScreen<?> screen) {
        return screen.getClass().getName().startsWith("net.minecraft.client.gui.screens.inventory.");
    }

    public static boolean isSupportedContainer(AbstractContainerScreen<?> screen) {
        return screen != null
                && VitrifyConfig.get().isScreenStyled(screen)
                && (isVanillaContainer(screen) || VitrifyConfig.get().autoStyleModdedContainers);
    }

    public static boolean shouldApplyEntranceTransform(Screen screen) {
        return isFullChromeStyledScreen()
                && screen != null
                && !isBookScreen(screen)
                && (!(screen instanceof AbstractContainerScreen<?> container) || isSupportedContainer(container));
    }

    public static void clearWidgetFocus(AbstractWidget widget) {
        Minecraft mc = Minecraft.getInstance();
        Screen screen = (mc != null && mc.gui != null) ? mc.gui.screen() : null;
        if (screen != null && screen.getFocused() == widget) {
            screen.setFocused(null);
        } else {
            widget.setFocused(false);
        }
    }

    public static void pushConvertContainerLabels() {
        convertContainerLabelDepth++;
    }

    public static void popConvertContainerLabels() {
        convertContainerLabelDepth = Math.max(0, convertContainerLabelDepth - 1);
    }

    public static boolean isConvertingContainerLabels() {
        return convertContainerLabelDepth > 0 && isFullChromeStyledScreen();
    }

    public static boolean handleInterceptedBlit(
            GuiGraphicsExtractor graphics,
            Identifier texture,
            int x0,
            int x1,
            int y0,
            int y1,
            float u0,
            float u1,
            float v0,
            float v1
    ) {
        int width = x1 - x0;
        int height = y1 - y0;
        if (texture == null || width <= 0 || height <= 0 || !isStylingActiveScreen()) {
            return false;
        }
        if ("minecraft".equals(texture.getNamespace()) && isFullChromeStyledScreen()) {
            String path = texture.getPath();
            if (path.equals("textures/gui/header_separator.png")
                    || path.equals("textures/gui/footer_separator.png")
                    || path.equals("textures/gui/inworld_header_separator.png")
                    || path.equals("textures/gui/inworld_footer_separator.png")) {
                return true;
            }
        }

        VitrifyConfig cfg = VitrifyConfig.get();
        boolean inContainer = activeScreen instanceof AbstractContainerScreen<?> acs && isSupportedContainer(acs);
        if (!inContainer && !cfg.autoStyleModdedContainers) {
            return false;
        }
        return handleClassifiedRegion(
                graphics,
                VitrifyTextureClassifier.classifyBlit(texture, u0, u1, v0, v1, width, height, inContainer),
                x0,
                y0,
                width,
                height,
                inContainer,
                true,
                1.0f
        );
    }

    public static boolean handleInterceptedSprite(
            GuiGraphicsExtractor graphics,
            Identifier sprite,
            int x,
            int y,
            int width,
            int height
    ) {
        if (sprite == null || !isStylingActiveScreen()) {
            return false;
        }
        boolean inContainer = activeScreen instanceof AbstractContainerScreen<?> acs && isSupportedContainer(acs);
        if (!inContainer && !VitrifyConfig.get().autoStyleModdedContainers) {
            return false;
        }
        return handleClassifiedRegion(
                graphics,
                VitrifyTextureClassifier.classifySprite(sprite, width, height, inContainer),
                x,
                y,
                width,
                height,
                inContainer,
                false,
                0.0f
        );
    }

    private static boolean handleClassifiedRegion(
            GuiGraphicsExtractor graphics,
            VitrifyTextureClassifier.RegionKind kind,
            int x,
            int y,
            int width,
            int height,
            boolean inContainer,
            boolean requireLargeOutsideContainer,
            float radiusExtra
    ) {
        if (kind == VitrifyTextureClassifier.RegionKind.OTHER) {
            return false;
        }
        VitrifyConfig cfg = VitrifyConfig.get();
        Matrix3x2fStack pose = graphics.pose();
        int sx0 = Math.round(pose.m00() * x + pose.m10() * y + pose.m20());
        int sy0 = Math.round(pose.m01() * x + pose.m11() * y + pose.m21());
        int sx1 = Math.round(pose.m00() * (x + width) + pose.m10() * (y + height) + pose.m20());
        int sy1 = Math.round(pose.m01() * (x + width) + pose.m11() * (y + height) + pose.m21());
        float alpha = VitrifyAnimator.screenAlphaFactor();

        if (kind == VitrifyTextureClassifier.RegionKind.SLOT_TILE) {
            if (!cfg.glassSlotsEnabled) {
                return false;
            }
            if (!inContainer || !(activeScreen instanceof ContainerBoundsAccess bounds) || !bounds.vitrify$hasMenuSlotNear(sx0 + 1, sy0 + 1)) {
                FrostedGlassRenderer.drawGlassSlot(graphics, x + 1, y + 1, 0.0f, false, alpha);
            }
            return true;
        }

        if (inContainer && activeScreen instanceof ContainerBoundsAccess bounds) {
            int[] b = bounds.vitrify$effectiveBounds();
            if (sx0 >= b[0] - 8 && sx1 <= b[0] + b[2] + 8 && sy0 >= b[1] - 8 && sy1 <= b[1] + b[3] + 8) {
                return true;
            }
        } else if (requireLargeOutsideContainer && (width < 80 || height < 56)) {
            return false;
        }

        float r = Math.min(cfg.cornerRadius + radiusExtra, Math.min(width, height) * 0.35f);
        FrostedGlassRenderer.drawFrostedPanel(graphics, x, y, width, height, r, alpha, 0.06f, true, true);
        return true;
    }

    public static boolean handleInterceptedSubSprite(Identifier sprite, int width, int height) {
        return sprite != null
                && width > 0
                && height > 0
                && isStylingActiveScreen()
                && VitrifyConfig.get().glassSlotsEnabled
                && activeScreen instanceof AbstractContainerScreen<?> acs
                && isSupportedContainer(acs)
                && sprite.getPath().endsWith("/chest_slots");
    }

    public static void onAfterBaseScreenBackground(Screen screen, GuiGraphicsExtractor graphics) {
        if (!isStylingActiveScreen() || screen == null) {
            return;
        }
        VitrifyConfig cfg = VitrifyConfig.get();
        float alpha = VitrifyAnimator.screenAlphaFactor();

        if (screen instanceof AbstractContainerScreen<?> container
                && isSupportedContainer(container)
                && container instanceof ContainerBoundsAccess bounds) {
            int[] b = bounds.vitrify$effectiveBounds();
            if (b[2] < 32 || b[3] < 32) {
                return;
            }
            if (inTopLevelBackgroundPass && !backgroundTransformPushed && !contentTransformPushed && shouldApplyEntranceTransform(screen)) {
                backgroundTransformPushed = pushEntranceMatrix(screen, graphics);
            }
            if (container instanceof CreativeModeInventoryScreen) {
                return;
            }
            FrostedGlassRenderer.drawFrostedPanel(
                    graphics,
                    b[0] - 4,
                    b[1] - 4,
                    b[2] + 8,
                    b[3] + 8,
                    cfg.cornerRadius + 1.5f,
                    alpha,
                    0.08f,
                    true,
                    true
            );
            return;
        }

        if (!cfg.containersOnly && isBookScreen(screen)) {
            FrostedGlassRenderer.drawFrostedPanel(
                    graphics,
                    (screen.width - 192) / 2 - 6,
                    2,
                    204,
                    196,
                    cfg.cornerRadius + 2.0f,
                    alpha,
                    0.06f,
                    true,
                    true
            );
        }
    }

    public static void drawAutomaticScreenCardIfNeeded(Screen screen, GuiGraphicsExtractor graphics) {
        if (!isFullChromeStyledScreen()
                || VitrifyConfig.get().containersOnly
                || screen instanceof AbstractContainerScreen<?>
                || isBookScreen(screen)
                || screen instanceof ChatScreen
                || screen instanceof TitleScreen
                || screen instanceof VitrifyConfigScreen) {
            return;
        }

        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int visibleWidgets = 0;

        for (GuiEventListener child : screen.children()) {
            if (child instanceof AbstractSelectionList<?>) {
                return;
            }
            if (child instanceof AbstractWidget widget && widget.visible && widget.getWidth() > 0 && widget.getHeight() > 0) {
                minX = Math.min(minX, widget.getX());
                minY = Math.min(minY, widget.getY());
                maxX = Math.max(maxX, widget.getRight());
                maxY = Math.max(maxY, widget.getBottom());
                visibleWidgets++;
            }
        }

        if (visibleWidgets > 0) {
            int cardX = Math.max(6, minX - 14);
            int cardY = Math.max(6, minY - Math.min(Math.max(12, minY - 6), 28));
            int cardRight = Math.min(screen.width - 6, maxX + 14);
            int cardBottom = Math.min(screen.height - 6, maxY + 12);
            if (cardRight - cardX >= 48 && cardBottom - cardY >= 32) {
                FrostedGlassRenderer.drawFrostedPanel(
                        graphics,
                        cardX,
                        cardY,
                        cardRight - cardX,
                        cardBottom - cardY,
                        VitrifyConfig.get().cornerRadius + 2.0f,
                        VitrifyAnimator.screenAlphaFactor(),
                        0.08f,
                        true,
                        true
                );
            }
        }
    }
}
