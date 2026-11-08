package local.primordial.smoke;

import local.primordial.Primordial;
import local.primordial.affix.AffixProjectiles;
import local.primordial.alchemy.AlchemyKnowledge;
import local.primordial.altar.AltarRecipe;
import local.primordial.item.AlchemyCharmItem;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("primordial")
@PrefixGameTestTemplate(false)
public final class TruthCharmGameTests {
    static final String[] CHARMS={"law_of_arms_charm","law_of_arcana_charm","law_of_gale_charm","law_of_tailwind_charm","unyielding_body_charm","arcane_harmony_charm"};
    private static void near(GameTestHelper helper,double actual,double expected,String what) {
        helper.assertTrue(Math.abs(actual-expected)<0.01,what+": expected "+expected+", got "+actual);
    }
    private static Player wearing(GameTestHelper helper,net.minecraft.world.item.Item charm) {
        var player=helper.makeMockSurvivalPlayer();TestSlots.wearCharm(player,new ItemStack(charm));return player;
    }
    private static Zombie target(GameTestHelper helper,int x,int z) {
        Zombie zombie=helper.spawn(net.minecraft.world.entity.EntityType.ZOMBIE,x,2,z);
        local.primordial.affix.AffixState.suppress(zombie);
        zombie.getAttribute(Attributes.ARMOR).setBaseValue(0);zombie.getAttribute(Attributes.MAX_HEALTH).setBaseValue(40);zombie.setHealth(40);
        return zombie;
    }

    @GameTest(template="empty") public static void truthCharmStats(GameTestHelper helper) {
        var plain=helper.makeMockSurvivalPlayer();
        double attack=plain.getAttributeValue(Attributes.ATTACK_DAMAGE),speed=plain.getAttributeValue(Attributes.ATTACK_SPEED);
        // luck with Sage's Gift on (it opens the charm slot): the basic charm's +5 taken back off
        double luck=wearing(helper,Primordial.BASIC_CHARM.get()).getAttributeValue(Attributes.LUCK)-5;

        var arms=wearing(helper,Primordial.LAW_OF_ARMS_CHARM.get());
        near(helper,arms.getAttributeValue(Attributes.ATTACK_DAMAGE),attack*2,"law of arms: +100% attack");
        near(helper,AlchemyCharmItem.armorPierce(arms),0.8,"law of arms: ignores 80% armor");
        near(helper,arms.getAttributeValue(Attributes.LUCK),luck+50,"law of arms: +50 luck");

        var arcana=wearing(helper,Primordial.LAW_OF_ARCANA_CHARM.get());
        near(helper,AlchemyCharmItem.nonPhysicalBonus(arcana),1.0,"law of arcana: +100% non-physical");
        helper.assertTrue(AlchemyCharmItem.magicAttacks(arcana),"law of arcana: magic attacks");
        near(helper,AlchemyCharmItem.resistancePierce(arcana),0.8,"law of arcana: ignores 80% resistance");

        var gale=wearing(helper,Primordial.LAW_OF_GALE_CHARM.get());
        near(helper,gale.getAttributeValue(Attributes.ATTACK_SPEED),speed*2,"law of the gale: +100% attack speed");
        helper.assertTrue(AlchemyCharmItem.ignoresInvulnerability(gale),"law of the gale: ignores invulnerability frames");

        var tailwind=wearing(helper,Primordial.LAW_OF_TAILWIND_CHARM.get());
        near(helper,AlchemyCharmItem.drawSpeed(tailwind),1.0,"law of the tailwind: +100% draw speed");
        near(helper,AlchemyCharmItem.projectileBonus(tailwind),1.0,"law of the tailwind: +100% projectile damage");
        helper.assertTrue(AlchemyCharmItem.homing(tailwind),"law of the tailwind: homing");
        Zombie shot=target(helper,1,1);var arrow=new Arrow(helper.getLevel(),tailwind);
        shot.hurt(tailwind.damageSources().arrow(arrow,tailwind),10);
        near(helper,40-shot.getHealth(),20,"law of the tailwind: a 10 damage arrow deals 20");
        shot.discard();arrow.discard();

        near(helper,AlchemyCharmItem.physicalReduction(wearing(helper,Primordial.UNYIELDING_BODY_CHARM.get())),0.7,"unyielding body: -70% physical damage");
        near(helper,AlchemyCharmItem.nonPhysicalReduction(wearing(helper,Primordial.ARCANE_HARMONY_CHARM.get())),0.7,"arcane harmony: -70% non-physical damage");
        for(var player:new Player[]{arcana,gale,tailwind})near(helper,player.getAttributeValue(Attributes.LUCK),luck+50,"+50 luck");
        helper.succeed();
    }

