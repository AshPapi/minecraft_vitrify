package ashpapi.vitrify.client.render;

import ashpapi.vitrify.client.VitrifyClient;
import ashpapi.vitrify.client.config.VitrifyConfig;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.device.GpuDevice;
import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTexture;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.Optional;
import java.util.OptionalDouble;

public final class VitrifyBlurController {
    private static final Matrix4f IDENTITY_MAT = new Matrix4f();

    private static GpuTexture pingTexture;
    private static GpuTextureView pingView;
    private static GpuTexture pongTexture;
    private static GpuTextureView pongView;
    private static int allocatedWidth;
    private static int allocatedHeight;

    private static boolean customBlurFailed;
    private static boolean blurStratumRequestedThisFrame;
    private static boolean localizedRegionQueuedThisFrame;
    private static boolean blurTargetReadyThisFrame;

    private VitrifyBlurController() {
    }

    public static void markCustomBlurFailed() {
        customBlurFailed = true;
    }

    public static boolean shouldUseLocalizedBlur() {
        VitrifyConfig cfg = VitrifyConfig.get();
        return cfg.enabled && cfg.localBlurEnabled && cfg.blurStrength > 0.01f && !customBlurFailed;
    }

    public static void onExtractionFrameStart() {
        blurStratumRequestedThisFrame = false;
        localizedRegionQueuedThisFrame = false;
        blurTargetReadyThisFrame = ensureBlurTargetsForCurrentWindow();
    }

    public static void requestScreenStratumSeparation(GuiGraphicsExtractor graphics) {
        if (blurStratumRequestedThisFrame) {
            return;
        }
        VitrifyConfig cfg = VitrifyConfig.get();
        if (!cfg.enabled || cfg.blurStrength <= 0.01f) {
            return;
        }
        try {
            graphics.blurBeforeThisStratum();
        } catch (IllegalStateException alreadyBlurred) {
            blurStratumRequestedThisFrame = true;
        }
        blurStratumRequestedThisFrame = true;
    }

    public static boolean canQueueLocalizedBlurQuad() {
        return blurStratumRequestedThisFrame
                && blurTargetReadyThisFrame
                && pongView != null
                && shouldUseLocalizedBlur()
                && VitrifyPipelines.getCompiledKawaseOrNull() != null;
    }

    public static TextureSetup markAndGetBlurTextureSetup() {
        localizedRegionQueuedThisFrame = true;
        return TextureSetup.singleTexture(pongView, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
    }

    public static boolean handleGuiBlurPhase(RenderTarget mainTarget) {
        VitrifyConfig cfg = VitrifyConfig.get();
        if (!cfg.enabled || !blurStratumRequestedThisFrame) {
            return false;
        }
        if (localizedRegionQueuedThisFrame && canQueueLocalizedBlurQuad()) {
            return executeKawaseCapture(mainTarget);
        }
        return cfg.blurStrength <= 0.01f || shouldUseLocalizedBlur();
    }

    private static boolean ensureBlurTargetsForCurrentWindow() {
        if (!shouldUseLocalizedBlur()) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getWindow() == null) {
            return false;
        }
        int targetW = Math.max(2, mc.getWindow().getWidth() / 2);
        int targetH = Math.max(2, mc.getWindow().getHeight() / 2);
        if (pingTexture != null && pongTexture != null && !pingTexture.isClosed() && !pongTexture.isClosed()
                && allocatedWidth == targetW && allocatedHeight == targetH) {
            return true;
        }
        destroyTargets();
        try {
            GpuDevice device = RenderSystem.tryGetDevice();
            if (device == null) {
                return false;
            }
            int usage = GpuTexture.USAGE_RENDER_ATTACHMENT | GpuTexture.USAGE_TEXTURE_BINDING;
            pingTexture = device.createTexture("Vitrify Blur Ping", usage, GpuFormat.RGBA8_UNORM, targetW, targetH, 1, 1);
            pingView = device.createTextureView(pingTexture);
            pongTexture = device.createTexture("Vitrify Blur Pong", usage, GpuFormat.RGBA8_UNORM, targetW, targetH, 1, 1);
            pongView = device.createTextureView(pongTexture);
            allocatedWidth = targetW;
            allocatedHeight = targetH;
            return true;
        } catch (Throwable t) {
            VitrifyClient.LOGGER.warn("Failed to allocate localized blur textures, switching to fallback", t);
            destroyTargets();
            markCustomBlurFailed();
            return false;
        }
    }

    private static boolean executeKawaseCapture(RenderTarget mainTarget) {
        if (mainTarget == null || mainTarget.getColorTextureView() == null || pingView == null || pongView == null) {
            return false;
        }
        CompiledRenderPipeline compiled = VitrifyPipelines.getCompiledKawaseOrNull();
        if (compiled == null) {
            return false;
        }
        try {
            VitrifyConfig cfg = VitrifyConfig.get();
            float radius = cfg.blurRadius * (0.35f + 0.65f * cfg.blurStrength);
            GpuSampler linearClamp = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);

            float invMainW = 1.0f / Math.max(1, mainTarget.width);
            float invMainH = 1.0f / Math.max(1, mainTarget.height);
            float invHalfW = 1.0f / Math.max(1, allocatedWidth);
            float invHalfH = 1.0f / Math.max(1, allocatedHeight);

            runSingleKawasePass(compiled, mainTarget.getColorTextureView(), pingView, linearClamp, invMainW, invMainH, Math.max(1.0f, radius * 0.45f));
            runSingleKawasePass(compiled, pingView, pongView, linearClamp, invHalfW, invHalfH, Math.max(1.0f, radius * 0.65f));

            if (radius >= 5.5f) {
                runSingleKawasePass(compiled, pongView, pingView, linearClamp, invHalfW, invHalfH, Math.max(1.2f, radius * 0.85f));
                runSingleKawasePass(compiled, pingView, pongView, linearClamp, invHalfW, invHalfH, Mth.clamp(radius * 0.95f, 1.0f, 18.0f));
            }
            return true;
        } catch (Throwable t) {
            VitrifyClient.LOGGER.warn("Localized blur pass failed, falling back to vanilla blur", t);
            markCustomBlurFailed();
            return false;
        }
    }

    private static void runSingleKawasePass(
            CompiledRenderPipeline pipeline,
            GpuTextureView srcView,
            GpuTextureView dstView,
            GpuSampler sampler,
            float texelW,
            float texelH,
            float spread
    ) {
        GpuBufferSlice ubo = RenderSystem.getDynamicUniforms().writeTransform(
                IDENTITY_MAT,
                new Vector4f(texelW, texelH, spread, 1.0f)
        );
        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        try (RenderPass pass = encoder.createRenderPass(() -> "Vitrify Kawase Blur", dstView, Optional.empty(), null, OptionalDouble.empty())) {
            pass.setPipeline(pipeline);
            pass.setUniform("DynamicTransforms", ubo);
            pass.setUniform("InSampler", srcView, sampler);
            pass.draw(3, 1, 0, 0);
        }
    }

    public static void destroyTargets() {
        if (pingView != null && !pingView.isClosed()) {
            pingView.close();
        }
        pingView = null;
        if (pingTexture != null && !pingTexture.isClosed()) {
            pingTexture.close();
        }
        pingTexture = null;
        if (pongView != null && !pongView.isClosed()) {
            pongView.close();
        }
        pongView = null;
        if (pongTexture != null && !pongTexture.isClosed()) {
            pongTexture.close();
        }
        pongTexture = null;
        allocatedWidth = 0;
        allocatedHeight = 0;
    }
}
