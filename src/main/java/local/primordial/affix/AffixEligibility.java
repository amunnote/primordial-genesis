package local.primordial.affix;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.TridentItem;

/** Which ordinary affixes can actually do something on a given mob; spawning only rolls those. */
public final class AffixEligibility {
    private AffixEligibility() {}

    public static boolean usable(Mob mob,MobAffix affix) {
        return switch(affix) {
            // homes the mob's own projectiles
            case CAUSALITY -> shoots(mob);
            // vanilla Strength only raises the melee attack attribute
            case STRENGTH -> mob.getAttribute(Attributes.ATTACK_DAMAGE)!=null&&melee(mob);
            // these mobs' attacks are already magic, which ignores armor
            case MAGIC,ARMOR_PIERCE -> !magicAttacker(mob);
            // shooters gain a melee hit, which needs an attack attribute; everything else gains arrows
            case MASTERY -> !shoots(mob)||mob.getAttribute(Attributes.ATTACK_DAMAGE)!=null;
            default -> true;
        };
    }
    /** Magic damage already ignores armor, so the two are never rolled together. */
    public static boolean conflicts(MobAffix affix,java.util.Set<MobAffix> chosen) {
        return affix==MobAffix.ARMOR_PIERCE&&chosen.contains(MobAffix.MAGIC)
            ||affix==MobAffix.MAGIC&&chosen.contains(MobAffix.ARMOR_PIERCE);
    }

    /** fires projectiles of its own: bows, crossbows, tridents, fireballs, shulker bullets, potions, skulls */
    public static boolean shoots(Mob mob) {
        if(holdsLauncher(mob))return true;
        // these only shoot with the right weapon in hand
        if(mob instanceof Drowned||mob instanceof AbstractSkeleton||mob instanceof AbstractPiglin||mob instanceof AbstractIllager)return false;
        return mob instanceof net.minecraft.world.entity.monster.RangedAttackMob||mob instanceof Ghast||mob instanceof Blaze
            ||mob instanceof Shulker||mob instanceof EnderDragon;
    }
    private static boolean melee(Mob mob) {
        if(mob instanceof Creeper||mob instanceof Witch||mob instanceof Evoker)return false;
        // skeletons, pillagers and trident drowned keep their distance and shoot
        return !holdsLauncher(mob);
    }
    private static boolean magicAttacker(Mob mob) {return mob instanceof Witch||mob instanceof Evoker||mob instanceof Guardian;}
    private static boolean holdsLauncher(Mob mob) {
        var item=mob.getMainHandItem().getItem();
        return item instanceof ProjectileWeaponItem||item instanceof TridentItem;
    }
}
