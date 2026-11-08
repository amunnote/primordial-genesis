package local.primordial.entity.alchemical;

import java.util.List;
import local.primordial.Primordial;
import local.primordial.alchemy.AlchemyKnowledge;
import local.primordial.alchemy.AlchemyWorldKnowledge;
import local.primordial.item.AlchemyRecipeItem;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegistryObject;

/**
 * The alchemical creations: entity types, spawn eggs, attributes and spawn rules. The overworld's spawn throughout the overworld
 * (not on mushroom islands) like other monsters, at about two thirds of the weight of the vanilla monster each one is made from, by the
 * biome modifier forge/biome_modifier/alchemical_creations.json; the Nether's in the Nether biomes their kin haunt, by the
 * alchemical_nether_*.json modifiers (the blaze and the wither skeleton, which vanilla keeps to fortresses, roam the wastes too).
 * Killed by a player, one may drop the recipe of an affix accessory that not everyone in the world knows yet.
 */
public final class AlchemicalCreations {
    public static final RegistryObject<EntityType<AlchemicalZombie>> ZOMBIE=Primordial.ENTITIES.register("alchemical_zombie",
        ()->EntityType.Builder.<AlchemicalZombie>of(AlchemicalZombie::new,MobCategory.MONSTER).sized(0.6F,1.95F).clientTrackingRange(8)
            .build(Primordial.ID+":alchemical_zombie"));
    public static final RegistryObject<EntityType<AlchemicalSkeleton>> SKELETON=Primordial.ENTITIES.register("alchemical_skeleton",
        ()->EntityType.Builder.<AlchemicalSkeleton>of(AlchemicalSkeleton::new,MobCategory.MONSTER).sized(0.6F,1.99F).clientTrackingRange(8)
            .build(Primordial.ID+":alchemical_skeleton"));
    public static final RegistryObject<EntityType<AlchemicalSpider>> SPIDER=Primordial.ENTITIES.register("alchemical_spider",
        ()->EntityType.Builder.<AlchemicalSpider>of(AlchemicalSpider::new,MobCategory.MONSTER).sized(1.4F,0.9F).clientTrackingRange(8)
            .build(Primordial.ID+":alchemical_spider"));
    public static final RegistryObject<EntityType<AlchemicalCreeper>> CREEPER=Primordial.ENTITIES.register("alchemical_creeper",
        ()->EntityType.Builder.<AlchemicalCreeper>of(AlchemicalCreeper::new,MobCategory.MONSTER).sized(0.6F,1.7F).clientTrackingRange(8)
            .build(Primordial.ID+":alchemical_creeper"));
    public static final RegistryObject<EntityType<AlchemicalEnderman>> ENDERMAN=Primordial.ENTITIES.register("alchemical_enderman",
        ()->EntityType.Builder.<AlchemicalEnderman>of(AlchemicalEnderman::new,MobCategory.MONSTER).sized(0.6F,2.9F).clientTrackingRange(8)
            .build(Primordial.ID+":alchemical_enderman"));
    // the Nether's
    public static final RegistryObject<EntityType<AlchemicalBlaze>> BLAZE=Primordial.ENTITIES.register("alchemical_blaze",
        ()->EntityType.Builder.<AlchemicalBlaze>of(AlchemicalBlaze::new,MobCategory.MONSTER).sized(0.6F,1.8F).fireImmune().clientTrackingRange(8)
            .build(Primordial.ID+":alchemical_blaze"));
    public static final RegistryObject<EntityType<AlchemicalGhast>> GHAST=Primordial.ENTITIES.register("alchemical_ghast",
        ()->EntityType.Builder.<AlchemicalGhast>of(AlchemicalGhast::new,MobCategory.MONSTER).sized(4.0F,4.0F).fireImmune().clientTrackingRange(10)
            .build(Primordial.ID+":alchemical_ghast"));
    public static final RegistryObject<EntityType<AlchemicalWitherSkeleton>> WITHER_SKELETON=Primordial.ENTITIES.register("alchemical_wither_skeleton",
        ()->EntityType.Builder.<AlchemicalWitherSkeleton>of(AlchemicalWitherSkeleton::new,MobCategory.MONSTER).sized(0.7F,1.99F).fireImmune()
            .immuneTo(net.minecraft.world.level.block.Blocks.WITHER_ROSE).clientTrackingRange(8).build(Primordial.ID+":alchemical_wither_skeleton"));
    public static final RegistryObject<EntityType<AlchemicalMagmaCube>> MAGMA_CUBE=Primordial.ENTITIES.register("alchemical_magma_cube",
        ()->EntityType.Builder.<AlchemicalMagmaCube>of(AlchemicalMagmaCube::new,MobCategory.MONSTER).sized(2.04F,2.04F).fireImmune().clientTrackingRange(8)
            .build(Primordial.ID+":alchemical_magma_cube"));

