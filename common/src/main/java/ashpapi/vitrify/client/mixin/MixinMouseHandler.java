package ashpapi.vitrify.client.mixin;

import ashpapi.vitrify.client.anim.VitrifyAnimator;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.MouseHandler;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MouseHandler.class)
public abstract class MixinMouseHandler {
    @WrapOperation(
            method = "onScroll",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;setSelectedSlot(I)V")
    )
    private void vitrify$trackHotbarScroll(Inventory inventory, int slot, Operation<Void> original) {
        int prevSlot = inventory.getSelectedSlot();
        original.call(inventory, slot);
        VitrifyAnimator.onHotbarScroll(prevSlot, inventory.getSelectedSlot());
    }
}
