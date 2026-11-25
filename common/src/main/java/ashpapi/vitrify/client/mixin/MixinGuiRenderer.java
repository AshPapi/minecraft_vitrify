package ashpapi.vitrify.client.mixin;

import ashpapi.vitrify.client.render.VitrifyBlurController;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiRenderer.class)
public abstract class MixinGuiRenderer {
    @WrapOperation(
            method = "draw",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;processBlurEffect()V")
    )
    private void vitrify$redirectGuiBlur(GameRenderer gameRenderer, Operation<Void> original) {
        if (!VitrifyBlurController.handleGuiBlurPhase(gameRenderer.mainRenderTarget())) {
            original.call(gameRenderer);
        }
    }

    @Inject(method = "close", at = @At("TAIL"))
    private void vitrify$onClose(CallbackInfo ci) {
        VitrifyBlurController.destroyTargets();
    }
}
