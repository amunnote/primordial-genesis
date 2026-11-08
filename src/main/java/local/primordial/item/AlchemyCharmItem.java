package local.primordial.item;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.List;
import java.util.UUID;
import java.util.function.ToDoubleFunction;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/** Charms worn in the alchemical charm slot (from Sage's Gift): the basic charm, the physical and arcane attack lines, and the physical and non-physical defense lines. */
public final class AlchemyCharmItem extends Item implements ICurioItem {
    private final double attack,attackPercent,armor,armorPierce;
    private double nonPhysical,resistancePierce,physicalReduction,nonPhysicalReduction,attackSpeed,projectileBonus,drawSpeed,luck,maxHealth;
    private boolean infiniteAmmo,feebleOnHit,ignoreInvulnerability,homing;
    private boolean magic;
    /** the primordial tier's own powers (see their setters) */
    private double echo,hitCap,reflect,knockbackResist;
    private boolean stasis,cleanse,primordial;
    /** the supreme tier's own powers (see their setters) */
    private double insight,allReduction;private int ward;private boolean supreme;
    /** the primordial charms' rarity: gold, above epic */
    public static final Rarity PRIMORDIAL=Rarity.create("ANGEL_CHARM_PRIMORDIAL",ChatFormatting.GOLD);
    /** the supreme charms' rarity, above primordial: aqua */
    public static final Rarity SUPREME=Rarity.create("ANGEL_CHARM_SUPREME",ChatFormatting.AQUA);
    /**
     * @param attack flat attack damage
     * @param attackPercent attack damage multiplier (0.3 = +30%)
     * @param armor flat armor points
     * @param armorPierce share of the target's armor this wearer's attacks ignore (0.5 = 50%)
     */
    public AlchemyCharmItem(Rarity rarity,double attack,double attackPercent,double armor,double armorPierce) {
        super(new Properties().stacksTo(1).rarity(rarity));
        this.attack=attack;this.attackPercent=attackPercent;this.armor=armor;this.armorPierce=armorPierce;
    }
    /** extra non-physical damage dealt (0.3 = +30%) */
    public AlchemyCharmItem nonPhysical(double bonus) {nonPhysical=bonus;return this;}
    /** the wearer's melee and projectile attacks deal magic damage */
    public AlchemyCharmItem magicAttacks() {magic=true;return this;}
    /** share of the target's resistance (resistance affixes, Resistance effect) ignored */
    public AlchemyCharmItem resistancePierce(double share) {resistancePierce=share;return this;}
    /** physical damage taken is reduced by this share (0.3 = 30% less) */
    public AlchemyCharmItem physicalReduction(double share) {physicalReduction=share;return this;}
    /** non-physical damage taken (magic, fire, explosions, environment) is reduced by this share */
    public AlchemyCharmItem nonPhysicalReduction(double share) {nonPhysicalReduction=share;return this;}
    /** attack speed multiplier (0.3 = +30%) */
    public AlchemyCharmItem attackSpeed(double bonus) {attackSpeed=bonus;return this;}
    /** flat luck */
    public AlchemyCharmItem luck(double amount) {luck=amount;return this;}
    /** flat maximum health */
    public AlchemyCharmItem maxHealth(double amount) {maxHealth=amount;return this;}
    /** extra damage dealt by the wearer's projectiles (0.3 = +30%) */
    public AlchemyCharmItem projectileBonus(double bonus) {projectileBonus=bonus;return this;}
    /** bows and crossbows charge this much faster (0.6 = +60%) */
    public AlchemyCharmItem drawSpeed(double bonus) {drawSpeed=bonus;return this;}
    /** bows and crossbows shoot without using up arrows */
    public AlchemyCharmItem infiniteAmmo() {infiniteAmmo=true;return this;}
    /** whoever hurts the wearer becomes feeble */
    public AlchemyCharmItem feebleOnHit() {feebleOnHit=true;return this;}

