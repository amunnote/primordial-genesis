package local.primordial.smoke;

import local.primordial.affix.AffixState;
import local.primordial.affix.BossAffix;
import local.primordial.boss.BaihuLord;
import local.primordial.boss.SinBosses;
import local.primordial.boss.SinStatueBlock;
import local.primordial.boss.QinglongLord;
import local.primordial.boss.XuanwuLord;
import local.primordial.boss.ZhuqueLord;
import local.primordial.effect.AttributeCeiling;
import local.primordial.spirit.Guardian;
import local.primordial.spirit.PrimordialTemple;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** The health ceiling lifted, and the four guardians: the Black Tortoise's shell, the White Tiger's fragility, their door and statues. */
@GameTestHolder("primordial")
@PrefixGameTestTemplate(false)
public final class GuardianGameTests {
    @GameTest(template="empty") public static void healthCeiling(GameTestHelper helper) {
        helper.assertTrue(AttributeCeiling.maxHealth()>=AttributeCeiling.MAX_HEALTH,"max health may go to a million: "+AttributeCeiling.maxHealth());
        var lord=helper.spawn(SinBosses.PRIMORDIAL.get(),1,2,1);
        var tortoise=helper.spawn(SinBosses.XUANWU.get(),1,2,1);
        helper.assertTrue(lord.getMaxHealth()==1500&&tortoise.getMaxHealth()==3200,"the Primordial Lord has 1500 and the Black Tortoise 3200: "+lord.getMaxHealth()+" / "+tortoise.getMaxHealth());
        lord.discard();tortoise.discard();
        helper.succeed();
    }

    @GameTest(template="empty") public static void doorsOfTheGuardians(GameTestHelper helper) {
        var player=helper.makeMockSurvivalPlayer();
        for(var sin:BossAffix.values())PrimordialTemple.record(player,sin);
        helper.assertTrue(PrimordialTemple.sinsFelled(player)&&PrimordialTemple.qualified(player),"the seven alone open the temple: the guardians are no gate");
        helper.assertTrue(Guardian.BAIHU.within(Vec3.atCenterOf(Guardian.BAIHU.seat()))&&!Guardian.BAIHU.within(Vec3.ZERO),"the White Tiger's terrace bounds");
        helper.assertTrue(Guardian.XUANWU.at(0,Guardian.FLOOR,-112)!=null&&Guardian.BAIHU.at(-112,Guardian.FLOOR,0)!=null
            &&Guardian.QINGLONG.at(112,Guardian.FLOOR,0)!=null&&Guardian.ZHUQUE.at(0,Guardian.FLOOR,112)!=null,"all four terraces have floors");
        var recipe=helper.getLevel().getRecipeManager().byKey(SinBosses.PRIMORDIAL_SIGIL_RECIPE).orElseThrow();
        for(var relic:java.util.List.of(SinBosses.BAIHU_FANG,SinBosses.XUANWU_SCUTE,SinBosses.QINGLONG_SCALE,SinBosses.ZHUQUE_FEATHER)){var stack=new ItemStack(relic.get());
            helper.assertTrue(recipe.getIngredients().stream().noneMatch(i->i.test(stack)),"the Primordial Sigil no longer wants "+stack);}
        helper.succeed();
    }

    @GameTest(template="empty") public static void tortoiseShell(GameTestHelper helper) {
        XuanwuLord tortoise=helper.spawn(SinBosses.XUANWU.get(),2,2,2);
        tortoise.wake();
        tortoise.setYRot(0);tortoise.yBodyRot=0;tortoise.yBodyRotO=0;   // facing +z
        var zombie=helper.spawn(EntityType.ZOMBIE,2,2,2);AffixState.suppress(zombie);
        zombie.setPos(tortoise.getX(),tortoise.getY(),tortoise.getZ()+6);
        float before=tortoise.getHealth();
        tortoise.hurt(tortoise.damageSources().indirectMagic(zombie,zombie),20);tortoise.invulnerableTime=0;
        float head=before-tortoise.getHealth();
        zombie.setPos(tortoise.getX(),tortoise.getY(),tortoise.getZ()-6);
        before=tortoise.getHealth();
        tortoise.hurt(tortoise.damageSources().indirectMagic(zombie,zombie),20);tortoise.invulnerableTime=0;
        float shell=before-tortoise.getHealth();
        helper.assertTrue(head>=19&&shell<=5.5F&&shell>0,"its head takes it all, its shell a quarter: "+head+" / "+shell);
        zombie.discard();tortoise.discard();
        helper.succeed();
    }

