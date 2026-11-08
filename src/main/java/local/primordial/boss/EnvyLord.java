package local.primordial.boss;

import java.util.ArrayList;
import java.util.List;
import local.primordial.affix.BossAffix;
import local.primordial.affix.Imbuing;
import local.primordial.affix.ItemSeal;
import local.primordial.entity.alchemical.ArcaneSkill;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 嫉妒之主 · 无貌之影 — Envy, the Assassin. A faceless shadow in a ragged cloak that covets whatever you have.
 * <ul>
 * <li>影遁 Shadow Step: it melts away, unseen for two and a half seconds (only a faint wisp of shadow gives it away), then
 *     appears behind you and stabs for 16, blinding.</li>
 * <li>妒影分身 Envious Shades: three shadows of itself rise round you (fragile, but they hurt), at most four at once.</li>
 * <li>夺形 Usurp: it copies the weapon in your hand, wielding it as its own (its blows then hit as hard as that weapon,
 *     plus four), and seals yours for five seconds.</li>
 * </ul>
 */
public class EnvyLord extends SinBoss {
    public static final int VANISH_TICKS=50,SHADES=3,MAX_SHADES=4;public static final float BACKSTAB=16;
    /** the dark of its shadow, and the pale green of its mirrors */
    static final int SHADOW=0x18261E,MIRROR=0xBFF5DC;
    /** mirror shards flung out in a ring round a spot */
    static void shards(ServerLevel level,Vec3 at,int count,double speed) {
        for(int i=0;i<count;i++){double a=Math.PI*2*i/count;
            level.sendParticles(SinParticles.SHARD.get().of(MIRROR),at.x,at.y,at.z,0,Math.cos(a)*speed,0.05+level.random.nextDouble()*0.1,Math.sin(a)*speed,1);}
    }
    private int vanish;private LivingEntity quarry;

