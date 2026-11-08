package local.primordial.smoke;

import local.primordial.affix.BossAffix;
import local.primordial.affix.ItemSeal;
import local.primordial.boss.EnvyShade;
import local.primordial.boss.PrideLord;
import local.primordial.boss.SinBoss;
import local.primordial.boss.SinBosses;
import local.primordial.boss.TreasureWeapon;
import local.primordial.spirit.SinSanctums;
import local.primordial.spirit.SpiritIslands;
import local.primordial.spirit.SpiritRealm;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("primordial")
@PrefixGameTestTemplate(false)
public final class SinLordGameTests {
    static boolean hurt(Pig pig) {return !pig.isAlive()||pig.getHealth()<pig.getMaxHealth();}

    @GameTest(template="empty",timeoutTicks=600) public static void sanctumsHoldTheirLords(GameTestHelper helper) {
        var realm=helper.getLevel().getServer().getLevel(SpiritRealm.LEVEL);
        for(var sin:BossAffix.values()){
            var seat=SinSanctums.seat(sin);
            helper.assertTrue(SpiritIslands.island(SpiritIslands.cellOf(seat.getX()),SpiritIslands.cellOf(seat.getZ()))==null,sin+"'s sanctum floats alone");
            helper.assertTrue(seat.distSqr(net.minecraft.core.BlockPos.ZERO)>700*700,sin+"'s sanctum is far out, at "+seat);
            realm.getChunk(seat.getX()>>4,seat.getZ()>>4);
            helper.assertTrue(!realm.getBlockState(seat.below()).isAir(),sin+"'s arena floor under the seat at "+seat);
        }
        var seat=SinSanctums.seat(BossAffix.PRIDE);
        realm.setChunkForced(seat.getX()>>4,seat.getZ()>>4,true);
        helper.succeedWhen(()->{
            var lords=realm.getEntitiesOfClass(PrideLord.class,new AABB(seat).inflate(6));
            helper.assertTrue(!lords.isEmpty(),"the Lord of Pride sleeps on his seat");
            helper.assertTrue(lords.get(0).dormant()&&lords.get(0).home().equals(seat),"asleep, at home");
            realm.setChunkForced(seat.getX()>>4,seat.getZ()>>4,false);
        });
    }

    @GameTest(template="empty") public static void lordsSleepUntilWoken(GameTestHelper helper) {
        var pride=helper.spawn(SinBosses.PRIDE.get(),1,3,1);
        var sloth=helper.spawn(SinBosses.SLOTH.get(),3,2,3);
        helper.assertTrue(pride.getMaxHealth()==600&&sloth.getMaxHealth()==420,"Pride is the strongest, 600; the others about a Wither's 400");
        helper.assertTrue(sloth.dormant(),"lords sleep at first");
        var player=helper.makeMockSurvivalPlayer();
        helper.assertTrue(!sloth.hurt(sloth.damageSources().playerAttack(player),10)&&sloth.getHealth()==420,"a sleeping lord takes no harm");
        helper.assertTrue(!sloth.dormant(),"but the blow wakes it");
        var sleeper=helper.spawn(SinBosses.GREED.get(),1,2,3);
        sleeper.kill();
        helper.assertTrue(!sleeper.isAlive(),"/kill still removes a sleeping lord");
        helper.assertTrue(!pride.getType().is(local.primordial.affix.AffixEvents.BOSSES)&&local.primordial.affix.AffixState.get(pride)==null,"lords roll no affixes");
        pride.discard();sloth.discard();
        helper.succeed();
    }