    @GameTest(template="empty",timeoutTicks=100,batch="guardian_shell") public static void tortoiseWithdrawsAndCracks(GameTestHelper helper) {
        var level=helper.getLevel();var difficulty=level.getDifficulty();level.getServer().setDifficulty(Difficulty.NORMAL,true);
        XuanwuLord tortoise=helper.spawn(SinBosses.XUANWU.get(),2,2,2);
        tortoise.wake();
        tortoise.setHealth(tortoise.getMaxHealth()*0.74F);
        helper.runAfterDelay(4,()->{
            helper.assertTrue(tortoise.hiding(),"under three quarters it hides");
            var zombie=helper.spawn(EntityType.ZOMBIE,2,2,2);AffixState.suppress(zombie);
            float before=tortoise.getHealth();
            for(int i=0;i<12&&!tortoise.stunned();i++){tortoise.invulnerableTime=0;tortoise.hurt(tortoise.damageSources().indirectMagic(zombie,zombie),20);}
            helper.assertTrue(tortoise.stunned()&&!tortoise.hiding(),"220 harm of cracks breaks its shell: cracks "+tortoise.cracks());
            helper.assertTrue(before-tortoise.getHealth()<20,"while it hid it took next to nothing: "+(before-tortoise.getHealth()));
            before=tortoise.getHealth();tortoise.invulnerableTime=0;
            tortoise.hurt(tortoise.damageSources().indirectMagic(zombie,zombie),20);
            helper.assertTrue(before-tortoise.getHealth()>=39,"broken open, it takes double: "+(before-tortoise.getHealth()));
            zombie.discard();tortoise.discard();
            level.getServer().setDifficulty(difficulty,true);
            helper.succeed();
        });
    }

    @GameTest(template="empty") public static void dragonOnTheWind(GameTestHelper helper) {
        QinglongLord dragon=helper.spawn(SinBosses.QINGLONG.get(),2,2,2);
        dragon.wake();
        helper.assertTrue(dragon.getMaxHealth()==QinglongLord.HEALTH&&dragon.isNoGravity()&&!dragon.grounded(),"the Azure Dragon wakes on the wind");
        float before=dragon.getHealth();
        dragon.hurt(dragon.damageSources().magic(),10);dragon.invulnerableTime=0;
        float flying=before-dragon.getHealth();
        dragon.land(QinglongLord.SPENT);
        before=dragon.getHealth();
        dragon.hurt(dragon.damageSources().magic(),10);
        float spent=before-dragon.getHealth();
        helper.assertTrue(dragon.grounded()&&Math.abs(flying-7)<0.01&&Math.abs(spent-15)<0.01,"flying it takes 70%, spent 150%: "+flying+" / "+spent);
        dragon.discard();
        helper.succeed();
    }

    @GameTest(template="empty") public static void dragonStruckAlongItsLength(GameTestHelper helper) {
        QinglongLord dragon=helper.spawn(SinBosses.QINGLONG.get(),2,2,2);
        dragon.wake();dragon.setYRot(0);dragon.yBodyRot=0;dragon.placeParts();
        var parts=dragon.getParts();
        helper.assertTrue(dragon.isMultipartEntity()&&parts.length==8,"a head and seven lengths of body");
        for(int i=0;i<parts.length;i++)helper.assertTrue(parts[i].getId()==dragon.getId()+i+1,"each piece has the id after the last");
        // facing south (+z): the head ahead of its middle, the last length well behind it
        var head=parts[0];var tail=parts[parts.length-1];
        helper.assertTrue(head.getZ()-dragon.getZ()>5&&dragon.getZ()-tail.getZ()>14,"head "+(head.getZ()-dragon.getZ())+" ahead, tail "+(dragon.getZ()-tail.getZ())+" behind");
        // the world finds the tail where it lies, and a blow there harms the dragon
        helper.assertTrue(helper.getLevel().getEntities((net.minecraft.world.entity.Entity)null,tail.getBoundingBox(),e->e==tail).size()==1,"the tail is found where it lies");
        float before=dragon.getHealth();
        tail.hurt(dragon.damageSources().magic(),10);
        helper.assertTrue(Math.abs(before-dragon.getHealth()-7)<0.01,"a blow on the tail harms the dragon (70% in flight): "+(before-dragon.getHealth()));
        dragon.discard();
        helper.succeed();
    }

