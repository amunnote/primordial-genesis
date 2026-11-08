package local.primordial.fauna;

import java.util.List;
import local.primordial.Primordial;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.MobBucketItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegistryObject;

/**
 * The four guardians' small creatures and what comes of them:
 * <ul>
 * <li>毕方 Bifang, the Vermilion Bird's rare elite: a one-legged fire crane; drops 毕方炎晶, a long-burning fuel that
 *     can be thrown to burst into flame.</li>
 * <li>雷萤 thunder fireflies, the Azure Dragon's night lights: a glass bottle takes one, and the 萤雷瓶 is a lamp to set
 *     down, a shock to throw, or a firefly to let go.</li>
 * <li>鸣金雀 golden-bell sparrows of the White Tiger's wastes: 金铃羽 feathers, fletched into 鸣镝, whistling arrows
 *     that turn toward the nearest foe.</li>
 * <li>冥灯鱼 netherlamp fish of the Black Tortoise's sea, taken in a bucket: 冥灯囊 lamps, made into 冥灯, a lantern
 *     that burns under water and gives night vision there to the one holding it.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid=Primordial.ID,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class FaunaContent {
    public static final RegistryObject<EntityType<Bifang>> BIFANG=Primordial.ENTITIES.register("bifang",
        ()->EntityType.Builder.of(Bifang::new,MobCategory.MONSTER).sized(0.9F,1.9F).fireImmune().clientTrackingRange(10).build(Primordial.ID+":bifang"));
    public static final RegistryObject<EntityType<ThunderFirefly>> FIREFLY=Primordial.ENTITIES.register("thunder_firefly",
        ()->EntityType.Builder.of(ThunderFirefly::new,MobCategory.AMBIENT).sized(0.3F,0.3F).clientTrackingRange(8).build(Primordial.ID+":thunder_firefly"));
    public static final RegistryObject<EntityType<GoldenBellSparrow>> SPARROW=Primordial.ENTITIES.register("golden_bell_sparrow",
        ()->EntityType.Builder.of(GoldenBellSparrow::new,MobCategory.CREATURE).sized(0.4F,0.45F).clientTrackingRange(8).build(Primordial.ID+":golden_bell_sparrow"));
    public static final RegistryObject<EntityType<NetherlampFish>> LAMPFISH=Primordial.ENTITIES.register("netherlamp_fish",
        ()->EntityType.Builder.of(NetherlampFish::new,MobCategory.WATER_AMBIENT).sized(0.5F,0.4F).clientTrackingRange(6).build(Primordial.ID+":netherlamp_fish"));
    public static final RegistryObject<EntityType<ThrownFlameCrystal>> THROWN_CRYSTAL=Primordial.ENTITIES.register("thrown_bifang_crystal",
        ()->EntityType.Builder.<ThrownFlameCrystal>of(ThrownFlameCrystal::new,MobCategory.MISC).sized(0.25F,0.25F).clientTrackingRange(4).updateInterval(10).build(Primordial.ID+":thrown_bifang_crystal"));
    public static final RegistryObject<EntityType<ThrownFireflyBottle>> THROWN_BOTTLE=Primordial.ENTITIES.register("thrown_firefly_bottle",
        ()->EntityType.Builder.<ThrownFireflyBottle>of(ThrownFireflyBottle::new,MobCategory.MISC).sized(0.25F,0.25F).clientTrackingRange(4).updateInterval(10).build(Primordial.ID+":thrown_firefly_bottle"));
    public static final RegistryObject<EntityType<WhistlingArrow>> WHISTLING_ARROW=Primordial.ENTITIES.register("whistling_arrow",
        ()->EntityType.Builder.<WhistlingArrow>of(WhistlingArrow::new,MobCategory.MISC).sized(0.5F,0.5F).clientTrackingRange(4).updateInterval(20).build(Primordial.ID+":whistling_arrow"));

    public static final RegistryObject<Block> FIREFLY_JAR=Primordial.BLOCKS.register("thunder_firefly_jar",
        ()->new FireflyJarBlock(Block.Properties.copy(Blocks.LANTERN).sound(SoundType.GLASS).lightLevel(s->12).noOcclusion()));
    public static final RegistryObject<Block> NETHERLAMP_BLOCK=Primordial.BLOCKS.register("netherlamp",
        ()->new LanternBlock(Block.Properties.copy(Blocks.SOUL_LANTERN).lightLevel(s->15).noOcclusion()));

    public static final RegistryObject<Item> FLAME_CRYSTAL=Primordial.ITEMS.register("bifang_flame_crystal",()->new FlameCrystalItem(new Item.Properties().rarity(Rarity.RARE)));
    public static final RegistryObject<Item> FIREFLY_BOTTLE=Primordial.ITEMS.register("thunder_firefly_bottle",()->new FireflyBottleItem(FIREFLY_JAR.get(),new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> BELL_FEATHER=Primordial.ITEMS.register("golden_bell_feather",()->new Item(new Item.Properties()));
    public static final RegistryObject<Item> WHISTLING_ARROW_ITEM=Primordial.ITEMS.register("whistling_arrow",()->new WhistlingArrowItem(new Item.Properties()));
    public static final RegistryObject<Item> LAMP_SAC=Primordial.ITEMS.register("netherlamp_sac",()->new Item(new Item.Properties()));
    public static final RegistryObject<Item> NETHERLAMP=Primordial.ITEMS.register("netherlamp",()->new BlockItem(NETHERLAMP_BLOCK.get(),new Item.Properties()){
        @Override public void appendHoverText(net.minecraft.world.item.ItemStack stack,net.minecraft.world.level.Level level,java.util.List<net.minecraft.network.chat.Component> lines,net.minecraft.world.item.TooltipFlag flag){
            lines.add(net.minecraft.network.chat.Component.translatable("tooltip.primordial.netherlamp").withStyle(net.minecraft.ChatFormatting.DARK_AQUA));}});
    public static final RegistryObject<Item> LAMPFISH_BUCKET=Primordial.ITEMS.register("netherlamp_fish_bucket",
        ()->new MobBucketItem(LAMPFISH,()->Fluids.WATER,()->SoundEvents.BUCKET_EMPTY_FISH,new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> BIFANG_EGG=egg("bifang",BIFANG,0x1E6E78,0xE04A2C);
    public static final RegistryObject<Item> FIREFLY_EGG=egg("thunder_firefly",FIREFLY,0x141A28,0x7FE2EF);
    public static final RegistryObject<Item> SPARROW_EGG=egg("golden_bell_sparrow",SPARROW,0xF6F1E4,0xD9AA37);
    public static final RegistryObject<Item> LAMPFISH_EGG=egg("netherlamp_fish",LAMPFISH,0x14223A,0x38B9D1);
    /** for the creative tab */
    public static final List<RegistryObject<Item>> TAB=List.of(FLAME_CRYSTAL,FIREFLY_BOTTLE,BELL_FEATHER,WHISTLING_ARROW_ITEM,LAMP_SAC,NETHERLAMP,LAMPFISH_BUCKET,BIFANG_EGG,FIREFLY_EGG,SPARROW_EGG,LAMPFISH_EGG);

    private static RegistryObject<Item> egg(String id,RegistryObject<? extends EntityType<? extends net.minecraft.world.entity.Mob>> type,int base,int spots) {
        return Primordial.ITEMS.register(id+"_spawn_egg",()->new ForgeSpawnEggItem(type,base,spots,new Item.Properties()));
    }
    private FaunaContent() {}
    public static void register() {}

    @SubscribeEvent public static void attributes(EntityAttributeCreationEvent event) {
        event.put(BIFANG.get(),Bifang.attributes().build());
        event.put(FIREFLY.get(),ThunderFirefly.attributes().build());
        event.put(SPARROW.get(),GoldenBellSparrow.attributes().build());
        event.put(LAMPFISH.get(),NetherlampFish.attributes().build());
    }
    @SubscribeEvent public static void placements(SpawnPlacementRegisterEvent event) {
        event.register(BIFANG.get(),SpawnPlacements.Type.ON_GROUND,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,Bifang::canSpawn,SpawnPlacementRegisterEvent.Operation.REPLACE);
        event.register(FIREFLY.get(),SpawnPlacements.Type.NO_RESTRICTIONS,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,ThunderFirefly::canSpawn,SpawnPlacementRegisterEvent.Operation.REPLACE);
        event.register(SPARROW.get(),SpawnPlacements.Type.ON_GROUND,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,GoldenBellSparrow::canSpawn,SpawnPlacementRegisterEvent.Operation.REPLACE);
        event.register(LAMPFISH.get(),SpawnPlacements.Type.IN_WATER,Heightmap.Types.OCEAN_FLOOR,NetherlampFish::canSpawn,SpawnPlacementRegisterEvent.Operation.REPLACE);
    }
}