    public EnvyLord(EntityType<? extends EnvyLord> type,Level level) {
        super(type,level);
        skills.add(new ArcaneSkill(160,8,3,20,false,(mob,target)->target.position(),
            (level1,mob,target,mark,left)->SinParticles.send(level1,SinParticles.WISP.get(),SHADOW,position().add(0,1,0),3,0.5,0,0.02,0),
            (level1,mob,target,mark)->{
                act(1,VANISH_TICKS);vanish=VANISH_TICKS;quarry=target;setInvisible(true);
                SinParticles.send(level1,SinParticles.WISP.get(),SHADOW,position().add(0,1,0),30,0.7,0,0.04,0);
                shards(level1,position().add(0,1.2,0),14,0.25);
                sound(SoundEvents.ILLUSIONER_MIRROR_MOVE,1.5F,0.6F);
            }));
        skills.add(new ArcaneSkill(260,20,0,24,false,(mob,target)->target.position(),
            (level1,mob,target,mark,left)->{if(left%2==0)SinParticles.ring(level1,SinParticles.WISP.get(),SHADOW,mark,3,12,0.03);},
            (level1,mob,target,mark)->{
                act(2,20);
                int alive=shades().size();
                for(int i=0;i<SHADES&&alive<MAX_SHADES;i++,alive++){
                    double a=Math.PI*2*i/SHADES+getRandom().nextDouble();
                    var shade=new EnvyShade(SinBosses.ENVY_SHADE.get(),level1);
                    shade.moveTo(mark.x+Math.cos(a)*3,mark.y,mark.z+Math.sin(a)*3,getRandom().nextFloat()*360,0);
                    shade.setTarget(target);shade.setItemSlot(EquipmentSlot.MAINHAND,getMainHandItem().copy());
                    level1.addFreshEntity(shade);
                    SinParticles.send(level1,SinParticles.WISP.get(),SHADOW,shade.position().add(0,1,0),16,0.4,0,0.06,0);
                    shards(level1,shade.position().add(0,1.4,0),6,0.15);
                }
                sound(SoundEvents.ILLUSIONER_PREPARE_MIRROR,2F,0.7F);
            }));
        skills.add(new ArcaneSkill(400,12,0,8,false,(mob,target)->target.position(),
            (level1,mob,target,mark,left)->SinParticles.line(level1,SinParticles.SHARD.get(),MIRROR,target.position().add(0,1,0),position().add(0,1.2,0),0.6,0),
            (level1,mob,target,mark)->usurp(target)));
    }
    @Override protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(3,new MeleeAttackGoal(this,1.2,true));
    }
    @Override public BossAffix sin() {return BossAffix.ENVY;}
    @Override public String bossId() {return "envy_lord";}
    @Override protected BarLook barLook() {return new BarLook(vanish>0?2:0,0,-1,0);}
    @Override protected BossEvent.BossBarColor barColour() {return BossEvent.BossBarColor.GREEN;}
    @Override protected String[] actions() {return new String[]{"","vanish","summon","usurp"};}
    @Override protected SoundEvent wakeSound() {return SoundEvents.ILLUSIONER_PREPARE_MIRROR;}

    /** copy the weapon in its target's hand, and seal the original */
    void usurp(LivingEntity target) {
        var held=target.getMainHandItem();
        if(!Imbuing.isWeapon(held))return;
        act(3,15);
        var copy=held.copyWithCount(1);
        setItemSlot(EquipmentSlot.MAINHAND,copy);setDropChance(EquipmentSlot.MAINHAND,0);
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(Math.max(10,TreasureWeapon.damageOf(copy)+4));
        ItemSeal.apply(target);
        if(target instanceof Player player)player.displayClientMessage(Component.translatable("message.primordial.envy_lord.usurp",copy.getHoverName()).withStyle(s->s.withColor(colour())),true);
        sound(SoundEvents.ILLUSIONER_MIRROR_MOVE,2F,1.2F);
    }
    public List<EnvyShade> shades() {
        return new ArrayList<>(level().getEntitiesOfClass(EnvyShade.class,getBoundingBox().inflate(48),EnvyShade::isAlive));
    }
    public boolean vanished() {return vanish>0;}
    /** unseen, it holds its blade for the backstab */
    @Override public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {return vanish<=0&&super.doHurtTarget(target);}

    @Override protected void fight(ServerLevel level) {
        if(vanish<=0)return;
        vanish--;
        var target=quarry;
        if(target==null||!target.isAlive()){vanish=0;setInvisible(false);return;}
        // stalking unseen: only a faint wisp now and then
        if(vanish%6==0)SinParticles.send(level,SinParticles.WISP.get(),SHADOW,position().add(0,0.4,0),1,0.2,0,0.01,0);
        var behind=target.position().subtract(target.getLookAngle().multiply(1,0,1).normalize().scale(1.4));
        getNavigation().moveTo(behind.x,behind.y,behind.z,1.4);
        if(vanish==0){
            setInvisible(false);
            if(level.noCollision(this,getDimensions(getPose()).makeBoundingBox(behind)))teleportTo(behind.x,behind.y,behind.z);
            lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,target.getEyePosition());
            swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            if(distanceTo(target)<3.5){strike(target,BACKSTAB);target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,40,0),this);}
            SinParticles.send(level,SinParticles.WISP.get(),SHADOW,position().add(0,1,0),26,0.6,0,0.05,0);
            shards(level,position().add(0,1.3,0),18,0.35);
            SinShockwave.spread(level,position(),3.5F,10,0x43C982,false,0);
            sound(SoundEvents.PLAYER_ATTACK_CRIT,1.5F,0.6F);
        }
    }
    @Override protected void onRest() {
        vanish=0;setInvisible(false);setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(10);
        for(var shade:shades())shade.discard();
    }
    @Override public void die(net.minecraft.world.damagesource.DamageSource source) {super.die(source);for(var shade:shades())shade.dissolve();}
    @Override protected void ambient() {
        if(isInvisible()||getRandom().nextInt(2)!=0)return;
        if(getRandom().nextInt(6)==0){double a=getRandom().nextDouble()*Math.PI*2;
            level().addParticle(SinParticles.SHARD.get().of(MIRROR),getX()+Math.cos(a)*0.9,getY()+1.2+getRandom().nextDouble()*0.6,getZ()+Math.sin(a)*0.9,0,0.01,0);}
        level().addParticle(SinParticles.WISP.get().of(SHADOW),getX()+(getRandom().nextDouble()-0.5),getY()+getRandom().nextDouble()*0.6,getZ()+(getRandom().nextDouble()-0.5),0,0.01,0);
    }
}
