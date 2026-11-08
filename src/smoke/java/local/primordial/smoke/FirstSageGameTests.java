package local.primordial.smoke;

import local.primordial.affix.AffixState;
import local.primordial.boss.FirstSage;
import local.primordial.boss.SinBosses;
import local.primordial.spirit.PrimordialTemple;
import local.primordial.spirit.SageSanctum;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** The First Sage: its stages, its ward and the circles that break it, its omniscience, its court and its spoils. */
@GameTestHolder("primordial")
@PrefixGameTestTemplate(false)
public final class FirstSageGameTests {
    /** a blow of so much, after the Sage's armor */
    static float armored(FirstSage sage,float amount) {
        return net.minecraft.world.damagesource.CombatRules.getDamageAfterAbsorb(amount,sage.getArmorValue(),(float)sage.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS));
    }
    @GameTest(template="empty") public static void sageOmniscience(GameTestHelper helper) {
        FirstSage sage=helper.spawn(SinBosses.FIRST_SAGE.get(),2,2,2);
        sage.wake();
        helper.assertTrue(sage.getMaxHealth()==FirstSage.HEALTH,"the First Sage has "+FirstSage.HEALTH+": "+sage.getMaxHealth());
        float before=sage.getHealth();
        sage.hurt(sage.damageSources().magic(),40);sage.invulnerableTime=0;
        float first=before-sage.getHealth();
        helper.assertTrue(sage.seenThrough()==2,"after a heavy spell it sees through spells: "+sage.seenThrough());
        before=sage.getHealth();
        sage.hurt(sage.damageSources().magic(),40);sage.invulnerableTime=0;
        float second=before-sage.getHealth();
        var zombie=helper.spawn(EntityType.ZOMBIE,2,2,2);AffixState.suppress(zombie);
        before=sage.getHealth();
        sage.hurt(sage.damageSources().mobAttack(zombie),20);
        float blade=before-sage.getHealth();
        helper.assertTrue(Math.abs(first-40)<0.01&&Math.abs(second-28)<0.01&&Math.abs(blade-armored(sage,20))<0.01,"spells seen through take 70%, blades still whole (past its armor): "+first+" / "+second+" / "+blade);
        zombie.discard();sage.discard();
        helper.succeed();
    }

    @GameTest(template="empty",timeoutTicks=260,batch="first_sage_stages") public static void sageGreatWork(GameTestHelper helper) {
        var level=helper.getLevel();var difficulty=level.getDifficulty();level.getServer().setDifficulty(Difficulty.NORMAL,true);
        level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DOMOBLOOT).set(true,level.getServer());
        FirstSage sage=helper.spawn(SinBosses.FIRST_SAGE.get(),2,2,2);
        sage.wake();
        sage.setHealth(sage.getMaxHealth()*0.74F);
        helper.runAfterDelay(4,()->{
            helper.assertTrue(sage.stage()==2&&sage.warded()&&sage.isNoGravity(),"under three quarters: albedo, warded and risen");
            float before=sage.getHealth();
            sage.hurt(sage.damageSources().magic(),100);sage.invulnerableTime=0;
            helper.assertTrue(before-sage.getHealth()<=10.5F,"the ward lets a tenth through: "+(before-sage.getHealth()));
            for(int i=0;i<3;i++)sage.purify(i);
        });
        helper.runAfterDelay(8,()->{
            helper.assertTrue(sage.warded(),"three circles are not enough");
            sage.purify(3);
        });
        helper.runAfterDelay(12,()->{
            helper.assertTrue(!sage.warded()&&sage.stunned(),"all four purified: the ward shatters and it falls stunned");
            var zombie=helper.spawn(EntityType.ZOMBIE,2,2,2);AffixState.suppress(zombie);
            float before=sage.getHealth();
            sage.hurt(sage.damageSources().mobAttack(zombie),20);sage.invulnerableTime=0;
            helper.assertTrue(Math.abs(before-sage.getHealth()-armored(sage,30))<0.01,"stunned, it takes half again: "+(before-sage.getHealth()));
            zombie.discard();
            sage.setHealth(sage.getMaxHealth()*0.49F);
        });
        helper.runAfterDelay(FirstSage.STUN_TICKS+20,()->{
            helper.assertTrue(sage.stage()==3&&sage.enraged(),"under half, once it stands again: citrinitas, enraged");
            sage.setHealth(sage.getMaxHealth()*0.24F);
        });
        helper.runAfterDelay(FirstSage.STUN_TICKS+26,()->{
            helper.assertTrue(sage.stage()==4,"under a quarter: rubedo");
            var at=sage.position();
            sage.kill();
            var drops=level.getEntitiesOfClass(ItemEntity.class,new AABB(at,at).inflate(4));
            helper.assertTrue(drops.stream().anyMatch(i->i.getItem().is(SinBosses.PHILOSOPHER_STONE.get())),"it drops the Philosopher's Stone");
            helper.assertTrue(drops.stream().anyMatch(i->i.getItem().is(SinBosses.SAGE_SIGIL.get())),"and its sigil");
            helper.assertTrue(drops.stream().anyMatch(i->i.getItem().is(local.primordial.Primordial.ALCHEMY_RECIPE.get())
                &&SinBosses.GEM_SWORD_RECIPE.equals(local.primordial.item.AlchemyRecipeItem.recipe(i.getItem()))),"and the Gem Sword's recipe");
            for(var item:drops)item.discard();
            level.getServer().setDifficulty(difficulty,true);
            helper.succeed();
        });
    }

    @GameTest(template="empty") public static void courtOfTruth(GameTestHelper helper) {
        var player=helper.makeMockSurvivalPlayer();
        helper.assertTrue(!PrimordialTemple.primordialFelled(player),"no Primordial Lord felled: the court is shut");
        PrimordialTemple.recordPrimordial(player);
        helper.assertTrue(PrimordialTemple.primordialFelled(player),"felled: it opens");
        helper.assertTrue(SageSanctum.at(SageSanctum.CX,SageSanctum.FLOOR,SageSanctum.CZ)!=null&&SageSanctum.at(SageSanctum.CX+13,SageSanctum.FLOOR,SageSanctum.CZ)!=null,"the court has a floor, circles and all");
        helper.assertTrue(SageSanctum.within(net.minecraft.world.phys.Vec3.atCenterOf(SageSanctum.SEAT))&&!SageSanctum.within(net.minecraft.world.phys.Vec3.ZERO),"the court's bounds");
        helper.assertTrue(SinBosses.reviver(SinBosses.FIRST_SAGE.get())==SinBosses.SAGE_SIGIL.get(),"its sigil wakes its statue");
        helper.assertTrue(SinBosses.isSinType(SinBosses.FIRST_SAGE.get())&&SinBosses.isSinType(SinBosses.BAIHU.get())&&SinBosses.isSinType(SinBosses.ZHUQUE.get()),"none of them is spawned among the realm's spirits");
        helper.succeed();
    }
}
