package local.primordial.fauna;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

/**
 * A thrown 萤雷瓶: it shatters in a shock, 4 damage and two seconds of slowness to every creature within 2.5 blocks but
 * the thrower, and the firefly flies free.
 */
public class ThrownFireflyBottle extends ThrowableItemProjectile {
    public static final float DAMAGE=4,RADIUS=2.5F;
    public ThrownFireflyBottle(EntityType<? extends ThrownFireflyBottle> type,Level level) {super(type,level);}
    public ThrownFireflyBottle(Level level,LivingEntity owner) {super(FaunaContent.THROWN_BOTTLE.get(),owner,level);}
    @Override protected Item getDefaultItem() {return FaunaContent.FIREFLY_BOTTLE.get();}
    @Override protected void onHit(HitResult result) {
        super.onHit(result);
        if(!(level() instanceof ServerLevel level))return;
        var owner=getOwner();
        var source=owner instanceof LivingEntity living?level.damageSources().indirectMagic(this,living):level.damageSources().magic();
        for(var e:level.getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(RADIUS),e->e!=owner&&e.isAlive()&&e.distanceTo(this)<=RADIUS)){
            e.hurt(source,DAMAGE);e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,40,0),owner);
        }
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,getX(),getY()+0.3,getZ(),40,0.8,0.5,0.8,0.4);
        level.playSound(null,getX(),getY(),getZ(),SoundEvents.GLASS_BREAK,SoundSource.NEUTRAL,1F,1.2F);
        level.playSound(null,getX(),getY(),getZ(),SoundEvents.LIGHTNING_BOLT_IMPACT,SoundSource.NEUTRAL,0.4F,1.8F);
        FaunaContent.FIREFLY.get().spawn(level,BlockPos.containing(position().add(0,0.5,0)),MobSpawnType.BUCKET);
        discard();
    }
}
