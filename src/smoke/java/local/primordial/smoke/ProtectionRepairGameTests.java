package local.primordial.smoke;

import java.util.UUID;
import local.primordial.effect.Protection;
import local.primordial.effect.ProtectionRepair;
import local.primordial.mixin.EntityRemovalAccessor;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** The Ascension Charm's general defences: no attack reaches its wearer, and what slips past the guards is put right. */
@GameTestHolder("primordial")
@PrefixGameTestTemplate(false)
public final class ProtectionRepairGameTests {
    @GameTest(template="empty") public static void attacksAreRefused(GameTestHelper helper) {
        var wearer=helper.makeMockSurvivalPlayer();TestSlots.wearAscensionCharm(wearer);
        var attacker=helper.makeMockSurvivalPlayer();var bystander=helper.makeMockSurvivalPlayer();
        helper.assertTrue(Protection.active(wearer),"wearing the Ascension Charm");
        helper.assertTrue(!ForgeHooks.onPlayerAttackTarget(attacker,wearer),"an attack on the wearer, and any weapon's left-click power, is refused");
        helper.assertTrue(ForgeHooks.onPlayerAttackTarget(attacker,bystander),"an attack on anyone else goes ahead");
        helper.succeed();
    }

    @GameTest(template="empty") public static void heldInTheWorld(GameTestHelper helper) {
        var wearer=helper.makeMockSurvivalPlayer();TestSlots.wearAscensionCharm(wearer);
        var plain=helper.makeMockSurvivalPlayer();
        // the world's entity index: the wearer is not struck from it, unless it is leaving (logging out, changing dimension)
        var lookup=new net.minecraft.world.level.entity.EntityLookup<Entity>();
        lookup.add(wearer);lookup.add(plain);
        lookup.remove(wearer);lookup.remove(plain);
        helper.assertTrue(lookup.getEntity(wearer.getId())==wearer,"the wearer stays in the index");
        helper.assertTrue(lookup.getEntity(plain.getId())==null,"anyone else can be taken out");
        ((EntityRemovalAccessor)wearer).angelSetRemovalReason(Entity.RemovalReason.CHANGED_DIMENSION);
        lookup.remove(wearer);
        helper.assertTrue(lookup.getEntity(wearer.getId())==null,"changing dimension, it leaves as it should");
        ((EntityRemovalAccessor)wearer).angelSetRemovalReason(null);
        // no position that is not a number
        var before=wearer.position();
        wearer.setPos(Double.NaN,Double.POSITIVE_INFINITY,0);
        helper.assertTrue(wearer.position().equals(before),"a NaN or infinite position is refused");
        // thrown under the world: back where it last stood
        wearer.setOnGround(true);ProtectionRepair.repair(wearer);
        wearer.setPos(before.x,helper.getLevel().getMinBuildHeight()-200,before.z);
        ProtectionRepair.repair(wearer);
        helper.assertTrue(wearer.position().distanceTo(before)<0.01,"brought back from under the world, at "+wearer.position());
        // nothing drops from it
        var drops=new java.util.ArrayList<net.minecraft.world.entity.item.ItemEntity>();
        helper.assertTrue(net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.entity.living.LivingDropsEvent(wearer,wearer.damageSources().generic(),drops,0,true)),"its loot does not drop");
        helper.assertTrue(!net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.entity.living.LivingDropsEvent(plain,plain.damageSources().generic(),drops,0,true)),"anyone else's does");
        helper.succeed();
    }

    @GameTest(template="empty") public static void bossesKnownByTheirBars(GameTestHelper helper) {
        var level=helper.getLevel();
        helper.assertTrue(local.primordial.affix.AffixEvents.isBoss(new net.minecraft.world.entity.boss.wither.WitherBoss(net.minecraft.world.entity.EntityType.WITHER,level)),"the Wither, by its tag");
        helper.assertTrue(!local.primordial.affix.AffixEvents.isBoss(new net.minecraft.world.entity.monster.Zombie(level)),"a zombie is no boss");
        // any mod's creature with a boss bar of its own, tagged or not
        var barred=new net.minecraft.world.entity.monster.Zombie(level){
            final net.minecraft.server.level.ServerBossEvent bar=new net.minecraft.server.level.ServerBossEvent(getDisplayName(),net.minecraft.world.BossEvent.BossBarColor.RED,net.minecraft.world.BossEvent.BossBarOverlay.PROGRESS);
        };
        helper.assertTrue(local.primordial.affix.AffixEvents.isBoss(barred),"a creature keeping a boss bar is a boss");
        helper.succeed();
    }

    @GameTest(template="empty") public static void slippedHarmIsRepaired(GameTestHelper helper) {
        var wearer=helper.makeMockSurvivalPlayer();TestSlots.wearAscensionCharm(wearer);
        // a removal mark written straight into the field, past the guards
        ((EntityRemovalAccessor)wearer).angelSetRemovalReason(Entity.RemovalReason.KILLED);
        // a modifier robbing it of maximum health
        var max=wearer.getAttribute(Attributes.MAX_HEALTH);
        var robbery=UUID.fromString("0a0b0c0d-1e2f-4a5b-8c6d-7e8f9a0b1c2d");
        max.addTransientModifier(new AttributeModifier(robbery,"robbery",-15,AttributeModifier.Operation.ADDITION));
        ((local.primordial.mixin.HealthAccessor)wearer).angelSetDead(true);
        ProtectionRepair.repair(wearer);
        helper.assertTrue(!wearer.isRemoved(),"the removal mark is cleared");
        helper.assertTrue(!((local.primordial.mixin.HealthAccessor)wearer).angelIsDead(),"the death flag is cleared");
        helper.assertTrue(max.getModifier(robbery)==null,"the modifier lowering its health is stripped");
        // someone without the charm is left alone
        var plain=helper.makeMockSurvivalPlayer();
        ((EntityRemovalAccessor)plain).angelSetRemovalReason(Entity.RemovalReason.DISCARDED);
        ProtectionRepair.repair(plain);
        helper.assertTrue(plain.isRemoved(),"no charm, no repair");
        helper.succeed();
    }
}
