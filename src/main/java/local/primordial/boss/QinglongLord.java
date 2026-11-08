package local.primordial.boss;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import local.primordial.affix.BossAffix;
import local.primordial.entity.alchemical.ArcaneSkill;
import local.primordial.item.AlchemyRecipeItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 青龙 · 孟章 — the Azure Dragon of the East, guardian of wood and thunder: a long serpentine dragon, azure-scaled and
 * gold-bellied, antlered and whiskered, a jade mane down its back and a pearl of light at its claw. It does not stand and
 * fight: it rides the air round its terrace, out of reach, and strikes with thunder, until it tires and comes down.
 * <ul>
 * <li>御风 On the Wind: while it flies it takes 30% less; every twenty seconds it must come down for six, 力竭 spent,
 *     and then takes half again as much. A dive ends with it on the ground too, for three.</li>
 * <li>雷霆龙息 Thunder Breath: a beam of lightning from its jaws that it drags after its prey for two seconds.</li>
 * <li>惊雷 Thunderstorm: rings on the ground, then bolts out of the sky (ten once enraged).</li>
 * <li>青木缠绕 Binding Wood: vines rise and hold their prey still, wringing it, for three seconds.</li>
 * <li>龙卷俯冲 Dive: it folds and falls on a marked spot, 18 and thrown up, and lies spent a moment after.</li>
 * <li>春雷 Spring Thunder (enraged): it heals while it flies, and flies faster.</li>
 * </ul>
 */
public class QinglongLord extends SinBoss {
    /** how much bigger it is drawn than its model, and how far that reaches past its hitbox (for culling) */
    public static final float MODEL_SCALE=2.2F;
    @Override public double modelReach() {return 17;}
    public static final double HEALTH=2000;
    public static final int COLOUR=0x3AD8A0,THUNDER=0xBFF0FF,WOOD=0x6ADA5A;
    public static final float FLYING_SHARE=0.7F,SPENT_SHARE=1.5F;
    public static final int FLIGHT=400,SPENT=120,DIVE_SPENT=60,BIND=60,BREATH=40;
    static final EntityDataAccessor<Boolean> GROUNDED=SynchedEntityData.defineId(QinglongLord.class,EntityDataSerializers.BOOLEAN);
    private int flight,spent,breath,dive;private double orbit;
    private Vec3 breathAt=Vec3.ZERO,diveTo=Vec3.ZERO;private LivingEntity breathTarget;
    private final List<Vec3> bolts=new ArrayList<>();
    private final Map<UUID,Integer> bound=new HashMap<>();

    /**
     * Its head and its body toward the tail as pieces of their own (QinglongPart), so the whole long length can be struck:
     * {how far ahead of its middle, how wide, how tall} in model pixels, grown with the model. Its middle is its own hitbox.
     */
    private static final float[][] PIECES={{48,26,22},{-30,20,18},{-45,20,18},{-60,19,17},{-75,18,16},{-90,16,15},{-105,13,13},{-118,11,12}};
    /** the height of its spine above its feet, in model pixels */
    private static final float SPINE=24;
    private final QinglongPart[] parts=new QinglongPart[PIECES.length];

