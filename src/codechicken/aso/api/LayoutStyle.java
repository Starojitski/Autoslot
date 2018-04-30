package codechicken.aso.api;

import codechicken.aso.Button;
import codechicken.aso.VisiblityData;
import codechicken.aso.guihook.GuiContainerManager;
import net.minecraft.client.gui.inventory.GuiContainer;

public abstract class LayoutStyle
{
    public abstract void init();
    public abstract void reset();
    public abstract void layout(GuiContainer gui, VisiblityData visibility);
    public abstract String getName();
    
    public void drawBackground(GuiContainerManager gui)
    {
    }
    public abstract void drawButton(Button button, int mousex, int mousey);
    public abstract void drawSubsetTag(String text, int x, int y, int w, int h, int state, boolean mouseover);
}
