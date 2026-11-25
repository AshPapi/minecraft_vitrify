package ashpapi.vitrify.client.mixin;

import ashpapi.vitrify.client.anim.VitrifyAnimator;
import ashpapi.vitrify.client.render.FrostedGlassRenderer;
import ashpapi.vitrify.client.render.VitrifyScreenContext;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EditBox.class)
public abstract class MixinEditBox extends AbstractWidget {
    @Shadow
    private int textX;

    @Shadow
    private int textY;

    @Shadow
    public abstract boolean isBordered();

    @Shadow
    public abstract boolean isCentered();

    @Shadow
    public abstract boolean isVisible();

    @Shadow
    private void updateTextPosition() {
    }

    @Unique
    private boolean vitrify$posAdjusted;

    protected MixinEditBox(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
    }

    @Unique
    private void vitrify$applyTextOffsets(boolean bordered) {
        if (!this.isCentered() && this.width >= 24) {
            this.textX = this.getX() + (bordered ? 5 : 3);
        }
        if (!bordered) {
            this.textY = this.getY() - 1;
        } else if (this.height <= 12) {
            this.textY = this.getY() + (this.height - 8) / 2 - 1;
        }
    }

    @Unique
    private void vitrify$syncTextPosition(boolean bordered) {
        if (!this.vitrify$posAdjusted) {
            this.vitrify$posAdjusted = true;
            this.updateTextPosition();
        } else {
            this.vitrify$applyTextOffsets(bordered);
        }
    }

    @Inject(method = "getInnerWidth", at = @At("RETURN"), cancellable = true)
    private void vitrify$adjustInnerWidth(CallbackInfoReturnable<Integer> cir) {
        if (this.width < 24) {
            return;
        }
        if (!this.isBordered() && VitrifyScreenContext.isCreativeScreenActiveNow()) {
            cir.setReturnValue(this.width - 6);
        } else if (this.isBordered() && VitrifyScreenContext.isWidgetStylingEnabledNow()) {
            cir.setReturnValue(this.width - 15);
        }
    }

    @Inject(method = "updateTextPosition", at = @At("TAIL"))
    private void vitrify$adjustTextPosition(CallbackInfo ci) {
        if (!this.isBordered() && VitrifyScreenContext.isCreativeScreenActiveNow()) {
            this.vitrify$applyTextOffsets(false);
        } else if (this.isBordered() && VitrifyScreenContext.isWidgetStylingEnabledNow()) {
            this.vitrify$applyTextOffsets(true);
        }
    }

    @Inject(method = "extractWidgetRenderState", at = @At("HEAD"))
    private void vitrify$drawUnborderedCreativeSearchGlass(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (!this.isVisible()) {
            return;
        }
        if (!this.isBordered() && VitrifyScreenContext.isCreativeScreenActive()) {
            this.vitrify$syncTextPosition(false);
            FrostedGlassRenderer.drawGlassEditBox(
                    graphics,
                    this.getX() - 2,
                    this.getY() - 3,
                    this.getWidth() + 10,
                    this.getHeight() + 4,
                    this.active,
                    this.isFocused(),
                    VitrifyAnimator.updateWidget(this)
            );
        } else if (this.isBordered() && VitrifyScreenContext.shouldStyleWidgets()) {
            this.vitrify$syncTextPosition(true);
        } else if (this.vitrify$posAdjusted) {
            this.vitrify$posAdjusted = false;
            this.updateTextPosition();
        }
    }

    @WrapOperation(
            method = "extractWidgetRenderState",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V")
    )
    private void vitrify$wrapEditBoxBackground(
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
            return;
        }
        int gy = height <= 12 ? (y - 1) : y;
        int gh = height <= 12 ? (height + 1) : height;
        FrostedGlassRenderer.drawGlassEditBox(graphics, x, gy, width, gh, this.active, this.isFocused(), VitrifyAnimator.updateWidget(this));
    }
}
