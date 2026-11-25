package ashpapi.vitrify.client.mixin;

import ashpapi.vitrify.client.render.AnimatedPipState;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.pip.GuiEntityRenderState;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import org.joml.Matrix3x2fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GuiEntityRenderState.class)
public abstract class MixinGuiEntityRenderState implements PictureInPictureRenderState, AnimatedPipState {
    @Unique
    private Matrix3x2fc vitrify$customPose;

    @Unique
    private ScreenRectangle vitrify$customBounds;

    @Unique
    private int vitrify$premulColor = -1;

    @Override
    public void vitrify$setAnimation(Matrix3x2fc pose, int premultipliedColor, ScreenRectangle transformedBounds) {
        this.vitrify$customPose = pose;
        this.vitrify$premulColor = premultipliedColor;
        this.vitrify$customBounds = transformedBounds;
    }

    @Override
    public boolean vitrify$hasAnimation() {
        return this.vitrify$customPose != null;
    }

    @Override
    public int vitrify$premultipliedColor() {
        return this.vitrify$premulColor;
    }

    @Override
    public Matrix3x2fc pose() {
        return this.vitrify$customPose != null ? this.vitrify$customPose : PictureInPictureRenderState.IDENTITY_POSE;
    }

    @Inject(method = "bounds", at = @At("HEAD"), cancellable = true)
    private void vitrify$overrideAnimatedBounds(CallbackInfoReturnable<ScreenRectangle> cir) {
        if (this.vitrify$customPose != null) {
            cir.setReturnValue(this.vitrify$customBounds);
        }
    }
}
