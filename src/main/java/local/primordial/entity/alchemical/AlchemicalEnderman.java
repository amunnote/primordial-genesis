package local.primordial.entity.alchemical;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

/**
 * 炼金行者. An enderman that water and rain no longer hurt, and that does not flee the daylight: it keeps its target
 * under the open sky instead of teleporting away. Tougher and harder hitting.
 */
public class AlchemicalEnderman extends EnderMan implements AlchemicalCreation {
    private final AnimatableInstanceCache cache=AlchemicalCreation.cache(this);
    public AlchemicalEnderman(EntityType<? extends EnderMan> type,Level level) {super(type,level);}

    public static AttributeSupplier.Builder attributes() {
        return EnderMan.createAttributes().add(Attributes.MAX_HEALTH,60).add(Attributes.ATTACK_DAMAGE,9);
    }
    @Override public String creationId() {return "alchemical_enderman";}
    @Override public boolean isSensitiveToWater() {return false;}
    @Override public boolean channeling() {return isCreepy()||isAggressive();}
    /** the enderman's only own step here is dropping its target and teleporting away in daylight: instead, its rift strike */
    @Override protected void customServerAiStep() {skill.tick(this);}

    /**
     * 虚空折跃斩 Rift Strike: with its foe 3 to 16 blocks off, sparks gather round it and a glyph circle opens behind the
     * foe; then it steps through the void to that spot and strikes from behind for one and a half times its blow, blinding.
     */
    public static final float RIFT_MULTIPLIER=1.5F;
    private final ArcaneSkill skill=new ArcaneSkill(140,12,3,16,false,
        (mob,target)->behind(target),
        (level,mob,target,mark,left)->{
            mob.getNavigation().stop();
            var spot=behind(target);
            if(left%2==0)ArcaneSkill.ring(level,mob,spot,0.9,8,0.05);
            ArcaneSkill.spark(level,mob,mob.getX()+(mob.getRandom().nextDouble()-0.5)*2,mob.getY(0.5)+(mob.getRandom().nextDouble()-0.5)*2,mob.getZ()+(mob.getRandom().nextDouble()-0.5)*2,0,0,0);
        },
        (level,mob,target,mark)->{
            var from=mob.position();
            var spot=behind(target);
            if(!level.noCollision(mob,mob.getDimensions(mob.getPose()).makeBoundingBox(spot)))spot=target.position();
            mob.teleportTo(spot.x,spot.y,spot.z);
            mob.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,target.getEyePosition());
            mob.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            target.hurt(ArcaneSkill.melee(mob),(float)mob.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)*RIFT_MULTIPLIER);
            target.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.BLINDNESS,40,0),mob);
            ArcaneSkill.burst(level,mob,from.add(0,1.4,0),16,0.25);ArcaneSkill.burst(level,mob,spot.add(0,1.4,0),16,0.25);
            ArcaneSkill.line(level,mob,from.add(0,1.4,0),spot.add(0,1.4,0),0.7);
            level.playSound(null,spot.x,spot.y,spot.z,net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT,net.minecraft.sounds.SoundSource.HOSTILE,1F,0.7F);
        });
    /** a step and a half behind where the foe is looking */
    static net.minecraft.world.phys.Vec3 behind(net.minecraft.world.entity.LivingEntity target) {
        var look=target.getLookAngle();var flat=new net.minecraft.world.phys.Vec3(look.x,0,look.z);
        if(flat.lengthSqr()<1e-4)flat=new net.minecraft.world.phys.Vec3(0,0,1);
        return target.position().subtract(flat.normalize().scale(1.6));
    }
    public ArcaneSkill skill() {return skill;}

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AlchemicalCreation.movementAndAttack(this,controllers);
        RawAnimation carry=RawAnimation.begin().thenLoop("animation.alchemical_enderman.carry");
        controllers.add(new AnimationController<>(this,"carry",4,state->
            getCarriedBlock()!=null?state.setAndContinue(carry):PlayState.STOP));
        // stared at: the head splits open and shakes
        RawAnimation creepy=RawAnimation.begin().thenLoop("animation.alchemical_enderman.creepy");
        controllers.add(new AnimationController<>(this,"creepy",3,state->isCreepy()?state.setAndContinue(creepy):PlayState.STOP));
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() {return cache;}
    // ------------------------------------------------------------------ its voice (see ModSounds)
    @Override protected net.minecraft.sounds.SoundEvent getAmbientSound() {return local.primordial.sound.ModSounds.voice("alchemical_enderman","ambient");}
    @Override protected net.minecraft.sounds.SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) {return local.primordial.sound.ModSounds.voice("alchemical_enderman","hurt");}
    @Override protected net.minecraft.sounds.SoundEvent getDeathSound() {return local.primordial.sound.ModSounds.voice("alchemical_enderman","death");}
}
