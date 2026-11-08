package local.primordial.entity.alchemical;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

/**
 * 炼金焰灵. A blaze sealed in a brass armillary: water, rain and snowballs no longer hurt it, and it has twice the health.
 */
public class AlchemicalBlaze extends Blaze implements AlchemicalCreation {
    private final AnimatableInstanceCache cache=AlchemicalCreation.cache(this);
    public AlchemicalBlaze(EntityType<? extends Blaze> type,Level level) {super(type,level);}

    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes().add(Attributes.ATTACK_DAMAGE,6).add(Attributes.MOVEMENT_SPEED,0.25)
            .add(Attributes.FOLLOW_RANGE,48).add(Attributes.MAX_HEALTH,40).add(Attributes.ARMOR,6);
    }
    @Override public String creationId() {return "alchemical_blaze";}
    @Override public boolean isSensitiveToWater() {return false;}
    @Override public boolean channeling() {return isOnFire()||isAggressive();}
    @Override public boolean hurt(DamageSource source,float amount) {
        if(source.getDirectEntity() instanceof Snowball)return false;
        return super.hurt(source,amount);
    }

    /**
     * 焚炉法阵 Pyre Sigil: seeing its foe within 14 blocks, it draws a circle of glyphs under it that fills with flame, and
     * after a second and a half the circle erupts: 6 damage, five seconds alight and a toss into the air for all inside.
     */
    public static final float SIGIL_DAMAGE=6;public static final double SIGIL_RADIUS=2.6;
    private final ArcaneSkill skill=new ArcaneSkill(180,30,0,14,true,
        (mob,target)->target.position(),
        (level,mob,target,mark,left)->{
            if(left%2==0)ArcaneSkill.ring(level,mob,mark,SIGIL_RADIUS,12,0.02);
            int flames=(30-left)/6+1;
            for(int i=0;i<flames;i++){
                double a=mob.getRandom().nextDouble()*Math.PI*2,r=mob.getRandom().nextDouble()*SIGIL_RADIUS;
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME,mark.x+Math.cos(a)*r,mark.y+0.1,mark.z+Math.sin(a)*r,1,0,0.02,0,0.01);
            }
        },
        (level,mob,target,mark)->{
            for(var e:ArcaneSkill.foesAround(mob,mark,SIGIL_RADIUS,3)){
                e.hurt(ArcaneSkill.magic(mob),SIGIL_DAMAGE);
                e.setSecondsOnFire(5);
                ArcaneSkill.knock(e,mark,0.2,0.6);
            }
            for(int i=0;i<40;i++){
                double a=Math.PI*2*i/40;
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME,mark.x+Math.cos(a)*SIGIL_RADIUS,mark.y+0.1,mark.z+Math.sin(a)*SIGIL_RADIUS,0,0,1,0,0.25);
            }
            ArcaneSkill.vanilla(level,net.minecraft.core.particles.ParticleTypes.LAVA,mark,12,1,0);
            ArcaneSkill.burst(level,mob,mark.add(0,0.4,0),20,0.4);
            level.playSound(null,mark.x,mark.y,mark.z,net.minecraft.sounds.SoundEvents.FIRECHARGE_USE,net.minecraft.sounds.SoundSource.HOSTILE,1.2F,0.7F);
        });
    public ArcaneSkill skill() {return skill;}
    @Override protected void customServerAiStep() {super.customServerAiStep();skill.tick(this);}

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AlchemicalCreation.movementAndAttack(this,controllers);
        // charging its volley: the rods flare out and the core blazes
        RawAnimation charge=RawAnimation.begin().thenLoop("animation.alchemical_blaze.charge");
        controllers.add(new AnimationController<>(this,"charge",3,state->isOnFire()?state.setAndContinue(charge):PlayState.STOP));
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() {return cache;}
    // ------------------------------------------------------------------ its voice (see ModSounds)
    @Override protected net.minecraft.sounds.SoundEvent getAmbientSound() {return local.primordial.sound.ModSounds.voice("alchemical_blaze","ambient");}
    @Override protected net.minecraft.sounds.SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) {return local.primordial.sound.ModSounds.voice("alchemical_blaze","hurt");}
    @Override protected net.minecraft.sounds.SoundEvent getDeathSound() {return local.primordial.sound.ModSounds.voice("alchemical_blaze","death");}
}
