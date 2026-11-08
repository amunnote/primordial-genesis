package local.primordial.boss;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import local.primordial.affix.AffixAccessories;
import local.primordial.affix.AffixCombat;
import local.primordial.affix.AffixMaterials;
import local.primordial.affix.AffixState;
import local.primordial.affix.BossAffix;
import local.primordial.affix.MobAffix;
import local.primordial.alchemy.AlchemyKnowledge;
import local.primordial.entity.alchemical.ArcaneSkill;
import local.primordial.item.AlchemyRecipeItem;
import local.primordial.spirit.PrimordialTemple;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 原初之主 · 裁定者 — the Primordial Lord, the Ruler: what the seven sins were before they were seven. A silver-white,
 * gold-trimmed knight four blocks tall with a halo of seven coloured shards at its back, sleeping on the dais of the
 * Primordial Temple. It bears 【原初】 Primordial: every ordinary affix and every sin at once (see {@link AffixState}).
 * <ol>
 * <li><b>原初显现</b> (to 60%): 裁定之剑 a three-blow sword combo; 原初法阵 a circle at its feet that bursts after two
 *     seconds; 七罪借形 every twelve seconds or so it borrows one sin lord's signature, its halo lighting in that colour as
 *     warning: Pride's gates, Wrath's charge, Envy's step behind you, Greed's falling gold, Lust's butterflies, Gluttony's
 *     pull, Sloth's sleep.</li>
 * <li><b>七罪归一</b> (from 60%): it rises over the dais and raises seven Sin Pillars round the arena; while any stands it
 *     takes half harm and rains judgement on everyone below. Breaking a pillar strikes that sin off it for the rest of the
 *     fight. When all are broken, after a minute, or under a quarter of its health, the rest collapse.</li>
 * <li><b>回归原初</b>: its halo shatters; it comes down faster, and every thirty seconds 创世重演 Genesis: a great circle
 *     over the temple counts down five seconds, then everyone outside the three safe circles takes 40% of their health.</li>
 * </ol>
 * Undying and Time each give it one more life; Split casts off two faint echoes. It drops the Primordial Heart and every
 * sin's material, and leaves a statue like the seven.
 */
public class PrimordialLord extends SinBoss {
    /** above vanilla's ceiling of 1024, which {@link local.primordial.effect.AttributeCeiling} lifts */
    public static final double HEALTH=1500;
    public static final int COLOUR=AffixState.PRIMORDIAL_COLOUR,LEVEL=30;
    public static final float PHASE_TWO=0.6F,PHASE_THREE=0.25F,PILLAR_WARD=0.5F,GENESIS_SHARE=0.4F,UNDYING_HEALTH=0.15F;
    public static final int PILLAR_TICKS=1200,PILLAR_RING=20,HOVER=7,RAIN_INTERVAL=50,GENESIS_COOLDOWN=600,GENESIS_WINDUP=100,SAFE_CIRCLES=3;
    public static final double SAFE_RADIUS=3.2;
    public static final int ECHO_HEALTH=150,ECHO_LIFE=1200;
    static final EntityDataAccessor<Integer> PHASE=SynchedEntityData.defineId(PrimordialLord.class,EntityDataSerializers.INT);
    static final EntityDataAccessor<Boolean> ECHO=SynchedEntityData.defineId(PrimordialLord.class,EntityDataSerializers.BOOLEAN);
    static final EntityDataAccessor<Integer> BORROWED=SynchedEntityData.defineId(PrimordialLord.class,EntityDataSerializers.INT);
    static final UUID FRENZY=UUID.fromString("c1d2e3f4-5a6b-4c7d-8e9f-0a1b2c3d4e51");

    private final List<UUID> pillars=new ArrayList<>(),echoes=new ArrayList<>();
    private final List<TreasuryGate> gates=new ArrayList<>();
    private final List<Strike> strikes=new ArrayList<>();
    private final List<Vec3> safe=new ArrayList<>();
    private final Set<UUID> trampled=new HashSet<>();
    private int pillarTicks,pillarsAlive,rain,comboStep,comboTimer,charge,gateFire,echoLife;
    private Vec3 chargeDirection=Vec3.ZERO;
    private boolean undyingUsed;
    private UUID master;
    private final ArcaneSkill genesis;

    /** a blow marked on the ground, falling when its time is up */
    static final class Strike {
        final Vec3 at;final double radius;final float damage;final BossAffix kind;int left;
        Strike(Vec3 at,double radius,float damage,BossAffix kind,int left) {this.at=at;this.radius=radius;this.damage=damage;this.kind=kind;this.left=left;}
    }

