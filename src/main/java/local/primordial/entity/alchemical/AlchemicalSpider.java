package local.primordial.entity.alchemical;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;

/**
 * 炼金织蛛. A spider that stays hostile in daylight, is far sturdier, and binds what it bites: slowness and poison.
 * No arthropod, so Bane of Arthropods does nothing special.
 */
public class AlchemicalSpider extends Spider implements AlchemicalCreation {
    private final AnimatableInstanceCache cache=AlchemicalCreation.cache(this);
    public AlchemicalSpider(EntityType<? extends Spider> type,Level level) {super(type,level);}

    public static AttributeSupplier.Builder attributes() {
        return Spider.createAttributes().add(Attributes.MAX_HEALTH,28).add(Attributes.MOVEMENT_SPEED,0.32).add(Attributes.ATTACK_DAMAGE,4);
    }
    @Override public String creationId() {return "alchemical_spider";}
    @Override public MobType getMobType() {return MobType.UNDEFINED;}

    /** the spider's goals with plain attack and target goals: they no longer give up in the light */
    @Override protected void registerGoals() {
        goalSelector.addGoal(1,new FloatGoal(this));
        goalSelector.addGoal(3,new LeapAtTargetGoal(this,0.4F));
        goalSelector.addGoal(4,new MeleeAttackGoal(this,1.0,true));
        goalSelector.addGoal(5,new WaterAvoidingRandomStrollGoal(this,0.8));
        goalSelector.addGoal(6,new LookAtPlayerGoal(this,Player.class,8.0F));
        goalSelector.addGoal(6,new RandomLookAroundGoal(this));
        targetSelector.addGoal(1,new HurtByTargetGoal(this));
        targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Player.class,true));
        targetSelector.addGoal(3,new NearestAttackableTargetGoal<>(this,IronGolem.class,true));
    }
    @Override public boolean doHurtTarget(Entity target) {
        if(!super.doHurtTarget(target))return false;
        if(target instanceof LivingEntity living){
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,60,1),this);
            living.addEffect(new MobEffectInstance(MobEffects.POISON,80,0),this);
        }
        return true;
    }

    /**
     * 灵丝牵引 Soul-thread: seeing its foe 4 to 12 blocks off, it spins a thread of sparks to it and yanks it in, slowed and
     * poisoned, straight into its mandibles.
     */
    private final ArcaneSkill skill=new ArcaneSkill(120,10,4,12,true,
        (mob,target)->target.position(),
        (level,mob,target,mark,left)->{
            if(left%2==0)ArcaneSkill.line(level,mob,mob.position().add(0,0.6,0),target.position().add(0,target.getBbHeight()*0.5,0),0.6);
        },
        (level,mob,target,mark)->{
            var pull=mob.position().subtract(target.position());
            var horizontal=new net.minecraft.world.phys.Vec3(pull.x,0,pull.z);
            double distance=horizontal.length();
            if(distance>0.1)horizontal=horizontal.scale(Math.min(1.6,0.25+distance*0.16)/distance);
            target.setDeltaMovement(horizontal.x,0.38,horizontal.z);target.hurtMarked=true;
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,50,1),mob);
            target.addEffect(new MobEffectInstance(MobEffects.POISON,60,0),mob);
            ArcaneSkill.line(level,mob,mob.position().add(0,0.6,0),target.position().add(0,target.getBbHeight()*0.5,0),0.35);
            level.playSound(null,mob.getX(),mob.getY(),mob.getZ(),net.minecraft.sounds.SoundEvents.FISHING_BOBBER_RETRIEVE,net.minecraft.sounds.SoundSource.HOSTILE,1F,0.6F);
        });
    public ArcaneSkill skill() {return skill;}
    @Override protected void customServerAiStep() {super.customServerAiStep();skill.tick(this);}

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {AlchemicalCreation.movementAndAttack(this,controllers);}
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() {return cache;}
    // ------------------------------------------------------------------ its voice (see ModSounds)
    @Override protected net.minecraft.sounds.SoundEvent getAmbientSound() {return local.primordial.sound.ModSounds.voice("alchemical_spider","ambient");}
    @Override protected net.minecraft.sounds.SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) {return local.primordial.sound.ModSounds.voice("alchemical_spider","hurt");}
    @Override protected net.minecraft.sounds.SoundEvent getDeathSound() {return local.primordial.sound.ModSounds.voice("alchemical_spider","death");}
}
