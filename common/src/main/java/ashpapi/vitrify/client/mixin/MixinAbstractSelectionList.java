package ashpapi.vitrify.client.mixin;

import ashpapi.vitrify.client.anim.VitrifyAnimator;
import ashpapi.vitrify.client.config.VitrifyConfig;
import ashpapi.vitrify.client.render.FrostedGlassRenderer;
import ashpapi.vitrify.client.render.VitrifyScreenContext;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractContainerWidget;
import net.minecraft.client.gui.components.AbstractScrollArea;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractSelectionList.class)
public abstract class MixinAbstractSelectionList extends AbstractContainerWidget {
    protected MixinAbstractSelectionList(int x, int y, int width, int height, Component message, AbstractScrollArea.ScrollbarSettings settings) {
        super(x, y, width, height, message, settings);
    }

    @Inject(method = "extractListBackground", at = @At("HEAD"), cancellable = true)
    private void vitrify$drawFrostedListBackground(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        if (!VitrifyScreenContext.shouldStyleWidgets()) {
            return;
        }
        int pad = Math.min(8, Math.max(0, this.getWidth() / 32));
        FrostedGlassRenderer.drawFrostedPanel(
                graphics,
                this.getX() + pad,
                this.getY(),
                Math.max(8, this.getWidth() - pad * 2),
                this.getHeight(),
                VitrifyConfig.get().cornerRadius + 1.0f,
                VitrifyAnimator.screenAlphaFactor(),
                0.05f,
                true,
                true
        );
        ci.cancel();
    }

    @Inject(method = "extractListSeparators", at = @At("HEAD"), cancellable = true)
    private void vitrify$suppressListSeparators(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        if (VitrifyScreenContext.shouldStyleWidgets()) {
            ci.cancel();
        }
    }

    @WrapOperation(
            method = "extractSelection",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;fill(IIIII)V", ordinal = 0)
    )
    private void vitrify$wrapSelectionOuter(GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1, int color, Operation<Void> original) {
        if (!VitrifyScreenContext.shouldStyleWidgets()) {
            original.call(graphics, x0, y0, x1, y1, color);
            return;
        }
        FrostedGlassRenderer.drawFrostedPanel(
                graphics,
                x0,
                y0,
                x1 - x0,
                y1 - y0,
                Math.min(5.0f, VitrifyConfig.get().cornerRadius * 0.7f),
                VitrifyAnimator.screenAlphaFactor() * 0.85f,
                0.85f,
                false,
                false
        );
    }

    @WrapOperation(
            method = "extractSelection",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;fill(IIIII)V", ordinal = 1)
    )
    private void vitrify$wrapSelectionInner(GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1, int color, Operation<Void> original) {
        if (!VitrifyScreenContext.shouldStyleWidgets()) {
            original.call(graphics, x0, y0, x1, y1, color);
        }
    }
}
