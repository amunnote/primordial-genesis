package local.primordial.altar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import local.primordial.Primordial;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Holds the items resting on a basic alchemy altar, in the order they were placed. */
public final class AltarBlockEntity extends BlockEntity {
    /** largest capacity of any altar tier (saved data is read up to this) */
    public static final int MAX_CAPACITY=16;
    private final List<ItemStack> items=new ArrayList<>();
    /** index in {@link #items} of the recipe's main material (floats in the middle), or -1; decided server side */
    private int mainIndex=-1;

    public AltarBlockEntity(BlockPos pos,BlockState state) {super(Primordial.BASIC_ALCHEMY_ALTAR_ENTITY.get(),pos,state);}

    public List<ItemStack> items() {return Collections.unmodifiableList(items);}
    public int mainIndex() {return mainIndex>=0&&mainIndex<items.size()?mainIndex:-1;}
    public int capacity() {return getBlockState().getBlock() instanceof AlchemyAltarBlock block?block.capacity():8;}
    public boolean isFull() {return items.size()>=capacity();}
    public void add(ItemStack stack) {items.add(stack);changed();}
    /** Removes and returns the most recently placed item, or EMPTY. */
    public ItemStack removeLast() {
        if(items.isEmpty())return ItemStack.EMPTY;
        ItemStack stack=items.remove(items.size()-1);
        changed();
        return stack;
    }
    /** Empties the altar, returning everything that was on it. */
    public List<ItemStack> clear() {
        List<ItemStack> all=new ArrayList<>(items);
        items.clear();
        changed();
        return all;
    }
    private void changed() {
        if(level!=null&&!level.isClientSide)mainIndex=findMain();
        setChanged();
        if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);
    }
    /** the first altar recipe that everything on the table could belong to, and that has one of these items as its main material */
    private int findMain() {
        if(items.isEmpty()||level==null)return -1;
        for(AltarRecipe recipe:level.getRecipeManager().getAllRecipesFor(Primordial.ALTAR_RECIPE_TYPE.get())){
            int index=recipe.mainIndex(items);
            if(index>=0)return index;
        }
        return -1;
    }
    @Override public void onLoad() {
        super.onLoad();
        if(level!=null&&!level.isClientSide)mainIndex=findMain(); // recipes may have changed since the save
    }

    @Override public void load(CompoundTag tag) {
        super.load(tag);
        items.clear();
        ListTag list=tag.getList("Items",Tag.TAG_COMPOUND);
        for(int i=0;i<list.size()&&i<MAX_CAPACITY;i++){
            ItemStack stack=ItemStack.of(list.getCompound(i));
            if(!stack.isEmpty())items.add(stack);
        }
        mainIndex=tag.contains("Main",Tag.TAG_INT)?tag.getInt("Main"):-1;
    }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ListTag list=new ListTag();
        for(ItemStack stack:items)list.add(stack.save(new CompoundTag()));
        tag.put("Items",list);
        tag.putInt("Main",mainIndex);
    }
    @Override public CompoundTag getUpdateTag() {return saveWithoutMetadata();}
    @Override public Packet<ClientGamePacketListener> getUpdatePacket() {return ClientboundBlockEntityDataPacket.create(this);}
    // items float above the block, so don't let the renderer get culled at the block's own bounds
    @Override public AABB getRenderBoundingBox() {return new AABB(worldPosition).expandTowards(0,1,0).inflate(0.25);}
}
