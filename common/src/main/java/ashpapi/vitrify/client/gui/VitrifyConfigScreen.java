package ashpapi.vitrify.client.gui;

import ashpapi.vitrify.client.anim.VitrifyAnimator;
import ashpapi.vitrify.client.config.VitrifyConfig;
import ashpapi.vitrify.client.config.VitrifyPreset;
import ashpapi.vitrify.client.render.FrostedGlassRenderer;
import ashpapi.vitrify.client.render.VitrifyBlurController;
import ashpapi.vitrify.client.render.VitrifyContrastAnalyzer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.Function;

public final class VitrifyConfigScreen extends Screen {
    private static final int[] TINT_PALETTE = {
            0x242528, 0x17181A, 0x323438, 0x44464B, 0x585A60, 0x111214
    };

    private final Screen parent;
    private final List<ScrollEntry> scrollEntries = new ArrayList<>();
    private final VitrifyAnimator.WidgetState previewBtnState = new VitrifyAnimator.WidgetState();
    private List<ItemStack> previewItems;

    private double scrollOffset;
    private int maxScroll;
    private int listLeft;
    private int listTop;
    private int listWidth;
    private int listBottom;

    private int previewLeft;
    private int previewTop;
    private int previewWidth;
    private int previewHeight;
    private PreviewBackdrop previewBackdrop = PreviewBackdrop.AUTO_SCENE;

