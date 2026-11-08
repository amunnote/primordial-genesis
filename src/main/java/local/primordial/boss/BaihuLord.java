package local.primordial.boss;

import java.util.ArrayList;
import java.util.List;
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
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 白虎 · 监兵 — the White Tiger of the West, guardian of metal: a great white tiger striped in black, steel plates on its
 * shoulders and brow, a ring of blades turning over its back. All attack and little armour.
 * <ul>
 * <li>杀意 Killing Intent: each blow it lands adds 6% to its damage (ten times, twenty once enraged); it ebbs when it
 *     stops landing them, and is lost whole when a pounce finds no one.</li>
 * <li>扑杀 Pounce: it crouches, marking where it will land, and leaps: 24 and thrown, then it pants a moment, open. Once
 *     enraged it chains three pounces.</li>
 * <li>裂空爪 Rending Claws: three red rakes ahead of it, 16 each and bleeding.</li>
 * <li>虎啸 Roar: a cone of fourteen blocks, weakening, slowing and throwing back.</li>
 * <li>金刃风暴 Blade Storm: eight blades rise over it, aim, and fly.</li>
 * <li>Fragile: it takes a fifth more harm, and half again while it pants.</li>
 * </ul>
 */
public class BaihuLord extends SinBoss {
    /** how much bigger it is drawn than its model, and how far that reaches past its hitbox (for culling) */
    public static final float MODEL_SCALE=1.6F;
    @Override public double modelReach() {return 8;}
    public static final double HEALTH=1400;
    public static final int COLOUR=0xF4F4F0,STRIPE=0x2A2A30,STEEL=0xB8D8FF;
    public static final int MAX_INTENT=10,ENRAGED_INTENT=20,LEAP_TICKS=14,PANT=26;
    public static final float INTENT_BONUS=0.06F,FRAGILE=1.2F,PANT_BONUS=1.5F;
    static final EntityDataAccessor<Integer> INTENT=SynchedEntityData.defineId(BaihuLord.class,EntityDataSerializers.INT);
    static final UUID FURY=UUID.fromString("e4b1c2d3-6a7b-4c8d-9e0f-1a2b3c4d5e61");
    private final List<TreasuryGate> blades=new ArrayList<>();
    private Vec3 leapFrom,leapTo;private int leapTicks,chains,pant,sinceHit,chainDelay;private boolean leapLanded;

