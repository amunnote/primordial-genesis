package local.primordial.affix;

import java.util.ArrayList;
import local.primordial.effect.Protection;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;

public final class AffixCombat {
    public static final TagKey<net.minecraft.world.damagesource.DamageType> MAGIC=TagKey.create(Registries.DAMAGE_TYPE,new ResourceLocation("primordial","magic"));
    private static final ThreadLocal<Integer> SECONDARY=ThreadLocal.withInitial(()->0);
    private static final ThreadLocal<Boolean> RUIN_TERRAIN=ThreadLocal.withInitial(()->false);
    private AffixCombat() {}
    public static boolean secondary() {return SECONDARY.get()>0;}
    public static boolean ruinTerrain(){return RUIN_TERRAIN.get();}
    public static void ruinTerrain(Runnable action){RUIN_TERRAIN.set(true);try{action.run();}finally{RUIN_TERRAIN.remove();}}
    public static void secondary(Runnable action) {
        int before=SECONDARY.get();SECONDARY.set(before+1);try{action.run();}finally{if(before==0)SECONDARY.remove();else SECONDARY.set(before);}
    }
    public static AffixState source(DamageSource source) {return source.getEntity() instanceof Mob mob?AffixState.get(mob):null;}
    public static Float piercedArmor(LivingEntity victim,DamageSource source,float amount) {
        if(victim.level().isClientSide||source.is(DamageTypeTags.BYPASSES_ARMOR))return null;
        double pierce=source.getEntity() instanceof LivingEntity living?armorPierce(victim,living):0;
        // Breach (a bond): the stacks the attacker built on this target thin its armor
        double breach=source.getEntity() instanceof LivingEntity living?AffixBonds.breachFactor(victim,living):1;
        if(pierce<=0&&breach>=1)return null;
        ((local.primordial.mixin.AffixLivingAccess)victim).angelAffixHurtArmor(source,amount);
        float armor=(float)(victim.getArmorValue()*(1-pierce)*breach);
        return CombatRules.getDamageAfterAbsorb(amount,armor,(float)victim.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
    }
    /** how much of the victim's armor the attacker ignores: a mob's Armor Pierce affix, or a charm's or the accessory's pierce */
    public static double armorPierce(LivingEntity victim,LivingEntity attacker) {
        var state=attacker instanceof Mob mob?AffixState.get(mob):null;
        if(AffixAccessories.acts(state,MobAffix.ARMOR_PIERCE,victim))return Math.min(100,state.level)/100.0;
        return Math.max(local.primordial.item.AlchemyCharmItem.armorPierce(attacker),
            AffixAccessories.wears(attacker,MobAffix.ARMOR_PIERCE)?Math.min(1,0.25*AffixAccessories.power(attacker)):0);
    }
    public static DamageSource magic(DamageSource source,LivingEntity victim) {
        var type=victim.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.MAGIC);
        return new DamageSource(type,source.getDirectEntity(),source.getEntity());
    }
    public static float outgoing(AffixState attacker,LivingEntity victim,DamageSource source,float amount) {
        if(attacker==null)return amount;
        // Vanilla dragons/projectiles may use hardcoded damage instead of the attack attribute.
        // (the Primordial Lord's spells are tuned as they are: its affix count does not swell them)
        if(!attacker.primordial()&&(attacker.mob.getAttribute(Attributes.ATTACK_DAMAGE)==null||source.getDirectEntity()!=attacker.mob)) {
            amount=(amount+attacker.affixes.size()*3)*(float)(1+attacker.affixes.size()*AffixState.PER_AFFIX);
            if(attacker.tag.getBoolean("pridePhase"))amount*=1.5F;
            if(attacker.boss(BossAffix.WRATH))amount*=1+attacker.tag.getInt("rageSteps")*0.1F;
        }
        if(attacker.boss(BossAffix.PRIDE)&&victim.getHealth()<attacker.mob.getHealth())amount*=2;return amount;
    }
    /**
     * Physical attacks are direct entity hits and non-elemental projectiles. Fire,
     * explosions, magic and environmental damage count as non-physical damage.
     */
    public static boolean physical(DamageSource source) {
        // true and fixed damage are neither: they pass both sides' resistances (AffixDamage)
        return AffixDamage.mitigable(source)&&!source.is(MAGIC)&&!source.is(DamageTypeTags.IS_FIRE)&&!source.is(DamageTypeTags.IS_EXPLOSION)
            &&(source.getDirectEntity() instanceof LivingEntity
                ||source.is(DamageTypeTags.IS_PROJECTILE));
    }
    public static float incoming(AffixState victim,DamageSource source,float amount) {
        if(victim==null)return amount;
        // true and fixed damage pass every resistance
        if(!AffixDamage.mitigable(source))return amount;
        boolean physical=physical(source);
        var hitter=source.getEntity() instanceof LivingEntity living?living:null;
        if(!physical&&AffixAccessories.acts(victim,MobAffix.MAGIC_RESISTANCE,hitter)||physical&&AffixAccessories.acts(victim,MobAffix.PHYSICAL_RESISTANCE,hitter)) {
            double pierce=hitter!=null?local.primordial.item.AlchemyCharmItem.resistancePierce(hitter):0;
            double breach=hitter!=null?AffixBonds.breachFactor(victim.mob,hitter):1;
            amount*=1-AffixScaling.resistancePercent(victim.level)/100.0F*(float)((1-pierce)*breach);
        }
        if(victim.boss(BossAffix.SLOTH))amount*=0.5F;
        if(victim.tag.getBoolean("pridePhase"))amount*=0.5F;
        return amount;
    }
    /** Split the post-mitigation hit among the bearer and every living entity within ten blocks. */
    public static float sharedDamage(AffixState defender,DamageSource source,float amount) {
        // what passes through invulnerability (/kill, the void) is the bearer's own and no one else's
        if(defender==null||source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
            ||!AffixAccessories.acts(defender,MobAffix.SHARED,source.getEntity() instanceof LivingEntity living?living:null)||amount<=0||secondary()
            ||!(defender.mob.level() instanceof ServerLevel level))return amount;
        var bearer=defender.mob;
        var others=level.getEntitiesOfClass(LivingEntity.class,bearer.getBoundingBox().inflate(10),
            other->other!=bearer&&other.isAlive()&&bearer.distanceToSqr(other)<=100);
        if(others.isEmpty())return amount;
        float share=amount/(others.size()+1);
        secondary(()->{for(var other:others)if(other.isAlive())other.hurt(source,share);});
        return share;
    }
    private static boolean roll(AffixState state,int percent) {return state.mob.getRandom().nextInt(100)<percent;}
    private static void curse(AffixState state,LivingEntity target) {
        if(Protection.active(target)||AffixAccessories.blocks(target,state,MobAffix.CURSE)||!roll(state,AffixScaling.curseChance(state.level)))return;
        var candidates=new ArrayList<net.minecraft.world.effect.MobEffect>();
        for(var effect:ForgeRegistries.MOB_EFFECTS)if(effect.getCategory()==MobEffectCategory.HARMFUL)candidates.add(effect);
        if(!candidates.isEmpty())target.addEffect(new MobEffectInstance(candidates.get(state.mob.getRandom().nextInt(candidates.size())),200));
    }
    public static void landed(AffixState attacker,AffixState defender,LivingEntity victim,DamageSource source,float amount) {
        if(amount<=0||secondary())return;
        AffixExtras.landed(attacker,defender,victim,source);
        if(attacker!=null) {
            attacker.combat();if(attacker.has(MobAffix.CURSE))curse(attacker,victim);
            if(victim instanceof Player player){
                if(AffixAccessories.acts(attacker,MobAffix.MALICE,player))AffixStatuses.applyMalice(player);
                if(AffixAccessories.acts(attacker,MobAffix.DESATURATION,player))AffixStatuses.applyDesaturation(player);
            }
            if(AffixAccessories.acts(attacker,MobAffix.SEAL,victim)&&roll(attacker,AffixScaling.sealChance(attacker.level)))ItemSeal.apply(victim);
            if(attacker.boss(BossAffix.GREED)&&victim.level() instanceof ServerLevel level)AffixLedger.get(level).steal(attacker.mob,victim);
            if(attacker.boss(BossAffix.ENVY)&&roll(attacker,30)&&victim.level() instanceof ServerLevel level)AffixLedger.get(level).disable(attacker.mob,victim);
            if(attacker.boss(BossAffix.SLOTH))victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,100));
        }
        if(defender!=null) {
            defender.combat();if(source.getEntity() instanceof LivingEntity attacking) {
                if(defender.has(MobAffix.CURSE))curse(defender,attacking);
                if(AffixAccessories.acts(defender,MobAffix.SLOWNESS,attacking)&&!Protection.active(attacking))attacking.addEffect(
                    new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,100,AffixScaling.slownessTier(defender.level)-1));
                if(AffixAccessories.acts(defender,MobAffix.SEAL,attacking)&&roll(defender,AffixScaling.sealChance(defender.level)))ItemSeal.apply(attacking);
                if(AffixAccessories.acts(defender,MobAffix.REFLECTION,attacking))secondary(()->attacking.hurt(defender.mob.damageSources().thorns(defender.mob),
                    amount*AffixScaling.reflectPercent(defender.level)/100F));
            }
        }
    }
    public static void execute(AffixState attacker,Player player) {
        if(attacker==null||!attacker.boss(BossAffix.LUST)||Protection.active(player)||player.getHealth()>=player.getMaxHealth()*0.2F)return;
        secondary(()->player.hurt(player.damageSources().fellOutOfWorld(),Float.MAX_VALUE));
        if(!player.isAlive())AffixMobActions.smallPlayer(attacker,player);
    }
}
