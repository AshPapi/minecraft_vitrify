package ashpapi.vitrify.client.render;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.joml.Matrix3x2fc;

public interface AnimatedPipState {
    void vitrify$setAnimation(Matrix3x2fc pose, int premultipliedColor, ScreenRectangle transformedBounds);

    boolean vitrify$hasAnimation();

    int vitrify$premultipliedColor();
}
