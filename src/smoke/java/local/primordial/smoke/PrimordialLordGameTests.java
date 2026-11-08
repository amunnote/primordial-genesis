package local.primordial.smoke;

import local.primordial.affix.AffixState;
import local.primordial.affix.BossAffix;
import local.primordial.affix.MobAffix;
import local.primordial.alchemy.AlchemyKnowledge;
import local.primordial.boss.PrimordialLord;
import local.primordial.boss.SinBosses;
import local.primordial.boss.SinPillar;
import local.primordial.boss.SinStatueBlock;
import local.primordial.spirit.PrimordialTemple;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** The Primordial Lord: its affix, its temple's door, its phases, its echoes and its statue. */
@GameTestHolder("primordial")
@PrefixGameTestTemplate(false)
public final class PrimordialLordGameTests {
    @GameTest(template="empty") public static void primordialAffix(GameTestHelper helper) {
        PrimordialLord lord=helper.spawn(SinBosses.PRIMORDIAL.get(),1,2,1);
        var state=AffixState.assignPrimordial(lord,PrimordialLord.LEVEL);
        helper.assertTrue(state.primordial()&&state.affixes.size()==MobAffix.values().length,"every ordinary affix");
        for(var sin:BossAffix.values())helper.assertTrue(state.boss(sin),"and every sin: "+sin);
        helper.assertTrue(Math.abs(lord.getAttributeValue(Attributes.MAX_HEALTH)-PrimordialLord.HEALTH)<1,"its health is its own, not swollen by affixes: "+lord.getMaxHealth());
        state.strikeSin(BossAffix.WRATH);
        helper.assertTrue(!AffixState.get(lord).boss(BossAffix.WRATH)&&AffixState.get(lord).boss(BossAffix.PRIDE),"a sin can be struck off");
        AffixState.get(lord).restoreSins();
        helper.assertTrue(AffixState.get(lord).boss(BossAffix.WRATH),"and restored");
        lord.discard();
        helper.succeed();
    }

    @GameTest(template="empty") public static void templeDoor(GameTestHelper helper) {
        var player=helper.makeMockSurvivalPlayer();
        helper.assertTrue(!PrimordialTemple.qualified(player),"no lords felled: no entry");
        for(var sin:BossAffix.values()){if(sin!=BossAffix.LUST)PrimordialTemple.record(player,sin);}
        helper.assertTrue(!PrimordialTemple.qualified(player)&&PrimordialTemple.defeated(player).size()==6,"six of seven is not enough");
        PrimordialTemple.record(player,BossAffix.LUST);
        helper.assertTrue(PrimordialTemple.sinsFelled(player)&&PrimordialTemple.qualified(player),"all seven: the temple opens");
        // the temple's shape
        helper.assertTrue(PrimordialTemple.at(0,PrimordialTemple.FLOOR,0)!=null,"a floor under the dais");
        helper.assertTrue(PrimordialTemple.at(0,PrimordialTemple.FLOOR+1,0).is(Blocks.SEA_LANTERN),"a light at the dais's heart");
        helper.assertTrue(PrimordialTemple.at(200,PrimordialTemple.FLOOR,0)==null,"nothing far out");
        helper.assertTrue(PrimordialTemple.within(new Vec3(0,PrimordialTemple.FLOOR+5,10))&&!PrimordialTemple.within(new Vec3(56,100,56)),"its bounds");
        helper.assertTrue(PrimordialTemple.brazier(BossAffix.PRIDE).z<-50,"Pride's arch stands to the north");
        var recipe=helper.getLevel().getServer().getRecipeManager().byKey(SinBosses.PRIMORDIAL_SIGIL_RECIPE);
        helper.assertTrue(recipe.isPresent()&&recipe.get() instanceof local.primordial.altar.AltarRecipe altar&&altar.tier()==4,"the sigil is made at a primordial altar");
        helper.assertTrue(!AlchemyKnowledge.findable(SinBosses.PRIMORDIAL_SIGIL_RECIPE),"its recipe only comes by epiphany");
        helper.succeed();
    }

