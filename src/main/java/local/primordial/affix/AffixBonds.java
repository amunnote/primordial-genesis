package local.primordial.affix;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/**
 * Bonds between affix accessories: a set of affixes worn together (accessories in real slots plus the main-hand weapon's
 * forged affixes, {@link AffixAccessories#worn}) holds a bond for as long as every part is worn; order and slot do not
 * matter and any number of bonds can hold at once. Which bonds hold is an independent status kept and synced by
 * {@link AffixStatuses} (not a MobEffect: milk does not clear it and Sage's Gift does not rescale it); what each does is
 * in {@link local.primordial.effect.AffixBondEvents} and at the accessory effects it touches. Text: bond.primordial.&lt;id&gt;(.desc).
 */
public final class AffixBonds {
    public enum Bond {
        // the six archetypes, four parts each (AffixSynergy's language); 不熄 has three
        THRONG("throng",0xFFE8C860,MobAffix.SPLIT,MobAffix.SUMMON,MobAffix.SHARED,MobAffix.HEALING),
        UNMOVING("unmoving",0xFFC6D2E0,MobAffix.REFLECTION,MobAffix.RESISTANCE,MobAffix.DAMAGE_CAP,MobAffix.PHYSICAL_RESISTANCE),
        BREACH("breach",0xFFFF7A50,MobAffix.STRENGTH,MobAffix.ARMOR_PIERCE,MobAffix.SHATTER,MobAffix.SPACE),
        BINDING("binding",0xFFA070F0,MobAffix.MAGIC,MobAffix.CURSE,MobAffix.SEAL,MobAffix.MASTERY),
        HEAVENBIND("heavenbind",0xFF88C8FF,MobAffix.SLOWNESS,MobAffix.THUNDER,MobAffix.BEFUDDLEMENT,MobAffix.DESATURATION),
        NAMING("naming",0xFFC050E8,MobAffix.MALICE,MobAffix.BLOOD_PACT,MobAffix.AMPLIFY,MobAffix.ENLIGHTENMENT),
        UNQUENCHED("unquenched",0xFFFFB040,MobAffix.TIME,MobAffix.GROWTH,MobAffix.RUIN),
        // three-part crossovers
        AEGIS("aegis",0xFFEBCB69,MobAffix.RESISTANCE,MobAffix.PHYSICAL_RESISTANCE,MobAffix.MAGIC_RESISTANCE),
        WHEEL("wheel",0xFF70E0D0,MobAffix.SPACE,MobAffix.TIME,MobAffix.CAUSALITY),
        FADED_WORLD("faded_world",0xFFB8B8C0,MobAffix.DESATURATION,MobAffix.VOID,MobAffix.BEFUDDLEMENT),
        DEFIANCE("defiance",0xFFFF5555,MobAffix.STRENGTH,MobAffix.GROWTH,MobAffix.RUIN),
        // pairs
        SPACETIME("spacetime",0xFF9B8CFF,MobAffix.TIME,MobAffix.SPACE),
        KARMA("karma",0xFFD08CF0,MobAffix.CAUSALITY,MobAffix.CURSE),
        BLOOD_FOR_BLOOD("blood_for_blood",0xFFD03040,MobAffix.REFLECTION,MobAffix.BLOOD_PACT),
        LONGEVITY("longevity",0xFF63E68B,MobAffix.UNDYING,MobAffix.REGENERATION),
        EMPTY_GATE("empty_gate",0xFF8070C0,MobAffix.DAMAGE_CAP,MobAffix.VOID),
        THUNDERCLAP("thunderclap",0xFFFBE276,MobAffix.THUNDER,MobAffix.ARMOR_PIERCE),
        EMBERS("embers",0xFFDF4545,MobAffix.SPLIT,MobAffix.RUIN),
        SPELL_BODY("spell_body",0xFF789AFF,MobAffix.MAGIC,MobAffix.MAGIC_RESISTANCE),
        AWAKENING("awakening",0xFF7FE0FF,MobAffix.SUMMON,MobAffix.ENLIGHTENMENT);

        private final String id;
        private final int color;
        private final Set<MobAffix> parts;
        Bond(String id,int color,MobAffix... parts) {this.id=id;this.color=color;this.parts=EnumSet.of(parts[0],parts);}
        public String id() {return id;}
        /** ARGB */
        public int color() {return color;}
        public Set<MobAffix> parts() {return parts;}
        public int bit() {return 1<<ordinal();}
        public boolean complete(Set<MobAffix> worn) {return worn.containsAll(parts);}
        public int count(Set<MobAffix> worn) {int n=0;for(var part:parts)if(worn.contains(part))n++;return n;}
        public MutableComponent label() {return Component.translatable("bond.primordial."+id);}
        public MutableComponent description() {return Component.translatable("bond.primordial."+id+".desc");}
        public static Bond byId(String id) {for(var bond:values())if(bond.id.equals(id))return bond;return null;}
    }
    private AffixBonds() {}

