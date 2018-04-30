package codechicken.aso;

import codechicken.lib.vec.Rectangle4i;
import codechicken.aso.api.GuiInfo;
import codechicken.aso.api.IASOGuiHandler;
import codechicken.aso.guihook.GuiContainerManager;
import codechicken.aso.recipe.GuiCraftingRecipe;
import codechicken.aso.recipe.GuiRecipe;
import codechicken.aso.recipe.GuiUsageRecipe;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;

import static codechicken.lib.gui.GuiDraw.drawRect;

public class ItemPanel extends Widget
{
    /**
     * Should not be externally modified, use updateItemList
     */
    public static ArrayList<ItemStack> items = new ArrayList<ItemStack>();
    /**
     * Swapped into visible items on update
     */
    private static ArrayList<ItemStack> _items = items;

    public static void updateItemList(ArrayList<ItemStack> newItems) {
        _items = newItems;
    }

    public class ItemPanelSlot
    {
        public ItemStack item;
        public int slotIndex;

        public ItemPanelSlot(int index) {
            item = items.get(index);
            slotIndex = index;
        }
    }

    public ItemStack draggedStack;
    public int mouseDownSlot = -1;

    private int marginLeft;
    private int marginTop;
    private int rows;
    private int columns;

    private boolean[] validSlotMap;
    private int firstIndex;
    private int itemsPerPage;

    private int page;
    private int numPages;

    public void resize() {
        items = _items;

        marginLeft = x + (w % 18) / 2;
        marginTop = y + (h % 18) / 2;
        columns = w / 18;
        rows = h / 18;
        //sometimes width and height can be negative with certain resizing
        if(rows < 0) rows = 0;
        if(columns < 0) columns = 0;

        calculatePage();
        updateValidSlots();
    }

    private void calculatePage() {
        if (itemsPerPage == 0)
            numPages = 0;
        else
            numPages = (int) Math.ceil((float) items.size() / (float) itemsPerPage);

        if (firstIndex >= items.size())
            firstIndex = 0;

        if (numPages == 0)
            page = 0;
        else
            page = firstIndex / itemsPerPage + 1;
    }

    private void updateValidSlots() {
        GuiContainer gui = ASOClientUtils.getGuiContainer();
        validSlotMap = new boolean[rows * columns];
        itemsPerPage = 0;
        for (int i = 0; i < validSlotMap.length; i++)
            if (slotValid(gui, i)) {
                validSlotMap[i] = true;
                itemsPerPage++;
            }
    }

    private boolean slotValid(GuiContainer gui, int i) {
        Rectangle4i rect = getSlotRect(i);
        for (IASOGuiHandler handler : GuiInfo.guiHandlers)
            if (handler.hideItemPanelSlot(gui, rect.x, rect.y, rect.w, rect.h))
                return false;
        return true;
    }

    public Rectangle4i getSlotRect(int i) {
        return getSlotRect(i / columns, i % columns);
    }

    public Rectangle4i getSlotRect(int row, int column) {
        return new Rectangle4i(marginLeft + column * 18, marginTop + row * 18, 18, 18);
    }

    @Override
    public void draw(int mousex, int mousey) {
        if (itemsPerPage == 0)
            return;

        GuiContainerManager.enableMatrixStackLogging();
        int index = firstIndex;
        for (int i = 0; i < rows * columns && index < items.size(); i++) {
            if (validSlotMap[i]) {
                Rectangle4i rect = getSlotRect(i);
                if (rect.contains(mousex, mousey))
                    drawRect(rect.x, rect.y, rect.w, rect.h, 0xee555555);//highlight

                GuiContainerManager.drawItem(rect.x + 1, rect.y + 1, items.get(index));

                index++;
            }
        }
        GuiContainerManager.disableMatrixStackLogging();
    }

    @Override
    public void postDraw(int mousex, int mousey) {
        if (draggedStack != null) {
            GuiContainerManager.drawItems.zLevel += 100;
            GuiContainerManager.drawItem(mousex - 8, mousey - 8, draggedStack);
            GuiContainerManager.drawItems.zLevel -= 100;
        }
    }

    @Override
    public void mouseDragged(int mousex, int mousey, int button, long heldTime) {
        if (mouseDownSlot >= 0 && draggedStack == null && ASOClientUtils.getHeldItem() == null &&
                ASOClientConfig.hasSMPCounterPart() && !GuiInfo.hasCustomSlots(ASOClientUtils.getGuiContainer())) {
            ItemPanelSlot mouseOverSlot = getSlotMouseOver(mousex, mousey);
            ItemStack stack = new ItemPanelSlot(mouseDownSlot).item;
            if (stack != null && (mouseOverSlot == null || mouseOverSlot.slotIndex != mouseDownSlot || heldTime > 500)) {
                int amount = ASOClientConfig.getItemQuantity();
                if (amount == 0)
                    amount = stack.getMaxStackSize();

                draggedStack = ASOServerUtils.copyStack(stack, amount);
            }
        }
    }