    public VitrifyConfigScreen(Screen parent) {
        super(Component.translatable("vitrify.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.scrollEntries.clear();
        VitrifyConfig cfg = VitrifyConfig.get();

        int outerMargin = 14;
        int usableW = this.width - outerMargin * 2;
        this.listWidth = Math.max(190, Math.min(280, (int) (usableW * 0.54f)));
        this.listLeft = outerMargin;
        this.listTop = 36;
        this.listBottom = this.height - 36;

        this.previewLeft = this.listLeft + this.listWidth + 12;
        this.previewTop = 36;
        this.previewWidth = Math.max(140, this.width - this.previewLeft - outerMargin);
        this.previewHeight = this.listBottom - this.previewTop;

        int ctrlW = this.listWidth - 18;
        int ctrlX = this.listLeft + 8;
        int curY = 0;

        curY = addToggleRow(ctrlX, ctrlW, curY, "vitrify.config.enabled", () -> cfg.enabled, v -> cfg.enabled = v);
        curY = addToggleRow(ctrlX, ctrlW, curY, "vitrify.config.containers_only", () -> cfg.containersOnly, v -> cfg.containersOnly = v);
        curY = addToggleRow(ctrlX, ctrlW, curY, "vitrify.config.auto_modded", () -> cfg.autoStyleModdedContainers, v -> cfg.autoStyleModdedContainers = v);
        curY = addToggleRow(ctrlX, ctrlW, curY, "vitrify.config.style_hotbar", () -> cfg.styleHotbar, v -> cfg.styleHotbar = v);

        curY = addRow(Button.builder(Component.translatable("vitrify.config.preset", Component.translatable(cfg.preset.translationKey())), btn -> {
            cfg.applyPreset(switch (cfg.preset) {
                case BALANCED -> VitrifyPreset.DARK_FROSTED;
                case DARK_FROSTED -> VitrifyPreset.HIGH_CONTRAST;
                case HIGH_CONTRAST, CUSTOM -> VitrifyPreset.BALANCED;
            });
            double savedScroll = this.scrollOffset;
            this.rebuildWidgets();
            this.scrollOffset = Mth.clamp(savedScroll, 0.0, this.maxScroll);
            updateScrolledPositions();
        }).bounds(ctrlX, 0, ctrlW, 20).build(), curY);

        curY = addToggleRow(ctrlX, ctrlW, curY, "vitrify.config.local_blur", () -> cfg.localBlurEnabled, v -> cfg.localBlurEnabled = v);
        curY = addPercentSliderRow(ctrlX, ctrlW, curY, 0.05, 0.90, "vitrify.config.panel_opacity", () -> cfg.panelOpacity, v -> cfg.panelOpacity = (float) v);
        curY = addPercentSliderRow(ctrlX, ctrlW, curY, 0.0, 1.0, "vitrify.config.blur_strength", () -> cfg.blurStrength, v -> cfg.blurStrength = (float) v);
        curY = addFormattedSliderRow(ctrlX, ctrlW, curY, 1.0, 16.0, "vitrify.config.blur_radius", "%.1f", 1.0, true, () -> cfg.blurRadius, v -> cfg.blurRadius = (float) v);
        curY = addFormattedSliderRow(ctrlX, ctrlW, curY, 0.0, 18.0, "vitrify.config.corner_radius", "%.1f", 1.0, true, () -> cfg.cornerRadius, v -> cfg.cornerRadius = (float) v);
        curY = addPercentSliderRow(ctrlX, ctrlW, curY, 0.05, 0.90, "vitrify.config.tint_density", () -> cfg.tintDensity, v -> cfg.tintDensity = (float) v);

        int halfW = (ctrlW - 4) / 2;
        EditBox hexTintInput = new EditBox(this.font, ctrlX, 0, halfW, 20, Component.translatable("vitrify.config.tint_hex"));
        hexTintInput.setMaxLength(7);
        hexTintInput.setValue(formatHexColor(cfg.tintColorRgb));
        hexTintInput.setResponder(text -> {
            String clean = text.startsWith("#") ? text.substring(1) : text;
            if (clean.length() == 6 && clean.chars().allMatch(c -> Character.digit(c, 16) >= 0)) {
                cfg.tintColorRgb = Integer.parseInt(clean, 16) & 0xFFFFFF;
                cfg.markCustom();
            }
        });
        addScrollWidget(hexTintInput, curY);

        curY = addRow(Button.builder(Component.translatable("vitrify.config.cycle_tint"), btn -> {
            int idx = 0;
            for (int i = 0; i < TINT_PALETTE.length; i++) {
                if (TINT_PALETTE[i] == (cfg.tintColorRgb & 0xFFFFFF)) {
                    idx = (i + 1) % TINT_PALETTE.length;
                    break;
                }
            }
            cfg.tintColorRgb = TINT_PALETTE[idx];
            cfg.markCustom();
            hexTintInput.setValue(formatHexColor(cfg.tintColorRgb));
        }).bounds(ctrlX + halfW + 4, 0, ctrlW - halfW - 4, 20).build(), curY);

        curY = addFormattedSliderRow(ctrlX, ctrlW, curY, 0.0, 0.08, "vitrify.config.noise_strength", "%.1f", 100.0, true, () -> cfg.noiseStrength, v -> cfg.noiseStrength = (float) v);
        curY = addPercentSliderRow(ctrlX, ctrlW, curY, 0.0, 0.80, "vitrify.config.rim_brightness", () -> cfg.rimBrightness, v -> cfg.rimBrightness = (float) v);
        curY = addPercentSliderRow(ctrlX, ctrlW, curY, 0.0, 0.85, "vitrify.config.shadow_strength", () -> cfg.shadowStrength, v -> cfg.shadowStrength = (float) v);
        curY = addToggleRow(ctrlX, ctrlW, curY, "vitrify.config.auto_contrast", () -> cfg.autoContrast, v -> cfg.autoContrast = v);
        curY = addPercentSliderRow(ctrlX, ctrlW, curY, 0.20, 0.85, "vitrify.config.auto_contrast_threshold", () -> cfg.autoContrastThreshold, v -> cfg.autoContrastThreshold = (float) v);
        curY = addToggleRow(ctrlX, ctrlW, curY, "vitrify.config.animations", () -> cfg.animationsEnabled, v -> cfg.animationsEnabled = v);
        curY = addFormattedSliderRow(ctrlX, ctrlW, curY, 0.25, 2.50, "vitrify.config.animation_speed", "%.2fx", 1.0, false, () -> cfg.animationSpeed, v -> cfg.animationSpeed = (float) v);
        curY = addToggleRow(ctrlX, ctrlW, curY, "vitrify.config.glass_slots", () -> cfg.glassSlotsEnabled, v -> cfg.glassSlotsEnabled = v);
        curY = addToggleRow(ctrlX, ctrlW, curY, "vitrify.config.style_widgets", () -> cfg.styleWidgets, v -> cfg.styleWidgets = v);
        curY = addToggleRow(ctrlX, ctrlW, curY, "vitrify.config.style_tooltips", () -> cfg.styleTooltips, v -> cfg.styleTooltips = v);

        EditBox excludedClassesInput = new EditBox(this.font, ctrlX, 0, ctrlW, 20, Component.translatable("vitrify.config.excluded_screens"));
        excludedClassesInput.setMaxLength(1024);
        excludedClassesInput.setHint(Component.translatable("vitrify.config.excluded_hint"));
        excludedClassesInput.setValue(cfg.excludedScreensAsText());
        excludedClassesInput.setResponder(cfg::setExcludedScreensFromText);
        curY = addRow(excludedClassesInput, curY) + 4;

        int viewHeight = Math.max(40, this.listBottom - this.listTop - 12);
        this.maxScroll = Math.max(0, curY - viewHeight);
        this.scrollOffset = Mth.clamp(this.scrollOffset, 0.0, this.maxScroll);
        updateScrolledPositions();

        int previewBtnW = Math.min(this.previewWidth - 16, 190);
        this.addRenderableWidget(Button.builder(previewBackdropLabel(), btn -> {
            this.previewBackdrop = this.previewBackdrop.next();
            btn.setMessage(previewBackdropLabel());
        }).bounds(this.previewLeft + (this.previewWidth - previewBtnW) / 2, this.previewTop + this.previewHeight - 28, previewBtnW, 20).build());

        int footerBtnW = 110;
        int footerY = this.height - 28;
        this.addRenderableWidget(Button.builder(Component.translatable("vitrify.config.reset"), btn -> {
            cfg.resetToDefaults();
            this.rebuildWidgets();
        }).bounds(this.width / 2 - footerBtnW - 6, footerY, footerBtnW, 20).build());

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, btn -> this.onClose())
                .bounds(this.width / 2 + 6, footerY, footerBtnW, 20).build());
    }

