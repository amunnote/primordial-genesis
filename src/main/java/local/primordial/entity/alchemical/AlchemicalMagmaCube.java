package local.primordial.entity.alchemical;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;

/**
 * 炼金熔核. A magma cube held to one size by a cage of plates round a floating core: always full grown, it never breaks
 * into the little cubes that make its kin harmless, leaps twice as often, and has far more health.
 */
public class AlchemicalMagmaCube extends MagmaCube implements AlchemicalCreation {
    public static final int SIZE=3;
    public static final double HEALTH=40,DAMAGE=7;
    private final AnimatableInstanceCache cache=AlchemicalCreation.cache(this);
    /** set while it dies, so it can drop to size one and leave no little cubes behind */
    private boolean dissolving;
    public AlchemicalMagmaCube(EntityType<? extends MagmaCube> type,Level level) {super(type,level);setSize(SIZE,true);}

    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes().add(Attributes.MOVEMENT_SPEED,0.2).add(Attributes.MAX_HEALTH,HEALTH).add(Attributes.ATTACK_DAMAGE,DAMAGE);
    }
    @Override public String creationId() {return "alchemical_magma_cube";}
    @Override public boolean channeling() {return !onGround();}
    @Override public void setSize(int size,boolean resetHealth) {
        super.setSize(dissolving?1:SIZE,resetHealth);
        if(dissolving)return;
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(HEALTH);
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(DAMAGE);
        if(resetHealth)setHealth(getMaxHealth());
    }
    @Override protected int getJumpDelay() {return super.getJumpDelay()/2;}
    @Override public void remove(RemovalReason reason) {
        // dying, it shrinks to nothing first: no little cubes split off
        if(!level().isClientSide&&isDeadOrDying()){dissolving=true;setSize(1,false);}
        super.remove(reason);
    }

    /**
     * 熔核坠击 Meteor Slam: with its foe 2 to 9 blocks off, it crouches in a closing ring of glyphs, then hurls itself in a
     * high arc at the foe; where it lands, magma bursts out: 7 damage, three seconds alight and thrown back, within 3.5 blocks.
     */
    public static final float SLAM_DAMAGE=7;public static final double SLAM_RADIUS=3.5;
    private boolean armed,wasOnGround=true;
    private final ArcaneSkill skill=new ArcaneSkill(110,12,2,9,false,
        (mob,target)->target.position(),
        (level,mob,target,mark,left)->{
            if(left%2==0)ArcaneSkill.ring(level,mob,mob.position(),1.6*left/12.0+0.6,10,0.03);
        },
        (level,mob,target,mark)->{
            var to=target.position().subtract(mob.position());
            mob.setDeltaMovement(to.x*0.11,0.95,to.z*0.11);mob.hasImpulse=true;
            ((AlchemicalMagmaCube)mob).armed=true;
            ArcaneSkill.burst(level,mob,mob.position().add(0,0.3,0),14,0.3);
            level.playSound(null,mob.getX(),mob.getY(),mob.getZ(),net.minecraft.sounds.SoundEvents.MAGMA_CUBE_JUMP,net.minecraft.sounds.SoundSource.HOSTILE,1.5F,0.6F);
        });
    public ArcaneSkill skill() {return skill;}
    /** its landing, if it came down from a slam leap */
    void land(net.minecraft.server.level.ServerLevel level) {
        armed=false;
        var at=position();
        for(var e:ArcaneSkill.foesAround(this,at,SLAM_RADIUS,2)){
            e.hurt(ArcaneSkill.melee(this),SLAM_DAMAGE);e.setSecondsOnFire(3);ArcaneSkill.knock(e,at,0.9,0.4);
        }
        for(double r=1;r<=SLAM_RADIUS;r+=1.25)ArcaneSkill.ring(level,this,at,r,(int)(r*8),0.1);
        ArcaneSkill.vanilla(level,net.minecraft.core.particles.ParticleTypes.LAVA,at,20,1.5,0);
        ArcaneSkill.vanilla(level,net.minecraft.core.particles.ParticleTypes.FLAME,at.add(0,0.2,0),30,1.5,0.08);
        level.playSound(null,at.x,at.y,at.z,net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE,net.minecraft.sounds.SoundSource.HOSTILE,0.8F,0.7F);
    }
    @Override protected void customServerAiStep() {
        super.customServerAiStep();
        skill.tick(this);
        if(armed&&onGround()&&!wasOnGround&&level() instanceof net.minecraft.server.level.ServerLevel level)land(level);
        wasOnGround=onGround();
    }

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        RawAnimation idle=RawAnimation.begin().thenLoop("animation.alchemical_magma_cube.idle");
        RawAnimation leap=RawAnimation.begin().thenLoop("animation.alchemical_magma_cube.leap");
        controllers.add(new AnimationController<>(this,"move",3,state->state.setAndContinue(onGround()?idle:leap)));
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() {return cache;}
    // ------------------------------------------------------------------ its voice (see ModSounds)
    @Override protected net.minecraft.sounds.SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) {return local.primordial.sound.ModSounds.voice("alchemical_magma_cube","hurt");}
    @Override protected net.minecraft.sounds.SoundEvent getDeathSound() {return local.primordial.sound.ModSounds.voice("alchemical_magma_cube","death");}
}
