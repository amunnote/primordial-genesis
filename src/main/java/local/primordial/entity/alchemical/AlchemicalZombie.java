package local.primordial.entity.alchemical;

import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;

/**
 * 炼金尸傀. A zombie that does not burn in daylight, never drowns or turns into a drowned, is faster and tougher,
 * always breaks doors (on Hard, as zombies do) and is no undead, so Smite and healing potions do nothing special.
 */
public class AlchemicalZombie extends Zombie implements AlchemicalCreation {
    private final AnimatableInstanceCache cache=AlchemicalCreation.cache(this);
    public AlchemicalZombie(EntityType<? extends Zombie> type,Level level) {super(type,level);}

    public static AttributeSupplier.Builder attributes() {
        return Zombie.createAttributes().add(Attributes.MAX_HEALTH,30).add(Attributes.MOVEMENT_SPEED,0.29)
            .add(Attributes.ARMOR,4).add(Attributes.ATTACK_DAMAGE,4);
    }
    @Override public String creationId() {return "alchemical_zombie";}
    @Override protected boolean isSunSensitive() {return false;}
    @Override protected boolean convertsInWater() {return false;}
    @Override public boolean canBreatheUnderwater() {return true;}
    @Override public MobType getMobType() {return MobType.UNDEFINED;}
    /** always full grown: the model has no baby form */
    @Override public void setBaby(boolean baby) {super.setBaby(false);}
    @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor level,DifficultyInstance difficulty,MobSpawnType reason,
                                                  SpawnGroupData data,net.minecraft.nbt.CompoundTag tag) {
        SpawnGroupData result=super.finalizeSpawn(level,difficulty,reason,data,tag);
        setCanBreakDoors(true);
        return result;
    }

    /**
     * 符文震地 Rune Slam: with its foe within 4.5 blocks it plants itself while a ring of glyphs closes in round it, then
     * brings the gauntlet down: everything within 3.5 blocks takes 7 damage, is thrown up and out, and slowed.
     */
    public static final float SLAM_DAMAGE=7;public static final double SLAM_RADIUS=3.5;
    private final ArcaneSkill skill=new ArcaneSkill(160,18,0,4.5,false,
        (mob,target)->mob.position(),
        (level,mob,target,mark,left)->{
            mob.getNavigation().stop();
            if(left%3==0)ArcaneSkill.ring(level,mob,mark,SLAM_RADIUS*left/18.0+0.5,10,0.02);
        },
        (level,mob,target,mark)->{
            mob.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            for(var e:ArcaneSkill.foesAround(mob,mark,SLAM_RADIUS,2)){
                e.hurt(ArcaneSkill.melee(mob),SLAM_DAMAGE);
                ArcaneSkill.knock(e,mark,0.7,0.55);
                e.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN,40,1),mob);
            }
            for(double r=1;r<=SLAM_RADIUS;r+=1.25)ArcaneSkill.ring(level,mob,mark,r,(int)(r*8),0.12);
            ArcaneSkill.burst(level,mob,mark.add(0,0.3,0),24,0.35);
            ArcaneSkill.vanilla(level,net.minecraft.core.particles.ParticleTypes.EXPLOSION,mark,2,1.2,0);
            level.playSound(null,mark.x,mark.y,mark.z,net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE,net.minecraft.sounds.SoundSource.HOSTILE,0.7F,1.5F);
        });
    public ArcaneSkill skill() {return skill;}
    @Override protected void customServerAiStep() {super.customServerAiStep();skill.tick(this);}

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {AlchemicalCreation.movementAndAttack(this,controllers);}
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() {return cache;}
    // ------------------------------------------------------------------ its voice (see ModSounds)
    @Override protected net.minecraft.sounds.SoundEvent getAmbientSound() {return local.primordial.sound.ModSounds.voice("alchemical_zombie","ambient");}
    @Override protected net.minecraft.sounds.SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) {return local.primordial.sound.ModSounds.voice("alchemical_zombie","hurt");}
    @Override protected net.minecraft.sounds.SoundEvent getDeathSound() {return local.primordial.sound.ModSounds.voice("alchemical_zombie","death");}
}
