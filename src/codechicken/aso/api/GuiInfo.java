package codechicken.aso.api;

import codechicken.aso.ASOChestGuiHandler;
import codechicken.aso.ASOCreativeGuiHandler;
import codechicken.aso.ASODummySlotHandler;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiContainerCreative;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

public class GuiInfo
{
    public static LinkedList<IASOGuiHandler> guiHandlers = new LinkedList<IASOGuiHandler>();
    public static HashSet<Class<? extends GuiContainer>> customSlotGuis = new HashSet<Class<? extends GuiContainer>>();

    public static void load() {
        API.registerASOGuiHandler(new ASOCreativeGuiHandler());
        API.registerASOGuiHandler(new ASOChestGuiHandler());
        API.registerASOGuiHandler(new ASODummySlotHandler());
        customSlotGuis.add(GuiContainerCreative.class);
    }

    public static void clearGuiHandlers() {
        for (Iterator<IASOGuiHandler> iterator = guiHandlers.iterator(); iterator.hasNext(); )
            if (iterator.next() instanceof GuiContainer)
                iterator.remove();
    }

    public static boolean hasCustomSlots(GuiContainer gui) {
        return customSlotGuis.contains(gui.getClass());
    }
}
