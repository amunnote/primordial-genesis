package local.primordial.affix;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/** One alchemical material per affix, dropped by mobs that carry it; boss affixes have rarer ones. */
public final class AffixMaterials {
    public static final Map<MobAffix,RegistryObject<Item>> ITEMS=new EnumMap<>(MobAffix.class);
    public static final Map<BossAffix,RegistryObject<Item>> BOSS_ITEMS=new EnumMap<>(BossAffix.class);
    /** per mob, for an ordinary mob killed by a player: 5%, +1.5% per Looting level */
    public static final double CHANCE=0.05,LOOTING_BONUS=0.015;
    public static double chance(int looting){return Math.min(1,CHANCE+LOOTING_BONUS*Math.max(0,looting));}
    private AffixMaterials() {}

    public static String id(MobAffix affix) {
        return switch(affix) {
            case ARMOR_PIERCE -> "piercing_thorn_shard";
            case CAUSALITY -> "causal_link";
            case SUMMON -> "spirit_bell";
            case REFLECTION -> "reflective_gold_film";
            case DAMAGE_CAP -> "threshold_gold_leaf";
            case STRENGTH -> "brute_copper_marrow";
            case RESISTANCE -> "ward_alloy_plate";
            case PHYSICAL_RESISTANCE -> "shockproof_copper_foil";
            case THUNDER -> "latent_thunder_core";
            case SHARED -> "shared_fate_wire";
            case MALICE -> "malice_copper_spike";
            case RUIN -> "ruin_gold_sand";
            case SPACE -> "rift_prism_shard";
            case TIME -> "reversal_crystal_sand";
            case REGENERATION -> "regrowth_jade";
            case CURSE -> "curse_amethyst";
            case SEAL -> "sealing_crystal_core";
            case MAGIC -> "arcane_crystal";
            case SPLIT -> "twin_split_crystal";
            case MAGIC_RESISTANCE -> "silencing_quartz";
            case SLOWNESS -> "stasis_bead";
            case HEALING -> "soothing_jade_drop";
            case DESATURATION -> "fading_crystal_salt";
            case VOID -> "lightless_crystal";
            case SHATTER -> "shieldbreaker_wedge";
            case UNDYING -> "undying_gold_calyx";
            case BLOOD_PACT -> "blood_oath_crystal";
            case GROWTH -> "sprouting_copper_bud";
            case ENLIGHTENMENT -> "insight_crystal_cluster";
            case BEFUDDLEMENT -> "muddled_spiral_crystal";
            case AMPLIFY -> "resonant_silver_plate";
            case MASTERY -> "twinfold_alloy";
        };
    }
    public static String id(BossAffix affix) {
        return switch(affix) {
            case PRIDE -> "usurper_crown_shard";
            case GREED -> "greed_gold_marrow";
            case SLOTH -> "stasis_crystal_marrow";
            case ENVY -> "mimic_mirror_crystal";
            case WRATH -> "seething_copper_heart";
            case GLUTTONY -> "devouring_core";
            case LUST -> "beguiling_crystal_tear";
        };
    }
    public static void register(DeferredRegister<Item> items) {
        for(var affix:MobAffix.values())ITEMS.put(affix,items.register(id(affix),
            ()->new Material(id(affix),affix::tag,Rarity.UNCOMMON,false)));
        for(var affix:BossAffix.values())BOSS_ITEMS.put(affix,items.register(id(affix),
            ()->new Material(id(affix),affix::tag,Rarity.RARE,true)));
    }

    /** the killer's luck needed for ordinary affix materials, and for a boss affix's material */
    public static final double LUCK_REQUIRED=20,BOSS_LUCK_REQUIRED=100;
    /** the killer's experience level needed for ordinary affix materials */
    public static final int LEVEL_REQUIRED=20;
    /**
     * Drops for a dying affixed mob killed by a player. One ordinary material at most, for one of its own affixes picked
     * at random: a 5% chance (+1.5% per Looting level), always for a boss. Needs a killer
     * with 20 luck and 20 experience levels. A boss also drops its boss affix's material for a killer with 100 luck.
     * Split clones give none.
     */
    public static void drop(AffixState state,int looting,net.minecraft.world.entity.player.Player killer,java.util.Collection<ItemEntity> drops) {
        var mob=state.mob;
        if(killer==null||state.tag.getBoolean("clone"))return;
        double luck=killer.getLuck();
        var own=new java.util.ArrayList<MobAffix>();
        for(var affix:state.affixes)if(!state.temporary.contains(affix))own.add(affix);   // lent by Amplify, not its own
        boolean rolled=state.originalBoss!=null||mob.getRandom().nextDouble()<chance(looting)*local.primordial.spirit.SpiritRealm.materialMultiplier(killer,mob);
        if(rolled&&!own.isEmpty()&&luck>=LUCK_REQUIRED&&killer.experienceLevel>=LEVEL_REQUIRED){
            var affix=own.get(mob.getRandom().nextInt(own.size()));
            var drop=new ItemEntity(mob.level(),mob.getX(),mob.getY(),mob.getZ(),new ItemStack(ITEMS.get(affix).get()));
            drop.setDefaultPickUpDelay();drops.add(drop);
        }
        // a boss bearing seven affixes (its sin's among them), felled by someone of level 50 or more: its sin's sigil
        if(state.originalBoss!=null&&own.size()+1>=local.primordial.boss.SinSigilItem.AFFIXES_REQUIRED
            &&killer.experienceLevel>=local.primordial.boss.SinSigilItem.LEVEL_REQUIRED){
            var drop=new ItemEntity(mob.level(),mob.getX(),mob.getY(),mob.getZ(),new ItemStack(local.primordial.boss.SinBosses.SIGILS.get(state.originalBoss).get()));
            drop.setDefaultPickUpDelay();drops.add(drop);
        }
        // the Warden, the Wither or the Ender Dragon bearing seven affixes: its world's soul
        local.primordial.effect.WorldCores.drop(state,own.size()+(state.originalBoss!=null?1:0),killer,drops);
        if(state.originalBoss!=null&&luck>=BOSS_LUCK_REQUIRED){
            var drop=new ItemEntity(mob.level(),mob.getX(),mob.getY(),mob.getZ(),new ItemStack(BOSS_ITEMS.get(state.originalBoss).get()));
            drop.setDefaultPickUpDelay();drops.add(drop);
        }
    }

    public static final class Material extends Item {
        private final String id;
        private final java.util.function.Supplier<Component> affixTag;
        private final boolean boss;
        Material(String id,java.util.function.Supplier<Component> affixTag,Rarity rarity,boolean boss) {
            super(new Properties().rarity(rarity));this.id=id;this.affixTag=affixTag;this.boss=boss;
        }
        @Override public void appendHoverText(ItemStack stack,Level level,List<Component> tooltip,TooltipFlag flag) {
            tooltip.add(Component.translatable("tooltip.primordial.material."+id).withStyle(ChatFormatting.GRAY,ChatFormatting.ITALIC));
            tooltip.add(Component.translatable("tooltip.primordial.material.source",affixTag.get()).withStyle(ChatFormatting.DARK_GRAY));
            if(boss)tooltip.add(Component.translatable("tooltip.primordial.material.luck",(int)BOSS_LUCK_REQUIRED).withStyle(ChatFormatting.DARK_GREEN));
            else tooltip.add(Component.translatable("tooltip.primordial.material.luck_level",(int)LUCK_REQUIRED,LEVEL_REQUIRED).withStyle(ChatFormatting.DARK_GREEN));
        }
    }
}
