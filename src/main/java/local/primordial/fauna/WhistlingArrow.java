package local.primordial.fauna;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 鸣镝 a whistling arrow: it whistles as it flies, and bends toward the nearest hostile creature ahead of it (within 16
 * blocks, inside a cone of about sixty degrees), a little each tick, never faster than it was shot.
 */
public class WhistlingArrow extends AbstractArrow {
    public static final double SEEK_RANGE=16,SEEK_CONE=0.5,TURN=0.12;
    public WhistlingArrow(EntityType<? extends WhistlingArrow> type,Level level) {super(type,level);}
    public WhistlingArrow(Level level,LivingEntity shooter) {super(FaunaContent.WHISTLING_ARROW.get(),shooter,level);}
    @Override protected ItemStack getPickupItem() {return new ItemStack(FaunaContent.WHISTLING_ARROW_ITEM.get());}

    @Override public void tick() {
        super.tick();
        if(level().isClientSide||inGround)return;
        if(tickCount%6==0)level().playSound(null,getX(),getY(),getZ(),SoundEvents.NOTE_BLOCK_FLUTE.value(),SoundSource.NEUTRAL,0.5F,1.9F);
        var target=seek();
        if(target==null)return;
        var velocity=getDeltaMovement();double speed=velocity.length();
        var want=target.getBoundingBox().getCenter().subtract(position()).normalize().scale(speed);
        setDeltaMovement(velocity.add(want.subtract(velocity).scale(TURN)).normalize().scale(speed));
    }
    /** the nearest hostile creature ahead, or null */
    public LivingEntity seek() {
        var heading=getDeltaMovement();if(heading.lengthSqr()<1e-4)return null;heading=heading.normalize();
        LivingEntity best=null;double bestDistance=Double.MAX_VALUE;
        for(var e:level().getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(SEEK_RANGE),e->e instanceof Enemy&&e.isAlive()&&e!=getOwner())){
            Vec3 to=e.getBoundingBox().getCenter().subtract(position());double d=to.length();
            if(d>SEEK_RANGE||d<1e-3||to.scale(1/d).dot(heading)<SEEK_CONE)continue;
            if(d<bestDistance){best=e;bestDistance=d;}
        }
        return best;
    }
}
