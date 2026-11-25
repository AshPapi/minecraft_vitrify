package ashpapi.vitrify.client.mixin;

import ashpapi.vitrify.client.anim.VitrifyAnimator;
import ashpapi.vitrify.client.config.VitrifyConfig;
import ashpapi.vitrify.client.render.FrostedGlassRenderer;
import ashpapi.vitrify.client.render.VitrifyScreenContext;
import ashpapi.vitrify.client.render.WidgetBoundsAccess;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeBookTabButton;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(RecipeBookComponent.class)
public abstract class MixinRecipeBookComponent {
    @Shadow
    @Final
    private List<RecipeBookTabButton> tabButtons;

    @Shadow
    private RecipeBookTabButton selectedTab;

    @Inject(method = "setVisible", at = @At("TAIL"))
    private void vitrify$clearToggleFocusOnHide(boolean visible, CallbackInfo ci) {
        if (visible || !VitrifyConfig.get().enabled || VitrifyAnimator.isKeyboardFocusActive()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Screen screen = (mc != null && mc.gui != null) ? mc.gui.screen() : null;
        if (screen != null && screen.getFocused() instanceof ImageButton) {
            screen.setFocused(null);
        }
    }

    @WrapOperation(
            method = "extractRenderState",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blit(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIII)V")
    )
    private void vitrify$wrapRecipeBookBackgroundBlit(
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
            Operation<Void> original,
            @Local(argsOnly = true, ordinal = 0) int mouseX,
            @Local(argsOnly = true, ordinal = 1) int mouseY
    ) {
        if (!VitrifyScreenContext.isStylingActiveScreen()) {
            original.call(graphics, pipeline, texture, x, y, u, v, width, height, texWidth, texHeight);
            return;
        }
        float alpha = VitrifyAnimator.screenAlphaFactor();
        int px = x - 2;
        int py = y - 2;
        int pw = width + 4;
        int ph = height + 4;
        float radius = Math.min(VitrifyConfig.get().cornerRadius, 5.0f);
        boolean drewWithTab = false;

        if (VitrifyScreenContext.shouldStyleWidgets()) {
            if (this.tabButtons != null) {
                for (RecipeBookTabButton tab : this.tabButtons) {
                    if (tab == null || !tab.visible || tab == this.selectedTab) {
                        continue;
                    }
                    WidgetBoundsAccess b = (WidgetBoundsAccess) tab;
                    int gx = b.vitrify$visualX();
                    int gy = b.vitrify$visualY();
                    int gw = b.vitrify$visualWidth();
                    int gh = b.vitrify$visualHeight();
                    boolean hovered = mouseX >= gx && mouseX < gx + gw && mouseY >= gy && mouseY < gy + gh;
                    VitrifyAnimator.WidgetState state = VitrifyAnimator.updateWidget(tab, hovered, tab.isFocused());
                    FrostedGlassRenderer.drawTabGlass(graphics, gx, gy, gw, gh, 3, Math.max(state.hover, state.focus * 0.85f), alpha);
                }
            }
            if (this.selectedTab != null && this.selectedTab.visible) {
                WidgetBoundsAccess b = (WidgetBoundsAccess) this.selectedTab;
                int selGy = b.vitrify$visualY();
                int selGh = b.vitrify$visualHeight();
                FrostedGlassRenderer.drawFrostedPanelWithTab(
                        graphics, px, py, pw, ph, radius, alpha, 0.06f, true, true,
                        3, (selGy + selGh * 0.5f) - (py + ph * 0.5f), selGh / 2, b.vitrify$visualWidth()
                );
                drewWithTab = true;
            }
        }
        if (!drewWithTab) {
            FrostedGlassRenderer.drawFrostedPanel(graphics, px, py, pw, ph, radius, alpha, 0.06f, true, true);
        }
        FrostedGlassRenderer.drawRecipeBookMagnifier(graphics, x + 10, y + 14, 12, alpha);
    }
}
