package local.primordial.affix;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static local.primordial.affix.MobAffix.*;

/**
 * Which ordinary affixes work together. When a mob's affixes are rolled, each one already chosen makes its partners
 * likelier ({@link #WEIGHT} per partner), so mobs come out as builds rather than loose collections: a splitter that
 * shares its harm and comes back, a summoner its helpers heal and keep alive, a reflector that will not go down, a
 * striker that closes in and breaks through, a slower that calls down thunder, a caller of crowds that turns the whole
 * area against you, a blast that keeps coming back.
 */
public final class AffixSynergy {
    /** the weight each chosen partner adds to an affix (the base weight is 1) */
    public static final int WEIGHT=3;
    private static final Map<MobAffix,EnumSet<MobAffix>> PARTNERS=new EnumMap<>(MobAffix.class);
    static {
        for(var affix:MobAffix.values())PARTNERS.put(affix,EnumSet.noneOf(MobAffix.class));
        // the many: split, summon, and what shares, heals and keeps them alive
        pair(SPLIT,SHARED,TIME,RUIN,REGENERATION);
        pair(SUMMON,HEALING,UNDYING,AMPLIFY,ENLIGHTENMENT,SHARED,BLOOD_PACT);
        pair(HEALING,UNDYING,SHARED,AMPLIFY,BLOOD_PACT);
        pair(SHARED,REGENERATION,BLOOD_PACT);
        // the wall: it gives back what it takes, and will not fall
        pair(REFLECTION,RESISTANCE,DAMAGE_CAP,REGENERATION,PHYSICAL_RESISTANCE,VOID);
        pair(DAMAGE_CAP,REGENERATION,TIME,RESISTANCE,GROWTH);
        pair(REGENERATION,GROWTH,RESISTANCE);
        pair(MAGIC_RESISTANCE,VOID,RESISTANCE);
        // the striker: it reaches you and breaks through
        pair(STRENGTH,ARMOR_PIERCE,SHATTER,SPACE,GROWTH);
        pair(ARMOR_PIERCE,SHATTER,SPACE);
        pair(SPACE,SLOWNESS,BEFUDDLEMENT,VOID,RUIN);
        // the marksman and the caster
        pair(CAUSALITY,MASTERY,CURSE,SLOWNESS);
        pair(MASTERY,MAGIC,SLOWNESS);
        pair(MAGIC,CURSE,SEAL);
        // the binder: slowed, sealed, cursed, befuddled, and then the thunder
        pair(SLOWNESS,THUNDER);
        pair(THUNDER,SEAL,BEFUDDLEMENT);
        pair(SEAL,SHATTER,BEFUDDLEMENT);
        pair(CURSE,DESATURATION,BEFUDDLEMENT);
        pair(DESATURATION,BEFUDDLEMENT,VOID);
        // the caller: the whole area turned against you, and made smarter and stronger
        pair(MALICE,BLOOD_PACT,AMPLIFY,ENLIGHTENMENT,SUMMON);
        pair(BLOOD_PACT,AMPLIFY,UNDYING,ENLIGHTENMENT);
        pair(UNDYING,AMPLIFY,RUIN);
        // what keeps coming back
        pair(TIME,GROWTH,RUIN);
    }
    private AffixSynergy() {}
    /** the first works with each of the rest (and they with it) */
    private static void pair(MobAffix first,MobAffix... rest) {
        for(var other:rest){PARTNERS.get(first).add(other);PARTNERS.get(other).add(first);}
    }
    public static Set<MobAffix> partners(MobAffix affix) {return java.util.Collections.unmodifiableSet(PARTNERS.get(affix));}
    public static boolean works(MobAffix a,MobAffix b) {return PARTNERS.get(a).contains(b);}
    /** how likely an affix is to be picked next, given those chosen so far */
    public static int weight(MobAffix affix,Set<MobAffix> chosen) {
        int weight=1;
        for(var other:chosen)if(works(affix,other))weight+=WEIGHT;
        return weight;
    }
}