    public BaihuLord(EntityType<? extends BaihuLord> type,Level level) {
        super(type,level);
        // 裂空爪
        skills.add(new ArcaneSkill(70,10,0,6,false,(mob,target)->target.position(),
            (level1,mob,target,mark,left)->{getNavigation().stop();getLookControl().setLookAt(target);if(left%2==0)for(var line:rakes())SinParticles.line(level1,SinParticles.EMBER.get(),0xFF3A3A,line[0],line[1],0.6,0);},
            (level1,mob,target,mark)->{
                act(2,14);
                var hit=new java.util.HashSet<UUID>();
                for(var line:rakes()){
                    for(var e:foes(line[0].lerp(line[1],0.5),4.5))if(nearLine(e.position(),line[0],line[1],1.0)&&hit.add(e.getUUID())){
                        if(strike(e,16*intent()))landed();e.addEffect(new MobEffectInstance(MobEffects.WITHER,60,0),this);}
                    SinParticles.line(level1,SinParticles.SPARK.get(),0xFFFFFF,line[0],line[1],0.4,0.02);
                }
                sound(SoundEvents.PLAYER_ATTACK_SWEEP,2F,0.6F);sound(SoundEvents.RAVAGER_ATTACK,1.2F,1.4F);
            }).when(m->leapTicks==0&&pant==0));
        // 扑杀
        skills.add(new ArcaneSkill(100,14,4,22,true,(mob,target)->target.position(),
            (level1,mob,target,mark,left)->{getNavigation().stop();getLookControl().setLookAt(target);act(1,left+2);
                if(left%3==0)SinParticles.ring(level1,SinParticles.GLINT.get(),0xFF4A4A,mark,3.5,20,0.03);},
            (level1,mob,target,mark)->{chains=enraged()?2:0;leap(mark);}).when(m->leapTicks==0&&pant==0));
        // 虎啸
        skills.add(new ArcaneSkill(260,20,0,14,false,(mob,target)->position(),
            (level1,mob,target,mark,left)->{getNavigation().stop();getLookControl().setLookAt(target);
                SinParticles.send(level1,SinParticles.FLARE.get(),STEEL,front(2.6).add(0,2*MODEL_SCALE,0),2,1.5,0,-0.02,0);},
            (level1,mob,target,mark)->{
                act(3,26);
                var facing=front(1).subtract(position()).normalize();
                for(var e:foes(position(),14)){var to=e.position().subtract(position()).multiply(1,0,1);
                    if(to.lengthSqr()<1e-3||to.normalize().dot(facing)<Math.cos(Math.toRadians(50)))continue;
                    spell(e,10);e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,100,1),this);e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,60,1),this);
                    ArcaneSkill.knock(e,position(),1.6,0.4);}
                SinShockwave.spread(level1,front(2).add(0,2*MODEL_SCALE,0),9F,14,STEEL,true,getYRot());SinShockwave.spread(level1,front(2).add(0,2*MODEL_SCALE,0),5F,10,0xFFFFFF,true,getYRot());
                sound(SoundEvents.RAVAGER_ROAR,3F,1.3F);sound(SoundEvents.ENDER_DRAGON_GROWL,1.2F,1.8F);
            }).when(m->leapTicks==0&&pant==0));
        // 金刃风暴
        skills.add(new ArcaneSkill(200,30,0,30,true,(mob,target)->{openBlades(target);return target.position();},
            (level1,mob,target,mark,left)->{for(var blade:blades)blade.aim(PrideLord.aimPoint(blade.position(),target));},
            (level1,mob,target,mark)->fireBlades(level1,target)).when(m->leapTicks==0&&pant==0));
    }
    @Override protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(3,new MeleeAttackGoal(this,1.15,true){
            @Override public boolean canUse() {return leapTicks==0&&pant==0&&super.canUse();}
            @Override public boolean canContinueToUse() {return leapTicks==0&&pant==0&&super.canContinueToUse();}
        });
    }
    @Override protected void defineSynchedData() {super.defineSynchedData();entityData.define(INTENT,0);}
    @Override public BossAffix sin() {return null;}
    @Override public int colour() {return COLOUR;}
    @Override public String bossId() {return "baihu";}
    @Override protected BarLook barLook() {return new BarLook(pant>0?2:0,killingIntent(),killingIntent()/(float)(enraged()?ENRAGED_INTENT:MAX_INTENT),0);}
    @Override protected BossEvent.BossBarColor barColour() {return BossEvent.BossBarColor.WHITE;}
    @Override protected String[] actions() {return new String[]{"","crouch","claws","roar","storm","leap","pant"};}
    @Override protected SoundEvent wakeSound() {return SoundEvents.RAVAGER_ROAR;}
    @Override public int experience() {return 1000;}
    @Override protected double wakeRadius() {return 22;}
    @Override protected double leash() {return 34;}
    @Override protected double forget() {return 52;}

    public int killingIntent() {return entityData.get(INTENT);}
    float intent() {return 1+killingIntent()*INTENT_BONUS;}
    public boolean panting() {return pant>0;}
    void landed() {
        sinceHit=0;
        entityData.set(INTENT,Math.min(enraged()?ENRAGED_INTENT:MAX_INTENT,killingIntent()+1));
    }
    /** a point this far ahead of it, in model blocks (grown with the model) */
    Vec3 front(double reach) {double yaw=Math.toRadians(yBodyRot);reach*=MODEL_SCALE;return position().add(-Math.sin(yaw)*reach,0,Math.cos(yaw)*reach);}
    /** its three rakes: lines reaching eight blocks ahead, side by side */
    List<Vec3[]> rakes() {
        double yaw=Math.toRadians(yBodyRot);Vec3 ahead=new Vec3(-Math.sin(yaw),0,Math.cos(yaw)),side=new Vec3(Math.cos(yaw),0,Math.sin(yaw));
        var out=new ArrayList<Vec3[]>();
        for(int k=-1;k<=1;k++){var from=position().add(side.scale(k*1.6)).add(ahead.scale(1.2)).add(0,0.4,0);out.add(new Vec3[]{from,from.add(ahead.scale(8)).add(side.scale(k*0.8))});}
        return out;
    }
    static boolean nearLine(Vec3 p,Vec3 a,Vec3 b,double width) {
        var ab=b.subtract(a).multiply(1,0,1);double t=Math.max(0,Math.min(1,p.subtract(a).multiply(1,0,1).dot(ab)/ab.lengthSqr()));
        return ArcaneSkill.horizontal(a.add(ab.scale(t)),p)<=width;
    }
    @Override public boolean doHurtTarget(Entity target) {
        boolean hit=super.doHurtTarget(target);
        if(hit){landed();if(target instanceof LivingEntity living)living.hurt(ArcaneSkill.magic(this),(float)(getAttributeValue(Attributes.ATTACK_DAMAGE)*0.25*intent()));}
        return hit;
    }
    @Override public boolean hurt(DamageSource source,float amount) {
        if(!dormant())amount*=FRAGILE*(pant>0?PANT_BONUS:1);
        return super.hurt(source,amount);
    }

    // ------------------------------------------------------------------ the pounce
    void leap(Vec3 to) {
        leapFrom=position();leapTo=to;leapTicks=LEAP_TICKS;leapLanded=false;act(5,LEAP_TICKS+2);
        setYRot((float)Math.toDegrees(Math.atan2(to.z-getZ(),to.x-getX()))-90);yBodyRot=getYRot();
        sound(SoundEvents.RAVAGER_ROAR,1.5F,1.6F);
    }
    void leaping(ServerLevel level) {
        getNavigation().stop();
        int done=LEAP_TICKS-leapTicks+1;double t=done/(double)LEAP_TICKS;
        var want=leapFrom.lerp(leapTo,t).add(0,Math.sin(t*Math.PI)*4.5,0);
        move(MoverType.SELF,want.subtract(position()));
        SinParticles.send(level,SinParticles.GLINT.get(),STEEL,position().add(0,1.3,0),2,0.8,0,0,0);
        if(--leapTicks>0)return;
        // landing: whoever is under it
        boolean any=false;
        for(var e:foes(position(),3.5)){if(strike(e,24*intent())){any=true;landed();}ArcaneSkill.knock(e,position(),1.4,0.6);}
        SinShockwave.spread(level,position(),5F,12,COLOUR,false,0);SinShockwave.spread(level,position(),3F,8,STEEL,false,0);
        SinParticles.send(level,SinParticles.SPARK.get(),0xFFFFFF,position().add(0,0.3,0),30,1.5,0,0.25,0);
        sound(SoundEvents.GENERIC_EXPLODE,1F,1.4F);
        var target=getTarget();
        if(chains>0&&target!=null&&target.isAlive()){chains--;chainDelay=6;}
        else{
            pant=any?PANT/2:PANT;act(6,pant);
            if(!any&&killingIntent()>0){entityData.set(INTENT,0);for(var player:bar.getPlayers())player.displayClientMessage(Component.translatable("message.primordial.baihu.miss").withStyle(s->s.withColor(STEEL)),true);}
        }
    }

    // ------------------------------------------------------------------ the blade storm
    void openBlades(LivingEntity target) {
        PrideLord.closeAll(blades);
        for(int i=0;i<8;i++){double a=Math.PI*2*i/8;
            var at=position().add(Math.cos(a)*3.2,4.5+(i%2)*0.8,Math.sin(a)*3.2);
            blades.add(TreasuryGate.open(level(),at,PrideLord.aimPoint(at,target),new ItemStack(i%4==0?Items.NETHERITE_SWORD:Items.IRON_SWORD),30,0.8F));}
        act(4,34);sound(SoundEvents.ARMOR_EQUIP_NETHERITE,2F,0.6F);
    }
    void fireBlades(ServerLevel level,LivingEntity target) {
        for(var blade:blades){
            if(blade.isRemoved())continue;
            var from=blade.position();
            TreasureWeapon.loose(level,this,blade.weapon().isEmpty()?new ItemStack(Items.IRON_SWORD):blade.weapon(),from,PrideLord.aimPoint(from,target),1.9,10*intent(),STEEL);
            blade.fire();
        }
        blades.clear();
        sound(SoundEvents.TRIDENT_THROW,2F,1.2F);
    }

    @Override protected void fight(ServerLevel level) {
        if(chainDelay>0&&--chainDelay==0){var target=getTarget();if(target!=null)leap(target.position());}
        if(leapTicks>0){leaping(level);return;}
        if(pant>0){pant--;getNavigation().stop();setDeltaMovement(0,getDeltaMovement().y,0);
            if(pant%4==0)level.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD,front(2).x,getY()+1.6*MODEL_SCALE,front(2).z,2,0.2,0.1,0.2,0.01);}
        // killing intent ebbs when it lands nothing for five seconds
        if(++sinceHit>100&&sinceHit%20==0&&killingIntent()>0)entityData.set(INTENT,killingIntent()-1);
    }
    @Override protected void onEnrage(ServerLevel level) {
        var speed=getAttribute(Attributes.MOVEMENT_SPEED);speed.removeModifier(FURY);
        speed.addTransientModifier(new AttributeModifier(FURY,"baihu fury",0.25,AttributeModifier.Operation.MULTIPLY_BASE));
        for(var player:bar.getPlayers())player.displayClientMessage(Component.translatable("message.primordial.baihu.enrage").withStyle(s->s.withColor(COLOUR).withBold(true)),false);
    }
    @Override protected void onRest() {
        PrideLord.closeAll(blades);leapTicks=0;pant=0;chains=0;chainDelay=0;entityData.set(INTENT,0);
        getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(FURY);
    }
    @Override public void die(DamageSource source) {super.die(source);if(isDeadOrDying())PrideLord.closeAll(blades);}
    @Override protected void dropCustomDeathLoot(DamageSource source,int looting,boolean hit) {
        super.dropCustomDeathLoot(source,looting,hit);
        if(!(level() instanceof ServerLevel level))return;
        spawnAtLocation(new ItemStack(SinBosses.BAIHU_FANG.get(),2+Math.min(2,looting)));
        spawnAtLocation(new ItemStack(SinBosses.BAIHU_SIGIL.get()));
        spawnAtLocation(new ItemStack(SinBosses.METAL_ESSENCE.get()));
        // the supreme charm recipe it carries, always
        {var item=new ItemEntity(level,getX(),getY()+1,getZ(),AlchemyRecipeItem.create(SinBosses.CLEAR_WORLD_RECIPE));item.setDefaultPickUpDelay();level.addFreshEntity(item);}
        for(int i=0;i<2;i++){var recipe=local.primordial.spirit.SpiritRealm.realmRecipe(level.getServer(),getRandom());
            if(recipe!=null){var item=new ItemEntity(level,getX(),getY()+1,getZ(),AlchemyRecipeItem.create(recipe));item.setDefaultPickUpDelay();level.addFreshEntity(item);}}
    }
    @Override protected void ambient() {
        var random=getRandom();
        if(dormant()&&random.nextInt(3)!=0)return;
        // steel motes off its blades, and the more it has killed the redder its breath
        if(random.nextInt(2)==0)level().addParticle(SinParticles.GLINT.get().of(STEEL),getX()+(random.nextDouble()-0.5)*3,getY()+2.4+random.nextDouble()*1.2,getZ()+(random.nextDouble()-0.5)*3,0,0.01,0);
        int intent=killingIntent();
        if(intent>0&&random.nextInt(Math.max(1,12-intent/2))==0){double yaw=Math.toRadians(yBodyRot);
            level().addParticle(SinParticles.EMBER.get().of(intent>=10?0xFF2A2A:0xFF8A8A),getX()-Math.sin(yaw)*2.4,getY()+1.8,getZ()+Math.cos(yaw)*2.4,(random.nextDouble()-0.5)*0.05,0.03,(random.nextDouble()-0.5)*0.05);}
    }
    @Override public void addAdditionalSaveData(CompoundTag tag) {super.addAdditionalSaveData(tag);tag.putInt("Intent",killingIntent());}
    @Override public void readAdditionalSaveData(CompoundTag tag) {super.readAdditionalSaveData(tag);entityData.set(INTENT,tag.getInt("Intent"));}
}
