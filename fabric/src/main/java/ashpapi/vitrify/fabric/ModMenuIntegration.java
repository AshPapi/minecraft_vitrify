package ashpapi.vitrify.fabric;

import ashpapi.vitrify.client.gui.VitrifyConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public final class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return VitrifyConfigScreen::new;
    }
}
