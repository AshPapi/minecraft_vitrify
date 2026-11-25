package ashpapi.vitrify.client.mixin;

import ashpapi.vitrify.client.render.FrostedGlassRenderer;
import ashpapi.vitrify.client.render.VitrifyScreenContext;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractScrollArea;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractScrollArea.class)
public abstract class MixinAbstractScrollArea extends AbstractWidget {
    @Shadow
    protected abstract boolean scrollable();

    @Shadow
    public abstract int scrollbarWidth();

    @Shadow
    protected abstract int scrollerHeight();

    @Shadow
    protected abstract int scrollBarX();

    @Shadow
    public abstract int scrollBarY();

    @Shadow
    protected abstract boolean isOverScrollbar(double mouseX, double mouseY);

    protected MixinAbstractScrollArea(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
    }

    @Inject(method = "extractScrollbar", at = @At("HEAD"), cancellable = true)
    private void vitrify$drawFrostedScrollbar(GuiGraphicsExtractor graphics, int mouseX, int mouseY, CallbackInfo ci) {
        if (!VitrifyScreenContext.shouldStyleWidgets()) {
            return;
        }
        if (this.scrollable()) {
            FrostedGlassRenderer.drawGlassScrollbar(
                    graphics,
                    this.scrollBarX(),
                    this.getY() + 2,
                    this.scrollbarWidth(),
                    Math.max(8, this.getHeight() - 4),
                    this.scrollBarY(),
                    this.scrollerHeight(),
                    this.isOverScrollbar(mouseX, mouseY)
            );
        }
        ci.cancel();
    }
}
