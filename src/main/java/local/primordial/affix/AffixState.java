package local.primordial.affix;

import java.util.EnumSet;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public final class AffixState {
    public static final String KEY="primordial:affixes";
    /** attack, health and armor bonus per ordinary affix, as a share of the base value */
    public static final double PER_AFFIX=0.1;
    private static final UUID ORDINARY=UUID.fromString("a88e0690-f1ea-465b-80a5-cfa0d9f0ad01");
    private static final UUID BOSS=UUID.fromString("a88e0690-f1ea-465b-80a5-cfa0d9f0ad02");
    private static final UUID RAGE=UUID.fromString("a88e0690-f1ea-465b-80a5-cfa0d9f0ad03");
    private static final UUID ORDINARY_PERCENT=UUID.fromString("a88e0690-f1ea-465b-80a5-cfa0d9f0ad04");
    public final Mob mob;
    public final CompoundTag tag;
    public final EnumSet<MobAffix> affixes;
    public final BossAffix originalBoss;
    public final int level;
    private AffixState(Mob mob,CompoundTag tag) {
        this.mob=mob;this.tag=tag;level=Math.max(0,tag.getInt("level"));
        affixes=EnumSet.noneOf(MobAffix.class);
        for(var entry:tag.getList("ordinary",Tag.TAG_STRING)) {var affix=MobAffix.byId(entry.getAsString());if(affix!=null)affixes.add(affix);}
        // affixes lent by a nearby Amplify bearer: id -> ticks left
        temporary=EnumSet.noneOf(MobAffix.class);
        var lent=tag.getCompound("temporary");
        for(var id:lent.getAllKeys()){var affix=MobAffix.byId(id);if(affix!=null&&!affixes.contains(affix)){temporary.add(affix);affixes.add(affix);}}
        originalBoss=BossAffix.byId(tag.getString("boss"));
    }
    /** affixes lent by Amplify (also in affixes); they drop no materials and expire */
    public final EnumSet<MobAffix> temporary;
    /** lend an affix, or refresh how long a lent one lasts; returns whether the affixes changed */
    public boolean lend(MobAffix affix,int ticks) {
        var lent=tag.getCompound("temporary");boolean added=!lent.contains(affix.id());
        lent.putInt(affix.id(),ticks);tag.put("temporary",lent);
        if(added){temporary.add(affix);affixes.add(affix);applyAttributes();name();}
        return added;
    }
    /** count lent affixes down by elapsed ticks, dropping the expired ones */
    public void expireLent(int elapsed) {
        if(!tag.contains("temporary"))return;
        var lent=tag.getCompound("temporary");boolean changed=false;
        for(var id:java.util.List.copyOf(lent.getAllKeys())){
            int left=lent.getInt(id)-elapsed;
            if(left>0)lent.putInt(id,left);
            else{lent.remove(id);var affix=MobAffix.byId(id);if(affix!=null){temporary.remove(affix);affixes.remove(affix);}changed=true;}
        }
        if(lent.isEmpty())tag.remove("temporary");
        if(changed){applyAttributes();name();}
    }
    public static AffixState get(Mob mob) {
        if(!mob.getPersistentData().contains(KEY))return null;
        var tag=mob.getPersistentData().getCompound(KEY);
        return tag.getBoolean("active")?new AffixState(mob,tag):null;
    }
    public static AffixState assign(Mob mob,int level,EnumSet<MobAffix> affixes,BossAffix boss) {
        var tag=new CompoundTag();tag.putBoolean("initialized",true);tag.putBoolean("active",!affixes.isEmpty()||boss!=null);
        tag.putInt("level",Math.max(0,level));var list=new ListTag();for(var affix:affixes)list.add(StringTag.valueOf(affix.id()));
        tag.put("ordinary",list);if(boss!=null)tag.putString("boss",boss.id());
        tag.putString("name",Component.Serializer.toJson(mob.getName()));tag.putBoolean("oldNameVisible",mob.isCustomNameVisible());
        tag.putLong("lastCombat",mob.level().getGameTime());mob.getPersistentData().put(KEY,tag);
        var state=new AffixState(mob,tag);state.applyAttributes();if(tag.getBoolean("active")){state.name();mob.setHealth(mob.getMaxHealth());state.rage();}return state;
    }
    /** a sealed mob's affixes do nothing until the seal runs out (Seal's accessory) */
    public boolean has(MobAffix affix) {return affixes.contains(affix)&&!sealed();}
    public boolean sealed() {return tag.getLong("sealedUntil")>mob.level().getGameTime();}
    public void seal(int ticks) {
        tag.putLong("sealedUntil",Math.max(tag.getLong("sealedUntil"),mob.level().getGameTime()+ticks));
        if(!mob.level().isClientSide)local.primordial.network.MenuDropNetwork.syncSeal(mob,(int)(tag.getLong("sealedUntil")-mob.level().getGameTime()));
    }
    /** the hidden marker on each affix tag in the name: primordial:affix/<id>, with /lent for an Amplify loan */
    public static final String MARKER="primordial:affix/",LENT="/lent";
    public boolean boss(BossAffix affix) {
        if(primordial())return !sinOff(affix);
        return originalBoss==affix && !(affix==BossAffix.PRIDE&&tag.getBoolean("pridePhase"));
    }
    /**
     * 【原初】 Primordial: the Primordial Lord's own affix, every ordinary affix and every sin at once. Its numbers are
     * its own (the per-affix bonuses to health, attack and armor do not pile up on it), and a sin can be struck off it for
     * the rest of a fight (its pillar broken).
     */
    public boolean primordial() {return tag.getBoolean("primordial");}
    /** a sin or a primordial bearer: the affixed bosses */
    public boolean anyBoss() {return originalBoss!=null||primordial();}
    public boolean sinOff(BossAffix sin) {
        for(var entry:tag.getList("sinsOff",Tag.TAG_STRING))if(entry.getAsString().equals(sin.id()))return true;
        return false;
    }
    public void strikeSin(BossAffix sin) {
        if(sinOff(sin))return;
        var list=tag.getList("sinsOff",Tag.TAG_STRING);list.add(StringTag.valueOf(sin.id()));tag.put("sinsOff",list);
        applyAttributes();
    }
    public void restoreSins() {tag.remove("sinsOff");applyAttributes();}
    public static final int PRIMORDIAL_COLOUR=0xFFF0C8;
    public static Component primordialTag() {
        return Component.translatable("affix.primordial.tag",Component.translatableWithFallback("affix.primordial.primordial","原初")).withStyle(style->style.withColor(PRIMORDIAL_COLOUR));
    }
    public static AffixState assignPrimordial(Mob mob,int level) {
        var state=assign(mob,level,EnumSet.allOf(MobAffix.class),null);
        state.tag.putBoolean("primordial",true);state.tag.putBoolean("active",true);
        state.applyAttributes();state.name();mob.setHealth(mob.getMaxHealth());
        return state;
    }
    public void combat() {tag.putLong("lastCombat",mob.level().getGameTime());}
    public void applyAttributes() {
        // each affix: +3 attack, +5 health, +2 armor, and on top +10% of the mob's attack, health and armor
        // (the percentages add up: 4 affixes = +40%)
        int count=primordial()?0:affixes.size();double bonus=count*PER_AFFIX;
        modify(Attributes.ATTACK_DAMAGE,ORDINARY,count*3,AttributeModifier.Operation.ADDITION);
        modify(Attributes.MAX_HEALTH,ORDINARY,count*5,AttributeModifier.Operation.ADDITION);
        modify(Attributes.ARMOR,ORDINARY,count*2,AttributeModifier.Operation.ADDITION);
        modify(Attributes.ATTACK_DAMAGE,ORDINARY_PERCENT,bonus,AttributeModifier.Operation.MULTIPLY_BASE);
        modify(Attributes.MAX_HEALTH,ORDINARY_PERCENT,bonus,AttributeModifier.Operation.MULTIPLY_BASE);
        modify(Attributes.ARMOR,ORDINARY_PERCENT,bonus,AttributeModifier.Operation.MULTIPLY_BASE);
        if(mob.getHealth()>mob.getMaxHealth())mob.setHealth(mob.getMaxHealth());
        double health=boss(BossAffix.GLUTTONY)&&!primordial()?1:tag.getBoolean("pridePhase")?0.5:0;
        modify(Attributes.MAX_HEALTH,BOSS,health,AttributeModifier.Operation.MULTIPLY_TOTAL);
        modify(Attributes.ATTACK_DAMAGE,BOSS,tag.getBoolean("pridePhase")?0.5:0,AttributeModifier.Operation.MULTIPLY_TOTAL);
        rage();
    }
    private void modify(Attribute attribute,UUID uuid,double amount,AttributeModifier.Operation operation) {
        var instance=mob.getAttribute(attribute);if(instance==null)return;instance.removeModifier(uuid);
        if(amount!=0)instance.addPermanentModifier(new AttributeModifier(uuid,"primordial.affix",amount,operation));
    }
    public void rage() {
        int steps=boss(BossAffix.WRATH)?Math.min(9,(int)Math.floor((1-mob.getHealth()/Math.max(1,mob.getMaxHealth()))*10+0.00001)):0;
        if(steps==tag.getInt("rageSteps")&&tag.getBoolean("rageInitialized"))return;
        tag.putInt("rageSteps",steps);tag.putBoolean("rageInitialized",true);
        modify(Attributes.ATTACK_DAMAGE,RAGE,steps*0.1,AttributeModifier.Operation.MULTIPLY_TOTAL);
        modify(Attributes.ARMOR,RAGE,primordial()?0:steps*10,AttributeModifier.Operation.ADDITION);
    }
    public void name() {
        Component original;
        try{original=Component.Serializer.fromJson(tag.getString("name"));}catch(Exception e){original=null;}
        if(original==null)original=mob.getType().getDescription();
        var text=Component.empty().append(original);
        if(primordial()){text.append(primordialTag());mob.setCustomName(text);mob.setCustomNameVisible(true);return;}
        // each tag carries a hidden marker naming its affix (and whether it is lent), so clients can strike out the ones that do not touch them
        for(var affix:affixes)text.append(affix.tag().withStyle(style->style.withInsertion(MARKER+affix.id()+(temporary.contains(affix)?LENT:""))));
        if(originalBoss!=null&&!tag.getBoolean("pridePhase"))text.append(originalBoss.tag());
        mob.setCustomName(text);mob.setCustomNameVisible(true);
    }
    public void reset() {
        tag.putBoolean("timeUsed",false);tag.putBoolean("splitUsed",false);tag.putBoolean("pridePhase",false);
        tag.putBoolean("rageInitialized",false);applyAttributes();mob.setHealth(mob.getMaxHealth());
        mob.clearFire();mob.removeAllEffects();mob.deathTime=0;
        ((local.primordial.mixin.HealthAccessor)mob).angelSetDead(false);rage();name();
    }
    /** the remembered pre-affix name is only the creature's type name, i.e. nobody named it */
    private static boolean namedByType(CompoundTag tag,Mob mob) {
        return !tag.contains("name")||Component.Serializer.toJson(mob.getType().getDescription()).equals(tag.getString("name"));
    }
    /** a name a player gave the mob before it had affixes, or null */
    public static Component givenName(Mob mob) {
        if(!mob.getPersistentData().contains(KEY))return mob.getCustomName();
        var tag=mob.getPersistentData().getCompound(KEY);
        if(!tag.contains("name"))return mob.getCustomName();   // suppressed summons were never renamed
        if(namedByType(tag,mob))return null;
        try{return Component.Serializer.fromJson(tag.getString("name"));}catch(Exception e){return null;}
    }
    public static boolean givenNameVisible(Mob mob) {
        var tag=mob.getPersistentData().getCompound(KEY);
        return tag.contains("name")?tag.getBoolean("oldNameVisible"):mob.isCustomNameVisible();
    }
    /**
     * A mob turned into another (husk to zombie, skeleton to stray, piglin to zombified piglin...) keeps its affixes,
     * minus any the new creature cannot use. Replaces whatever the new mob rolled when it joined the world.
     */
    public static void inherit(Mob from,Mob to) {
        var old=from.getPersistentData().getCompound(KEY);
        if(!old.getBoolean("active"))return;
        var tag=old.copy();
        if(namedByType(old,from))tag.putString("name",Component.Serializer.toJson(to.getType().getDescription()));
        var kept=new ListTag();
        for(var entry:old.getList("ordinary",Tag.TAG_STRING)){var affix=MobAffix.byId(entry.getAsString());
            if(affix!=null&&AffixEligibility.usable(to,affix))kept.add(StringTag.valueOf(affix.id()));}
        tag.put("ordinary",kept);
        tag.putBoolean("active",!kept.isEmpty()||tag.contains("boss"));
        to.getPersistentData().put(KEY,tag);
        var state=new AffixState(to,tag);
        state.applyAttributes();   // also removes the attribute bonuses of whatever the new mob rolled
        if(tag.getBoolean("active"))state.name();
        else{to.setCustomName(givenName(from));to.setCustomNameVisible(givenNameVisible(from));}
        if(to.getHealth()>to.getMaxHealth())to.setHealth(to.getMaxHealth());
    }
    /**
     * A smaller slime split from a dying one: it keeps the parent's affixes (not a fresh roll), counts as a split copy
     * (no drops, no further summoning or splitting of its own) and starts at full health. A parent without affixes
     * passes on its plain name and the pieces stay without affixes too.
     */
    public static void splitPiece(Mob parent,Mob piece) {
        if(!parent.getPersistentData().contains(KEY))return;   // never rolled: the pieces roll normally
        var old=parent.getPersistentData().getCompound(KEY);
        if(!old.getBoolean("active")){suppress(piece);piece.setCustomName(givenName(parent));piece.setCustomNameVisible(givenNameVisible(parent));return;}
        inherit(parent,piece);
        var tag=piece.getPersistentData().getCompound(KEY);
        tag.putBoolean("clone",true);tag.putBoolean("splitUsed",true);tag.putBoolean("timeUsed",false);tag.remove("temporary");
        var state=new AffixState(piece,tag);state.applyAttributes();state.name();
        piece.setHealth(piece.getMaxHealth());
    }
    public static void suppress(Mob mob) {
        var tag=new CompoundTag();tag.putBoolean("initialized",true);tag.putBoolean("suppressed",true);mob.getPersistentData().put(KEY,tag);
    }
}
