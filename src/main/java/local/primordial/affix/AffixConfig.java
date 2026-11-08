package local.primordial.affix;

import net.minecraftforge.common.ForgeConfigSpec;

public final class AffixConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue ENABLED, BLOCK_CONSUMPTION, DAMAGE_FEEDBACK, DAMAGE_FEEDBACK_SOUNDS;
    public static final ForgeConfigSpec.IntValue RADIUS;
    static {
        var builder=new ForgeConfigSpec.Builder();builder.push("levelAffixes");
        ENABLED=builder.comment("Enable natural hostile mob affixes and boss affixes.").define("enabled",true);
        // renamed from playerRadius (64), so existing config files pick up the wider default: natural spawns happen
        // up to 128 blocks from a player, and with 64 most of them had no player in range and spawned without affixes
        RADIUS=builder.comment("Highest experience level of survival/adventure players within this radius is frozen when a mob gets its affixes.").defineInRange("playerSearchRadius",128,16,256);
        BLOCK_CONSUMPTION=builder.comment("Gluttony consumes whitelisted terrain materials in all chunks, including player-placed blocks; respects mobGriefing.").define("gluttonyConsumesBlocks",true);
        builder.pop();builder.push("damageFeedback");
        DAMAGE_FEEDBACK=builder.comment("Particles where a hit lands, by its kind: physical, magic, true, fixed, adaptive.").define("particles",true);
        DAMAGE_FEEDBACK_SOUNDS=builder.comment("A sound for magic, true, fixed and adaptive hits (physical keeps the game's own).").define("sounds",true);
        builder.pop();SPEC=builder.build();
    }
    private AffixConfig() {}
}
