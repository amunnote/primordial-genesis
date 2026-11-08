package local.primordial.qinglong;

import local.primordial.Primordial;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.*;

public final class QinglongWorldgen {
    public static final ResourceKey<Biome> BIOME=ResourceKey.create(Registries.BIOME,id("qinglong_thunder_grove"));
    public static ResourceLocation id(String path){return new ResourceLocation(Primordial.ID,path);}
    private static final DeferredRegister<Feature<?>> FEATURES=DeferredRegister.create(Registries.FEATURE,Primordial.ID);
    private static final DeferredRegister<StructureType<?>> STRUCTURES=DeferredRegister.create(Registries.STRUCTURE_TYPE,Primordial.ID);
    private static final DeferredRegister<StructurePieceType> PIECES=DeferredRegister.create(Registries.STRUCTURE_PIECE,Primordial.ID);
    private static final DeferredRegister<StructurePlacementType<?>> PLACEMENTS=DeferredRegister.create(Registries.STRUCTURE_PLACEMENT,Primordial.ID);
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> SPINE=FEATURES.register("dragon_spine",DragonSpineFeature::new);
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> SPIRE=FEATURES.register("copper_spire",()->new GroveFeature(GroveFeature.Kind.SPIRE));
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> TREE=FEATURES.register("grove_giant_oak",()->new GroveFeature(GroveFeature.Kind.TREE));
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> ROCK=FEATURES.register("cloudrock_layer",()->new GroveFeature(GroveFeature.Kind.ROCK));
    public static final RegistryObject<StructureType<MengzhangCourtStructure>> COURT=STRUCTURES.register("mengzhang_court",()->()->MengzhangCourtStructure.CODEC);
    public static final RegistryObject<StructurePieceType> COURT_PIECE=PIECES.register("mengzhang_court",()->(StructurePieceType.ContextlessType)MengzhangCourtStructure.Piece::new);
    public static final RegistryObject<StructurePlacementType<HalfPlanePlacement>> HALF_PLANE=PLACEMENTS.register("half_plane",()->()->HalfPlanePlacement.CODEC);
    public static final RegistryObject<StructurePlacementType<GuardianPlacement>> GUARDIAN_SEAT=PLACEMENTS.register("guardian_seat",()->()->GuardianPlacement.CODEC);
    public static void register(IEventBus bus){FEATURES.register(bus);STRUCTURES.register(bus);PIECES.register(bus);PLACEMENTS.register(bus);GuardianWorldgen.register(bus);}
    // The four lands are laid over the overworld by GuardianAnchors, and their ground by GuardianSurface.
}
