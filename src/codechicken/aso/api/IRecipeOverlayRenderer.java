package codechicken.aso.api;

import codechicken.aso.guihook.GuiContainerManager;
import net.minecraft.inventory.Slot;

public interface IRecipeOverlayRenderer
{
    public void renderOverlay(GuiContainerManager gui, Slot slot);
}