    public static int mask(Set<MobAffix> worn) {int mask=0;for(var bond:Bond.values())if(bond.complete(worn))mask|=bond.bit();return mask;}
    public static List<Bond> of(int mask) {var list=new ArrayList<Bond>();for(var bond:Bond.values())if((mask&bond.bit())!=0)list.add(bond);return list;}
    /** the bonds this affix's accessory is a part of */
    public static List<Bond> containing(MobAffix affix) {var list=new ArrayList<Bond>();for(var bond:Bond.values())if(bond.parts.contains(affix))list.add(bond);return list;}
    /** does the bond hold for this creature (only players wear accessories); read from the kept status, refreshed every tick */
    public static boolean active(LivingEntity entity,Bond bond) {return entity instanceof Player player&&(AffixStatuses.bonds(player)&bond.bit())!=0;}

    // ------------------------------------------------------------------ shared numbers
    /** Time's accessory shortens the wearer's affix accessory cooldowns (not charms', not bonds') */
    public static final double TIME_COOLDOWN=0.7;
    public static double cooldownFactor(LivingEntity wearer) {return AffixAccessories.wears(wearer,MobAffix.TIME)?TIME_COOLDOWN:1;}
    /** 唤名: the wearer's aura-like effects reach two blocks further */
    public static double auraBonus(LivingEntity wearer) {return active(wearer,Bond.NAMING)?2:0;}
    /**
     * How long a harmful effect or status lasts on this player: Time's accessory halves it, then each bond's own factor
     * multiplies in (万御 x0.7, 业报 x0.6). Magic Resistance's 30% immunity is rolled apart from this (at Applicable).
     */
    public static double negativeFactor(Player player) {
        double factor=AffixAccessories.wears(player,MobAffix.TIME)?0.5:1;
        if(active(player,Bond.AEGIS))factor*=0.7;
        if(active(player,Bond.KARMA))factor*=0.6;
        return factor;
    }
    public static int negativeTicks(Player player,int ticks) {
        double factor=negativeFactor(player);
        return factor>=1?ticks:(int)Math.max(1,Math.round(ticks*factor));
    }
    private static final ThreadLocal<Boolean> PASSING=ThreadLocal.withInitial(()->false);
    /**
     * Called on every effect added to a living entity (SageLivingMixin, after Sage's Gift rescales it): a harmful effect on a
     * player is shortened by {@link #negativeFactor}; with 业报 the time cut away goes to whoever last hurt the player as a curse.
     */
    public static MobEffectInstance effect(LivingEntity owner,MobEffectInstance original) {
        if(owner.level().isClientSide||!(owner instanceof Player player)||original.isInfiniteDuration()
            ||original.getEffect().getCategory()!=MobEffectCategory.HARMFUL)return original;
        double factor=negativeFactor(player);if(factor>=1)return original;
        int ticks=(int)Math.max(1,Math.round(original.getDuration()*factor));
        var copy=new MobEffectInstance(original);
        ((local.primordial.mixin.SageEffectAccess)(Object)copy).sageDuration(ticks);
        if(active(player,Bond.KARMA)&&!PASSING.get()){
            int cut=original.getDuration()-ticks;var attacker=player.getLastHurtByMob();
            if(cut>0&&attacker!=null&&attacker!=player&&attacker.isAlive()){
                PASSING.set(true);
                try{attacker.addEffect(new MobEffectInstance(local.primordial.effect.ArmorImbueEvents.randomHarm(player),cut,0),player);}
                finally{PASSING.set(false);}
            }
        }
        return copy;
    }

