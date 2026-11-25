package ashpapi.vitrify.client.render;

import ashpapi.vitrify.client.VitrifyClient;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.resources.Identifier;

import java.util.Optional;

public final class VitrifyPipelines {
    public static final VertexFormat VITRIFY_QUAD_FORMAT = VertexFormat.builder(0)
            .addAttribute(DefaultVertexFormat.POSITION_SEMANTIC_NAME, GpuFormat.RGB32_FLOAT)
            .addAttribute(DefaultVertexFormat.UV0_SEMANTIC_NAME, GpuFormat.RG32_FLOAT)
            .addAttribute(DefaultVertexFormat.UV3_SEMANTIC_NAME, GpuFormat.RG32_FLOAT)
            .addAttribute(DefaultVertexFormat.UV1_SEMANTIC_NAME, GpuFormat.RG16_SINT)
            .addAttribute(DefaultVertexFormat.UV2_SEMANTIC_NAME, GpuFormat.RG16_SINT)
            .addAttribute(DefaultVertexFormat.COLOR_SEMANTIC_NAME, GpuFormat.RGBA8_UNORM)
            .build();

    public static final RenderPipeline KAWASE_BLUR_PIPELINE = RenderPipeline.builder()
            .withLocation(id("pipeline/kawase_blur"))
            .withVertexShader(id("core/kawase_blur"))
            .withFragmentShader(id("core/kawase_blur"))
            .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
            .withBindGroupLayout(BindGroupLayouts.IN_SAMPLER)
            .withColorTargetState(new ColorTargetState(Optional.empty(), GpuFormat.RGBA8_UNORM, ColorTargetState.WRITE_ALL))
            .withDepthStencilState(Optional.empty())
            .withCull(false)
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .build();

    public static final RenderPipeline FROSTED_BLUR_PIPELINE = buildQuadPipeline("frosted_blur", true);
    public static final RenderPipeline FROSTED_SDF_PIPELINE = buildQuadPipeline("frosted_sdf", false);

    private VitrifyPipelines() {
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(VitrifyClient.MOD_ID, path);
    }

    private static RenderPipeline buildQuadPipeline(String name, boolean withSampler0) {
        RenderPipeline.Builder builder = RenderPipeline.builder()
                .withLocation(id("pipeline/" + name))
                .withVertexShader(id("core/" + name))
                .withFragmentShader(id("core/" + name))
                .withBindGroupLayout(BindGroupLayouts.PROJECTION)
                .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS);
        if (withSampler0) {
            builder.withBindGroupLayout(BindGroupLayouts.SAMPLER0);
        }
        return builder
                .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                .withDepthStencilState(Optional.empty())
                .withCull(false)
                .withVertexBinding(0, VITRIFY_QUAD_FORMAT)
                .withPrimitiveTopology(PrimitiveTopology.QUADS)
                .build();
    }

    public static CompiledRenderPipeline getCompiledKawaseOrNull() {
        try {
            CompiledRenderPipeline kawase = RenderSystem.getCompiledPipelineNullable(KAWASE_BLUR_PIPELINE);
            return kawase != null && RenderSystem.getCompiledPipelineNullable(FROSTED_BLUR_PIPELINE) != null ? kawase : null;
        } catch (Throwable t) {
            VitrifyClient.LOGGER.warn("Disabling localized blur pipeline, falling back to vanilla stratum blur", t);
            VitrifyBlurController.markCustomBlurFailed();
            return null;
        }
    }
}
