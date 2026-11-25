package ashpapi.vitrify.client.config;

public enum VitrifyPreset {
    DARK_FROSTED("vitrify.preset.dark_frosted"),
    BALANCED("vitrify.preset.balanced"),
    HIGH_CONTRAST("vitrify.preset.high_contrast"),
    CUSTOM("vitrify.preset.custom");

    private final String translationKey;

    VitrifyPreset(String translationKey) {
        this.translationKey = translationKey;
    }

    public String translationKey() {
        return this.translationKey;
    }
}
