package local.primordial.baihu;

import local.primordial.Primordial;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.*;

public final class BaihuWorldgen {
    public static final ResourceKey<Biome> BIOME=ResourceKey.create(Registries.BIOME,id("baihu_blade_wastes"));
    public static ResourceLocation id(String path){return new ResourceLocation(Primordial.ID,path);}
    private static final DeferredRegister<Feature<?>> FEATURES=DeferredRegister.create(Registries.FEATURE,Primordial.ID);
    private static final DeferredRegister<StructureType<?>> STRUCTURES=DeferredRegister.create(Registries.STRUCTURE_TYPE,Primordial.ID);
    private static final DeferredRegister<StructurePieceType> PIECES=DeferredRegister.create(Registries.STRUCTURE_PIECE,Primordial.ID);
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> BLADE_STONES=FEATURES.register("blade_stones",()->new WastesFeature(WastesFeature.Kind.BLADES));
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> TIGER_CLAW_RIFT=FEATURES.register("tiger_claw_rift",()->new WastesFeature(WastesFeature.Kind.RIFT));
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> BLADESTONE_OUTCROP=FEATURES.register("bladestone_outcrop",()->new WastesFeature(WastesFeature.Kind.ROCK));
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> WASTES_QUARTZ_VEIN=FEATURES.register("wastes_quartz_vein",()->new WastesFeature(WastesFeature.Kind.QUARTZ));
    public static final RegistryObject<StructureType<JianbingCourtStructure>> COURT=STRUCTURES.register("jianbing_court",()->()->JianbingCourtStructure.CODEC);
    public static final RegistryObject<StructurePieceType> COURT_PIECE=PIECES.register("jianbing_court",()->(StructurePieceType.ContextlessType)JianbingCourtStructure.Piece::new);
    public static void register(IEventBus bus){FEATURES.register(bus);STRUCTURES.register(bus);PIECES.register(bus);}
}
