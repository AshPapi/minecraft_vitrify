package ashpapi.vitrify.client.mixin;

import ashpapi.vitrify.client.anim.VitrifyAnimator;
import ashpapi.vitrify.client.config.VitrifyConfig;
import ashpapi.vitrify.client.render.FrostedGlassRenderer;
import ashpapi.vitrify.client.render.VitrifyBlurController;
import ashpapi.vitrify.client.render.VitrifyScreenContext;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public abstract class MixinHud {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    private boolean isHidden;

    @Shadow
    @Final
    private static Identifier HOTBAR_SPRITE;

    @Shadow
    @Final
    private static Identifier HOTBAR_SELECTION_SPRITE;

    @Shadow
    @Final
    private static Identifier HOTBAR_OFFHAND_LEFT_SPRITE;

    @Shadow
    @Final
    private static Identifier HOTBAR_OFFHAND_RIGHT_SPRITE;

    @Shadow
    private Player getCameraPlayer() {
        return null;
    }

    @Unique
    private boolean vitrify$isHotbarStyled() {
        VitrifyConfig cfg = VitrifyConfig.get();
        return cfg.enabled && cfg.styleHotbar;
    }

    @Unique
    private boolean vitrify$isHudOnlyFrame() {
        return this.minecraft != null && this.minecraft.gui != null && this.minecraft.gui.screen() == null;
    }

    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void vitrify$onHudFrameStart(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (this.vitrify$isHotbarStyled() && !this.isHidden) {
            VitrifyScreenContext.beginTopLevelFrame(this.minecraft, null);
        }
    }

    @Inject(method = "extractItemHotbar", at = @At("HEAD"))
    private void vitrify$prepareHotbarBlurStratum(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (this.vitrify$isHotbarStyled() && this.getCameraPlayer() != null && this.vitrify$isHudOnlyFrame()) {
            VitrifyBlurController.requestScreenStratumSeparation(graphics);
        }
    }

    @WrapOperation(
            method = "extractItemHotbar",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V")
    )
    private void vitrify$replaceHotbarSprites(
            GuiGraphicsExtractor graphics,
            RenderPipeline pipeline,
            Identifier sprite,
            int x,
            int y,
            int width,
            int height,
            Operation<Void> original
    ) {
        if (!this.vitrify$isHotbarStyled() || sprite == null) {
            original.call(graphics, pipeline, sprite, x, y, width, height);
            return;
        }
        boolean localizedBlur = this.vitrify$isHudOnlyFrame();
        if (sprite.equals(HOTBAR_SPRITE)) {
            FrostedGlassRenderer.drawHotbarPill(graphics, x, y, width, height, 9, localizedBlur);
            return;
        }
        if (sprite.equals(HOTBAR_SELECTION_SPRITE)) {
            Player player = this.getCameraPlayer();
            int selected = player != null ? player.getInventory().getSelectedSlot() : 0;
            FrostedGlassRenderer.drawHotbarSelection(graphics, graphics.guiWidth() / 2, graphics.guiHeight(), VitrifyAnimator.updateHotbarSelection(selected));
            return;
        }
        if (sprite.equals(HOTBAR_OFFHAND_LEFT_SPRITE) || sprite.equals(HOTBAR_OFFHAND_RIGHT_SPRITE)) {
            FrostedGlassRenderer.drawHotbarPill(graphics, sprite.equals(HOTBAR_OFFHAND_RIGHT_SPRITE) ? x + 7 : x, graphics.guiHeight() - 22, 22, 22, 1, localizedBlur);
            return;
        }
        original.call(graphics, pipeline, sprite, x, y, width, height);
    }
}
