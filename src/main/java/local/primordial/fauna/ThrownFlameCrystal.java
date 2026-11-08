package local.primordial.fauna;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

/**
 * A thrown 毕方炎晶: where it lands a ring of flame bursts, 6 damage and five seconds alight to every creature within
 * 3.5 blocks but the thrower. It sets no blocks on fire.
 */
public class ThrownFlameCrystal extends ThrowableItemProjectile {
    public static final float DAMAGE=6,RADIUS=3.5F;
    public ThrownFlameCrystal(EntityType<? extends ThrownFlameCrystal> type,Level level) {super(type,level);}
    public ThrownFlameCrystal(Level level,LivingEntity owner) {super(FaunaContent.THROWN_CRYSTAL.get(),owner,level);}
    @Override protected Item getDefaultItem() {return FaunaContent.FLAME_CRYSTAL.get();}
    @Override protected void onHit(HitResult result) {
        super.onHit(result);
        if(!(level() instanceof ServerLevel level))return;
        burst(level,this,getOwner());
        discard();
    }
    public static void burst(ServerLevel level,net.minecraft.world.entity.Entity at,net.minecraft.world.entity.Entity owner) {
        var source=owner instanceof LivingEntity living?level.damageSources().indirectMagic(at,living):level.damageSources().inFire();
        for(var e:level.getEntitiesOfClass(LivingEntity.class,at.getBoundingBox().inflate(RADIUS),e->e!=owner&&e.isAlive()&&e.distanceTo(at)<=RADIUS)){
            e.hurt(source,DAMAGE);e.setSecondsOnFire(5);
        }
        level.sendParticles(ParticleTypes.FLAME,at.getX(),at.getY()+0.3,at.getZ(),60,1.2,0.4,1.2,0.05);
        level.sendParticles(ParticleTypes.LAVA,at.getX(),at.getY()+0.3,at.getZ(),12,0.6,0.2,0.6,0);
        level.playSound(null,at.getX(),at.getY(),at.getZ(),SoundEvents.GENERIC_EXPLODE,SoundSource.NEUTRAL,0.6F,1.4F);
        level.playSound(null,at.getX(),at.getY(),at.getZ(),SoundEvents.FIRECHARGE_USE,SoundSource.NEUTRAL,1F,0.8F);
    }
}