    public static final RegistryObject<Item> ZOMBIE_EGG=egg("alchemical_zombie",ZOMBIE,0x4E6B5C,0xD9A441);
    public static final RegistryObject<Item> SKELETON_EGG=egg("alchemical_skeleton",SKELETON,0xD9D3BF,0x9FD8E8);
    public static final RegistryObject<Item> SPIDER_EGG=egg("alchemical_spider",SPIDER,0x3A3436,0x7CF0A8);
    public static final RegistryObject<Item> CREEPER_EGG=egg("alchemical_creeper",CREEPER,0xA9B83A,0xFF9A3C);
    public static final RegistryObject<Item> ENDERMAN_EGG=egg("alchemical_enderman",ENDERMAN,0x1C1A24,0xC77DFF);
    public static final RegistryObject<Item> BLAZE_EGG=egg("alchemical_blaze",BLAZE,0x3A2A1E,0xFFB347);
    public static final RegistryObject<Item> GHAST_EGG=egg("alchemical_ghast",GHAST,0xE8E4DC,0x6FE3E8);
    public static final RegistryObject<Item> WITHER_SKELETON_EGG=egg("alchemical_wither_skeleton",WITHER_SKELETON,0x1E1C1E,0x6FE3E8);
    public static final RegistryObject<Item> MAGMA_CUBE_EGG=egg("alchemical_magma_cube",MAGMA_CUBE,0x3A1410,0xFF7A1F);
    /** every creation's type */
    public static List<EntityType<?>> types() {
        return List.of(ZOMBIE.get(),SKELETON.get(),SPIDER.get(),CREEPER.get(),ENDERMAN.get(),BLAZE.get(),GHAST.get(),WITHER_SKELETON.get(),MAGMA_CUBE.get());
    }
    public static List<RegistryObject<Item>> eggs() {
        return List.of(ZOMBIE_EGG,SKELETON_EGG,SPIDER_EGG,CREEPER_EGG,ENDERMAN_EGG,BLAZE_EGG,GHAST_EGG,WITHER_SKELETON_EGG,MAGMA_CUBE_EGG);
    }

