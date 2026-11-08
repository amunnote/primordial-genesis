package local.primordial.entity;

import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * A clone called by Summon's accessory: a see-through copy of its owner with 30% of four of its numbers (max health,
 * attack, movement, armor), taken when it is called. It carries none of the owner's affixes or bonds and none of its
 * gear or gear skills; it fights what its owner fights and follows it, and lasts until it dies. It counts as a summon
 * ({@link #isSummon}), so whatever reads summons reads it; it never calls clones of its own.
 * Its owner is synced so the client can wear the owner's skin (SummonCloneRenderer).
 */
public class SummonClone extends PathfinderMob implements OwnableEntity {
    private static final EntityDataAccessor<Optional<UUID>> OWNER=SynchedEntityData.defineId(SummonClone.class,EntityDataSerializers.OPTIONAL_UUID);
    /** the share of the owner's numbers */
    public static final double SHARE=0.30;
    /** 点化: the weak blessing it was given (amplify / regeneration / strength), if any */
    public static final String BLESSING="primordial:clone_blessing";
    private static final java.util.UUID BLESSING_MODIFIER=java.util.UUID.fromString("5e2b7c1a-9f34-4d8e-a1b6-0c7d2e9f4a31");
    private int ownerMissing;
    public SummonClone(EntityType<? extends SummonClone> type,Level level) {super(type,level);xpReward=0;setPersistenceRequired();}
    public static AttributeSupplier.Builder attributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH,6).add(Attributes.ATTACK_DAMAGE,1)
            .add(Attributes.MOVEMENT_SPEED,0.3).add(Attributes.ARMOR,0).add(Attributes.FOLLOW_RANGE,32);
    }
    public static boolean isSummon(Entity entity) {return entity instanceof SummonClone;}
    /** is this one of this player's clones */
    public static boolean ownedBy(Entity entity,Player player) {return entity instanceof SummonClone clone&&player.getUUID().equals(clone.getOwnerUUID());}

    @Override protected void defineSynchedData() {super.defineSynchedData();entityData.define(OWNER,Optional.empty());}
    @Override public UUID getOwnerUUID() {return entityData.get(OWNER).orElse(null);}
    @Override public LivingEntity getOwner() {
        var id=getOwnerUUID();return id==null?null:level().getPlayerByUUID(id);
    }
    /**
     * Copies the owner's numbers, 30% of each. Movement is taken as 30% of how fast the owner really goes: a walking
     * mob covers ground with the square of its speed attribute where a player covers it with the attribute itself, so the
     * attribute that moves a clone at 30% of its owner's pace is sqrt(0.3 x 0.98 x the owner's).
     */
    public void copyFrom(Player owner) {
        entityData.set(OWNER,Optional.of(owner.getUUID()));
        // the mod's own mark for a summoned creature (as AffixMobActions gives Summon's minions)
        getPersistentData().putBoolean("primordial:summoned",true);
        setBase(Attributes.MAX_HEALTH,Math.max(1,owner.getMaxHealth()*SHARE));
        setBase(Attributes.ATTACK_DAMAGE,owner.getAttributeValue(Attributes.ATTACK_DAMAGE)*SHARE);
        setBase(Attributes.MOVEMENT_SPEED,Math.sqrt(SHARE*0.98*owner.getAttributeValue(Attributes.MOVEMENT_SPEED)));
        setBase(Attributes.ARMOR,owner.getAttributeValue(Attributes.ARMOR)*SHARE);
        setHealth(getMaxHealth());
    }
    private void setBase(net.minecraft.world.entity.ai.attributes.Attribute attribute,double value) {
        var instance=getAttribute(attribute);if(instance!=null)instance.setBaseValue(value);
    }
    /** 点化: a weak copy of one blessing (amplify: +20% attack; regeneration: 0.5 health and 1% a second; strength: +2 attack) */
    public void bless(String blessing) {
        getPersistentData().putString(BLESSING,blessing);
        var attack=getAttribute(Attributes.ATTACK_DAMAGE);if(attack==null)return;
        attack.removeModifier(BLESSING_MODIFIER);
        switch(blessing){
            case "amplify"->attack.addPermanentModifier(new AttributeModifier(BLESSING_MODIFIER,"primordial.clone_amplify",0.2,AttributeModifier.Operation.MULTIPLY_TOTAL));
            case "strength"->attack.addPermanentModifier(new AttributeModifier(BLESSING_MODIFIER,"primordial.clone_strength",2,AttributeModifier.Operation.ADDITION));
            default->{}
        }
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(0,new FloatGoal(this));
        goalSelector.addGoal(2,new MeleeAttackGoal(this,1.2,true));
        goalSelector.addGoal(3,new FollowOwner(this));
        goalSelector.addGoal(6,new LookAtPlayerGoal(this,Player.class,8));
        goalSelector.addGoal(7,new RandomLookAroundGoal(this));
    }
    /** what it may fight: never its owner, a player, another clone or its owner's pets */
    private boolean foe(LivingEntity entity,Player owner) {
        return entity!=null&&entity.isAlive()&&entity!=owner&&!(entity instanceof Player)&&!(entity instanceof SummonClone)
            &&!(entity instanceof OwnableEntity pet&&owner.getUUID().equals(pet.getOwnerUUID()))&&distanceToSqr(entity)<=32*32;
    }
    @Override protected void customServerAiStep() {
        super.customServerAiStep();
        if(tickCount%10!=0)return;
        var owner=getOwner();
        // its owner gone (left, or in another world) for thirty seconds: it fades without dying
        if(!(owner instanceof Player player)||!player.isAlive()){if((ownerMissing+=10)>=600)fade();return;}
        ownerMissing=0;
        if(getPersistentData().getString(BLESSING).equals("regeneration")&&tickCount%20==0)heal(0.5F+getMaxHealth()*0.01F);
        if(foe(getTarget(),player))return;
        LivingEntity next=null;
        if(foe(player.getLastHurtByMob(),player)&&player.tickCount-player.getLastHurtByMobTimestamp()<200)next=player.getLastHurtByMob();
        else if(foe(player.getLastHurtMob(),player)&&player.tickCount-player.getLastHurtMobTimestamp()<200)next=player.getLastHurtMob();
        setTarget(next);
    }
    public void fade() {
        if(level() instanceof ServerLevel level)level.sendParticles(ParticleTypes.SOUL,getX(),getY(0.5),getZ(),12,0.3,0.4,0.3,0.03);
        discard();
    }
    @Override public boolean hurt(DamageSource source,float amount) {
        // its owner's own blows and shots pass through it
        var owner=getOwnerUUID();
        if(owner!=null&&source.getEntity()!=null&&owner.equals(source.getEntity().getUUID()))return false;
        return super.hurt(source,amount);
    }
    @Override public boolean isAlliedTo(Entity other) {
        var owner=getOwnerUUID();
        if(owner!=null&&(owner.equals(other.getUUID())||other instanceof SummonClone clone&&owner.equals(clone.getOwnerUUID())))return true;
        return super.isAlliedTo(other);
    }
    @Override public void die(DamageSource source) {
        super.die(source);
        if(!level().isClientSide&&getOwner() instanceof Player owner)local.primordial.effect.AffixAccessoryEvents.cloneDied(this,owner);
    }
    @Override protected boolean shouldDropLoot() {return false;}
    @Override public boolean removeWhenFarAway(double distance) {return false;}
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if(getOwnerUUID()!=null)tag.putUUID("Owner",getOwnerUUID());
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if(tag.hasUUID("Owner"))entityData.set(OWNER,Optional.of(tag.getUUID("Owner")));
    }

    /** keeps near its owner when it has nothing to fight; too far behind, it steps to the owner's side */
    static final class FollowOwner extends Goal {
        private final SummonClone clone;
        FollowOwner(SummonClone clone) {this.clone=clone;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
        private Player owner() {return clone.getOwner() instanceof Player player&&player.isAlive()&&!player.isSpectator()?player:null;}
        @Override public boolean canUse() {var owner=owner();return owner!=null&&clone.getTarget()==null&&clone.distanceToSqr(owner)>36;}
        @Override public boolean canContinueToUse() {var owner=owner();return owner!=null&&clone.getTarget()==null&&clone.distanceToSqr(owner)>9;}
        @Override public void stop() {clone.getNavigation().stop();}
        @Override public void tick() {
            var owner=owner();if(owner==null)return;
            clone.getLookControl().setLookAt(owner,10,clone.getMaxHeadXRot());
            if(clone.distanceToSqr(owner)>16*16){
                clone.moveTo(owner.getX()+clone.getRandom().nextGaussian(),owner.getY(),owner.getZ()+clone.getRandom().nextGaussian(),clone.getYRot(),clone.getXRot());
                if(!clone.level().noCollision(clone))clone.moveTo(owner.getX(),owner.getY(),owner.getZ(),clone.getYRot(),clone.getXRot());
                clone.getNavigation().stop();
            }else if(clone.tickCount%10==0)clone.getNavigation().moveTo(owner,1.4);
        }
    }
}
