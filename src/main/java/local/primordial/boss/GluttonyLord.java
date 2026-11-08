package local.primordial.boss;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import local.primordial.affix.BossAffix;
import local.primordial.entity.alchemical.ArcaneSkill;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 暴食之主 · 噬魂枪王 — Gluttony, the Lancer. A bloated armoured lancer with a maw for a belly, never full.
 * <ul>
 * <li>噬伤 Devoured: its lance's wounds stop all healing (eight seconds after a lunge or a thrown lance, five after a blow),
 *     and every blow it lands feeds it a quarter of the damage.</li>
 * <li>噬魂突刺 Soul Lunge: it drives forward nine blocks behind its lance (14, devoured, starving).</li>
 * <li>暴食吞噬 Gorge: its maw opens and for two seconds draws in everything within fourteen blocks, then snaps shut on
 *     whatever is close (16), healing twice what it bites.</li>
 * <li>投枪 Hurled Lance: a spectral lance thrown hard and straight (12, devoured).</li>
 * </ul>
 */
public class GluttonyLord extends SinBoss {
    public static final int LUNGE_TICKS=8,GORGE_TICKS=40;public static final float LUNGE=14,BITE=16,LANCE=12;
    private int lunge,gorge;private Vec3 lungeDirection=Vec3.ZERO;private final Set<UUID> pierced=new HashSet<>();

    public GluttonyLord(EntityType<? extends GluttonyLord> type,Level level) {
        super(type,level);
        skills.add(new ArcaneSkill(110,12,3,14,true,(mob,target)->target.position(),
            (level1,mob,target,mark,left)->{getNavigation().stop();getLookControl().setLookAt(target);
                if(left%2==0)SinParticles.line(level1,SinParticles.BUBBLE.get(),colour(),position().add(0,1.5,0),position().add(mark.subtract(position()).multiply(1,0,1).normalize().scale(10)).add(0,1.5,0),0.8,0);},
            (level1,mob,target,mark)->{
                act(1,LUNGE_TICKS+4);lunge=LUNGE_TICKS;pierced.clear();
                lungeDirection=mark.subtract(position()).multiply(1,0,1).normalize();
                sound(SoundEvents.TRIDENT_RIPTIDE_2,2F,0.6F);
            }));
        skills.add(new ArcaneSkill(260,10,0,14,false,(mob,target)->position(),
            (level1,mob,target,mark,left)->getNavigation().stop(),
            (level1,mob,target,mark)->{act(2,GORGE_TICKS+10);gorge=GORGE_TICKS;sound(SoundEvents.RAVAGER_ROAR,2F,0.4F);}));
        skills.add(new ArcaneSkill(150,16,8,32,true,(mob,target)->target.position(),
            (level1,mob,target,mark,left)->{getLookControl().setLookAt(target);SinParticles.send(level1,SinParticles.BUBBLE.get(),colour(),position().add(0,2.6,0),2,0.4,0,0.02,0);},
            (level1,mob,target,mark)->{
                act(3,16);
                var from=position().add(0,2.4,0);
                TreasureWeapon.loose(level1,this,new ItemStack(Items.TRIDENT),from,target.position().add(0,target.getBbHeight()*0.5,0),2.0,LANCE*(enraged()?1.2F:1),colour()).devouring();
                sound(SoundEvents.TRIDENT_THROW,2F,0.6F);
            }));
    }
    @Override protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(3,new MeleeAttackGoal(this,1.1,true));
    }
    @Override public BossAffix sin() {return BossAffix.GLUTTONY;}
    @Override public String bossId() {return "gluttony_lord";}
    @Override protected BarLook barLook() {return new BarLook(gorge>0?2:0,0,-1,0);}
    @Override protected BossEvent.BossBarColor barColour() {return BossEvent.BossBarColor.PURPLE;}
    @Override protected String[] actions() {return new String[]{"","lunge","gorge","throw"};}
    @Override protected SoundEvent wakeSound() {return SoundEvents.RAVAGER_ROAR;}

    static void devour(LivingEntity victim,LivingEntity by,int ticks) {
        victim.addEffect(new MobEffectInstance(SinBosses.DEVOURED.get(),ticks,0),by);
        victim.addEffect(new MobEffectInstance(MobEffects.HUNGER,ticks,1),by);
    }
    @Override public boolean doHurtTarget(Entity target) {
        float before=target instanceof LivingEntity living?living.getHealth():0;
        boolean hit=super.doHurtTarget(target);
        if(hit&&target instanceof LivingEntity living){
            devour(living,this,100);
            heal(Math.max(0,before-living.getHealth())*0.25F);
        }
        return hit;
    }
    @Override protected void fight(ServerLevel level) {
        if(lunge>0){
            lunge--;getNavigation().stop();
            setDeltaMovement(lungeDirection.x*1.15,getDeltaMovement().y,lungeDirection.z*1.15);
            setYRot((float)Math.toDegrees(Math.atan2(lungeDirection.z,lungeDirection.x))-90);yBodyRot=getYRot();
            var tip=position().add(lungeDirection.scale(2)).add(0,1.4,0);
            for(var e:foes(tip.subtract(0,1.4,0),1.8))if(pierced.add(e.getUUID())){strike(e,LUNGE);devour(e,this,160);ArcaneSkill.knock(e,position(),0.8,0.3);}
            SinParticles.send(level,SinParticles.BUBBLE.get(),colour(),tip,2,0.3,0,0,0);
            if(horizontalCollision)lunge=0;
        }
        if(gorge>0){
            gorge--;getNavigation().stop();
            var maw=position().add(0,1.6,0);
            for(var e:foes(position(),14)){
                var pull=maw.subtract(e.position()).multiply(1,0,1);
                if(pull.length()>1.6){pull=pull.normalize().scale(0.28);e.setDeltaMovement(pull.x,e.getDeltaMovement().y,pull.z);e.hurtMarked=true;}
            }
            for(int i=0;i<3;i++){
                double a=getRandom().nextDouble()*Math.PI*2,r=6+getRandom().nextDouble()*8;
                var from=position().add(Math.cos(a)*r,0.5+getRandom().nextDouble()*2,Math.sin(a)*r);
                var to=maw.subtract(from).scale(0.08);
                level.sendParticles(SinParticles.BUBBLE.get().of(colour()),from.x,from.y,from.z,0,to.x,to.y,to.z,1);
            }
            if(gorge==0){
                float bitten=0;
                for(var e:foes(position(),3.5)){float before=e.getHealth();if(strike(e,BITE))bitten+=Math.max(0,before-e.getHealth());devour(e,this,100);}
                heal(bitten*2);
                SinParticles.send(level,SinParticles.BUBBLE.get(),colour(),maw,30,1,0,0.1,0);
                sound(SoundEvents.PLAYER_BURP,3F,0.4F);sound(SoundEvents.EVOKER_FANGS_ATTACK,2F,0.6F);
            }
        }
    }
    @Override protected void onRest() {lunge=0;gorge=0;}
    public boolean gorging() {return gorge>0;}
    @Override protected void ambient() {
        if(getRandom().nextInt(3)!=0)return;
        var look=getViewVector(1);
        level().addParticle(SinParticles.BUBBLE.get().of(colour()),getX()+look.x*0.9+(getRandom().nextDouble()-0.5)*0.6,getY()+1.4+getRandom().nextDouble()*0.5,getZ()+look.z*0.9+(getRandom().nextDouble()-0.5)*0.6,0,-0.01,0);
    }
}
