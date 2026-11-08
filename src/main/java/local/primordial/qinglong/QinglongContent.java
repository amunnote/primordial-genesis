package local.primordial.qinglong;

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
public final class QinglongContent {
    public static final RegistryObject<Block> CLOUDROCK=Primordial.BLOCKS.register("coppervein_cloudrock",()->new Block(net.minecraft.world.level.block.state.BlockBehaviour.Properties.copy(Blocks.CALCITE).strength(2.5F).requiresCorrectToolForDrops()));
    public static final RegistryObject<Item> CLOUDROCK_ITEM=Primordial.ITEMS.register("coppervein_cloudrock",()->new BlockItem(CLOUDROCK.get(),new Item.Properties()));
    public static final RegistryObject<Item> WHISKER=Primordial.ITEMS.register("jiao_whisker",()->new Item(new Item.Properties()));
    public static final RegistryObject<Item> ANTLER=Primordial.ITEMS.register("sprouting_antler",Antler::new);
    public static final RegistryObject<Item> TALISMAN=Primordial.ITEMS.register("thunder_talisman",Talisman::new);
    public static final RegistryObject<EntityType<ThunderJiao>> JIAO=Primordial.ENTITIES.register("thunder_jiao",()->EntityType.Builder.of(ThunderJiao::new,MobCategory.MONSTER).sized(1.2F,0.9F).clientTrackingRange(10).build(Primordial.ID+":thunder_jiao"));
    public static final RegistryObject<EntityType<VerdantAntlerDeer>> DEER=Primordial.ENTITIES.register("verdant_antler_deer",()->EntityType.Builder.of(VerdantAntlerDeer::new,MobCategory.CREATURE).sized(0.9F,1.8F).clientTrackingRange(8).build(Primordial.ID+":verdant_antler_deer"));
    public static void register() {}
    @SubscribeEvent public static void attributes(EntityAttributeCreationEvent e){e.put(JIAO.get(),ThunderJiao.attributes().build());e.put(DEER.get(),VerdantAntlerDeer.attributes().build());}
    @SubscribeEvent public static void placements(SpawnPlacementRegisterEvent e){
        e.register(JIAO.get(),SpawnPlacements.Type.NO_RESTRICTIONS,net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            (type,l,reason,pos,r)->l.getDifficulty()!=net.minecraft.world.Difficulty.PEACEFUL&&l.getLevel().isNight()&&l.canSeeSky(pos)&&!l.getBlockState(pos.below()).isAir()&&net.minecraft.world.entity.monster.Monster.checkMonsterSpawnRules(type,l,reason,pos,r),SpawnPlacementRegisterEvent.Operation.REPLACE);
        e.register(DEER.get(),SpawnPlacements.Type.ON_GROUND,net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            (type,l,reason,pos,r)->l.getBlockState(pos.below()).is(net.minecraft.tags.BlockTags.ANIMALS_SPAWNABLE_ON)&&l.getRawBrightness(pos,0)>8,SpawnPlacementRegisterEvent.Operation.REPLACE);
    }
    /** One antler carries five applications, each equivalent to a vanilla bone-meal attempt. */
    public static final class Antler extends Item {
        public Antler(){super(new Properties().durability(5));}
        @Override public net.minecraft.world.InteractionResult useOn(UseOnContext c){
            if(!(c.getLevel().getBlockState(c.getClickedPos()).getBlock() instanceof net.minecraft.world.level.block.CropBlock))return net.minecraft.world.InteractionResult.PASS;
            if(c.getLevel().isClientSide)return net.minecraft.world.InteractionResult.SUCCESS;
            if(BoneMealItem.growCrop(new ItemStack(Items.BONE_MEAL),c.getLevel(),c.getClickedPos())){
                c.getLevel().levelEvent(1505,c.getClickedPos(),0);
                if(c.getPlayer()!=null&&!c.getPlayer().getAbilities().instabuild)c.getItemInHand().hurtAndBreak(1,c.getPlayer(),p->p.broadcastBreakEvent(c.getHand()));
                return net.minecraft.world.InteractionResult.CONSUME;
            }return net.minecraft.world.InteractionResult.PASS;
        }
    }
    public static final class Talisman extends Item {
        public Talisman(){super(new Properties().stacksTo(16));}
        @Override public net.minecraft.world.InteractionResultHolder<ItemStack> use(net.minecraft.world.level.Level l,net.minecraft.world.entity.player.Player p,net.minecraft.world.InteractionHand hand){
            var stack=p.getItemInHand(hand);var hit=p.pick(32,0,false);
            if(hit.getType()!=net.minecraft.world.phys.HitResult.Type.BLOCK||p.getCooldowns().isOnCooldown(this))return net.minecraft.world.InteractionResultHolder.pass(stack);
            var pos=((net.minecraft.world.phys.BlockHitResult)hit).getBlockPos().above();
            if(!l.isClientSide){
                var bolt=EntityType.LIGHTNING_BOLT.create(l);
                if(bolt!=null){bolt.moveTo(net.minecraft.world.phys.Vec3.atBottomCenterOf(pos));if(p instanceof net.minecraft.server.level.ServerPlayer sp)bolt.setCause(sp);l.addFreshEntity(bolt);p.getCooldowns().addCooldown(this,100);if(!p.getAbilities().instabuild)stack.shrink(1);}
            }return net.minecraft.world.InteractionResultHolder.sidedSuccess(stack,l.isClientSide);
        }
    }
}
