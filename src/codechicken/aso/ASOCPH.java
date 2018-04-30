package codechicken.aso;

import codechicken.core.ClientUtils;
import codechicken.lib.inventory.InventoryUtils;
import codechicken.lib.packet.PacketCustom;
import codechicken.lib.packet.PacketCustom.IClientPacketHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.World;

public class ASOCPH implements IClientPacketHandler
{
    public static final String channel = "ASO";

    @Override
    public void handlePacket(PacketCustom packet, Minecraft mc, INetHandlerPlayClient netHandler) {
        switch (packet.getType()) {
            case 1:
                handleSMPCheck(packet.readUByte(), packet.readString(), mc.theWorld);
                break;
            case 10:
                handleLoginState(packet);
                break;
            case 11:
                handleActionDisabled(packet);
                break;
            case 12:
                handleActionEnabled(packet);
                break;
            case 13:
                ClientHandler.instance().addSMPMagneticItem(packet.readInt(), mc.theWorld);
                break;
            case 14:
                handleGamemode(mc, packet.readUByte());
                break;
            case 21:
                ClientUtils.openSMPGui(packet.readUByte(), new GuiEnchantmentModifier(mc.thePlayer.inventory, mc.theWorld));
                break;
            case 23:
                if (packet.readBoolean())
                    ClientUtils.openSMPGui(packet.readUByte(), new GuiExtendedCreativeInv(new ContainerCreativeInv(mc.thePlayer, new ExtendedCreativeInv(null, Side.CLIENT))));
                else
                    mc.displayGuiScreen(new GuiInventory(mc.thePlayer));
                break;
            case 24:
                ClientUtils.openSMPGui(packet.readUByte(), new GuiPotionCreator(mc.thePlayer.inventory));
                break;
        }
    }

    private void handleGamemode(Minecraft mc, int mode) {
        mc.playerController.setGameType(ASOServerUtils.getGameType(mode));
    }

    private void handleActionEnabled(PacketCustom packet) {
        String name = packet.readString();
        if (packet.readBoolean())
            ASOClientConfig.enabledActions.add(name);
        else
            ASOClientConfig.enabledActions.remove(name);
    }

    private void handleActionDisabled(PacketCustom packet) {
        String name = packet.readString();
        if (packet.readBoolean())
            ASOClientConfig.disabledActions.add(name);
        else
            ASOClientConfig.disabledActions.remove(name);
    }

    private void handleLoginState(PacketCustom packet) {
        ASOClientConfig.permissableActions.clear();
        int num = packet.readUByte();
        for (int i = 0; i < num; i++)
            ASOClientConfig.permissableActions.add(packet.readString());

        ASOClientConfig.disabledActions.clear();
        num = packet.readUByte();
        for (int i = 0; i < num; i++)
            ASOClientConfig.disabledActions.add(packet.readString());

        ASOClientConfig.enabledActions.clear();
        num = packet.readUByte();
        for (int i = 0; i < num; i++)
            ASOClientConfig.enabledActions.add(packet.readString());

        ASOClientConfig.bannedBlocks.clear();
        num = packet.readInt();
        for(int i = 0; i < num; i++)
            ASOClientConfig.bannedBlocks.add(packet.readItemStack());

        if (ASOClientUtils.getGuiContainer() != null)
            LayoutManager.instance().refresh(ASOClientUtils.getGuiContainer());
    }

    private void handleSMPCheck(int serverprotocol, String worldName, World world) {
        if (serverprotocol > ASOActions.protocol) {
            ASOClientUtils.printChatMessage(new ChatComponentTranslation("aso.chat.mismatch.client"));
        } else if (serverprotocol < ASOActions.protocol) {
            ASOClientUtils.printChatMessage(new ChatComponentTranslation("aso.chat.mismatch.server"));
        } else {
            try {
                ClientHandler.instance().loadWorld(world);
                ASOClientConfig.setHasSMPCounterPart(true);
                ASOClientConfig.loadWorld(getSaveName(worldName));
                sendRequestLoginInfo();
            } catch (Exception e) {
                ASOClientConfig.logger.error("Error handling SMP Check", e);
            }
        }
    }

