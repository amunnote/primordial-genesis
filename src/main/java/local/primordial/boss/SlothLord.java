package local.primordial.boss;

import local.primordial.affix.BossAffix;
import local.primordial.entity.alchemical.ArcaneSkill;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 懒惰之主 · 沉眠剑王 — Sloth, the Saber. A sleep-walking knight with a vast sword, barely awake and in no hurry.
 * <ul>
 * <li>怠惰领域 Field of Sloth: within fourteen blocks of it everything is slowed and dulled (Slowness I, II enraged;
 *     Mining Fatigue).</li>
 * <li>假寐 Doze: it nods off for three seconds, taking 60% less harm and mending fast.</li>
 * <li>誓约之剑 Oath Blade: it raises its sword for two and a half seconds while a line of light runs out along the ground
 *     toward you; then the light becomes a thirty-block blade (18). Step off the line.</li>
 * <li>剑压 Sword Pressure: a half-circle sweep before it (13, thrown back).</li>
 * </ul>
 */
public class SlothLord extends SinBoss {
    public static final double FIELD=14,BLADE_LENGTH=30,BLADE_WIDTH=1.8;public static final float BLADE=18,SWEEP=13;
    public static final int DOZE_TICKS=60;
    private int doze;private Vec3 bladeFrom=Vec3.ZERO,bladeDirection=Vec3.ZERO;

    public SlothLord(EntityType<? extends SlothLord> type,Level level) {
        super(type,level);
        skills.add(new ArcaneSkill(320,10,0,40,false,(mob,target)->position(),
            (level1,mob,target,mark,left)->getNavigation().stop(),
            (level1,mob,target,mark)->{act(1,DOZE_TICKS);doze=DOZE_TICKS;sound(SoundEvents.FOX_SLEEP,2F,0.5F);}));
        skills.add(new ArcaneSkill(200,50,5,30,false,(mob,target)->{
                bladeFrom=position();bladeDirection=target.position().subtract(position()).multiply(1,0,1).normalize();return target.position();
            },
            (level1,mob,target,mark,left)->{
                getNavigation().stop();
                if(left==49)act(2,52);
                if(left%3==0)SinParticles.line(level1,SinParticles.DREAM.get(),colour(),bladeFrom.add(0,0.1,0),bladeFrom.add(bladeDirection.scale(BLADE_LENGTH*(1-left/50.0))).add(0,0.1,0),0.7,0.01);
                if(left%2==0)level1.sendParticles(ParticleTypes.END_ROD,getX(),getY()+3+(50-left)*0.06,getZ(),2,0.2,0.4,0.2,0.01);
            },
            (level1,mob,target,mark)->oathBlade(level1)));
        skills.add(new ArcaneSkill(90,10,0,5,false,(mob,target)->position(),
            (level1,mob,target,mark,left)->{getNavigation().stop();getLookControl().setLookAt(target);},
            (level1,mob,target,mark)->{
                act(3,10);swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                var facing=Vec3.directionFromRotation(0,getYRot());
                for(var e:foes(position(),5)){
                    var to=e.position().subtract(position()).multiply(1,0,1);
                    if(to.lengthSqr()<0.25||to.normalize().dot(facing)>=0){strike(e,SWEEP);ArcaneSkill.knock(e,position(),1.2,0.3);}
                }
                for(int i=0;i<=12;i++){double a=Math.toRadians(getYRot()+90)+Math.toRadians(-90+15*i);
                    level1.sendParticles(SinParticles.DREAM.get().of(colour()),getX()+Math.cos(a)*3.5,getY()+1.2,getZ()+Math.sin(a)*3.5,0,0,0.02,0,1);}
                sound(SoundEvents.PLAYER_ATTACK_SWEEP,2F,0.5F);
            }));
    }
    @Override protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(3,new MeleeAttackGoal(this,1.0,true));
    }
    @Override public BossAffix sin() {return BossAffix.SLOTH;}
    @Override public String bossId() {return "sloth_lord";}
    @Override protected BarLook barLook() {return new BarLook(doze>0?2:0,0,-1,0);}
    @Override protected BossEvent.BossBarColor barColour() {return BossEvent.BossBarColor.BLUE;}
    @Override protected String[] actions() {return new String[]{"","doze","oath","sweep"};}
    @Override protected SoundEvent wakeSound() {return SoundEvents.WARDEN_EMERGE;}
    public boolean dozing() {return doze>0;}

    /** whether a spot lies on the blade's line */
    public boolean onBlade(Vec3 at) {
        var rel=at.subtract(bladeFrom).multiply(1,0,1);
        double along=rel.dot(bladeDirection);
        if(along<-1||along>BLADE_LENGTH)return false;
        var side=rel.subtract(bladeDirection.scale(along));
        return side.length()<=BLADE_WIDTH&&Math.abs(at.y-bladeFrom.y)<4;
    }
    void oathBlade(ServerLevel level) {
        swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        for(var e:foes(bladeFrom.add(bladeDirection.scale(BLADE_LENGTH/2)),BLADE_LENGTH/2+2))if(onBlade(e.position()))spell(e,BLADE);
        for(double d=0;d<=BLADE_LENGTH;d+=0.6){
            var p=bladeFrom.add(bladeDirection.scale(d)).add(0,0.6,0);
            level.sendParticles(ParticleTypes.END_ROD,p.x,p.y,p.z,1,0.1,0.4,0.1,0.02);
            level.sendParticles(SinParticles.DREAM.get().of(colour()),p.x,p.y+0.6,p.z,0,0,0.05,0,1);
        }
        level.playSound(null,getX(),getY(),getZ(),SoundEvents.LIGHTNING_BOLT_THUNDER,net.minecraft.sounds.SoundSource.HOSTILE,1.2F,1.6F);
        sound(SoundEvents.BEACON_POWER_SELECT,3F,0.5F);
    }
    @Override public boolean hurt(DamageSource source,float amount) {return super.hurt(source,doze>0?amount*0.4F:amount);}
    @Override protected void fight(ServerLevel level) {
        if(tickCount%20==0)
            for(var e:foes(position(),FIELD)){
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,40,enraged()?1:0,false,true),this);
                e.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN,40,0,false,true),this);
            }
        if(tickCount%10==0)SinParticles.ring(level,SinParticles.DREAM.get(),colour(),position(),FIELD,20,0.01);
        if(doze>0){
            doze--;getNavigation().stop();setTarget(doze>0?null:getTarget());
            if(doze%20==0)heal(getMaxHealth()*0.015F);
            if(doze%8==0)level.sendParticles(SinParticles.DREAM.get().of(colour()),getX(),getY()+2.9,getZ(),0,0.02,0.03,0,1);
        }
    }
    @Override public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {return doze<=0&&super.doHurtTarget(target);}
    @Override protected void onRest() {doze=0;}
    @Override protected void ambient() {
        if(getRandom().nextInt(6)!=0)return;
        level().addParticle(SinParticles.DREAM.get().of(colour()),getX()+(getRandom().nextDouble()-0.5),getY()+2.8,getZ()+(getRandom().nextDouble()-0.5),0.01,0.02,0);
    }
}
