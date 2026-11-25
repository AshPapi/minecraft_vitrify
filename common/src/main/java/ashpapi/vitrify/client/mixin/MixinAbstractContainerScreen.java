package ashpapi.vitrify.client.mixin;

import ashpapi.vitrify.client.anim.VitrifyAnimator;
import ashpapi.vitrify.client.config.VitrifyConfig;
import ashpapi.vitrify.client.render.ContainerBoundsAccess;
import ashpapi.vitrify.client.render.CreativeContainerAccess;
import ashpapi.vitrify.client.render.FrostedGlassRenderer;
import ashpapi.vitrify.client.render.VitrifyScreenContext;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(AbstractContainerScreen.class)
public abstract class MixinAbstractContainerScreen<T extends AbstractContainerMenu> extends Screen implements ContainerBoundsAccess {
    @Shadow
    @Final
    protected int imageWidth;

    @Shadow
    @Final
    protected int imageHeight;

    @Shadow
    protected int leftPos;

    @Shadow
    protected int topPos;

    @Shadow
    protected Slot hoveredSlot;

    @Shadow
    @Final
    protected T menu;

    @Shadow
    protected boolean isQuickCrafting;

    @Shadow
    @Final
    protected Set<Slot> quickCraftSlots;

    @Unique
    private int[] vitrify$cachedBounds;

    @Unique
    private int vitrify$cachedKeyHash;

    protected MixinAbstractContainerScreen(Component title) {
        super(title);
    }

    @Override
    public int[] vitrify$effectiveBounds() {
        int slotCount = (this.menu != null && this.menu.slots != null) ? this.menu.slots.size() : 0;
        int keyHash = (((this.leftPos * 31 + this.topPos) * 31 + this.imageWidth) * 31 + this.imageHeight) * 31 + slotCount;
        if (this.vitrify$cachedBounds != null && this.vitrify$cachedKeyHash == keyHash) {
            return this.vitrify$cachedBounds;
        }
        int minX = this.leftPos;
        int minY = this.topPos;
        int maxX = this.leftPos + this.imageWidth;
        int maxY = this.topPos + this.imageHeight;
        if (!VitrifyScreenContext.isVanillaContainer((AbstractContainerScreen<?>) (Object) this)
                && this.menu != null
                && this.menu.slots != null) {
            for (Slot slot : this.menu.slots) {
                if (!slot.isActive()) {
                    continue;
                }
                int sx = this.leftPos + slot.x;
                int sy = this.topPos + slot.y;
                if (sx >= this.leftPos - 64 && sx <= maxX + 64 && sy >= this.topPos - 64 && sy <= maxY + 64) {
                    minX = Math.min(minX, sx - 7);
                    minY = Math.min(minY, sy - 7);
                    maxX = Math.max(maxX, sx + 23);
                    maxY = Math.max(maxY, sy + 23);
                }
            }
        }
        this.vitrify$cachedBounds = new int[]{minX, minY, Math.max(0, maxX - minX), Math.max(0, maxY - minY)};
        this.vitrify$cachedKeyHash = keyHash;
        return this.vitrify$cachedBounds;
    }

    @Override
    public boolean vitrify$hasMenuSlotNear(int screenX, int screenY) {
        if (this.menu == null || this.menu.slots == null) {
            return false;
        }
        for (Slot slot : this.menu.slots) {
            if (slot.isActive()
                    && Math.abs(this.leftPos + slot.x - screenX) <= 2
                    && Math.abs(this.topPos + slot.y - screenY) <= 2) {
                return true;
            }
        }
        return false;
    }

    @Unique
    private boolean vitrify$areGlassSlotsActive() {
        return VitrifyConfig.get().glassSlotsEnabled
                && VitrifyScreenContext.isSupportedContainer((AbstractContainerScreen<?>) (Object) this);
    }

    @WrapOperation(
            method = "extractContents",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;extractLabels(Lnet/minecraft/client/gui/GuiGraphicsExtractor;II)V")
    )
    private void vitrify$wrapExtractLabels(AbstractContainerScreen<?> instance, GuiGraphicsExtractor graphics, int mouseX, int mouseY, Operation<Void> original) {
        boolean convert = VitrifyScreenContext.isSupportedContainer(instance);
        if (convert) {
            VitrifyScreenContext.pushConvertContainerLabels();
        }
        try {
            original.call(instance, graphics, mouseX, mouseY);
        } finally {
            if (convert) {
                VitrifyScreenContext.popConvertContainerLabels();
            }
        }
    }

    @Inject(method = "extractSlots", at = @At("HEAD"))
    private void vitrify$drawGlassSlotGrid(GuiGraphicsExtractor graphics, int mouseX, int mouseY, CallbackInfo ci) {
        if (!this.vitrify$areGlassSlotsActive()) {
            return;
        }
        this.vitrify$cachedBounds = null;
        float alpha = VitrifyAnimator.screenAlphaFactor();
        Slot destroySlot = (this instanceof CreativeContainerAccess creative) ? creative.vitrify$destroyItemSlot() : null;
        for (Slot slot : this.menu.slots) {
            if (!slot.isActive()) {
                continue;
            }
            float hoverAnim = VitrifyAnimator.updateSlotHover(slot, slot == this.hoveredSlot && slot.isHighlightable());
            if (destroySlot != null && slot == destroySlot) {
                FrostedGlassRenderer.drawDestroyItemSlot(graphics, slot.x, slot.y, hoverAnim, slot.hasItem(), alpha);
            } else {
                FrostedGlassRenderer.drawGlassSlot(graphics, slot.x, slot.y, hoverAnim, this.isQuickCrafting && this.quickCraftSlots.contains(slot), alpha);
            }
        }
    }

    @Inject(method = "extractSlotHighlightBack", at = @At("HEAD"), cancellable = true)
    private void vitrify$replaceSlotHighlightBack(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        if (this.vitrify$areGlassSlotsActive()) {
            ci.cancel();
        }
    }

    @Inject(method = "extractSlotHighlightFront", at = @At("HEAD"), cancellable = true)
    private void vitrify$replaceSlotHighlightFront(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        if (this.vitrify$areGlassSlotsActive()) {
            if (this.hoveredSlot != null && this.hoveredSlot.isHighlightable()) {
                float hoverAnim = VitrifyAnimator.updateSlotHover(this.hoveredSlot, true);
                FrostedGlassRenderer.drawSlotFrontSheen(graphics, this.hoveredSlot.x, this.hoveredSlot.y, Math.max(0.35f, hoverAnim));
            }
            ci.cancel();
        }
    }
}
