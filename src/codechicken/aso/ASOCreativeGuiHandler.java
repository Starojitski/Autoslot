package codechicken.aso;

import codechicken.aso.api.IASOGuiAdapter;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.creativetab.CreativeTabs;

public class ASOCreativeGuiHandler extends IASOGuiAdapter
{
    @Override
    public VisiblityData modifyVisiblity(GuiContainer gui, VisiblityData currentVisibility)
    {
        if(!(gui instanceof GuiContainerCreative))
            return currentVisibility;
        
        if(((GuiContainerCreative)gui).getSelectedTabIndex() != CreativeTabs.tabInventory.getTabIndex())
            currentVisibility.showItemSection = currentVisibility.enableDeleteMode = false;

        return currentVisibility;
    }

}