    public QinglongLord(EntityType<? extends QinglongLord> type,Level level) {
        super(type,level);
        for(int i=0;i<PIECES.length;i++){var p=PIECES[i];float k=MODEL_SCALE/16;parts[i]=new QinglongPart(this,p[0]*k,p[1]*k,p[2]*k);}
        // the pieces take the ids after its own (as the ender dragon's do)
        setId(ENTITY_COUNTER.getAndAdd(parts.length+1)+1);
        setNoGravity(true);
        // 雷霆龙息
        skills.add(new ArcaneSkill(160,20,0,28,true,(mob,target)->target.position(),
            (level1,mob,target,mark,left)->{if(left%2==0)SinParticles.send(level1,SinParticles.GLINT.get(),THUNDER,mouth(),4,0.4,0,0,0);},
            (level1,mob,target,mark)->{breath=BREATH;breathTarget=target;breathAt=target.position();act(1,BREATH);sound(SoundEvents.LIGHTNING_BOLT_THUNDER,1.5F,1.6F);})
            .when(m->!grounded()&&dive==0));
        // 惊雷
        skills.add(new ArcaneSkill(200,30,0,32,false,(mob,target)->{chooseBolts(target);return target.position();},
            (level1,mob,target,mark,left)->{if(left%3==0)for(var at:bolts)SinParticles.ringFar(level1,SinParticles.GLINT.get(),THUNDER,at,2.5,14,0.05);},
            (level1,mob,target,mark)->{
                act(2,20);
                for(var at:bolts){
                    var bolt=EntityType.LIGHTNING_BOLT.create(level1);
                    if(bolt!=null){bolt.setVisualOnly(true);bolt.moveTo(at);level1.addFreshEntity(bolt);}
                    for(var e:foes(at,2.5)){spell(e,12);e.setDeltaMovement(e.getDeltaMovement().add(0,0.6,0));e.hurtMarked=true;}
                }
                bolts.clear();
            }).when(m->dive==0));
        // 青木缠绕
        skills.add(new ArcaneSkill(260,16,0,24,true,(mob,target)->target.position(),
            (level1,mob,target,mark,left)->{for(int i=0;i<3;i++){double a=Math.PI*2*i/3+left*0.3;
                SinParticles.send(level1,SinParticles.WISP.get(),WOOD,target.position().add(Math.cos(a)*1.2,(16-left)*0.12,Math.sin(a)*1.2),1,0.1,0,0.02,0);}},
            (level1,mob,target,mark)->{
                act(3,16);bound.put(target.getUUID(),BIND);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,BIND,6),this);
                sound(SoundEvents.AZALEA_LEAVES_PLACE,2F,0.5F);sound(SoundEvents.VINE_STEP,2F,0.5F);
            }).when(m->dive==0));
        // 龙卷俯冲
        skills.add(new ArcaneSkill(220,18,4,30,true,(mob,target)->target.position(),
            (level1,mob,target,mark,left)->{if(left%2==0){SinParticles.ringFar(level1,SinParticles.GLINT.get(),COLOUR,mark,3.5,18,0.04);
                SinParticles.lineFar(level1,SinParticles.FLARE.get(),COLOUR,position(),mark.add(0,0.5,0),1.5,0);}},
            (level1,mob,target,mark)->{dive=14;diveTo=mark;act(4,16);sound(SoundEvents.PHANTOM_SWOOP,3F,0.5F);})
            .when(m->!grounded()&&dive==0));
    }
    @Override protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(3,new MeleeAttackGoal(this,1.0,true){
            @Override public boolean canUse() {return grounded()&&super.canUse();}
            @Override public boolean canContinueToUse() {return grounded()&&super.canContinueToUse();}
        });
    }
    @Override protected void defineSynchedData() {super.defineSynchedData();entityData.define(GROUNDED,false);}
    @Override public BossAffix sin() {return null;}
    @Override public int colour() {return COLOUR;}
    @Override public String bossId() {return "qinglong";}
    @Override protected BarLook barLook() {return grounded()?new BarLook(2,0,spent/(float)SPENT,0):new BarLook(0,0,1-flight/(float)FLIGHT,0);}
    @Override protected BossEvent.BossBarColor barColour() {return BossEvent.BossBarColor.GREEN;}
    @Override protected String[] actions() {return new String[]{"","breath","storm","bind","dive","spent"};}
    @Override protected SoundEvent wakeSound() {return SoundEvents.ENDER_DRAGON_GROWL;}
    @Override public int experience() {return 1000;}
    @Override protected double wakeRadius() {return 22;}
    @Override protected double leash() {return 40;}
    @Override protected double forget() {return 56;}

    public boolean grounded() {return entityData.get(GROUNDED);}
    public boolean binds(LivingEntity e) {return bound.containsKey(e.getUUID());}
    Vec3 mouth() {double yaw=Math.toRadians(yBodyRot),ahead=3*MODEL_SCALE;return position().add(-Math.sin(yaw)*ahead,1.4*MODEL_SCALE,Math.cos(yaw)*ahead);}
    public void land(int ticks) {
        spent=ticks;entityData.set(GROUNDED,true);setNoGravity(false);act(5,ticks);
        for(var player:bar.getPlayers())player.displayClientMessage(Component.translatable("message.primordial.qinglong.spent").withStyle(s->s.withColor(COLOUR)),true);
    }
    void chooseBolts(LivingEntity target) {
        bolts.clear();
        if(level() instanceof ServerLevel level)for(var player:level.players())if(contender(player)&&player.position().distanceTo(Vec3.atBottomCenterOf(home()))<forget())bolts.add(player.position());
        int extra=enraged()?8:4;
        for(int i=0;i<extra;i++)bolts.add(target.position().add((getRandom().nextDouble()-0.5)*14,0,(getRandom().nextDouble()-0.5)*14));
        act(2,34);
    }
    @Override public boolean hurt(DamageSource source,float amount) {
        if(!dormant())amount*=grounded()?SPENT_SHARE:FLYING_SHARE;
        return super.hurt(source,amount);
    }

    @Override protected void fight(ServerLevel level) {
        var target=getTarget();var seat=Vec3.atBottomCenterOf(home());
        // the thunder breath, dragged after its prey
        if(breath>0){
            breath--;
            if(breathTarget!=null&&breathTarget.isAlive())breathAt=breathAt.lerp(breathTarget.position().add(0,1,0),0.15);
            var from=mouth();
            if(breath%2==0)SinParticles.lineFar(level,SinParticles.GLINT.get(),THUNDER,from,breathAt,0.8,0);
            if(breath%4==0)for(var e:foes(breathAt,6))if(BaihuLord.nearLine(e.position(),from,breathAt,1.4)){spell(e,5);e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,30,1),this);}
            getLookControl().setLookAt(breathAt);
        }
        // the bound are held where they stand
        for(var it=bound.entrySet().iterator();it.hasNext();){var e=it.next();
            if(level.getEntity(e.getKey()) instanceof LivingEntity held&&held.isAlive()&&e.getValue()>0){
                e.setValue(e.getValue()-1);held.setDeltaMovement(0,Math.min(0,held.getDeltaMovement().y),0);held.hurtMarked=true;
                if(e.getValue()%20==0)held.hurt(ArcaneSkill.magic(this),3);
                if(e.getValue()%4==0)SinParticles.send(level,SinParticles.WISP.get(),WOOD,held.position().add(0,0.6,0),3,0.6,0,0.04,0);
            }else it.remove();}
        // the dive
        if(dive>0){
            dive--;var step=diveTo.subtract(position());
            move(MoverType.SELF,step.length()>2.2?step.normalize().scale(2.2):step);
            setYRot((float)(Mth.atan2(step.z,step.x)*Mth.RAD_TO_DEG)-90);yBodyRot=getYRot();
            if(dive==0||step.length()<1.5){
                dive=0;
                for(var e:foes(position(),3.5)){strike(e,18);ArcaneSkill.knock(e,position(),1.0,0.9);}
                SinShockwave.spread(level,position(),5F,12,COLOUR,false,0);
                var bolt=EntityType.LIGHTNING_BOLT.create(level);if(bolt!=null){bolt.setVisualOnly(true);bolt.moveTo(position());level.addFreshEntity(bolt);}
                land(DIVE_SPENT);
            }
            return;
        }
        // spent, on the ground
        if(spent>0){
            if(--spent==0){entityData.set(GROUNDED,false);setNoGravity(true);flight=0;sound(SoundEvents.ENDER_DRAGON_FLAP,2F,0.8F);}
            return;
        }
        // on the wind: circling round its terrace, high over its prey
        if(++flight>=FLIGHT){land(SPENT);return;}
        if(enraged()&&tickCount%20==0)heal(getMaxHealth()*0.004F);
        orbit+=enraged()?0.035:0.025;
        var centre=target!=null&&target.position().distanceTo(seat)<forget()?target.position():seat;
        var want=centre.add(Math.cos(orbit)*13,7+Math.sin(tickCount*0.05)*1.5,Math.sin(orbit)*13);
        if(want.distanceTo(seat)>ARENA_RADIUS)want=seat.add(want.subtract(seat).normalize().scale(ARENA_RADIUS)).add(0,7,0);
        var move=want.subtract(position()).scale(0.08);
        if(move.length()>(enraged()?0.7:0.5))move=move.normalize().scale(enraged()?0.7:0.5);
        setDeltaMovement(move);
        if(breath==0){
            if(target!=null){getLookControl().setLookAt(target,30,30);}
            if(move.horizontalDistanceSqr()>1e-4){setYRot((float)(Mth.atan2(move.z,move.x)*Mth.RAD_TO_DEG)-90);yBodyRot=getYRot();}
        }
    }
    @Override public void setId(int id) {super.setId(id);if(parts!=null)for(int i=0;i<parts.length;i++)parts[i].setId(id+i+1);}
    @Override public boolean isMultipartEntity() {return true;}
    @Override public net.minecraftforge.entity.PartEntity<?>[] getParts() {return parts;}
    @Override public void aiStep() {super.aiStep();placeParts();}
    /** its pieces along its body as it faces now; asleep and coiled, they draw in to its middle */
    public void placeParts() {
        double yaw=Math.toRadians(yBodyRot),dx=-Math.sin(yaw),dz=Math.cos(yaw),spine=SPINE*MODEL_SCALE/16;
        for(var part:parts){
            double ahead=dormant()?0:part.ahead;
            double x=getX()+dx*ahead,y=getY()+(dormant()?getBbHeight()/2:spine)-part.getBbHeight()/2,z=getZ()+dz*ahead;
            part.xo=part.getX();part.yo=part.getY();part.zo=part.getZ();part.xOld=part.xo;part.yOld=part.yo;part.zOld=part.zo;
            part.setPos(x,y,z);
        }
    }
    @Override public void wake() {super.wake();setNoGravity(true);entityData.set(GROUNDED,false);}
    @Override protected void onRest() {breath=0;dive=0;spent=0;flight=0;bound.clear();bolts.clear();entityData.set(GROUNDED,false);setNoGravity(false);}
    @Override protected void dropCustomDeathLoot(DamageSource source,int looting,boolean hit) {
        super.dropCustomDeathLoot(source,looting,hit);
        if(!(level() instanceof ServerLevel level))return;
        spawnAtLocation(new ItemStack(SinBosses.QINGLONG_SCALE.get(),2+Math.min(2,looting)));
        spawnAtLocation(new ItemStack(SinBosses.QINGLONG_SIGIL.get()));
        // the irregular crystal's recipe, always
        {var item=new ItemEntity(level,getX(),getY()+1,getZ(),AlchemyRecipeItem.create(SinBosses.IRREGULAR_CRYSTAL_RECIPE));item.setDefaultPickUpDelay();level.addFreshEntity(item);}
        for(int i=0;i<2;i++){var recipe=local.primordial.spirit.SpiritRealm.realmRecipe(level.getServer(),getRandom());
            if(recipe!=null){var item=new ItemEntity(level,getX(),getY()+1,getZ(),AlchemyRecipeItem.create(recipe));item.setDefaultPickUpDelay();level.addFreshEntity(item);}}
    }
    @Override protected void ambient() {
        var random=getRandom();
        if(dormant()&&random.nextInt(3)!=0)return;
        // sparks of thunder along its body, leaves on the wind behind it
        double yaw=Math.toRadians(yBodyRot);
        for(int i=0;i<2;i++){double back=random.nextDouble()*9;
            level().addParticle(SinParticles.GLINT.get().of(random.nextInt(3)==0?THUNDER:COLOUR),getX()+Math.sin(yaw)*back+(random.nextDouble()-0.5),getY()+1+random.nextDouble(),getZ()-Math.cos(yaw)*back+(random.nextDouble()-0.5),0,0.01,0);}
        if(random.nextInt(4)==0)level().addParticle(SinParticles.PETAL.get().of(WOOD),getX()+(random.nextDouble()-0.5)*4,getY()+random.nextDouble()*2,getZ()+(random.nextDouble()-0.5)*4,0,-0.02,0);
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {super.readAdditionalSaveData(tag);entityData.set(GROUNDED,false);setNoGravity(!dormant());}
}
