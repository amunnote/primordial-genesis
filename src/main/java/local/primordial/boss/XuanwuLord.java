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
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 玄武 · 执明 — the Black Tortoise of the North, guardian of water: a vast black tortoise, its shell carved with glowing
 * runes and grown with ice, a great serpent coiled round it. All armour and little attack, and very hard to bring down.
 * <ul>
 * <li>玄甲 Dark Shell: only its head, within the arc it faces, takes full harm; the shell takes a quarter.</li>
 * <li>缩壳 Withdrawal: at three quarters, half and a quarter of its health it draws into its shell for eight seconds,
 *     taking almost nothing and healing 1% a second; but every blow on the shell cracks it, and 220 harm of cracks breaks
 *     it open: it is stunned for five seconds and takes double from everywhere.</li>
 * <li>灵蛇 the Serpent bites whoever comes within its reach every three seconds (twice as often while it hides), wherever
 *     they stand: behind the shell is no safe place.</li>
 * <li>蛇噬 Serpent Strike: a long lunge, 16 and poison, dragging its prey to the tortoise's jaws.</li>
 * <li>玄冥水柱 Dark Geysers: a ring of water under everyone near, then a geyser, 12 and thrown up.</li>
 * <li>镇海 Stomp: it rears and stamps, a quake of nine blocks.</li>
 * <li>寒潮 Frost Tide (enraged): a ring of frost rolls out from it; whoever is on the ground when it passes is frozen. Jump.</li>
 * </ul>
 */
public class XuanwuLord extends SinBoss {
    /** how much bigger it is drawn than its model, and how far that reaches past its hitbox (for culling) */
    public static final float MODEL_SCALE=1.5F;
    @Override public double modelReach() {return 9;}
    public static final double HEALTH=3200;
    public static final int COLOUR=0x3AB8D8,DARK=0x14242E,FROST=0xBFF4FF;
    public static final float SHELL_SHARE=0.25F,HIDING_SHARE=0.05F,BROKEN_SHARE=2F,CRACK=220,HEAD_ARC=55;
    public static final int HIDE_TICKS=160,STUN_TICKS=100,BITE_INTERVAL=60,TIDE_TICKS=30;
    public static final double BITE_REACH=7,TIDE_RADIUS=18;
    static final EntityDataAccessor<Integer> STATE=SynchedEntityData.defineId(XuanwuLord.class,EntityDataSerializers.INT);
    private int hide,stun,bite,withdrawals,tide;private float crack;
    private Vec3 tideCentre=Vec3.ZERO;private final Set<UUID> tideHit=new HashSet<>();
    private final List<Vec3> geysers=new ArrayList<>();

