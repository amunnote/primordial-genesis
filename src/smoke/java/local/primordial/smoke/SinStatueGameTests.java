package local.primordial.smoke;

import java.util.ArrayList;
import java.util.EnumSet;
import local.primordial.affix.AffixMaterials;
import local.primordial.affix.AffixState;
import local.primordial.affix.BossAffix;
import local.primordial.affix.MobAffix;
import local.primordial.boss.SinBoss;
import local.primordial.boss.SinBosses;
import local.primordial.boss.SinStatueBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** The seven sigils and the fallen lords' statues. */
@GameTestHolder("primordial")
@PrefixGameTestTemplate(false)
public final class SinStatueGameTests {
    static boolean dropsSigil(GameTestHelper helper,int affixes,int level) {
        var zombie=helper.spawn(EntityType.ZOMBIE,1,2,1);
        var own=EnumSet.noneOf(MobAffix.class);
        for(var affix:MobAffix.values()){if(own.size()>=affixes-1)break;own.add(affix);}
        var state=AffixState.assign(zombie,60,own,BossAffix.WRATH);
        var killer=helper.makeMockSurvivalPlayer();killer.experienceLevel=level;
        var drops=new ArrayList<ItemEntity>();
        AffixMaterials.drop(state,0,killer,drops);
        zombie.discard();
        return drops.stream().anyMatch(d->d.getItem().is(SinBosses.SIGILS.get(BossAffix.WRATH).get()));
    }

    @GameTest(template="empty") public static void sigilsFromSevenAffixBosses(GameTestHelper helper) {
        helper.assertTrue(dropsSigil(helper,7,50),"a seven-affix wrath boss drops the Sigil of Wrath to a level 50 killer");
        helper.assertTrue(!dropsSigil(helper,7,49),"not below level 50");
        helper.assertTrue(!dropsSigil(helper,6,60),"not from a boss with six affixes");
        helper.succeed();
    }

    @GameTest(template="empty") public static void statueRaisesAndRevives(GameTestHelper helper) {
        var level=helper.getLevel();
        SinBoss lord=helper.spawn(SinBosses.SLOTH.get(),2,2,2);
        var home=lord.home();
        lord.kill();
        var pos=helper.absolutePos(new BlockPos(2,2,2));
        BlockPos found=null;
        for(int dy=0;dy<4&&found==null;dy++)if(level.getBlockState(pos.above(dy)).is(SinBosses.STATUE.get()))found=pos.above(dy);
        helper.assertTrue(found!=null,"the fallen lord leaves a statue");
        var statue=(SinStatueBlock.Statue)level.getBlockEntity(found);
        helper.assertTrue(statue.sin()==BossAffix.SLOTH,"the statue is Sloth's");
        var player=helper.makeMockSurvivalPlayer();
        // the wrong sigil does nothing
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(SinBosses.SIGILS.get(BossAffix.PRIDE).get()));
        level.getBlockState(found).use(level,player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(found),net.minecraft.core.Direction.UP,found,false));
        helper.assertTrue(level.getBlockState(found).is(SinBosses.STATUE.get())&&player.getMainHandItem().getCount()==1,"the wrong sigil is refused");
        // its own sigil brings it back
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(SinBosses.SIGILS.get(BossAffix.SLOTH).get()));
        level.getBlockState(found).use(level,player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(found),net.minecraft.core.Direction.UP,found,false));
        helper.assertTrue(!level.getBlockState(found).is(SinBosses.STATUE.get())&&player.getMainHandItem().isEmpty(),"the sigil is spent and the statue gone");
        var risen=level.getEntitiesOfClass(SinBoss.class,new AABB(found).inflate(3),SinBoss::isAlive);
        helper.assertTrue(risen.size()==1&&risen.get(0).sin()==BossAffix.SLOTH&&!risen.get(0).dormant(),"the Lord of Sloth rises, awake");
        helper.assertTrue(risen.get(0).home().equals(home),"and keeps its sanctum seat");
        risen.get(0).discard();
        helper.succeed();
    }
}
