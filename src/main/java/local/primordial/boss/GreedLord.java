package local.primordial.boss;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 贪婪之主 · 黄金骑王 — Greed, the Rider. A crowned rider grown into a golden beast, hoarding all it can take.
 * <ul>
 * <li>掠夺 Plunder: every hit it lands takes up to three of your experience levels (five once enraged) into its hoard and
 *     heals it; when it falls, the whole hoard spills out again.</li>
 * <li>黄金冲锋 Golden Charge: three charges in a row, each turning after you, trampling and plundering.</li>
 * <li>金雨 Gold Rain: five circles of falling coins mark the ground round you, then the gold comes down (8, slowed).</li>
 * </ul>
 */
public class GreedLord extends SinBoss {
    public static final int PLUNDER=3,ENRAGED_PLUNDER=5,DASHES=3,DASH_TICKS=12;
    private int hoard,dashes,dash;private Vec3 dashDirection=Vec3.ZERO;private LivingEntity rider;
    private final List<Vec3> coins=new ArrayList<>();private final Set<UUID> trampled=new HashSet<>();

    public GreedLord(EntityType<? extends GreedLord> type,Level level) {
        super(type,level);
        skills.add(new ArcaneSkill(130,14,5,24,true,(mob,target)->target.position(),
            (level1,mob,target,mark,left)->{getNavigation().stop();getLookControl().setLookAt(target);
                if(left%3==0)SinParticles.send(level1,SinParticles.COIN.get(),colour(),position().add(0,0.3,0),4,0.8,0,0.2,0);},
            (level1,mob,target,mark)->{rider=target;dashes=DASHES;startDash();}));
        skills.add(new ArcaneSkill(200,30,0,24,false,(mob,target)->{
                coins.clear();coins.add(target.position());
                for(int i=0;i<4;i++){double a=getRandom().nextDouble()*Math.PI*2,r=2+getRandom().nextDouble()*3;coins.add(target.position().add(Math.cos(a)*r,0,Math.sin(a)*r));}
                return target.position();
            },
            (level1,mob,target,mark,left)->{
                for(var spot:coins){
                    if(left%3==0)SinParticles.ring(level1,SinParticles.COIN.get(),colour(),spot,2,10,0.02);
                    if(left%2==0)SinParticles.send(level1,SinParticles.COIN.get(),colour(),spot.add(0,8,0),1,1.5,0,-0.1,0);
                }
            },
            (level1,mob,target,mark)->{
                act(2,20);
                for(var spot:coins){
                    for(var e:foes(spot,2)){spell(e,8);e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,60,1),this);}
                    SinParticles.send(level1,SinParticles.COIN.get(),colour(),spot.add(0,1.5,0),18,1,0,-0.2,0);
                }
                sound(SoundEvents.CHAIN_BREAK,2F,0.6F);sound(SoundEvents.AMETHYST_BLOCK_CHIME,2F,0.5F);
            }));
    }
    @Override protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(3,new MeleeAttackGoal(this,1.25,true));
    }
    @Override public BossAffix sin() {return BossAffix.GREED;}
    @Override public String bossId() {return "greed_lord";}
    @Override protected BarLook barLook() {return new BarLook(0,hoard,-1,0);}
    @Override protected BossEvent.BossBarColor barColour() {return BossEvent.BossBarColor.YELLOW;}
    @Override protected String[] actions() {return new String[]{"","charge","rain"};}
    @Override protected SoundEvent wakeSound() {return SoundEvents.HORSE_ANGRY;}
    public int hoard() {return hoard;}

    /** experience points between two levels */
    static int pointsBetween(int from,int to) {
        int points=0;
        for(int level=from;level<to;level++)points+=level>=30?112+(level-30)*9:level>=15?37+(level-15)*5:7+level*2;
        return points;
    }
    /** take experience levels from a player into its hoard, and heal on them */
    public int plunder(LivingEntity victim) {
        if(!(victim instanceof Player player)||player.isCreative())return 0;
        int levels=Math.min(enraged()?ENRAGED_PLUNDER:PLUNDER,player.experienceLevel);
        if(levels<=0)return 0;
        hoard+=pointsBetween(player.experienceLevel-levels,player.experienceLevel);
        player.giveExperienceLevels(-levels);
        heal(levels*4);
        if(level() instanceof ServerLevel level)SinParticles.line(level,SinParticles.COIN.get(),colour(),player.position().add(0,1,0),position().add(0,1.5,0),0.5,0.05);
        player.displayClientMessage(Component.translatable("message.primordial.greed_lord.plunder",levels).withStyle(s->s.withColor(colour())),true);
        sound(SoundEvents.EXPERIENCE_ORB_PICKUP,1.5F,0.5F);
        return levels;
    }
    @Override public boolean doHurtTarget(Entity target) {
        boolean hit=super.doHurtTarget(target);
        if(hit&&target instanceof LivingEntity living)plunder(living);
        return hit;
    }
    private void startDash() {
        var target=rider;
        if(target==null||!target.isAlive()){dashes=0;return;}
        act(1,DASH_TICKS);
        dashDirection=target.position().subtract(position()).multiply(1,0,1).normalize();dash=DASH_TICKS;trampled.clear();
        sound(SoundEvents.HORSE_GALLOP,2F,0.7F);
    }
    @Override protected void fight(ServerLevel level) {
        if(dash<=0)return;
        dash--;getNavigation().stop();
        setDeltaMovement(dashDirection.x*1.15,getDeltaMovement().y,dashDirection.z*1.15);
        setYRot((float)Math.toDegrees(Math.atan2(dashDirection.z,dashDirection.x))-90);yBodyRot=getYRot();
        for(var e:foes(position(),1.4))if(trampled.add(e.getUUID())){strike(e,10);plunder(e);ArcaneSkill.knock(e,position(),1.2,0.4);}
        SinParticles.send(level,SinParticles.COIN.get(),colour(),position().add(0,0.4,0),2,0.6,0,0.1,0);
        if(dash==0||horizontalCollision){dash=0;if(--dashes>0)startDash();}
    }
    @Override public void die(net.minecraft.world.damagesource.DamageSource source) {
        super.die(source);
        if(level() instanceof ServerLevel level&&hoard>0){
            ExperienceOrb.award(level,position().add(0,1,0),hoard);hoard=0;
            SinParticles.send(level,SinParticles.COIN.get(),colour(),position().add(0,2,0),60,1.5,0,0.3,0);
        }
    }
    @Override protected void onEnrage(ServerLevel level) {getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).setBaseValue(0.44);}
    @Override protected void onRest() {dash=0;dashes=0;getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).setBaseValue(0.38);}
    @Override protected void ambient() {
        if(getRandom().nextInt(4)!=0)return;
        level().addParticle(SinParticles.COIN.get().of(colour()),getX()+(getRandom().nextDouble()-0.5)*1.6,getY()+1+getRandom().nextDouble()*1.5,getZ()+(getRandom().nextDouble()-0.5)*1.6,0,0.1,0);
    }
    @Override public void addAdditionalSaveData(CompoundTag tag) {super.addAdditionalSaveData(tag);tag.putInt("Hoard",hoard);}
    @Override public void readAdditionalSaveData(CompoundTag tag) {super.readAdditionalSaveData(tag);hoard=tag.getInt("Hoard");}
}