    /** the wearer's hits ignore the target's invulnerability frames */
    public AlchemyCharmItem ignoreInvulnerability() {ignoreInvulnerability=true;return this;}
    /** a primordial charm: marked as such in its tooltip */
    public AlchemyCharmItem primordial() {primordial=true;return this;}
    /** every physical hit the wearer lands is followed by a magic echo dealing this share of it (0.5 = 50%) */
    public AlchemyCharmItem echo(double share) {echo=share;return this;}
    /** whatever the wearer hits is held still for a moment, as if time stopped */
    public AlchemyCharmItem stasis() {stasis=true;return this;}
    /** no single physical hit takes more than this share of the wearer's maximum health */
    public AlchemyCharmItem hitCap(double share) {hitCap=share;return this;}
    /** knockback resistance (1 = none at all) */
    public AlchemyCharmItem knockbackResist(double amount) {knockbackResist=amount;return this;}
    /** the wearer cannot be given harmful effects */
    public AlchemyCharmItem cleanse() {cleanse=true;return this;}
    /** this share of non-physical harm taken goes back to whoever dealt it, as magic */
    public AlchemyCharmItem reflect(double share) {reflect=share;return this;}
    /** every kind of damage taken is reduced by this share, physical or not */
    public AlchemyCharmItem allReduction(double share) {allReduction=share;return this;}
    /** a supreme charm: marked as such in its tooltip */
    public AlchemyCharmItem supreme() {supreme=true;return this;}
    /** this chance that a hit the wearer lands sees clean through its target: two and a half times the harm */
    public AlchemyCharmItem insight(double chance) {insight=chance;return this;}
    /** a blow that would kill the wearer is refused, once per this many ticks: it is left at 30% health and shielded */
    public AlchemyCharmItem ward(int cooldown) {ward=cooldown;return this;}
    /** the wearer's projectiles home in on the creature they were aimed at, through walls, and hit nothing else */
    public AlchemyCharmItem homing() {homing=true;return this;}

