package local.primordial.boss;

import java.util.ArrayList;
import java.util.List;
import local.primordial.affix.AffixMaterials;
import local.primordial.affix.BossAffix;
import local.primordial.entity.alchemical.ArcaneSkill;
import local.primordial.item.AlchemyRecipeItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * A Sin Lord: one of the seven bosses of the Spirit Realm, each sleeping in its own sanctum. It lies dormant (and cannot
 * be hurt from afar: any hit wakes it) until a player comes within {@link #WAKE_RADIUS} of its seat; then it rises, shows
 * its bar, and fights within its arena, pulled back to its seat if drawn too far. Left alone for half a minute it
 * heals, returns and sleeps again. At half health it is enraged. Killed, it drops its sin's boss material, experience
 * and alchemy recipes, and never returns: it was born with its sanctum.
 *
 * Each lord has a set of {@link ArcaneSkill}s, only one cast at a time with a short breath between, and an action number
 * synced to clients so its model plays the matching animation.
 */
public abstract class SinBoss extends Monster implements GeoEntity, SinEntity {
    public static final double WAKE_RADIUS=20,ARENA_RADIUS=30,LEASH=36,FORGET_RADIUS=56;
    public static final int RESET_TICKS=600,BREATH=25;
    static final EntityDataAccessor<Boolean> DORMANT=SynchedEntityData.defineId(SinBoss.class,EntityDataSerializers.BOOLEAN);
    static final EntityDataAccessor<Boolean> ENRAGED=SynchedEntityData.defineId(SinBoss.class,EntityDataSerializers.BOOLEAN);
    static final EntityDataAccessor<Integer> ACTION=SynchedEntityData.defineId(SinBoss.class,EntityDataSerializers.INT);

    private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
    protected final ServerBossEvent bar;
    protected BlockPos home;
    private int alone,breath,actionTicks;
    protected final List<ArcaneSkill> skills=new ArrayList<>();
    /** client side only: this is a stand-in drawn as a statue, not a living lord */
    public boolean statue;

    protected SinBoss(EntityType<? extends SinBoss> type,Level level) {
        super(type,level);
        bar=new ServerBossEvent(Component.translatable(type.getDescriptionId()).withStyle(s->s.withColor(colour())),barColour(),BossEvent.BossBarOverlay.NOTCHED_10);
        bar.setVisible(false);
        setPersistenceRequired();
        xpReward=0;
    }

    // ------------------------------------------------------------------ what each lord says about itself
    /** its sin; null for the Primordial Lord, which is above the seven */
    public abstract BossAffix sin();
    /** how near its seat a player wakes it, how far it may be drawn before it is pulled back, and how far it gives up */
    protected double wakeRadius() {return WAKE_RADIUS;}
    protected double leash() {return LEASH;}
    protected double forget() {return FORGET_RADIUS;}
    /** whether it shows a boss bar (a mere echo of a lord does not) */
    protected boolean showsBar() {return true;}
    /** whether its fall is announced and leaves a statue */
    protected boolean memorable() {return true;}
    /** its fall is remembered by everyone who fought it: a sin lord's or a guardian's opens the way on */
    protected void onFelled(ServerLevel level) {
        if(sin()!=null)local.primordial.spirit.PrimordialTemple.recordDefeat(level,sin(),position());
        var guardian=local.primordial.spirit.Guardian.of(getType());
        if(guardian!=null)local.primordial.spirit.PrimordialTemple.recordGuardianDefeat(level,guardian,position());
    }
    /** whether it is enraged on falling to half health (the Primordial Lord has its own phases instead) */
    protected boolean ragesAtHalf() {return true;}
    /** the entity id path, which also names its model, animations and texture */
    public abstract String bossId();
    /**
     * Beside its health, what its own bar shows (BossBars on the client): flags (1 enraged, set here; 2 and 4 a state of
     * its own), a count, a meter from 0 to 1 (negative: none) and an accent colour (0: its own).
     */
    public record BarLook(int flags,int count,float meter,int accent) {}
    protected BarLook barLook() {return new BarLook(0,0,-1,0);}
    private BarLook sentLook;
    private BarLook fullLook() {var look=barLook();float meter=look.meter()<0?-1:Math.round(Math.min(1,look.meter())*50)/50F;return new BarLook(look.flags()|(enraged()?1:0),look.count(),meter,look.accent());}
    /** how far its drawn model reaches past its hitbox, so it is not culled while part of it is in view */
    public double modelReach() {return 0;}
    @Override public net.minecraft.world.phys.AABB getBoundingBoxForCulling() {
        double reach=modelReach();return reach>0?getBoundingBox().inflate(reach):super.getBoundingBoxForCulling();
    }
    protected abstract BossEvent.BossBarColor barColour();
    /** the names of its action animations, by action number (0 is none) */
    protected abstract String[] actions();
    /** its own ambient particles, client side */
    protected abstract void ambient();
    /** what to say and do when it wakes */
    protected abstract SoundEvent wakeSound();
    /** called once, when it falls below half health */
    protected void onEnrage(ServerLevel level) {}
    /** its own per-tick fighting beyond its skills (dashes, auras...), server side, while awake */
    protected void fight(ServerLevel level) {}
    public int experience() {return 400;}
    @Override public int colour() {return sin().color();}

    @Override protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(DORMANT,true);entityData.define(ENRAGED,false);entityData.define(ACTION,0);
    }
    public boolean dormant() {return entityData.get(DORMANT);}
    public boolean enraged() {return entityData.get(ENRAGED);}
    public int action() {return entityData.get(ACTION);}
    /** play an action animation for a while */
    protected void act(int action,int ticks) {entityData.set(ACTION,action);actionTicks=ticks;}
    public BlockPos home() {return home==null?blockPosition():home;}
    /** its skills, in the order it was given them */
    public List<ArcaneSkill> skills() {return skills;}
    public void setHome(BlockPos pos) {home=pos;}
    /** wake it now (as a nearby player or a blow does) */
    /** a stand-in drawn on the client (a boss bar's portrait): awake, so it shows its waking pose */
    public void portrait() {entityData.set(DORMANT,false);}
    public void wake() {
        if(!dormant()||!(level() instanceof ServerLevel level))return;
        entityData.set(DORMANT,false);
        playSound(wakeSound(),3F,0.8F);
        var at=position().add(0,getBbHeight()*0.5,0);
        ArcaneSkill.burst(level,this,at,40,0.5);
        ArcaneSkill.ring(level,this,position(),3,24,0.1);
        for(var player:level.players())if(player.distanceToSqr(this)<64*64)
            player.displayClientMessage(Component.translatable("message.primordial.sin_lord.wake",getDisplayName()).withStyle(s->s.withColor(colour()).withBold(true)),true);
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(0,new FloatGoal(this));
        goalSelector.addGoal(8,new LookAtPlayerGoal(this,Player.class,24F));
        targetSelector.addGoal(1,new HurtByTargetGoal(this));
        targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Player.class,false));
    }
    /** a dormant lord neither moves, turns nor seeks a target: its goals are held by their control flags */
    private void hold(boolean asleep) {
        for(var flag:new net.minecraft.world.entity.ai.goal.Goal.Flag[]{net.minecraft.world.entity.ai.goal.Goal.Flag.MOVE,
            net.minecraft.world.entity.ai.goal.Goal.Flag.LOOK,net.minecraft.world.entity.ai.goal.Goal.Flag.JUMP}){
            if(asleep)goalSelector.disableControlFlag(flag);else goalSelector.enableControlFlag(flag);
        }
        if(asleep)targetSelector.disableControlFlag(net.minecraft.world.entity.ai.goal.Goal.Flag.TARGET);
        else targetSelector.enableControlFlag(net.minecraft.world.entity.ai.goal.Goal.Flag.TARGET);
    }

    @Override public void aiStep() {
        super.aiStep();
        if(level().isClientSide){ambient();return;}
        if(actionTicks>0&&--actionTicks==0)entityData.set(ACTION,0);
    }
    @Override protected void customServerAiStep() {
        super.customServerAiStep();
        if(!(level() instanceof ServerLevel level))return;
        if(home==null)home=blockPosition();
        var seat=Vec3.atBottomCenterOf(home);
        hold(dormant());
        if(dormant()){
            setTarget(null);getNavigation().stop();setDeltaMovement(getDeltaMovement().multiply(0,1,0));
            getMoveControl().setWantedPosition(getX(),getY(),getZ(),0);
            if(getHealth()<getMaxHealth()&&tickCount%20==0)heal(getMaxHealth()*0.05F);
            for(var player:level.players())if(contender(player)&&player.position().distanceTo(seat)<wakeRadius()){wake();break;}
            return;
        }
        // the bar, for everyone near its sanctum
        if(showsBar()){
            bar.setVisible(true);bar.setProgress(getHealth()/getMaxHealth());
            for(var player:level.players()){
                boolean near=player.position().distanceTo(seat)<forget()+16;
                if(near&&!bar.getPlayers().contains(player)){bar.addPlayer(player);local.primordial.network.MenuDropNetwork.sendBossBar(player,bar.getId(),bossId(),getId(),fullLook());}
                else if(!near&&bar.getPlayers().contains(player))bar.removePlayer(player);
            }
            var look=fullLook();
            if(!look.equals(sentLook)){sentLook=look;for(var player:bar.getPlayers())local.primordial.network.MenuDropNetwork.sendBossBar(player,bar.getId(),bossId(),getId(),look);}
        }
        // abandoned: heal, return, sleep
        boolean anyone=false;
        for(var player:level.players())if(contender(player)&&player.position().distanceTo(seat)<forget()){anyone=true;break;}
        if(!anyone){if(++alone>=RESET_TICKS){rest(seat);return;}}else alone=0;
        // held to its arena
        if(position().distanceTo(seat)>leash()){
            ArcaneSkill.burst(level,this,position().add(0,1,0),20,0.3);
            teleportTo(seat.x,seat.y+0.5,seat.z);getNavigation().stop();
            ArcaneSkill.burst(level,this,seat.add(0,1,0),20,0.3);
        }
        if(getTarget()!=null&&getTarget().position().distanceTo(seat)>forget())setTarget(null);
        if(ragesAtHalf()&&!enraged()&&getHealth()<getMaxHealth()*0.5F){
            entityData.set(ENRAGED,true);
            ArcaneSkill.burst(level,this,position().add(0,getBbHeight()*0.6,0),60,0.6);
            playSound(wakeSound(),3F,0.6F);
            for(var player:bar.getPlayers())player.displayClientMessage(Component.translatable("message.primordial.sin_lord.enrage",getDisplayName()).withStyle(s->s.withColor(colour()).withBold(true)),true);
            onEnrage(level);
        }
        fight(level);
        // one skill at a time, with a breath between
        boolean casting=false;
        for(var skill:skills)if(skill.casting()){skill.tick(this,false);casting=true;}
        if(casting){breath=BREATH;return;}
        boolean may=breath<=0;if(breath>0)breath--;
        int start=getRandom().nextInt(Math.max(1,skills.size()));
        for(int i=0;i<skills.size();i++){
            var skill=skills.get((start+i)%skills.size());
            if(skill.tick(this,may))may=false;
        }
    }
    /** a survival player who can fight it */
    public static boolean contender(Player player) {return player.isAlive()&&!player.isCreative()&&!player.isSpectator();}
    private void rest(Vec3 seat) {
        alone=0;setTarget(null);teleportTo(seat.x,seat.y,seat.z);setHealth(getMaxHealth());
        entityData.set(DORMANT,true);entityData.set(ENRAGED,false);entityData.set(ACTION,0);
        bar.setVisible(false);bar.removeAllPlayers();
        onRest();
    }
    /** put back whatever the fight changed */
    protected void onRest() {}

    // ------------------------------------------------------------------ harm
    @Override public boolean hurt(DamageSource source,float amount) {
        if(isInvulnerableTo(source))return false;
        // a sleeping lord takes no harm, except what passes through invulnerability (such as /kill)
        if(dormant()&&!source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)){
            // a blow from someone who came to fight wakes it; it takes no harm while asleep
            if(source.getEntity() instanceof Player player&&contender(player))wake();
            return false;
        }
        return super.hurt(source,amount);
    }
    @Override public boolean isInvulnerableTo(DamageSource source) {
        return source.is(DamageTypeTags.IS_FALL)||source.is(DamageTypes.IN_WALL)||source.is(DamageTypes.DROWN)||source.is(DamageTypes.CRAMMING)
            ||source.is(DamageTypeTags.IS_FIRE)&&fireImmune()||super.isInvulnerableTo(source);
    }
    @Override public boolean removeWhenFarAway(double distance) {return false;}
    @Override public boolean requiresCustomPersistence() {return true;}
    @Override public boolean canChangeDimensions() {return false;}
    @Override public boolean canBeLeashed(Player player) {return false;}
    @Override protected boolean canRide(net.minecraft.world.entity.Entity vehicle) {return false;}
    @Override public boolean isPushable() {return false;}

    // ------------------------------------------------------------------ death and spoils
    @Override public int getExperienceReward() {return experience();}
    @Override protected void dropCustomDeathLoot(DamageSource source,int looting,boolean hit) {
        super.dropCustomDeathLoot(source,looting,hit);
        if(sin()==null)return;
        var material=AffixMaterials.BOSS_ITEMS.get(sin()).get();
        spawnAtLocation(new ItemStack(material,2+getRandom().nextInt(2)+Math.min(2,looting)));
        if(level() instanceof ServerLevel level){
            // the primordial charm recipe this lord carries in this world, always
            var primordial=SinBosses.primordialRecipe(level.getServer(),sin());
            if(primordial!=null){var item=new ItemEntity(level,getX(),getY()+1,getZ(),AlchemyRecipeItem.create(primordial));item.setDefaultPickUpDelay();level.addFreshEntity(item);}
            int recipes=1+getRandom().nextInt(2);
            for(int i=0;i<recipes;i++){
                var recipe=local.primordial.spirit.SpiritRealm.realmRecipe(level.getServer(),getRandom());
                if(recipe!=null){var item=new ItemEntity(level,getX(),getY()+1,getZ(),AlchemyRecipeItem.create(recipe));item.setDefaultPickUpDelay();level.addFreshEntity(item);}
            }
        }
    }
    @Override public void die(DamageSource source) {
        super.die(source);
        // the Primordial Temple opens to whoever has fought each of the seven to its end
        if(level() instanceof ServerLevel level&&isDeadOrDying())onFelled(level);
        // a lord woken from its statue counts toward 旧敌新战 as it falls again
        if(level() instanceof ServerLevel level&&isDeadOrDying())local.primordial.achievement.Achievements.bossFelled(level,this);
        if(level() instanceof ServerLevel level&&memorable()&&isDeadOrDying()){
            for(var player:level.players())if(player.distanceToSqr(this)<96*96)
                player.sendSystemMessage(Component.translatable("message.primordial.sin_lord.fall",getDisplayName()).withStyle(s->s.withColor(colour()).withBold(true)));
            ArcaneSkill.burst(level,this,position().add(0,getBbHeight()*0.5,0),80,0.7);
            // it leaves a statue where it fell; its sigil will call it back
            if(local.primordial.spirit.Guardian.of(getType())==null)SinStatueBlock.raise(level,this);
        }
        bar.setVisible(false);bar.removeAllPlayers();
    }
    @Override public void stopSeenByPlayer(ServerPlayer player) {super.stopSeenByPlayer(player);bar.removePlayer(player);}
    @Override public void remove(RemovalReason reason) {super.remove(reason);bar.removeAllPlayers();}

    // ------------------------------------------------------------------ saving
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if(home!=null)tag.put("SinHome",NbtUtils.writeBlockPos(home));
        tag.putBoolean("SinDormant",dormant());tag.putBoolean("SinEnraged",enraged());
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if(tag.contains("SinHome"))home=NbtUtils.readBlockPos(tag.getCompound("SinHome"));
        if(tag.contains("SinDormant"))entityData.set(DORMANT,tag.getBoolean("SinDormant"));
        entityData.set(ENRAGED,tag.getBoolean("SinEnraged"));
        bar.setName(getDisplayName().copy().withStyle(s->s.withColor(colour())));
    }

    // ------------------------------------------------------------------ helpers for the lords
    /** a hit from its own hand */
    protected boolean strike(LivingEntity target,float amount) {return target.hurt(ArcaneSkill.melee(this),amount*(enraged()?1.2F:1));}
    protected boolean spell(LivingEntity target,float amount) {return target.hurt(ArcaneSkill.magic(this),amount*(enraged()?1.2F:1));}
    protected void sound(SoundEvent sound,float volume,float pitch) {level().playSound(null,getX(),getY(),getZ(),sound,SoundSource.HOSTILE,volume,pitch);}
    /** its foes on the arena (everyone who could be hurt within its reach) */
    protected List<LivingEntity> foes(Vec3 at,double radius) {return ArcaneSkill.foesAround(this,at,radius,3);}

    // ------------------------------------------------------------------ animation
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        String id=bossId();
        RawAnimation idle=RawAnimation.begin().thenLoop("animation."+id+".idle");
        RawAnimation walk=RawAnimation.begin().thenLoop("animation."+id+".walk");
        RawAnimation sleep=RawAnimation.begin().thenLoop("animation."+id+".dormant");
        controllers.add(new AnimationController<>(this,"move",5,state->{
            if(dormant())return state.setAndContinue(sleep);
            return state.setAndContinue(state.isMoving()?walk:idle);
        }));
        RawAnimation attack=RawAnimation.begin().thenPlay("animation."+id+".attack");
        controllers.add(new AnimationController<>(this,"attack",2,state->{
            if(swinging&&action()==0)return state.setAndContinue(attack);
            state.getController().forceAnimationReset();
            return PlayState.STOP;
        }));
        var names=actions();
        RawAnimation[] plays=new RawAnimation[names.length];
        for(int i=1;i<names.length;i++)plays[i]=RawAnimation.begin().thenLoop("animation."+id+"."+names[i]);
        controllers.add(new AnimationController<>(this,"action",3,state->{
            int a=action();
            if(a<=0||a>=plays.length){state.getController().forceAnimationReset();return PlayState.STOP;}
            return state.setAndContinue(plays[a]);
        }));
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() {return cache;}
    // ------------------------------------------------------------------ voice and music (see ModSounds)
    /** its own voice: silent while it sleeps */
    @Override protected net.minecraft.sounds.SoundEvent getAmbientSound() {return dormant()||statue?null:local.primordial.sound.ModSounds.voice(bossId(),"ambient");}
    @Override protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource source) {var voice=local.primordial.sound.ModSounds.voice(bossId(),"hurt");return voice!=null?voice:super.getHurtSound(source);}
    @Override protected net.minecraft.sounds.SoundEvent getDeathSound() {var voice=local.primordial.sound.ModSounds.voice(bossId(),"death");return voice!=null?voice:super.getDeathSound();}
    /** a boss is heard from afar */
    @Override protected float getSoundVolume() {return 2.0F;}
    @Override public int getAmbientSoundInterval() {return 160;}
    /** the music of its fight, played to everyone near while it is awake; null for none */
    public net.minecraft.sounds.SoundEvent battleMusic() {return local.primordial.sound.ModSounds.music(bossId());}
}
