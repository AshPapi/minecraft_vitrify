package ashpapi.vitrify.client.render;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.GuiRenderState;

public interface GuiGraphicsAccess {
    GuiRenderState vitrify$guiRenderState();

    ScreenRectangle vitrify$currentScissor();
}