    @GameTest(template="empty",timeoutTicks=200,batch="primordial_phases") public static void phasesAndPillars(GameTestHelper helper) {
        PrimordialLord lord=helper.spawn(SinBosses.PRIMORDIAL.get(),1,2,1);
        // its pillars rise twenty blocks out: keep that ground loaded; and monsters do not stay in peace
        var level=helper.getLevel();var centre=lord.blockPosition();
        var difficulty=level.getDifficulty();level.getServer().setDifficulty(net.minecraft.world.Difficulty.NORMAL,true);
        for(int cx=(centre.getX()-24)>>4;cx<=(centre.getX()+24)>>4;cx++)for(int cz=(centre.getZ()-24)>>4;cz<=(centre.getZ()+24)>>4;cz++)level.setChunkForced(cx,cz,true);
        AffixState.assignPrimordial(lord,PrimordialLord.LEVEL);
        lord.wake();
        lord.setHealth(lord.getMaxHealth()*0.55F);
        helper.runAfterDelay(5,()->{
            helper.assertTrue(lord.phase()==2,"under 60%: the sins become one; phase "+lord.phase()+" dormant "+lord.dormant()+" health "+lord.getHealth()+"/"+lord.getMaxHealth()+" alive "+lord.isAlive()+" removed "+lord.getRemovalReason()+" at "+lord.position()+" home "+lord.home());
            var pillars=helper.getLevel().getEntitiesOfClass(SinPillar.class,new AABB(lord.blockPosition()).inflate(30),SinPillar::isAlive);
            helper.assertTrue(pillars.size()==7,"seven pillars rise: "+pillars.size());
            var wrath=pillars.stream().filter(p->p.sin()==BossAffix.WRATH).findFirst().orElseThrow();
            wrath.kill();
            helper.assertTrue(!AffixState.get(lord).boss(BossAffix.WRATH),"breaking Wrath's pillar strikes off Wrath");
            helper.assertTrue(lord.splitEchoes()==2,"its split: two echoes");
            lord.setHealth(lord.getMaxHealth()*0.2F);
            helper.runAfterDelay(5,()->{
                helper.assertTrue(lord.phase()==3,"under a quarter: the last phase");
                helper.assertTrue(helper.getLevel().getEntitiesOfClass(SinPillar.class,new AABB(lord.blockPosition()).inflate(30),SinPillar::isAlive).isEmpty(),"the rest of the pillars collapse");
                var echoes=helper.getLevel().getEntitiesOfClass(PrimordialLord.class,new AABB(lord.blockPosition()).inflate(10),PrimordialLord::echo);
                helper.assertTrue(echoes.size()>=2&&echoes.stream().allMatch(e->e.getMaxHealth()==PrimordialLord.ECHO_HEALTH&&AffixState.get(e)==null),"faint echoes, without its affixes: "+echoes.size()+" "+echoes.stream().map(e->e.getMaxHealth()+"/"+(AffixState.get(e)!=null)).toList());
                for(var e:echoes)e.discard();
                lord.discard();
                for(int cx=(centre.getX()-24)>>4;cx<=(centre.getX()+24)>>4;cx++)for(int cz=(centre.getZ()-24)>>4;cz<=(centre.getZ()+24)>>4;cz++)level.setChunkForced(cx,cz,false);
                level.getServer().setDifficulty(difficulty,true);
                helper.succeed();
            });
        });
    }

    @GameTest(template="empty") public static void statueAndSigil(GameTestHelper helper) {
        var level=helper.getLevel();
        PrimordialLord lord=helper.spawn(SinBosses.PRIMORDIAL.get(),2,2,2);
        var home=lord.home();
        lord.kill();
        var pos=helper.absolutePos(new BlockPos(2,2,2));
        BlockPos found=null;
        for(int dy=0;dy<6&&found==null;dy++)if(level.getBlockState(pos.above(dy)).is(SinBosses.STATUE.get()))found=pos.above(dy);
        helper.assertTrue(found!=null,"the fallen lord leaves a statue");
        var statue=(SinStatueBlock.Statue)level.getBlockEntity(found);
        helper.assertTrue(statue.primordial()&&statue.lordType()==SinBosses.PRIMORDIAL.get(),"the Primordial Lord's own");
        var player=helper.makeMockSurvivalPlayer();
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(SinBosses.SIGILS.get(BossAffix.PRIDE).get()));
        level.getBlockState(found).use(level,player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(found),net.minecraft.core.Direction.UP,found,false));
        helper.assertTrue(level.getBlockState(found).is(SinBosses.STATUE.get()),"a sin's sigil does not wake it");
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(SinBosses.PRIMORDIAL_HEART.get()));
        level.getBlockState(found).use(level,player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(found),net.minecraft.core.Direction.UP,found,false));
        helper.assertTrue(level.getBlockState(found).is(SinBosses.STATUE.get()),"nor does its heart");
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(SinBosses.PRIMORDIAL_SIGIL.get()));
        level.getBlockState(found).use(level,player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(found),net.minecraft.core.Direction.UP,found,false));
        helper.assertTrue(!level.getBlockState(found).is(SinBosses.STATUE.get())&&player.getMainHandItem().isEmpty(),"the Primordial Sigil is spent");
        var risen=level.getEntitiesOfClass(PrimordialLord.class,new AABB(found).inflate(3),PrimordialLord::isAlive);
        helper.assertTrue(risen.size()==1&&!risen.get(0).dormant()&&risen.get(0).home().equals(home),"and the Primordial Lord rises, awake, at home");
        risen.get(0).discard();
        helper.succeed();
    }
}
