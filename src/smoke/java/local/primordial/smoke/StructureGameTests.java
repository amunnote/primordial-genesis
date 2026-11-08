package local.primordial.smoke;

import local.primordial.Primordial;
import local.primordial.affix.AffixState;
import local.primordial.item.AlchemyRecipeItem;
import local.primordial.structure.AlchemistStructureEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("primordial")
@PrefixGameTestTemplate(false)
public final class StructureGameTests {
    static final String[] BUILDINGS={"alchemist_cottage","abandoned_workshop","stargazer_tower","nether_forge","primordial_sanctum","alchemist_camp"};
    static final String[] TABLES={"alchemist_cottage","alchemist_cottage_house","abandoned_workshop","abandoned_workshop_lab","stargazer_tower","stargazer_tower_top",
        "nether_forge","primordial_sanctum","alchemist_camp"};

    @GameTest(template="empty") public static void buildingsAreRegistered(GameTestHelper helper) {
        var level=helper.getLevel();var access=level.registryAccess();
        for(var id:BUILDINGS){
            var rl=new ResourceLocation(Primordial.ID,id);
            var template=level.getStructureManager().get(rl);
            helper.assertTrue(template.isPresent()&&template.get().getSize().getX()>0,"template "+id);
            helper.assertTrue(access.registryOrThrow(Registries.STRUCTURE).containsKey(rl),"structure "+id);
            helper.assertTrue(access.registryOrThrow(Registries.STRUCTURE_SET).containsKey(rl),"structure set "+id);
            helper.assertTrue(access.registryOrThrow(Registries.TEMPLATE_POOL).containsKey(new ResourceLocation(Primordial.ID,id+"/main")),"pool "+id);
        }
        for(var table:TABLES){
            var loot=level.getServer().getLootData().getLootTable(new ResourceLocation(Primordial.ID,"chests/"+table));
            helper.assertTrue(loot!=LootTable.EMPTY,"loot table "+table);
            var params=new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN,helper.absoluteVec(new net.minecraft.world.phys.Vec3(1,2,1))).create(LootContextParamSets.CHEST);
            helper.assertTrue(!loot.getRandomItems(params).isEmpty(),"loot table "+table+" gives items");
        }
        helper.succeed();
    }

    @GameTest(template="empty") public static void sanctumHoldsRecipes(GameTestHelper helper) {
        var level=helper.getLevel();
        var loot=level.getServer().getLootData().getLootTable(new ResourceLocation(Primordial.ID,"chests/primordial_sanctum"));
        int withRecipe=0,chests=600;
        for(int i=0;i<chests;i++){
            var params=new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN,helper.absoluteVec(new net.minecraft.world.phys.Vec3(1,2,1))).create(LootContextParamSets.CHEST);
            long recipes=loot.getRandomItems(params).stream().filter(s->s.getItem() instanceof AlchemyRecipeItem).count();
            helper.assertTrue(recipes<=1,"at most one recipe per chest");if(recipes==1)withRecipe++;
        }
        // 1-(0.2*0.5*0.5*0.84*0.8) = 97%
        double share=withRecipe/(double)chests;
        helper.assertTrue(share>0.9,"sanctum chests: ~97% hold a recipe, got "+share);
        helper.succeed();
    }

    @GameTest(template="empty") public static void guardiansGetTheirAffixes(GameTestHelper helper) {
        var zombie=EntityType.ZOMBIE.create(helper.getLevel());zombie.moveTo(helper.absoluteVec(new net.minecraft.world.phys.Vec3(1,2,1)));
        var spec=new CompoundTag();spec.putInt("level",30);spec.putInt("count",3);
        zombie.getPersistentData().put(AlchemistStructureEvents.GUARDIAN,spec);
        AlchemistStructureEvents.guardian(zombie);
        var state=AffixState.get(zombie);
        helper.assertTrue(state!=null&&state.affixes.size()==3&&state.level==30,"workshop guard: 3 affixes at level 30");
        helper.assertTrue(!zombie.getPersistentData().contains(AlchemistStructureEvents.GUARDIAN),"guardian spec is used up");
        zombie.discard();

        var evoker=EntityType.EVOKER.create(helper.getLevel());evoker.moveTo(helper.absoluteVec(new net.minecraft.world.phys.Vec3(1,2,1)));
        var boss=new CompoundTag();boss.putInt("level",60);boss.putInt("count",7);boss.putBoolean("boss",true);boss.putString("name","entity.primordial.primordial_guardian");
        evoker.getPersistentData().put(AlchemistStructureEvents.GUARDIAN,boss);
        helper.getLevel().addFreshEntity(evoker);   // the join handler sets it up
        var guardian=AffixState.get(evoker);
        helper.assertTrue(guardian!=null&&guardian.originalBoss!=null&&guardian.affixes.size()>=5,"sanctum guardian: boss affix and many affixes");
        helper.assertTrue(evoker.isPersistenceRequired(),"guardian does not despawn");
        var name=AffixState.givenName(evoker);
        helper.assertTrue(name!=null&&name.getContents() instanceof TranslatableContents t&&t.getKey().equals("entity.primordial.primordial_guardian"),"named Primordial Guardian");
        evoker.discard();
        helper.succeed();
    }

    @GameTest(template="empty") public static void campAlchemistTrades(GameTestHelper helper) {
        var level=helper.getLevel();
        var template=level.getStructureManager().get(new ResourceLocation(Primordial.ID,"alchemist_camp")).orElseThrow();
        BlockPos origin=helper.absolutePos(new BlockPos(0,1,0)).offset(0,0,40);
        template.placeInWorld(level,origin,origin,new StructurePlaceSettings(),level.getRandom(),2);
        var villagers=level.getEntitiesOfClass(Villager.class,new net.minecraft.world.phys.AABB(origin).inflate(12));
        helper.assertTrue(villagers.size()==1,"camp has one alchemist, found "+villagers.size());
        var alchemist=villagers.get(0);
        helper.assertTrue(alchemist.isNoAi()&&alchemist.isPersistenceRequired(),"the alchemist stays put");
        helper.assertTrue(alchemist.getCustomName()!=null&&alchemist.getCustomName().getContents() instanceof TranslatableContents t
            &&t.getKey().equals("entity.primordial.wandering_alchemist"),"named Wandering Alchemist");
        var offers=alchemist.getOffers();
        helper.assertTrue(offers.size()==9,"9 offers, got "+offers.size());
        long recipes=offers.stream().filter(o->o.getResult().getItem() instanceof AlchemyRecipeItem).count();
        helper.assertTrue(recipes==5,"5 recipe offers, got "+recipes);
        helper.assertTrue(offers.stream().anyMatch(o->o.getResult().is(Primordial.ALCHEMICAL_ESSENCE.get())),"sells essence");
        // joining again (e.g. after reloading) keeps the same offers
        var first=offers.get(0).getResult().copy();AlchemistStructureEvents.wanderingAlchemist(alchemist);
        helper.assertTrue(net.minecraft.world.item.ItemStack.matches(first,alchemist.getOffers().get(0).getResult()),"offers are generated once");
        for(var e:level.getEntitiesOfClass(net.minecraft.world.entity.Entity.class,new net.minecraft.world.phys.AABB(origin).inflate(12),e->!(e instanceof net.minecraft.world.entity.player.Player)))e.discard();
        helper.succeed();
    }
}
