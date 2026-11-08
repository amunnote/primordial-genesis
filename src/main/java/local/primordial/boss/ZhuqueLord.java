package local.primordial.boss;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
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
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 朱雀 · 陵光 — the Vermilion Bird of the South, guardian of fire: a great phoenix, crimson shading to gold, its crest and
 * the long plumes of its tail burning. It hovers over its terrace and burns everything below, and does not die easily.
 * <ul>
 * <li>焚身 Burning Body: whoever strikes it in reach catches fire, and the burning take a fifth more from it.</li>
 * <li>涅槃 Rebirth: the first time it falls it becomes an egg of flame on the ground, with a quarter of its health, for ten
 *     seconds. Break the egg in time and it is dead; fail, and it is reborn with half its health, enraged.</li>
 * <li>焚天之羽 Feathers of Burning Sky: burning feathers rain on rings round its prey.</li>
 * <li>炎翼横扫 Wing Sweep: it swoops low along a line of flame, scorching and leaving the ground burning behind it.</li>
 * <li>凤鸣 Phoenix Cry: a burst of flame round it that throws back and burns, and strips fire resistance; it heals 5%.</li>
 * <li>烈日 Blazing Sun (enraged): it rises high and gathers a sun, then lets it fall on the terrace: the nearer, the worse.</li>
 * </ul>
 */
public class ZhuqueLord extends SinBoss {
    /** how much bigger it is drawn than its model, and how far that reaches past its hitbox (for culling) */
    public static final float MODEL_SCALE=1.9F;
    @Override public double modelReach() {return 7;}
    public static final double HEALTH=1800;
    public static final int COLOUR=0xFF5A2A,GOLD=0xFFC84A,EMBER=0xFF8A2A;
    public static final float BURNING_BONUS=1.2F,EGG_HEALTH=0.25F,REBORN_HEALTH=0.5F;
    public static final int EGG_TICKS=200,SWEEP=16,TRAIL_LIFE=100,SUN_WINDUP=60;
    static final EntityDataAccessor<Boolean> EGG=SynchedEntityData.defineId(ZhuqueLord.class,EntityDataSerializers.BOOLEAN);
    private int egg,sweep;private boolean reborn;
    private Vec3 sweepFrom=Vec3.ZERO,sweepTo=Vec3.ZERO;
    private final Set<UUID> swept=new HashSet<>();
    private final List<Vec3> feathers=new ArrayList<>();
    private final List<double[]> trail=new ArrayList<>();   // x, y, z, ticks left

