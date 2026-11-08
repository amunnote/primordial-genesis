package local.primordial.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** Purely visual prismatic bolt: no fire, no damage, no block interaction. */
public final class GemLightning extends Entity {
    public final long seed;
    private int age;
    public GemLightning(EntityType<? extends GemLightning> type,Level level) {
        super(type,level);
        noCulling=true;
        seed=random.nextLong();
    }
    @Override public void tick() {
        super.tick();
        if(level().isClientSide) {
            if(age==0) {
                level().playLocalSound(getX(),getY(),getZ(),SoundEvents.LIGHTNING_BOLT_THUNDER,SoundSource.WEATHER,
                    3.0F,0.9F+random.nextFloat()*0.4F,false);
                level().playLocalSound(getX(),getY(),getZ(),SoundEvents.LIGHTNING_BOLT_IMPACT,SoundSource.WEATHER,
                    1.5F,0.6F+random.nextFloat()*0.3F,false);
            }
            if(age<2)level().setSkyFlashTime(2);
        }
        if(++age>8&&!level().isClientSide)discard();
    }
    @Override public boolean shouldRenderAtSqrDistance(double distance) {return distance<256.0*256.0;}
    @Override protected void defineSynchedData() {}
    @Override protected void readAdditionalSaveData(CompoundTag tag) {}
    @Override protected void addAdditionalSaveData(CompoundTag tag) {}
}
