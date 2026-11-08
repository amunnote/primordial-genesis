package local.primordial.boss;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * A shockwave seen, not felt: a ring of cracked fire spreading over the ground from a blow (or, standing up, rolling
 * out from a roar), fading as it grows. Only something to see; the harm is dealt by whoever made it. Never saved.
 */
public class SinShockwave extends Entity {
    static final EntityDataAccessor<Integer> COLOUR=SynchedEntityData.defineId(SinShockwave.class,EntityDataSerializers.INT);
    static final EntityDataAccessor<Float> RADIUS=SynchedEntityData.defineId(SinShockwave.class,EntityDataSerializers.FLOAT);
    static final EntityDataAccessor<Integer> LIFE=SynchedEntityData.defineId(SinShockwave.class,EntityDataSerializers.INT);
    static final EntityDataAccessor<Boolean> UPRIGHT=SynchedEntityData.defineId(SinShockwave.class,EntityDataSerializers.BOOLEAN);

    public SinShockwave(EntityType<? extends SinShockwave> type,Level level) {super(type,level);noPhysics=true;setNoGravity(true);}
    /** a ring at a spot that spreads to a radius over so many ticks; upright ones face along a yaw */
    public static SinShockwave spread(Level level,Vec3 at,float radius,int life,int colour,boolean upright,float yaw) {
        var wave=new SinShockwave(SinBosses.SHOCKWAVE.get(),level);
        wave.setPos(at.x,at.y+0.06,at.z);wave.setYRot(yaw);wave.yRotO=yaw;
        wave.entityData.set(RADIUS,radius);wave.entityData.set(LIFE,life);wave.entityData.set(COLOUR,colour);wave.entityData.set(UPRIGHT,upright);
        level.addFreshEntity(wave);
        return wave;
    }
    public int colour() {return entityData.get(COLOUR);}
    public float radius() {return entityData.get(RADIUS);}
    public int life() {return entityData.get(LIFE);}
    public boolean upright() {return entityData.get(UPRIGHT);}
    /** 0 to 1 over its life */
    public float progress(float partialTick) {return Math.min(1,(tickCount+partialTick)/Math.max(1,life()));}
    @Override protected void defineSynchedData() {
        entityData.define(COLOUR,0xE63248);entityData.define(RADIUS,6F);entityData.define(LIFE,14);entityData.define(UPRIGHT,false);
    }
    @Override public void tick() {super.tick();if(!level().isClientSide&&tickCount>life()+1)discard();}
    @Override public boolean isPickable() {return false;}
    @Override public boolean shouldBeSaved() {return false;}
    @Override public boolean shouldRenderAtSqrDistance(double distance) {return distance<128*128;}
    @Override protected void readAdditionalSaveData(CompoundTag tag) {}
    @Override protected void addAdditionalSaveData(CompoundTag tag) {}
}
