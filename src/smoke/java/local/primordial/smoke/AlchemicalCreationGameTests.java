package local.primordial.smoke;

import local.primordial.entity.alchemical.AlchemicalCreations;
import local.primordial.entity.alchemical.AlchemicalCreeper;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("primordial")
@PrefixGameTestTemplate(false)
public final class AlchemicalCreationGameTests {
    @GameTest(template="empty") public static void alchemicalCreeperSurvivesItsBlast(GameTestHelper helper) {
        var creeper=helper.spawn(AlchemicalCreations.CREEPER.get(),2,2,2);
        BlockPos[] walls={new BlockPos(1,2,2),new BlockPos(3,2,2),new BlockPos(2,2,1),new BlockPos(2,2,3)};
        for(var wall:walls)helper.setBlock(wall,Blocks.STONE);
        float health=creeper.getHealth();
        creeper.detonate();
        helper.assertTrue(creeper.isAlive()&&creeper.getHealth()==health,"The blast leaves the creeper whole");
        for(var wall:walls)helper.assertBlockPresent(Blocks.STONE,wall);
        helper.assertTrue(creeper.recharge()==AlchemicalCreeper.RECHARGE,"It recharges before the next blast");
        helper.assertTrue(creeper.getSwelling(1)==0&&creeper.getSwellDir()<0,"The fuse is out");
        creeper.discard();
        helper.succeed();
    }

    @GameTest(template="empty") public static void alchemicalCreationsMendTheirWeakPoints(GameTestHelper helper) {
        var zombie=helper.spawn(AlchemicalCreations.ZOMBIE.get(),1,2,1);
        zombie.setBaby(true);
        helper.assertTrue(!zombie.isBaby(),"No baby ghouls");
        helper.assertTrue(zombie.canBreatheUnderwater(),"Ghouls do not drown");
        helper.assertTrue(zombie.getMobType()==MobType.UNDEFINED,"Ghouls are not undead");
        var skeleton=helper.spawn(AlchemicalCreations.SKELETON.get(),2,2,1);
        helper.assertTrue(!skeleton.canFreeze()&&skeleton.getMobType()==MobType.UNDEFINED,"Archers do not freeze into strays");
        var enderman=helper.spawn(AlchemicalCreations.ENDERMAN.get(),3,2,1);
        helper.assertTrue(!enderman.isSensitiveToWater(),"Water does not hurt striders");

        var spider=helper.spawn(AlchemicalCreations.SPIDER.get(),1,2,3);
        var pig=helper.spawn(EntityType.PIG,2,2,3);
        helper.assertTrue(spider.getMobType()==MobType.UNDEFINED,"Weavers are not arthropods");
        spider.doHurtTarget(pig);
        helper.assertTrue(pig.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)&&pig.hasEffect(MobEffects.POISON),"A weaver's bite slows and poisons");

        helper.assertTrue(Math.abs(AlchemicalCreations.recipeChance(0)-0.10)<1e-9&&Math.abs(AlchemicalCreations.recipeChance(3)-0.10)<1e-9,"recipe odds");
        for(var e:new net.minecraft.world.entity.Entity[]{zombie,skeleton,enderman,spider,pig})e.discard();
        helper.succeed();
    }

    @GameTest(template="empty") public static void netherCreationsMendTheirWeakPoints(GameTestHelper helper) {
        var level=helper.getLevel();
        var blaze=helper.spawn(AlchemicalCreations.BLAZE.get(),1,2,1);
        var snowball=new net.minecraft.world.entity.projectile.Snowball(level,blaze.getX(),blaze.getY()+3,blaze.getZ());
        helper.assertTrue(!blaze.isSensitiveToWater()&&!blaze.hurt(level.damageSources().thrown(snowball,null),3)&&blaze.getHealth()==40,"Snowballs and water do not hurt pyre wraiths");
        snowball.discard();

        var player=helper.makeMockPlayer();
        var ghast=helper.spawn(AlchemicalCreations.GHAST.get(),2,2,2);
        var fireball=new net.minecraft.world.entity.projectile.LargeFireball(level,player,0,0,0,1);
        helper.assertTrue(!ghast.hurt(level.damageSources().fireball(fireball,player),1000)&&ghast.getHealth()==40,"A fireball sent back does not hurt a soulbag");
        fireball.discard();

        var wither=helper.spawn(AlchemicalCreations.WITHER_SKELETON.get(),3,2,1);
        helper.assertTrue(wither.getMobType()==MobType.UNDEFINED&&wither.getBbHeight()<2&&wither.getEyeHeight()<2,"Witherbones fit two-high gaps and are no undead");
        var pig=helper.spawn(EntityType.PIG,3,2,3);
        wither.doHurtTarget(pig);
        helper.assertTrue(pig.hasEffect(MobEffects.WITHER)&&pig.getEffect(MobEffects.WITHER).getAmplifier()==1,"A witherbone's blow withers twice as hard");

        var cube=helper.spawn(AlchemicalCreations.MAGMA_CUBE.get(),1,2,3);
        helper.assertTrue(cube.getSize()==3&&cube.getMaxHealth()==40,"Magma cores are always full grown, got size "+cube.getSize());
        cube.kill();
        helper.runAfterDelay(30,()->{
            var box=new net.minecraft.world.phys.AABB(helper.absolutePos(BlockPos.ZERO)).inflate(16);
            helper.assertTrue(level.getEntitiesOfClass(net.minecraft.world.entity.monster.MagmaCube.class,box).isEmpty(),"No little cubes split off");
            for(var e:new net.minecraft.world.entity.Entity[]{blaze,ghast,wither,pig})e.discard();
            helper.succeed();
        });
    }
}
