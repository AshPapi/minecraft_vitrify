package ashpapi.vitrify.fabric;

import ashpapi.vitrify.client.VitrifyClient;
import net.fabricmc.api.ClientModInitializer;

public final class VitrifyFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        VitrifyClient.init();
    }
}