    public ZhuqueLord(EntityType<? extends ZhuqueLord> type,Level level) {
        super(type,level);
        setNoGravity(true);
        // 焚天之羽
        skills.add(new ArcaneSkill(120,20,0,30,true,(mob,target)->{chooseFeathers(target);return target.position();},
            (level1,mob,target,mark,left)->{if(left%3==0)for(var at:feathers){SinParticles.ringFar(level1,SinParticles.GLINT.get(),COLOUR,at,2,12,0.04);
                SinParticles.far(level1,SinParticles.FLARE.get(),EMBER,at.x,at.y+8-left*0.35,at.z,0,-0.1,0);}},
            (level1,mob,target,mark)->{
                act(1,20);
                for(var at:feathers){
                    for(var e:foes(at,2)){scorch(e,9);e.setSecondsOnFire(4);}
                    level1.sendParticles(ParticleTypes.FLAME,at.x,at.y+0.3,at.z,14,0.6,0.3,0.6,0.04);
                    SinParticles.send(level1,SinParticles.FLARE.get(),EMBER,at.add(0,0.5,0),5,0.6,0,0.08,0);
                }
                feathers.clear();sound(SoundEvents.BLAZE_SHOOT,2F,0.8F);
            }).when(m->egg==0&&sweep==0));
        // 炎翼横扫
        skills.add(new ArcaneSkill(220,18,0,28,true,(mob,target)->target.position(),
            (level1,mob,target,mark,left)->{if(left%2==0){var to=mark.add(mark.subtract(position()).multiply(1,0,1).normalize().scale(10));
                SinParticles.lineFar(level1,SinParticles.FLARE.get(),EMBER,new Vec3(getX(),mark.y+0.2,getZ()),new Vec3(to.x,mark.y+0.2,to.z),1.2,0.02);}},
            (level1,mob,target,mark)->{
                sweepFrom=position();var dir=mark.subtract(position()).multiply(1,0,1).normalize();
                sweepTo=new Vec3(mark.x,mark.y+1.5,mark.z).add(dir.scale(10));sweep=SWEEP;swept.clear();act(2,SWEEP+4);
                sound(SoundEvents.PHANTOM_SWOOP,3F,0.8F);
            }).when(m->egg==0&&sweep==0));
        // 凤鸣
        skills.add(new ArcaneSkill(240,16,0,10,false,(mob,target)->position(),
            (level1,mob,target,mark,left)->{if(left%2==0)SinParticles.ringFar(level1,SinParticles.FLARE.get(),GOLD,position(),10*(1-left/16.0)+1,24,0.03);},
            (level1,mob,target,mark)->{
                act(3,22);
                for(var e:foes(position(),10)){scorch(e,6);e.setSecondsOnFire(6);e.removeEffect(MobEffects.FIRE_RESISTANCE);ArcaneSkill.knock(e,position(),1.6,0.5);}
                heal(getMaxHealth()*0.05F);
                SinShockwave.spread(level1,position(),10F,14,COLOUR,true,getYRot());SinShockwave.spread(level1,position(),10F,14,GOLD,false,0);
                sound(SoundEvents.ENDER_DRAGON_GROWL,2F,1.8F);sound(SoundEvents.BLAZE_AMBIENT,2F,0.6F);
            }).when(m->egg==0&&sweep==0));
        // 烈日
        skills.add(new ArcaneSkill(400,SUN_WINDUP,0,40,false,(mob,target)->Vec3.atBottomCenterOf(home()),
            (level1,mob,target,mark,left)->{
                act(4,left+10);
                var sun=mark.add(0,14,0);double size=2.5*(1-left/(double)SUN_WINDUP)+0.5;
                if(left%2==0){SinParticles.send(level1,SinParticles.FLARE.get(),GOLD,sun,8,size,0,0,0);SinParticles.ringFar(level1,SinParticles.GLINT.get(),COLOUR,mark,12,40,0.03);}
                level1.sendParticles(ParticleTypes.FLAME,sun.x,sun.y,sun.z,6,size,size,size,0.02);
            },
            (level1,mob,target,mark)->{
                for(var player:level1.players()){
                    if(!contender(player))continue;double d=player.position().distanceTo(mark);if(d>14)continue;
                    scorch(player,(float)Math.max(6,30*(1-d/14)));player.setSecondsOnFire(8);
                }
                SinShockwave.spread(level1,mark,14F,20,COLOUR,false,0);SinShockwave.spread(level1,mark,8F,16,GOLD,false,0);
                level1.sendParticles(ParticleTypes.EXPLOSION_EMITTER,mark.x,mark.y+1,mark.z,1,0,0,0,0);
                level1.sendParticles(ParticleTypes.FLAME,mark.x,mark.y+1,mark.z,200,8,1,8,0.1);
                sound(SoundEvents.GENERIC_EXPLODE,3F,0.6F);
            }).when(m->egg==0&&sweep==0&&enraged()));
    }
    @Override protected void defineSynchedData() {super.defineSynchedData();entityData.define(EGG,false);}
    @Override public BossAffix sin() {return null;}
    @Override public int colour() {return COLOUR;}
    @Override public String bossId() {return "zhuque";}
    @Override protected BarLook barLook() {return new BarLook((egg()?2:0)|(reborn?4:0),0,egg()?egg/(float)EGG_TICKS:-1,0);}
    @Override protected BossEvent.BossBarColor barColour() {return BossEvent.BossBarColor.RED;}
    @Override protected String[] actions() {return new String[]{"","feathers","sweep","cry","sun","egg"};}
    @Override protected SoundEvent wakeSound() {return SoundEvents.BLAZE_AMBIENT;}
    @Override public int experience() {return 1000;}
    @Override protected double wakeRadius() {return 22;}
    @Override protected double leash() {return 38;}
    @Override protected double forget() {return 56;}
    @Override public boolean fireImmune() {return true;}

