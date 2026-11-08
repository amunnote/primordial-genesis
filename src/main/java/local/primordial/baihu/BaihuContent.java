package local.primordial.baihu;

import local.primordial.Primordial;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.*;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(modid=Primordial.ID,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class BaihuContent {
    public static final RegistryObject<Block> BLADESTONE=Primordial.BLOCKS.register("ironvein_bladestone",()->new Block(net.minecraft.world.level.block.state.BlockBehaviour.Properties.copy(Blocks.CALCITE).strength(2.5F).requiresCorrectToolForDrops()));
    public static final RegistryObject<Item> BLADESTONE_ITEM=Primordial.ITEMS.register("ironvein_bladestone",()->new BlockItem(BLADESTONE.get(),new Item.Properties()));
    public static final RegistryObject<Item> SHARD=Primordial.ITEMS.register("fine_gold_shard",()->new Item(new Item.Properties()));
    public static final RegistryObject<Item> INGOT=Primordial.ITEMS.register("fine_gold_ingot",()->new Item(new Item.Properties()));
    public static final RegistryObject<Item> PELT=Primordial.ITEMS.register("white_stripe_pelt",()->new Item(new Item.Properties()));
    public static final RegistryObject<EntityType<GengBladeWraith>> WRAITH=Primordial.ENTITIES.register("geng_blade_wraith",()->EntityType.Builder.of(GengBladeWraith::new,MobCategory.MONSTER).sized(1.2F,0.9F).clientTrackingRange(10).build(Primordial.ID+":geng_blade_wraith"));
    public static final RegistryObject<EntityType<WhiteStripedTiger>> TIGER=Primordial.ENTITIES.register("white_striped_tiger",()->EntityType.Builder.of(WhiteStripedTiger::new,MobCategory.CREATURE).sized(1.0F,1.1F).clientTrackingRange(8).build(Primordial.ID+":white_striped_tiger"));
    public static void register() {}
    @SubscribeEvent public static void attributes(EntityAttributeCreationEvent e){e.put(WRAITH.get(),GengBladeWraith.attributes().build());e.put(TIGER.get(),WhiteStripedTiger.attributes().build());}
    @SubscribeEvent public static void placements(SpawnPlacementRegisterEvent e){
        e.register(WRAITH.get(),SpawnPlacements.Type.NO_RESTRICTIONS,net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            (type,l,reason,pos,r)->l.getDifficulty()!=net.minecraft.world.Difficulty.PEACEFUL&&pos.getY()>=95&&l.getLevel().isNight()&&l.canSeeSky(pos)&&!l.getBlockState(pos.below()).isAir()&&net.minecraft.world.entity.monster.Monster.checkMonsterSpawnRules(type,l,reason,pos,r),SpawnPlacementRegisterEvent.Operation.REPLACE);
        e.register(TIGER.get(),SpawnPlacements.Type.ON_GROUND,net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            (type,l,reason,pos,r)->l.getBlockState(pos.below()).isSolidRender(l,pos.below())&&l.getRawBrightness(pos,0)>8,SpawnPlacementRegisterEvent.Operation.REPLACE);
    }
}