    @Override
    public boolean handleClick(int mousex, int mousey, int button) {
        if (handleDraggedClick(mousex, mousey, button))
            return true;

        if (ASOClientUtils.getHeldItem() != null) {
            for (IASOGuiHandler handler : GuiInfo.guiHandlers)
                if (handler.hideItemPanelSlot(ASOClientUtils.getGuiContainer(), mousex, mousey, 1, 1))
                    return false;

            if (ASOClientConfig.canPerformAction("delete") && ASOClientConfig.canPerformAction("item"))
                if (button == 1)
                    ASOClientUtils.decreaseSlotStack(-999);
                else
                    ASOClientUtils.deleteHeldItem();
            else
                ASOClientUtils.dropHeldItem();

            return true;
        }

        ItemPanelSlot hoverSlot = getSlotMouseOver(mousex, mousey);
        if (hoverSlot != null) {
            if (button == 2) {
                ItemStack stack = hoverSlot.item;
                if (stack != null) {
                    int amount = ASOClientConfig.getItemQuantity();
                    if (amount == 0)
                        amount = stack.getMaxStackSize();

                    draggedStack = ASOServerUtils.copyStack(stack, amount);
                }
            } else {
                mouseDownSlot = hoverSlot.slotIndex;
            }
            return true;
        }
        return false;
    }

    private boolean handleDraggedClick(int mousex, int mousey, int button) {
        if (draggedStack == null)
            return false;

        GuiContainer gui = ASOClientUtils.getGuiContainer();
        boolean handled = false;
        for (IASOGuiHandler handler : GuiInfo.guiHandlers)
            if (handler.handleDragNDrop(gui, mousex, mousey, draggedStack, button)) {
                handled = true;
                if (draggedStack.stackSize == 0) {
                    draggedStack = null;
                    return true;
                }
            }

        if (handled)
            return true;

        Slot overSlot = gui.getSlotAtPosition(mousex, mousey);
        if (overSlot != null && overSlot.isItemValid(draggedStack)) {
            if (ASOClientConfig.canCheatItem(draggedStack)) {
                int contents = overSlot.getHasStack() ? overSlot.getStack().stackSize : 0;
                int add = button == 0 ? draggedStack.stackSize : 1;
                if (overSlot.getHasStack() && !ASOServerUtils.areStacksSameType(draggedStack, overSlot.getStack()))
                    contents = 0;
                int total = Math.min(contents + add, Math.min(overSlot.getSlotStackLimit(), draggedStack.getMaxStackSize()));

                if (total > contents) {
                    ASOClientUtils.setSlotContents(overSlot.slotNumber, ASOServerUtils.copyStack(draggedStack, total), true);
                    ASOCPH.sendGiveItem(ASOServerUtils.copyStack(draggedStack, total), false, false);
                    draggedStack.stackSize -= total - contents;
                }
                if (draggedStack.stackSize == 0)
                    draggedStack = null;
            } else {
                draggedStack = null;
            }
        } else if (mousex < gui.guiLeft || mousey < gui.guiTop || mousex >= gui.guiLeft + gui.xSize || mousey >= gui.guiTop + gui.ySize) {
            draggedStack = null;
        }

        return true;
    }

    @Override
    public boolean handleClickExt(int mousex, int mousey, int button) {
        return handleDraggedClick(mousex, mousey, button);
    }

    @Override
    public void mouseUp(int mousex, int mousey, int button) {
        ItemPanelSlot hoverSlot = getSlotMouseOver(mousex, mousey);
        if (hoverSlot != null && hoverSlot.slotIndex == mouseDownSlot && draggedStack == null) {
            ItemStack item = hoverSlot.item;
            if (ASOController.manager.window instanceof GuiRecipe || !ASOClientConfig.canCheatItem(item)) {
                if (button == 0)
                    GuiCraftingRecipe.openRecipeGui("item", item);
                else if (button == 1)
                    GuiUsageRecipe.openRecipeGui("item", item);

                draggedStack = null;
                mouseDownSlot = -1;
                return;
            }

            ASOClientUtils.cheatItem(item, button, -1);
        }

        mouseDownSlot = -1;
    }

    @Override
    public boolean onMouseWheel(int i, int mousex, int mousey) {
        if (!contains(mousex, mousey))
            return false;

        scroll(-i);
        return true;
    }

    @Override
    public boolean handleKeyPress(int keyID, char keyChar) {
        if (keyID == ASOClientConfig.getKeyBinding("gui.next")) {
            scroll(1);
            return true;
        }
        if (keyID == ASOClientConfig.getKeyBinding("gui.prev")) {
            scroll(-1);
            return true;
        }
        return false;
    }

    @Override
    public ItemStack getStackMouseOver(int mousex, int mousey) {
        ItemPanelSlot slot = getSlotMouseOver(mousex, mousey);
        return slot == null ? null : slot.item;
    }

    public ItemPanelSlot getSlotMouseOver(int mousex, int mousey) {
        int index = firstIndex;
        for (int i = 0; i < rows * columns && index < items.size(); i++)
            if (validSlotMap[i]) {
                if (getSlotRect(i).contains(mousex, mousey))
                    return new ItemPanelSlot(index);
                index++;
            }

        return null;
    }

    public void scroll(int i) {
        if (itemsPerPage != 0) {
            int oldIndex = firstIndex;
            firstIndex += i * itemsPerPage;
            if (firstIndex >= items.size())
                firstIndex = 0;
            if (firstIndex < 0)
                if (oldIndex > 0)
                    firstIndex = 0;
                else
                    firstIndex = (items.size() - 1) / itemsPerPage * itemsPerPage;

            calculatePage();
        }
    }

    public int getPage() {
        return page;
    }

    public int getNumPages() {
        return numPages;
    }

    @Override
    public boolean contains(int px, int py) {
        GuiContainer gui = ASOClientUtils.getGuiContainer();
        Rectangle4i rect = new Rectangle4i(px, py, 1, 1);
        for (IASOGuiHandler handler : GuiInfo.guiHandlers)
            if (handler.hideItemPanelSlot(gui, rect.x, rect.y, rect.w, rect.h))
                return false;

        return super.contains(px, py);
    }
}
