package ashpapi.vitrify.fabric;

import ashpapi.vitrify.platform.VitrifyPlatform;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

public final class FabricPlatform implements VitrifyPlatform {
    @Override
    public Path getConfigDir() {
        return FabricLoader.getInstance().getConfigDir();
    }
}
