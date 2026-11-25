package ashpapi.vitrify.client.mixin;

import ashpapi.vitrify.client.render.FrostedGlassRenderer;
import ashpapi.vitrify.client.render.VitrifyScreenContext;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TooltipRenderUtil.class)
public abstract class MixinTooltipRenderUtil {
    @Inject(method = "extractTooltipBackground", at = @At("HEAD"), cancellable = true)
    private static void vitrify$drawFrostedTooltip(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int width,
            int height,
            Identifier tooltipStyle,
            CallbackInfo ci
    ) {
        if (!VitrifyScreenContext.shouldStyleTooltips()) {
            return;
        }
        FrostedGlassRenderer.drawGlassTooltip(graphics, x, y, width, height);
        ci.cancel();
    }
}
