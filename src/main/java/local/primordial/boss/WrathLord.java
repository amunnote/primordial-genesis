package local.primordial.boss;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import local.primordial.affix.BossAffix;
import local.primordial.entity.alchemical.ArcaneSkill;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 暴怒之主 · 狂王 — Wrath, the Berserker. A hulking horned beast in broken chains whose fury only grows.
 * <ul>
 * <li>狂化 Rage: every wound adds a point of rage (up to 20), each making it 3% faster and harder hitting.</li>
 * <li>狂怒冲锋 Mad Charge: it paws the ground while a trail of embers marks its path, then charges straight down it,
 *     trampling all in its way (14, thrown hard). Run it into a wall and it staggers, open, for a second and a half.</li>
 * <li>碎地 Shatter: a leap and a slam, 12 damage and thrown up within six blocks.</li>
 * <li>怒吼 Roar (enraged): weakens and slows everything near and throws it back, and its rage goes to the full.</li>
 * <li>十二试炼 Twelve Labours: the first time it falls, it rises again with a third of its health, enraged.</li>
 * </ul>
 */
public class WrathLord extends SinBoss {
    public static final int MAX_RAGE=20;
    static final UUID RAGE_SPEED=UUID.fromString("5b3e1f20-77a1-4c3e-9f1a-1a2b3c4d5e01"),RAGE_DAMAGE=UUID.fromString("5b3e1f20-77a1-4c3e-9f1a-1a2b3c4d5e02");
    private int rage,charge,stagger;private Vec3 chargeDirection=Vec3.ZERO;private boolean revived;
    private final Set<UUID> trampled=new HashSet<>();

