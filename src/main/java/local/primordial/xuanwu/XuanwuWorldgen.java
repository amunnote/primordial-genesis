package local.primordial.xuanwu;

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

public final class XuanwuWorldgen {
    public static final ResourceKey<Biome> BIOME=ResourceKey.create(Registries.BIOME,id("xuanwu_tide_sea"));
    public static ResourceLocation id(String path){return new ResourceLocation(Primordial.ID,path);}
    private static final DeferredRegister<Feature<?>> FEATURES=DeferredRegister.create(Registries.FEATURE,Primordial.ID);
    private static final DeferredRegister<StructureType<?>> STRUCTURES=DeferredRegister.create(Registries.STRUCTURE_TYPE,Primordial.ID);
    private static final DeferredRegister<StructurePieceType> PIECES=DeferredRegister.create(Registries.STRUCTURE_PIECE,Primordial.ID);
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> REEF=FEATURES.register("turtleback_reef",()->new TideSeaFeature(TideSeaFeature.Kind.REEF));
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> ICE=FEATURES.register("tide_ice_spire",()->new TideSeaFeature(TideSeaFeature.Kind.ICE));
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> ROCK=FEATURES.register("shell_rock_outcrop",()->new TideSeaFeature(TideSeaFeature.Kind.ROCK));
    public static final RegistryObject<StructureType<ZhimingCourtStructure>> COURT=STRUCTURES.register("zhiming_court",()->()->ZhimingCourtStructure.CODEC);
    public static final RegistryObject<StructurePieceType> COURT_PIECE=PIECES.register("zhiming_court",()->(StructurePieceType.ContextlessType)ZhimingCourtStructure.Piece::new);
    public static void register(IEventBus bus){FEATURES.register(bus);STRUCTURES.register(bus);PIECES.register(bus);}
}
