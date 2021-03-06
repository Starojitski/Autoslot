package codechicken.aso;

import codechicken.lib.inventory.InventoryUtils;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;

import java.io.File;
import java.util.HashSet;

public class PlayerSave
{
    public EntityPlayerMP player;

    private File saveFile;
    private NBTTagCompound nbt;

    public ItemStack[] creativeInv;
    private boolean creativeInvDirty;

    private boolean isDirty;
    private boolean wasOp;

    //runtime things
    public HashSet<EntityItem> magneticItems = new HashSet<EntityItem>();

    public PlayerSave(EntityPlayerMP player, File saveLocation) {
        this.player = player;
        wasOp = MinecraftServer.getServer().getConfigurationManager().canSendCommands(player.getGameProfile());

        saveFile = new File(saveLocation, player.getCommandSenderName() + ".dat");
        if (!saveFile.getParentFile().exists())
            saveFile.getParentFile().mkdirs();
        load();
    }

    private void load() {
        nbt = new NBTTagCompound();
        try {
            if (!saveFile.exists())
                saveFile.createNewFile();
            if (saveFile.length() > 0)
                nbt = ASOServerUtils.readNBT(saveFile);
        } catch (Exception e) {
            ASOClientConfig.logger.error("Error loading player save: "+player, e);
        }

        loadCreativeInv();
    }

    private void loadCreativeInv() {
        creativeInv = new ItemStack[54];
        NBTTagList itemList = nbt.getTagList("creativeitems", 10);
        if (itemList != null)
            InventoryUtils.readItemStacksFromTag(creativeInv, itemList);
    }

    public void save() {
        if (!isDirty)
            return;

        if (creativeInvDirty)
            saveCreativeInv();

        try {
            ASOServerUtils.writeNBT(nbt, saveFile);
            isDirty = false;
        } catch (Exception e) {
            ASOClientConfig.logger.error("Error saving player: "+player, e);
        }
    }

    private void saveCreativeInv() {
        NBTTagList invsave = InventoryUtils.writeItemStacksToTag(creativeInv);
        nbt.setTag("creativeitems", invsave);

        creativeInvDirty = false;
    }

    public void setCreativeDirty() {
        creativeInvDirty = isDirty = true;
    }

    public void setDirty() {
        isDirty = true;
    }

    public void updateOpChange() {
        boolean isOp = MinecraftServer.getServer().getConfigurationManager().canSendCommands(player.getGameProfile());
        if (isOp != wasOp) {
            ASOSPH.sendHasServerSideTo(player);
            wasOp = isOp;
        }
    }

    public boolean isActionEnabled(String name) {
        return getEnabledActions().getBoolean(name);
    }

    private NBTTagCompound getEnabledActions() {
        NBTTagCompound tag = nbt.getCompoundTag("enabledActions");
        if (!nbt.hasKey("enabledActions"))
            nbt.setTag("enabledActions", tag);
        return tag;
    }

    public void enableAction(String name, boolean enabled) {
        getEnabledActions().setBoolean(name, enabled);
        ASOSPH.sendActionEnabled(player, name, enabled);
        setDirty();
    }

    public void onWorldReload() {
        ASOSPH.sendHasServerSideTo(player);
        magneticItems.clear();
    }
}