    public WrathLord(EntityType<? extends WrathLord> type,Level level) {
        super(type,level);
        skills.add(new ArcaneSkill(120,16,6,26,true,(mob,target)->target.position(),
            (level1,mob,target,mark,left)->{
                getNavigation().stop();getLookControl().setLookAt(target);
                var dir=mark.subtract(position()).multiply(1,0,1).normalize();
                if(left%2==0)SinParticles.line(level1,SinParticles.EMBER.get(),0xFF5A1E,position().add(0,0.1,0),position().add(dir.scale(22)).add(0,0.1,0),0.7,0.03);
                // it paws the ground: sparks and smoke kick up behind its feet
                SinParticles.send(level1,SinParticles.SPARK.get(),0xFF9A3C,position().add(0,0.3,0).subtract(dir.scale(1.2)),3,0.6,-dir.x*0.25,0.25,-dir.z*0.25);
                if(left%3==0)SinParticles.send(level1,SinParticles.SMOKE.get(),0x2A1A1A,position().add(0,0.4,0),2,0.8,0,0.03,0);
            },
            (level1,mob,target,mark)->{
                act(1,16);
                chargeDirection=mark.subtract(position()).multiply(1,0,1).normalize();charge=16;trampled.clear();
                sound(SoundEvents.RAVAGER_ROAR,2F,0.8F);
            }));
        skills.add(new ArcaneSkill(140,18,0,7,false,(mob,target)->position(),
            (level1,mob,target,mark,left)->{getNavigation().stop();if(left%3==0)SinParticles.ring(level1,SinParticles.EMBER.get(),0xFF5A1E,mark,6.0*left/18+0.5,16,0.05);},
            (level1,mob,target,mark)->{
                act(2,20);
                for(var e:foes(mark,6)){strike(e,12*rageMultiplier());ArcaneSkill.knock(e,mark,0.9,0.75);}
                SinShockwave.spread(level1,mark,7F,14,0xFF5A2A,false,0);
                SinShockwave.spread(level1,mark,4.5F,9,0xFFD27A,false,0);
                for(int i=0;i<40;i++){double a=Math.PI*2*i/40,s=0.4+getRandom().nextDouble()*0.4;
                    level1.sendParticles(SinParticles.SPARK.get().of(0xFFB347),mark.x+Math.cos(a)*1.2,mark.y+0.3,mark.z+Math.sin(a)*1.2,0,Math.cos(a)*s,0.35+getRandom().nextDouble()*0.4,Math.sin(a)*s,1);}
                SinParticles.send(level1,SinParticles.SMOKE.get(),0x241414,mark.add(0,0.6,0),24,2.5,0,0.05,0);
                level1.sendParticles(new net.minecraft.core.particles.BlockParticleOption(net.minecraft.core.particles.ParticleTypes.BLOCK,
                    level1.getBlockState(net.minecraft.core.BlockPos.containing(mark).below())),mark.x,mark.y+0.2,mark.z,60,2.5,0.2,2.5,0.3);
                sound(SoundEvents.GENERIC_EXPLODE,1.5F,0.6F);
            }));
        skills.add(new ArcaneSkill(320,12,0,12,false,(mob,target)->position(),
            (level1,mob,target,mark,left)->getNavigation().stop(),
            (level1,mob,target,mark)->{
                if(!enraged())return;
                act(3,24);
                for(var e:foes(mark,10)){
                    e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,120,1),this);e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,60,1),this);
                    ArcaneSkill.knock(e,mark,1.3,0.4);
                }
                setRage(MAX_RAGE);
                SinShockwave.spread(level1,position().add(0,0.1,0),10F,16,0xE63248,false,0);
                SinShockwave.spread(level1,position().add(0,2.6,0),8F,12,0xFF7A3C,true,getYRot());
                SinParticles.send(level1,SinParticles.EMBER.get(),0xFF5A1E,position().add(0,2.6,0),60,1.5,0,0.25,0);
                SinParticles.send(level1,SinParticles.SMOKE.get(),0x2A1414,position().add(0,2,0),20,2,0,0.08,0);
                sound(SoundEvents.RAVAGER_ROAR,3F,0.5F);
            }));
    }
    @Override protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(3,new MeleeAttackGoal(this,1.15,true));
    }
    @Override public BossAffix sin() {return BossAffix.WRATH;}
    @Override public String bossId() {return "wrath_lord";}
    @Override protected BarLook barLook() {return new BarLook(revived?2:0,rage,rage/20F,0);}
    @Override protected BossEvent.BossBarColor barColour() {return BossEvent.BossBarColor.RED;}
    @Override protected String[] actions() {return new String[]{"","charge","slam","roar","stagger"};}
    @Override protected SoundEvent wakeSound() {return SoundEvents.RAVAGER_ROAR;}

    public int rage() {return rage;}
    float rageMultiplier() {return 1+rage*0.03F;}
    void setRage(int value) {
        rage=Math.max(0,Math.min(MAX_RAGE,value));
        var speed=getAttribute(Attributes.MOVEMENT_SPEED);var damage=getAttribute(Attributes.ATTACK_DAMAGE);
        speed.removeModifier(RAGE_SPEED);damage.removeModifier(RAGE_DAMAGE);
        if(rage>0){
            speed.addTransientModifier(new AttributeModifier(RAGE_SPEED,"wrath rage",rage*0.03,AttributeModifier.Operation.MULTIPLY_BASE));
            damage.addTransientModifier(new AttributeModifier(RAGE_DAMAGE,"wrath rage",rage*0.03,AttributeModifier.Operation.MULTIPLY_BASE));
        }
    }
    @Override public boolean hurt(DamageSource source,float amount) {
        boolean hurt=super.hurt(source,amount);
        if(hurt&&!level().isClientSide)setRage(rage+1);
        return hurt;
    }
    /** Twelve Labours: the first death is refused */
    @Override public void die(DamageSource source) {
        if(!revived&&!level().isClientSide){
            revived=true;
            setHealth(getMaxHealth()*0.33F);
            entityData.set(ENRAGED,true);setRage(MAX_RAGE);
            if(level() instanceof ServerLevel level){
                SinParticles.send(level,SinParticles.EMBER.get(),0xFF5A1E,position().add(0,1.6,0),120,1.2,0,0.45,0);
                for(int i=0;i<16;i++)SinParticles.send(level,SinParticles.SPARK.get(),0xFFD27A,position().add(0,i*0.6,0),3,0.6,0,0.3,0);
                SinShockwave.spread(level,position(),12F,20,0xFF3A2A,false,0);
                for(var player:bar.getPlayers())player.displayClientMessage(Component.translatable("message.primordial.wrath_lord.revive").withStyle(s->s.withColor(colour()).withBold(true)),false);
            }
            sound(SoundEvents.TOTEM_USE,2F,0.6F);sound(SoundEvents.RAVAGER_ROAR,3F,0.5F);
            return;
        }
        super.die(source);
    }
    public boolean revived() {return revived;}

    @Override protected void fight(ServerLevel level) {
        if(stagger>0){stagger--;getNavigation().stop();setDeltaMovement(0,getDeltaMovement().y,0);return;}
        if(charge>0){
            charge--;getNavigation().stop();
            setDeltaMovement(chargeDirection.x*1.25,getDeltaMovement().y,chargeDirection.z*1.25);
            setYRot((float)Math.toDegrees(Math.atan2(chargeDirection.z,chargeDirection.x))-90);yBodyRot=getYRot();
            for(var e:foes(position(),1.6))if(trampled.add(e.getUUID())){strike(e,14*rageMultiplier());ArcaneSkill.knock(e,position(),2.0,0.55);}
            SinParticles.send(level,SinParticles.EMBER.get(),0xFF5A1E,position().add(0,0.3,0),3,0.6,0,0.05,0);
            SinParticles.send(level,SinParticles.SPARK.get(),0xFFB347,position().add(0,0.2,0),4,0.7,-chargeDirection.x*0.3,0.3,-chargeDirection.z*0.3);
            if(tickCount%2==0)SinParticles.send(level,SinParticles.SMOKE.get(),0x2A1A1A,position().add(0,0.5,0),2,0.6,0,0.03,0);
            if(horizontalCollision){
                charge=0;stagger=30;act(4,30);
                sound(SoundEvents.ANVIL_LAND,1.5F,0.5F);
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT,getX(),getY()+2.5,getZ(),20,0.5,0.3,0.5,0.3);
                var face=position().add(chargeDirection.scale(1.4)).add(0,1.6,0);
                SinShockwave.spread(level,face,4F,8,0xFFB347,true,getYRot());
                SinParticles.send(level,SinParticles.SPARK.get(),0xFFD27A,face,30,0.6,-chargeDirection.x*0.4,0.2,-chargeDirection.z*0.4);
            }
        }
    }
    @Override protected void onRest() {setRage(0);charge=0;stagger=0;}
    @Override protected void ambient() {
        if(dormant()&&getRandom().nextInt(3)!=0)return;
        var random=getRandom();
        double yaw=Math.toRadians(yBodyRot);
        // the crest of fire along its back, leaning forward with its hunch
        var back=new Vec3(Math.sin(yaw)*0.9,0,-Math.cos(yaw)*0.9);
        int n=enraged()?3:1;
        for(int i=0;i<n;i++){
            double up=2.4+random.nextDouble()*1.2;
            level().addParticle(SinParticles.EMBER.get().of(enraged()?0xFF4A1A:0xFF7A2A),getX()+back.x+(random.nextDouble()-0.5)*0.9,getY()+up,getZ()+back.z+(random.nextDouble()-0.5)*0.9,0,0.06,0);
        }
        if(random.nextInt(enraged()?2:4)==0)
            level().addParticle(SinParticles.SMOKE.get().of(0x2A1818),getX()+back.x+(random.nextDouble()-0.5),getY()+2.9,getZ()+back.z+(random.nextDouble()-0.5),0,0.02,0);
        // embers leaking from the cracks in its hide
        if(random.nextInt(3)==0)
            level().addParticle(SinParticles.EMBER.get().of(0xFF8A3C),getX()+(random.nextDouble()-0.5)*1.8,getY()+0.8+random.nextDouble()*2.2,getZ()+(random.nextDouble()-0.5)*1.8,0,0.02,0);
        if(enraged()&&random.nextInt(4)==0)
            level().addParticle(SinParticles.SPARK.get().of(0xFFD27A),getX()+(random.nextDouble()-0.5)*2,getY()+1+random.nextDouble()*2,getZ()+(random.nextDouble()-0.5)*2,(random.nextDouble()-0.5)*0.2,0.2,(random.nextDouble()-0.5)*0.2);
    }
    @Override public void addAdditionalSaveData(CompoundTag tag) {super.addAdditionalSaveData(tag);tag.putBoolean("Revived",revived);}
    @Override public void readAdditionalSaveData(CompoundTag tag) {super.readAdditionalSaveData(tag);revived=tag.getBoolean("Revived");}
}
