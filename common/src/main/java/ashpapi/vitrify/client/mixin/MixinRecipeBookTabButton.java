package ashpapi.vitrify.client.mixin;

import ashpapi.vitrify.client.render.VitrifyScreenContext;
import ashpapi.vitrify.client.render.WidgetBoundsAccess;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeBookTabButton;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(RecipeBookTabButton.class)
public abstract class MixinRecipeBookTabButton extends ImageButton implements WidgetBoundsAccess {
    @Shadow
    @Final
    private RecipeBookComponent.TabInfo tabInfo;

    @Shadow
    private boolean selected;

    protected MixinRecipeBookTabButton(int x, int y, int width, int height, WidgetSprites sprites, OnPress onPress) {
        super(x, y, width, height, sprites, onPress);
    }

    @Override
    public boolean vitrify$hasCustomBounds() {
        return VitrifyScreenContext.isWidgetStylingEnabledNow();
    }

    @Override
    public int vitrify$visualX() {
        return this.selected ? (this.getX() - 2) : this.getX();
    }

    @Override
    public int vitrify$visualY() {
        return this.getY() + 1;
    }

    @Override
    public int vitrify$visualWidth() {
        return this.selected ? 30 : 28;
    }

    @Override
    public int vitrify$visualHeight() {
        return 24;
    }

    @WrapOperation(
            method = "extractContents",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V")
    )
    private void vitrify$styleRecipeBookTabSprite(
            GuiGraphicsExtractor graphics,
            RenderPipeline pipeline,
            Identifier sprite,
            int x,
            int y,
            int width,
            int height,
            Operation<Void> original
    ) {
        if (!VitrifyScreenContext.shouldStyleWidgets()) {
            original.call(graphics, pipeline, sprite, x, y, width, height);
        }
    }

    @WrapOperation(
            method = "extractIcon",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;fakeItem(Lnet/minecraft/world/item/ItemStack;II)V")
    )
    private void vitrify$alignRecipeTabIcons(
            GuiGraphicsExtractor graphics,
            ItemStack stack,
            int ix,
            int iy,
            Operation<Void> original
    ) {
        if (!VitrifyScreenContext.shouldStyleWidgets()) {
            original.call(graphics, stack, ix, iy);
            return;
        }
        int gx = this.vitrify$visualX();
        int gw = this.vitrify$visualWidth();
        int centeredX = (this.tabInfo != null && this.tabInfo.secondaryIcon().isPresent())
                ? gx + (gw - 26) / 2 + ((ix - gx) >= 8 ? 10 : 0)
                : gx + (gw - 16) / 2;
        original.call(graphics, stack, centeredX, this.vitrify$visualY() + (this.vitrify$visualHeight() - 16) / 2);
    }
}
