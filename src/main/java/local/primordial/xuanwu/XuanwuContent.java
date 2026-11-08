package local.primordial.xuanwu;

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
public final class XuanwuContent {
    public static final RegistryObject<Block> ROCK=Primordial.BLOCKS.register("xuanwu_shell_rock",()->new Block(net.minecraft.world.level.block.state.BlockBehaviour.Properties.copy(Blocks.DEEPSLATE).strength(4F,8F).requiresCorrectToolForDrops()));
    public static final RegistryObject<Item> ROCK_ITEM=Primordial.ITEMS.register("xuanwu_shell_rock",()->new BlockItem(ROCK.get(),new Item.Properties()));
    public static final RegistryObject<Item> SCALE=Primordial.ITEMS.register("xuan_scale",()->new Item(new Item.Properties()));
    public static final RegistryObject<Item> SHELL=Primordial.ITEMS.register("coldjade_shell",()->new Item(new Item.Properties()));
    public static final RegistryObject<Item> TALISMAN=Primordial.ITEMS.register("tide_ward_talisman",TideWardTalisman::new);
    public static final RegistryObject<EntityType<XuanSerpent>> SERPENT=Primordial.ENTITIES.register("xuan_serpent",()->EntityType.Builder.of(XuanSerpent::new,MobCategory.MONSTER).sized(1.1F,0.65F).clientTrackingRange(10).build(Primordial.ID+":xuan_serpent"));
    public static final RegistryObject<EntityType<ColdjadeTurtle>> TURTLE=Primordial.ENTITIES.register("coldjade_turtle",()->EntityType.Builder.of(ColdjadeTurtle::new,MobCategory.CREATURE).sized(1.6F,0.8F).clientTrackingRange(8).build(Primordial.ID+":coldjade_turtle"));
    public static void register() {}
    @SubscribeEvent public static void attributes(EntityAttributeCreationEvent e){e.put(SERPENT.get(),XuanSerpent.attributes().build());e.put(TURTLE.get(),ColdjadeTurtle.attributes().build());}
    @SubscribeEvent public static void placements(SpawnPlacementRegisterEvent e){
        e.register(SERPENT.get(),SpawnPlacements.Type.IN_WATER,net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR,
            (t,l,reason,pos,r)->l.getDifficulty()!=net.minecraft.world.Difficulty.PEACEFUL&&l.getFluidState(pos).is(net.minecraft.tags.FluidTags.WATER),SpawnPlacementRegisterEvent.Operation.REPLACE);
        e.register(TURTLE.get(),SpawnPlacements.Type.NO_RESTRICTIONS,net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR,
            (t,l,reason,pos,r)->l.getFluidState(pos).is(net.minecraft.tags.FluidTags.WATER)||l.getBlockState(pos.below()).isSolidRender(l,pos.below()),SpawnPlacementRegisterEvent.Operation.REPLACE);
    }
}
