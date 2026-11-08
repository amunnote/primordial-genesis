package local.primordial.affix;

import local.primordial.Primordial;
import local.primordial.effect.ArmorImbueEvents;
import local.primordial.item.AlchemyCharmItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * The three damage kinds beside ordinary damage (the author's definitions):
 * <ul>
 * <li>adaptive: each hit is settled against whichever of the target's physical or non-physical side lets more through
 * (only the physical and magic parts are turned; fire, explosions and the like stay as they are). Causality's accessory
 * makes all its wearer's damage adaptive ({@link #adapt}, called from AffixArmorMixin at the head of hurt()).</li>
 * <li>true ({@code primordial:true_damage}): passes armor and every resistance and reduction, but per-hit damage caps
 * still hold it ({@link #mitigable} false, {@link #cappable} true).</li>
 * <li>fixed ({@code primordial:fixed_damage}): the number is the result. No bonus, reduction, crit or cap; only a dodge
 * (the hit cancelled at LivingAttackEvent) avoids it, and death rescues still save from it. Nothing deals it yet: only
 * the definition and its settlement ({@link #dealFixed}) are in place.</li>
 * </ul>
 * Both new types are tagged bypasses_armor / bypasses_effects / bypasses_enchantments / bypasses_shield in data, so the
 * vanilla reductions skip them; the mod's own reductions ask {@link #mitigable} and its caps {@link #cappable}.
 */
@Mod.EventBusSubscriber(modid=Primordial.ID)
public final class AffixDamage {
    public enum Kind {NORMAL,ADAPTIVE,TRUE,FIXED}
    public static final ResourceKey<DamageType> TRUE_DAMAGE=ResourceKey.create(Registries.DAMAGE_TYPE,new ResourceLocation(Primordial.ID,"true_damage"));
    public static final ResourceKey<DamageType> FIXED_DAMAGE=ResourceKey.create(Registries.DAMAGE_TYPE,new ResourceLocation(Primordial.ID,"fixed_damage"));
    /** the fixed hit being dealt right now, and the victim's absorption when its hurt event ran */
    private static final ThreadLocal<float[]> FIXED=new ThreadLocal<>();
    private AffixDamage() {}

    public static boolean isTrue(DamageSource source) {return source.is(TRUE_DAMAGE);}
    public static boolean isFixed(DamageSource source) {return source.is(FIXED_DAMAGE);}
    /** armor, resistances and damage reductions apply (not to true or fixed damage) */
    public static boolean mitigable(DamageSource source) {return !isTrue(source)&&!isFixed(source);}
    /** per-hit damage caps apply: true damage is still capped, fixed damage is not */
    public static boolean cappable(DamageSource source) {return !isFixed(source);}
    /** bonuses, multipliers and crits apply (not to fixed damage) */
    public static boolean scalable(DamageSource source) {return !isFixed(source);}
    public static Kind kind(DamageSource source) {
        if(isFixed(source))return Kind.FIXED;
        if(isTrue(source))return Kind.TRUE;
        return source.getEntity() instanceof Player player&&AffixAccessories.wears(player,MobAffix.CAUSALITY)?Kind.ADAPTIVE:Kind.NORMAL;
    }

    public static DamageSource source(LivingEntity victim,ResourceKey<DamageType> key,Entity attacker) {
        var type=victim.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(key);
        return new DamageSource(type,attacker,attacker);
    }
    /** a true-damage hit on top of one that just landed (it would be swallowed by the damage immunity otherwise) */
    public static void dealTrue(LivingEntity victim,LivingEntity attacker,float amount) {
        if(amount<=0||!victim.isAlive())return;
        int frames=victim.invulnerableTime;victim.invulnerableTime=0;
        AffixCombat.secondary(()->victim.hurt(source(victim,TRUE_DAMAGE,attacker),amount));
        victim.invulnerableTime=Math.max(victim.invulnerableTime,frames);
    }
    /** a fixed-damage hit: exactly this much, unless dodged; returns whether it landed */
    public static boolean dealFixed(LivingEntity victim,Entity attacker,float amount) {
        if(amount<=0||!victim.isAlive())return false;
        var before=FIXED.get();boolean[] landed={false};
        FIXED.set(new float[]{amount,0});
        try{AffixCombat.secondary(()->landed[0]=victim.hurt(source(victim,FIXED_DAMAGE,attacker),amount));}
        finally{if(before==null)FIXED.remove();else FIXED.set(before);}
        return landed[0];
    }
    /** after every other listener: a fixed hit is put back to its number */
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void fixedHurt(LivingHurtEvent event) {
        var fixed=FIXED.get();
        if(fixed==null||!isFixed(event.getSource()))return;
        event.setAmount(fixed[0]);fixed[1]=event.getEntity().getAbsorptionAmount();
    }
    /** absorption hearts are health, not a reduction: what they took is not given back */
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void fixedDamage(LivingDamageEvent event) {
        var fixed=FIXED.get();
        if(fixed==null||!isFixed(event.getSource()))return;
        float absorbed=Math.max(0,fixed[1]-event.getEntity().getAbsorptionAmount());
        event.setAmount(Math.max(0,fixed[0]-absorbed));
    }

    // ------------------------------------------------------------------ adaptive
    /** Causality: a physical hit the target's non-physical side would let more of through becomes magic, and the other way round. */
    public static DamageSource adapt(DamageSource source,LivingEntity victim) {
        if(victim.level().isClientSide||!(source.getEntity() instanceof Player player)||player==victim||!mitigable(source)
            ||source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)||!AffixAccessories.wears(player,MobAffix.CAUSALITY))return source;
        boolean magic=source.is(AffixCombat.MAGIC),physical=AffixCombat.physical(source);
        if(!magic&&!physical)return source;
        double throughPhysical=physicalTaken(victim,player),throughOther=nonPhysicalTaken(victim,player);
        if(physical&&throughOther>throughPhysical)return AffixCombat.magic(source,victim);
        if(magic&&throughPhysical>throughOther)return physicalSource(source,victim);
        return source;
    }
    private static DamageSource physicalSource(DamageSource source,LivingEntity victim) {
        var types=victim.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
        var direct=source.getDirectEntity();
        // a shot stays a shot (is_projectile), anything else is a blow
        var type=types.getHolderOrThrow(direct instanceof Projectile?DamageTypes.ARROW:DamageTypes.PLAYER_ATTACK);
        return new DamageSource(type,direct!=null?direct:source.getEntity(),source.getEntity());
    }
    /** the share of a physical hit from this attacker that gets through: armor (after pierce and Breach) and physical resistances */
    public static double physicalTaken(LivingEntity victim,LivingEntity attacker) {
        float reference=(float)Math.max(1,attacker.getAttribute(Attributes.ATTACK_DAMAGE)!=null?attacker.getAttributeValue(Attributes.ATTACK_DAMAGE):1);
        float armor=(float)(victim.getArmorValue()*(1-AffixCombat.armorPierce(victim,attacker))*AffixBonds.breachFactor(victim,attacker));
        double taken=CombatRules.getDamageAfterAbsorb(reference,armor,(float)victim.getAttributeValue(Attributes.ARMOR_TOUGHNESS))/reference;
        return taken*resistanceTaken(victim,attacker,true);
    }
    /** the share of a non-physical hit from this attacker that gets through its resistances */
    public static double nonPhysicalTaken(LivingEntity victim,LivingEntity attacker) {return resistanceTaken(victim,attacker,false);}
    /** the resistances that depend on the kind of hit (the ones that take any hit alike do not decide anything) */
    private static double resistanceTaken(LivingEntity victim,LivingEntity attacker,boolean physical) {
        double taken=1;
        if(victim instanceof Mob mob){
            var state=AffixState.get(mob);
            if(AffixAccessories.acts(state,physical?MobAffix.PHYSICAL_RESISTANCE:MobAffix.MAGIC_RESISTANCE,attacker))
                taken*=1-AffixScaling.resistancePercent(state.level)/100.0*(1-AlchemyCharmItem.resistancePierce(attacker))*AffixBonds.breachFactor(victim,attacker);
        }
        if(victim instanceof Player player){
            var worn=AffixAccessories.worn(player);double power=AffixAccessories.power(player);
            if(physical&&worn.contains(MobAffix.PHYSICAL_RESISTANCE))taken*=1-0.22*power;
            if(!physical&&worn.contains(MobAffix.MAGIC_RESISTANCE))taken*=1-0.20*power;
            int n=ArmorImbueEvents.pieces(player,physical?MobAffix.PHYSICAL_RESISTANCE:MobAffix.MAGIC_RESISTANCE);
            if(n>0)taken*=1-(physical?0.08:0.07)*n;
            taken*=1-Math.max(AlchemyCharmItem.allReduction(player),physical?AlchemyCharmItem.physicalReduction(player):AlchemyCharmItem.nonPhysicalReduction(player));
        }
        return taken;
    }
}