    public boolean egg() {return entityData.get(EGG);}
    public boolean reborn() {return reborn;}
    /** its fire: a fifth more on whoever already burns */
    void scorch(LivingEntity e,float amount) {spell(e,amount*(e.isOnFire()?BURNING_BONUS:1));}
    void chooseFeathers(LivingEntity target) {
        feathers.clear();feathers.add(target.position());
        for(int i=0;i<8;i++){double a=Math.PI*2*i/8+getRandom().nextDouble()*0.4,r=2.5+getRandom().nextDouble()*3;feathers.add(target.position().add(Math.cos(a)*r,0,Math.sin(a)*r));}
        act(1,24);
    }
    @Override public boolean hurt(DamageSource source,float amount) {
        boolean hurt=super.hurt(source,amount);
        // 焚身: whoever strikes it in reach catches fire
        if(hurt&&!level().isClientSide&&source.getDirectEntity() instanceof LivingEntity striker&&striker.distanceTo(this)<5)striker.setSecondsOnFire(4);
        return hurt;
    }
    @Override public void die(DamageSource source) {
        // 涅槃: the first fall is an egg, not a death
        if(!level().isClientSide&&!reborn&&egg==0&&!source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)){
            egg=EGG_TICKS;entityData.set(EGG,true);setHealth(getMaxHealth()*EGG_HEALTH);act(5,EGG_TICKS);
            setNoGravity(false);getNavigation().stop();feathers.clear();sweep=0;
            for(var player:bar.getPlayers())player.displayClientMessage(Component.translatable("message.primordial.zhuque.egg").withStyle(s->s.withColor(GOLD).withBold(true)),false);
            if(level() instanceof ServerLevel level)level.sendParticles(ParticleTypes.FLAME,getX(),getY()+1,getZ(),60,1,1,1,0.1);
            sound(SoundEvents.BLAZE_DEATH,2F,0.6F);
            return;
        }
        super.die(source);
    }
    void rebirth(ServerLevel level) {
        reborn=true;egg=0;entityData.set(EGG,false);setHealth(getMaxHealth()*REBORN_HEALTH);setNoGravity(true);
        entityData.set(ENRAGED,true);
        for(var e:foes(position(),8)){scorch(e,10);e.setSecondsOnFire(6);ArcaneSkill.knock(e,position(),1.4,0.6);}
        SinShockwave.spread(level,position(),8F,14,GOLD,false,0);SinShockwave.spread(level,position().add(0,1.5,0),8F,14,COLOUR,true,getYRot());
        level.sendParticles(ParticleTypes.FLAME,getX(),getY()+1.5,getZ(),120,1.5,1.5,1.5,0.2);
        for(var player:bar.getPlayers())player.displayClientMessage(Component.translatable("message.primordial.zhuque.reborn").withStyle(s->s.withColor(COLOUR).withBold(true)),false);
        sound(SoundEvents.TOTEM_USE,2F,0.7F);sound(SoundEvents.ENDER_DRAGON_GROWL,2F,1.6F);
    }

    @Override protected void fight(ServerLevel level) {
        // the burning trail left by its sweeps
        for(var it=trail.iterator();it.hasNext();){var t=it.next();
            if(--t[3]<=0){it.remove();continue;}
            if(tickCount%3==0)level.sendParticles(ParticleTypes.FLAME,t[0],t[1]+0.1,t[2],1,0.3,0.05,0.3,0.01);
            if(tickCount%10==0)for(var e:foes(new Vec3(t[0],t[1],t[2]),1.5)){scorch(e,2);e.setSecondsOnFire(3);}
        }
        // the egg: still, burning, counting down
        if(egg>0){
            getNavigation().stop();setDeltaMovement(0,getDeltaMovement().y,0);
            if(egg%5==0)level.sendParticles(ParticleTypes.FLAME,getX(),getY()+1,getZ(),6,0.6,0.8,0.6,0.02);
            if(egg%20==0)for(var player:bar.getPlayers())player.displayClientMessage(Component.translatable("message.primordial.zhuque.egg_timer",egg/20).withStyle(s->s.withColor(GOLD)),true);
            if(--egg==0)rebirth(level);
            return;
        }
        // the sweep
        if(sweep>0){
            sweep--;double t=1-sweep/(double)SWEEP;
            var want=sweepFrom.lerp(sweepTo,t);if(t>0.15&&t<0.85)want=new Vec3(want.x,sweepTo.y,want.z);
            move(MoverType.SELF,want.subtract(position()));
            var dir=sweepTo.subtract(sweepFrom);setYRot((float)(Mth.atan2(dir.z,dir.x)*Mth.RAD_TO_DEG)-90);yBodyRot=getYRot();
            for(var e:foes(position(),2.8))if(swept.add(e.getUUID())){scorch(e,14);e.setSecondsOnFire(5);ArcaneSkill.knock(e,position(),0.8,0.5);}
            var below=level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,(int)Math.floor(getX()),(int)Math.floor(getZ()));
            trail.add(new double[]{getX(),below,getZ(),TRAIL_LIFE});
            level.sendParticles(ParticleTypes.FLAME,getX(),getY()+1,getZ(),8,1.2,0.4,1.2,0.05);
            return;
        }
        // hovering over its prey, a dozen blocks off and high
        var target=getTarget();var seat=Vec3.atBottomCenterOf(home());
        Vec3 want;
        if(target!=null){
            var away=position().subtract(target.position()).multiply(1,0,1);away=away.lengthSqr()<1e-3?new Vec3(1,0,0):away.normalize();
            want=target.position().add(away.scale(10)).add(0,6,0);
            if(want.distanceTo(seat)>ARENA_RADIUS-3)want=seat.add(want.subtract(seat).normalize().scale(ARENA_RADIUS-3)).add(0,6,0);
            getLookControl().setLookAt(target,30,30);
            setYRot((float)(Mth.atan2(target.getZ()-getZ(),target.getX()-getX())*Mth.RAD_TO_DEG)-90);yBodyRot=getYRot();
        }else want=seat.add(0,5,0);
        if(action()==4)want=seat.add(0,16,0);   // gathering its sun, high over the heart of the terrace
        var move=want.subtract(position()).scale(0.07);if(move.length()>0.5)move=move.normalize().scale(0.5);
        setDeltaMovement(move);
    }
    @Override public void wake() {super.wake();setNoGravity(true);}
    @Override protected void onRest() {egg=0;sweep=0;reborn=false;entityData.set(EGG,false);trail.clear();feathers.clear();setNoGravity(false);}
    @Override protected void dropCustomDeathLoot(DamageSource source,int looting,boolean hit) {
        super.dropCustomDeathLoot(source,looting,hit);
        if(!(level() instanceof ServerLevel level))return;
        spawnAtLocation(new ItemStack(SinBosses.ZHUQUE_FEATHER.get(),2+Math.min(2,looting)));
        spawnAtLocation(new ItemStack(SinBosses.ZHUQUE_SIGIL.get()));
        // the Gem Sword's hilt, always
        spawnAtLocation(new ItemStack(local.primordial.Primordial.PRIMORDIAL_HILT.get()));
        for(int i=0;i<2;i++){var recipe=local.primordial.spirit.SpiritRealm.realmRecipe(level.getServer(),getRandom());
            if(recipe!=null){var item=new ItemEntity(level,getX(),getY()+1,getZ(),AlchemyRecipeItem.create(recipe));item.setDefaultPickUpDelay();level.addFreshEntity(item);}}
    }
    @Override protected void ambient() {
        var random=getRandom();
        if(dormant()&&random.nextInt(3)!=0)return;
        // flame off its crest, wings and tail; embers drifting down
        level().addParticle(ParticleTypes.FLAME,getX()+(random.nextDouble()-0.5)*3,getY()+1+random.nextDouble()*2,getZ()+(random.nextDouble()-0.5)*3,0,0.02,0);
        if(random.nextInt(2)==0)level().addParticle(SinParticles.EMBER.get().of(random.nextInt(3)==0?GOLD:EMBER),getX()+(random.nextDouble()-0.5)*5,getY()+random.nextDouble()*3,getZ()+(random.nextDouble()-0.5)*5,0,-0.02,0);
        if(egg()&&random.nextInt(2)==0)level().addParticle(ParticleTypes.LAVA,getX(),getY()+1.2,getZ(),0,0,0);
    }
    @Override public void addAdditionalSaveData(CompoundTag tag) {super.addAdditionalSaveData(tag);tag.putBoolean("Reborn",reborn);}
    @Override public void readAdditionalSaveData(CompoundTag tag) {super.readAdditionalSaveData(tag);reborn=tag.getBoolean("Reborn");setNoGravity(!dormant());}
}
