package local.primordial.boss;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import local.primordial.affix.AffixCombat;
import local.primordial.affix.BossAffix;
import local.primordial.entity.alchemical.ArcaneSkill;
import local.primordial.item.AlchemyRecipeItem;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 最初的贤者 — the First Sage, the last of them all: the alchemist who first sought the truth, and from whose work the
 * sins, the guardians and the origin itself were made. An old man in robes of deep violet and gold, hooded and white-
 * bearded, with a staff crowned by the Philosopher's Stone, an open grimoire floating at his hand, the four elements
 * circling him and a transmutation circle turning at his back. He waits in the Court of Truth
 * ({@link local.primordial.spirit.SageSanctum}), and fights the Great Work through its four stages.
 * <p>
 * 全知 Omniscience, always: whichever kind of harm (blade, missile or spell) has hurt it most of late, it sees through,
 * and takes 30% less of it. Change your weapons.
 * <ol>
 * <li><b>黑化 Nigredo</b> (to 75%): 分解 Solve, black circles under everyone that burst and leave pools of rot; 四象借力
 *     the Four Symbols, one guardian's art at a time (the Tiger's rakes, the Tortoise's geysers, the Dragon's thunder,
 *     the Bird's feathers); 缩地 a step to behind its prey; and a withering touch.</li>
 * <li><b>白化 Albedo</b> (to 50%): 凝结 Coagula: it rises over its dais behind a ward (a tenth of all harm) and heals,
 *     while the four elements' circles on the floor wait to be purified: a player must stand in each for five seconds.
 *     All four, and the ward shatters and it falls stunned, taking half again as much; a minute gone first, and each
 *     circle left lashes everyone for a fifth of their health. 净化 Purge strips its foes' blessings from now on.</li>
 * <li><b>黄化 Citrinitas</b> (to 25%): enraged. 黄金律 the Golden Rule: two beams of gold sweep round from it, best
 *     outrun close at hand; 等价交换 Equivalent Exchange: it takes its prey's blessings for its own and trades places.</li>
 * <li><b>红化 Rubedo</b>: the Stone wakes; it moves faster, four beams sweep, and those near it bleed life into it. Every
 *     forty seconds 终焉炼成 the Final Transmutation: one of the four circles lights, and in seven seconds everyone
 *     outside it loses half their health.</li>
 * </ol>
 * It drops the Philosopher's Stone, the Gem Sword's recipe, three Divinity Shards and its sigil, and leaves a statue.
 */
public class FirstSage extends SinBoss {
    public static final double HEALTH=4000;
    public static final int COLOUR=0xE8C25A,NIGREDO=0x7A5AAA,ALBEDO=0xF4F4FF,CITRINITAS=0xFFD84A,RUBEDO=0xE8283A;
    public static final float ALBEDO_AT=0.75F,CITRINITAS_AT=0.5F,RUBEDO_AT=0.25F;
    public static final float OMNISCIENCE=0.7F,WARDED=0.1F,STUNNED_SHARE=1.5F,BACKLASH=0.2F,OPUS_SHARE=0.5F,SEEN_THROUGH=30;
    public static final int CIRCLE_TICKS=100,COAGULA_TICKS=1200,STUN_TICKS=120,SWEEP_TICKS=120,OPUS_COOLDOWN=800,OPUS_WINDUP=140,POOL_TICKS=120,HOVER=5;
    public static final double CIRCLE_RADIUS=3,SWEEP_LENGTH=30,SWEEP_WIDTH=1.3;
    /** the four elements' circles about its seat: water north, wood east, fire south, metal west */
    public static final int[][] CIRCLES={{0,-13},{13,0},{0,13},{-13,0}};
    public static final int[] CIRCLE_COLOURS={0x3AB8D8,0x3AD8A0,0xFF5A2A,0xF4F4F0};
    public static final String[] ELEMENTS={"water","wood","fire","metal"};
    /** the kinds of harm it learns: blade, missile, spell */
    public static final String[] KINDS={"melee","projectile","magic"};
    static final EntityDataAccessor<Integer> STAGE=SynchedEntityData.defineId(FirstSage.class,EntityDataSerializers.INT);
    static final UUID HASTE=UUID.fromString("5a6e1f3c-2b7d-4e8a-9c0f-1d2e3f4a5b6c");

    private final float[] recent=new float[3];
    private final int[] progress=new int[4];
    private boolean coagula;
    private int coagulaTicks,stun,sweep,lit=-1,symbol=-1,seen=-1,light;
    private double sweepAngle,sweepDir=1;
    private final List<Strike> strikes=new ArrayList<>();
    private final List<double[]> pools=new ArrayList<>();
    private final List<Vec3> solve=new ArrayList<>();
    private final Map<UUID,Integer> beamHit=new HashMap<>();
    private final ArcaneSkill opus,sweepSkill;

