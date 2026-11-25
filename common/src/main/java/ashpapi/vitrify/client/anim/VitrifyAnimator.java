package ashpapi.vitrify.client.anim;

import ashpapi.vitrify.client.config.VitrifyConfig;
import ashpapi.vitrify.client.render.WidgetBoundsAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.Mth;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

public final class VitrifyAnimator {
    private static final Map<AbstractWidget, WidgetState> WIDGET_STATES = new WeakHashMap<>();
    private static final Map<Object, Float> SLOT_HOVER_STATES = new WeakHashMap<>();

    private static long lastFrameNanos = System.nanoTime();
    private static float frameDeltaSeconds = 1.0f / 60.0f;
    private static long currentFrameId;
    private static boolean mouseDownThisFrame;
    private static WeakReference<Screen> activeScreenRef = new WeakReference<>(null);
    private static float screenOpenProgress = 1.0f;

    private static boolean focusRingInitialized;
    private static float focusX;
    private static float focusY;
    private static float focusW;
    private static float focusH;
    private static float focusAlpha;
    private static boolean hotbarSelectionInitialized;
    private static float hotbarSelectionSlot;
    private static float hotbarSelectionTarget;
    private static int lastKnownHotbarSlot;

    private VitrifyAnimator() {
    }

    public static void beginFrame(Screen currentScreen) {
        long now = System.nanoTime();
        frameDeltaSeconds = Mth.clamp(now <= lastFrameNanos ? 0.0f : (now - lastFrameNanos) * 1.0e-9f, 0.0f, 0.05f);
        lastFrameNanos = now;
        currentFrameId++;
        mouseDownThisFrame = isAnyMouseButtonDown();

        VitrifyConfig cfg = VitrifyConfig.get();
        if (currentScreen != activeScreenRef.get()) {
            activeScreenRef = new WeakReference<>(currentScreen);
            focusRingInitialized = false;
            focusAlpha = 0.0f;
            screenOpenProgress = (currentScreen != null && cfg.enabled && cfg.animationsEnabled) ? 0.0f : 1.0f;
        }

        if (!cfg.enabled || !cfg.animationsEnabled) {
            screenOpenProgress = 1.0f;
            return;
        }
        if (screenOpenProgress < 1.0f) {
            screenOpenProgress = Math.min(1.0f, screenOpenProgress + frameDeltaSeconds * 5.8f * cfg.animationSpeed);
        }
    }

    public static float frameDelta() {
        return frameDeltaSeconds;
    }

    private static float screenEasedOpen() {
        VitrifyConfig cfg = VitrifyConfig.get();
        if (!cfg.enabled || !cfg.animationsEnabled) {
            return 1.0f;
        }
        float inv = 1.0f - Mth.clamp(screenOpenProgress, 0.0f, 1.0f);
        return 1.0f - inv * inv * inv;
    }

    public static float screenAlphaFactor() {
        return Mth.clamp(0.18f + 0.82f * screenEasedOpen(), 0.0f, 1.0f);
    }

    public static float screenMicroScale() {
        return 0.945f + 0.055f * screenEasedOpen();
    }

    public static float screenSlideOffsetY() {
        return (1.0f - screenEasedOpen()) * 7.5f;
    }

    public static boolean isKeyboardFocusActive() {
        Minecraft mc = Minecraft.getInstance();
        return mc != null && mc.getLastInputType().isKeyboard();
    }

    private static boolean isAnyMouseButtonDown() {
        Minecraft mc = Minecraft.getInstance();
        return mc != null
                && mc.mouseHandler != null
                && (mc.mouseHandler.isLeftPressed() || mc.mouseHandler.isRightPressed());
    }

    private static float expStep(float rate, float speed) {
        return 1.0f - (float) Math.exp(-rate * frameDeltaSeconds * speed);
    }

    private static float expLerp(float current, float target, float rate, float speed) {
        return Mth.lerp(expStep(rate, speed), current, target);
    }

    public static WidgetState updateWidget(AbstractWidget widget) {
        return updateWidget(widget, widget.isHovered(), widget.isFocused());
    }

    public static WidgetState updateWidget(AbstractWidget widget, boolean hovered, boolean focused) {
        WidgetState state = WIDGET_STATES.computeIfAbsent(widget, k -> new WidgetState());
        if (state.pressedDown && !mouseDownThisFrame) {
            state.pressedDown = false;
        }
        VitrifyConfig cfg = VitrifyConfig.get();
        boolean showFocus = focused && ((widget instanceof EditBox) || isKeyboardFocusActive());
        float targetHover = (widget.active && hovered) ? 1.0f : 0.0f;
        float targetFocus = (widget.active && showFocus) ? 1.0f : 0.0f;
        if (!cfg.enabled || !cfg.animationsEnabled) {
            state.hover = targetHover;
            state.focus = targetFocus;
            state.press = 0.0f;
            state.pressedDown = false;
            state.lastFrameId = currentFrameId;
            return state;
        }
        if (state.lastFrameId == currentFrameId) {
            return state;
        }
        state.lastFrameId = currentFrameId;
        float speed = cfg.animationSpeed;
        state.hover = expLerp(state.hover, targetHover, 16.0f, speed);
        state.focus = expLerp(state.focus, targetFocus, 14.0f, speed);
        state.press = expLerp(state.press, state.pressedDown ? 1.0f : 0.0f, 18.0f, speed);

        if (showFocus && widget.visible && !(widget instanceof EditBox)) {
            if (widget instanceof WidgetBoundsAccess bounds && bounds.vitrify$hasCustomBounds()) {
                updateFocusTarget(bounds.vitrify$visualX(), bounds.vitrify$visualY(), bounds.vitrify$visualWidth(), bounds.vitrify$visualHeight(), speed);
            } else {
                updateFocusTarget(widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight(), speed);
            }
        }
        return state;
    }

