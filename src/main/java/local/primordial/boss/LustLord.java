package local.primordial.boss;

import java.util.ArrayList;
import java.util.List;
import local.primordial.affix.BossAffix;
import local.primordial.entity.alchemical.ArcaneSkill;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 色欲之主 · 魅影魔女 — Lust, the Caster. A floating enchantress on butterfly wings who never lets you near, and never
 * lets you go.
 * <ul>
 * <li>魅惑之吻 Charm: a stream of petals finds you; then for three seconds you are drawn helplessly toward her, dizzy and
 *     weakened, while she drinks from you.</li>
 * <li>幻蝶 Butterflies: five seeking butterflies (eight enraged) curve after you; each bursts for 5, weakening and dizzying.</li>
 * <li>镜花水月 Mirage: she vanishes to elsewhere in her garden, leaving a bloom that bursts a second later (8 within three blocks).</li>
 * </ul>
 */
public class LustLord extends SinBoss {
    public static final int CHARM_TICKS=60,BUTTERFLIES=5,ENRAGED_BUTTERFLIES=8;public static final float BLOOM=8;
    private int charm;private LivingEntity charmed;private final List<double[]> blooms=new ArrayList<>();

    public LustLord(EntityType<? extends LustLord> type,Level level) {
        super(type,level);
        setNoGravity(true);
        skills.add(new ArcaneSkill(200,20,4,22,true,(mob,target)->target.position(),
            (level1,mob,target,mark,left)->{if(left%2==0)SinParticles.line(level1,SinParticles.PETAL.get(),colour(),position().add(0,1.5,0),target.position().add(0,1.2,0),0.6,0);},
            (level1,mob,target,mark)->{
                act(1,CHARM_TICKS);charm=CHARM_TICKS;charmed=target;
                target.addEffect(new MobEffectInstance(MobEffects.CONFUSION,100,0),this);target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,100,1),this);
                sound(SoundEvents.ALLAY_ITEM_GIVEN,2F,0.6F);
            }));
        skills.add(new ArcaneSkill(70,14,0,32,false,(mob,target)->target.position(),
            (level1,mob,target,mark,left)->SinParticles.send(level1,SinParticles.PETAL.get(),colour(),position().add(0,1.5,0),2,0.8,0,0.02,0),
            (level1,mob,target,mark)->{
                act(2,14);
                int count=enraged()?ENRAGED_BUTTERFLIES:BUTTERFLIES;
                var toward=target.position().subtract(position()).multiply(1,0,1).normalize();
                for(int i=0;i<count;i++){
                    double a=(i/(count-1.0)-0.5)*Math.PI*1.2;
                    var motion=new Vec3(toward.x*Math.cos(a)-toward.z*Math.sin(a),0.25,toward.x*Math.sin(a)+toward.z*Math.cos(a)).scale(0.45);
                    SinOrb.loose(level1,this,target,position().add(0,1.6,0),motion,colour());
                }
                sound(SoundEvents.ALLAY_THROW,2F,0.8F);
            }));
        skills.add(new ArcaneSkill(180,10,0,30,false,(mob,target)->position(),
            (level1,mob,target,mark,left)->SinParticles.ring(level1,SinParticles.PETAL.get(),colour(),mark,1.5,10,0.05),
            (level1,mob,target,mark)->{
                act(3,10);
                blooms.add(new double[]{mark.x,mark.y,mark.z,20});
                var seat=Vec3.atBottomCenterOf(home());
                double a=getRandom().nextDouble()*Math.PI*2,r=4+getRandom().nextDouble()*(ARENA_RADIUS-10);
                var to=seat.add(Math.cos(a)*r,2,Math.sin(a)*r);
                SinParticles.send(level1,SinParticles.PETAL.get(),colour(),position().add(0,1.2,0),20,0.6,0,0.05,0);
                teleportTo(to.x,to.y,to.z);
                SinParticles.send(level1,SinParticles.PETAL.get(),colour(),to.add(0,1.2,0),20,0.6,0,0.05,0);
                sound(SoundEvents.ILLUSIONER_MIRROR_MOVE,1.5F,1.4F);
            }));
    }
    @Override public BossAffix sin() {return BossAffix.LUST;}
    @Override public String bossId() {return "lust_lord";}
    @Override protected BarLook barLook() {return new BarLook(charm>0?2:0,0,-1,0);}
    @Override protected BossEvent.BossBarColor barColour() {return BossEvent.BossBarColor.PINK;}
    @Override protected String[] actions() {return new String[]{"","charm","cast","mirage"};}
    @Override protected SoundEvent wakeSound() {return SoundEvents.ALLAY_AMBIENT_WITH_ITEM;}
    public boolean charming(LivingEntity e) {return charm>0&&charmed==e;}

    @Override protected void fight(ServerLevel level) {
        var target=getTarget();var seat=Vec3.atBottomCenterOf(home());
        // float a little above the ground, keeping eight blocks from her target
        Vec3 want;
        if(target!=null){
            var away=position().subtract(target.position()).multiply(1,0,1);
            away=away.lengthSqr()<1e-3?new Vec3(1,0,0):away.normalize();
            want=target.position().add(away.scale(charm>0?6:9)).add(0,2,0);
            if(want.distanceTo(seat)>ARENA_RADIUS-3)want=seat.add(want.subtract(seat).normalize().scale(ARENA_RADIUS-3)).add(0,2,0);
            getLookControl().setLookAt(target,30,30);
            setYRot((float)(Mth.atan2(target.getZ()-getZ(),target.getX()-getX())*Mth.RAD_TO_DEG)-90);yBodyRot=getYRot();
        }else want=seat.add(0,2,0);
        var move=want.subtract(position()).scale(0.05);
        if(move.length()>0.35)move=move.normalize().scale(0.35);
        setDeltaMovement(move);
        // the charmed are drawn in
        if(charm>0){
            charm--;
            if(charmed==null||!charmed.isAlive()){charm=0;}
            else{
                var pull=position().subtract(charmed.position()).multiply(1,0,1);
                if(pull.length()>1.5){pull=pull.normalize().scale(0.24);charmed.setDeltaMovement(pull.x,charmed.getDeltaMovement().y,pull.z);charmed.hurtMarked=true;}
                if(charm%10==0){heal(2);SinParticles.line(level,SinParticles.PETAL.get(),colour(),charmed.position().add(0,1.2,0),position().add(0,1.5,0),0.7,0);}
            }
        }
        for(var it=blooms.iterator();it.hasNext();){
            var bloom=it.next();var at=new Vec3(bloom[0],bloom[1],bloom[2]);
            if(--bloom[3]<=0){
                it.remove();
                for(var e:foes(at,3))spell(e,BLOOM);
                SinParticles.send(level,SinParticles.PETAL.get(),colour(),at.add(0,0.8,0),30,1.2,0,0.15,0);
                sound(SoundEvents.CHERRY_LEAVES_BREAK,2F,0.6F);
            }else if((int)bloom[3]%4==0)SinParticles.ring(level,SinParticles.PETAL.get(),colour(),at,3,14,0.04);
        }
    }
    @Override protected void onRest() {charm=0;blooms.clear();}
    @Override protected void ambient() {
        if(getRandom().nextInt(2)!=0)return;
        double a=getRandom().nextDouble()*Math.PI*2;
        level().addParticle(SinParticles.PETAL.get().of(colour()),getX()+Math.cos(a)*1.4,getY()+1+getRandom().nextDouble()*1.4,getZ()+Math.sin(a)*1.4,0,-0.01,0);
    }
}
