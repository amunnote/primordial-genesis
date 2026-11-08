package local.primordial.zhuque;

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
public final class ZhuqueContent {
    public static final RegistryObject<Block> LOG=Primordial.BLOCKS.register("sunset_wutong_log",()->new RotatedPillarBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.copy(Blocks.ACACIA_LOG)));
    public static final RegistryObject<Block> LEAVES=Primordial.BLOCKS.register("sunset_wutong_leaves",()->new LeavesBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.copy(Blocks.ACACIA_LEAVES)));
    public static final RegistryObject<Item> LOG_ITEM=Primordial.ITEMS.register("sunset_wutong_log",()->new BlockItem(LOG.get(),new Item.Properties()));
    public static final RegistryObject<Item> LEAVES_ITEM=Primordial.ITEMS.register("sunset_wutong_leaves",()->new BlockItem(LEAVES.get(),new Item.Properties()));
    public static final RegistryObject<Item> FEATHER=Primordial.ITEMS.register("ember_feather",()->new Item(new Item.Properties()));
    public static final RegistryObject<Item> FLUFF=Primordial.ITEMS.register("ember_fluff",()->new Item(new Item.Properties()));
    public static final RegistryObject<Item> TALISMAN=Primordial.ITEMS.register("kindling_talisman",KindlingTalisman::new);
    public static final RegistryObject<EntityType<EmberFeatherCrow>> CROW=Primordial.ENTITIES.register("ember_feather_crow",()->EntityType.Builder.of(EmberFeatherCrow::new,MobCategory.MONSTER).sized(0.8F,0.7F).clientTrackingRange(10).build(Primordial.ID+":ember_feather_crow"));
    public static final RegistryObject<EntityType<EmberTailRat>> RAT=Primordial.ENTITIES.register("ember_tail_rat",()->EntityType.Builder.of(EmberTailRat::new,MobCategory.CREATURE).sized(0.6F,0.45F).clientTrackingRange(8).build(Primordial.ID+":ember_tail_rat"));
    public static void register() {}
    @SubscribeEvent public static void attributes(EntityAttributeCreationEvent e){e.put(CROW.get(),EmberFeatherCrow.attributes().build());e.put(RAT.get(),EmberTailRat.attributes().build());}
    @SubscribeEvent public static void placements(SpawnPlacementRegisterEvent e){
        e.register(CROW.get(),SpawnPlacements.Type.NO_RESTRICTIONS,net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            (type,l,reason,pos,r)->l.getDifficulty()!=net.minecraft.world.Difficulty.PEACEFUL&&l.getLevel().isDay()&&l.canSeeSky(pos)&&!l.getBlockState(pos.below()).isAir()&&l.getRawBrightness(pos,0)>8&&nearWutong(l,pos),SpawnPlacementRegisterEvent.Operation.REPLACE);
        e.register(RAT.get(),SpawnPlacements.Type.ON_GROUND,net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            (type,l,reason,pos,r)->l.getBlockState(pos.below()).isSolidRender(l,pos.below())&&(!l.canSeeSky(pos)||nearWutong(l,pos)),SpawnPlacementRegisterEvent.Operation.REPLACE);
    }
    // only chunks at hand: while a chunk is generated its neighbours may not be, and asking for one out of the region crashes
    private static boolean nearWutong(net.minecraft.world.level.ServerLevelAccessor l,BlockPos p){for(var q:BlockPos.betweenClosed(p.offset(-7,-3,-7),p.offset(7,24,7)))if(l.hasChunk(q.getX()>>4,q.getZ()>>4)&&l.getBlockState(q).is(LOG.get()))return true;return false;}
}
