package ashpapi.vitrify.client.mixin;

import ashpapi.vitrify.client.anim.VitrifyAnimator;
import ashpapi.vitrify.client.render.FrostedGlassRenderer;
import ashpapi.vitrify.client.render.VitrifyBlurController;
import ashpapi.vitrify.client.render.VitrifyScreenContext;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.ARGB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class MixinScreen {
    @Shadow
    public int width;

    @Shadow
    public int height;

    @Shadow
    protected Minecraft minecraft;

    @WrapMethod(method = "extractRenderStateWithTooltipAndSubtitles")
    private void vitrify$wrapFullScreenExtract(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, Operation<Void> original) {
        Screen self = (Screen) (Object) this;
        VitrifyScreenContext.beginTopLevelFrame(this.minecraft, self);
        VitrifyScreenContext.enterScreenPass(self);
        try {
            original.call(graphics, mouseX, mouseY, partialTick);
        } finally {
            VitrifyScreenContext.exitScreenPass();
        }
    }

    @WrapOperation(
            method = "extractRenderStateWithTooltipAndSubtitles",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;extractBackground(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V")
    )
    private void vitrify$wrapBackgroundRenderState(Screen instance, GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, Operation<Void> original) {
        VitrifyScreenContext.enterTopLevelBackgroundPass();
        try {
            original.call(instance, graphics, mouseX, mouseY, partialTick);
        } finally {
            VitrifyScreenContext.exitTopLevelBackgroundPass(graphics);
        }
    }

    @WrapOperation(
            method = "extractRenderStateWithTooltipAndSubtitles",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V")
    )
    private void vitrify$wrapContentRenderState(Screen instance, GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, Operation<Void> original) {
        if (!VitrifyScreenContext.isStylingActiveScreen()) {
            original.call(instance, graphics, mouseX, mouseY, partialTick);
            return;
        }
        boolean pushed = VitrifyScreenContext.pushContentTransformIfNeeded(instance, graphics);
        try {
            VitrifyScreenContext.drawAutomaticScreenCardIfNeeded(instance, graphics);
            original.call(instance, graphics, mouseX, mouseY, partialTick);
            boolean anyFocused = false;
            if (VitrifyAnimator.isKeyboardFocusActive()) {
                for (GuiEventListener child : instance.children()) {
                    if (child instanceof AbstractWidget widget && !(widget instanceof EditBox) && widget.visible && widget.isFocused()) {
                        anyFocused = true;
                        break;
                    }
                }
            }
            VitrifyAnimator.decayFocusIfNone(anyFocused);
            FrostedGlassRenderer.drawSmoothFocusRing(graphics);
        } finally {
            if (pushed) {
                VitrifyScreenContext.popContentTransform(graphics);
            }
        }
    }

    @Inject(method = "extractBackground", at = @At("TAIL"))
    private void vitrify$afterScreenExtractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        VitrifyScreenContext.onAfterBaseScreenBackground((Screen) (Object) this, graphics);
    }

    @Inject(method = "extractPanorama", at = @At("TAIL"))
    private void vitrify$afterExtractPanorama(GuiGraphicsExtractor graphics, float partialTick, CallbackInfo ci) {
        if (VitrifyScreenContext.isFullChromeStyledScreen()) {
            graphics.nextStratum();
            VitrifyBlurController.requestScreenStratumSeparation(graphics);
        }
    }

    @Inject(method = "extractBlurredBackground", at = @At("HEAD"), cancellable = true)
    private void vitrify$onExtractBlurredBackground(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        if (!VitrifyScreenContext.isStylingActiveScreen()) {
            return;
        }
        VitrifyBlurController.requestScreenStratumSeparation(graphics);
        if (VitrifyScreenContext.isFullChromeStyledScreen()) {
            ci.cancel();
        }
    }

    @Inject(method = "extractMenuBackground(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIII)V", at = @At("HEAD"), cancellable = true)
    private void vitrify$onExtractMenuBackground(GuiGraphicsExtractor graphics, int x, int y, int w, int h, CallbackInfo ci) {
        if (!VitrifyScreenContext.isFullChromeStyledScreen()) {
            return;
        }
        float alphaFactor = VitrifyAnimator.screenAlphaFactor();
        boolean fullWorldVisible = this.minecraft != null && this.minecraft.level != null && VitrifyBlurController.shouldUseLocalizedBlur();
        int dimColor = ARGB.color(Math.round((fullWorldVisible ? 0.04f : 0.16f) * alphaFactor * 255.0f), 20, 21, 23);
        graphics.fill(x, y, x + w, y + h, dimColor);
        ci.cancel();
    }

    @Inject(method = "extractTransparentBackground", at = @At("HEAD"), cancellable = true)
    private void vitrify$onExtractTransparentBackground(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        if (!VitrifyScreenContext.isStylingActiveScreen()) {
            return;
        }
        VitrifyBlurController.requestScreenStratumSeparation(graphics);
        if (!VitrifyScreenContext.isFullChromeStyledScreen()) {
            return;
        }
        float alphaFactor = VitrifyAnimator.screenAlphaFactor();
        boolean localized = VitrifyBlurController.shouldUseLocalizedBlur();
        int topAlpha = Math.round((localized ? 0.03f : 0.14f) * alphaFactor * 255.0f);
        int bottomAlpha = Math.round((localized ? 0.06f : 0.20f) * alphaFactor * 255.0f);
        graphics.fillGradient(0, 0, this.width, this.height, ARGB.color(topAlpha, 22, 23, 25), ARGB.color(bottomAlpha, 18, 19, 21));
        ci.cancel();
    }
}
