package ashpapi.vitrify.client.config;

import ashpapi.vitrify.client.VitrifyClient;
import ashpapi.vitrify.client.gui.VitrifyConfigScreen;
import ashpapi.vitrify.platform.VitrifyPlatform;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.util.Mth;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class VitrifyConfig {
    public static final int CURRENT_CONFIG_VERSION = 4;

    private static final List<String> DEFAULT_EXCLUDED_SCREENS = List.of(
            "net.minecraft.client.gui.screens.ReceivingLevelScreen",
            "net.minecraft.client.gui.screens.ProgressScreen",
            "net.minecraft.client.gui.screens.LevelLoadingScreen",
            "mezz.jei.",
            "dev.emi.",
            "me.shedaniel.rei.",
            "net.caffeinemc.",
            "dev.isxander.",
            "eu.midnightdust.",
            "dev.vitrail.",
            "com.cartographica_unrolled."
    );
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static VitrifyConfig instance = new VitrifyConfig();

    public int configVersion = CURRENT_CONFIG_VERSION;
    public boolean enabled = true;
    public boolean containersOnly = true;
    public boolean autoStyleModdedContainers = true;
    public boolean styleHotbar = true;
    public VitrifyPreset preset = VitrifyPreset.BALANCED;
    public boolean localBlurEnabled = true;
    public float panelOpacity;
    public float blurStrength;
    public float blurRadius;
    public float cornerRadius;
    public int tintColorRgb;
    public float tintDensity;
    public float noiseStrength;
    public float rimBrightness;
    public float shadowStrength;
    public boolean autoContrast;
    public float autoContrastThreshold;
    public boolean animationsEnabled = true;
    public float animationSpeed = 1.0f;
    public boolean glassSlotsEnabled = true;
    public boolean styleWidgets = true;
    public boolean styleTooltips = true;
    public boolean useWhitelist = false;
    public List<String> excludedScreens = new ArrayList<>(DEFAULT_EXCLUDED_SCREENS);
    public List<String> whitelistedScreens = new ArrayList<>();

    public VitrifyConfig() {
        applyPreset(VitrifyPreset.BALANCED);
    }

    public static VitrifyConfig get() {
        return instance;
    }

    public static Path configPath() {
        return VitrifyPlatform.INSTANCE.getConfigDir().resolve("vitrify.json");
    }

    public static void load() {
        Path path = configPath();
        if (!Files.exists(path)) {
            instance = new VitrifyConfig();
            save();
            return;
        }
        try {
            VitrifyConfig loaded = GSON.fromJson(Files.readString(path), VitrifyConfig.class);
            if (loaded != null) {
                if (loaded.configVersion < CURRENT_CONFIG_VERSION) {
                    loaded.containersOnly = true;
                    loaded.autoStyleModdedContainers = true;
                    loaded.styleHotbar = true;
                    loaded.applyPreset(VitrifyPreset.BALANCED);
                    instance = loaded;
                    save();
                    return;
                }
                loaded.sanitize();
                instance = loaded;
            }
        } catch (Exception e) {
            VitrifyClient.LOGGER.warn("Failed to read vitrify.json, keeping defaults", e);
            instance = new VitrifyConfig();
        }
    }

    public static void save() {
        instance.configVersion = CURRENT_CONFIG_VERSION;
        instance.sanitize();
        Path path = configPath();
        String json = GSON.toJson(instance);
        try {
            Files.createDirectories(path.getParent());
            Path temp = path.resolveSibling("vitrify.json.tmp");
            Files.writeString(temp, json);
            Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception fallbackEx) {
            try {
                Files.writeString(path, json);
            } catch (Exception e) {
                VitrifyClient.LOGGER.warn("Failed to save vitrify.json", e);
            }
        }
    }

    public void resetToDefaults() {
        this.enabled = true;
        this.containersOnly = true;
        this.autoStyleModdedContainers = true;
        this.styleHotbar = true;
        this.localBlurEnabled = true;
        this.animationsEnabled = true;
        this.animationSpeed = 1.0f;
        this.glassSlotsEnabled = true;
        this.styleWidgets = true;
        this.styleTooltips = true;
        this.useWhitelist = false;
        this.excludedScreens = new ArrayList<>(DEFAULT_EXCLUDED_SCREENS);
        applyPreset(VitrifyPreset.BALANCED);
    }

    public void applyPreset(VitrifyPreset target) {
        this.preset = target;
        switch (target) {
            case BALANCED -> setGlassParams(0.58f, 0.94f, 7.0f, 7.0f, 0x242528, 0.56f, 0.016f, 0.44f, 0.36f, true, 0.50f);
            case DARK_FROSTED -> setGlassParams(0.72f, 0.96f, 7.5f, 7.5f, 0x17181A, 0.70f, 0.016f, 0.42f, 0.44f, true, 0.48f);
            case HIGH_CONTRAST -> setGlassParams(0.80f, 0.96f, 7.5f, 6.5f, 0x1B1C1F, 0.78f, 0.012f, 0.58f, 0.48f, true, 0.42f);
            case CUSTOM -> {
            }
        }
    }

    private void setGlassParams(
            float opacity,
            float blurStr,
            float blurRad,
            float cornerRad,
            int tintRgb,
            float tintDens,
            float noiseStr,
            float rimBright,
            float shadowStr,
            boolean contrast,
            float contrastThresh
    ) {
        this.panelOpacity = opacity;
        this.blurStrength = blurStr;
        this.blurRadius = blurRad;
        this.cornerRadius = cornerRad;
        this.tintColorRgb = tintRgb;
        this.tintDensity = tintDens;
        this.noiseStrength = noiseStr;
        this.rimBrightness = rimBright;
        this.shadowStrength = shadowStr;
        this.autoContrast = contrast;
        this.autoContrastThreshold = contrastThresh;
    }

    public void markCustom() {
        this.preset = VitrifyPreset.CUSTOM;
    }

    public void sanitize() {
        if (this.preset == null) {
            this.preset = VitrifyPreset.BALANCED;
        }
        this.panelOpacity = Mth.clamp(this.panelOpacity, 0.05f, 0.90f);
        this.blurStrength = Mth.clamp(this.blurStrength, 0.0f, 1.0f);
        this.blurRadius = Mth.clamp(this.blurRadius, 1.0f, 16.0f);
        this.cornerRadius = Mth.clamp(this.cornerRadius, 0.0f, 18.0f);
        this.tintColorRgb = this.tintColorRgb & 0xFFFFFF;
        this.tintDensity = Mth.clamp(this.tintDensity, 0.05f, 0.90f);
        this.noiseStrength = Mth.clamp(this.noiseStrength, 0.0f, 0.08f);
        this.rimBrightness = Mth.clamp(this.rimBrightness, 0.0f, 0.85f);
        this.shadowStrength = Mth.clamp(this.shadowStrength, 0.0f, 0.85f);
        this.autoContrastThreshold = Mth.clamp(this.autoContrastThreshold, 0.20f, 0.85f);
        this.animationSpeed = Mth.clamp(this.animationSpeed, 0.25f, 2.5f);
        if (this.excludedScreens == null) {
            this.excludedScreens = new ArrayList<>();
        }
        if (this.whitelistedScreens == null) {
            this.whitelistedScreens = new ArrayList<>();
        }
    }

    public boolean isScreenStyled(Screen screen) {
        if (!this.enabled || screen == null) {
            return false;
        }
        if (screen instanceof VitrifyConfigScreen) {
            return true;
        }
        String fqcn = screen.getClass().getName();
        if (matchesAny(fqcn, this.excludedScreens)) {
            return false;
        }
        if (this.useWhitelist && !this.whitelistedScreens.isEmpty()) {
            return matchesAny(fqcn, this.whitelistedScreens);
        }
        if (this.containersOnly) {
            return screen instanceof AbstractContainerScreen<?> || (this.autoStyleModdedContainers && !fqcn.startsWith("net.minecraft."));
        }
        return true;
    }

    private static boolean matchesAny(String fqcn, List<String> rules) {
        String fqcnLower = fqcn.toLowerCase(Locale.ROOT);
        for (String raw : rules) {
            if (raw != null) {
                String token = raw.trim().toLowerCase(Locale.ROOT);
                if (!token.isEmpty() && fqcnLower.contains(token)) {
                    return true;
                }
            }
        }
        return false;
    }

    public String excludedScreensAsText() {
        return String.join(", ", this.excludedScreens);
    }

    public void setExcludedScreensFromText(String csv) {
        this.excludedScreens.clear();
        if (csv == null || csv.isBlank()) {
            return;
        }
        for (String part : csv.split("[,;\\n]")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                this.excludedScreens.add(trimmed);
            }
        }
    }
}
