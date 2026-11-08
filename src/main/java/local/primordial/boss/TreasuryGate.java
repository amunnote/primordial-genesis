package local.primordial.boss;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * One gate of the King's Treasury: a golden portal standing in the air, facing the king's foe. It opens rippling,
 * a weapon slowly slides out of it point first while the king winds up, and when the king looses, the weapon flies and
 * the gate folds shut. Only something to see (and to read the danger from): it has no body and is never saved.
 */
public class TreasuryGate extends Entity {
    static final EntityDataAccessor<ItemStack> ITEM=SynchedEntityData.defineId(TreasuryGate.class,EntityDataSerializers.ITEM_STACK);
    static final EntityDataAccessor<Integer> HOLD=SynchedEntityData.defineId(TreasuryGate.class,EntityDataSerializers.INT);
    static final EntityDataAccessor<Integer> CLOSING=SynchedEntityData.defineId(TreasuryGate.class,EntityDataSerializers.INT);
    static final EntityDataAccessor<Float> SIZE=SynchedEntityData.defineId(TreasuryGate.class,EntityDataSerializers.FLOAT);
    public static final int OPEN_TICKS=6,CLOSE_TICKS=10;
    private boolean burst;

    public TreasuryGate(EntityType<? extends TreasuryGate> type,Level level) {super(type,level);noPhysics=true;setNoGravity(true);}
    /** open a gate at a spot, facing a point, holding a weapon for so many ticks before it fires */
    public static TreasuryGate open(Level level,Vec3 at,Vec3 facing,ItemStack weapon,int hold,float size) {
        var gate=new TreasuryGate(SinBosses.TREASURY_GATE.get(),level);
        gate.setPos(at);gate.face(facing.subtract(at));
        gate.entityData.set(ITEM,weapon.copyWithCount(1));gate.entityData.set(HOLD,hold);gate.entityData.set(SIZE,size);
        level.addFreshEntity(gate);
        return gate;
    }
    void face(Vec3 direction) {
        setYRot((float)(Mth.atan2(direction.x,direction.z)*Mth.RAD_TO_DEG));
        setXRot((float)(Mth.atan2(direction.y,direction.horizontalDistance())*Mth.RAD_TO_DEG));
        yRotO=getYRot();xRotO=getXRot();
    }
    /** turn to face a point (the target moved) */
    public void aim(Vec3 point) {face(point.subtract(position()));}
    public ItemStack weapon() {return entityData.get(ITEM);}
    public int hold() {return entityData.get(HOLD);}
    public float size() {return entityData.get(SIZE);}
    /** ticks since it began to close, or -1 while open */
    public int closing() {return entityData.get(CLOSING);}
    /** the way it faces (yaw kept as projectiles keep it: atan2(x,z)) */
    public Vec3 facing() {
        double yaw=Math.toRadians(getYRot()),pitch=Math.toRadians(getXRot());
        return new Vec3(Math.sin(yaw)*Math.cos(pitch),Math.sin(pitch),Math.cos(yaw)*Math.cos(pitch));
    }
    /** the weapon has flown: empty the gate and fold it shut */
    public void fire() {entityData.set(ITEM,ItemStack.EMPTY);if(closing()<0)entityData.set(CLOSING,0);}
    /** how far the gate is open, 0 to 1 */
    public float openness(float partialTick) {
        float age=tickCount+partialTick;
        if(closing()>=0)return Math.max(0,1-(closing()+partialTick)/CLOSE_TICKS);
        return Math.min(1,age/OPEN_TICKS);
    }
    /** how far the weapon has slid out, 0 to 1 */
    public float emergence(float partialTick) {return Mth.clamp((tickCount+partialTick-OPEN_TICKS*0.5F)/Math.max(1,hold()),0,1);}

    @Override protected void defineSynchedData() {
        entityData.define(ITEM,ItemStack.EMPTY);entityData.define(HOLD,20);entityData.define(CLOSING,-1);entityData.define(SIZE,1F);
    }
    @Override public void tick() {
        super.tick();
        xRotO=getXRot();yRotO=getYRot();
        if(level().isClientSide){effects();return;}
        if(closing()>=0){
            entityData.set(CLOSING,closing()+1);
            if(closing()>CLOSE_TICKS)discard();
        }else if(tickCount>hold()+40)fire();
    }
    /** client side: gold dust drawn spiralling into the open gate; a flare and a spray of dust as it fires and folds */
    private void effects() {
        var random=level().random;
        var normal=facing();
        var a=Math.abs(normal.y)>0.9?new Vec3(1,0,0):normal.cross(new Vec3(0,1,0)).normalize();
        var b=normal.cross(a).normalize();
        float size=size(),open=openness(0);
        int colour=0xFFDF7E;
        if(closing()>=0&&!burst){
            burst=true;
            level().addParticle(SinParticles.FLARE.get().of(0xFFF1B8),getX(),getY(),getZ(),0,0,0);
            for(int i=0;i<14;i++){
                double t=random.nextDouble()*Math.PI*2,s=0.12+random.nextDouble()*0.18;
                var v=a.scale(Math.cos(t)*s).add(b.scale(Math.sin(t)*s)).add(normal.scale(0.15));
                level().addParticle(SinParticles.GOLD_DUST.get().of(colour),getX(),getY(),getZ(),v.x,v.y,v.z);
            }
            return;
        }
        if(closing()>=0||open<0.3)return;
        for(int i=0;i<2;i++){
            double t=random.nextDouble()*Math.PI*2,r=size*(1.2+random.nextDouble()*0.4);
            var at=position().add(a.scale(Math.cos(t)*r)).add(b.scale(Math.sin(t)*r)).subtract(normal.scale(0.1));
            // inward and swirling round
            var inward=position().subtract(at).scale(0.07);
            var swirl=a.scale(-Math.sin(t)).add(b.scale(Math.cos(t))).scale(0.05);
            var v=inward.add(swirl);
            level().addParticle(SinParticles.GOLD_DUST.get().of(colour),at.x,at.y,at.z,v.x,v.y,v.z);
        }
        if(random.nextInt(5)==0){
            double t=random.nextDouble()*Math.PI*2,r=size*random.nextDouble()*0.8;
            var at=position().add(a.scale(Math.cos(t)*r)).add(b.scale(Math.sin(t)*r));
            level().addParticle(SinParticles.GLINT.get().of(0xFFF4C8),at.x,at.y,at.z,0,0,0);
        }
    }
    @Override public boolean isPickable() {return false;}
    @Override public boolean isAttackable() {return false;}
    @Override public boolean shouldBeSaved() {return false;}
    @Override public boolean shouldRenderAtSqrDistance(double distance) {return distance<160*160;}
    @Override protected void readAdditionalSaveData(CompoundTag tag) {}
    @Override protected void addAdditionalSaveData(CompoundTag tag) {}
}