    /**
     * Each creation's own drop (in its loot table: 35% when killed by a player, +10% per Looting level), worked into the
     * charm, core and altar recipes.
     */
    public static final RegistryObject<Item> RUNIC_FLESH=drop("runic_flesh","alchemical_zombie",net.minecraft.world.item.Rarity.UNCOMMON);
    public static final RegistryObject<Item> ETCHED_BONE=drop("etched_bone","alchemical_skeleton",net.minecraft.world.item.Rarity.UNCOMMON);
    public static final RegistryObject<Item> STARLIT_SILK=drop("starlit_silk","alchemical_spider",net.minecraft.world.item.Rarity.UNCOMMON);
    public static final RegistryObject<Item> VOLATILE_CORE=drop("volatile_core","alchemical_creeper",net.minecraft.world.item.Rarity.UNCOMMON);
    public static final RegistryObject<Item> PHASE_PEARL=drop("phase_pearl","alchemical_enderman",net.minecraft.world.item.Rarity.RARE);
    public static final RegistryObject<Item> RUNE_EMBER=drop("rune_ember","alchemical_blaze",net.minecraft.world.item.Rarity.RARE);
    public static final RegistryObject<Item> SPIRIT_TEAR_CRYSTAL=drop("spirit_tear_crystal","alchemical_ghast",net.minecraft.world.item.Rarity.RARE);
    public static final RegistryObject<Item> WITHERED_SOUL_SHARD=drop("withered_soul_shard","alchemical_wither_skeleton",net.minecraft.world.item.Rarity.RARE);
    public static final RegistryObject<Item> MOLTEN_GEL=drop("molten_gel","alchemical_magma_cube",net.minecraft.world.item.Rarity.RARE);
    public static List<RegistryObject<Item>> drops() {
        return List.of(RUNIC_FLESH,ETCHED_BONE,STARLIT_SILK,VOLATILE_CORE,PHASE_PEARL,RUNE_EMBER,SPIRIT_TEAR_CRYSTAL,WITHERED_SOUL_SHARD,MOLTEN_GEL);
    }
    private static RegistryObject<Item> drop(String id,String creature,net.minecraft.world.item.Rarity rarity) {
        return Primordial.ITEMS.register(id,()->new CreationDrop(id,creature,rarity));
    }
    /** a creation's own material: a line of lore, and who drops it */
    public static final class CreationDrop extends Item {
        private final String id,creature;
        CreationDrop(String id,String creature,net.minecraft.world.item.Rarity rarity) {super(new Item.Properties().rarity(rarity));this.id=id;this.creature=creature;}
        @Override public void appendHoverText(net.minecraft.world.item.ItemStack stack,net.minecraft.world.level.Level level,List<net.minecraft.network.chat.Component> lines,net.minecraft.world.item.TooltipFlag flag) {
            lines.add(net.minecraft.network.chat.Component.translatable("tooltip.primordial."+id).withStyle(net.minecraft.ChatFormatting.GRAY,net.minecraft.ChatFormatting.ITALIC));
            lines.add(net.minecraft.network.chat.Component.translatable("tooltip.primordial.creation_drop.source",
                net.minecraft.network.chat.Component.translatable("entity.primordial."+creature)).withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        }
    }

    /** per creation killed by a player: a flat 10% chance of an affix accessory recipe, Looting or not */
    public static final double RECIPE_CHANCE=0.10,RECIPE_LOOTING_BONUS=0;
    public static double recipeChance(int looting) {return Math.min(1,RECIPE_CHANCE+RECIPE_LOOTING_BONUS*Math.max(0,looting));}

    private AlchemicalCreations() {}
    /** loads the registrations above; called while Primordial's registers are being filled */
    public static void register() {}

    private static RegistryObject<Item> egg(String id,RegistryObject<? extends EntityType<? extends net.minecraft.world.entity.Mob>> type,int base,int spots) {
        return Primordial.ITEMS.register(id+"_spawn_egg",()->new ForgeSpawnEggItem(type,base,spots,new Item.Properties()));
    }
    static <T extends Monster> boolean canSpawn(EntityType<T> type,ServerLevelAccessor level,MobSpawnType reason,BlockPos pos,RandomSource random) {
        return Monster.checkMonsterSpawnRules(type,level,reason,pos,random)&&!level.getBiome(pos).is(Tags.Biomes.IS_MUSHROOM);
    }
    /** the ghast and the magma cube are no Monsters: they spawn by any light, as their vanilla kin do, but not in peace */
    static <T extends net.minecraft.world.entity.Mob> boolean canSpawnAnyLight(EntityType<T> type,ServerLevelAccessor level,MobSpawnType reason,BlockPos pos,RandomSource random) {
        return level.getDifficulty()!=net.minecraft.world.Difficulty.PEACEFUL&&net.minecraft.world.entity.Mob.checkMobSpawnRules(type,level,reason,pos,random)
            &&(type!=GHAST.get()||reason!=MobSpawnType.NATURAL||random.nextInt(20)==0);
    }

