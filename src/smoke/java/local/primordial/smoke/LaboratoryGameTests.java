package local.primordial.smoke;

import local.primordial.Primordial;
import local.primordial.altar.AltarBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("primordial")
@PrefixGameTestTemplate(false)
public final class LaboratoryGameTests {
    static final String[] VARIANTS={"temperate","arid","cold","lush"};
    static final ResourceLocation LOOT=new ResourceLocation(Primordial.ID,"chests/alchemy_laboratory");

    @GameTest(template="empty",timeoutTicks=200) public static void laboratoryTemplatesAndStructures(GameTestHelper helper) {
        var level=helper.getLevel();
        var templates=level.getStructureManager();
        var structures=level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        for(int i=0;i<VARIANTS.length;i++){
            String v=VARIANTS[i];
            var template=templates.get(new ResourceLocation(Primordial.ID,"alchemy_laboratory/"+v)).orElse(null);
            helper.assertTrue(template!=null,"template "+v+" loads");
            helper.assertTrue(template.getSize().getX()==13&&template.getSize().getZ()==11,"template "+v+" is 13x11");
            // place it far below the test area and inspect what it put down
            BlockPos origin=new BlockPos(1000+i*20,-50,1000);
            template.placeInWorld(level,origin,origin,new StructurePlaceSettings(),level.random,2);
            helper.assertTrue(level.getBlockEntity(origin.offset(6,1,5)) instanceof AltarBlockEntity,v+": altar in the middle");
            for(BlockPos c:new BlockPos[]{origin.offset(2,1,4),origin.offset(10,1,6)}){
                helper.assertTrue(level.getBlockEntity(c) instanceof ChestBlockEntity,v+": chest at "+c);
                var tag=level.getBlockEntity(c).saveWithoutMetadata();
                helper.assertTrue(LOOT.toString().equals(tag.getString("LootTable")),v+": chest uses the laboratory loot table, got "+tag);
            }
            var structure=structures.get(ResourceKey.create(Registries.STRUCTURE,new ResourceLocation(Primordial.ID,"alchemy_laboratory_"+v)));
            helper.assertTrue(structure!=null,"structure "+v+" registered");
            // the same call /place structure makes (biome check bypassed)
            var generator=level.getChunkSource().getGenerator();
            var start=structure.generate(level.registryAccess(),generator,generator.getBiomeSource(),level.getChunkSource().randomState(),
                templates,level.getSeed(),new ChunkPos(200+i*4,200),0,level,biome->true);
            helper.assertTrue(start.isValid()&&!start.getPieces().isEmpty(),"structure "+v+" generates pieces");
        }
        var loot=level.getServer().getLootData().getLootTable(LOOT);
        helper.assertTrue(loot!=net.minecraft.world.level.storage.loot.LootTable.EMPTY,"laboratory loot table exists");
        helper.succeed();
    }
}