    public static void notifyWidgetPress(AbstractWidget widget, boolean down) {
        WidgetState state = WIDGET_STATES.computeIfAbsent(widget, k -> new WidgetState());
        state.pressedDown = down;
        if (down) {
            mouseDownThisFrame = true;
            state.press = Math.max(state.press, 0.65f);
        }
    }

    public static float updateSlotHover(Object slotKey, boolean hovered) {
        VitrifyConfig cfg = VitrifyConfig.get();
        float target = hovered ? 1.0f : 0.0f;
        if (!cfg.enabled || !cfg.animationsEnabled) {
            SLOT_HOVER_STATES.put(slotKey, target);
            return target;
        }
        float next = expLerp(SLOT_HOVER_STATES.getOrDefault(slotKey, 0.0f), target, 20.0f, cfg.animationSpeed);
        if (next < 0.002f && !hovered) {
            SLOT_HOVER_STATES.remove(slotKey);
            return 0.0f;
        }
        SLOT_HOVER_STATES.put(slotKey, next);
        return next;
    }

    private static float wrapHotbarSlot(float slot) {
        float wrapped = slot % 9.0f;
        if (wrapped < 0.0f) {
            wrapped += 9.0f;
        }
        return wrapped >= 9.0f ? 0.0f : wrapped;
    }

    public static void onHotbarScroll(int prevSlot, int newSlot) {
        int clampedPrev = Mth.clamp(prevSlot, 0, 8);
        int clampedNext = Mth.clamp(newSlot, 0, 8);
        VitrifyConfig cfg = VitrifyConfig.get();
        if (!cfg.enabled || !cfg.animationsEnabled) {
            hotbarSelectionSlot = clampedNext;
            hotbarSelectionTarget = clampedNext;
            lastKnownHotbarSlot = clampedNext;
            hotbarSelectionInitialized = true;
            return;
        }
        if (!hotbarSelectionInitialized) {
            hotbarSelectionSlot = clampedPrev;
            hotbarSelectionTarget = clampedPrev;
            lastKnownHotbarSlot = clampedPrev;
            hotbarSelectionInitialized = true;
        } else if (clampedPrev != lastKnownHotbarSlot) {
            hotbarSelectionTarget += clampedPrev - lastKnownHotbarSlot;
        }
        int delta = clampedNext - clampedPrev;
        if (delta > 4) {
            delta -= 9;
        } else if (delta < -4) {
            delta += 9;
        }
        hotbarSelectionTarget += delta;
        while (hotbarSelectionTarget - hotbarSelectionSlot > 13.5f) {
            hotbarSelectionTarget -= 9.0f;
        }
        while (hotbarSelectionTarget - hotbarSelectionSlot < -13.5f) {
            hotbarSelectionTarget += 9.0f;
        }
        lastKnownHotbarSlot = clampedNext;
    }

    public static float updateHotbarSelection(int selectedSlot) {
        int clampedSlot = Mth.clamp(selectedSlot, 0, 8);
        VitrifyConfig cfg = VitrifyConfig.get();
        if (!cfg.enabled || !cfg.animationsEnabled || !hotbarSelectionInitialized) {
            hotbarSelectionSlot = clampedSlot;
            hotbarSelectionTarget = clampedSlot;
            lastKnownHotbarSlot = clampedSlot;
            hotbarSelectionInitialized = true;
            return hotbarSelectionSlot;
        }
        if (clampedSlot != lastKnownHotbarSlot) {
            hotbarSelectionSlot = wrapHotbarSlot(hotbarSelectionSlot);
            hotbarSelectionTarget = clampedSlot;
            lastKnownHotbarSlot = clampedSlot;
        }
        hotbarSelectionSlot = expLerp(hotbarSelectionSlot, hotbarSelectionTarget, 22.0f, cfg.animationSpeed);
        if (Math.abs(hotbarSelectionSlot - hotbarSelectionTarget) < 0.004f) {
            hotbarSelectionSlot = wrapHotbarSlot(hotbarSelectionTarget);
            hotbarSelectionTarget = hotbarSelectionSlot;
        }
        return wrapHotbarSlot(hotbarSelectionSlot);
    }

    private static void updateFocusTarget(int x, int y, int w, int h, float speed) {
        if (!focusRingInitialized) {
            focusX = x;
            focusY = y;
            focusW = w;
            focusH = h;
            focusAlpha = 1.0f;
            focusRingInitialized = true;
            return;
        }
        float glide = expStep(18.0f, speed);
        focusX = Mth.lerp(glide, focusX, x);
        focusY = Mth.lerp(glide, focusY, y);
        focusW = Mth.lerp(glide, focusW, w);
        focusH = Mth.lerp(glide, focusH, h);
        focusAlpha = Mth.lerp(glide, focusAlpha, 1.0f);
    }

    public static void decayFocusIfNone(boolean anyWidgetFocused) {
        if (!anyWidgetFocused && focusRingInitialized) {
            focusAlpha = expLerp(focusAlpha, 0.0f, 14.0f, VitrifyConfig.get().animationSpeed);
            if (focusAlpha < 0.02f) {
                focusRingInitialized = false;
                focusAlpha = 0.0f;
            }
        }
    }

    public static float focusX() {
        return focusX;
    }

    public static float focusY() {
        return focusY;
    }

    public static float focusW() {
        return focusW;
    }

    public static float focusH() {
        return focusH;
    }

    public static float focusAlpha() {
        return focusAlpha;
    }

    public static final class WidgetState {
        public float hover;
        public float focus;
        public float press;
        public boolean pressedDown;
        public long lastFrameId = -1L;
    }
}
