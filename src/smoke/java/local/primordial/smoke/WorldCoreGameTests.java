package local.primordial.smoke;

import java.util.ArrayList;
import java.util.EnumSet;
import local.primordial.Primordial;
import local.primordial.affix.AffixMaterials;
import local.primordial.affix.AffixState;
import local.primordial.affix.MobAffix;
import local.primordial.alchemy.AlchemyKnowledge;
import local.primordial.effect.WorldCores;
import local.primordial.effect.WorldCores.Realm;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import top.theillusivec4.curios.api.CuriosApi;

/** The world souls, their epiphanies, and the three world cores. */
@GameTestHolder("primordial")
@PrefixGameTestTemplate(false)
public final class WorldCoreGameTests {
    static Player sage(GameTestHelper helper) {
        var player=helper.makeMockSurvivalPlayer();
        CuriosApi.getCuriosInventory(player).orElseThrow(IllegalStateException::new).setEquippedCurio(Primordial.SAGE_SLOT,0,new ItemStack(Primordial.SAGE_HEART.get()));
        TestSlots.tick(player);return player;
    }
    static boolean dropsSoul(GameTestHelper helper,Player killer,int affixes) {
        var warden=helper.spawn(EntityType.WARDEN,1,2,1);
        var own=EnumSet.noneOf(MobAffix.class);
        for(var affix:MobAffix.values()){if(own.size()>=affixes)break;own.add(affix);}
        var state=AffixState.assign(warden,120,own,null);
        var drops=new ArrayList<ItemEntity>();
        AffixMaterials.drop(state,0,killer,drops);
        warden.discard();
        return drops.stream().anyMatch(d->d.getItem().is(WorldCores.SOULS.get(Realm.OVERWORLD).get()));
    }

    @GameTest(template="empty") public static void soulsAndEpiphanies(GameTestHelper helper) {
        var server=helper.getLevel().getServer();
        for(var realm:Realm.values()){
            var recipe=server.getRecipeManager().byKey(realm.recipe());
            helper.assertTrue(recipe.isPresent()&&recipe.get() instanceof local.primordial.altar.AltarRecipe altar&&altar.tier()==3,"truth altar recipe for "+realm.coreId());
            helper.assertTrue(!AlchemyKnowledge.findable(realm.recipe()),realm.coreId()+" is learned only by epiphany");
        }
        for(int i=0;i<200;i++){var r=local.primordial.spirit.SpiritRealm.realmRecipe(server,helper.getLevel().random);
            helper.assertTrue(r==null||AlchemyKnowledge.findable(r),"random recipe drop gave a world core: "+r);}
        helper.assertTrue(EntityType.WARDEN.is(local.primordial.affix.AffixEvents.BOSSES),"the Warden counts as a boss");
        var player=sage(helper);
        helper.assertTrue(!dropsSoul(helper,player,6),"no soul from a six-affix Warden");
        helper.assertTrue(!AlchemyKnowledge.knows(player,Realm.OVERWORLD.recipe()),"and no epiphany");
        helper.assertTrue(dropsSoul(helper,player,7),"a seven-affix Warden drops the Overworld Soul");
        helper.assertTrue(AlchemyKnowledge.knows(player,Realm.OVERWORLD.recipe()),"and its slayer learns the Overworld Core");
        helper.succeed();
    }

    @GameTest(template="empty") public static void corePowers(GameTestHelper helper) {
        var player=sage(helper);
        player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.LUCK).setBaseValue(60);
        var inv=CuriosApi.getCuriosInventory(player).orElseThrow(IllegalStateException::new);
        inv.setEquippedCurio(Primordial.ALCHEMICAL_ACCESSORY_SLOT,0,new ItemStack(WorldCores.CORES.get(Realm.END).get()));
        TestSlots.tick(player);
        helper.assertTrue(WorldCores.wears(player,Realm.END),"wearing the End Core");
        float before=player.getHealth();
        player.hurt(player.damageSources().fall(),10);
        helper.assertTrue(player.getHealth()==before,"the End Core takes no fall damage");
        inv.setEquippedCurio(Primordial.ALCHEMICAL_ACCESSORY_SLOT,0,new ItemStack(WorldCores.CORES.get(Realm.NETHER).get()));
        TestSlots.tick(player);
        var zombie=helper.spawn(EntityType.ZOMBIE,1,2,1);AffixState.suppress(zombie);
        zombie.hurt(player.damageSources().playerAttack(player),2);
        helper.assertTrue(zombie.isOnFire(),"a Nether Core blow sets the target alight");
        zombie.discard();
        helper.succeed();
    }

    @GameTest(template="empty") public static void worldCoreAndSevenSins(GameTestHelper helper) {
        var server=helper.getLevel().getServer();
        for(var id:new net.minecraft.resources.ResourceLocation[]{WorldCores.WORLD_RECIPE,local.primordial.boss.SinBosses.SEVEN_SINS_RECIPE}){
            var recipe=server.getRecipeManager().byKey(id);
            helper.assertTrue(recipe.isPresent()&&recipe.get() instanceof local.primordial.altar.AltarRecipe altar&&altar.tier()==4,"primordial altar recipe for "+id);
            helper.assertTrue(AlchemyKnowledge.bossOnly(id),id+" is carried only by a Sin Lord");
        }
        var player=sage(helper);
        player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.LUCK).setBaseValue(90);
        var inv=CuriosApi.getCuriosInventory(player).orElseThrow(IllegalStateException::new);
        inv.setEquippedCurio(Primordial.ALCHEMICAL_ACCESSORY_SLOT,0,new ItemStack(WorldCores.WORLD_CORE.get()));
        TestSlots.tick(player);
        helper.assertTrue(WorldCores.wears(player,Realm.NETHER)&&WorldCores.wears(player,Realm.END)&&WorldCores.atHome(player,Realm.END),"the World Core holds every realm's power, at home anywhere");
        float before=player.getHealth();
        player.hurt(player.damageSources().fall(),10);
        helper.assertTrue(player.getHealth()==before,"the World Core takes no fall damage");
        // the Seven Sins: Lust executes a non-boss brought under 20%
        inv.setEquippedCurio(Primordial.ALCHEMICAL_ACCESSORY_SLOT,0,new ItemStack(local.primordial.boss.SinBosses.SEVEN_SINS.get()));
        TestSlots.tick(player);
        helper.assertTrue(local.primordial.boss.SevenSinsItem.wears(player),"wearing the Seven Sins");
        var zombie=helper.spawn(EntityType.ZOMBIE,1,2,1);AffixState.suppress(zombie);
        zombie.setHealth(zombie.getMaxHealth()*0.3F);
        zombie.hurt(player.damageSources().playerAttack(player),zombie.getMaxHealth()*0.12F);
        helper.assertTrue(!zombie.isAlive(),"Lust executes at 20% for the Seven Sins' wearer");
        helper.succeed();
    }
}
