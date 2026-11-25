package ashpapi.vitrify.client.mixin;

import ashpapi.vitrify.client.anim.VitrifyAnimator;
import ashpapi.vitrify.client.config.VitrifyConfig;
import ashpapi.vitrify.client.render.FrostedGlassRenderer;
import ashpapi.vitrify.client.render.VitrifyBlurController;
import ashpapi.vitrify.client.render.VitrifyScreenContext;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatScreen.class)
public abstract class MixinChatScreen {
    @Inject(method = "extractBackground", at = @At("HEAD"))
    private void vitrify$requestChatBlurStratum(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (VitrifyScreenContext.isStylingActiveScreen()) {
            VitrifyBlurController.requestScreenStratumSeparation(graphics);
        }
    }

    @WrapOperation(
            method = "extractRenderState",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;fill(IIIII)V")
    )
    private void vitrify$drawFrostedChatBar(
            GuiGraphicsExtractor graphics,
            int x0,
            int y0,
            int x1,
            int y1,
            int color,
            Operation<Void> original
    ) {
        if (!VitrifyScreenContext.isStylingActiveScreen()) {
            original.call(graphics, x0, y0, x1, y1, color);
            return;
        }
        FrostedGlassRenderer.drawFrostedPanel(
                graphics,
                x0,
                y0 - 1,
                x1 - x0,
                y1 - y0 + 2,
                Math.min(5.0f, VitrifyConfig.get().cornerRadius * 0.75f),
                VitrifyAnimator.screenAlphaFactor(),
                0.22f,
                true,
                true
        );
    }
}
