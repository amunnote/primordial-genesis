package local.primordial.zhuque;

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

public final class ZhuqueWorldgen {
    public static final ResourceKey<Biome> BIOME=ResourceKey.create(Registries.BIOME,id("zhuque_sun_fields"));
    public static ResourceLocation id(String path){return new ResourceLocation(Primordial.ID,path);}
    private static final DeferredRegister<Feature<?>> FEATURES=DeferredRegister.create(Registries.FEATURE,Primordial.ID);
    private static final DeferredRegister<StructureType<?>> STRUCTURES=DeferredRegister.create(Registries.STRUCTURE_TYPE,Primordial.ID);
    private static final DeferredRegister<StructurePieceType> PIECES=DeferredRegister.create(Registries.STRUCTURE_PIECE,Primordial.ID);
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> SUNSET_WUTONG=FEATURES.register("sunset_wutong",()->new SunFieldsFeature(SunFieldsFeature.Kind.TREE));
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> RED_EARTH_KNOLL=FEATURES.register("red_earth_knoll",()->new SunFieldsFeature(SunFieldsFeature.Kind.KNOLL));
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> SUN_SANDSTONE_OUTCROP=FEATURES.register("sun_sandstone_outcrop",()->new SunFieldsFeature(SunFieldsFeature.Kind.ROCK));
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> SHALLOW_EMBER_RIFT=FEATURES.register("shallow_ember_rift",()->new SunFieldsFeature(SunFieldsFeature.Kind.RIFT));
    public static final RegistryObject<StructureType<LingguangCourtStructure>> COURT=STRUCTURES.register("lingguang_court",()->()->LingguangCourtStructure.CODEC);
    public static final RegistryObject<StructurePieceType> COURT_PIECE=PIECES.register("lingguang_court",()->(StructurePieceType.ContextlessType)LingguangCourtStructure.Piece::new);
    public static void register(IEventBus bus){FEATURES.register(bus);STRUCTURES.register(bus);PIECES.register(bus);}
}
