package local.primordial.smoke;

import local.primordial.entity.alchemical.AlchemicalCreations;
import local.primordial.entity.alchemical.AlchemicalCreeper;
import local.primordial.entity.alchemical.AlchemicalGhast;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Every creation's signature attack, cast once at a pig. */
@GameTestHolder("primordial")
@PrefixGameTestTemplate(false)
public final class ArcaneSkillGameTests {
    static boolean hurt(Pig pig) {return !pig.isAlive()||pig.getHealth()<pig.getMaxHealth();}

    @GameTest(template="empty") public static void runeSlamAndCleave(GameTestHelper helper) {
        var zombie=helper.spawn(AlchemicalCreations.ZOMBIE.get(),1,2,1);
        var pig=helper.spawn(EntityType.PIG,3,2,1);
        zombie.skill().cast(zombie,pig);
        helper.assertTrue(hurt(pig)&&pig.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)&&pig.getDeltaMovement().y>0.3,"Rune Slam hurts, slows and throws up");
        var wither=helper.spawn(AlchemicalCreations.WITHER_SKELETON.get(),1,2,3);
        var pig2=helper.spawn(EntityType.PIG,3,2,3);
        wither.skill().cast(wither,pig2);
        helper.assertTrue(hurt(pig2)&&pig2.hasEffect(MobEffects.WITHER),"Spectral Cleave cuts and withers what is in front");
        for(var e:new net.minecraft.world.entity.Entity[]{zombie,pig,wither,pig2})e.discard();
        helper.succeed();
    }

    @GameTest(template="empty") public static void sigilThreadAndRift(GameTestHelper helper) {
        var blaze=helper.spawn(AlchemicalCreations.BLAZE.get(),1,3,1);
        var pig=helper.spawn(EntityType.PIG,3,2,1);
        blaze.skill().cast(blaze,pig);
        helper.assertTrue(hurt(pig)&&pig.isOnFire(),"The Pyre Sigil burns what stands in it");
        var spider=helper.spawn(AlchemicalCreations.SPIDER.get(),0,2,3);
        var pig2=helper.spawn(EntityType.PIG,4,2,3);
        spider.skill().cast(spider,pig2);
        helper.assertTrue(pig2.getDeltaMovement().x<-0.2&&pig2.hasEffect(MobEffects.POISON),"The soul-thread yanks toward the weaver, got "+pig2.getDeltaMovement());
        var enderman=helper.spawn(AlchemicalCreations.ENDERMAN.get(),0,2,0);
        var pig3=helper.spawn(EntityType.PIG,4,2,0);
        enderman.skill().cast(enderman,pig3);
        helper.assertTrue(enderman.distanceTo(pig3)<2.5&&hurt(pig3)&&pig3.hasEffect(MobEffects.BLINDNESS),"The Rift Strike steps behind and strikes");
        for(var e:new net.minecraft.world.entity.Entity[]{blaze,pig,spider,pig2,enderman,pig3})e.discard();
        helper.succeed();
    }

    @GameTest(template="empty") public static void starfallBeamMinesAndLeap(GameTestHelper helper) {
        var level=helper.getLevel();
        var skeleton=helper.spawn(AlchemicalCreations.SKELETON.get(),0,2,0);
        var pig=helper.spawn(EntityType.PIG,4,2,4);
        skeleton.skill().cast(skeleton,pig);
        int arrows=level.getEntitiesOfClass(Arrow.class,new AABB(pig.blockPosition()).inflate(4,16,4)).size();
        helper.assertTrue(arrows>=6,"Starfall rains arrows over the mark, got "+arrows);
        var ghast=helper.spawn(AlchemicalCreations.GHAST.get(),2,4,2);
        ghast.skill().cast(ghast,pig);
        helper.assertTrue(ghast.beamLeft()==AlchemicalGhast.BEAM_TICKS,"The Soul Gaze opens its beam");
        var creeper=helper.spawn(AlchemicalCreations.CREEPER.get(),1,2,3);
        creeper.detonate();
        helper.assertTrue(creeper.minesLaid()==AlchemicalCreeper.MINES,"Each blast lays its chain sigils");
        var cube=helper.spawn(AlchemicalCreations.MAGMA_CUBE.get(),0,2,4);
        cube.skill().cast(cube,pig);
        helper.assertTrue(cube.getDeltaMovement().y>0.5,"The magma core hurls itself up");
        for(var e:level.getEntitiesOfClass(Arrow.class,new AABB(pig.blockPosition()).inflate(8,20,8)))e.discard();
        for(var e:new net.minecraft.world.entity.Entity[]{skeleton,pig,ghast,creeper,cube})e.discard();
        helper.succeed();
    }
}
