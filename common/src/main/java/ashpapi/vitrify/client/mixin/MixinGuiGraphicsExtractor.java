package ashpapi.vitrify.client.mixin;

import ashpapi.vitrify.client.anim.VitrifyAnimator;
import ashpapi.vitrify.client.render.AnimatedPipState;
import ashpapi.vitrify.client.render.GuiGraphicsAccess;
import ashpapi.vitrify.client.render.VitrifyContrastAnalyzer;
import ashpapi.vitrify.client.render.VitrifyScreenContext;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayDeque;
import java.util.Deque;

@Mixin(GuiGraphicsExtractor.class)
public abstract class MixinGuiGraphicsExtractor implements GuiGraphicsAccess {
    @Unique
    private static final int VITRIFY_VANILLA_CONTAINER_LABEL_COLOR = -12566464;

    @Shadow
    @Final
    private GuiRenderState guiRenderState;

    @Shadow
    @Final
    private Matrix3x2fStack pose;

    @Shadow
    public abstract int guiWidth();

    @Shadow
    public abstract int guiHeight();

    @Unique
    private final Deque<ScreenRectangle> vitrify$scissorMirror = new ArrayDeque<>();

    @Override
    public GuiRenderState vitrify$guiRenderState() {
        return this.guiRenderState;
    }

    @Override
    public ScreenRectangle vitrify$currentScissor() {
        return this.vitrify$scissorMirror.peekLast();
    }

    @Inject(method = "enableScissor", at = @At("TAIL"))
    private void vitrify$onEnableScissor(int x0, int y0, int x1, int y1, CallbackInfo ci) {
        ScreenRectangle local = new ScreenRectangle(x0, y0, x1 - x0, y1 - y0).transformAxisAligned(this.pose);
        ScreenRectangle parent = this.vitrify$scissorMirror.peekLast();
        if (parent == null) {
            parent = new ScreenRectangle(0, 0, this.guiWidth(), this.guiHeight());
        }
        ScreenRectangle clipped = local.intersection(parent);
        this.vitrify$scissorMirror.addLast(clipped != null ? clipped : ScreenRectangle.empty());
    }

    @Inject(method = "disableScissor", at = @At("TAIL"))
    private void vitrify$onDisableScissor(CallbackInfo ci) {
        if (!this.vitrify$scissorMirror.isEmpty()) {
            this.vitrify$scissorMirror.removeLast();
        }
    }

    @Inject(
            method = "innerBlit(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIIFFFFI)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void vitrify$interceptInnerBlit(
            RenderPipeline pipeline,
            Identifier texture,
            int x0,
            int x1,
            int y0,
            int y1,
            float u0,
            float u1,
            float v0,
            float v1,
            int color,
            CallbackInfo ci
    ) {
        if (VitrifyScreenContext.handleInterceptedBlit((GuiGraphicsExtractor) (Object) this, texture, x0, x1, y0, y1, u0, u1, v0, v1)) {
            ci.cancel();
        }
    }

    @Inject(
            method = "blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIII)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void vitrify$interceptBlitSprite(
            RenderPipeline pipeline,
            Identifier sprite,
            int x,
            int y,
            int width,
            int height,
            int color,
            CallbackInfo ci
    ) {
        if (VitrifyScreenContext.handleInterceptedSprite((GuiGraphicsExtractor) (Object) this, sprite, x, y, width, height)) {
            ci.cancel();
        }
    }

    @Inject(
            method = "blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIIIIIII)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void vitrify$interceptSubBlitSprite(
            RenderPipeline pipeline,
            Identifier sprite,
            int spriteWidth,
            int spriteHeight,
            int u,
            int v,
            int x,
            int y,
            int width,
            int height,
            int color,
            CallbackInfo ci
    ) {
        if (VitrifyScreenContext.handleInterceptedSubSprite(sprite, width, height)) {
            ci.cancel();
        }
    }

    @ModifyVariable(
            method = {
                    "text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V",
                    "text(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)V",
                    "text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)V"
            },
            at = @At("HEAD"),
            ordinal = 2,
            argsOnly = true
    )
    private int vitrify$adjustLabelColor(int color) {
        if (VitrifyScreenContext.isConvertingContainerLabels() && color == VITRIFY_VANILLA_CONTAINER_LABEL_COLOR) {
            return VitrifyContrastAnalyzer.computeContainerLabelColor(1.0f);
        }
        return color;
    }

    @ModifyVariable(
            method = {
                    "text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V",
                    "text(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)V",
                    "text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)V"
            },
            at = @At("HEAD"),
            ordinal = 0,
            argsOnly = true
    )
    private boolean vitrify$adjustLabelShadow(boolean shadow) {
        return VitrifyScreenContext.isConvertingContainerLabels() || shadow;
    }

    @WrapOperation(
            method = "entity",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/state/gui/GuiRenderState;addPicturesInPictureState(Lnet/minecraft/client/renderer/state/gui/pip/PictureInPictureRenderState;)V")
    )
    private void vitrify$animateEntityInGui(
            GuiRenderState guiState,
            PictureInPictureRenderState pipState,
            Operation<Void> original
    ) {
        if (VitrifyScreenContext.isStylingActiveScreen() && pipState instanceof AnimatedPipState anim) {
            float alpha = VitrifyAnimator.screenAlphaFactor();
            boolean hasPoseTransform = Math.abs(this.pose.m00() - 1.0f) > 0.0001f
                    || Math.abs(this.pose.m11() - 1.0f) > 0.0001f
                    || Math.abs(this.pose.m01()) > 0.0001f
                    || Math.abs(this.pose.m10()) > 0.0001f
                    || Math.abs(this.pose.m20()) > 0.001f
                    || Math.abs(this.pose.m21()) > 0.001f;
            if (hasPoseTransform || alpha < 0.999f) {
                Matrix3x2f copiedPose = new Matrix3x2f(this.pose);
                int ch = Math.round(Mth.clamp(alpha, 0.0f, 1.0f) * 255.0f);
                ScreenRectangle transformedBounds = PictureInPictureRenderState.getBounds(
                        pipState.x0(),
                        pipState.y0(),
                        pipState.x1(),
                        pipState.y1(),
                        copiedPose,
                        pipState.scissorArea()
                );
                anim.vitrify$setAnimation(copiedPose, ARGB.color(ch, ch, ch, ch), transformedBounds);
            }
        }
        original.call(guiState, pipState);
    }
}
