package local.primordial.smoke;

import local.primordial.Primordial;
import local.primordial.affix.BossAffix;
import local.primordial.alchemy.AlchemyKnowledge;
import local.primordial.boss.SinBosses;
import local.primordial.item.AlchemyCharmItem;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** The four primordial charms: their recipes, who carries them, and their powers. */
@GameTestHolder("primordial")
@PrefixGameTestTemplate(false)
public final class PrimordialCharmGameTests {
    static Player wearing(GameTestHelper helper,net.minecraft.world.item.Item charm) {
        var player=helper.makeMockSurvivalPlayer();TestSlots.wearCharm(player,new ItemStack(charm));return player;
    }

    @GameTest(template="empty") public static void primordialRecipesAndCarriers(GameTestHelper helper) {
        var server=helper.getLevel().getServer();
        for(var id:new String[]{"omni_amplify_charm","fleeting_eternity_charm","unbreakable_charm","spell_annul_charm"}){
            var recipe=server.getRecipeManager().byKey(new ResourceLocation(Primordial.ID,id));
            helper.assertTrue(recipe.isPresent()&&recipe.get() instanceof local.primordial.altar.AltarRecipe altar&&altar.tier()==4,"primordial altar recipe for "+id);
            helper.assertTrue(AlchemyKnowledge.bossOnly(new ResourceLocation(Primordial.ID,id)),id+" is carried only by the Sin Lords");
        }
        helper.assertTrue(SinBosses.primordialRecipe(server,BossAffix.PRIDE)==null,"Pride carries none");
        var carried=new java.util.HashSet<ResourceLocation>();int carriers=0;
        for(var sin:BossAffix.values()){var r=SinBosses.primordialRecipe(server,sin);if(r!=null){carriers++;carried.add(r);}}
        helper.assertTrue(carriers==6&&carried.size()==6,"the six lords carry six recipes, one each: "+carriers+"/"+carried.size());
        helper.assertTrue(carried.contains(SinBosses.SEVEN_SINS_RECIPE)&&carried.contains(local.primordial.effect.WorldCores.WORLD_RECIPE),"the two left over carry the Seven Sins and the World Core");
        // the four charm recipes stay with the lords that carried them before
        for(int i=0;i<4;i++)helper.assertTrue(AlchemyKnowledge.bossOnlyRecipes().get(i).getPath().endsWith("_charm"),"charm recipes keep the first four places");
        // never in the realm's random recipe pool
        for(int i=0;i<200;i++){var r=local.primordial.spirit.SpiritRealm.realmRecipe(server,helper.getLevel().random);
            helper.assertTrue(r==null||!AlchemyKnowledge.bossOnly(r),"random recipe drop gave a primordial one: "+r);}
        helper.succeed();
    }

    @GameTest(template="empty") public static void primordialPowers(GameTestHelper helper) {
        var omni=wearing(helper,Primordial.OMNI_AMPLIFY_CHARM.get());
        helper.assertTrue(AlchemyCharmItem.echo(omni)==0.5&&AlchemyCharmItem.armorPierce(omni)==1.0&&AlchemyCharmItem.resistancePierce(omni)==1.0,"Omni Amplify");
        var eternity=wearing(helper,Primordial.FLEETING_ETERNITY_CHARM.get());
        helper.assertTrue(AlchemyCharmItem.stasis(eternity)&&AlchemyCharmItem.homing(eternity)&&AlchemyCharmItem.infiniteAmmo(eternity),"Fleeting Eternity");
        // Unbreakable: a huge physical blow takes no more than a quarter of maximum health
        var tank=wearing(helper,Primordial.UNBREAKABLE_CHARM.get());
        var zombie=helper.spawn(EntityType.ZOMBIE,1,2,1);local.primordial.affix.AffixState.suppress(zombie);
        float before=tank.getHealth();
        tank.hurt(tank.damageSources().mobAttack(zombie),1000);
        helper.assertTrue(tank.isAlive()&&before-tank.getHealth()<=tank.getMaxHealth()*0.25F+0.01F,"Unbreakable caps the blow, took "+(before-tank.getHealth()));
        // Spell Annulment: no harmful effects
        var ward=wearing(helper,Primordial.SPELL_ANNUL_CHARM.get());
        ward.addEffect(new MobEffectInstance(MobEffects.POISON,100));
        ward.addEffect(new MobEffectInstance(MobEffects.REGENERATION,100));
        helper.assertTrue(!ward.hasEffect(MobEffects.POISON)&&ward.hasEffect(MobEffects.REGENERATION),"Spell Annulment refuses harm, keeps help");
        zombie.discard();
        helper.succeed();
    }
}
