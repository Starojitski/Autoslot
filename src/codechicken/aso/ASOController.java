package codechicken.aso;

import codechicken.aso.api.*;
import codechicken.aso.guihook.GuiContainerManager;
import codechicken.aso.guihook.IContainerInputHandler;
import codechicken.aso.guihook.IContainerSlotClickHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.inventory.SlotCrafting;
import net.minecraft.item.ItemStack;

import java.awt.*;
import java.util.LinkedList;

import static codechicken.lib.gui.GuiDraw.getMousePosition;

public class ASOController implements IContainerSlotClickHandler, IContainerInputHandler
{
    private static ASOController instance = new ASOController();

    public static GuiContainerManager manager;
    public static FastTransferManager fastTransferManager;

    private static boolean deleteMode;
    private static int pickedUpFromSlot;
    private static IInfiniteItemHandler heldStackInfinite;

    private static int selectedItem;
    private ItemStack firstheld;

    public static void load() {
        GuiContainerManager.addSlotClickHandler(instance);
        GuiContainerManager.addInputHandler(instance);
    }

    public static void load(GuiContainer gui) {
        manager = GuiContainerManager.getManager(gui);
        deleteMode = false;
        GuiInfo.clearGuiHandlers();
        fastTransferManager = null;
        if (!ASOClientConfig.isEnabled())
            return;

        fastTransferManager = new FastTransferManager();
        if (gui instanceof IASOGuiHandler)
            API.registerASOGuiHandler((IASOGuiHandler) gui);
    }

    public static boolean isSpreading(GuiContainer gui) {
        return gui.dragSplitting && gui.dragSplittingSlots.size() > 1;
    }

    public static void updateUnlimitedItems(InventoryPlayer inventory) {
        if (!ASOClientConfig.canPerformAction("item") || !ASOClientConfig.hasSMPCounterPart())
            return;

        LinkedList<ItemStack> beforeStacks = new LinkedList<ItemStack>();
        for (int i = 0; i < inventory.getSizeInventory(); i++)
            beforeStacks.add(ASOServerUtils.copyStack(inventory.getStackInSlot(i)));

        for (int i = 0; i < inventory.getSizeInventory(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack == null)
                continue;

            for (IInfiniteItemHandler handler : ItemInfo.infiniteHandlers)
                if (handler.canHandleItem(stack) && handler.isItemInfinite(stack))
                    handler.replenishInfiniteStack(inventory, i);
        }

        for (int i = 0; i < inventory.getSizeInventory(); i++) {
            ItemStack newstack = inventory.getStackInSlot(i);

            if (!ASOServerUtils.areStacksIdentical(beforeStacks.get(i), newstack)) {
                inventory.setInventorySlotContents(i, beforeStacks.get(i));//restore in case of SMP fail
                ASOClientUtils.setSlotContents(i, newstack, false);//sends via SMP handler ;)
            }
        }
    }

    public static void processCreativeCycling(InventoryPlayer inventory) {
        if (ASOClientConfig.invCreativeMode() && ASOClientUtils.controlKey()) {
            if (selectedItem != inventory.currentItem) {
                if (inventory.currentItem == selectedItem + 1 || (inventory.currentItem == 0 && selectedItem == 8))//foward
                {
                    ASOCPH.sendCreativeScroll(1);
                    inventory.currentItem = selectedItem;
                } else if (inventory.currentItem == selectedItem - 1 || (inventory.currentItem == 8 && selectedItem == 0)) {
                    ASOCPH.sendCreativeScroll(-1);
                    inventory.currentItem = selectedItem;
                }
            }
        }

        selectedItem = inventory.currentItem;
    }

    @Override
    public void beforeSlotClick(GuiContainer gui, int slotIndex, int button, Slot slot, int modifier) {
        if (!ASOClientConfig.isEnabled())
            return;

        firstheld = ASOClientUtils.getHeldItem();
    }

    @Override
    public boolean handleSlotClick(GuiContainer gui, int slotIndex, int button, Slot slot, int modifier, boolean eventconsumed) {
        if (eventconsumed ||
                !ASOClientConfig.isEnabled() ||
                isSpreading(gui))
            return eventconsumed;

        if (deleteMode && slotIndex >= 0 && slot != null) {
            if (ASOClientUtils.shiftKey() && button == 0) {
                ItemStack itemstack1 = slot.getStack();
                if (itemstack1 != null)
                    ASOClientUtils.deleteItemsOfType(itemstack1);

            } else if (button == 1)
                ASOClientUtils.decreaseSlotStack(slot.slotNumber);
            else
                ASOClientUtils.deleteSlotStack(slot.slotNumber);
            return true;
        }

        if (button == 1 && slot instanceof SlotCrafting)//right click
        {
            for (int i1 = 0; i1 < 64; i1++)//click this slot 64 times
                manager.handleSlotClick(slot.slotNumber, button, 0);
            return true;
        }

        if (ASOClientUtils.controlKey()
                && slot != null && slot.getStack() != null
                && slot.isItemValid(slot.getStack())) {
            ASOClientUtils.cheatItem(slot.getStack(), button, 1);
            return true;
        }

        if(GuiInfo.hasCustomSlots(gui))
            return false;

        if (slotIndex >= 0 && ASOClientUtils.shiftKey() && ASOClientUtils.getHeldItem() != null && !slot.getHasStack()) {
            ItemStack held = ASOClientUtils.getHeldItem();
            manager.handleSlotClick(slot.slotNumber, button, 0);
            if (slot.isItemValid(held) && !ItemInfo.fastTransferExemptions.contains(slot.getClass()))
                fastTransferManager.performMassTransfer(gui, pickedUpFromSlot, slotIndex, held);

            return true;
        }

        if (slotIndex == -999 && ASOClientUtils.shiftKey() && button == 0) {
            fastTransferManager.throwAll(gui, pickedUpFromSlot);
            return true;
        }

        return false;
    }