    /** a blow marked on the ground, falling when its time is up: kind 0 light, 1 geyser, 2 thunder, 3 feathers */
    static final class Strike {
        final Vec3 at;final double radius;final float damage;final int kind;int left;
        Strike(Vec3 at,double radius,float damage,int kind,int left) {this.at=at;this.radius=radius;this.damage=damage;this.kind=kind;this.left=left;}
    }

    public FirstSage(EntityType<? extends FirstSage> type,Level level) {
        super(type,level);
        // 分解 Solve: black circles under everyone, bursting into pools of rot
        skills.add(new ArcaneSkill(150,30,0,40,false,(mob,target)->{chooseSolve(target);return target.position();},
            (level1,mob,target,mark,left)->{if(left%3==0)for(var at:solve){SinParticles.ringFar(level1,SinParticles.WISP.get(),stageColour(),at,2.5,16,0.02);
                SinParticles.ring(level1,SinParticles.SMOKE.get(),0x1A1020,at,2.5*(1-left/30.0)+0.3,10,0.01);}},
            (level1,mob,target,mark)->{
                act(1,20);
                for(var at:solve){
                    for(var e:foes(at,2.5)){spell(e,12);e.addEffect(new MobEffectInstance(MobEffects.WITHER,80,1),this);}
                    pools.add(new double[]{at.x,at.y,at.z,POOL_TICKS});
                    SinShockwave.spread(level1,at,3F,8,stageColour(),false,0);
                    SinParticles.send(level1,SinParticles.SMOKE.get(),0x1A1020,at.add(0,0.5,0),14,1.2,0,0.06,0);
                }
                solve.clear();sound(SoundEvents.WITHER_SHOOT,1.5F,0.6F);
            }).when(m->stun==0&&sweep==0));
        // 四象借力: one guardian's art at a time
        skills.add(new ArcaneSkill(200,24,0,30,true,(mob,target)->{symbol=getRandom().nextInt(4);act(2,30);return target.position();},
            (level1,mob,target,mark,left)->symbolWarning(level1,target,mark,left),
            (level1,mob,target,mark)->symbolRelease(level1,target,mark)).when(m->stun==0&&sweep==0&&!coagula));
        // 缩地: a step to behind its prey
        skills.add(new ArcaneSkill(180,10,8,40,true,(mob,target)->{
                var look=target.getLookAngle().multiply(1,0,1);look=look.lengthSqr()<1e-4?new Vec3(0,0,1):look.normalize();
                return target.position().subtract(look.scale(2.5));},
            (level1,mob,target,mark,left)->SinParticles.send(level1,SinParticles.WISP.get(),stageColour(),position().add(0,2,0),3,0.6,0,0.03,0),
            (level1,mob,target,mark)->{
                if(mark.distanceTo(Vec3.atBottomCenterOf(home()))>leash()-4)return;
                act(7,10);
                SinParticles.send(level1,SinParticles.WISP.get(),stageColour(),position().add(0,2,0),24,0.8,0,0.05,0);
                teleportTo(mark.x,target.getY(),mark.z);getLookControl().setLookAt(target);
                SinParticles.send(level1,SinParticles.WISP.get(),stageColour(),mark.add(0,2,0),24,0.8,0,0.05,0);
                strike(target,10);target.addEffect(new MobEffectInstance(MobEffects.WITHER,60,1),this);
                sound(SoundEvents.ENDERMAN_TELEPORT,1.5F,0.5F);
            }).when(m->stun==0&&sweep==0&&!coagula));
        // 净化 Purge: from the whitening on, its foes' blessings are stripped
        skills.add(new ArcaneSkill(400,20,0,40,false,(mob,target)->position(),
            (level1,mob,target,mark,left)->{if(left%2==0)SinParticles.ringFar(level1,SinParticles.FLARE.get(),ALBEDO,position(),12*(1-left/20.0)+1,30,0.04);},
            (level1,mob,target,mark)->{
                act(9,16);
                for(var player:level1.players())if(contender(player)&&near(player))purge(player);
                SinShockwave.spread(level1,position(),14F,16,ALBEDO,false,0);
                sound(SoundEvents.BEACON_DEACTIVATE,2F,1.6F);sound(SoundEvents.AMETHYST_BLOCK_CHIME,2F,0.6F);
            }).when(m->stage()>=2&&stun==0&&sweep==0));
        // 黄金律: beams of gold sweeping round
        sweepSkill=new ArcaneSkill(300,40,0,40,false,(mob,target)->{
                sweepAngle=Math.atan2(target.getZ()-getZ(),target.getX()-getX());sweepDir=getRandom().nextBoolean()?1:-1;act(4,40+SWEEP_TICKS);return position();},
            (level1,mob,target,mark,left)->{getNavigation().stop();if(left%3==0)drawBeams(level1,true);},
            (level1,mob,target,mark)->{sweep=SWEEP_TICKS;beamHit.clear();sound(SoundEvents.BEACON_ACTIVATE,2F,1.8F);}).when(m->stage()>=3&&stun==0&&!coagula&&sweep==0);
        skills.add(sweepSkill);
        // 等价交换: its prey's blessings for its own, and their places traded
        skills.add(new ArcaneSkill(360,30,6,30,true,(mob,target)->target.position(),
            (level1,mob,target,mark,left)->{if(left%2==0)SinParticles.lineFar(level1,SinParticles.GLINT.get(),CITRINITAS,position().add(0,2,0),target.position().add(0,1,0),0.8,0);},
            (level1,mob,target,mark)->{
                act(8,16);
                int taken=0;
                for(var effect:new ArrayList<>(target.getActiveEffects()))if(effect.getEffect().getCategory()==MobEffectCategory.BENEFICIAL){
                    addEffect(new MobEffectInstance(effect.getEffect(),Math.min(effect.getDuration(),600),effect.getAmplifier()));
                    target.removeEffect(effect.getEffect());taken++;}
                heal(getMaxHealth()*0.01F*taken);
                var mine=position();var theirs=target.position();
                teleportTo(theirs.x,theirs.y,theirs.z);target.teleportTo(mine.x,mine.y,mine.z);
                SinParticles.send(level1,SinParticles.COIN.get(),CITRINITAS,mine.add(0,1,0),20,0.8,0,0.1,0);SinParticles.send(level1,SinParticles.COIN.get(),CITRINITAS,theirs.add(0,1,0),20,0.8,0,0.1,0);
                sound(SoundEvents.ENDERMAN_TELEPORT,2F,1.4F);sound(SoundEvents.CHAIN_BREAK,1.5F,0.8F);
            }).when(m->stage()>=3&&stun==0&&sweep==0&&!coagula));
        // 终焉炼成: in the reddening, every forty seconds
        opus=new ArcaneSkill(OPUS_COOLDOWN,OPUS_WINDUP,0,64,false,(mob,target)->{lit=getRandom().nextInt(4);act(5,OPUS_WINDUP+20);return Vec3.atBottomCenterOf(home());},
            (level1,mob,target,mark,left)->opusWarning(level1,mark,left),
            (level1,mob,target,mark)->opusFall(level1,mark)).when(m->stage()==4&&stun==0&&sweep==0);
        skills.add(opus);
    }
    @Override protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(3,new MeleeAttackGoal(this,1.0,true){
            @Override public boolean canUse() {return free()&&super.canUse();}
            @Override public boolean canContinueToUse() {return free()&&super.canContinueToUse();}
        });
    }
    @Override protected void defineSynchedData() {super.defineSynchedData();entityData.define(STAGE,1);}
    @Override public BossAffix sin() {return null;}
    @Override public int colour() {return COLOUR;}
    @Override public String bossId() {return "first_sage";}
    @Override protected BarLook barLook() {return new BarLook(coagula?2:0,stage(),-1,0);}
    @Override protected BossEvent.BossBarColor barColour() {return BossEvent.BossBarColor.PURPLE;}
    @Override protected String[] actions() {return new String[]{"","solve","symbol","coagula","sweep","opus","stunned","blink","exchange","purge"};}
    @Override protected SoundEvent wakeSound() {return SoundEvents.EVOKER_PREPARE_SUMMON;}
    @Override public int experience() {return 3000;}
    @Override protected double wakeRadius() {return 24;}
    @Override protected double leash() {return 38;}
    @Override protected double forget() {return 60;}
    @Override protected boolean ragesAtHalf() {return false;}
    /** the Great Work has a track for each stage: So Below, Chrysopoeia (and again, quicker), Rubedo */
    @Override public net.minecraft.sounds.SoundEvent battleMusic() {
        return local.primordial.sound.ModSounds.music(switch(stage()){case 2->"first_sage_albedo";case 3->"first_sage_citrinitas";case 4->"first_sage_rubedo";default->"first_sage_nigredo";});
    }

    public int stage() {return entityData.get(STAGE);}
    public boolean warded() {return coagula;}
    public boolean stunned() {return stun>0;}
    public int circleProgress(int i) {return progress[i];}
    /** mark a circle purified, as five seconds stood in it would */
    public void purify(int i) {progress[i]=CIRCLE_TICKS;}
    public int litCircle() {return lit;}
    public int sweeping() {return sweep;}
    /** the kind of harm it has seen through, or -1 */
    public int seenThrough() {return seen;}
    boolean free() {return stun==0&&sweep==0&&!coagula;}
    boolean near(LivingEntity e) {return e.position().distanceTo(Vec3.atBottomCenterOf(home()))<forget();}
    int stageColour() {return switch(stage()){case 2->ALBEDO;case 3->CITRINITAS;case 4->RUBEDO;default->NIGREDO;};}
    public Vec3 circle(int i) {return Vec3.atBottomCenterOf(home()).add(CIRCLES[i][0],0,CIRCLES[i][1]);}
    /** the floor under its seat: the dais lifts it a block */
    Vec3 floor() {return Vec3.atBottomCenterOf(home()).add(0,-1,0);}
    static void purge(LivingEntity e) {
        for(var effect:new ArrayList<>(e.getActiveEffects()))if(effect.getEffect().getCategory()==MobEffectCategory.BENEFICIAL)e.removeEffect(effect.getEffect());
    }

    // ------------------------------------------------------------------ the stages
    @Override protected void fight(ServerLevel level) {
        local.primordial.achievement.Achievements.sageFight(this,level);
        for(int i=0;i<3;i++)recent[i]*=0.99F;
        float share=getHealth()/getMaxHealth();
        if(stage()==1&&share<=ALBEDO_AT)albedo(level);
        else if(stage()==2&&!coagula&&stun==0&&share<=CITRINITAS_AT)citrinitas(level);
        else if(stage()==3&&share<=RUBEDO_AT)rubedo(level);
        if(coagula)coagula(level);
        if(stun>0){stun--;getNavigation().stop();setDeltaMovement(0,getDeltaMovement().y,0);}
        if(sweep>0)sweeping(level);
        for(var it=strikes.iterator();it.hasNext();){var strike=it.next();if(--strike.left<=0){land(level,strike);it.remove();}else mark(level,strike);}
        for(var it=pools.iterator();it.hasNext();){var pool=it.next();
            if(--pool[3]<=0){it.remove();continue;}
            var at=new Vec3(pool[0],pool[1],pool[2]);
            if(tickCount%4==0)SinParticles.send(level,SinParticles.SMOKE.get(),0x1A1020,at.add(0,0.2,0),2,1.2,0,0.01,0);
            if(tickCount%10==0)for(var e:foes(at,2)){spell(e,2);e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,40,2),this);}
        }
        // the reddening: those near it bleed life into it
        if(stage()==4&&tickCount%20==0&&stun==0)for(var e:foes(position(),6)){if(spell(e,2))heal(6);
            SinParticles.lineFar(level,SinParticles.FLARE.get(),RUBEDO,e.position().add(0,1,0),position().add(0,2,0),0.6,0);}
    }
    /** 白化: it rises behind its ward, and the four circles wait */
    void albedo(ServerLevel level) {
        entityData.set(STAGE,2);bar.setColor(BossEvent.BossBarColor.WHITE);
        coagula=true;coagulaTicks=COAGULA_TICKS;java.util.Arrays.fill(progress,0);
        setNoGravity(true);getNavigation().stop();act(3,COAGULA_TICKS);light=0;
        announce("message.primordial.first_sage.albedo",ALBEDO);
        SinShockwave.spread(level,position(),16F,18,ALBEDO,false,0);
        sound(SoundEvents.BEACON_ACTIVATE,3F,0.6F);sound(SoundEvents.AMETHYST_BLOCK_RESONATE,3F,0.5F);
    }
    void coagula(ServerLevel level) {
        // hovering over its dais, healing behind the ward
        var want=Vec3.atBottomCenterOf(home()).add(0,HOVER,0);
        var move=want.subtract(position()).scale(0.08);if(move.length()>0.4)move=move.normalize().scale(0.4);
        setDeltaMovement(move);getNavigation().stop();
        var target=getTarget();if(target!=null)getLookControl().setLookAt(target,30,30);
        if(tickCount%20==0)heal(getMaxHealth()*0.003F);
        if(tickCount%4==0)SinParticles.send(level,SinParticles.GLINT.get(),ALBEDO,position().add(0,1.8,0),6,1.6,0,0,0);
        int done=0;
        for(int i=0;i<4;i++){
            var at=circle(i);
            if(progress[i]>=CIRCLE_TICKS){done++;if(tickCount%10==0)SinParticles.ringFar(level,SinParticles.GLINT.get(),0xFFFFFF,at,CIRCLE_RADIUS,16,0.08);continue;}
            boolean held=false;
            for(var player:level.players())if(contender(player)&&ArcaneSkill.horizontal(player.position(),at)<=CIRCLE_RADIUS&&Math.abs(player.getY()-at.y)<3){held=true;break;}
            if(held){progress[i]++;
                if(tickCount%3==0)SinParticles.send(level,SinParticles.FLARE.get(),CIRCLE_COLOURS[i],at.add(0,0.3,0),4,CIRCLE_RADIUS*0.6,0,0.12,0);
                if(progress[i]>=CIRCLE_TICKS){
                    for(var player:bar.getPlayers())player.displayClientMessage(Component.translatable("message.primordial.first_sage.purified",
                        element(i)).withStyle(s->s.withColor(ALBEDO)),true);
                    SinShockwave.spread(level,at,4F,10,CIRCLE_COLOURS[i],false,0);sound(SoundEvents.AMETHYST_BLOCK_BREAK,2F,1.2F);
                }
            }
            if(tickCount%5==0){SinParticles.ringFar(level,SinParticles.FLARE.get(),CIRCLE_COLOURS[i],at,CIRCLE_RADIUS,20,0.03);
                SinParticles.lineFar(level,SinParticles.GLINT.get(),CIRCLE_COLOURS[i],at.add(0,0.5,0),position().add(0,1.5,0),1.5,0);}
        }
        // light falls on everyone below
        if(++light>=60){light=0;int n=0;
            for(var player:level.players())if(contender(player)&&near(player)&&n<4){strikes.add(new Strike(player.position(),2.4,8,0,30));n++;}}
        if(coagulaTicks%20==0)for(var player:bar.getPlayers())player.displayClientMessage(Component.translatable("message.primordial.first_sage.coagula",done,coagulaTicks/20)
            .withStyle(s->s.withColor(ALBEDO)),true);
        if(done==4){breakWard(level,true);return;}
        if(--coagulaTicks<=0){
            // the circles left lash out
            int left=4-done;
            for(var player:level.players())if(contender(player)&&near(player))
                AffixCombat.secondary(()->player.hurt(ArcaneSkill.magic(this),player.getMaxHealth()*BACKLASH*left));
            for(int i=0;i<4;i++)if(progress[i]<CIRCLE_TICKS)SinShockwave.spread(level,circle(i),8F,14,CIRCLE_COLOURS[i],false,0);
            announce("message.primordial.first_sage.backlash",ALBEDO);
            sound(SoundEvents.GENERIC_EXPLODE,3F,0.7F);
            breakWard(level,false);
        }
    }
    /** the ward goes: shattered by the four circles (and it falls stunned), or let go when its time runs out */
    void breakWard(ServerLevel level,boolean shattered) {
        coagula=false;setNoGravity(false);act(0,1);
        if(shattered){
            local.primordial.achievement.Achievements.circlesPurified(this,level);
            stun=STUN_TICKS;act(6,STUN_TICKS);
            announce("message.primordial.first_sage.shattered",ALBEDO);
            SinParticles.send(level,SinParticles.SHARD.get(),ALBEDO,position().add(0,2,0),60,1.5,0,0.15,0);
            sound(SoundEvents.GLASS_BREAK,3F,0.5F);
        }
    }
    /** 黄化: enraged, and the gold begins to turn */
    void citrinitas(ServerLevel level) {
        entityData.set(STAGE,3);entityData.set(ENRAGED,true);bar.setColor(BossEvent.BossBarColor.YELLOW);
        sweepSkill.hurry(60);
        announce("message.primordial.first_sage.citrinitas",CITRINITAS);
        SinShockwave.spread(level,position(),16F,18,CITRINITAS,false,0);
        SinParticles.send(level,SinParticles.GOLD_DUST.get(),CITRINITAS,position().add(0,2,0),60,2,0,0.1,0);
        sound(SoundEvents.BEACON_POWER_SELECT,3F,0.6F);
    }
    /** 红化: the Stone wakes */
    void rubedo(ServerLevel level) {
        entityData.set(STAGE,4);bar.setColor(BossEvent.BossBarColor.RED);
        var speed=getAttribute(Attributes.MOVEMENT_SPEED);speed.removeModifier(HASTE);
        speed.addTransientModifier(new AttributeModifier(HASTE,"sage haste",0.25,AttributeModifier.Operation.MULTIPLY_BASE));
        opus.hold(160);
        announce("message.primordial.first_sage.rubedo",RUBEDO);
        SinShockwave.spread(level,position(),18F,20,RUBEDO,false,0);
        SinParticles.send(level,SinParticles.FLARE.get(),RUBEDO,position().add(0,2,0),80,2,0,0.15,0);
        sound(SoundEvents.WITHER_SPAWN,1.5F,1.4F);sound(SoundEvents.BEACON_ACTIVATE,3F,0.4F);
    }

    // ------------------------------------------------------------------ omniscience and harm
    /** blade, missile or spell */
    static int kind(DamageSource source) {
        if(source.getDirectEntity() instanceof Projectile)return 1;
        if(source.is(DamageTypes.MAGIC)||source.is(DamageTypes.INDIRECT_MAGIC)||source.getDirectEntity()==null||source.getDirectEntity()!=source.getEntity())return 2;
        return 0;
    }
    @Override public boolean hurt(DamageSource source,float amount) {
        if(!dormant()&&!level().isClientSide&&!source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)){
            int kind=kind(source);
            if(kind==seen)amount*=OMNISCIENCE;
            if(coagula)amount*=WARDED;else if(stun>0)amount*=STUNNED_SHARE;
            recent[kind]+=amount;
            int most=0;for(int i=1;i<3;i++)if(recent[i]>recent[most])most=i;
            int now=recent[most]>=SEEN_THROUGH?most:-1;
            if(now!=seen){seen=now;
                if(now>=0)for(var player:bar.getPlayers())player.displayClientMessage(Component.translatable("message.primordial.first_sage.omniscience",
                    Component.translatable("message.primordial.first_sage.kind."+KINDS[now])).withStyle(s->s.withColor(COLOUR)),true);}
        }
        return super.hurt(source,amount);
    }
    @Override public boolean doHurtTarget(Entity target) {
        boolean hit=super.doHurtTarget(target);
        if(hit&&target instanceof LivingEntity living)living.addEffect(new MobEffectInstance(MobEffects.WITHER,60,1),this);
        return hit;
    }

    // ------------------------------------------------------------------ its arts
    void chooseSolve(LivingEntity target) {
        solve.clear();
        if(level() instanceof ServerLevel level)for(var player:level.players())if(contender(player)&&near(player))solve.add(player.position());
        if(solve.isEmpty())solve.add(target.position());
        for(int i=0;i<2;i++)solve.add(target.position().add((getRandom().nextDouble()-0.5)*10,0,(getRandom().nextDouble()-0.5)*10));
        act(1,34);
    }
    static final int[] SYMBOL_COLOURS={0xF4F4F0,0x3AB8D8,0x3AD8A0,0xFF5A2A};
    void symbolWarning(ServerLevel level,LivingEntity target,Vec3 mark,int left) {
        if(symbol<0)return;
        int c=SYMBOL_COLOURS[symbol];
        getLookControl().setLookAt(target);
        if(left%2==0)SinParticles.send(level,SinParticles.FLARE.get(),c,position().add(0,2.4,0),3,0.8,0,0.04,0);
        if(left%3!=0)return;
        if(symbol==0)for(var to:rakes(target))SinParticles.lineFar(level,SinParticles.GLINT.get(),0xE83232,position().add(0,0.2,0),to.add(0,0.2,0),0.8,0);
        else SinParticles.ringFar(level,SinParticles.GLINT.get(),c,target.position(),3,14,0.05);
    }
    /** the Tiger's three rakes, fanned toward its prey */
    List<Vec3> rakes(LivingEntity target) {
        var out=new ArrayList<Vec3>();
        double base=Math.atan2(target.getZ()-getZ(),target.getX()-getX());
        for(int i=-1;i<=1;i++){double a=base+i*0.35;out.add(position().add(Math.cos(a)*14,0,Math.sin(a)*14));}
        return out;
    }
    void symbolRelease(ServerLevel level,LivingEntity target,Vec3 mark) {
        int which=symbol;symbol=-1;
        if(which<0)return;
        for(var player:bar.getPlayers()){int c=SYMBOL_COLOURS[which];player.displayClientMessage(Component.translatable("message.primordial.first_sage.symbol."+which).withStyle(s->s.withColor(c)),true);}
        switch(which){
            case 0->{
                var from=position();
                for(var to:rakes(target)){
                    for(var e:foes(from,15))if(BaihuLord.nearLine(e.position(),from,to,1.2)){strike(e,14);e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,60,1),this);}
                    SinParticles.lineFar(level,SinParticles.SPARK.get(),0xE83232,from.add(0,0.6,0),to.add(0,0.6,0),0.5,0);
                }
                sound(SoundEvents.PLAYER_ATTACK_SWEEP,2F,0.5F);
            }
            case 1->{for(var player:level.players())if(contender(player)&&near(player))strikes.add(new Strike(player.position(),2.5,12,1,20));sound(SoundEvents.BUBBLE_COLUMN_UPWARDS_INSIDE,2F,0.6F);}
            case 2->{
                for(var player:level.players())if(contender(player)&&near(player))strikes.add(new Strike(player.position(),2.5,12,2,20));
                for(int i=0;i<4;i++)strikes.add(new Strike(target.position().add((getRandom().nextDouble()-0.5)*14,0,(getRandom().nextDouble()-0.5)*14),2.5,12,2,20+i*3));
                sound(SoundEvents.LIGHTNING_BOLT_THUNDER,1.2F,1.6F);
            }
            default->{
                strikes.add(new Strike(target.position(),2,9,3,16));
                for(int i=0;i<8;i++){double a=Math.PI*2*i/8;strikes.add(new Strike(target.position().add(Math.cos(a)*4,0,Math.sin(a)*4),2,9,3,16+i));}
                sound(SoundEvents.BLAZE_SHOOT,2F,0.8F);
            }
        }
    }
    void mark(ServerLevel level,Strike strike) {
        if(strike.left%3!=0)return;
        int c=strike.kind==0?ALBEDO:SYMBOL_COLOURS[strike.kind];
        SinParticles.ring(level,SinParticles.GLINT.get(),c,strike.at,strike.radius,(int)(strike.radius*8),0.02);
        if(strike.kind==0)SinParticles.lineFar(level,SinParticles.FLARE.get(),c,strike.at.add(0,14,0),strike.at.add(0,14-14*(1-strike.left/30.0),0),1.5,0);
        if(strike.kind==3)SinParticles.far(level,SinParticles.EMBER.get(),c,strike.at.x,strike.at.y+6,strike.at.z,0,-0.3,0);
    }
    void land(ServerLevel level,Strike strike) {
        int c=strike.kind==0?ALBEDO:SYMBOL_COLOURS[strike.kind];
        for(var e:foes(strike.at,strike.radius)){
            spell(e,strike.damage);
            if(strike.kind==1){e.setDeltaMovement(e.getDeltaMovement().add(0,1.0,0));e.hurtMarked=true;}
            if(strike.kind==2){e.setDeltaMovement(e.getDeltaMovement().add(0,0.5,0));e.hurtMarked=true;}
            if(strike.kind==3)e.setSecondsOnFire(4);
        }
        if(strike.kind==2){var bolt=EntityType.LIGHTNING_BOLT.create(level);if(bolt!=null){bolt.setVisualOnly(true);bolt.moveTo(strike.at);level.addFreshEntity(bolt);}}
        if(strike.kind==1)level.sendParticles(ParticleTypes.SPLASH,strike.at.x,strike.at.y+1,strike.at.z,60,0.8,1.5,0.8,0.2);
        if(strike.kind==3)level.sendParticles(ParticleTypes.FLAME,strike.at.x,strike.at.y+0.3,strike.at.z,14,0.6,0.3,0.6,0.04);
        SinShockwave.spread(level,strike.at,(float)strike.radius+0.5F,8,c,false,0);
    }
    /** the beams of the Golden Rule: two, or four once the Stone wakes */
    int beams() {return stage()==4?4:2;}
    void drawBeams(ServerLevel level,boolean warning) {
        var from=position().add(0,1,0);
        for(int b=0;b<beams();b++){double a=sweepAngle+Math.PI*2*b/beams();
            var to=from.add(Math.cos(a)*SWEEP_LENGTH,0,Math.sin(a)*SWEEP_LENGTH);
            SinParticles.lineFar(level,warning?SinParticles.GLINT.get():SinParticles.FLARE.get(),CITRINITAS,from,to,warning?1.5:0.7,0);}
    }
    void sweeping(ServerLevel level) {
        sweep--;getNavigation().stop();setDeltaMovement(0,getDeltaMovement().y,0);
        sweepAngle+=sweepDir*Math.toRadians(stage()==4?40:30)/20;
        setYRot((float)Math.toDegrees(sweepAngle)-90);yBodyRot=getYRot();
        if(sweep%2==0)drawBeams(level,false);
        for(var player:level.players()){
            if(!contender(player)||!near(player))continue;
            var rel=player.position().subtract(position());double dy=rel.y;
            if(dy<-1.5||dy>3)continue;
            for(int b=0;b<beams();b++){double a=sweepAngle+Math.PI*2*b/beams();
                double along=rel.x*Math.cos(a)+rel.z*Math.sin(a),across=Math.abs(-rel.x*Math.sin(a)+rel.z*Math.cos(a));
                if(along<0||along>SWEEP_LENGTH||across>SWEEP_WIDTH)continue;
                int last=beamHit.getOrDefault(player.getUUID(),-100);
                if(tickCount-last>=10){beamHit.put(player.getUUID(),tickCount);spell(player,8);}
            }
        }
        if(sweep%20==0)sound(SoundEvents.BEACON_AMBIENT,2F,1.8F);
    }
    void opusWarning(ServerLevel level,Vec3 seat,int left) {
        getNavigation().stop();
        if(lit<0)return;
        var at=circle(lit);
        if(left%2==0){
            SinParticles.ringFar(level,SinParticles.FLARE.get(),RUBEDO,seat.add(0,20,0),26,64,-0.02);
            SinParticles.ringFar(level,SinParticles.GLINT.get(),0xFFFFFF,seat.add(0,20,0),26*(1-left/(double)OPUS_WINDUP),40,-0.05);
            SinParticles.ringFar(level,SinParticles.FLARE.get(),CIRCLE_COLOURS[lit],at,CIRCLE_RADIUS,24,0.08);
            SinParticles.lineFar(level,SinParticles.FLARE.get(),CIRCLE_COLOURS[lit],at,at.add(0,16,0),1,0);
        }
        if(left%20==0)for(var player:bar.getPlayers())player.displayClientMessage(Component.translatable("message.primordial.first_sage.opus",
            element(lit),left/20).withStyle(s->s.withColor(RUBEDO).withBold(true)),true);
    }
    void opusFall(ServerLevel level,Vec3 seat) {
        act(5,30);
        if(lit<0)return;
        var at=circle(lit);
        for(var player:level.players()){
            if(!contender(player)||!near(player))continue;
            if(ArcaneSkill.horizontal(at,player.position())<=CIRCLE_RADIUS+0.5&&Math.abs(at.y-player.getY())<4)continue;
            boolean[] struck={false};
            AffixCombat.secondary(()->struck[0]=player.hurt(ArcaneSkill.magic(this),player.getMaxHealth()*OPUS_SHARE));
            if(struck[0])local.primordial.achievement.Achievements.opusSurvived(player);
        }
        SinShockwave.spread(level,seat,(float)SinBoss.ARENA_RADIUS,24,RUBEDO,false,0);
        SinShockwave.spread(level,seat.add(0,0.2,0),18F,18,COLOUR,false,0);
        level.sendParticles(ParticleTypes.END_ROD,seat.x,seat.y+10,seat.z,200,12,5,12,0.1);
        sound(SoundEvents.GENERIC_EXPLODE,3F,0.4F);sound(SoundEvents.BEACON_DEACTIVATE,3F,0.4F);
        lit=-1;
    }
    static Component element(int i) {int c=CIRCLE_COLOURS[i];return Component.translatable("element.primordial."+ELEMENTS[i]).withStyle(s->s.withColor(c));}
    void announce(String key,int colour) {
        for(var player:bar.getPlayers())player.displayClientMessage(Component.translatable(key).withStyle(s->s.withColor(colour).withBold(true)),false);
    }

    // ------------------------------------------------------------------ death, spoils, rest
    @Override public void die(DamageSource source) {
        super.die(source);
        if(isDeadOrDying()&&level() instanceof ServerLevel level){
            clear();
            local.primordial.achievement.Achievements.firstSageFelled(level,this,source);
            // the Great Work is done: the whole world hears of it
            for(var player:level.getServer().getPlayerList().getPlayers())
                player.sendSystemMessage(Component.translatable("message.primordial.first_sage.complete").withStyle(s->s.withColor(RUBEDO).withBold(true)));
        }
    }
    @Override protected void dropCustomDeathLoot(DamageSource source,int looting,boolean hit) {
        super.dropCustomDeathLoot(source,looting,hit);
        if(!(level() instanceof ServerLevel level))return;
        spawnAtLocation(new ItemStack(SinBosses.PHILOSOPHER_STONE.get()));
        spawnAtLocation(new ItemStack(SinBosses.DIVINITY_SHARD.get(),3));
        spawnAtLocation(new ItemStack(SinBosses.SAGE_SIGIL.get()));
        // the Gem Sword's recipe, always
        {var item=new ItemEntity(level,getX(),getY()+1,getZ(),AlchemyRecipeItem.create(SinBosses.GEM_SWORD_RECIPE));item.setDefaultPickUpDelay();level.addFreshEntity(item);}
        for(int i=0;i<2;i++){
            var recipe=local.primordial.spirit.SpiritRealm.realmRecipe(level.getServer(),getRandom());
            if(recipe!=null){var item=new ItemEntity(level,getX(),getY()+1,getZ(),AlchemyRecipeItem.create(recipe));item.setDefaultPickUpDelay();level.addFreshEntity(item);}
        }
    }
    void clear() {strikes.clear();pools.clear();solve.clear();beamHit.clear();}
    @Override protected void onRest() {
        clear();
        local.primordial.achievement.Achievements.sageReset(this);
        entityData.set(STAGE,1);bar.setColor(barColour());setNoGravity(false);
        getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(HASTE);
        coagula=false;coagulaTicks=0;stun=0;sweep=0;lit=-1;symbol=-1;seen=-1;
        java.util.Arrays.fill(progress,0);java.util.Arrays.fill(recent,0);
    }

    @Override protected void ambient() {
        var random=getRandom();
        if(dormant()&&random.nextInt(3)!=0)return;
        int c=switch(stage()){case 2->ALBEDO;case 3->CITRINITAS;case 4->RUBEDO;default->NIGREDO;};
        // glyphs drifting off its robe, and the four elements' sparks round it
        if(random.nextInt(2)==0){double a=random.nextDouble()*Math.PI*2,r=0.8+random.nextDouble()*1.2;
            level().addParticle(SinParticles.GLINT.get().of(c),getX()+Math.cos(a)*r,getY()+0.4+random.nextDouble()*3.2,getZ()+Math.sin(a)*r,0,0.02,0);}
        if(random.nextInt(7)==0){int e=random.nextInt(4);double a=tickCount*0.05+Math.PI/2*e;
            level().addParticle(SinParticles.FLARE.get().of(CIRCLE_COLOURS[e]),getX()+Math.cos(a)*1.2,getY()+2.2,getZ()+Math.sin(a)*1.2,0,0,0);}
        if(!dormant()&&random.nextInt(4)==0)level().addParticle(ParticleTypes.ENCHANT,getX(),getY()+3.6,getZ(),(random.nextDouble()-0.5)*2,-0.5,(random.nextDouble()-0.5)*2);
    }
    @Override public void addAdditionalSaveData(CompoundTag tag) {super.addAdditionalSaveData(tag);}
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        // a fight broken off by a restart starts again from the beginning
        entityData.set(STAGE,1);setNoGravity(false);
    }
}