    public PrimordialLord(EntityType<? extends PrimordialLord> type,Level level) {
        super(type,level);
        // 裁定之剑: three blows, the last throwing you up
        skills.add(new ArcaneSkill(60,8,0,4.5,false,(mob,target)->target.position(),
            (level1,mob,target,mark,left)->{getNavigation().stop();getLookControl().setLookAt(target);
                SinParticles.send(level1,SinParticles.GLINT.get(),COLOUR,front(1.2).add(0,2.4,0),2,0.4,0,0.02,0);},
            (level1,mob,target,mark)->{act(1,22);comboStep=3;comboTimer=0;}).when(m->phase()!=2&&charge==0));
        // 原初法阵: a circle at its feet, bursting after two seconds
        skills.add(new ArcaneSkill(180,40,0,9,false,(mob,target)->position(),
            (level1,mob,target,mark,left)->{getNavigation().stop();
                if(left%3==0){SinParticles.ring(level1,SinParticles.FLARE.get(),COLOUR,mark,8,36,0.02);ArcaneSkill.ring(level1,this,mark,8*(1-left/40.0)+0.5,18,0.03);}},
            (level1,mob,target,mark)->{
                act(2,20);
                for(var e:foes(mark,8)){spell(e,16);ArcaneSkill.knock(e,mark,1.2,0.5);}
                SinShockwave.spread(level1,mark,8.5F,14,COLOUR,false,0);SinShockwave.spread(level1,mark,5F,10,0xFFFFFF,false,0);
                SinParticles.send(level1,SinParticles.GOLD_DUST.get(),COLOUR,mark.add(0,0.5,0),60,4,0,0.15,0);
                sound(SoundEvents.BEACON_DEACTIVATE,2F,1.4F);sound(SoundEvents.GENERIC_EXPLODE,1.2F,1.2F);
            }).when(m->phase()!=2&&charge==0));
        // 七罪借形: one of the seven's signatures, its colour warning first
        skills.add(new ArcaneSkill(220,24,0,30,true,(mob,target)->{entityData.set(BORROWED,pickSin());return target.position();},
            (level1,mob,target,mark,left)->{
                var sin=borrowed();if(sin==null)return;
                getLookControl().setLookAt(target);
                if(left%2==0)SinParticles.send(level1,SinParticles.FLARE.get(),sin.color(),haloCentre(),3,0.9,0,0.02,0);
                if(left%4==0)SinParticles.ring(level1,SinParticles.GLINT.get(),sin.color(),mark,2.5,12,0.05);
            },
            (level1,mob,target,mark)->borrow(level1,target,mark)).when(m->phase()!=2&&!echo()&&charge==0&&pickSin()>=0));
        // 创世重演: in the last phase, every thirty seconds
        genesis=new ArcaneSkill(GENESIS_COOLDOWN,GENESIS_WINDUP,0,64,false,(mob,target)->{chooseSafe();return Vec3.atBottomCenterOf(home());},
            (level1,mob,target,mark,left)->genesisWarning(level1,mark,left),
            (level1,mob,target,mark)->genesisFall(level1,mark)).when(m->phase()==3&&!echo());
        skills.add(genesis);
    }
    @Override protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(3,new MeleeAttackGoal(this,1.05,true){
            @Override public boolean canUse() {return phase()!=2&&charge==0&&super.canUse();}
            @Override public boolean canContinueToUse() {return phase()!=2&&charge==0&&super.canContinueToUse();}
        });
    }
    @Override protected void defineSynchedData() {super.defineSynchedData();entityData.define(PHASE,1);entityData.define(ECHO,false);entityData.define(BORROWED,-1);}
    @Override public BossAffix sin() {return null;}
    @Override public int colour() {return COLOUR;}
    @Override public String bossId() {return "primordial_lord";}
    @Override protected BarLook barLook() {var sin=borrowed();return new BarLook(0,phase(),-1,sin==null?0:0xFF000000|sin.color());}
    @Override protected BossEvent.BossBarColor barColour() {return BossEvent.BossBarColor.WHITE;}
    @Override protected String[] actions() {return new String[]{"","combo","circle","borrow","ascend","judgement","genesis"};}
    @Override protected SoundEvent wakeSound() {return SoundEvents.BEACON_ACTIVATE;}
    @Override public int experience() {return echo()?0:1500;}
    @Override protected double wakeRadius() {return 24;}
    @Override protected double leash() {return 42;}
    @Override protected double forget() {return 64;}
    @Override protected boolean showsBar() {return !echo();}
    @Override protected boolean memorable() {return !echo();}
    @Override protected boolean ragesAtHalf() {return false;}
    /** its echoes bring no music of their own */
    @Override public net.minecraft.sounds.SoundEvent battleMusic() {return echo()?null:super.battleMusic();}
    /** whoever saw it fall may pass into the Court of Truth */
    @Override protected void onFelled(ServerLevel level) {super.onFelled(level);if(!echo())PrimordialTemple.recordPrimordialDefeat(level,position());}

    public int phase() {return entityData.get(PHASE);}
    public boolean echo() {return entityData.get(ECHO);}
    public BossAffix borrowed() {int i=entityData.get(BORROWED);return i<0||i>=BossAffix.values().length?null:BossAffix.values()[i];}
    public int pillarsStanding() {return pillarsAlive;}
    public List<Vec3> safeCircles() {return safe;}
    /** it does not blink about while it hovers over its pillars or charges (Space leaves it be) */
    public static boolean holdsStill(Mob mob) {return mob instanceof PrimordialLord lord&&(lord.phase()==2||lord.charge>0);}
    Vec3 front(double reach) {
        double yaw=Math.toRadians(yBodyRot);
        return position().add(-Math.sin(yaw)*reach,0,Math.cos(yaw)*reach);
    }
    Vec3 haloCentre() {
        double yaw=Math.toRadians(yBodyRot);
        return position().add(Math.sin(yaw)*0.9,getBbHeight()*0.72,-Math.cos(yaw)*0.9);
    }
    AffixState state() {return AffixState.get(this);}
    /** a sin still on it, at random; -1 if none is left */
    int pickSin() {
        var state=state();var left=new ArrayList<BossAffix>();
        for(var sin:BossAffix.values())if(state==null||!state.sinOff(sin))left.add(sin);
        return left.isEmpty()?-1:left.get(getRandom().nextInt(left.size())).ordinal();
    }

    // ------------------------------------------------------------------ the fight
    @Override protected void customServerAiStep() {
        if(!echo()&&AffixState.get(this)==null)AffixState.assignPrimordial(this,LEVEL);
        super.customServerAiStep();
    }
    @Override protected void fight(ServerLevel level) {
        if(echo()){
            if(master()==null||++echoLife>ECHO_LIFE){dissolve(level);return;}
        }else phases(level);
        if(gateFire>0&&--gateFire==0)fireGates(level);
        if(comboStep>0&&--comboTimer<=0)comboBlow(level);
        if(charge>0)charging(level);
        for(var it=strikes.iterator();it.hasNext();){var strike=it.next();if(--strike.left<=0){land(level,strike);it.remove();}else mark(level,strike);}
        if(phase()==2)hover(level);
    }
    void phases(ServerLevel level) {
        float share=getHealth()/getMaxHealth();
        if(phase()==1&&share<=PHASE_TWO){phaseTwo(level);return;}
        if(phase()==2){
            pillarsAlive=0;
            for(var id:pillars)if(level.getEntity(id) instanceof SinPillar pillar&&pillar.isAlive())pillarsAlive++;
            if(pillarsAlive==0||--pillarTicks<=0||share<=PHASE_THREE)phaseThree(level);
            else if(++rain>=RAIN_INTERVAL){rain=0;judgement(level);}
        }
    }
    /** 七罪归一: it rises, and the seven pillars come up round the arena */
    void phaseTwo(ServerLevel level) {
        entityData.set(PHASE,2);setNoGravity(true);getNavigation().stop();comboStep=0;
        act(4,PILLAR_TICKS+40);
        var seat=Vec3.atBottomCenterOf(home());var state=state();
        for(var sin:BossAffix.values()){
            if(state!=null&&state.sinOff(sin))continue;
            double a=PrimordialTemple.angle(sin);
            var at=seat.add(Math.cos(a)*PILLAR_RING,0,Math.sin(a)*PILLAR_RING);
            pillars.add(SinPillar.raise(level,this,sin,at).getUUID());
        }
        pillarsAlive=pillars.size();pillarTicks=PILLAR_TICKS;rain=0;
        announce("message.primordial.primordial_lord.phase_two");
        SinParticles.send(level,SinParticles.FLARE.get(),COLOUR,position().add(0,2,0),40,2,0,0.1,0);
        sound(SoundEvents.BEACON_ACTIVATE,3F,0.5F);sound(SoundEvents.WITHER_SPAWN,1.2F,1.6F);
    }
    /** 回归原初: the halo breaks and it comes down to finish it */
    void phaseThree(ServerLevel level) {
        for(var id:pillars)if(level.getEntity(id) instanceof SinPillar pillar)pillar.collapse();
        pillars.clear();pillarsAlive=0;
        entityData.set(PHASE,3);entityData.set(ENRAGED,true);setNoGravity(false);act(6,30);
        var speed=getAttribute(Attributes.MOVEMENT_SPEED);speed.removeModifier(FRENZY);
        speed.addTransientModifier(new AttributeModifier(FRENZY,"primordial frenzy",0.3,AttributeModifier.Operation.MULTIPLY_BASE));
        genesis.hold(200);
        announce("message.primordial.primordial_lord.phase_three");
        for(var sin:BossAffix.values())SinParticles.send(level,SinParticles.SHARD.get(),sin.color(),haloCentre(),10,0.8,0,0.1,0);
        sound(SoundEvents.GLASS_BREAK,3F,0.5F);sound(SoundEvents.ENDER_DRAGON_GROWL,1.5F,1.4F);
    }
    /** hovering over the dais while the pillars stand */
    void hover(ServerLevel level) {
        getNavigation().stop();
        var want=Vec3.atBottomCenterOf(home()).add(0,HOVER,0);
        var move=want.subtract(position()).scale(0.08);
        if(move.length()>0.4)move=move.normalize().scale(0.4);
        setDeltaMovement(move);
        var target=getTarget();
        if(target!=null){getLookControl().setLookAt(target,30,30);setYRot((float)(Mth.atan2(target.getZ()-getZ(),target.getX()-getX())*Mth.RAD_TO_DEG)-90);yBodyRot=getYRot();}
    }
    /** judgement rain: light falls on everyone fighting it, after a moment's warning */
    void judgement(ServerLevel level) {
        int n=0;
        for(var player:level.players())if(contender(player)&&player.position().distanceTo(Vec3.atBottomCenterOf(home()))<forget()&&n<4){
            strikes.add(new Strike(player.position(),2.4,10,null,30));n++;}
        if(n>0){act(5,20);sound(SoundEvents.BEACON_POWER_SELECT,1.5F,1.8F);}
    }
    public void pillarBroken(BossAffix sin) {
        var state=state();if(state!=null)state.strikeSin(sin);
        for(var player:bar.getPlayers())player.displayClientMessage(Component.translatable("message.primordial.primordial_lord.pillar",
            Component.translatable(SinBosses.lordType(sin).getDescriptionId()).withStyle(s->s.withColor(sin.color())),sin.tag()).withStyle(s->s.withColor(COLOUR)),false);
        if(level() instanceof ServerLevel level)SinParticles.send(level,SinParticles.SHARD.get(),sin.color(),haloCentre(),20,0.8,0,0.1,0);
    }
    @Override public boolean hurt(DamageSource source,float amount) {
        if(phase()==2&&pillarsAlive>0)amount*=PILLAR_WARD;
        return super.hurt(source,amount);
    }

    // ------------------------------------------------------------------ its blows
    void comboBlow(ServerLevel level) {
        comboStep--;comboTimer=7;
        boolean last=comboStep==0;
        var at=front(1.8);
        for(var e:foes(at,2.8)){strike(e,last?14:10);if(last)ArcaneSkill.knock(e,position(),0.6,0.9);else ArcaneSkill.knock(e,position(),0.4,0.1);}
        double yaw=Math.toRadians(yBodyRot)+(comboStep==1?0.6:comboStep==2?-0.6:0);
        for(int i=-4;i<=4;i++){double a=yaw+i*0.18;
            var p=position().add(-Math.sin(a)*2.4,2.0-(last?Math.abs(i)*0.25:0)+(comboStep==2?i*0.12:0),Math.cos(a)*2.4);
            level.sendParticles(SinParticles.GLINT.get().of(COLOUR),p.x,p.y,p.z,0,0,0,0,1);
            level.sendParticles(SinParticles.FLARE.get().of(0xFFFFFF),p.x,p.y,p.z,0,0,0.01,0,1);}
        if(last)SinShockwave.spread(level,at,3F,8,COLOUR,false,0);
        sound(SoundEvents.PLAYER_ATTACK_SWEEP,1.5F,last?0.6F:0.9F);
    }
    void borrow(ServerLevel level,LivingEntity target,Vec3 mark) {
        var sin=borrowed();entityData.set(BORROWED,-1);
        if(sin==null)return;
        act(3,20);
        for(var player:bar.getPlayers())player.displayClientMessage(Component.translatable("message.primordial.primordial_lord.borrow",sin.tag()).withStyle(s->s.withColor(sin.color())),true);
        switch(sin){
            case PRIDE->{
                PrideLord.closeAll(gates);
                var toward=target.position().subtract(position()).multiply(1,0,1);toward=toward.lengthSqr()<1e-4?new Vec3(0,0,1):toward.normalize();
                var side=new Vec3(-toward.z,0,toward.x);
                for(int i=0;i<5;i++){double t=(i/4.0-0.5)*2;
                    var at=position().add(side.scale(t*4)).add(toward.scale(-1.5+Math.abs(t))).add(0,3+Math.cos(t*Math.PI*0.5),0);
                    gates.add(TreasuryGate.open(level,at,PrideLord.aimPoint(at,target),TreasureWeapon.randomTreasure(getRandom()),18,1F));}
                gateFire=18;sound(SoundEvents.BEACON_POWER_SELECT,1.6F,1.5F);
            }
            case WRATH->{
                chargeDirection=target.position().subtract(position()).multiply(1,0,1).normalize();charge=14;trampled.clear();
                sound(SoundEvents.RAVAGER_ROAR,2F,1F);
            }
            case ENVY->{
                var look=target.getLookAngle().multiply(1,0,1);look=look.lengthSqr()<1e-4?new Vec3(0,0,1):look.normalize();
                var behind=target.position().subtract(look.scale(1.8));
                SinParticles.send(level,SinParticles.WISP.get(),sin.color(),position().add(0,2,0),20,0.8,0,0.05,0);
                teleportTo(behind.x,target.getY(),behind.z);getLookControl().setLookAt(target);
                SinParticles.send(level,SinParticles.WISP.get(),sin.color(),behind.add(0,2,0),20,0.8,0,0.05,0);
                strike(target,12);target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,40),this);
                sound(SoundEvents.ENDERMAN_TELEPORT,1.5F,0.6F);
            }
            case GREED->{
                for(int i=0;i<5;i++){var at=i==0?target.position():target.position().add((getRandom().nextDouble()-0.5)*7,0,(getRandom().nextDouble()-0.5)*7);
                    strikes.add(new Strike(at,2,8,BossAffix.GREED,22+i*3));}
                sound(SoundEvents.CHAIN_PLACE,1.5F,1.6F);
            }
            case LUST->{
                for(int i=0;i<4;i++){double a=Math.PI*2*i/4;
                    SinOrb.loose(level,this,target,position().add(0,2.6,0),new Vec3(Math.cos(a)*0.35,0.25,Math.sin(a)*0.35),sin.color());}
                sound(SoundEvents.ALLAY_THROW,1.5F,0.8F);
            }
            case GLUTTONY->{
                for(var e:foes(position(),12)){var pull=position().subtract(e.position()).normalize().scale(1.1);
                    e.setDeltaMovement(e.getDeltaMovement().add(pull.x,0.25,pull.z));e.hurtMarked=true;
                    spell(e,6);e.addEffect(new MobEffectInstance(SinBosses.DEVOURED.get(),100),this);}
                SinParticles.send(level,SinParticles.BUBBLE.get(),sin.color(),position().add(0,1.5,0),40,4,0,0.02,0);
                sound(SoundEvents.GENERIC_EAT,2F,0.4F);
            }
            case SLOTH->{strikes.add(new Strike(target.position(),5,0,BossAffix.SLOTH,20));sound(SoundEvents.BELL_RESONATE,1.5F,0.6F);}
        }
    }
    void fireGates(ServerLevel level) {
        var target=getTarget();
        for(var gate:gates){
            if(gate.isRemoved())continue;
            if(target!=null){
                var stack=gate.weapon().isEmpty()?TreasureWeapon.randomTreasure(getRandom()):gate.weapon();
                var from=gate.position().add(gate.facing().scale(0.4));
                TreasureWeapon.loose(level,this,stack,from,PrideLord.aimPoint(from,target),1.8,Math.min(12F,TreasureWeapon.damageOf(stack)),BossAffix.PRIDE.color());
            }
            gate.fire();
        }
        gates.clear();
        sound(SoundEvents.ILLUSIONER_CAST_SPELL,2F,1.4F);
    }
    void charging(ServerLevel level) {
        charge--;getNavigation().stop();
        setDeltaMovement(chargeDirection.x*1.2,getDeltaMovement().y,chargeDirection.z*1.2);
        setYRot((float)Math.toDegrees(Math.atan2(chargeDirection.z,chargeDirection.x))-90);yBodyRot=getYRot();
        for(var e:foes(position(),1.8))if(trampled.add(e.getUUID())){strike(e,12);ArcaneSkill.knock(e,position(),1.8,0.5);}
        SinParticles.send(level,SinParticles.EMBER.get(),BossAffix.WRATH.color(),position().add(0,0.4,0),3,0.6,0,0.05,0);
        if(horizontalCollision)charge=0;
    }
    /** a marked blow's warning, each tick until it falls */
    void mark(ServerLevel level,Strike strike) {
        if(strike.left%3!=0)return;
        int colour=strike.kind==null?COLOUR:strike.kind.color();
        SinParticles.ring(level,SinParticles.GLINT.get(),colour,strike.at,strike.radius,(int)(strike.radius*8),0.02);
        if(strike.kind==null)SinParticles.lineFar(level,SinParticles.FLARE.get(),colour,strike.at.add(0,14,0),strike.at.add(0,14-14*(1-strike.left/30.0),0),1.5,0);
        if(strike.kind==BossAffix.GREED)SinParticles.send(level,SinParticles.COIN.get(),colour,strike.at.add(0,6,0),2,strike.radius*0.5,0,-0.3,0);
        if(strike.kind==BossAffix.SLOTH)SinParticles.send(level,SinParticles.DREAM.get(),colour,strike.at.add(0,0.5,0),3,strike.radius*0.6,0,0.03,0);
    }
    void land(ServerLevel level,Strike strike) {
        var near=foes(strike.at,strike.radius);
        if(strike.kind==BossAffix.SLOTH){
            for(var e:near){e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,80,3),this);e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,80,1),this);
                e.addEffect(new MobEffectInstance(MobEffects.CONFUSION,80,0),this);}
            SinParticles.send(level,SinParticles.DREAM.get(),strike.kind.color(),strike.at.add(0,1,0),30,strike.radius*0.6,0,0.05,0);
            return;
        }
        for(var e:near){spell(e,strike.damage);if(strike.kind==BossAffix.GREED)e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,60,1),this);}
        int colour=strike.kind==null?COLOUR:strike.kind.color();
        SinShockwave.spread(level,strike.at,(float)strike.radius+0.5F,8,colour,false,0);
        SinParticles.send(level,strike.kind==BossAffix.GREED?SinParticles.COIN.get():SinParticles.FLARE.get(),colour,strike.at.add(0,0.6,0),16,strike.radius*0.5,0,0.12,0);
        level.playSound(null,strike.at.x,strike.at.y,strike.at.z,strike.kind==null?SoundEvents.BEACON_DEACTIVATE:SoundEvents.CHAIN_BREAK,net.minecraft.sounds.SoundSource.HOSTILE,1F,1.6F);
    }

    // ------------------------------------------------------------------ genesis
    void chooseSafe() {
        safe.clear();
        var seat=Vec3.atBottomCenterOf(home());
        for(int tries=0;tries<60&&safe.size()<SAFE_CIRCLES;tries++){
            double a=getRandom().nextDouble()*Math.PI*2,r=6+getRandom().nextDouble()*(PrimordialTemple.ARENA-12);
            var at=seat.add(Math.cos(a)*r,0,Math.sin(a)*r);
            if(safe.stream().allMatch(o->o.distanceTo(at)>9))safe.add(at);
        }
        act(6,GENESIS_WINDUP+20);
    }
    void genesisWarning(ServerLevel level,Vec3 seat,int left) {
        getNavigation().stop();
        if(left%2==0){
            SinParticles.ringFar(level,SinParticles.FLARE.get(),COLOUR,seat.add(0,22,0),PrimordialTemple.ARENA-4,64,-0.02);
            SinParticles.ringFar(level,SinParticles.GLINT.get(),0xFFFFFF,seat.add(0,22,0),(PrimordialTemple.ARENA-4)*(1-left/(double)GENESIS_WINDUP),40,-0.05);
            for(var at:safe){SinParticles.ringFar(level,SinParticles.FLARE.get(),0x9AFFC8,at,SAFE_RADIUS,20,0.06);
                SinParticles.send(level,SinParticles.GLINT.get(),0x9AFFC8,at.add(0,1.5,0),2,SAFE_RADIUS*0.5,0,0.08,0);}
        }
        if(left%20==0)for(var player:bar.getPlayers())player.displayClientMessage(Component.translatable("message.primordial.primordial_lord.genesis",left/20)
            .withStyle(s->s.withColor(COLOUR).withBold(true)),true);
    }
    void genesisFall(ServerLevel level,Vec3 seat) {
        act(6,30);
        for(var player:level.players()){
            if(!contender(player)||player.position().distanceTo(seat)>forget())continue;
            boolean sheltered=safe.stream().anyMatch(at->ArcaneSkill.horizontal(at,player.position())<=SAFE_RADIUS&&Math.abs(at.y-player.getY())<4);
            if(sheltered)continue;
            // exactly its share: the affixes neither swell nor soften it
            AffixCombat.secondary(()->player.hurt(ArcaneSkill.magic(this),player.getMaxHealth()*GENESIS_SHARE));
        }
        SinShockwave.spread(level,seat,PrimordialTemple.ARENA,24,COLOUR,false,0);
        SinShockwave.spread(level,seat.add(0,0.2,0),PrimordialTemple.ARENA*0.6F,18,0xFFFFFF,false,0);
        level.sendParticles(ParticleTypes.END_ROD,seat.x,seat.y+12,seat.z,200,PrimordialTemple.ARENA*0.4,6,PrimordialTemple.ARENA*0.4,0.1);
        sound(SoundEvents.GENERIC_EXPLODE,3F,0.5F);sound(SoundEvents.BEACON_DEACTIVATE,3F,0.5F);
        safe.clear();
    }

    // ------------------------------------------------------------------ echoes (its Split)
    /** two faint echoes of itself, weak and short-lived, fighting beside it */
    public int splitEchoes() {
        if(echo()||!(level() instanceof ServerLevel level))return 0;
        int made=0;
        for(int i=0;i<2;i++){
            var echo=SinBosses.PRIMORDIAL.get().create(level);if(echo==null)continue;
            double a=getRandom().nextDouble()*Math.PI*2;
            echo.moveTo(getX()+Math.cos(a)*3,getY(),getZ()+Math.sin(a)*3,getYRot(),0);
            echo.becomeEcho(this);
            if(level.addFreshEntity(echo)){echoes.add(echo.getUUID());made++;
                SinParticles.send(level,SinParticles.FLARE.get(),COLOUR,echo.position().add(0,2,0),20,0.8,0,0.05,0);}
        }
        if(made>0)announce("message.primordial.primordial_lord.split");
        return made;
    }
    void becomeEcho(PrimordialLord of) {
        entityData.set(ECHO,true);entityData.set(DORMANT,false);master=of.getUUID();setHome(of.home());
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(ECHO_HEALTH);setHealth(ECHO_HEALTH);
        setTarget(of.getTarget());
    }
    PrimordialLord master() {
        return master!=null&&level() instanceof ServerLevel level&&level.getEntity(master) instanceof PrimordialLord lord&&lord.isAlive()?lord:null;
    }
    void dissolve(ServerLevel level) {
        SinParticles.send(level,SinParticles.FLARE.get(),COLOUR,position().add(0,2,0),24,0.8,0,0.06,0);
        discard();
    }

    // ------------------------------------------------------------------ death, spoils, rest
    @Override public void die(DamageSource source) {
        // 【永生】: once, unless its slayer wears Undying's accessory
        if(!level().isClientSide&&!echo()&&!undyingUsed&&!source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
            &&!(source.getEntity() instanceof Player killer&&AffixAccessories.wears(killer,MobAffix.UNDYING))){
            undyingUsed=true;setHealth(getMaxHealth()*UNDYING_HEALTH);
            level().broadcastEntityEvent(this,(byte)35);
            announce("message.primordial.primordial_lord.undying");
            return;
        }
        super.die(source);
        if(isDeadOrDying()&&level() instanceof ServerLevel level)clear(level);
    }
    @Override protected void dropCustomDeathLoot(DamageSource source,int looting,boolean hit) {
        super.dropCustomDeathLoot(source,looting,hit);
        if(echo()||!(level() instanceof ServerLevel level))return;
        spawnAtLocation(new ItemStack(SinBosses.PRIMORDIAL_HEART.get()));
        spawnAtLocation(new ItemStack(SinBosses.DIVINITY_SHARD.get(),2));
        // the Ascension Charm's recipe, always
        {var item=new ItemEntity(level,getX(),getY()+1,getZ(),AlchemyRecipeItem.create(SinBosses.ASCENSION_RECIPE));item.setDefaultPickUpDelay();level.addFreshEntity(item);}
        for(var sin:BossAffix.values())spawnAtLocation(new ItemStack(AffixMaterials.BOSS_ITEMS.get(sin).get()));
        for(int i=0;i<2;i++){
            var recipe=local.primordial.spirit.SpiritRealm.realmRecipe(level.getServer(),getRandom());
            if(recipe!=null){var item=new ItemEntity(level,getX(),getY()+1,getZ(),AlchemyRecipeItem.create(recipe));item.setDefaultPickUpDelay();level.addFreshEntity(item);}
        }
        // those who saw it fall understand how its sigil is made
        for(var player:level.players())if(contender(player)&&player.distanceTo(this)<PrimordialTemple.WITNESS)AlchemyKnowledge.epiphany(player,SinBosses.PRIMORDIAL_SIGIL_RECIPE);
    }
    /** pillars, echoes and gates go with the fight */
    void clear(ServerLevel level) {
        for(var id:pillars)if(level.getEntity(id) instanceof SinPillar pillar)pillar.collapse();
        for(var id:echoes)if(level.getEntity(id) instanceof PrimordialLord echo)echo.dissolve(level);
        pillars.clear();echoes.clear();strikes.clear();safe.clear();PrideLord.closeAll(gates);
    }
    @Override protected void onRest() {
        if(echo()){discard();return;}
        if(level() instanceof ServerLevel level)clear(level);
        entityData.set(PHASE,1);entityData.set(BORROWED,-1);setNoGravity(false);
        getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(FRENZY);
        undyingUsed=false;charge=0;comboStep=0;gateFire=0;pillarsAlive=0;
        var state=state();if(state!=null){state.restoreSins();state.reset();}
    }
    void announce(String key) {
        for(var player:bar.getPlayers())player.displayClientMessage(Component.translatable(key).withStyle(s->s.withColor(COLOUR).withBold(true)),false);
    }

    @Override protected void ambient() {
        var random=getRandom();
        if(echo()){if(random.nextInt(3)==0)level().addParticle(SinParticles.FLARE.get().of(COLOUR),getX()+(random.nextDouble()-0.5),getY()+random.nextDouble()*3.5,getZ()+(random.nextDouble()-0.5),0,0.02,0);return;}
        if(dormant()&&random.nextInt(3)!=0)return;
        if(random.nextInt(2)==0){
            double a=random.nextDouble()*Math.PI*2,r=0.7+random.nextDouble()*0.9;
            level().addParticle(SinParticles.GOLD_DUST.get().of(COLOUR),getX()+Math.cos(a)*r,getY()+0.5+random.nextDouble()*3.4,getZ()+Math.sin(a)*r,0,0.015,0);
        }
        // the halo's seven lights, or once it is broken, white motes rising off it
        var halo=haloCentre();
        if(phase()!=3&&random.nextInt(2)==0){
            var sin=BossAffix.values()[random.nextInt(BossAffix.values().length)];
            double yaw=Math.toRadians(yBodyRot),a=Math.PI*2*sin.ordinal()/7.0+tickCount*0.02;
            double side=Math.cos(a)*1.3,up=Math.sin(a)*1.3;
            level().addParticle(SinParticles.GLINT.get().of(sin.color()),halo.x+Math.cos(yaw)*side,halo.y+up,halo.z+Math.sin(yaw)*side,0,0,0);
        }
        if(phase()==3&&random.nextInt(2)==0)
            level().addParticle(SinParticles.FLARE.get().of(0xFFFFFF),getX()+(random.nextDouble()-0.5)*1.6,getY()+1+random.nextDouble()*3,getZ()+(random.nextDouble()-0.5)*1.6,0,0.05,0);
    }
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Echo",echo());if(master!=null)tag.putUUID("Master",master);
        tag.putBoolean("UndyingUsed",undyingUsed);
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(ECHO,tag.getBoolean("Echo"));master=tag.hasUUID("Master")?tag.getUUID("Master"):null;
        undyingUsed=tag.getBoolean("UndyingUsed");
        // a fight broken off by a restart starts again from the beginning
        entityData.set(PHASE,1);setNoGravity(false);
    }
}
