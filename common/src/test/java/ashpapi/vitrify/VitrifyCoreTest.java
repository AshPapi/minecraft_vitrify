package ashpapi.vitrify;

import ashpapi.vitrify.client.config.VitrifyConfig;
import ashpapi.vitrify.client.config.VitrifyPreset;
import ashpapi.vitrify.client.render.VitrifyContrastAnalyzer;
import ashpapi.vitrify.client.render.VitrifyPipelines;
import ashpapi.vitrify.client.render.VitrifyTextureClassifier;
import net.minecraft.util.ARGB;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.shaderc.Shaderc;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class VitrifyCoreTest {
    @Test
    void presetsApplyAndSanitizeBounds() {
        VitrifyConfig cfg = VitrifyConfig.get();
        cfg.applyPreset(VitrifyPreset.BALANCED);
        assertEquals(VitrifyPreset.BALANCED, cfg.preset);
        assertTrue(cfg.containersOnly);
        assertTrue(cfg.styleHotbar);
        assertTrue(cfg.panelOpacity >= 0.50f && cfg.panelOpacity <= 0.70f);

        cfg.applyPreset(VitrifyPreset.HIGH_CONTRAST);
        assertEquals(VitrifyPreset.HIGH_CONTRAST, cfg.preset);
        assertTrue(cfg.rimBrightness >= 0.50f);

        cfg.panelOpacity = 9.0f;
        cfg.blurRadius = -5.0f;
        cfg.cornerRadius = 99.0f;
        cfg.animationSpeed = 0.01f;
        cfg.sanitize();

        assertEquals(0.90f, cfg.panelOpacity, 1e-4f);
        assertEquals(1.0f, cfg.blurRadius, 1e-4f);
        assertEquals(18.0f, cfg.cornerRadius, 1e-4f);
        assertEquals(0.25f, cfg.animationSpeed, 1e-4f);
    }

    @Test
    void autoContrastIncreasesDarkeningOnSnowVsCave() {
        VitrifyConfig cfg = VitrifyConfig.get();
        cfg.applyPreset(VitrifyPreset.BALANCED);
        cfg.autoContrast = true;

        VitrifyContrastAnalyzer.setPreviewLuminanceOverride(0.04f);
        int caveTint = VitrifyContrastAnalyzer.computeAdaptivePanelTint(1.0f);
        int caveOuter = VitrifyContrastAnalyzer.computeOuterContourColor(1.0f);

        VitrifyContrastAnalyzer.setPreviewLuminanceOverride(0.95f);
        int snowTint = VitrifyContrastAnalyzer.computeAdaptivePanelTint(1.0f);
        int snowOuter = VitrifyContrastAnalyzer.computeOuterContourColor(1.0f);

        VitrifyContrastAnalyzer.setPreviewLuminanceOverride(-1.0f);

        assertTrue(ARGB.alpha(snowTint) > ARGB.alpha(caveTint));
        assertTrue(ARGB.alpha(snowTint) >= Math.round(0.65f * 255.0f));
        assertTrue(ARGB.red(snowTint) <= ARGB.red(caveTint));
        assertTrue(ARGB.alpha(snowOuter) > ARGB.alpha(caveOuter));
    }

    @Test
    void excludedScreenParsingHandlesCsvAndWhitespace() {
        VitrifyConfig cfg = VitrifyConfig.get();
        cfg.setExcludedScreensFromText(" foo.BarScreen , baz.Qux ; net.example.Custom ");
        assertEquals(3, cfg.excludedScreens.size());
        assertEquals("foo.BarScreen", cfg.excludedScreens.get(0));
        assertEquals("baz.Qux", cfg.excludedScreens.get(1));
        assertEquals("net.example.Custom", cfg.excludedScreens.get(2));
    }

    @Test
    void textureClassifierDetectsModdedContainerPanelsAndSlotsWhileSkippingIcons() {
        int w = 176;
        int h = 166;
        int[] panel = new int[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int c = 0xFFC6C6C6;
                if (x < 2 || y < 2) {
                    c = 0xFFFFFFFF;
                } else if (x >= w - 2 || y >= h - 2) {
                    c = 0xFF555555;
                }
                panel[y * w + x] = c;
            }
        }
        assertEquals(
                VitrifyTextureClassifier.RegionKind.CONTAINER_PANEL,
                VitrifyTextureClassifier.analyzeRawPixels(panel, w, h, "textures/gui/custom_crate.png", 0f, 1f, 0f, 1f, w, h, true)
        );
        assertEquals(
                VitrifyTextureClassifier.RegionKind.CONTAINER_PANEL,
                VitrifyTextureClassifier.analyzeRawPixels(panel, w, h, "textures/gui/custom_crate.png", 0f, 1f, 0f, 1f, w, h, false)
        );

        int[] slot = new int[18 * 18];
        for (int y = 0; y < 18; y++) {
            for (int x = 0; x < 18; x++) {
                int c = 0xFF8B8B8B;
                if (x == 0 || y == 0) {
                    c = 0xFF373737;
                } else if (x == 17 || y == 17) {
                    c = 0xFFFFFFFF;
                }
                slot[y * 18 + x] = c;
            }
        }
        assertEquals(
                VitrifyTextureClassifier.RegionKind.SLOT_TILE,
                VitrifyTextureClassifier.analyzeRawPixels(slot, 18, 18, "textures/gui/modular.png", 0f, 1f, 0f, 1f, 18, 18, true)
        );

        int[] flame = new int[32 * 32];
        for (int y = 0; y < 32; y++) {
            for (int x = 0; x < 32; x++) {
                flame[y * 32 + x] = (x + y) % 2 == 0 ? 0x00000000 : 0xFFFF6600;
            }
        }
        assertEquals(
                VitrifyTextureClassifier.RegionKind.OTHER,
                VitrifyTextureClassifier.analyzeRawPixels(flame, 32, 32, "textures/gui/furnace_extra.png", 0f, 1f, 0f, 1f, 32, 32, true)
        );
    }

    @Test
    void allCustomShadersCompileToVulkanSpirv() throws Exception {
        assertEquals(6, VitrifyPipelines.VITRIFY_QUAD_FORMAT.getElements().size());
        String[] shaders = {
                "kawase_blur.vsh",
                "kawase_blur.fsh",
                "frosted_blur.vsh",
                "frosted_blur.fsh",
                "frosted_sdf.vsh",
                "frosted_sdf.fsh"
        };
        long compiler = Shaderc.shaderc_compiler_initialize();
        long options = Shaderc.shaderc_compile_options_initialize();
        try {
            Shaderc.shaderc_compile_options_set_target_env(options, Shaderc.shaderc_target_env_vulkan, Shaderc.shaderc_env_version_vulkan_1_2);
            Shaderc.shaderc_compile_options_set_auto_bind_uniforms(options, true);
            Shaderc.shaderc_compile_options_set_preserve_bindings(options, false);

            for (String file : shaders) {
                String resPath = "/assets/vitrify/shaders/core/" + file;
                try (InputStream in = VitrifyCoreTest.class.getResourceAsStream(resPath)) {
                    assertNotNull(in, "Missing shader resource: " + resPath);
                    String src = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                    int kind = file.endsWith(".vsh") ? Shaderc.shaderc_glsl_vertex_shader : Shaderc.shaderc_glsl_fragment_shader;
                    long res = Shaderc.shaderc_compile_into_spv(compiler, src, kind, file, "main", options);
                    try {
                        int status = Shaderc.shaderc_result_get_compilation_status(res);
                        String err = Shaderc.shaderc_result_get_error_message(res);
                        assertEquals(Shaderc.shaderc_compilation_status_success, status, "Shader failed (" + file + "): " + err);
                        ByteBuffer bytes = Shaderc.shaderc_result_get_bytes(res);
                        assertNotNull(bytes);
                        assertTrue(bytes.remaining() > 32);
                    } finally {
                        Shaderc.shaderc_result_release(res);
                    }
                }
            }
        } finally {
            Shaderc.shaderc_compile_options_release(options);
            Shaderc.shaderc_compiler_release(compiler);
        }
    }
}
