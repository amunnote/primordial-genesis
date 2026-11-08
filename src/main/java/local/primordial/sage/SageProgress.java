package local.primordial.sage;

import java.util.UUID;
import local.primordial.Primordial;
import local.primordial.network.MenuDropNetwork;
import net.minecraft.advancements.Advancement;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.Attributes;

public final class SageProgress {
    public static final String KEY="primordial:sage_advancements";
    private static final UUID DAMAGE_BONUS=UUID.fromString("25f31e01-e2d3-423d-905d-a25a23c80e21");
    private static final UUID LUCK_BONUS=UUID.fromString("25f31e01-e2d3-423d-905d-a25a23c80e22");
    private static final UUID REDUCTION_BONUS=UUID.fromString("25f31e01-e2d3-423d-905d-a25a23c80e23");
    private static volatile int clientCount;
    private static volatile int clientUnlocks;
    private SageProgress() {}
    private static ListTag earned(Player player) {
        return player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getList(KEY,Tag.TAG_STRING);
    }
    /** every completed advancement counts, vanilla's and other mods' included, but for this mod's hidden mirrors */
    public static int count(Player player) {
        var entries=earned(player);int count=0;
        for(int i=0;i<entries.size();i++)if(counts(ResourceLocation.tryParse(entries.getString(i))))count++;
        return count;
    }
    /** the hidden known/ (recipe) and affix/ (encounter) advancements only mirror state: they do not count */
    public static boolean counts(ResourceLocation advancement) {
        if(advancement==null)return false;
        if(!advancement.getNamespace().equals(Primordial.ID))return true;
        return !advancement.getPath().startsWith("known/")&&!advancement.getPath().startsWith("affix/");
    }
    public static int clientCount() {return clientCount;}
    public static int clientUnlocks() {return clientUnlocks;}
    public static void updateClientStatus(int count,int unlocks) {clientCount=Math.max(0,count);clientUnlocks=unlocks&3;}
    public static void sync(ServerPlayer player) {MenuDropNetwork.syncSage(player,count(player),SageHeart.unlocks(player));}
    public static void copy(Player old,Player fresh) {
        var previous=old.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if(!previous.contains(KEY,Tag.TAG_LIST))return;
        var data=fresh.getPersistentData();var persisted=data.getCompound(Player.PERSISTED_NBT_TAG);
        persisted.put(KEY,previous.getList(KEY,Tag.TAG_STRING).copy());data.put(Player.PERSISTED_NBT_TAG,persisted);
    }
    public static boolean record(ServerPlayer player,Advancement advancement) {
        // every displayed advancement counts (vanilla's, other mods', all of this mod's tabs), but the hidden mirrors
        if(advancement.getDisplay()==null||!counts(advancement.getId()))return false;
        var entries=earned(player);String id=advancement.getId().toString();
        for(int i=0;i<entries.size();i++)if(entries.getString(i).equals(id))return false;
        entries.add(StringTag.valueOf(id));
        var data=player.getPersistentData();CompoundTag persisted=data.getCompound(Player.PERSISTED_NBT_TAG);
        persisted.put(KEY,entries);data.put(Player.PERSISTED_NBT_TAG,persisted);
        refresh(player);sync(player);return true;
    }
    public static void importCompleted(ServerPlayer player) {
        for(var advancement:player.server.getAdvancements().getAllAdvancements())
            if(advancement.getDisplay()!=null&&player.getAdvancements().getOrStartProgress(advancement).isDone())record(player,advancement);
        sync(player);
    }
    public static void refresh(Player player) {
        if(player.level().isClientSide)return;
        boolean equipped=SageHeart.active(player);
        // runs every tick: count (a pass over the list now) once, not per bonus
        int count=equipped?count(player):0;
        update(player,Primordial.DAMAGE.get(),DAMAGE_BONUS,"sage_advancement_damage",equipped?damageBonus(count):0,AttributeModifier.Operation.MULTIPLY_TOTAL);
        update(player,Primordial.REDUCTION.get(),REDUCTION_BONUS,"sage_advancement_reduction",equipped?reductionBonus(count):0,AttributeModifier.Operation.ADDITION);
        update(player,Attributes.LUCK,LUCK_BONUS,"sage_advancement_luck",equipped?luckBonus(count):0,AttributeModifier.Operation.MULTIPLY_TOTAL);
    }
    /** +1% of total Luck per completed advancement, capped at 33% like the damage bonus */
    public static double luckBonus(int count) {return Math.min(0.33,Math.max(0,count)*0.01);}
    public static double damageBonus(int count) {return Math.min(0.33,Math.max(0,count)*0.01);}
    public static double reductionBonus(int count) {return Math.min(0.33,Math.max(0,count)*0.005);}
    private static void update(Player player,Attribute attribute,UUID id,String label,double bonus,AttributeModifier.Operation operation) {
        var instance=player.getAttribute(attribute);if(instance==null)return;
        var existing=instance.getModifier(id);
        if(existing!=null&&Double.compare(existing.getAmount(),bonus)==0)return;
        if(existing!=null)instance.removeModifier(id);
        if(bonus>0)instance.addTransientModifier(new AttributeModifier(id,label,bonus,operation));
    }
}
