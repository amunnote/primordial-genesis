package local.primordial.qinglong;

import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;

/** A flying valley patrol; chained arcs never target allies or its own species. */
public class ThunderJiao extends Monster implements GeoEntity {
    private final AnimatableInstanceCache cache=software.bernie.geckolib.util.GeckoLibUtil.createInstanceCache(this);
    private int arcCooldown=60;
    public ThunderJiao(EntityType<? extends ThunderJiao> type,Level l){super(type,l);setNoGravity(true);xpReward=8;}
    public static AttributeSupplier.Builder attributes(){return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,40).add(Attributes.ATTACK_DAMAGE,6).add(Attributes.MOVEMENT_SPEED,0.3).add(Attributes.FOLLOW_RANGE,32);}
    @Override protected void registerGoals(){targetSelector.addGoal(1,new HurtByTargetGoal(this));targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Player.class,true));}
    @Override public boolean causeFallDamage(float d,float m,net.minecraft.world.damagesource.DamageSource s){return false;}
    @Override protected void customServerAiStep(){
        super.customServerAiStep();setNoGravity(true);
        var target=getTarget();Vec3 wanted;
        if(target!=null&&target.isAlive()){
            double a=tickCount*0.025+getId();wanted=target.position().add(Math.cos(a)*9,6,Math.sin(a)*9);
            if(--arcCooldown<=0&&distanceToSqr(target)<32*32&&hasLineOfSight(target)){arcCooldown=80;swing(net.minecraft.world.InteractionHand.MAIN_HAND);arc(target);}
        }else{
            var ground=level().getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,blockPosition());
            double a=tickCount*0.025+getId();wanted=new Vec3(getX()+Math.cos(a)*6,ground.getY()+8,getZ()+Math.sin(a)*6);
        }
        Vec3 delta=wanted.subtract(position());if(delta.lengthSqr()>1)delta=delta.normalize().scale(0.22);
        if(horizontalCollision)delta=delta.add(0,0.2,0);
        setDeltaMovement(getDeltaMovement().scale(0.8).add(delta.scale(0.2)));
        if(target!=null)getLookControl().setLookAt(target,30,30);
        setYRot((float)(Math.atan2(-getDeltaMovement().x,getDeltaMovement().z)*180/Math.PI));yBodyRot=getYRot();
    }
    private void arc(LivingEntity first){
        if(!(level() instanceof net.minecraft.server.level.ServerLevel server))return;
        var used=new java.util.HashSet<LivingEntity>();LivingEntity next=first;Vec3 from=getEyePosition();
        for(int i=0;i<3&&next!=null;i++){
            var victim=next;used.add(victim);boolean metal=false;
            for(var armor:victim.getArmorSlots())if(armor.getItem() instanceof net.minecraft.world.item.ArmorItem a){var m=a.getMaterial();if(m==net.minecraft.world.item.ArmorMaterials.IRON||m==net.minecraft.world.item.ArmorMaterials.CHAIN||m==net.minecraft.world.item.ArmorMaterials.GOLD||m==net.minecraft.world.item.ArmorMaterials.NETHERITE)metal=true;}
            victim.hurt(damageSources().indirectMagic(this,this),metal?7.8F:6F);
            Vec3 end=victim.getEyePosition(),line=end.subtract(from);int count=Math.max(2,(int)(line.length()*3));
            for(int n=0;n<=count;n++){var p=from.add(line.scale(n/(double)count));server.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,p.x,p.y,p.z,1,0,0,0,0);}
            from=end;
            next=server.getEntitiesOfClass(LivingEntity.class,victim.getBoundingBox().inflate(7),e->e!=this&&!(e instanceof ThunderJiao)&&e.isAlive()&&!used.contains(e)&&!isAlliedTo(e)&&!(e instanceof Player p&&(p.isCreative()||p.isSpectator()))&&victim.hasLineOfSight(e)).stream().min(java.util.Comparator.comparingDouble(e->e.distanceToSqr(victim))).orElse(null);
        }
        playSound(net.minecraft.sounds.SoundEvents.LIGHTNING_BOLT_IMPACT,0.6F,1.7F);
    }
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar c){c.add(new AnimationController<>(this,"body",3,s->s.setAndContinue(RawAnimation.begin().thenLoop("animation.thunder_jiao."+(hurtTime>0?"hurt":swinging?"attack":getDeltaMovement().lengthSqr()>0.002?"fly":"idle")))));}
    @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
}
