package ashpapi.vitrify.neoforge;

import ashpapi.vitrify.platform.VitrifyPlatform;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Path;

public final class NeoForgePlatform implements VitrifyPlatform {
    @Override
    public Path getConfigDir() {
        return FMLPaths.CONFIGDIR.get();
    }
}