    /**
     * The recipe a creation killed by a player drops, if any: one affix accessory recipe that not every player of the
     * world knows, at {@link #recipeChance}.
     */
    public static ResourceLocation rollRecipe(LivingEntity creation,int looting) {
        var server=creation.getServer();
        double chance=recipeChance(looting)*(local.primordial.spirit.SpiritRealm.inRealm(creation)?2:1);
        if(server==null||creation.getRandom().nextDouble()>=chance)return null;
        var world=AlchemyWorldKnowledge.get(server);
        var pool=AlchemyKnowledge.affixAccessoryRecipes().stream().filter(id->!world.everyoneKnows(id)).toList();
        return pool.isEmpty()?null:pool.get(creation.getRandom().nextInt(pool.size()));
    }

    @Mod.EventBusSubscriber(modid=Primordial.ID,bus=Mod.EventBusSubscriber.Bus.MOD)
    public static final class AlchemicalCreationSetup {
        @SubscribeEvent public static void createAlchemicalCreationAttributes(EntityAttributeCreationEvent event) {
            event.put(ZOMBIE.get(),AlchemicalZombie.attributes().build());
            event.put(SKELETON.get(),AlchemicalSkeleton.attributes().build());
            event.put(SPIDER.get(),AlchemicalSpider.attributes().build());
            event.put(CREEPER.get(),AlchemicalCreeper.attributes().build());
            event.put(ENDERMAN.get(),AlchemicalEnderman.attributes().build());
            event.put(BLAZE.get(),AlchemicalBlaze.attributes().build());
            event.put(GHAST.get(),AlchemicalGhast.attributes().build());
            event.put(WITHER_SKELETON.get(),AlchemicalWitherSkeleton.attributes().build());
            event.put(MAGMA_CUBE.get(),AlchemicalMagmaCube.attributes().build());
        }
        @SubscribeEvent public static void placeAlchemicalCreations(SpawnPlacementRegisterEvent event) {
            for(var type:List.<EntityType<? extends Monster>>of(ZOMBIE.get(),SKELETON.get(),SPIDER.get(),CREEPER.get(),ENDERMAN.get(),BLAZE.get(),WITHER_SKELETON.get()))
                place(event,type);
            placeAnyLight(event,GHAST.get());placeAnyLight(event,MAGMA_CUBE.get());
        }
        private static <T extends net.minecraft.world.entity.Mob> void placeAnyLight(SpawnPlacementRegisterEvent event,EntityType<T> type) {
            event.register(type,SpawnPlacements.Type.ON_GROUND,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                AlchemicalCreations::canSpawnAnyLight,SpawnPlacementRegisterEvent.Operation.REPLACE);
        }
        private static <T extends Monster> void place(SpawnPlacementRegisterEvent event,EntityType<T> type) {
            event.register(type,SpawnPlacements.Type.ON_GROUND,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                AlchemicalCreations::canSpawn,SpawnPlacementRegisterEvent.Operation.REPLACE);
        }
    }

    @Mod.EventBusSubscriber(modid=Primordial.ID)
    public static final class AlchemicalCreationDrops {
        @SubscribeEvent public static void dropAlchemicalCreationRecipe(LivingDropsEvent event) {
            var mob=event.getEntity();
            if(!(mob instanceof AlchemicalCreation)||!event.isRecentlyHit()||!(event.getSource().getEntity() instanceof Player))return;
            var recipe=rollRecipe(mob,event.getLootingLevel());
            if(recipe==null)return;
            var drop=new ItemEntity(mob.level(),mob.getX(),mob.getY(),mob.getZ(),AlchemyRecipeItem.create(recipe));
            drop.setDefaultPickUpDelay();
            event.getDrops().add(drop);
        }
    }
}