    public XuanwuLord(EntityType<? extends XuanwuLord> type,Level level) {
        super(type,level);
        // 蛇噬
        skills.add(new ArcaneSkill(120,12,0,10,true,(mob,target)->target.position(),
            (level1,mob,target,mark,left)->{act(3,left+8);SinParticles.line(level1,SinParticles.WISP.get(),COLOUR,serpentHead(),target.position().add(0,1,0),1.2,0);},
            (level1,mob,target,mark)->{
                if(target.distanceTo(this)>12)return;
                strike(target,16);target.addEffect(new MobEffectInstance(MobEffects.POISON,80,1),this);
                var jaws=front(getBbWidth()*0.6+1.5);var pull=jaws.subtract(target.position()).scale(0.25);
                target.setDeltaMovement(pull.x,0.3,pull.z);target.hurtMarked=true;
                SinParticles.line(level1,SinParticles.SHARD.get(),COLOUR,serpentHead(),target.position().add(0,1,0),0.5,0);
                sound(SoundEvents.EVOKER_FANGS_ATTACK,2F,0.6F);
            }).when(m->hide==0&&stun==0));
        // 玄冥水柱
        skills.add(new ArcaneSkill(160,30,0,28,false,(mob,target)->{chooseGeysers(target);return target.position();},
            (level1,mob,target,mark,left)->{if(left%3==0)for(var at:geysers){SinParticles.ring(level1,SinParticles.GLINT.get(),COLOUR,at,2.2,16,0.04);
                level1.sendParticles(ParticleTypes.BUBBLE_POP,at.x,at.y+0.2,at.z,4,1,0.1,1,0.02);}},
            (level1,mob,target,mark)->{
                act(4,20);
                for(var at:geysers){
                    for(var e:foes(at,2.2)){spell(e,12);e.setDeltaMovement(e.getDeltaMovement().add(0,1.2,0));e.hurtMarked=true;}
                    for(int i=0;i<8;i++)level1.sendParticles(ParticleTypes.SPLASH,at.x,at.y+i*0.6,at.z,10,0.5,0.2,0.5,0.1);
                    level1.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP,at.x,at.y+0.5,at.z,30,0.4,1.5,0.4,0.1);
                    SinShockwave.spread(level1,at,2.6F,8,COLOUR,false,0);
                }
                geysers.clear();
                sound(SoundEvents.GENERIC_SPLASH,2F,0.6F);sound(SoundEvents.ELDER_GUARDIAN_CURSE,0.6F,1.6F);
            }).when(m->hide==0&&stun==0));
        // 镇海
        skills.add(new ArcaneSkill(140,24,0,9,false,(mob,target)->position(),
            (level1,mob,target,mark,left)->{getNavigation().stop();act(2,left+12);if(left%3==0)SinParticles.ring(level1,SinParticles.GLINT.get(),COLOUR,mark,9,40,0.03);},
            (level1,mob,target,mark)->{
                for(var e:foes(mark,9)){strike(e,14);e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,60,2),this);ArcaneSkill.knock(e,mark,0.8,0.5);}
                SinShockwave.spread(level1,mark,10F,16,COLOUR,false,0);SinShockwave.spread(level1,mark,6F,12,DARK,false,0);
                level1.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK,level1.getBlockState(net.minecraft.core.BlockPos.containing(mark).below())),
                    mark.x,mark.y+0.2,mark.z,80,4,0.2,4,0.3);
                sound(SoundEvents.GENERIC_EXPLODE,2F,0.5F);sound(SoundEvents.RAVAGER_STUNNED,1.5F,0.5F);
            }).when(m->hide==0&&stun==0));
        // 寒潮
        skills.add(new ArcaneSkill(240,20,0,TIDE_RADIUS,false,(mob,target)->position(),
            (level1,mob,target,mark,left)->{getNavigation().stop();if(left%2==0)SinParticles.ring(level1,SinParticles.FLARE.get(),FROST,mark,2.5,16,0.08);},
            (level1,mob,target,mark)->{act(4,20);tide=TIDE_TICKS;tideCentre=mark;tideHit.clear();sound(SoundEvents.GLASS_BREAK,2F,0.5F);sound(SoundEvents.POWDER_SNOW_STEP,2F,0.5F);})
            .when(m->hide==0&&stun==0&&enraged()));
    }
    @Override protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(3,new MeleeAttackGoal(this,1.0,true){
            @Override public boolean canUse() {return hide==0&&stun==0&&super.canUse();}
            @Override public boolean canContinueToUse() {return hide==0&&stun==0&&super.canContinueToUse();}
        });
    }
    @Override protected void defineSynchedData() {super.defineSynchedData();entityData.define(STATE,0);}
    @Override public BossAffix sin() {return null;}
    @Override public int colour() {return COLOUR;}
    @Override public String bossId() {return "xuanwu";}
    @Override protected BarLook barLook() {return new BarLook((hide>0?2:0)|(stun>0?4:0),0,crack/CRACK,0);}
    @Override protected BossEvent.BossBarColor barColour() {return BossEvent.BossBarColor.BLUE;}
    @Override protected String[] actions() {return new String[]{"","shell","stomp","serpent","cast","hide","cracked"};}
    @Override protected SoundEvent wakeSound() {return SoundEvents.ELDER_GUARDIAN_AMBIENT;}
    @Override public int experience() {return 1000;}
    @Override protected double wakeRadius() {return 22;}
    @Override protected double leash() {return 30;}
    @Override protected double forget() {return 52;}

    /** 0 out, 1 hiding in its shell, 2 its shell broken open */
    public int shellState() {return entityData.get(STATE);}
    public boolean hiding() {return hide>0;}
    public boolean stunned() {return stun>0;}
    public float cracks() {return crack;}
    Vec3 front(double reach) {double yaw=Math.toRadians(yBodyRot);return position().add(-Math.sin(yaw)*reach,0,Math.cos(yaw)*reach);}
    /** where the serpent's head rides: reared up over the front of the shell */
    Vec3 serpentHead() {return front(getBbWidth()*0.3).add(0,getBbHeight()+1.5,0);}
    /** a blow on its head, from within the arc it faces (or on the level of its head, not dropped from above) */
    boolean onHead(DamageSource source) {
        var from=source.getSourcePosition();if(from==null)return false;
        var to=from.subtract(position()).multiply(1,0,1);if(to.lengthSqr()<1e-3)return false;
        double yaw=Math.toRadians(yBodyRot);var facing=new Vec3(-Math.sin(yaw),0,Math.cos(yaw));
        return to.normalize().dot(facing)>=Math.cos(Math.toRadians(HEAD_ARC))&&from.y<getY()+getBbHeight()+1;
    }
    @Override public boolean hurt(DamageSource source,float amount) {
        if(!dormant()&&!source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)){
            if(stun>0)amount*=BROKEN_SHARE;
            else if(hide>0){
                crack+=amount;amount*=HIDING_SHARE;
                if(crack>=CRACK)breakShell();
                else if(tickCount%3==0)sound(SoundEvents.SHIELD_BLOCK,1.2F,0.6F);
            }
            else if(!onHead(source)){amount*=SHELL_SHARE;if(tickCount%4==0)sound(SoundEvents.SHIELD_BLOCK,0.8F,0.8F);}
        }
        return super.hurt(source,amount);
    }

    // ------------------------------------------------------------------ the shell
    void withdraw(ServerLevel level) {
        hide=HIDE_TICKS;crack=0;withdrawals++;entityData.set(STATE,1);act(5,HIDE_TICKS);getNavigation().stop();
        for(var skill:skills)skill.hold(40);
        for(var player:bar.getPlayers())player.displayClientMessage(Component.translatable("message.primordial.xuanwu.hide").withStyle(s->s.withColor(COLOUR).withBold(true)),false);
        SinParticles.send(level,SinParticles.FLARE.get(),COLOUR,position().add(0,2,0),30,2,0,0.05,0);
        sound(SoundEvents.SHULKER_CLOSE,2F,0.4F);
    }
    void breakShell() {
        hide=0;stun=STUN_TICKS;crack=0;entityData.set(STATE,2);act(6,STUN_TICKS);
        for(var player:bar.getPlayers())player.displayClientMessage(Component.translatable("message.primordial.xuanwu.cracked").withStyle(s->s.withColor(FROST).withBold(true)),false);
        if(level() instanceof ServerLevel level){
            SinParticles.send(level,SinParticles.SHARD.get(),FROST,position().add(0,2.5,0),60,2.5,0,0.2,0);
            level.sendParticles(new net.minecraft.core.particles.ItemParticleOption(ParticleTypes.ITEM,new ItemStack(net.minecraft.world.item.Items.BLUE_ICE)),getX(),getY()+2.5,getZ(),40,2,1,2,0.15);
        }
        sound(SoundEvents.GLASS_BREAK,3F,0.4F);sound(SoundEvents.TURTLE_EGG_CRACK,3F,0.5F);
    }
    void chooseGeysers(LivingEntity target) {
        geysers.clear();
        var seat=Vec3.atBottomCenterOf(home());
        if(level() instanceof ServerLevel level)for(var player:level.players())if(contender(player)&&player.position().distanceTo(seat)<forget())geysers.add(player.position());
        for(int i=0;i<3;i++)geysers.add(target.position().add((getRandom().nextDouble()-0.5)*10,0,(getRandom().nextDouble()-0.5)*10));
        act(4,34);
    }
    @Override protected void fight(ServerLevel level) {
        // withdrawals at three quarters, half and a quarter
        float share=getHealth()/getMaxHealth();
        if(hide==0&&stun==0&&withdrawals<3&&share<=0.75F-0.25F*withdrawals)withdraw(level);
        if(hide>0){
            getNavigation().stop();setDeltaMovement(0,getDeltaMovement().y,0);
            if(tickCount%20==0)heal(getMaxHealth()*0.01F);
            if(tickCount%5==0)SinParticles.ring(level,SinParticles.GLINT.get(),COLOUR,position(),getBbWidth()*0.7,14,0.05);
            if(--hide==0){entityData.set(STATE,0);sound(SoundEvents.SHULKER_OPEN,2F,0.4F);}
        }
        if(stun>0){getNavigation().stop();setDeltaMovement(0,getDeltaMovement().y,0);
            if(tickCount%6==0)level.sendParticles(ParticleTypes.CRIT,getX(),getY()+getBbHeight()+0.5,getZ(),6,1.2,0.2,1.2,0.1);
            if(--stun==0)entityData.set(STATE,0);}
        // the serpent bites, more often while the tortoise hides
        if(stun==0&&++bite>=(hide>0?BITE_INTERVAL/2:BITE_INTERVAL)){
            bite=0;var head=serpentHead();LivingEntity prey=null;double best=BITE_REACH*BITE_REACH;
            for(var e:foes(position(),BITE_REACH+getBbWidth())){double d=e.position().add(0,1,0).distanceToSqr(head.x,Math.min(head.y,e.getY()+1),head.z);if(d<best){best=d;prey=e;}}
            if(prey!=null){
                strike(prey,10);prey.addEffect(new MobEffectInstance(MobEffects.POISON,60,0),this);
                SinParticles.line(level,SinParticles.WISP.get(),COLOUR,head,prey.position().add(0,1,0),0.6,0);
                sound(SoundEvents.EVOKER_FANGS_ATTACK,1.2F,1.2F);
            }
        }
        // the frost tide rolling out
        if(tide>0){
            tide--;double radius=2+(TIDE_RADIUS-2)*(1-tide/(double)TIDE_TICKS);
            SinParticles.ringFar(level,SinParticles.FLARE.get(),FROST,tideCentre,radius,(int)(radius*5),0.02);
            for(var player:level.players()){
                if(!contender(player)||tideHit.contains(player.getUUID()))continue;
                double d=ArcaneSkill.horizontal(player.position(),tideCentre);
                if(Math.abs(d-radius)<1.1&&player.onGround()&&Math.abs(player.getY()-tideCentre.y)<3){
                    tideHit.add(player.getUUID());spell(player,10);
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,50,5),this);player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN,50,2),this);
                    player.setTicksFrozen(Math.max(player.getTicksFrozen(),200));
                    level.sendParticles(ParticleTypes.SNOWFLAKE,player.getX(),player.getY()+1,player.getZ(),20,0.4,0.6,0.4,0.02);
                }
            }
        }
    }
    @Override protected void onRest() {
        hide=0;stun=0;tide=0;withdrawals=0;crack=0;bite=0;entityData.set(STATE,0);geysers.clear();
    }
    @Override protected void dropCustomDeathLoot(DamageSource source,int looting,boolean hit) {
        super.dropCustomDeathLoot(source,looting,hit);
        if(!(level() instanceof ServerLevel level))return;
        spawnAtLocation(new ItemStack(SinBosses.XUANWU_SCUTE.get(),2+Math.min(2,looting)));
        spawnAtLocation(new ItemStack(SinBosses.XUANWU_SIGIL.get()));
        spawnAtLocation(new ItemStack(SinBosses.WATER_ESSENCE.get()));
        // the supreme charm recipe it carries, always
        {var item=new ItemEntity(level,getX(),getY()+1,getZ(),AlchemyRecipeItem.create(SinBosses.INVIOLABLE_RECIPE));item.setDefaultPickUpDelay();level.addFreshEntity(item);}
        for(int i=0;i<2;i++){var recipe=local.primordial.spirit.SpiritRealm.realmRecipe(level.getServer(),getRandom());
            if(recipe!=null){var item=new ItemEntity(level,getX(),getY()+1,getZ(),AlchemyRecipeItem.create(recipe));item.setDefaultPickUpDelay();level.addFreshEntity(item);}}
    }
    @Override protected void ambient() {
        var random=getRandom();
        if(dormant()&&random.nextInt(3)!=0)return;
        // mist and frost off its shell; drops of water off the serpent
        if(random.nextInt(2)==0)level().addParticle(SinParticles.FLARE.get().of(random.nextInt(3)==0?FROST:COLOUR),
            getX()+(random.nextDouble()-0.5)*getBbWidth(),getY()+getBbHeight()*(0.6+random.nextDouble()*0.4),getZ()+(random.nextDouble()-0.5)*getBbWidth(),0,0.02,0);
        if(random.nextInt(3)==0)level().addParticle(ParticleTypes.DRIPPING_WATER,getX()+(random.nextDouble()-0.5)*getBbWidth()*0.8,getY()+getBbHeight()*0.5,getZ()+(random.nextDouble()-0.5)*getBbWidth()*0.8,0,0,0);
        if(shellState()==1&&random.nextInt(2)==0)level().addParticle(SinParticles.GLINT.get().of(COLOUR),getX()+(random.nextDouble()-0.5)*getBbWidth(),getY()+random.nextDouble()*getBbHeight(),getZ()+(random.nextDouble()-0.5)*getBbWidth(),0,0.03,0);
    }
    @Override public boolean isPushable() {return false;}
    @Override public void addAdditionalSaveData(CompoundTag tag) {super.addAdditionalSaveData(tag);tag.putInt("Withdrawals",withdrawals);}
    @Override public void readAdditionalSaveData(CompoundTag tag) {super.readAdditionalSaveData(tag);withdrawals=tag.getInt("Withdrawals");}
}
