package ashpapi.vitrify.client;

import ashpapi.vitrify.client.config.VitrifyConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class VitrifyClient {
    public static final String MOD_ID = "vitrify";
    public static final Logger LOGGER = LoggerFactory.getLogger("Vitrify");
    private static boolean initialized;

    private VitrifyClient() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        VitrifyConfig.load();
    }
}
