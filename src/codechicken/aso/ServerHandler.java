package codechicken.aso;

import codechicken.lib.packet.PacketCustom;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerChangedDimensionEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerRespawnEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;

import java.util.List;

public class ServerHandler
{
    private static ServerHandler instance;

    public static void init() {
        instance = new ServerHandler();

        PacketCustom.assignHandler(ASOCPH.channel, new ASOSPH());
        FMLCommonHandler.instance().bus().register(instance);
        MinecraftForge.EVENT_BUS.register(instance);

        Item.getItemFromBlock(Blocks.mob_spawner).setHasSubtypes(true);
        ASOActions.init();
    }

    @SubscribeEvent
    public void tickEvent(TickEvent.WorldTickEvent event) {
        if (event.phase == Phase.START && !event.world.isRemote &&
                ASOServerConfig.dimTags.containsKey(event.world.provider.getDimensionId()))//fake worlds that don't call Load
            processDisabledProperties(event.world);
    }

    @SubscribeEvent
    public void loadEvent(WorldEvent.Load event) {
        if(!event.world.isRemote)
            ASOServerConfig.load(event.world);
    }

    @SubscribeEvent
    public void tickEvent(TickEvent.PlayerTickEvent event) {
        if (event.phase == Phase.START && event.player instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP) event.player;
            PlayerSave save = ASOServerConfig.forPlayer(player.getCommandSenderName());
            if (save == null)
                return;
            updateMagneticPlayer(player, save);
            save.updateOpChange();
            save.save();
        }
    }

    private void processDisabledProperties(World world) {
        ASOServerUtils.advanceDisabledTimes(world);
        if (ASOServerUtils.isRaining(world) && ASOServerConfig.isActionDisabled(world.provider.getDimensionId(), "rain"))
            ASOServerUtils.toggleRaining(world, false);
    }

    private void updateMagneticPlayer(EntityPlayerMP player, PlayerSave save) {
        if (!save.isActionEnabled("magnet") || player.isDead)
            return;

        float distancexz = 16;
        float distancey = 8;
        double maxspeedxz = 0.5;
        double maxspeedy = 0.5;
        double speedxz = 0.05;
        double speedy = 0.07;
        List<EntityItem> items = player.worldObj.getEntitiesWithinAABB(EntityItem.class, player.getEntityBoundingBox().expand(distancexz, distancey, distancexz));
        for (EntityItem item : items) {
            if (item.cannotPickup()) continue;
            if (!ASOServerUtils.canItemFitInInventory(player, item.getEntityItem())) continue;
            if (save.magneticItems.add(item))
                ASOSPH.sendAddMagneticItemTo(player, item);

            double dx = player.posX - item.posX;
            double dy = player.posY + player.getEyeHeight() - item.posY;
            double dz = player.posZ - item.posZ;
            double absxz = Math.sqrt(dx * dx + dz * dz);
            double absy = Math.abs(dy);
            if (absxz > distancexz)
                continue;

            if (absxz < 1)
                item.onCollideWithPlayer(player);

            if (absxz > 1) {
                dx /= absxz;
                dz /= absxz;
            }

            if (absy > 1)
                dy /= absy;

            double vx = item.motionX + speedxz * dx;
            double vy = item.motionY + speedy * dy;
            double vz = item.motionZ + speedxz * dz;

            double absvxz = Math.sqrt(vx * vx + vz * vz);
            double absvy = Math.abs(vy);

            double rationspeedxz = absvxz / maxspeedxz;
            if (rationspeedxz > 1) {
                vx /= rationspeedxz;
                vz /= rationspeedxz;
            }

            double rationspeedy = absvy / maxspeedy;
            if (absvy > 1)
                vy /= rationspeedy;

            item.motionX = vx;
            item.motionY = vy;
            item.motionZ = vz;
        }
    }

    @SubscribeEvent
    public void loginEvent(PlayerLoggedInEvent event) {
        ASOServerConfig.loadPlayer(event.player);
        ASOSPH.sendHasServerSideTo((EntityPlayerMP) event.player);
    }

    @SubscribeEvent
    public void logoutEvent(PlayerLoggedOutEvent event) {
        ASOServerConfig.unloadPlayer(event.player);
    }

    @SubscribeEvent
    public void dimChangeEvent(PlayerChangedDimensionEvent event) {
        ASOServerConfig.forPlayer(event.player.getCommandSenderName()).onWorldReload();
    }

    @SubscribeEvent
    public void loginEvent(PlayerRespawnEvent event) {
        ASOServerConfig.forPlayer(event.player.getCommandSenderName()).onWorldReload();
    }
}
