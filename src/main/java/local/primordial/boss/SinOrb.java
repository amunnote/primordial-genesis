package local.primordial.boss;

import java.util.UUID;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A seeking spell: Lust's charmed butterflies. It drifts out, then turns and hunts its target, curving after it for a
 * few seconds; it bursts on whatever it touches (5 magic damage, weakness and nausea) and fizzles against walls. Drawn
 * only by its trail of petals.
 */
public class SinOrb extends Projectile {
    static final EntityDataAccessor<Integer> COLOUR=SynchedEntityData.defineId(SinOrb.class,EntityDataSerializers.INT);
    public static final float DAMAGE=5;
    private UUID target;private int life;
    public SinOrb(EntityType<? extends SinOrb> type,Level level) {super(type,level);setNoGravity(true);}
    public static SinOrb loose(Level level,LivingEntity owner,LivingEntity target,Vec3 from,Vec3 motion,int colour) {
        var orb=new SinOrb(SinBosses.SIN_ORB.get(),level);
        orb.setOwner(owner);orb.setPos(from);orb.setDeltaMovement(motion);orb.target=target.getUUID();orb.entityData.set(COLOUR,colour);
        level.addFreshEntity(orb);
        return orb;
    }
    @Override protected void defineSynchedData() {entityData.define(COLOUR,0xF66ABC);}
    @Override public void tick() {
        super.tick();
        if(level().isClientSide){
            level().addParticle(SinParticles.PETAL.get().of(entityData.get(COLOUR)),getX(),getY(),getZ(),0,0,0);
            return;
        }
        if(++life>140){discard();return;}
        var motion=getDeltaMovement();
        if(life>8&&target!=null&&level() instanceof ServerLevel server&&server.getEntity(target) instanceof LivingEntity hunted&&hunted.isAlive()){
            var want=hunted.position().add(0,hunted.getBbHeight()*0.5,0).subtract(position()).normalize().scale(0.55);
            motion=motion.scale(0.88).add(want.scale(0.12));
        }
        HitResult hit=ProjectileUtil.getHitResultOnMoveVector(this,this::canHitEntity);
        if(hit.getType()!=HitResult.Type.MISS&&!net.minecraftforge.event.ForgeEventFactory.onProjectileImpact(this,hit)){onHit(hit);return;}
        setDeltaMovement(motion);
        setPos(getX()+motion.x,getY()+motion.y,getZ()+motion.z);
    }
    @Override protected boolean canHitEntity(Entity entity) {return super.canHitEntity(entity)&&!(entity instanceof SinEntity)&&!(entity instanceof SinOrb);}
    @Override protected void onHitEntity(EntityHitResult result) {
        var owner=getOwner() instanceof LivingEntity living?living:null;
        if(result.getEntity() instanceof LivingEntity target&&target.hurt(damageSources().indirectMagic(this,owner),DAMAGE)){
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,100,0),owner);
            target.addEffect(new MobEffectInstance(MobEffects.CONFUSION,80,0),owner);
        }
        burst();discard();
    }
    @Override protected void onHit(HitResult result) {super.onHit(result);if(!isRemoved()){burst();discard();}}
    private void burst() {
        if(level() instanceof ServerLevel server)SinParticles.send(server,SinParticles.PETAL.get(),entityData.get(COLOUR),position(),10,0.3,0,0.05,0);
    }
    @Override public boolean isPickable() {return false;}
}
