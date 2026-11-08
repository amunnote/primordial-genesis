package local.primordial.smoke;

import local.primordial.Primordial;
import local.primordial.altar.AltarBlockEntity;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Leaves stocked altars just south of world spawn, so the saved test world can be opened in a client to look at them. */
@GameTestHolder("primordial")
@PrefixGameTestTemplate(false)
public final class ShowcaseGameTests {
    @GameTest(template="empty") public static void altarShowcase(GameTestHelper helper) {
        var level=helper.getLevel();
        var spawn=level.getSharedSpawnPos();
        var base=new net.minecraft.core.BlockPos(spawn.getX(),150,spawn.getZ()+3);
        for(int dx=-2;dx<=4;dx++)for(int dz=-2;dz<=2;dz++)
            level.setBlockAndUpdate(base.offset(dx,-1,dz),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        org.slf4j.LoggerFactory.getLogger("primordial").info("SHOWCASE at {} {} {}",base.getX(),base.getY(),base.getZ());
        var blocks=new net.minecraft.world.level.block.Block[]{Primordial.PRIMORDIAL_ALCHEMY_ALTAR.get(),Primordial.BASIC_ALCHEMY_ALTAR.get()};
        for(int i=0;i<blocks.length;i++){
            var pos=base.offset(i*2,0,0);
            level.setBlockAndUpdate(pos,blocks[i].defaultBlockState());
            var altar=(AltarBlockEntity)level.getBlockEntity(pos);
            altar.add(new ItemStack(Items.HEART_OF_THE_SEA));
            for(int k=0;k<(i==0?8:4);k++)altar.add(new ItemStack(Primordial.ALCHEMICAL_SOURCE_INGOT.get()));
        }
        helper.succeed();
    }

    /** The four alchemy laboratory variants in a row on a grass platform at y=200, west to east: temperate, arid, cold, lush. */
    @GameTest(template="empty") public static void laboratoryShowcase(GameTestHelper helper) {
        var level=helper.getLevel();
        var spawn=level.getSharedSpawnPos();
        var base=new net.minecraft.core.BlockPos(spawn.getX()-30,200,spawn.getZ()+20);
        for(int dx=-3;dx<=4*17;dx++)for(int dz=-3;dz<=14;dz++)
            level.setBlock(base.offset(dx,-1,dz),net.minecraft.world.level.block.Blocks.GRASS_BLOCK.defaultBlockState(),2);
        String[] variants={"temperate","arid","cold","lush"};
        for(int i=0;i<variants.length;i++){
            var template=level.getStructureManager().get(new net.minecraft.resources.ResourceLocation(Primordial.ID,"alchemy_laboratory/"+variants[i])).orElseThrow();
            var origin=base.offset(i*17,0,0);
            template.placeInWorld(level,origin,origin,new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings(),level.random,2);
        }
        org.slf4j.LoggerFactory.getLogger("primordial").info("LAB SHOWCASE at {} {} {}",base.getX(),base.getY(),base.getZ());
        helper.succeed();
    }
}
