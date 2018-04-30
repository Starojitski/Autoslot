package codechicken.aso;

import codechicken.core.inventory.ContainerExtended;
import codechicken.core.inventory.SlotDummy;
import codechicken.aso.api.IASOGuiAdapter;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class ASODummySlotHandler extends IASOGuiAdapter
{
    @Override
    public boolean handleDragNDrop(GuiContainer gui, int mousex, int mousey, ItemStack draggedStack, int button)
    {
        Slot slot = gui.getSlotAtPosition(mousex, mousey);
        if(slot instanceof SlotDummy && slot.isItemValid(draggedStack) && gui.inventorySlots instanceof ContainerExtended)
        {
            ((SlotDummy)slot).slotClick(draggedStack, button, ASOClientUtils.shiftKey());
            ASOCPH.sendDummySlotSet(slot.slotNumber, slot.getStack());
            return true;
        }
        return false;
    }
}