    @Override
    public void afterSlotClick(GuiContainer gui, int slotIndex, int button, Slot slot, int modifier) {
        if (!ASOClientConfig.isEnabled())
            return;

        ItemStack nowHeld = ASOClientUtils.getHeldItem();

        if (firstheld != nowHeld)
            pickedUpFromSlot = slotIndex;

        if (ASOClientConfig.canPerformAction("item") && ASOClientConfig.hasSMPCounterPart()) {
            if (heldStackInfinite != null && slot != null && slot.inventory == Minecraft.getMinecraft().thePlayer.inventory) {
                ItemStack stack = slot.getStack();
                if (stack != null) {
                    heldStackInfinite.onPlaceInfinite(stack);
                }
                ASOClientUtils.setSlotContents(slotIndex, stack, true);
            }

            if (firstheld != nowHeld)
                heldStackInfinite = null;

            if (firstheld != nowHeld && nowHeld != null) {
                for (IInfiniteItemHandler handler : ItemInfo.infiniteHandlers) {
                    if (handler.canHandleItem(nowHeld) && handler.isItemInfinite(nowHeld)) {
                        handler.onPickup(nowHeld);
                        ASOClientUtils.setSlotContents(-999, nowHeld, true);
                        heldStackInfinite = handler;
                        break;
                    }
                }
            }
        }
    }

    @Override
    public boolean lastKeyTyped(GuiContainer gui, char keyChar, int keyCode) {
        if (!ASOClientConfig.isEnabled() ||
                GuiInfo.hasCustomSlots(gui) ||
                isSpreading(gui))
            return false;

        Slot slot = GuiContainerManager.getSlotMouseOver(gui);
        if (slot == null)
            return false;

        int slotIndex = slot.slotNumber;

        if (keyCode == Minecraft.getMinecraft().gameSettings.keyBindDrop.getKeyCode() && ASOClientUtils.shiftKey()) {
            FastTransferManager.clickSlot(gui, slotIndex);
            fastTransferManager.throwAll(gui, slotIndex);
            FastTransferManager.clickSlot(gui, slotIndex);

            return true;
        }

        return false;
    }

    @Override
    public boolean mouseScrolled(GuiContainer gui, int mousex, int mousey, int scrolled) {
        if (!ASOClientConfig.isEnabled() || GuiInfo.hasCustomSlots(gui))
            return false;

        Point mousePos = getMousePosition();
        Slot mouseover = manager.window.getSlotAtPosition(mousePos.x, mousePos.y);
        if (mouseover != null && mouseover.getHasStack()) {
            if (scrolled > 0)
                fastTransferManager.transferItem(manager.window, mouseover.slotNumber);
            else
                fastTransferManager.retrieveItem(manager.window, mouseover.slotNumber);
            return true;
        }
        return false;
    }

    @Override
    public boolean keyTyped(GuiContainer gui, char keyChar, int keyCode) {
        return false;
    }

    @Override
    public boolean mouseClicked(GuiContainer gui, int mousex, int mousey, int button) {
        return false;
    }

    @Override
    public void onKeyTyped(GuiContainer gui, char keyChar, int keyID) {
    }

    @Override
    public void onMouseClicked(GuiContainer gui, int mousex, int mousey, int button) {
    }

    @Override
    public void onMouseDragged(GuiContainer gui, int mousex, int mousey, int button, long heldTime) {
    }

    @Override
    public void onMouseScrolled(GuiContainer gui, int mousex, int mousey, int scrolled) {
    }

    @Override
    public void onMouseUp(GuiContainer gui, int mousex, int mousey, int button) {
    }

    public static boolean canUseDeleteMode() {
        return !(ASOClientUtils.getGuiContainer() instanceof GuiContainerCreative);
    }

    public static void toggleDeleteMode() {
        if(canUseDeleteMode())
            deleteMode = !deleteMode;
    }

    public static boolean getDeleteMode() {
        return deleteMode;
    }
}
