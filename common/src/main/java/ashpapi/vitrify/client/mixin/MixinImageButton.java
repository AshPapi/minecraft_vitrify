package ashpapi.vitrify.client.mixin;

import ashpapi.vitrify.client.config.VitrifyConfig;
import ashpapi.vitrify.client.render.FrostedGlassRenderer;
import ashpapi.vitrify.client.render.VitrifyScreenContext;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ImageButton.class)
public abstract class MixinImageButton extends Button {
    @Shadow
    @Final
    protected WidgetSprites sprites;

    protected MixinImageButton(int x, int y, int width, int height, Component message, OnPress onPress, CreateNarration createNarration) {
        super(x, y, width, height, message, onPress, createNarration);
    }

    @Unique
    private boolean vitrify$isRecipeBookActionButton() {
        Identifier enabled = this.sprites != null ? this.sprites.enabled() : null;
        if (enabled == null || !"minecraft".equals(enabled.getNamespace())) {
            return false;
        }
        String path = enabled.getPath();
        return path.equals("recipe_book/button")
                || path.startsWith("recipe_book/page_forward")
                || path.startsWith("recipe_book/page_backward");
    }

    @Override
    public boolean shouldTakeFocusAfterInteraction() {
        if (VitrifyConfig.get().enabled && this.vitrify$isRecipeBookActionButton()) {
            return false;
        }
        return super.shouldTakeFocusAfterInteraction();
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        super.onClick(event, doubleClick);
        if (VitrifyConfig.get().enabled && this.vitrify$isRecipeBookActionButton() && this.isFocused()) {
            VitrifyScreenContext.clearWidgetFocus(this);
        }
    }

    @WrapOperation(
            method = "extractContents",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V")
    )
    private void vitrify$styleRecipeBookImageButtons(
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
