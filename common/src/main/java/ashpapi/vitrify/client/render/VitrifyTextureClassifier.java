package ashpapi.vitrify.client.render;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class VitrifyTextureClassifier {
    public enum RegionKind {
        OTHER,
        CONTAINER_PANEL,
        SLOT_TILE
    }

    private static final int MAX_CACHED_IMAGES = 256;
    private static final int MAX_CACHED_REGIONS = 2048;

    private static final String[] EXCLUDED_TEXTURE_PREFIXES = {
            "textures/atlas/", "textures/font/", "textures/item/", "textures/block/",
            "textures/entity/", "textures/models/", "textures/painting/", "textures/mob_effect/",
            "textures/particle/", "textures/environment/", "textures/map/", "textures/misc/",
            "textures/colormap/"
    };
    private static final String[] EXCLUDED_TEXTURE_TOKENS = {
            "/title/", "panorama", "crosshair", "hotbar", "icons.png", "widgets.png"
    };
    private static final String[] EXCLUDED_SPRITE_PREFIXES = {
            "tooltip/", "hud/", "boss_bar/", "popup/", "container/slot/"
    };
    private static final String[] EXCLUDED_SPRITE_TOKENS = {
            "toast", "disabled", "locked", "blocked", "cancel", "confirm", "progress",
            "arrow", "flame", "burn", "lit", "bubble", "fuel", "error", "scroller",
            "tab", "button", "checkbox", "slider", "text_field", "highlight"
    };
    private static final String[] CONTAINER_PATH_HINTS = {
            "container", "inventory", "chest", "backpack", "storage",
            "crate", "barrel", "shulker", "machine", "pouch", "drawer"
    };

    private static final Map<Identifier, SheetData> SHEET_CACHE = new ConcurrentHashMap<>();
    private static final Map<RegionKey, RegionKind> REGION_CACHE = new ConcurrentHashMap<>();
    private static ResourceManager lastResourceManager;

    private VitrifyTextureClassifier() {
    }

    private static boolean startsWithAny(String s, String[] prefixes) {
        for (String prefix : prefixes) {
            if (s.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsAny(String s, String[] tokens) {
        for (String token : tokens) {
            if (s.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isSlotDrawSize(int w, int h) {
        return w >= 16 && w <= 26 && h >= 16 && h <= 26;
    }

    private static boolean isPanelCandidateSize(int w, int h, boolean insideContainerScreen) {
        return (insideContainerScreen && ((w >= 96 && h >= 4) || (w >= 24 && h >= 28))) || (w >= 80 && h >= 48);
    }

    private static int quantizeUv(float uv) {
        return Math.round(Mth.clamp(uv, 0.0f, 1.0f) * 1024.0f);
    }

    public static RegionKind classifyBlit(
            Identifier texture,
            float u0,
            float u1,
            float v0,
            float v1,
            int drawW,
            int drawH,
            boolean insideContainerScreen
    ) {
        if (texture == null || drawW < 16 || drawH < 4) {
            return RegionKind.OTHER;
        }
        String path = texture.getPath().toLowerCase(Locale.ROOT);
        if (startsWithAny(path, EXCLUDED_TEXTURE_PREFIXES) || containsAny(path, EXCLUDED_TEXTURE_TOKENS)) {
            return RegionKind.OTHER;
        }
        if ("minecraft".equals(texture.getNamespace()) && path.startsWith("textures/gui/container/") && drawW >= 100) {
            return RegionKind.CONTAINER_PANEL;
        }
        if (!isSlotDrawSize(drawW, drawH) && !isPanelCandidateSize(drawW, drawH, insideContainerScreen)) {
            return RegionKind.OTHER;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getResourceManager() == null) {
            return RegionKind.OTHER;
        }
        ResourceManager rm = mc.getResourceManager();
        if (rm != lastResourceManager) {
            lastResourceManager = rm;
            SHEET_CACHE.clear();
            REGION_CACHE.clear();
        }

        int qU0 = quantizeUv(Math.min(u0, u1));
        int qU1 = quantizeUv(Math.max(u0, u1));
        int qV0 = quantizeUv(Math.min(v0, v1));
        int qV1 = quantizeUv(Math.max(v0, v1));
        RegionKey key = new RegionKey(texture, qU0, qU1, qV0, qV1, drawW, drawH, insideContainerScreen);
        RegionKind cached = REGION_CACHE.get(key);
        if (cached != null) {
            return cached;
        }

        RegionKind result = analyzePixels(
                loadSheet(rm, texture),
                path,
                qU0 / 1024.0f,
                qU1 / 1024.0f,
                qV0 / 1024.0f,
                qV1 / 1024.0f,
                drawW,
                drawH,
                insideContainerScreen
        );
        if (REGION_CACHE.size() >= MAX_CACHED_REGIONS) {
            REGION_CACHE.clear();
        }
        REGION_CACHE.put(key, result);
        return result;
    }

    public static RegionKind analyzeRawPixels(
            int[] pixels,
            int imgW,
            int imgH,
            String pathLower,
            float u0,
            float u1,
            float v0,
            float v1,
            int drawW,
            int drawH,
            boolean insideContainerScreen
    ) {
        if (pixels == null || imgW <= 0 || imgH <= 0) {
            return RegionKind.OTHER;
        }
        return analyzePixels(new SheetData(imgW, imgH, pixels), pathLower, u0, u1, v0, v1, drawW, drawH, insideContainerScreen);
    }

    private static RegionKind analyzePixels(
            SheetData sheet,
            String path,
            float u0,
            float u1,
            float v0,
            float v1,
            int drawW,
            int drawH,
            boolean insideContainerScreen
    ) {
        if (sheet == null || sheet.width <= 0 || sheet.height <= 0 || sheet.pixels.length == 0) {
            return RegionKind.OTHER;
        }
        int px0 = Mth.clamp((int) Math.floor(u0 * sheet.width), 0, sheet.width - 1);
        int px1 = Mth.clamp((int) Math.ceil(u1 * sheet.width), px0 + 1, sheet.width);
        int py0 = Mth.clamp((int) Math.floor(v0 * sheet.height), 0, sheet.height - 1);
        int py1 = Mth.clamp((int) Math.ceil(v1 * sheet.height), py0 + 1, sheet.height);
        int regW = px1 - px0;
        int regH = py1 - py0;
        if (regW < 4 || regH < 4) {
            return RegionKind.OTHER;
        }

        if (isSlotDrawSize(drawW, drawH) && regW >= 16 && regW <= 28 && regH >= 16 && regH <= 28
                && matchesSlotBevel(sheet, px0, py0, px1, py1)) {
            return RegionKind.SLOT_TILE;
        }
        if (!isPanelCandidateSize(drawW, drawH, insideContainerScreen)) {
            return RegionKind.OTHER;
        }

        int stepsX = Math.min(20, regW);
        int stepsY = Math.min(20, regH);
        int total = stepsX * stepsY;
        int opaque = 0;
        int neutralSurface = 0;
        float lumSum = 0.0f;
        float lumSqSum = 0.0f;

        for (int sy = 0; sy < stepsY; sy++) {
            int y = py0 + (sy * regH) / stepsY;
            for (int sx = 0; sx < stepsX; sx++) {
                int argb = sheet.pixel(px0 + (sx * regW) / stepsX, y);
                if (ARGB.alpha(argb) < 215) {
                    continue;
                }
                opaque++;
                float val = brightness(argb);
                float sat = saturation(argb);
                lumSum += val;
                lumSqSum += val * val;
                if (sat <= 0.25f && val >= 0.16f && val <= 0.99f) {
                    neutralSurface++;
                }
            }
        }

        if (total == 0 || opaque == 0) {
            return RegionKind.OTHER;
        }
        float opaqueRatio = opaque / (float) total;
        float neutralRatio = neutralSurface / (float) opaque;
        float meanLum = lumSum / opaque;
        float lumVar = Math.max(0.0f, (lumSqSum / opaque) - meanLum * meanLum);
        boolean containerPathHint = containsAny(path, CONTAINER_PATH_HINTS);

        if (insideContainerScreen && drawW >= 96 && drawH >= 4
                && opaqueRatio >= 0.72f && (neutralRatio >= 0.56f || (containerPathHint && lumVar <= 0.065f))) {
            return RegionKind.CONTAINER_PANEL;
        }
        if (drawW >= 80 && drawH >= 48
                && opaqueRatio >= 0.76f && (neutralRatio >= 0.62f || (containerPathHint && opaqueRatio >= 0.82f && lumVar <= 0.055f))) {
            return RegionKind.CONTAINER_PANEL;
        }
        if (insideContainerScreen && drawW >= 24 && drawH >= 28
                && opaqueRatio >= 0.80f && neutralRatio >= 0.70f && lumVar <= 0.055f
                && (hasPanelEdgeBevel(sheet, px0, py0, px1, py1) || containerPathHint)) {
            return RegionKind.CONTAINER_PANEL;
        }
        return RegionKind.OTHER;
    }

    private static boolean matchesSlotBevel(SheetData sheet, int px0, int py0, int px1, int py1) {
        int center = sheet.pixel((px0 + px1) / 2, (py0 + py1) / 2);
        if (ARGB.alpha(center) < 230) {
            return false;
        }
        float cVal = brightness(center);
        if (saturation(center) > 0.22f || cVal < 0.34f || cVal > 0.76f) {
            return false;
        }
        int innerSample = sheet.pixel(px0 + (px1 - px0) / 3, py0 + (py1 - py0) / 3);
        if (Math.abs(ARGB.red(innerSample) - ARGB.red(center)) > 18
                || Math.abs(ARGB.green(innerSample) - ARGB.green(center)) > 18) {
            return false;
        }
        float topLeftVal = Math.min(brightness(sheet.pixel(px0, py0)), brightness(sheet.pixel(Math.min(px1 - 1, px0 + 1), Math.min(py1 - 1, py0 + 1))));
        float bottomRightVal = Math.max(brightness(sheet.pixel(px1 - 1, py1 - 1)), brightness(sheet.pixel(Math.max(px0, px1 - 2), Math.max(py0, py1 - 2))));
        return topLeftVal <= cVal - 0.12f && bottomRightVal >= cVal + 0.16f;
    }

    private static boolean hasPanelEdgeBevel(SheetData sheet, int px0, int py0, int px1, int py1) {
        int midX = (px0 + px1) / 2;
        float centerB = brightness(sheet.pixel(midX, (py0 + py1) / 2));
        float topB = Math.max(brightness(sheet.pixel(midX, Math.min(py1 - 1, py0 + 1))), brightness(sheet.pixel(midX, Math.min(py1 - 1, py0 + 2))));
        float bottomB = Math.min(brightness(sheet.pixel(midX, Math.max(py0, py1 - 2))), brightness(sheet.pixel(midX, Math.max(py0, py1 - 3))));
        return topB >= centerB + 0.08f || bottomB <= centerB - 0.08f;
    }

    private static float brightness(int argb) {
        return Math.max(ARGB.red(argb), Math.max(ARGB.green(argb), ARGB.blue(argb))) / 255.0f;
    }

    private static float saturation(int argb) {
        int r = ARGB.red(argb);
        int g = ARGB.green(argb);
        int b = ARGB.blue(argb);
        int maxC = Math.max(r, Math.max(g, b));
        return maxC == 0 ? 0.0f : (maxC - Math.min(r, Math.min(g, b))) / (float) maxC;
    }

    private static SheetData loadSheet(ResourceManager rm, Identifier texture) {
        SheetData existing = SHEET_CACHE.get(texture);
        if (existing != null) {
            return existing;
        }
        if (SHEET_CACHE.size() >= MAX_CACHED_IMAGES) {
            SHEET_CACHE.clear();
        }
        SheetData loaded = SheetData.EMPTY;
        try {
            Optional<Resource> opt = rm.getResource(texture);
            if (opt.isPresent()) {
                try (InputStream in = opt.get().open(); NativeImage img = NativeImage.read(in)) {
                    int w = img.getWidth();
                    int h = img.getHeight();
                    if (w > 0 && h > 0 && w <= 1024 && h <= 1024) {
                        int[] copy = new int[w * h];
                        for (int y = 0; y < h; y++) {
                            int row = y * w;
                            for (int x = 0; x < w; x++) {
                                copy[row + x] = img.getPixel(x, y);
                            }
                        }
                        loaded = new SheetData(w, h, copy);
                    }
                }
            }
        } catch (Throwable ignored) {
            loaded = SheetData.EMPTY;
        }
        SHEET_CACHE.put(texture, loaded);
        return loaded;
    }

    public static RegionKind classifySprite(Identifier sprite, int width, int height, boolean insideContainerScreen) {
        boolean slotSize = isSlotDrawSize(width, height);
        boolean panelSize = insideContainerScreen && width >= 28 && height >= 24;
        if (sprite == null || (!slotSize && !panelSize)) {
            return RegionKind.OTHER;
        }
        String path = sprite.getPath().toLowerCase(Locale.ROOT);
        if (startsWithAny(path, EXCLUDED_SPRITE_PREFIXES) || containsAny(path, EXCLUDED_SPRITE_TOKENS)) {
            return RegionKind.OTHER;
        }
        if (slotSize && (path.equals("container/slot") || path.endsWith("/slot") || path.endsWith("_slot"))) {
            return RegionKind.SLOT_TILE;
        }
        if (panelSize && (path.startsWith("container/inventory/effect_background") || path.contains("panel") || path.contains("background"))) {
            return RegionKind.CONTAINER_PANEL;
        }
        return RegionKind.OTHER;
    }

    private record SheetData(int width, int height, int[] pixels) {
        static final SheetData EMPTY = new SheetData(0, 0, new int[0]);

        int pixel(int x, int y) {
            return this.pixels[y * this.width + x];
        }
    }

    private record RegionKey(
            Identifier texture,
            int u0,
            int u1,
            int v0,
            int v1,
            int drawW,
            int drawH,
            boolean inContainer
    ) {
    }
}
