package local.primordial.effect;

import local.primordial.Primordial;
import local.primordial.affix.AffixAccessories;
import local.primordial.affix.MobAffix;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Space's accessory: an 18-slot pack of the wearer's own. It is kept on the player (under {@link Player#PERSISTED_NBT_TAG},
 * so it survives respawning and changing worlds) and can be opened only while the accessory is worn; taken off, the pack
 * is out of reach but keeps everything in it. On death what is in it drops as the main inventory does (kept with
 * keepInventory). It opens from a button on the Curios screen (SpacePackButton -> MenuDropNetwork.OpenSpacePack) as a
 * plain 9x2 chest menu, so it needs no screen of its own.
 */
@Mod.EventBusSubscriber(modid=Primordial.ID)
public final class SpacePack {
    public static final int SIZE=18;
    private static final String KEY="primordial:space_pack";
    private SpacePack() {}

    public static boolean usable(Player player) {return player.isAlive()&&!player.isSpectator()&&AffixAccessories.wears(player,MobAffix.SPACE);}
    static NonNullList<ItemStack> load(Player player) {
        var items=NonNullList.withSize(SIZE,ItemStack.EMPTY);
        ContainerHelper.loadAllItems(player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getCompound(KEY),items);
        return items;
    }
    static void save(Player player,NonNullList<ItemStack> items) {
        var data=player.getPersistentData();var kept=data.getCompound(Player.PERSISTED_NBT_TAG);
        var tag=new CompoundTag();ContainerHelper.saveAllItems(tag,items,true);
        kept.put(KEY,tag);data.put(Player.PERSISTED_NBT_TAG,kept);
    }
    public static void open(ServerPlayer player) {
        if(!usable(player)){player.displayClientMessage(Component.translatable("message.primordial.space_pack.closed"),true);return;}
        var pack=new Pack(player);
        player.openMenu(new SimpleMenuProvider((id,inventory,who)->new ChestMenu(MenuType.GENERIC_9x2,id,inventory,pack,2),
            Component.translatable("container.primordial.space_pack")));
    }
    /** what the pack holds while it is open: written back to the player on every change */
    static final class Pack extends SimpleContainer {
        private final Player owner;
        private boolean loading;
        Pack(Player owner) {
            super(SIZE);this.owner=owner;
            var items=load(owner);loading=true;
            for(int i=0;i<SIZE;i++)setItem(i,items.get(i));
            loading=false;
        }
        @Override public void setChanged() {super.setChanged();if(!loading)write();}
        @Override public boolean stillValid(Player player) {return player==owner&&usable(owner);}
        @Override public void stopOpen(Player player) {super.stopOpen(player);write();}
        private void write() {
            var items=NonNullList.withSize(SIZE,ItemStack.EMPTY);
            for(int i=0;i<SIZE;i++)items.set(i,getItem(i));
            save(owner,items);
        }
    }
    /** death: the pack drops what it holds, as the main inventory does (nothing with keepInventory) */
    @SubscribeEvent(priority=EventPriority.LOW) public static void packDrops(LivingDropsEvent event) {
        if(!(event.getEntity() instanceof Player player)||player.level().isClientSide
            ||player.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY))return;
        var items=load(player);
        // a pack still open must not write its contents back once the menu closes
        if(player.containerMenu instanceof ChestMenu menu&&menu.getContainer() instanceof Pack pack)pack.clearContent();
        save(player,NonNullList.withSize(SIZE,ItemStack.EMPTY));
        for(var stack:items)if(!stack.isEmpty()){
            var drop=new ItemEntity(player.level(),player.getX(),player.getEyeY()-0.3,player.getZ(),stack);
            drop.setPickUpDelay(40);
            event.getDrops().add(drop);
        }
    }
}
