package codechicken.aso;

import codechicken.aso.api.IASOGuiAdapter;
import codechicken.aso.api.TaggedInventoryArea;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.item.ItemStack;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ASOChestGuiHandler extends IASOGuiAdapter
{
    public int chestSize(GuiContainer gui) {
        return ((ContainerChest) gui.inventorySlots).getLowerChestInventory().getSizeInventory();
    }

    @Override
    public Iterable<Integer> getItemSpawnSlots(GuiContainer gui, ItemStack item) {
        return gui instanceof GuiChest ? ASOServerUtils.getRange(0, chestSize(gui)) : Collections.<Integer>emptyList();
    }

    @Override
    public List<TaggedInventoryArea> getInventoryAreas(GuiContainer gui) {
        return gui instanceof GuiChest ? Arrays.asList(new TaggedInventoryArea("Chest", 0, chestSize(gui), gui.inventorySlots)) : null;
    }
}
