package ashpapi.vitrify.client.mixin;

import ashpapi.vitrify.client.render.AnimatedPipState;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.state.gui.BlitRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import org.joml.Matrix3x2fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PictureInPictureRenderer.class)
public abstract class MixinPictureInPictureRenderer<T extends PictureInPictureRenderState> {
    @WrapOperation(
            method = "blitTexture",
            at = @At(value = "NEW", target = "(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/client/gui/render/TextureSetup;Lorg/joml/Matrix3x2fc;IIIIFFFFILnet/minecraft/client/gui/navigation/ScreenRectangle;Lnet/minecraft/client/gui/navigation/ScreenRectangle;)Lnet/minecraft/client/renderer/state/gui/BlitRenderState;")
    )
    private BlitRenderState vitrify$tintAnimatedPipBlit(
            RenderPipeline pipeline,
            TextureSetup textureSetup,
            Matrix3x2fc pose,
            int x0,
            int y0,
            int x1,
            int y1,
            float u0,
            float u1,
            float v0,
            float v1,
            int color,
            ScreenRectangle scissorArea,
            ScreenRectangle bounds,
            Operation<BlitRenderState> original,
            T pipState,
            GuiRenderState guiRenderState
    ) {
        int finalColor = (pipState instanceof AnimatedPipState anim && anim.vitrify$hasAnimation())
                ? anim.vitrify$premultipliedColor()
                : color;
        return original.call(pipeline, textureSetup, pose, x0, y0, x1, y1, u0, u1, v0, v1, finalColor, scissorArea, bounds);
    }
}
