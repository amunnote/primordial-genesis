package local.primordial.affix;

import java.util.EnumMap;
import java.util.Map;
import local.primordial.Primordial;
import local.primordial.alchemy.AlchemyKnowledge;
import local.primordial.item.AffixAccessoryItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * One alchemical accessory per ordinary affix, made on a transmutation altar from that affix's material. Each has an
 * effect of its own and wards its wearer against the same affix on monsters ({@link #blocks}).
 */
public final class AffixAccessories {
    public static final Map<MobAffix,RegistryObject<Item>> ITEMS=new EnumMap<>(MobAffix.class);
    /** luck needed to wear one */
    public static final double LUCK_REQUIRED=20;
    /** Amplify's accessory raises the numbers of the wearer's other accessories by this much */
    public static final double AMPLIFY=0.4;
    private AffixAccessories() {}

    public static String id(MobAffix affix) {return affix.id()+"_accessory";}
    public static ResourceLocation recipe(MobAffix affix) {return new ResourceLocation(Primordial.ID,id(affix));}
    public static void register(DeferredRegister<Item> items) {
        for(var affix:MobAffix.values()){
            ITEMS.put(affix,items.register(id(affix),()->new AffixAccessoryItem(affix)));
            AlchemyKnowledge.registerAffixAccessory(recipe(affix));
        }
    }

    /** does this creature wear the accessory of this affix (in a real, not cosmetic, slot), or hold a weapon forged with it? */
    public static boolean wears(LivingEntity entity,MobAffix affix) {
        if(!(entity instanceof Player player))return false;
        if(Imbuing.heldWeaponOrdinary(player.getMainHandItem()).contains(affix))return true;
        return CuriosApi.getCuriosInventory(player).map(inventory->{
            for(var found:inventory.findCurios(stack->stack.getItem() instanceof AffixAccessoryItem item&&item.affix()==affix))
                if(!found.slotContext().cosmetic())return true;
            return false;
        }).orElse(false);
    }
    /** every affix whose accessory this creature wears, plus those forged into the weapon in its main hand */
    public static java.util.EnumSet<MobAffix> worn(LivingEntity entity) {
        var worn=java.util.EnumSet.noneOf(MobAffix.class);
        if(entity instanceof Player player)worn.addAll(Imbuing.heldWeaponOrdinary(player.getMainHandItem()));
        if(entity instanceof Player player)CuriosApi.getCuriosInventory(player).ifPresent(inventory->{
            for(var found:inventory.findCurios(stack->stack.getItem() instanceof AffixAccessoryItem))
                if(!found.slotContext().cosmetic())worn.add(((AffixAccessoryItem)found.stack().getItem()).affix());
        });
        return worn;
    }
    /** Amplification comes only from a real alchemical accessory slot. */
    public static double power(LivingEntity entity) {
        if(!(entity instanceof Player player))return 1;
        return CuriosApi.getCuriosInventory(player).map(inventory->{
            for(var found:inventory.findCurios(stack->stack.getItem() instanceof AffixAccessoryItem item&&item.affix()==MobAffix.AMPLIFY))
                if(!found.slotContext().cosmetic()&&Primordial.ALCHEMICAL_ACCESSORY_SLOT.equals(found.slotContext().identifier()))return 1+AMPLIFY;
            return 1.0;
        }).orElse(1.0);
    }

    /**
     * Whether a monster's affix is ignored when it acts on this creature: the creature wears that affix's accessory, or
     * the affix is only lent by Amplify and it wears Amplify's accessory.
     */
    public static boolean blocks(LivingEntity entity,AffixState state,MobAffix affix) {
        if(!(entity instanceof Player))return false;
        return wears(entity,affix)||state!=null&&state.temporary.contains(affix)&&wears(entity,MobAffix.AMPLIFY);
    }
    /** a mob's affix acts on it unless the creature is warded against it */
    public static boolean acts(AffixState state,MobAffix affix,LivingEntity on) {return state!=null&&state.has(affix)&&!blocks(on,state,affix);}
    /** the mob's affix is ignored because the player it is fighting (its target, or whoever last hurt it) is warded */
    public static boolean foeBlocks(Mob mob,AffixState state,MobAffix affix) {
        return blocks(mob.getTarget(),state,affix)||blocks(mob.getLastHurtByMob(),state,affix);
    }
}
