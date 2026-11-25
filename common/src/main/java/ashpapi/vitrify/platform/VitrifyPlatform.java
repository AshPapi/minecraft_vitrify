package ashpapi.vitrify.platform;

import java.nio.file.Path;
import java.util.ServiceLoader;

public interface VitrifyPlatform {
    VitrifyPlatform INSTANCE = ServiceLoader.load(VitrifyPlatform.class).findFirst()
            .orElseThrow(() -> new IllegalStateException("No VitrifyPlatform implementation found"));

    Path getConfigDir();
}