    @Override public boolean canEquip(SlotContext context,ItemStack stack) {
        return local.primordial.Primordial.ALCHEMICAL_SLOT.equals(context.identifier())&&!context.cosmetic();
    }
    @Override public boolean canEquipFromUse(SlotContext context,ItemStack stack) {return true;}
    @Override public Multimap<Attribute,AttributeModifier> getAttributeModifiers(SlotContext context,UUID uuid,ItemStack stack) {
        Multimap<Attribute,AttributeModifier> result=HashMultimap.create();
        if(context.cosmetic())return result;
        String name="primordial."+net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(this).getPath();
        if(attack!=0)result.put(Attributes.ATTACK_DAMAGE,new AttributeModifier(uuid,name+"_attack",attack,AttributeModifier.Operation.ADDITION));
        if(attackPercent!=0)result.put(Attributes.ATTACK_DAMAGE,new AttributeModifier(uuid,name+"_attack_percent",attackPercent,AttributeModifier.Operation.MULTIPLY_TOTAL));
        if(luck!=0)result.put(Attributes.LUCK,new AttributeModifier(uuid,name+"_luck",luck,AttributeModifier.Operation.ADDITION));
        if(maxHealth!=0)result.put(Attributes.MAX_HEALTH,new AttributeModifier(uuid,name+"_max_health",maxHealth,AttributeModifier.Operation.ADDITION));
        if(attackSpeed!=0)result.put(Attributes.ATTACK_SPEED,new AttributeModifier(uuid,name+"_attack_speed",attackSpeed,AttributeModifier.Operation.MULTIPLY_TOTAL));
        if(armor!=0)result.put(Attributes.ARMOR,new AttributeModifier(uuid,name+"_armor",armor,AttributeModifier.Operation.ADDITION));
        if(knockbackResist!=0)result.put(Attributes.KNOCKBACK_RESISTANCE,new AttributeModifier(uuid,name+"_knockback",knockbackResist,AttributeModifier.Operation.ADDITION));
        return result;
    }
    /** taken off, health above the lowered maximum is lost (Curios removes the modifier first) */
    @Override public void onUnequip(SlotContext context,ItemStack replacement,ItemStack stack) {
        if(maxHealth>0&&context.entity().getHealth()>context.entity().getMaxHealth())context.entity().setHealth(context.entity().getMaxHealth());
    }
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> tooltip,TooltipFlag flag) {
        if(supreme)tooltip.add(Component.translatable("tooltip.primordial.charm.supreme").withStyle(ChatFormatting.AQUA,ChatFormatting.ITALIC));
        else if(primordial)tooltip.add(Component.translatable("tooltip.primordial.charm.primordial").withStyle(ChatFormatting.GOLD,ChatFormatting.ITALIC));
        if(insight>0)tooltip.add(Component.translatable("tooltip.primordial.charm.insight",Math.round(insight*100)).withStyle(ChatFormatting.AQUA));
        if(ward>0)tooltip.add(Component.translatable("tooltip.primordial.charm.ward",ward/20).withStyle(ChatFormatting.AQUA));
        if(echo>0)tooltip.add(Component.translatable("tooltip.primordial.charm.echo",Math.round(echo*100)).withStyle(ChatFormatting.LIGHT_PURPLE));
        if(stasis)tooltip.add(Component.translatable("tooltip.primordial.charm.stasis").withStyle(ChatFormatting.AQUA));
        if(hitCap>0)tooltip.add(Component.translatable("tooltip.primordial.charm.hit_cap",Math.round(hitCap*100)).withStyle(ChatFormatting.GOLD));
        if(cleanse)tooltip.add(Component.translatable("tooltip.primordial.charm.cleanse").withStyle(ChatFormatting.AQUA));
        if(reflect>0)tooltip.add(Component.translatable("tooltip.primordial.charm.reflect",Math.round(reflect*100)).withStyle(ChatFormatting.DARK_PURPLE));
        if(projectileBonus>0)tooltip.add(Component.translatable("tooltip.primordial.charm.projectile",Math.round(projectileBonus*100)).withStyle(ChatFormatting.BLUE));
        if(drawSpeed>0)tooltip.add(Component.translatable("tooltip.primordial.charm.draw_speed",Math.round(drawSpeed*100)).withStyle(ChatFormatting.BLUE));
        if(infiniteAmmo)tooltip.add(Component.translatable("tooltip.primordial.charm.infinite_ammo").withStyle(ChatFormatting.GOLD));
        if(homing)tooltip.add(Component.translatable("tooltip.primordial.charm.homing").withStyle(ChatFormatting.GOLD));
        if(ignoreInvulnerability)tooltip.add(Component.translatable("tooltip.primordial.charm.ignore_invulnerability").withStyle(ChatFormatting.GOLD));
        if(feebleOnHit)tooltip.add(Component.translatable("tooltip.primordial.charm.feeble_on_hit").withStyle(ChatFormatting.DARK_GREEN));
        if(nonPhysical>0)tooltip.add(Component.translatable("tooltip.primordial.charm.non_physical",Math.round(nonPhysical*100)).withStyle(ChatFormatting.BLUE));
        if(magic)tooltip.add(Component.translatable("tooltip.primordial.charm.magic_attacks").withStyle(ChatFormatting.LIGHT_PURPLE));
        if(armorPierce>0)tooltip.add(Component.translatable("tooltip.primordial.charm.armor_pierce",Math.round(armorPierce*100)).withStyle(ChatFormatting.DARK_RED));
        if(allReduction>0)tooltip.add(Component.translatable("tooltip.primordial.charm.all_reduction",Math.round(allReduction*100)).withStyle(ChatFormatting.GOLD));
        if(physicalReduction>0)tooltip.add(Component.translatable("tooltip.primordial.charm.physical_reduction",Math.round(physicalReduction*100)).withStyle(ChatFormatting.GOLD));
        if(nonPhysicalReduction>0)tooltip.add(Component.translatable("tooltip.primordial.charm.non_physical_reduction",Math.round(nonPhysicalReduction*100)).withStyle(ChatFormatting.AQUA));
        if(resistancePierce>0)tooltip.add(Component.translatable("tooltip.primordial.charm.resistance_pierce",Math.round(resistancePierce*100)).withStyle(ChatFormatting.DARK_PURPLE));
    }

    /** Largest armor share ignored by charms the attacker wears; 0 if none. */
    public static double armorPierce(LivingEntity attacker) {return best(attacker,charm->charm.armorPierce);}
    public static double nonPhysicalBonus(LivingEntity attacker) {return best(attacker,charm->charm.nonPhysical);}
    public static double resistancePierce(LivingEntity attacker) {return best(attacker,charm->charm.resistancePierce);}
    public static double physicalReduction(LivingEntity wearer) {return best(wearer,charm->charm.physicalReduction);}
    public static double nonPhysicalReduction(LivingEntity wearer) {return best(wearer,charm->charm.nonPhysicalReduction);}
    public static double projectileBonus(LivingEntity wearer) {return best(wearer,charm->charm.projectileBonus);}
    public static double drawSpeed(LivingEntity wearer) {return best(wearer,charm->charm.drawSpeed);}
    public static boolean infiniteAmmo(LivingEntity wearer) {return best(wearer,charm->charm.infiniteAmmo?1:0)>0;}
    public static boolean feebleOnHit(LivingEntity wearer) {return best(wearer,charm->charm.feebleOnHit?1:0)>0;}
    public static boolean ignoresInvulnerability(LivingEntity attacker) {return best(attacker,charm->charm.ignoreInvulnerability?1:0)>0;}
    public static boolean homing(LivingEntity shooter) {return best(shooter,charm->charm.homing?1:0)>0;}
    public static boolean magicAttacks(LivingEntity attacker) {return best(attacker,charm->charm.magic?1:0)>0;}
    public static double echo(LivingEntity attacker) {return best(attacker,charm->charm.echo);}
    public static boolean stasis(LivingEntity attacker) {return best(attacker,charm->charm.stasis?1:0)>0;}
    public static double hitCap(LivingEntity wearer) {return best(wearer,charm->charm.hitCap);}
    public static boolean cleanse(LivingEntity wearer) {return best(wearer,charm->charm.cleanse?1:0)>0;}
    public static double reflect(LivingEntity wearer) {return best(wearer,charm->charm.reflect);}
    public static double insight(LivingEntity attacker) {return best(attacker,charm->charm.insight);}
    public static double allReduction(LivingEntity wearer) {return best(wearer,charm->charm.allReduction);}
    public static int ward(LivingEntity wearer) {return (int)best(wearer,charm->charm.ward);}
    /** the charms whose powers the Ascension Charm carries */
    public static List<AlchemyCharmItem> ascended() {
        return List.of((AlchemyCharmItem)local.primordial.Primordial.CLEAR_WORLD_CHARM.get(),(AlchemyCharmItem)local.primordial.Primordial.INVIOLABLE_CHARM.get());
    }
    /** the attribute bonuses a charm gives (for the Ascension Charm to carry them too) */
    public double attackPercent() {return attackPercent;}
    public double attackSpeedBonus() {return attackSpeed;}
    public double armorBonus() {return armor;}
    public double knockbackBonus() {return knockbackResist;}
    private static double best(LivingEntity wearer,ToDoubleFunction<AlchemyCharmItem> value) {
        if(!(wearer instanceof Player player))return 0;
        return CuriosApi.getCuriosInventory(player).map(inv->{
            double best=0;
            for(var found:inv.findCurios(s->s.getItem() instanceof AlchemyCharmItem))
                if(!found.slotContext().cosmetic())best=Math.max(best,value.applyAsDouble((AlchemyCharmItem)found.stack().getItem()));
            // the Ascension Charm holds every power of both supreme charms
            if(local.primordial.effect.Protection.active(player))for(var supreme:ascended())best=Math.max(best,value.applyAsDouble(supreme));
            return best;
        }).orElse(0D);
    }
}
