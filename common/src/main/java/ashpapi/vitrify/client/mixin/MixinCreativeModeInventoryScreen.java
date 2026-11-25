package ashpapi.vitrify.client.mixin;

import ashpapi.vitrify.client.anim.VitrifyAnimator;
import ashpapi.vitrify.client.config.VitrifyConfig;
import ashpapi.vitrify.client.render.CreativeContainerAccess;
import ashpapi.vitrify.client.render.FrostedGlassRenderer;
import ashpapi.vitrify.client.render.VitrifyScreenContext;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.platform.cursor.CursorType;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(CreativeModeInventoryScreen.class)
public abstract class MixinCreativeModeInventoryScreen extends AbstractContainerScreen<CreativeModeInventoryScreen.ItemPickerMenu> implements CreativeContainerAccess {
    @Shadow
    private static CreativeModeTab selectedTab;

    @Shadow
    private Slot destroyItemSlot;

    @Shadow
    private boolean scrolling;

    @Shadow
    private boolean canScroll() {
        return false;
    }

    @Shadow
    private int getTabX(CreativeModeTab tab) {
        return 0;
    }

    @Shadow
    private int getTabY(CreativeModeTab tab) {
        return 0;
    }

    @Shadow
    protected abstract boolean insideScrollbar(double mouseX, double mouseY);

    protected MixinCreativeModeInventoryScreen(CreativeModeInventoryScreen.ItemPickerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    public Slot vitrify$destroyItemSlot() {
        return this.destroyItemSlot;
    }

    @Unique
    private boolean vitrify$isTopTab(CreativeModeTab tab) {
        return this.getTabY(tab) < 0;
    }

    @Unique
    private int vitrify$tabGlassY(CreativeModeTab tab, int gh) {
        return this.vitrify$isTopTab(tab) ? (this.topPos - 4 - gh) : (this.topPos + this.imageHeight + 4);
    }

    @WrapOperation(
            method = "extractBackground",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blit(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIII)V")
    )
    private void vitrify$drawUnifiedCreativePanel(
            GuiGraphicsExtractor graphics,
            RenderPipeline pipeline,
            Identifier texture,
            int x,
            int y,
            float u,
            float v,
            int width,
            int height,
            int texWidth,
            int texHeight,
            Operation<Void> original
    ) {
        if (!VitrifyScreenContext.isSupportedContainer(this)) {
            original.call(graphics, pipeline, texture, x, y, u, v, width, height, texWidth, texHeight);
            return;
        }
        int px = this.leftPos - 4;
        int py = this.topPos - 4;
        int pw = this.imageWidth + 8;
        int ph = this.imageHeight + 8;
        float crRadius = Math.min(VitrifyConfig.get().cornerRadius, 5.0f);
        float alpha = VitrifyAnimator.screenAlphaFactor();
        if (selectedTab != null) {
            int selGx = this.leftPos + this.getTabX(selectedTab) + 1;
            if (selGx >= px && selGx + 24 <= px + pw) {
                int tabSide = this.vitrify$isTopTab(selectedTab) ? 1 : 2;
                FrostedGlassRenderer.drawFrostedPanelWithTab(
                        graphics, px, py, pw, ph, crRadius, alpha, 0.08f, true, true,
                        tabSide, (selGx + 12.0f) - (px + pw * 0.5f), 12, 24
                );
                return;
            }
        }
        FrostedGlassRenderer.drawFrostedPanel(graphics, px, py, pw, ph, crRadius, alpha, 0.08f, true, true);
    }

    @WrapOperation(
            method = "extractBackground",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V")
    )
    private void vitrify$styleCreativeScrollbar(
            GuiGraphicsExtractor graphics,
            RenderPipeline pipeline,
            Identifier sprite,
            int x,
            int y,
            int width,
            int height,
            Operation<Void> original,
            @Local(argsOnly = true, ordinal = 0) int mouseX,
            @Local(argsOnly = true, ordinal = 1) int mouseY
    ) {
        if (!VitrifyScreenContext.isSupportedContainer(this)) {
            original.call(graphics, pipeline, sprite, x, y, width, height);
            return;
        }
        FrostedGlassRenderer.drawCreativeScrollbar(
                graphics,
                this.leftPos + 174,
                this.topPos + 17,
                14,
                112,
                x,
                y,
                width,
                height,
                this.canScroll(),
                this.scrolling || this.insideScrollbar(mouseX, mouseY),
                VitrifyAnimator.screenAlphaFactor()
        );
    }

    @WrapOperation(
            method = "extractTabButton",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;requestCursor(Lcom/mojang/blaze3d/platform/cursor/CursorType;)V")
    )
    private void vitrify$filterTabCursor(
            GuiGraphicsExtractor graphics,
            CursorType cursorType,
            Operation<Void> original,
            @Local(argsOnly = true, ordinal = 1) int mouseY,
            @Local(argsOnly = true) CreativeModeTab tab
    ) {
        int gh = tab == selectedTab ? 24 : 22;
        int gy = this.vitrify$tabGlassY(tab, gh);
        if (!VitrifyScreenContext.isSupportedContainer(this) || (mouseY >= gy && mouseY < gy + gh)) {
            original.call(graphics, cursorType);
        }
    }

    @WrapOperation(
            method = "extractTabButton",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V")
    )
    private void vitrify$styleCreativeTabSprite(
            GuiGraphicsExtractor graphics,
            RenderPipeline pipeline,
            Identifier sprite,
            int x,
            int y,
            int width,
            int height,
            Operation<Void> original,
            @Local(argsOnly = true, ordinal = 0) int mouseX,
            @Local(argsOnly = true, ordinal = 1) int mouseY,
            @Local(argsOnly = true) CreativeModeTab tab
    ) {
        if (!VitrifyScreenContext.isSupportedContainer(this)) {
            original.call(graphics, pipeline, sprite, x, y, width, height);
            return;
        }
        if (tab == selectedTab) {
            return;
        }
        int gx = x + 1;
        int gy = this.vitrify$tabGlassY(tab, 22);
        boolean hovered = mouseX >= gx && mouseX < gx + 24 && mouseY >= gy && mouseY < gy + 22;
        FrostedGlassRenderer.drawTabGlass(
                graphics,
                gx,
                gy,
                24,
                22,
                this.vitrify$isTopTab(tab) ? 1 : 2,
                hovered ? 1.0f : 0.0f,
                VitrifyAnimator.screenAlphaFactor()
        );
    }

    @WrapOperation(
            method = "extractTabButton",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;item(Lnet/minecraft/world/item/ItemStack;II)V")
    )
    private void vitrify$centerCreativeTabItem(
            GuiGraphicsExtractor graphics,
            ItemStack stack,
            int itemX,
            int itemY,
            Operation<Void> original,
            @Local(argsOnly = true) CreativeModeTab tab
    ) {
        if (!VitrifyScreenContext.isSupportedContainer(this)) {
            original.call(graphics, stack, itemX, itemY);
            return;
        }
        int gh = tab == selectedTab ? 24 : 22;
        original.call(graphics, stack, itemX, this.vitrify$tabGlassY(tab, gh) + (gh - 16) / 2);
    }
}
