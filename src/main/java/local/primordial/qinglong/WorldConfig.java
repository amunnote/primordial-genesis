package local.primordial.qinglong;

import net.minecraftforge.common.ForgeConfigSpec;

/** primordial-world.toml: where the four lands lie, how often the Bifang comes, and what the lands do to those in them. */
public final class WorldConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.DoubleValue TERRITORY_CHANCE,TIGER_MELEE_BONUS,BIRD_IGNITE_CHANCE,BIFANG_SPAWN_CHANCE;
    public static final ForgeConfigSpec.IntValue TERRITORY_MIN_RADIUS,TERRITORY_MAX_RADIUS,SEAT_DISTANCE,TORTOISE_EXTRA_AIR,DRAGON_HEAL_SECONDS;
    static {
        var builder=new ForgeConfigSpec.Builder();
        builder.comment("Where the four guardians' lands lie. Changing these in a world already explored leaves seams where new chunks meet old.").push("guardianLands");
        SEAT_DISTANCE=builder.comment("Nearest distance from spawn of each guardian's court; it is sought in rings 1600 blocks wide from here. A world's courts are chosen once and kept in its save.")
            .defineInRange("courtDistance",1600,400,20000);
        TERRITORY_CHANCE=builder.comment("Share of the 2560-block cells that hold a guardian territory (0 for none beyond the courts).").defineInRange("territoryChance",0.5,0,1);
        TERRITORY_MIN_RADIUS=builder.comment("Smallest radius of a territory, in blocks, before its edge wavers.").defineInRange("territoryMinRadius",520,64,800);
        TERRITORY_MAX_RADIUS=builder.comment("Largest radius of a territory, in blocks (raised to the smallest if below it). Up to 800, so a territory stays inside its cell.").defineInRange("territoryMaxRadius",740,64,800);
        builder.pop();
        builder.comment("What each guardian's land does to those in it.").push("landEffects");
        TIGER_MELEE_BONUS=builder.comment("Blade Wastes: extra share of harm on every melee blow (0.15 = +15%).").defineInRange("bladeWastesMeleeBonus",0.15,0,5);
        BIRD_IGNITE_CHANCE=builder.comment("Sun Fields: chance a melee hit sets the struck alight for 3 seconds.").defineInRange("sunFieldsIgniteChance",1/7.0,0,1);
        TORTOISE_EXTRA_AIR=builder.comment("Tide Sea: extra breath lost each tick under its water (1 = twice as fast, 0 = as anywhere).").defineInRange("tideSeaExtraAirLoss",1,0,4);
        DRAGON_HEAL_SECONDS=builder.comment("Thunder Grove: seconds between each half heart healed (0 to turn it off).").defineInRange("thunderGroveHealSeconds",10,0,600);
        builder.pop();
        builder.push("creatures");
        BIFANG_SPAWN_CHANCE=builder.comment("Share of the natural spawn chances a Bifang takes in the Sun Fields.").defineInRange("bifangSpawnChance",0.25,0,1);
        builder.pop();
        SPEC=builder.build();
    }
    private WorldConfig() {}

    /** a value, or its default when the config is not loaded yet (worldgen in tests, or before the file is read) */
    static double get(ForgeConfigSpec.DoubleValue value) {return SPEC.isLoaded()?value.get():value.getDefault();}
    static int get(ForgeConfigSpec.IntValue value) {return SPEC.isLoaded()?value.get():value.getDefault();}
    public static double bifangSpawnChance() {return get(BIFANG_SPAWN_CHANCE);}
}
