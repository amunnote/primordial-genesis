package local.primordial.boss;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

/** A shade of Envy: a see-through copy of its lord, fragile but quick, gone in fifteen seconds. It drops nothing. */
public class EnvyShade extends Monster implements GeoEntity, SinEntity {
    public static final int LIFE=300;
    private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
    public EnvyShade(EntityType<? extends EnvyShade> type,Level level) {super(type,level);xpReward=0;}
    @Override protected void registerGoals() {
        goalSelector.addGoal(0,new FloatGoal(this));
        goalSelector.addGoal(2,new MeleeAttackGoal(this,1.2,true));
        targetSelector.addGoal(1,new NearestAttackableTargetGoal<>(this,Player.class,false));
    }
    @Override public int colour() {return local.primordial.affix.BossAffix.ENVY.color();}
    @Override public void tick() {
        super.tick();
        if(!level().isClientSide&&tickCount>LIFE)dissolve();
    }
    public void dissolve() {
        if(level() instanceof ServerLevel level)SinParticles.send(level,SinParticles.WISP.get(),colour(),position().add(0,1,0),16,0.5,0,0.04,0);
        discard();
    }
    @Override protected boolean shouldDropLoot() {return false;}
    @Override public boolean removeWhenFarAway(double distance) {return true;}
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        RawAnimation idle=RawAnimation.begin().thenLoop("animation.envy_lord.idle"),walk=RawAnimation.begin().thenLoop("animation.envy_lord.walk");
        RawAnimation attack=RawAnimation.begin().thenPlay("animation.envy_lord.attack");
        controllers.add(new AnimationController<>(this,"move",4,state->state.setAndContinue(state.isMoving()?walk:idle)));
        controllers.add(new AnimationController<>(this,"attack",2,state->{
            if(swinging)return state.setAndContinue(attack);
            state.getController().forceAnimationReset();return PlayState.STOP;
        }));
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() {return cache;}
    // ------------------------------------------------------------------ its voice (see ModSounds)
    @Override protected net.minecraft.sounds.SoundEvent getAmbientSound() {return local.primordial.sound.ModSounds.voice("envy_shade","ambient");}
    @Override protected net.minecraft.sounds.SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) {return local.primordial.sound.ModSounds.voice("envy_shade","hurt");}
    @Override protected net.minecraft.sounds.SoundEvent getDeathSound() {return local.primordial.sound.ModSounds.voice("envy_shade","death");}
}
