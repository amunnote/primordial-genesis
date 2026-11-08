package local.primordial.qinglong;

import local.primordial.Primordial;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/** The landmarks and ruins shared out among the four guardians' lands (GuardianLandFeature, GuardianShrineFeature). */
public final class GuardianWorldgen {
    private static final DeferredRegister<Feature<?>> FEATURES=DeferredRegister.create(Registries.FEATURE,Primordial.ID);
    static {
        for(var kind:GuardianLandFeature.Kind.values())land(kind);
        for(var kind:GuardianShrineFeature.Kind.values())FEATURES.register(kind.name().toLowerCase(java.util.Locale.ROOT),()->new GuardianShrineFeature(kind));
    }
    private static RegistryObject<Feature<NoneFeatureConfiguration>> land(GuardianLandFeature.Kind kind) {
        return FEATURES.register(kind.name().toLowerCase(java.util.Locale.ROOT),()->new GuardianLandFeature(kind));
    }
    private GuardianWorldgen() {}
    public static void register(IEventBus bus) {FEATURES.register(bus);}
}
