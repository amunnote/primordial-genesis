package local.primordial.affix;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import local.primordial.Primordial;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Affix materials forged into a weapon or a piece of armor on an altar: it becomes a pseudo alchemical treasure (伪-炼金宝具).
 * A weapon held in the main hand: an ordinary affix works as if its accessory were worn (effect and ward alike); a boss
 * affix gives it a power of its own (see ImbueEvents). Armor worn: every affix material gives a power of its own, but
 * no ward against the affix (see ArmorImbueEvents). Either takes {@link #MAX} affix.
 */
public final class Imbuing {
    public static final String KEY="primordial:imbued";
    public static final int MAX=1;
    public static final String BOSS_PREFIX="boss/";
    /** weapons that do not look like weapons can be added to this tag; see {@link #isWeapon} */
    public static final TagKey<Item> IMBUABLE=TagKey.create(Registries.ITEM,new ResourceLocation(Primordial.ID,"imbuable"));
    private Imbuing() {}

    /** the affix a material carries: "armor_pierce", "boss/pride"; null if it is no affix material */
    public static String affixOf(ItemStack material) {
        for(var e:AffixMaterials.ITEMS.entrySet())if(material.is(e.getValue().get()))return e.getKey().id();
        for(var e:AffixMaterials.BOSS_ITEMS.entrySet())if(material.is(e.getValue().get()))return BOSS_PREFIX+e.getKey().id();
        return null;
    }
    public static List<String> imbues(ItemStack weapon) {
        var out=new ArrayList<String>();
        if(weapon.hasTag()&&weapon.getTag().contains(KEY,Tag.TAG_LIST))for(var t:weapon.getTag().getList(KEY,Tag.TAG_STRING))out.add(t.getAsString());
        return out;
    }
    public static boolean pseudoTreasure(ItemStack stack) {return !imbues(stack).isEmpty();}
    /**
     * Whether an item is a weapon that can take an affix: anything in the imbuable tag; swords, axes, tridents, bows and
     * crossbows (and anything built on them); whatever can sweep like a sword; and, from other mods, any single item that
     * adds attack damage in the main hand, except digging tools and armor.
     */
    public static boolean isWeapon(ItemStack stack) {
        if(stack.isEmpty()||stack.getMaxStackSize()>1)return false;
        if(stack.is(IMBUABLE))return true;
        var item=stack.getItem();
        if(item instanceof net.minecraft.world.item.SwordItem||item instanceof net.minecraft.world.item.AxeItem
            ||item instanceof net.minecraft.world.item.TridentItem||item instanceof net.minecraft.world.item.ProjectileWeaponItem)return true;
        if(stack.canPerformAction(net.minecraftforge.common.ToolActions.SWORD_SWEEP))return true;
        if(item instanceof net.minecraft.world.item.DiggerItem||item instanceof net.minecraft.world.item.ArmorItem)return false;
        try{
            for(var modifier:stack.getAttributeModifiers(net.minecraft.world.entity.EquipmentSlot.MAINHAND).get(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE))
                if(modifier.getAmount()>0)return true;
        }catch(RuntimeException ignored){
            // an item from another mod that cannot report its attributes outside a real holder: not a weapon here
        }
        return false;
    }
    /** a piece of armor worn in one of the four armor slots */
    public static boolean isArmor(ItemStack stack) {
        if(stack.isEmpty()||stack.getMaxStackSize()>1)return false;
        return stack.getItem() instanceof net.minecraft.world.item.ArmorItem armor&&armor.getEquipmentSlot().getType()==net.minecraft.world.entity.EquipmentSlot.Type.ARMOR;
    }
    /**
     * The ordinary affixes of a main-hand item, if it is a weapon: armor held in the hand gives nothing. Only weapons and
     * armor are ever forged, so whatever is forged and is no armor is a weapon; this does not ask {@link #isWeapon}, which
     * reads the item's attribute modifiers and so would call back into the attribute event that asks this.
     */
    public static EnumSet<MobAffix> heldWeaponOrdinary(ItemStack stack) {return isArmor(stack)?EnumSet.noneOf(MobAffix.class):ordinary(stack);}
    public static EnumSet<BossAffix> heldWeaponBoss(ItemStack stack) {return isArmor(stack)?EnumSet.noneOf(BossAffix.class):boss(stack);}
    /** a forged weapon (anything forged but armor) */
    public static boolean pseudoWeapon(ItemStack stack) {return pseudoTreasure(stack)&&!isArmor(stack);}
    public static boolean canImbue(ItemStack weapon,String affix) {
        if(affix==null||!(isWeapon(weapon)||isArmor(weapon)))return false;
        var have=imbues(weapon);
        return have.size()<MAX&&!have.contains(affix);
    }
    public static ItemStack imbue(ItemStack weapon,String affix) {
        var out=weapon.copyWithCount(1);
        var list=out.getOrCreateTag().getList(KEY,Tag.TAG_STRING);
        list.add(StringTag.valueOf(affix));
        out.getTag().put(KEY,list);
        return out;
    }
    public static EnumSet<MobAffix> ordinary(ItemStack weapon) {
        var out=EnumSet.noneOf(MobAffix.class);
        for(var id:imbues(weapon)){var affix=MobAffix.byId(id);if(affix!=null)out.add(affix);}
        return out;
    }
    public static EnumSet<BossAffix> boss(ItemStack weapon) {
        var out=EnumSet.noneOf(BossAffix.class);
        for(var id:imbues(weapon))if(id.startsWith(BOSS_PREFIX)){var affix=BossAffix.byId(id.substring(BOSS_PREFIX.length()));if(affix!=null)out.add(affix);}
        return out;
    }
}