    private static String formatHexColor(int rgb) {
        return String.format(Locale.ROOT, "#%06X", rgb & 0xFFFFFF);
    }

    private void addScrollWidget(AbstractWidget widget, int baseOffsetY) {
        this.scrollEntries.add(new ScrollEntry(widget, baseOffsetY));
        this.addRenderableWidget(widget);
    }

    private int addRow(AbstractWidget widget, int curY) {
        addScrollWidget(widget, curY);
        return curY + 24;
    }

    private int addToggleRow(int x, int w, int curY, String key, BooleanSupplier getter, Consumer<Boolean> setter) {
        return addRow(Button.builder(toggleText(key, getter.getAsBoolean()), btn -> {
            boolean next = !getter.getAsBoolean();
            setter.accept(next);
            btn.setMessage(toggleText(key, next));
        }).bounds(x, 0, w, 20).build(), curY);
    }

    private int addPercentSliderRow(int x, int w, int curY, double min, double max, String key, DoubleSupplier getter, DoubleConsumer setter) {
        VitrifyConfig cfg = VitrifyConfig.get();
        return addRow(new FloatRangeSlider(x, w, min, max, getter, v -> {
            setter.accept(v);
            cfg.markCustom();
        }, v -> Component.translatable(key, Math.round(v * 100.0))), curY);
    }

    private int addFormattedSliderRow(
            int x,
            int w,
            int curY,
            double min,
            double max,
            String key,
            String fmt,
            double displayScale,
            boolean customPreset,
            DoubleSupplier getter,
            DoubleConsumer setter
    ) {
        VitrifyConfig cfg = VitrifyConfig.get();
        return addRow(new FloatRangeSlider(x, w, min, max, getter, v -> {
            setter.accept(v);
            if (customPreset) {
                cfg.markCustom();
            }
        }, v -> Component.translatable(key, String.format(Locale.ROOT, fmt, v * displayScale))), curY);
    }

