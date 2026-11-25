package ashpapi.vitrify.client.mixin;

import ashpapi.vitrify.client.anim.VitrifyAnimator;
import ashpapi.vitrify.client.render.FrostedGlassRenderer;
import ashpapi.vitrify.client.render.VitrifyScreenContext;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.recipebook.RecipeButton;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(
        value = RecipeButton.class,
        targets = "net.minecraft.client.gui.screens.recipebook.OverlayRecipeComponent$OverlayRecipeButton"
)
public abstract class MixinRecipeButton extends AbstractWidget {
    protected MixinRecipeButton(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
    }

    @WrapOperation(
            method = "extractWidgetRenderState",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V")
    )
    private void vitrify$styleRecipeTileSprite(
            GuiGraphicsExtractor graphics,
            RenderPipeline pipeline,
            Identifier sprite,
            int x,
            int y,
            int width,
            int height,
            Operation<Void> original
    ) {
        if (!VitrifyScreenContext.shouldStyleWidgets() || sprite == null) {
            original.call(graphics, pipeline, sprite, x, y, width, height);
            return;
        }
        String path = sprite.getPath();
        boolean mainTile = (Object) this instanceof RecipeButton;
        int shrink = mainTile ? 3 : 2;
        FrostedGlassRenderer.drawRecipeTile(
                graphics,
                x + 1,
                y + 1,
                Math.max(18, width - shrink),
                Math.max(18, height - shrink),
                !path.contains("disabled") && !path.contains("uncraftable"),
                mainTile && path.contains("many"),
                VitrifyAnimator.updateWidget(this),
                VitrifyAnimator.screenAlphaFactor()
        );
    }
}
