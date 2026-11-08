package local.primordial.altar;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.items.ItemHandlerHelper;

/**
 * 炼金仪（工作方块）：右键把手中物品（一次一个）放到台面上绕台面旋转（容量见 {@link #capacity()}）；
 * Shift+右键取下最后放上的物品；空手右键按台面上的物品炼成（{@link AltarCrafting}）。
 * tier：1 基础、2 嬗变、3 真理、4 原初；炼成配方可要求最低 tier。
 */
public final class AlchemyAltarBlock extends BaseEntityBlock {
    public static final VoxelShape BASIC_SHAPE=Shapes.or(
        box(1,0,1,15,3,15),          // plinth
        box(4.5,3,4.5,11.5,10,11.5), // column
        box(1.5,10,1.5,14.5,13,14.5)); // table
    public static final VoxelShape TRANSMUTATION_SHAPE=Shapes.or(
        box(0.5,0,0.5,15.5,2,15.5),  // octagonal base
        box(4.5,2,4.5,11.5,10,11.5), // twisted column
        box(1.5,10,1.5,14.5,12.5,14.5)); // transmutation table
    public static final VoxelShape TRUTH_SHAPE=Shapes.or(
        box(0.5,0,0.5,15.5,3,15.5),  // marble base + step
        box(4.5,3,4.5,11.5,9,11.5),  // caged core
        box(2.5,9,2.5,13.5,11.5,13.5), // dais + rim
        box(1,1.5,1,2.5,13,2.5),box(13.5,1.5,1,15,13,2.5),box(1,1.5,13.5,2.5,13,15),box(13.5,1.5,13.5,15,13,15)); // obelisks
    public static final VoxelShape PRIMORDIAL_SHAPE=Shapes.or(
        box(0,0,0,16,2,16),          // octagonal base
        box(4.5,2,4.5,11.5,8,11.5),  // void crystal core
        box(3,5,3,13,5.5,13),        // floating star frame
        box(2,8,2,14,11,14));        // table
    private final int tier;
    private final VoxelShape shape;
    /** the y of the surface items orbit above, in block units */
    private final float tableTop;

    public AlchemyAltarBlock(int tier,VoxelShape shape,float tableTop,int light) {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(3.5F+tier,6.0F+tier*2)
            .requiresCorrectToolForDrops().sound(SoundType.STONE).noOcclusion().lightLevel(state->light));
        this.tier=tier;this.shape=shape;this.tableTop=tableTop;
    }
    public int tier() {return tier;}
    /** items the table can hold: 8 / 12 / 16 / 16 by tier */
    public int capacity() {return Math.min(AltarBlockEntity.MAX_CAPACITY,4+4*tier);}
    public float tableTop() {return tableTop;}

    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) {return shape;}
    @Override public RenderShape getRenderShape(BlockState state) {return RenderShape.MODEL;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) {return new AltarBlockEntity(pos,state);}

    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit) {
        if(!(level.getBlockEntity(pos) instanceof AltarBlockEntity altar))return InteractionResult.PASS;
        if(player.isSecondaryUseActive()){
            if(altar.items().isEmpty())return InteractionResult.PASS;
            if(!level.isClientSide){
                ItemHandlerHelper.giveItemToPlayer(player,altar.removeLast());
                level.playSound(null,pos,SoundEvents.ITEM_FRAME_REMOVE_ITEM,SoundSource.BLOCKS,1F,1F);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        ItemStack held=player.getItemInHand(hand);
        if(held.isEmpty()){
            // empty hand: perform the alchemy for whatever is on the table (main hand only, so it runs once)
            if(hand!=InteractionHand.MAIN_HAND||altar.items().isEmpty())return InteractionResult.PASS;
            if(level instanceof net.minecraft.server.level.ServerLevel server)AltarCrafting.tryCraft(server,pos,player);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if(altar.isFull()){
            if(!level.isClientSide)player.displayClientMessage(Component.translatable("message.primordial.altar.full"),true);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if(!level.isClientSide){
            altar.add(held.copyWithCount(1));
            if(!player.getAbilities().instabuild)held.shrink(1);
            level.playSound(null,pos,SoundEvents.ITEM_FRAME_ADD_ITEM,SoundSource.BLOCKS,1F,1F);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState newState,boolean moved) {
        if(!state.is(newState.getBlock())&&level.getBlockEntity(pos) instanceof AltarBlockEntity altar)
            for(ItemStack stack:altar.clear())Containers.dropItemStack(level,pos.getX()+0.5,pos.getY()+1.0,pos.getZ()+0.5,stack);
        super.onRemove(state,level,pos,newState,moved);
    }
}
