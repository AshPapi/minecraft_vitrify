package ashpapi.vitrify.client.mixin;

import ashpapi.vitrify.client.anim.VitrifyAnimator;
import ashpapi.vitrify.client.config.VitrifyConfig;
import ashpapi.vitrify.client.render.VitrifyScreenContext;
import ashpapi.vitrify.client.render.WidgetBoundsAccess;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractWidget.class)
public abstract class MixinAbstractWidget {
    @Inject(method = "areCoordinatesInRectangle", at = @At("HEAD"), cancellable = true)
    private void vitrify$useVisualBoundsHitbox(double x, double y, CallbackInfoReturnable<Boolean> cir) {
        if (this instanceof WidgetBoundsAccess bounds && bounds.vitrify$hasCustomBounds()) {
            int bx = bounds.vitrify$visualX();
            int by = bounds.vitrify$visualY();
            cir.setReturnValue(x >= bx && y >= by && x < bx + bounds.vitrify$visualWidth() && y < by + bounds.vitrify$visualHeight());
        }
    }

    @Inject(
            method = "extractRenderState",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/AbstractWidget;extractWidgetRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V")
    )
    private void vitrify$tickWidgetAnimation(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (VitrifyScreenContext.shouldStyleWidgets()) {
            VitrifyAnimator.updateWidget((AbstractWidget) (Object) this);
        }
    }

    @Unique
    private void vitrify$notifyPress(boolean down) {
        VitrifyConfig cfg = VitrifyConfig.get();
        if (cfg.enabled && cfg.styleWidgets) {
            VitrifyAnimator.notifyWidgetPress((AbstractWidget) (Object) this, down);
        }
    }

    @Inject(method = "onClick", at = @At("HEAD"))
    private void vitrify$onWidgetClick(MouseButtonEvent event, boolean doubleClick, CallbackInfo ci) {
        this.vitrify$notifyPress(true);
    }

    @Inject(method = "onRelease", at = @At("HEAD"))
    private void vitrify$onWidgetRelease(MouseButtonEvent event, CallbackInfo ci) {
        this.vitrify$notifyPress(false);
    }
}