    @GameTest(template="empty") public static void phoenixEggBroken(GameTestHelper helper) {
        var level=helper.getLevel();
        ZhuqueLord bird=helper.spawn(SinBosses.ZHUQUE.get(),2,2,2);
        bird.wake();
        bird.setHealth(1);
        bird.hurt(bird.damageSources().magic(),10);
        helper.assertTrue(bird.isAlive()&&bird.egg()&&Math.abs(bird.getHealth()-bird.getMaxHealth()*ZhuqueLord.EGG_HEALTH)<0.01,"its first fall is an egg: "+bird.getHealth());
        bird.invulnerableTime=0;
        bird.hurt(bird.damageSources().magic(),100000);
        helper.assertTrue(bird.isDeadOrDying(),"the egg broken, it dies");
        var pos=helper.absolutePos(new BlockPos(2,2,2));boolean statue=false;
        for(int dy=-1;dy<6&&!statue;dy++)statue=level.getBlockState(pos.above(dy)).is(SinBosses.STATUE.get());
        helper.assertTrue(!statue,"a guardian leaves no statue");
        helper.succeed();
    }

    @GameTest(template="empty",timeoutTicks=300,batch="guardian_phoenix") public static void phoenixReborn(GameTestHelper helper) {
        var level=helper.getLevel();var difficulty=level.getDifficulty();level.getServer().setDifficulty(Difficulty.NORMAL,true);
        ZhuqueLord bird=helper.spawn(SinBosses.ZHUQUE.get(),2,2,2);
        bird.wake();
        bird.setHealth(1);
        bird.hurt(bird.damageSources().magic(),10);
        helper.assertTrue(bird.egg(),"an egg");
        helper.runAfterDelay(ZhuqueLord.EGG_TICKS+10,()->{
            helper.assertTrue(bird.isAlive()&&!bird.egg()&&bird.reborn()&&bird.enraged()&&Math.abs(bird.getHealth()-bird.getMaxHealth()*ZhuqueLord.REBORN_HEALTH)<bird.getMaxHealth()*0.06F,
                "left alone, the egg hatches: reborn, enraged, at half: "+bird.getHealth());
            bird.setHealth(1);bird.invulnerableTime=0;
            bird.hurt(bird.damageSources().magic(),10);
            helper.assertTrue(bird.isDeadOrDying(),"and the second fall is the last");
            level.getServer().setDifficulty(difficulty,true);
            helper.succeed();
        });
    }

    @GameTest(template="empty") public static void gemSwordRecipe(GameTestHelper helper) {
        var recipe=helper.getLevel().getRecipeManager().byKey(SinBosses.GEM_SWORD_RECIPE).orElseThrow();
        helper.assertTrue(recipe.getResultItem(helper.getLevel().registryAccess()).is(local.primordial.Primordial.GEM_SWORD.get()),"it makes the Gem Sword");
        helper.assertTrue(local.primordial.alchemy.AlchemyKnowledge.bossOnly(SinBosses.GEM_SWORD_RECIPE)&&!local.primordial.alchemy.AlchemyKnowledge.findable(SinBosses.GEM_SWORD_RECIPE),"its recipe is carried by a boss, never found");
        var recipes=local.primordial.alchemy.AlchemyKnowledge.bossOnlyRecipes();
        helper.assertTrue(recipes.indexOf(SinBosses.GEM_SWORD_RECIPE)>recipes.indexOf(SinBosses.ASCENSION_RECIPE),"registered after the Ascension Charm, so no Sin Lord carries it");
        for(var item:java.util.List.of(local.primordial.Primordial.GEM_SWORD_BLADE,local.primordial.Primordial.PRIMORDIAL_HILT,SinBosses.PHILOSOPHER_STONE)){var stack=new ItemStack(item.get());
            helper.assertTrue(recipe.getIngredients().stream().anyMatch(i->i.test(stack)),"the Gem Sword wants "+stack);}
        helper.succeed();
    }

    @GameTest(template="empty") public static void tigerAndStatue(GameTestHelper helper) {
        var level=helper.getLevel();
        BaihuLord tiger=helper.spawn(SinBosses.BAIHU.get(),2,2,2);
        tiger.wake();
        float before=tiger.getHealth();
        tiger.hurt(tiger.damageSources().magic(),10);
        helper.assertTrue(Math.abs(before-tiger.getHealth()-10*BaihuLord.FRAGILE)<0.01,"the White Tiger is fragile: "+(before-tiger.getHealth()));
        tiger.kill();
        var pos=helper.absolutePos(new BlockPos(2,2,2));boolean found=false;
        for(int dy=0;dy<6&&!found;dy++)found=level.getBlockState(pos.above(dy)).is(SinBosses.STATUE.get());
        helper.assertTrue(!found,"a guardian leaves no statue: it does not come back");
        helper.succeed();
    }
}
