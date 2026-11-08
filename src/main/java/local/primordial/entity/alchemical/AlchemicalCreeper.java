package local.primordial.entity.alchemical;

import java.util.EnumSet;
import local.primordial.mixin.CreeperAccessor;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;

/**
 * 炼金爆破者. A creeper whose blast does not destroy it: it detonates again and again, every {@link #RECHARGE} ticks.
 * The blast leaves blocks whole. It is not scared of cats, keeps walking while its fuse burns, and once lit, backing
 * off a few blocks no longer puts the fuse out.
 */
public class AlchemicalCreeper extends Creeper implements AlchemicalCreation {
    public static final int RECHARGE=100;
    public static final float BLAST_RADIUS=2.0F;
    private final AnimatableInstanceCache cache=AlchemicalCreation.cache(this);
    private int recharge;
    public AlchemicalCreeper(EntityType<? extends Creeper> type,Level level) {super(type,level);}

    public static AttributeSupplier.Builder attributes() {
        return Creeper.createAttributes().add(Attributes.MAX_HEALTH,30).add(Attributes.MOVEMENT_SPEED,0.27);
    }
    @Override public String creationId() {return "alchemical_creeper";}
    @Override public boolean channeling() {return getSwelling(1)>0.05F||isAggressive();}
    @Override public MobType getMobType() {return MobType.UNDEFINED;}
    public int recharge() {return recharge;}

    @Override protected void registerGoals() {
        goalSelector.addGoal(1,new FloatGoal(this));
        goalSelector.addGoal(2,new RelentlessSwellGoal(this));
        goalSelector.addGoal(4,new MeleeAttackGoal(this,1.0,false));
        goalSelector.addGoal(5,new WaterAvoidingRandomStrollGoal(this,0.8));
        goalSelector.addGoal(6,new LookAtPlayerGoal(this,Player.class,8.0F));
        goalSelector.addGoal(6,new RandomLookAroundGoal(this));
        targetSelector.addGoal(1,new NearestAttackableTargetGoal<>(this,Player.class,true));
        targetSelector.addGoal(2,new HurtByTargetGoal(this));
    }
    @Override public void tick() {
        if(recharge>0)recharge--;
        super.tick();
        tickMines();
    }
    /** Called instead of the creeper's own explosion (see CreeperDetonationMixin): blast, survive, recharge. */
    public void detonate() {
        var access=(CreeperAccessor)this;
        access.angel$setSwell(0);access.angel$setOldSwell(0);
        if(level().isClientSide)return;
        level().explode(this,getX(),getY(),getZ(),BLAST_RADIUS*(isPowered()?2:1),Level.ExplosionInteraction.NONE);
        if(level() instanceof ServerLevel server)server.sendParticles(ParticleTypes.WITCH,getX(),getY(0.6),getZ(),20,0.4,0.5,0.4,0.05);
        entityData.set(CreeperAccessor.angel$ignited(),false);
        setSwellDir(-1);
        recharge=RECHARGE;
        layMines();
    }

    /**
     * 连锁符雷 Chain Sigils: each blast scatters three rune mines round its target (or round itself), each a small glyph
     * circle that blows {@link #MINE_FUSE} ticks later. Step out of the circles.
     */
    public static final int MINES=3,MINE_FUSE=30;public static final float MINE_POWER=1.5F;
    private final java.util.List<double[]> mines=new java.util.ArrayList<>();
    public int minesLaid() {return mines.size();}
    void layMines() {
        var target=getTarget();
        var centre=target!=null&&target.isAlive()&&distanceToSqr(target)<12*12?target.position():position();
        for(int i=0;i<MINES;i++){
            double a=getRandom().nextDouble()*Math.PI*2,r=1+getRandom().nextDouble()*2;
            mines.add(new double[]{centre.x+Math.cos(a)*r,centre.y,centre.z+Math.sin(a)*r,MINE_FUSE+i*6});
        }
    }
    private void tickMines() {
        if(!(level() instanceof ServerLevel server)||mines.isEmpty())return;
        for(var it=mines.iterator();it.hasNext();){
            var mine=it.next();
            var at=new net.minecraft.world.phys.Vec3(mine[0],mine[1],mine[2]);
            if(--mine[3]<=0){
                it.remove();
                server.explode(this,mine[0],mine[1]+0.1,mine[2],MINE_POWER,Level.ExplosionInteraction.NONE);
                ArcaneSkill.burst(server,this,at.add(0,0.3,0),12,0.3);
            }else if((int)mine[3]%3==0)ArcaneSkill.ring(server,this,at,0.9,6,0.03);
        }
    }

    /** Lights the fuse within 3 blocks and keeps it burning while the target stays within 16; does not stop the creeper moving. */
    static final class RelentlessSwellGoal extends Goal {
        private final AlchemicalCreeper creeper;
        RelentlessSwellGoal(AlchemicalCreeper creeper) {this.creeper=creeper;setFlags(EnumSet.noneOf(Goal.Flag.class));}
        @Override public boolean canUse() {
            var target=creeper.getTarget();
            return creeper.recharge==0&&target!=null&&target.isAlive()&&creeper.distanceToSqr(target)<9;
        }
        @Override public boolean canContinueToUse() {
            var target=creeper.getTarget();
            return creeper.recharge==0&&target!=null&&target.isAlive()&&creeper.distanceToSqr(target)<16*16;
        }
        @Override public void start() {creeper.setSwellDir(1);}
        @Override public void stop() {creeper.setSwellDir(-1);}
        @Override public boolean requiresUpdateEveryTick() {return true;}
        @Override public void tick() {creeper.setSwellDir(1);}
    }

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AlchemicalCreation.movementAndAttack(this,controllers);
        // the petals burst open and shake while the fuse burns
        var swell=software.bernie.geckolib.core.animation.RawAnimation.begin().thenLoop("animation.alchemical_creeper.swell");
        controllers.add(new software.bernie.geckolib.core.animation.AnimationController<>(this,"swell",3,state->
            getSwelling(state.getPartialTick())>0.05F?state.setAndContinue(swell):software.bernie.geckolib.core.object.PlayState.STOP));
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() {return cache;}
    // ------------------------------------------------------------------ its voice (see ModSounds)
    @Override protected net.minecraft.sounds.SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) {return local.primordial.sound.ModSounds.voice("alchemical_creeper","hurt");}
    @Override protected net.minecraft.sounds.SoundEvent getDeathSound() {return local.primordial.sound.ModSounds.voice("alchemical_creeper","death");}
}