    private void updateScrolledPositions() {
        int baseTop = this.listTop + 6 - (int) Math.round(this.scrollOffset);
        for (ScrollEntry entry : this.scrollEntries) {
            int wy = baseTop + entry.baseY;
            entry.widget.setY(wy);
            entry.widget.visible = wy + entry.widget.getHeight() > this.listTop + 2 && wy < this.listBottom - 2;
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x();
        double my = event.y();
        boolean insideScrollArea = mx >= this.listLeft + 2
                && mx <= this.listLeft + this.listWidth - 2
                && my >= this.listTop + 3
                && my <= this.listBottom - 3;
        if (!insideScrollArea) {
            for (GuiEventListener child : this.children()) {
                if (child instanceof AbstractWidget widget && isScrollWidget(widget)) {
                    continue;
                }
                if (child.mouseClicked(event, doubleClick)) {
                    this.setFocused(child);
                    if (event.button() == 0) {
                        this.setDragging(true);
                    }
                    return true;
                }
            }
            return false;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX >= this.listLeft && mouseX <= this.listLeft + this.listWidth && mouseY >= this.listTop && mouseY <= this.listBottom && this.maxScroll > 0) {
            this.scrollOffset = Mth.clamp(this.scrollOffset - scrollY * 18.0, 0.0, this.maxScroll);
            updateScrolledPositions();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        VitrifyConfig cfg = VitrifyConfig.get();
        float alpha = VitrifyAnimator.screenAlphaFactor();

        FrostedGlassRenderer.drawFrostedPanel(
                graphics,
                this.listLeft,
                this.listTop,
                this.listWidth,
                this.listBottom - this.listTop,
                cfg.cornerRadius + 2.0f,
                alpha,
                0.06f,
                true,
                true
        );

        renderLivePreviewPane(graphics, mouseX, mouseY, alpha);

        graphics.centeredText(this.font, this.title, this.width / 2, 10, 0xFFF6F7F9);
        Component backendBadge = VitrifyBlurController.shouldUseLocalizedBlur()
                ? Component.translatable("vitrify.config.status.localized").withStyle(ChatFormatting.WHITE)
                : Component.translatable("vitrify.config.status.fallback").withStyle(ChatFormatting.GRAY);
        graphics.centeredText(this.font, backendBadge, this.width / 2, 22, 0xFFD4D7DC);

        graphics.enableScissor(this.listLeft + 2, this.listTop + 3, this.listLeft + this.listWidth - 2, this.listBottom - 3);
        try {
            for (ScrollEntry entry : this.scrollEntries) {
                if (entry.widget.visible) {
                    entry.widget.extractRenderState(graphics, mouseX, mouseY, partialTick);
                }
            }
        } finally {
            graphics.disableScissor();
        }

        if (this.maxScroll > 0) {
            int trackH = this.listBottom - this.listTop - 8;
            int thumbH = Math.max(18, (int) ((trackH * (float) trackH) / (trackH + this.maxScroll)));
            int thumbY = this.listTop + 4 + (int) Math.round((this.scrollOffset / this.maxScroll) * (trackH - thumbH));
            FrostedGlassRenderer.drawGlassScrollbar(
                    graphics,
                    this.listLeft + this.listWidth - 6,
                    this.listTop + 4,
                    4,
                    trackH,
                    thumbY,
                    thumbH,
                    mouseX >= this.listLeft + this.listWidth - 8 && mouseX <= this.listLeft + this.listWidth
            );
        }

        for (var child : this.children()) {
            if (child instanceof AbstractWidget widget && !isScrollWidget(widget)) {
                widget.extractRenderState(graphics, mouseX, mouseY, partialTick);
            }
        }
    }

    private List<ItemStack> getPreviewItems() {
        if (this.previewItems == null) {
            this.previewItems = List.of(
                    new ItemStack(Items.DIAMOND_SWORD),
                    new ItemStack(Items.GOLDEN_APPLE, 12),
                    new ItemStack(Items.ENDER_PEARL, 16),
                    new ItemStack(Items.AMETHYST_SHARD, 32),
                    ItemStack.EMPTY,
                    new ItemStack(Items.ENCHANTED_BOOK),
                    new ItemStack(Items.REDSTONE, 64),
                    new ItemStack(Items.LANTERN, 4)
            );
        }
        return this.previewItems;
    }

    private boolean isScrollWidget(AbstractWidget candidate) {
        for (ScrollEntry entry : this.scrollEntries) {
            if (entry.widget == candidate) {
                return true;
            }
        }
        return false;
    }

    private void renderLivePreviewPane(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float alpha) {
        VitrifyConfig cfg = VitrifyConfig.get();
        int px = this.previewLeft;
        int py = this.previewTop;
        int pw = this.previewWidth;
        int ph = this.previewHeight;

        drawSimulatedBackdrop(graphics, px, py, pw, ph);

        if (this.previewBackdrop.luminanceOverride >= 0.0f) {
            VitrifyContrastAnalyzer.setPreviewLuminanceOverride(this.previewBackdrop.luminanceOverride);
        }
        try {
            int cardW = Math.min(pw - 20, 176);
            int cardH = Math.min(ph - 52, 134);
            int cardX = px + (pw - cardW) / 2;
            int cardY = py + Math.max(12, (ph - 34 - cardH) / 2);

            FrostedGlassRenderer.drawFrostedPanel(graphics, cardX, cardY, cardW, cardH, cfg.cornerRadius + 1.5f, alpha, 0.10f, true, true);
            graphics.text(this.font, Component.translatable("vitrify.preview.card_title"), cardX + 10, cardY + 8, VitrifyContrastAnalyzer.computeContainerLabelColor(alpha), true);

            int cols = 4;
            int rows = 2;
            int startX = cardX + (cardW - cols * 18) / 2;
            int startY = cardY + 25;
            List<ItemStack> stacks = getPreviewItems();
            ItemStack hoveredPreviewStack = ItemStack.EMPTY;

            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    int idx = r * cols + c;
                    int sx = startX + c * 18 + 1;
                    int sy = startY + r * 18 + 1;
                    boolean hovered = mouseX >= sx - 1 && mouseX < sx + 17 && mouseY >= sy - 1 && mouseY < sy + 17;
                    float hoverAnim = VitrifyAnimator.updateSlotHover("preview_slot_" + idx, hovered);
                    if (cfg.glassSlotsEnabled) {
                        FrostedGlassRenderer.drawGlassSlot(graphics, sx, sy, hoverAnim, idx == 3, alpha);
                    } else {
                        graphics.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0x88383838);
                    }

                    ItemStack stack = idx < stacks.size() ? stacks.get(idx) : ItemStack.EMPTY;
                    if (!stack.isEmpty()) {
                        graphics.fakeItem(stack, sx, sy);
                        graphics.itemDecorations(this.font, stack, sx, sy);
                        if (hovered) {
                            hoveredPreviewStack = stack;
                        }
                    }
                    if (hovered && cfg.glassSlotsEnabled) {
                        FrostedGlassRenderer.drawSlotFrontSheen(graphics, sx, sy, Math.max(0.4f, hoverAnim));
                    }
                }
            }

            int demoBtnW = Math.min(cardW - 20, 128);
            int demoBtnX = cardX + (cardW - demoBtnW) / 2;
            int demoBtnY = startY + rows * 18 + 10;
            if (demoBtnY + 18 < cardY + cardH - 4) {
                boolean btnHovered = mouseX >= demoBtnX && mouseX < demoBtnX + demoBtnW && mouseY >= demoBtnY && mouseY < demoBtnY + 18;
                this.previewBtnState.hover = VitrifyAnimator.updateSlotHover("preview_demo_btn", btnHovered);
                FrostedGlassRenderer.drawGlassButton(graphics, demoBtnX, demoBtnY, demoBtnW, 18, true, 1.0f, this.previewBtnState);
                graphics.centeredText(this.font, Component.translatable("vitrify.preview.sample_button"), demoBtnX + demoBtnW / 2, demoBtnY + 5, 0xFFF2F7FF);
            }

            if (!hoveredPreviewStack.isEmpty()) {
                graphics.setComponentTooltipForNextFrame(
                        this.font,
                        List.of(
                                hoveredPreviewStack.getHoverName(),
                                Component.translatable("vitrify.preview.tooltip_hint").withStyle(ChatFormatting.AQUA)
                        ),
                        mouseX,
                        mouseY
                );
            }
        } finally {
            VitrifyContrastAnalyzer.setPreviewLuminanceOverride(-1.0f);
        }
    }

    private void drawSimulatedBackdrop(GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
        switch (this.previewBackdrop) {
            case AUTO_SCENE -> FrostedGlassRenderer.drawFrostedPanel(graphics, x, y, w, h, 8.0f, 0.35f, 0.02f, false, false);
            case SNOW_DAYLIGHT -> {
                graphics.fillGradient(x, y, x + w, y + h / 2, 0xFFDCEEFF, 0xFFF6FBFF);
                graphics.fillGradient(x, y + h / 2, x + w, y + h, 0xFFF4F8FC, 0xFFDCE5F0);
                graphics.fill(x + w / 5, y + h / 3, x + w * 4 / 5, y + h / 3 + 10, 0xFFFFFFFF);
            }
            case DARK_CAVE -> {
                graphics.fillGradient(x, y, x + w, y + h, 0xFF07090E, 0xFF121620);
                graphics.fill(x + 14, y + 18, x + 46, y + 54, 0xFF1A202C);
                graphics.fill(x + w - 52, y + h - 64, x + w - 16, y + h - 28, 0xFF172330);
            }
            case LAVA_NETHER -> {
                graphics.fillGradient(x, y, x + w, y + h, 0xFF4A1208, 0xFFEB6A14);
                graphics.fill(x + 12, y + h / 2, x + w - 12, y + h / 2 + 18, 0xFFFFB22E);
            }
        }
        graphics.outline(x, y, w, h, ARGB.color(72, 210, 230, 255));
    }

    private Component toggleText(String key, boolean state) {
        return CommonComponents.optionStatus(Component.translatable(key), state);
    }

    private Component previewBackdropLabel() {
        return Component.translatable("vitrify.preview.backdrop", Component.translatable(this.previewBackdrop.key));
    }

    @Override
    public void onClose() {
        VitrifyContrastAnalyzer.setPreviewLuminanceOverride(-1.0f);
        VitrifyConfig.save();
        if (this.minecraft != null && this.minecraft.gui != null) {
            this.minecraft.gui.setScreen(this.parent);
        }
    }

    private record ScrollEntry(AbstractWidget widget, int baseY) {
    }

    private enum PreviewBackdrop {
        AUTO_SCENE("vitrify.preview.bg.auto", -1.0f),
        SNOW_DAYLIGHT("vitrify.preview.bg.snow", 0.94f),
        DARK_CAVE("vitrify.preview.bg.cave", 0.05f),
        LAVA_NETHER("vitrify.preview.bg.lava", 0.84f);

        final String key;
        final float luminanceOverride;

        PreviewBackdrop(String key, float luminanceOverride) {
            this.key = key;
            this.luminanceOverride = luminanceOverride;
        }

        PreviewBackdrop next() {
            PreviewBackdrop[] vals = values();
            return vals[(this.ordinal() + 1) % vals.length];
        }
    }

    private static final class FloatRangeSlider extends AbstractSliderButton {
        private final double min;
        private final double max;
        private final DoubleConsumer consumer;
        private final Function<Double, Component> formatter;

        FloatRangeSlider(
                int x,
                int width,
                double min,
                double max,
                DoubleSupplier getter,
                DoubleConsumer consumer,
                Function<Double, Component> formatter
        ) {
            super(x, 0, width, 20, CommonComponents.EMPTY, Mth.clamp((getter.getAsDouble() - min) / (max - min), 0.0, 1.0));
            this.min = min;
            this.max = max;
            this.consumer = consumer;
            this.formatter = formatter;
            this.updateMessage();
        }

        private double realValue() {
            return Mth.lerp(this.value, this.min, this.max);
        }

        @Override
        protected void updateMessage() {
            this.setMessage(this.formatter.apply(realValue()));
        }

        @Override
        protected void applyValue() {
            this.consumer.accept(realValue());
        }
    }
}