    @GameTest(template="empty") public static void treasuryAndTwelveLabours(GameTestHelper helper) {
        var level=helper.getLevel();
        helper.assertTrue(TreasureWeapon.treasury().contains(Items.DIAMOND_SWORD)&&TreasureWeapon.treasury().contains(Items.TRIDENT)
            &&!TreasureWeapon.treasury().contains(Items.BOW),"the treasury holds every weapon but bows");
        var pride=helper.spawn(SinBosses.PRIDE.get(),1,4,1);
        var pig=helper.spawn(EntityType.PIG,4,2,4);
        pride.skills().get(0).cast(pride,pig);
        int weapons=level.getEntitiesOfClass(TreasureWeapon.class,pride.getBoundingBox().inflate(12)).size();
        helper.assertTrue(weapons==PrideLord.GATES,"the Gate opens six portals and looses six weapons, got "+weapons);
        var wrath=helper.spawn(SinBosses.WRATH.get(),3,2,1);
        wrath.wake();
        wrath.hurt(wrath.damageSources().genericKill(),10000);
        helper.assertTrue(wrath.isAlive()&&wrath.revived()&&wrath.enraged()&&Math.abs(wrath.getHealth()-wrath.getMaxHealth()*0.33F)<1,"Twelve Labours: it rises again, got "+wrath.getHealth());
        wrath.invulnerableTime=0;
        wrath.hurt(wrath.damageSources().genericKill(),10000);
        helper.assertTrue(!wrath.isAlive(),"but only once");
        for(var w:level.getEntitiesOfClass(TreasureWeapon.class,pride.getBoundingBox().inflate(20)))w.discard();
        pride.discard();pig.discard();
        helper.succeed();
    }

    @GameTest(template="empty") public static void plunderDevourAndOath(GameTestHelper helper) {
        var greed=helper.spawn(SinBosses.GREED.get(),1,2,1);
        var player=helper.makeMockSurvivalPlayer();player.giveExperienceLevels(10);
        int taken=greed.plunder(player);
        helper.assertTrue(taken==3&&player.experienceLevel==7&&greed.hoard()>0,"Greed plunders three levels into its hoard");
        var pig=helper.spawn(EntityType.PIG,3,2,3);
        pig.hurt(pig.damageSources().generic(),4);float wounded=pig.getHealth();
        pig.addEffect(new net.minecraft.world.effect.MobEffectInstance(SinBosses.DEVOURED.get(),100));
        pig.heal(5);
        helper.assertTrue(pig.getHealth()==wounded,"the devoured cannot heal");
        var sloth=helper.spawn(SinBosses.SLOTH.get(),0,2,4);
        var inLine=helper.spawn(EntityType.PIG,4,2,4);
        var aside=helper.spawn(EntityType.PIG,4,2,1);
        sloth.skills().get(1).cast(sloth,inLine);
        helper.assertTrue(hurt(inLine)&&!hurt(aside),"the Oath Blade cuts along its line, and only there");
        for(var e:new net.minecraft.world.entity.Entity[]{greed,pig,sloth,inLine,aside})e.discard();
        helper.succeed();
    }

    @GameTest(template="empty") public static void envyAndLust(GameTestHelper helper) {
        var envy=helper.spawn(SinBosses.ENVY.get(),1,2,1);
        var player=helper.makeMockSurvivalPlayer();
        player.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.DIAMOND_SWORD));
        envy.skills().get(2).cast(envy,player);
        helper.assertTrue(envy.getMainHandItem().is(Items.DIAMOND_SWORD)&&ItemSeal.active(player,player.getMainHandItem()),"Envy copies your weapon and seals yours");
        var pig=helper.spawn(EntityType.PIG,3,2,3);
        envy.skills().get(1).cast(envy,pig);
        helper.assertTrue(envy.shades().size()==3,"three shades rise, got "+envy.shades().size());
        var lust=helper.spawn(SinBosses.LUST.get(),0,3,4);
        var pig2=helper.spawn(EntityType.PIG,4,2,4);
        lust.skills().get(0).cast(lust,pig2);
        helper.assertTrue(lust.charming(pig2),"Lust's kiss charms");
        for(var shade:envy.shades())shade.discard();
        for(var e:new net.minecraft.world.entity.Entity[]{envy,pig,lust,pig2})e.discard();
        helper.succeed();
    }
}
