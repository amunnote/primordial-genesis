package local.primordial.sage;

import java.util.*;
import local.primordial.Primordial;
import local.primordial.effect.Protection;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import top.theillusivec4.curios.api.CuriosApi;

public final class SageHeart {
    public static final String GIVEN="primordial:sage_given";
    private static final String NETHER_UNLOCKED="primordial:sage_nether_unlocked";
    private static final String END_UNLOCKED="primordial:sage_end_unlocked";
    private static final String SLOT_RELEASE_DUE="primordial:sage_slot_release_due";
    public static final TagKey<Item> ALCHEMICAL_WEAPONS=TagKey.create(Registries.ITEM,new ResourceLocation(Primordial.ID,"alchemical_weapons"));
    /** an alchemical treasure (炼金宝具): tagged as one, or a weapon with affixes forged in (伪-炼金宝具) */
    public static boolean alchemicalTreasure(net.minecraft.world.item.ItemStack stack) {return stack.is(ALCHEMICAL_WEAPONS)||local.primordial.affix.Imbuing.pseudoTreasure(stack);}
    /** under the End's curse, the share of its harm a weapon keeps: a true alchemical treasure all, a pseudo one 75%, anything else half */
    public static final float TREASURE_SHARE=1F,PSEUDO_SHARE=0.75F,ORDINARY_SHARE=0.5F;
    public static float weaponShare(net.minecraft.world.item.ItemStack stack) {
        if(stack.is(ALCHEMICAL_WEAPONS))return TREASURE_SHARE;
        return local.primordial.affix.Imbuing.pseudoWeapon(stack)?PSEUDO_SHARE:ORDINARY_SHARE;
    }
    /** under the Nether's curse, the share of its armor a piece keeps: pseudo alchemical armor half, anything else none */
    public static final double PSEUDO_ARMOR_SHARE=0.5;
    private static final UUID HEALTH_CAP=UUID.fromString("7b82d337-6b84-477a-9136-095aae17c101");
    private static final UUID ARMOR_CURSE=UUID.fromString("7b82d337-6b84-477a-9136-095aae17c102");
    private static final UUID TOUGHNESS_CURSE=UUID.fromString("7b82d337-6b84-477a-9136-095aae17c103");
    private SageHeart() {}
    public static boolean active(Entity entity) {
        if(!(entity instanceof Player player)||!Primordial.SAGE_HEART.isPresent())return false;
        return CuriosApi.getCuriosInventory(player).map(inv->inv.isEquipped(Primordial.SAGE_HEART.get())).orElse(false);
    }
    public static boolean bound(LivingEntity wearer, ItemStack stack) {
        return wearer!=null&&!wearer.level().isClientSide&&stack.is(Primordial.SAGE_HEART.get())
            &&!(wearer instanceof Player player && player.isCreative())&&!Protection.active(wearer)&&wearer.isAlive();
    }
    public static int unlocks(Player player) {
        var saved=player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        return (saved.getBoolean(NETHER_UNLOCKED)?1:0)|(saved.getBoolean(END_UNLOCKED)?2:0);
    }
    public static boolean netherUnlocked(Entity entity) {return entity instanceof Player player&&(unlocks(player)&1)!=0;}
    public static boolean endUnlocked(Entity entity) {return entity instanceof Player player&&(unlocks(player)&2)!=0;}
    public static void copyUnlocks(Player previous,Player current) {
        int flags=unlocks(previous);
        if(flags==0)return;
        var persisted=current.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if((flags&1)!=0)persisted.putBoolean(NETHER_UNLOCKED,true);
        if((flags&2)!=0)persisted.putBoolean(END_UNLOCKED,true);
        current.getPersistentData().put(Player.PERSISTED_NBT_TAG,persisted);
    }
    public static boolean discover(Player player) {
        if(player.level().isClientSide)return false;
        var persisted=player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        String key=player.level().dimension()==net.minecraft.world.level.Level.NETHER?NETHER_UNLOCKED
            :player.level().dimension()==net.minecraft.world.level.Level.END?END_UNLOCKED:null;
        if(key==null||persisted.getBoolean(key))return false;
        persisted.putBoolean(key,true);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG,persisted);
        return true;
    }
    public static void scheduleSlotRelease(Player player) {
        if(!player.level().isClientSide)
            player.getPersistentData().putLong(SLOT_RELEASE_DUE,player.level().getGameTime()+1);
    }
    public static void processSlotRelease(Player player) {
        if(player.level().isClientSide)return;
        var data=player.getPersistentData();
        if(!data.contains(SLOT_RELEASE_DUE)||player.level().getGameTime()<data.getLong(SLOT_RELEASE_DUE))return;
        data.remove(SLOT_RELEASE_DUE);
        releaseSlots(player);
    }
    public static void releaseSlots(Player player) {
        if(player.level().isClientSide||!player.isAlive()||active(player)||!Protection.active(player)&&!player.isCreative())return;
        // This callback follows removal of this item's slot modifiers. The wearer explicitly
        // released the heart through Ascension, so Curios may return any displaced items normally.
        local.primordial.effect.InventoryProtection.userEdit(()->CuriosApi.getCuriosInventory(player).ifPresent(inv->{
            var refresh=UUID.fromString("7b82d337-6b84-477a-9136-095aae17c104");
            for(String name:List.of(Primordial.ALCHEMICAL_SLOT,Primordial.ALCHEMICAL_ACCESSORY_SLOT))inv.getStacksHandler(name).ifPresent(handler->{
                handler.addTransientModifier(new AttributeModifier(refresh,"sage_slot_refresh",0,AttributeModifier.Operation.ADDITION));
                handler.removeModifier(refresh);handler.update();
            });
        }));
    }
    public static boolean attackCapable(Mob mob) {
        return mob instanceof Enemy||mob instanceof RangedAttackMob
            ||mob.getAttribute(Attributes.ATTACK_DAMAGE)!=null&&mob.getAttributeValue(Attributes.ATTACK_DAMAGE)>0;
    }
    public static boolean enemyOf(Mob mob, Entity other) {
        return attackCapable(mob)&&other instanceof Player player
            &&((active(player)&&endUnlocked(player)&&!Protection.active(player))
                ||local.primordial.affix.AffixStatuses.malice(player)>0)
            &&player.isAlive()&&!player.isCreative()&&!player.isSpectator();
    }
    public static MobEffectInstance effect(LivingEntity owner,MobEffectInstance original) {
        // Only the server rescales. The client receives the already-rescaled effect from the server (through
        // forceAddEffect) and must show it as sent: rescaling it again made a 2-minute effect read 1 minute or less.
        if(owner.level().isClientSide)return original;
        if(!active(owner)||Protection.active(owner)||original.isInfiniteDuration()||original.getEffect().getCategory()==MobEffectCategory.NEUTRAL)return original;
        var copy=copyEffect(original);
        resizeEffect(copy,original.getEffect().getCategory()==MobEffectCategory.BENEFICIAL);
        return copy;
    }
    private static MobEffectInstance copyEffect(MobEffectInstance original) {
        var copy=new MobEffectInstance(original);
        var hidden=((local.primordial.mixin.SageEffectAccess)(Object)original).sageHidden();
        if(hidden!=null)((local.primordial.mixin.SageEffectAccess)(Object)copy).sageHidden(copyEffect(hidden));
        return copy;
    }
    private static void resizeEffect(MobEffectInstance effect,boolean beneficial) {
        var access=(local.primordial.mixin.SageEffectAccess)(Object)effect;
        if(!effect.isInfiniteDuration())access.sageDuration(beneficial?Math.max(1,effect.getDuration()/2):(int)Math.min(Integer.MAX_VALUE,(long)effect.getDuration()*2));
        if(access.sageHidden()!=null)resizeEffect(access.sageHidden(),beneficial);
    }
    /** after joining, respawning or changing dimension, how long the level and health curses wait for the wearer's
     *  charms to be put back on (their luck is not there yet on the first ticks, and reading it then wiped the level) */
    public static final int SETTLE_TICKS=100;
    private static final String SETTLE="primordial:sage_settle_until";
    public static void settle(Player player) {if(!player.level().isClientSide)player.getPersistentData().putLong(SETTLE,player.level().getGameTime()+SETTLE_TICKS);}
    public static boolean settling(Player player) {return player.level().getGameTime()<player.getPersistentData().getLong(SETTLE);}
    /** whether every luck the wearer's curios give is really on it yet: until then its luck reads too low to judge by */
    public static boolean luckSettled(Player player) {
        var luck=player.getAttribute(Attributes.LUCK);if(luck==null)return true;
        boolean[] settled={true};
        CuriosApi.getCuriosInventory(player).ifPresent(inv->{for(var result:inv.findCurios(stack->!stack.isEmpty())){
            var context=result.slotContext();var uuid=CuriosApi.getSlotUuid(context);
            for(var modifier:CuriosApi.getAttributeModifiers(context,uuid,result.stack()).get(Attributes.LUCK))
                if(luck.getModifier(modifier.getId())==null){settled[0]=false;return;}
        }});
        return settled[0];
    }
    /** the level and health curses only judge by a luck that is all there */
    static boolean judging(Player player) {return !settling(player)&&luckSettled(player);}
    public static void capExperience(Player player) {
        if(!active(player)||Protection.active(player)||!judging(player))return;
        double luck=player.getAttributeValue(Attributes.LUCK);
        int limit=(int)Math.max(0,Math.min(Integer.MAX_VALUE,luck));
        // At the highest legal integer level discard excess progress, rather than banking forbidden levels.
        if(player.experienceLevel>=limit){player.experienceLevel=limit;player.experienceProgress=0;
            player.totalExperience=Math.min(player.totalExperience,experienceAt(limit));}
    }
    public static int allowedExperience(Player player,int amount) {
        if(amount<=0||!active(player)||Protection.active(player))return amount;
        int limit=(int)Math.max(0,Math.min(Integer.MAX_VALUE,player.getAttributeValue(Attributes.LUCK)));
        long current=(long)experienceAt(player.experienceLevel)+Math.round(player.experienceProgress*player.getXpNeededForNextLevel());
        return (int)Math.min(amount,Math.max(0,(long)experienceAt(limit)-current));
    }
    public static int experienceAt(int level) {
        double value=level<=16?(double)level*level+6D*level:level<=31?2.5D*level*level-40.5D*level+360:4.5D*level*level-162.5D*level+2220;
        return (int)Math.min(Integer.MAX_VALUE,Math.max(0,value));
    }
    public static void maintain(Player player) {
        if(player.level().isClientSide)return;
        SageProgress.refresh(player);
        boolean active=active(player);
        var health=player.getAttribute(Attributes.MAX_HEALTH);
        health.removeModifier(HEALTH_CAP);
        boolean curseArmor=active&&netherUnlocked(player)&&!Protection.active(player);
        adjustEquipmentArmor(player,Attributes.ARMOR,ARMOR_CURSE,curseArmor);
        adjustEquipmentArmor(player,Attributes.ARMOR_TOUGHNESS,TOUGHNESS_CURSE,curseArmor);
        if(!active||!judging(player))return;
        // third curse: the level cannot pass the luck, and max health cannot pass the level (at least 1)
        capExperience(player);
        if(!Protection.active(player)){
            double max=health.getValue();
            double cap=Math.max(1,player.experienceLevel);
            if(max>cap)health.addTransientModifier(new AttributeModifier(HEALTH_CAP,"sage_health_cap",cap/max-1,AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
        if(player.getHealth()>player.getMaxHealth())player.setHealth(player.getMaxHealth());
        player.getFoodData().setFoodLevel(20);player.getFoodData().setSaturation(0);player.getFoodData().setExhaustion(0);
    }
    private static void adjustEquipmentArmor(Player player,Attribute attribute,UUID curse,boolean active) {
        var instance=player.getAttribute(attribute);if(instance==null)return;
        instance.removeModifier(curse);if(!active)return;
        Set<UUID> equipment=new HashSet<>();
        for(var slot:EquipmentSlot.values())for(var modifier:player.getItemBySlot(slot).getAttributeModifiers(slot).get(attribute))equipment.add(modifier.getId());
        CuriosApi.getCuriosInventory(player).ifPresent(inv->{for(var result:inv.findCurios(stack->!stack.isEmpty())){
            var context=result.slotContext();var uuid=CuriosApi.getSlotUuid(context);
            for(var modifier:CuriosApi.getAttributeModifiers(context,uuid,result.stack()).get(attribute))equipment.add(modifier.getId());
        }});
        double raw=instance.getValue(),base=instance.getBaseValue();
        for(var modifier:instance.getModifiers(AttributeModifier.Operation.ADDITION))if(!equipment.contains(modifier.getId()))base+=modifier.getAmount();
        // pseudo alchemical armor keeps half of what it gives
        for(var slot:new EquipmentSlot[]{EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET}){
            var stack=player.getItemBySlot(slot);
            if(!local.primordial.affix.Imbuing.isArmor(stack)||!local.primordial.affix.Imbuing.pseudoTreasure(stack))continue;
            for(var modifier:stack.getAttributeModifiers(slot).get(attribute))
                if(modifier.getOperation()==AttributeModifier.Operation.ADDITION&&instance.getModifier(modifier.getId())!=null)base+=modifier.getAmount()*PSEUDO_ARMOR_SHARE;
        }
        double desired=base;
        for(var modifier:instance.getModifiers(AttributeModifier.Operation.MULTIPLY_BASE))if(!equipment.contains(modifier.getId()))desired+=base*modifier.getAmount();
        for(var modifier:instance.getModifiers(AttributeModifier.Operation.MULTIPLY_TOTAL))if(!equipment.contains(modifier.getId()))desired*=1+modifier.getAmount();
        desired=attribute.sanitizeValue(desired);
        if(raw>0&&Math.abs(raw-desired)>0.000001)instance.addTransientModifier(new AttributeModifier(curse,"sage_equipment_armor",desired/raw-1,AttributeModifier.Operation.MULTIPLY_TOTAL));
    }
}