    @GameTest(template="empty") public static void galeIgnoresInvulnerabilityFrames(GameTestHelper helper) {
        var plain=helper.makeMockSurvivalPlayer();
        Zombie a=target(helper,1,1);
        a.hurt(plain.damageSources().playerAttack(plain),5);a.hurt(plain.damageSources().playerAttack(plain),5);
        near(helper,40-a.getHealth(),5,"without the charm the second hit is swallowed by invulnerability frames");
        var gale=wearing(helper,Primordial.LAW_OF_GALE_CHARM.get());
        Zombie b=target(helper,2,1);
        b.hurt(gale.damageSources().playerAttack(gale),5);b.hurt(gale.damageSources().playerAttack(gale),5);b.hurt(gale.damageSources().playerAttack(gale),5);
        near(helper,40-b.getHealth(),15,"with the charm every hit lands");
        a.discard();b.discard();helper.succeed();
    }

    @GameTest(template="empty") public static void tailwindArrowsHomeOnTheAimedTarget(GameTestHelper helper) {
        var level=helper.getLevel();
        Zombie aimed=target(helper,1,8),aside=target(helper,7,3);
        var tailwind=wearing(helper,Primordial.LAW_OF_TAILWIND_CHARM.get());
        var from=helper.absoluteVec(new Vec3(1.5,2,1.5));tailwind.setPos(from.x,from.y,from.z);
        // fired roughly at the far zombie, 10 degrees off
        var arrow=new Arrow(level,tailwind);var to=aimed.getBoundingBox().getCenter().subtract(arrow.position());
        double turn=Math.toRadians(10);
        arrow.shoot(to.x*Math.cos(turn)-to.z*Math.sin(turn),to.y,to.x*Math.sin(turn)+to.z*Math.cos(turn),3,0);
        level.addFreshEntity(arrow);
        helper.assertTrue(AffixProjectiles.intended(arrow,aimed),"the arrow locks onto the zombie it was aimed at");
        helper.assertTrue(!AffixProjectiles.intended(arrow,aside),"not the one off to the side");
        arrow.discard();
        // without the charm nothing homes
        var plain=helper.makeMockSurvivalPlayer();plain.setPos(from.x,from.y,from.z);
        var normal=new Arrow(level,plain);normal.shoot(to.x,to.y,to.z,3,0);level.addFreshEntity(normal);
        helper.assertTrue(!AffixProjectiles.causal(normal),"plain arrows do not home");
        normal.discard();aimed.discard();aside.discard();
        helper.succeed();
    }

    @GameTest(template="empty") public static void truthCharmRecipes(GameTestHelper helper) {
        var recipes=helper.getLevel().getRecipeManager();
        for(var id:CHARMS){
            var key=new ResourceLocation(Primordial.ID,id);
            var recipe=recipes.byKey(key);
            helper.assertTrue(recipe.isPresent()&&recipe.get() instanceof AltarRecipe altar&&altar.tier()==3,"truth altar recipe "+id);
            helper.assertTrue(AlchemyKnowledge.recipes().contains(key),"alchemy recipe item for "+id);
            helper.assertTrue(helper.getLevel().getServer().getLootData().getLootTable(new ResourceLocation("minecraft","chests/ancient_city"))!=null,"loot");
        }
        helper.succeed();
    }
}
