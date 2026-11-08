package local.primordial.smoke;

import local.primordial.Primordial;
import local.primordial.spirit.SpiritIslands;
import local.primordial.spirit.SpiritRealm;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("primordial")
@PrefixGameTestTemplate(false)
public final class SpiritRealmGameTests {
    @GameTest(template="empty") public static void inspirationIsBrewed(GameTestHelper helper) {
        var awkward=PotionUtils.setPotion(new ItemStack(Items.POTION),Potions.AWKWARD);
        var out=BrewingRecipeRegistry.getOutput(awkward,new ItemStack(Primordial.ALCHEMICAL_ESSENCE.get()));
        helper.assertTrue(PotionUtils.getPotion(out)==SpiritRealm.INSPIRATION_POTION.get(),"awkward potion + essence = Inspiration, got "+out);
        var effects=PotionUtils.getMobEffects(out);
        helper.assertTrue(effects.size()==1&&effects.get(0).getEffect()==SpiritRealm.INSPIRATION.get()&&effects.get(0).getDuration()==3600,"three minutes of Inspiration");
        helper.succeed();
    }

    @GameTest(template="empty") public static void inspirationAndShatter(GameTestHelper helper) {
        var player=helper.makeMockSurvivalPlayer();
        helper.assertTrue(SpiritRealm.materialMultiplier(player,null)==1,"no bonus uninspired");
        player.addEffect(new MobEffectInstance(SpiritRealm.INSPIRATION.get(),3600));
        helper.assertTrue(SpiritRealm.materialMultiplier(player,null)==2,"twice the affix materials when inspired");
        double before=player.getAttributeValue(Attributes.MAX_HEALTH),attack=player.getAttributeValue(Attributes.MOVEMENT_SPEED);
        player.addEffect(new MobEffectInstance(SpiritRealm.SPIRIT_SHATTER.get(),SpiritRealm.SHATTER_TICKS));
        helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.MAX_HEALTH)-before*0.8)<1e-6,"Spirit Shatter: max health -20%");
        helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.MOVEMENT_SPEED)-attack*0.8)<1e-6,"and speed -20%");
        helper.assertTrue(SpiritRealm.SPIRIT_SHATTER.get().getCurativeItems().isEmpty(),"milk does not cure it");
        helper.succeed();
    }

    @GameTest(template="empty",timeoutTicks=400) public static void theRealmIsIslands(GameTestHelper helper) {
        var realm=helper.getLevel().getServer().getLevel(SpiritRealm.LEVEL);
        helper.assertTrue(realm!=null,"the Spirit Realm exists");
        var island=SpiritIslands.arrival();
        int x=(int)Math.floor(island.centerX()),z=(int)Math.floor(island.centerZ());
        realm.getChunk(x>>4,z>>4);
        int top=realm.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z);
        helper.assertTrue(top>60&&top<130,"the arrival island stands in the sky, top at "+top);
        helper.assertTrue(realm.getBiome(new BlockPos(x,top,z)).is(Biomes.PLAINS),"the arrival island is plains");
        helper.assertTrue(realm.getBlockState(new BlockPos(x,top-1,z)).isSolid()||!realm.getBlockState(new BlockPos(x,top-1,z)).isAir(),"ground under the arrival point");
        // beside the islands there is nothing
        int far=x+SpiritIslands.CELL/2;var gap=SpiritIslands.islandAt(far,z+SpiritIslands.CELL/2);
        helper.assertTrue(gap==null||gap.cellX()!=0||gap.cellZ()!=0,"islands stay in their cells");
        // islands from other worlds appear
        boolean nether=false,end=false;
        var source=(local.primordial.spirit.SpiritBiomeSource)realm.getChunkSource().getGenerator().getBiomeSource();
        for(int cx=-12;cx<=12;cx++)for(int cz=-12;cz<=12;cz++){
            var biome=source.biomeOfCell(cx,cz);
            nether|=biome.is(net.minecraft.tags.BiomeTags.IS_NETHER);end|=biome.is(net.minecraft.tags.BiomeTags.IS_END);
        }
        helper.assertTrue(nether&&end,"nether and end islands among the overworld ones");
        // a creature there is a spirit
        var zombie=EntityType.ZOMBIE.create(realm);
        zombie.moveTo(x+0.5,top,z+0.5,0,0);realm.addFreshEntity(zombie);
        helper.assertTrue(zombie.getPersistentData().getBoolean(SpiritRealm.SPIRIT)&&zombie.getMaxHealth()>=29.9F,"a spirit: x1.5 health, got "+zombie.getMaxHealth());
        zombie.discard();
        helper.assertTrue(!local.primordial.spirit.SpiritSpawner.pool().isEmpty()&&local.primordial.spirit.SpiritSpawner.pool().contains(EntityType.BLAZE)
            &&!local.primordial.spirit.SpiritSpawner.pool().contains(EntityType.WITHER),"spirits from every world, but no bosses");
        helper.succeed();
    }

    @GameTest(template="empty",timeoutTicks=600) public static void buildingsFloatAbove(GameTestHelper helper) {
        var realm=helper.getLevel().getServer().getLevel(SpiritRealm.LEVEL);
        // the arrival island always has a sanctum overhead, its chest on the altar, and the arrival point stays clear of it
        var sanctum=(local.primordial.spirit.SpiritSkyStructures.Sanctum)local.primordial.spirit.SpiritSkyStructures.piecesOfCell(0,0).get(0);
        var altar=sanctum.chests().get(0);
        realm.getChunk(altar.getX()>>4,altar.getZ()>>4);
        helper.assertTrue(realm.getBlockState(altar).is(net.minecraft.world.level.block.Blocks.CHEST),"a chest on the sanctum's altar at "+altar);
        helper.assertTrue(realm.getBlockState(altar.below()).is(net.minecraft.world.level.block.Blocks.SMOOTH_QUARTZ)||realm.getBlockState(altar.below()).is(net.minecraft.world.level.block.Blocks.PURPUR_BLOCK),"standing on the platform");
        var island=SpiritIslands.arrival();
        int x=(int)Math.floor(island.centerX()),z=(int)Math.floor(island.centerZ());
        helper.assertTrue(realm.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z)<altar.getY()-10,"the arrival point is under open sky");
        // an empty cell holds a sin spire, its chest at the foot
        int spires=0;
        for(int cx=-2;cx<=2;cx++)for(int cz=-2;cz<=2;cz++){
            if(SpiritIslands.island(cx,cz)!=null)continue;
            var spire=local.primordial.spirit.SpiritSkyStructures.piecesOfCell(cx,cz).get(0).chests().get(0);
            realm.getChunk(spire.getX()>>4,spire.getZ()>>4);
            helper.assertTrue(realm.getBlockState(spire).is(net.minecraft.world.level.block.Blocks.CHEST),"a chest in the spire of cell "+cx+","+cz+" at "+spire+", found "+realm.getBlockState(spire));
            spires++;
        }
        helper.assertTrue(spires>0,"some cell nearby is empty");
        helper.succeed();
    }
}