    private static String getSaveName(String worldName) {
        if (Minecraft.getMinecraft().isSingleplayer())
            return "local/" + ClientUtils.getWorldSaveName();

        return "remote/" + ClientUtils.getServerIP().replace(':', '~') + "/" + worldName;
    }

    public static void sendGiveItem(ItemStack spawnstack, boolean infinite, boolean doSpawn) {
        PacketCustom packet = new PacketCustom(channel, 1);
        packet.writeItemStack(spawnstack);
        packet.writeBoolean(infinite);
        packet.writeBoolean(doSpawn);
        packet.sendToServer();
    }

    public static void sendDeleteAllItems() {
        PacketCustom packet = new PacketCustom(channel, 4);
        packet.sendToServer();
    }

    public static void sendStateLoad(ItemStack[] state) {
        sendDeleteAllItems();
        for (int slot = 0; slot < state.length; slot++) {
            ItemStack item = state[slot];
            if (item == null) {
                continue;
            }
            sendSetSlot(slot, item, false);
        }

        PacketCustom packet = new PacketCustom(channel, 11);
        packet.sendToServer();
    }

    public static void sendSetSlot(int slot, ItemStack stack, boolean container) {
        PacketCustom packet = new PacketCustom(channel, 5);
        packet.writeBoolean(container);
        packet.writeShort(slot);
        packet.writeItemStack(stack);
        packet.sendToServer();
    }

    private static void sendRequestLoginInfo() {
        PacketCustom packet = new PacketCustom(channel, 10);
        packet.sendToServer();
    }

    public static void sendToggleMagnetMode() {
        PacketCustom packet = new PacketCustom(channel, 6);
        packet.sendToServer();
    }

    public static void sendSetTime(int hour) {
        PacketCustom packet = new PacketCustom(channel, 7);
        packet.writeByte(hour);
        packet.sendToServer();
    }

    public static void sendHeal() {
        PacketCustom packet = new PacketCustom(channel, 8);
        packet.sendToServer();
    }

    public static void sendToggleRain() {
        PacketCustom packet = new PacketCustom(channel, 9);
        packet.sendToServer();
    }

    public static void sendOpenEnchantmentWindow() {
        PacketCustom packet = new PacketCustom(channel, 21);
        packet.sendToServer();
    }

    public static void sendModifyEnchantment(int enchID, int level, boolean add) {
        PacketCustom packet = new PacketCustom(channel, 22);
        packet.writeByte(enchID);
        packet.writeByte(level);
        packet.writeBoolean(add);
        packet.sendToServer();
    }

    public static void sendSetPropertyDisabled(String name, boolean enable) {
        PacketCustom packet = new PacketCustom(channel, 12);
        packet.writeString(name);
        packet.writeBoolean(enable);
        packet.sendToServer();
    }

    public static void sendGamemode(int mode) {
        new PacketCustom(channel, 13)
                .writeByte(mode)
                .sendToServer();
    }

    public static void sendCreativeInv(boolean open) {
        PacketCustom packet = new PacketCustom(channel, 23);
        packet.writeBoolean(open);
        packet.sendToServer();
    }

    public static void sendCreativeScroll(int steps) {
        PacketCustom packet = new PacketCustom(channel, 14);
        packet.writeInt(steps);
        packet.sendToServer();
    }

    public static void sendMobSpawnerID(int x, int y, int z, String mobtype) {
        PacketCustom packet = new PacketCustom(channel, 15);
        packet.writeCoord(x, y, z);
        packet.writeString(mobtype);
        packet.sendToServer();
    }

    public static PacketCustom createContainerPacket() {
        return new PacketCustom(channel, 20);
    }

    public static void sendOpenPotionWindow() {
        ItemStack[] potionStore = new ItemStack[9];
        InventoryUtils.readItemStacksFromTag(potionStore, ASOClientConfig.global.nbt.getCompoundTag("potionStore").getTagList("items", 10));
        PacketCustom packet = new PacketCustom(channel, 24);
        for (ItemStack stack : potionStore)
            packet.writeItemStack(stack);
        packet.sendToServer();
    }

    public static void sendDummySlotSet(int slotNumber, ItemStack stack) {
        PacketCustom packet = new PacketCustom(channel, 25);
        packet.writeShort(slotNumber);
        packet.writeItemStack(stack);
        packet.sendToServer();
    }
}