    // ------------------------------------------------------------------ 破阵: Breach stacks, kept on the target
    private static final String BREACH_OWNER="primordial:bond_breach_owner",BREACH_STACKS="primordial:bond_breach_stacks",
        BREACH_TIME="primordial:bond_breach_time",BREACH_FREE="primordial:bond_breach_free",BREACH_TARGET="primordial:bond_breach_target";
    public static final int BREACH_MAX=5,BREACH_KEEP=100;
    public static final double BREACH_PER_STACK=0.03;
    public static int breachStacks(LivingEntity victim,LivingEntity attacker) {
        if(!(attacker instanceof Player player)||!active(player,Bond.BREACH))return 0;
        var data=victim.getPersistentData();
        if(!data.hasUUID(BREACH_OWNER)||!data.getUUID(BREACH_OWNER).equals(player.getUUID()))return 0;
        if(!player.getPersistentData().hasUUID(BREACH_TARGET)||!player.getPersistentData().getUUID(BREACH_TARGET).equals(victim.getUUID()))return 0;
        if(victim.level().getGameTime()-data.getLong(BREACH_TIME)>BREACH_KEEP)return 0;
        return data.getInt(BREACH_STACKS);
    }
    /** what is left of the victim's armor and resistances against this attacker's Breach (1 = none) */
    public static double breachFactor(LivingEntity victim,LivingEntity attacker) {
        int stacks=breachStacks(victim,attacker);return stacks<=0?1:1-BREACH_PER_STACK*stacks;
    }
    /** at the start of a blow (LivingAttackEvent): with Breach full, this blow passes the target's damage caps */
    public static void armBreach(Player player,LivingEntity victim) {
        if(breachStacks(victim,player)>=BREACH_MAX)victim.getPersistentData().putLong(BREACH_FREE,victim.level().getGameTime());
    }
    /** the blow being settled passes the victim's per-hit damage caps (full Breach) */
    public static boolean uncapped(LivingEntity victim,DamageSource source) {
        var data=victim.getPersistentData();
        return data.contains(BREACH_FREE)&&data.getLong(BREACH_FREE)==victim.level().getGameTime()&&source.getEntity()!=null
            &&data.hasUUID(BREACH_OWNER)&&data.getUUID(BREACH_OWNER).equals(source.getEntity().getUUID());
    }
    /** after a blow landed: one more stack on this target (a new target starts over); a blow that passed the cap spends them */
    public static void breachHit(Player player,LivingEntity victim) {
        var data=victim.getPersistentData();long now=victim.level().getGameTime();
        int stacks=breachStacks(victim,player);
        if(data.contains(BREACH_FREE)&&data.getLong(BREACH_FREE)==now){data.remove(BREACH_FREE);stacks=0;}
        else stacks=Math.min(BREACH_MAX,stacks+1);
        data.putUUID(BREACH_OWNER,player.getUUID());data.putInt(BREACH_STACKS,stacks);data.putLong(BREACH_TIME,now);
        player.getPersistentData().putUUID(BREACH_TARGET,victim.getUUID());
    }

    // ------------------------------------------------------------------ marks left on targets
    private static final String CURSED_UNTIL="primordial:bond_cursed_until",SLOWED_BY="primordial:bond_slowed_by",SLOWED_UNTIL="primordial:bond_slowed_until";
    /** 缚咒: the target carries the wearer's curse this long */
    public static void markCursed(LivingEntity victim,int ticks) {
        victim.getPersistentData().putLong(CURSED_UNTIL,Math.max(victim.getPersistentData().getLong(CURSED_UNTIL),victim.level().getGameTime()+ticks));
    }
    public static boolean cursed(LivingEntity victim) {return victim.level().getGameTime()<victim.getPersistentData().getLong(CURSED_UNTIL);}
    /** 天缚: this wearer slowed the target, for as long as the slowness it gave lasts */
    public static void markSlowed(LivingEntity victim,Player by,int ticks) {
        victim.getPersistentData().putUUID(SLOWED_BY,by.getUUID());victim.getPersistentData().putLong(SLOWED_UNTIL,victim.level().getGameTime()+ticks);
    }
    public static boolean slowedBy(LivingEntity victim,Player by) {
        var data=victim.getPersistentData();
        return data.hasUUID(SLOWED_BY)&&data.getUUID(SLOWED_BY).equals(by.getUUID())&&victim.level().getGameTime()<data.getLong(SLOWED_UNTIL);
    }

    // ------------------------------------------------------------------ 霹雳: armor shred layers
    private static final String SHRED_STACKS="primordial:bond_shred_stacks",SHRED_UNTIL="primordial:bond_shred_until";
    private static final UUID SHRED=UUID.fromString("5e2b7c1a-9f34-4d8e-a1b6-0c7d2e9f4a21");
    public static final int SHRED_MAX=5,SHRED_TICKS=100;
    public static final double SHRED_PER_LAYER=0.06;
    /** layers of 破甲 (each -6% armor, up to five, five seconds from the last) */
    public static void shred(LivingEntity victim,int layers) {
        var data=victim.getPersistentData();
        int stacks=Math.min(SHRED_MAX,(victim.level().getGameTime()<data.getLong(SHRED_UNTIL)?data.getInt(SHRED_STACKS):0)+layers);
        data.putInt(SHRED_STACKS,stacks);data.putLong(SHRED_UNTIL,victim.level().getGameTime()+SHRED_TICKS);
        var armor=victim.getAttribute(Attributes.ARMOR);if(armor==null)return;
        armor.removeModifier(SHRED);
        armor.addTransientModifier(new AttributeModifier(SHRED,"primordial.bond_shred",-SHRED_PER_LAYER*stacks,AttributeModifier.Operation.MULTIPLY_TOTAL));
    }
    public static void shredExpires(LivingEntity victim) {
        var data=victim.getPersistentData();
        if(!data.contains(SHRED_UNTIL)||victim.level().getGameTime()<data.getLong(SHRED_UNTIL))return;
        var armor=victim.getAttribute(Attributes.ARMOR);if(armor!=null)armor.removeModifier(SHRED);
        data.remove(SHRED_UNTIL);data.remove(SHRED_STACKS);
    }
}
