package ashpapi.vitrify.client.render;

public interface ContainerBoundsAccess {
    int[] vitrify$effectiveBounds();

    boolean vitrify$hasMenuSlotNear(int screenX, int screenY);
}
