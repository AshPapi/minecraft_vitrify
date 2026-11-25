package ashpapi.vitrify.neoforge;

import ashpapi.vitrify.client.VitrifyClient;
import ashpapi.vitrify.client.gui.VitrifyConfigScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = VitrifyClient.MOD_ID, dist = Dist.CLIENT)
public final class VitrifyNeoForgeClient {
    public VitrifyNeoForgeClient(IEventBus modBus, ModContainer container) {
        VitrifyClient.init();
        container.registerExtensionPoint(IConfigScreenFactory.class, (mc, parent) -> new VitrifyConfigScreen(parent));
    }
}
