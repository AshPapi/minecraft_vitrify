package ashpapi.vitrify.client.mixin;

import ashpapi.vitrify.client.anim.VitrifyAnimator;
import ashpapi.vitrify.client.render.FrostedGlassRenderer;
import ashpapi.vitrify.client.render.VitrifyScreenContext;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractButton.class)
public abstract class MixinAbstractButton extends AbstractWidget {
    protected MixinAbstractButton(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
    }

    @Inject(method = "extractDefaultSprite", at = @At("HEAD"), cancellable = true)
    private void vitrify$drawFrostedButtonSprite(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        if (!VitrifyScreenContext.shouldStyleWidgets()) {
            return;
        }
        FrostedGlassRenderer.drawGlassButton(
                graphics,
                this.getX(),
                this.getY(),
                this.getWidth(),
                this.getHeight(),
                this.active,
                this.alpha,
                VitrifyAnimator.updateWidget(this)
        );
        ci.cancel();
    }
}
