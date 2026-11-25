package ashpapi.vitrify.client.mixin;

import ashpapi.vitrify.client.render.FrostedGlassRenderer;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(CycleButton.class)
public abstract class MixinCycleButton<T> extends AbstractButton {
    protected MixinCycleButton(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
    }

    @WrapOperation(
            method = "extractContents",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V")
    )
    private void vitrify$styleRecipeFilterCycleButton(
            GuiGraphicsExtractor graphics,
            RenderPipeline pipeline,
            Identifier sprite,
            int x,
            int y,
            int width,
            int height,
            Operation<Void> original
    ) {
        if (!FrostedGlassRenderer.replaceRecipeBookButtonSprite(this, this.alpha, graphics, sprite, x, y, width, height)) {
            original.call(graphics, pipeline, sprite, x, y, width, height);
        }
    }
}
