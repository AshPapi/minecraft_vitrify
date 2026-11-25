package ashpapi.vitrify.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.util.Mth;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;

public record FrostedSdfQuadRenderState(
        RenderPipeline pipeline,
        TextureSetup textureSetup,
        Matrix3x2fc pose,
        float x0,
        float y0,
        float x1,
        float y1,
        float lx0,
        float ly0,
        float lx1,
        float ly1,
        float halfW,
        float halfH,
        float cornerRadius,
        float shapeParam,
        int uv2x,
        int uv2y,
        int color,
        ScreenRectangle scissorArea,
        ScreenRectangle bounds
) implements GuiElementRenderState {

    public static FrostedSdfQuadRenderState createSdf(
            Matrix3x2fc srcPose,
            float x,
            float y,
            float width,
            float height,
            float pad,
            float cornerRadius,
            float shapeParam,
            int modeFlags,
            int extraFlags,
            int color,
            ScreenRectangle scissor,
            int tabSide,
            int tabProtrude
    ) {
        return createQuad(
                VitrifyPipelines.FROSTED_SDF_PIPELINE,
                TextureSetup.noTexture(),
                srcPose,
                x,
                y,
                width,
                height,
                pad,
                cornerRadius,
                shapeParam,
                modeFlags,
                extraFlags,
                color,
                scissor,
                tabSide,
                tabProtrude
        );
    }

    public static FrostedSdfQuadRenderState createBlur(
            TextureSetup blurSetup,
            Matrix3x2fc srcPose,
            float x,
            float y,
            float width,
            float height,
            float cornerRadius,
            float tabCenter,
            int flagsPrefix,
            int color,
            ScreenRectangle scissor,
            int tabSide,
            int tabProtrude
    ) {
        return createQuad(
                VitrifyPipelines.FROSTED_BLUR_PIPELINE,
                blurSetup,
                srcPose,
                x,
                y,
                width,
                height,
                0.0f,
                cornerRadius,
                tabCenter,
                flagsPrefix,
                0,
                color,
                scissor,
                tabSide,
                tabProtrude
        );
    }

    private static FrostedSdfQuadRenderState createQuad(
            RenderPipeline pipeline,
            TextureSetup textureSetup,
            Matrix3x2fc srcPose,
            float x,
            float y,
            float width,
            float height,
            float pad,
            float cornerRadius,
            float shapeParam,
            int uv2x,
            int uv2y,
            int color,
            ScreenRectangle scissor,
            int tabSide,
            int tabProtrude
    ) {
        Matrix3x2f copiedPose = new Matrix3x2f(srcPose);
        float extL = tabSide == 3 ? tabProtrude : 0.0f;
        float extT = tabSide == 1 ? tabProtrude : 0.0f;
        float extB = tabSide == 2 ? tabProtrude : 0.0f;
        float halfW = width * 0.5f;
        float halfH = height * 0.5f;
        float x0 = x - pad - extL;
        float y0 = y - pad - extT;
        float x1 = x + width + pad;
        float y1 = y + height + pad + extB;
        int ix0 = Mth.floor(x0);
        int iy0 = Mth.floor(y0);
        int ix1 = Mth.ceil(x1);
        int iy1 = Mth.ceil(y1);
        ScreenRectangle rawBounds = new ScreenRectangle(ix0, iy0, Math.max(0, ix1 - ix0), Math.max(0, iy1 - iy0))
                .transformMaxBounds(copiedPose);
        ScreenRectangle clipped = scissor != null ? scissor.intersection(rawBounds) : rawBounds;
        return new FrostedSdfQuadRenderState(
                pipeline,
                textureSetup,
                copiedPose,
                x0,
                y0,
                x1,
                y1,
                -halfW - pad - extL,
                -halfH - pad - extT,
                halfW + pad,
                halfH + pad + extB,
                halfW,
                halfH,
                cornerRadius,
                shapeParam,
                uv2x,
                uv2y,
                color,
                scissor,
                clipped
        );
    }

    @Override
    public void buildVertices(VertexConsumer consumer) {
        int rFixed = Mth.clamp(Math.round(this.cornerRadius * 64.0f), 0, 32767);
        int pFixed = Mth.clamp(Math.round(this.shapeParam * 64.0f), -32768, 32767);
        emitVertex(consumer, this.x0, this.y0, this.lx0, this.ly0, rFixed, pFixed);
        emitVertex(consumer, this.x0, this.y1, this.lx0, this.ly1, rFixed, pFixed);
        emitVertex(consumer, this.x1, this.y1, this.lx1, this.ly1, rFixed, pFixed);
        emitVertex(consumer, this.x1, this.y0, this.lx1, this.ly0, rFixed, pFixed);
    }

    private void emitVertex(VertexConsumer consumer, float vx, float vy, float u, float v, int rFixed, int pFixed) {
        consumer.addVertexWith2DPose(this.pose, vx, vy)
                .setUv(u, v)
                .setUv3(this.halfW, this.halfH)
                .setUv1(rFixed, pFixed)
                .setUv2(this.uv2x, this.uv2y)
                .setColor(this.color);
    }
}
