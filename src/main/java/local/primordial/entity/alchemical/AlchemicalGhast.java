package local.primordial.entity.alchemical;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

/**
 * 炼金魂囊. A ghast whose hide is plated in brass: fireballs, its own sent back or anyone else's, do not hurt it, so it can
 * no longer be killed by batting its shot back. Four times the health, and between its great fireballs it sprays a fan of
 * three small ones at its target.
 */
public class AlchemicalGhast extends Ghast implements AlchemicalCreation {
    /** ticks between sprays of small fireballs */
    public static final int SPRAY_INTERVAL=70;
    private final AnimatableInstanceCache cache=AlchemicalCreation.cache(this);
    private int spray=SPRAY_INTERVAL;
    public AlchemicalGhast(EntityType<? extends Ghast> type,Level level) {super(type,level);}

    public static AttributeSupplier.Builder attributes() {
        return Ghast.createAttributes().add(Attributes.MAX_HEALTH,40).add(Attributes.ARMOR,4);
    }
    @Override public String creationId() {return "alchemical_ghast";}
    @Override public boolean channeling() {return isCharging();}
    /** no fireball hurts it: neither the blast nor the hit, whoever sent it */
    public static boolean fireball(DamageSource source) {return source.getDirectEntity() instanceof AbstractHurtingProjectile;}
    @Override public boolean hurt(DamageSource source,float amount) {
        if(fireball(source))return false;
        return super.hurt(source,amount);
    }

    /**
     * 魂灯凝视 Soul Gaze: seeing its foe within 40 blocks, its eye gathers light for a second, then holds a beam of soul
     * fire on it for two seconds while it stays in sight: 2.5 damage every half second, slowed, and every wound heals the
     * soulbag as much. Break line of sight to break the beam.
     */
    public static final int BEAM_TICKS=40;public static final float BEAM_DAMAGE=2.5F;
    private int beam;private net.minecraft.world.entity.LivingEntity beamTarget;
    private final ArcaneSkill skill=new ArcaneSkill(200,20,0,40,true,
        (mob,target)->target.position(),
        (level,mob,target,mark,left)->{
            var eye=((AlchemicalGhast)mob).eye();
            ArcaneSkill.spark(level,mob,eye.x+(mob.getRandom().nextDouble()-0.5)*3,eye.y+(mob.getRandom().nextDouble()-0.5)*3,eye.z+(mob.getRandom().nextDouble()-0.5)*3,0,0,0);
            if(left%4==0)ArcaneSkill.burst(level,mob,eye,3,0.05);
        },
        (level,mob,target,mark)->{
            var ghast=(AlchemicalGhast)mob;
            ghast.beam=BEAM_TICKS;ghast.beamTarget=target;
            level.playSound(null,mob.getX(),mob.getY(),mob.getZ(),net.minecraft.sounds.SoundEvents.BEACON_ACTIVATE,net.minecraft.sounds.SoundSource.HOSTILE,2F,1.6F);
        });
    public ArcaneSkill skill() {return skill;}
    public int beamLeft() {return beam;}
    net.minecraft.world.phys.Vec3 eye() {var look=getViewVector(1);return position().add(look.x*2.2,2.4,look.z*2.2);}
    private void tickBeam() {
        if(beam<=0||!(level() instanceof net.minecraft.server.level.ServerLevel level))return;
        beam--;
        var target=beamTarget;
        if(target==null||!target.isAlive()||!hasLineOfSight(target)){beam=0;return;}
        getLookControl().setLookAt(target);
        var from=eye();var to=target.position().add(0,target.getBbHeight()*0.6,0);
        if(beam%2==0)ArcaneSkill.line(level,this,from,to,0.5);
        if(beam%10==0){
            if(target.hurt(ArcaneSkill.magic(this),BEAM_DAMAGE))heal(BEAM_DAMAGE);
            target.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN,20,1),this);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,to.x,to.y,to.z,6,0.3,0.4,0.3,0.02);
        }
    }

    @Override protected void customServerAiStep() {
        super.customServerAiStep();
        skill.tick(this);tickBeam();
        var target=getTarget();
        if(target==null||!target.isAlive()||distanceToSqr(target)>64*64||!hasLineOfSight(target)){spray=SPRAY_INTERVAL;return;}
        if(--spray>0)return;
        spray=SPRAY_INTERVAL;
        var look=getViewVector(1);
        double x=getX()+look.x*2.5,y=getY(0.5),z=getZ()+look.z*2.5;
        double dx=target.getX()-x,dy=target.getY(0.5)-y,dz=target.getZ()-z;
        for(int i=-1;i<=1;i++){
            double angle=i*0.18,cos=Math.cos(angle),sin=Math.sin(angle);
            var ball=new SmallFireball(level(),this,dx*cos-dz*sin,dy,dx*sin+dz*cos);
            ball.setPos(x,y,z);
            level().addFreshEntity(ball);
        }
        playSound(SoundEvents.BLAZE_SHOOT,2F,0.7F);
    }

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        RawAnimation idle=RawAnimation.begin().thenLoop("animation.alchemical_ghast.idle");
        RawAnimation walk=RawAnimation.begin().thenLoop("animation.alchemical_ghast.walk");
        controllers.add(new AnimationController<>(this,"move",6,state->state.setAndContinue(state.isMoving()?walk:idle)));
        // about to fire: the maw gapes and the eye flares
        RawAnimation charge=RawAnimation.begin().thenLoop("animation.alchemical_ghast.charge");
        controllers.add(new AnimationController<>(this,"charge",3,state->isCharging()?state.setAndContinue(charge):PlayState.STOP));
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() {return cache;}
    // ------------------------------------------------------------------ its voice (see ModSounds)
    @Override protected net.minecraft.sounds.SoundEvent getAmbientSound() {return local.primordial.sound.ModSounds.voice("alchemical_ghast","ambient");}
    @Override protected net.minecraft.sounds.SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) {return local.primordial.sound.ModSounds.voice("alchemical_ghast","hurt");}
    @Override protected net.minecraft.sounds.SoundEvent getDeathSound() {return local.primordial.sound.ModSounds.voice("alchemical_ghast","death");}
}
