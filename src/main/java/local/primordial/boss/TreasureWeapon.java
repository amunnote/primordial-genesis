package local.primordial.boss;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * A weapon hurled point first from a Sin Lord's treasury: any weapon in the game, this mod's or any other's (whatever
 * {@link local.primordial.affix.Imbuing#isWeapon} counts as one, bows aside). It flies straight, hurts what it strikes by
 * the weapon's own attack damage, sticks quivering where it lands and fades a moment later; nothing to pick up.
 */
public class TreasureWeapon extends Projectile {
    static final EntityDataAccessor<ItemStack> ITEM=SynchedEntityData.defineId(TreasureWeapon.class,EntityDataSerializers.ITEM_STACK);
    static final EntityDataAccessor<Integer> COLOUR=SynchedEntityData.defineId(TreasureWeapon.class,EntityDataSerializers.INT);
    static final EntityDataAccessor<Boolean> STUCK=SynchedEntityData.defineId(TreasureWeapon.class,EntityDataSerializers.BOOLEAN);
    public static final float MIN_DAMAGE=5,MAX_DAMAGE=20;
    private float damage=8;private int life,stuckTicks;private boolean devours;

    public TreasureWeapon(EntityType<? extends TreasureWeapon> type,Level level) {super(type,level);setNoGravity(true);}
    /** loose a weapon from a spot, at a speed and toward a point */
    public static TreasureWeapon loose(Level level,LivingEntity owner,ItemStack stack,Vec3 from,Vec3 toward,double speed,float damage,int colour) {
        var weapon=new TreasureWeapon(SinBosses.TREASURE_WEAPON.get(),level);
        weapon.setOwner(owner);weapon.setPos(from);
        weapon.entityData.set(ITEM,stack.copyWithCount(1));weapon.entityData.set(COLOUR,colour);
        weapon.damage=damage;
        var motion=toward.subtract(from).normalize().scale(speed);
        weapon.setDeltaMovement(motion);weapon.face(motion);weapon.xRotO=weapon.getXRot();weapon.yRotO=weapon.getYRot();
        level.addFreshEntity(weapon);
        return weapon;
    }
    public TreasureWeapon devouring() {devours=true;return this;}
    public ItemStack weapon() {return entityData.get(ITEM);}
    public int colour() {return entityData.get(COLOUR);}
    public boolean stuck() {return entityData.get(STUCK);}
    public float damage() {return damage;}

    @Override protected void defineSynchedData() {
        entityData.define(ITEM,ItemStack.EMPTY);entityData.define(COLOUR,0xFFDF7E);entityData.define(STUCK,false);
    }
    private void face(Vec3 motion) {
        double horizontal=motion.horizontalDistance();
        setYRot((float)(Mth.atan2(motion.x,motion.z)*Mth.RAD_TO_DEG));
        setXRot((float)(Mth.atan2(motion.y,horizontal)*Mth.RAD_TO_DEG));
    }
    @Override public void tick() {
        super.tick();
        if(stuck()){
            if(!level().isClientSide&&++stuckTicks>40)discard();
            return;
        }
        if(!level().isClientSide&&++life>120){discard();return;}
        var motion=getDeltaMovement();
        HitResult hit=ProjectileUtil.getHitResultOnMoveVector(this,this::canHitEntity);
        if(hit.getType()!=HitResult.Type.MISS&&!net.minecraftforge.event.ForgeEventFactory.onProjectileImpact(this,hit))onHit(hit);
        if(isRemoved()||stuck())return;
        setPos(getX()+motion.x,getY()+motion.y,getZ()+motion.z);
        xRotO=getXRot();yRotO=getYRot();face(motion);
        if(level().isClientSide){
            // a trail of gold dust, and a glint now and then
            var back=motion.normalize().scale(-0.15);
            for(int i=0;i<2;i++)level().addParticle(SinParticles.GOLD_DUST.get().of(colour()),getX()-motion.x*i*0.5,getY()-motion.y*i*0.5,getZ()-motion.z*i*0.5,back.x,back.y+0.01,back.z);
            if(tickCount%3==0)level().addParticle(SinParticles.GLINT.get().of(0xFFF4C8),getX(),getY(),getZ(),0,0,0);
        }
    }
    @Override protected boolean canHitEntity(Entity entity) {
        return super.canHitEntity(entity)&&!(entity instanceof SinEntity)&&!(entity instanceof TreasureWeapon);
    }
    @Override protected void onHitEntity(EntityHitResult result) {
        var owner=getOwner() instanceof LivingEntity living?living:null;
        if(result.getEntity() instanceof LivingEntity target){
            if(target.hurt(damageSources().mobProjectile(this,owner),damage)&&devours)
                target.addEffect(new MobEffectInstance(SinBosses.DEVOURED.get(),160,0),owner);
        }else result.getEntity().hurt(damageSources().mobProjectile(this,owner),damage);
        playSound(SoundEvents.TRIDENT_HIT,0.8F,1.2F);
        discard();
    }
    @Override protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        var at=result.getLocation();
        setPos(at.x,at.y,at.z);setDeltaMovement(Vec3.ZERO);entityData.set(STUCK,true);
        playSound(SoundEvents.TRIDENT_HIT_GROUND,0.6F,1.4F);
        if(level() instanceof net.minecraft.server.level.ServerLevel server)server.sendParticles(ParticleTypes.CRIT,at.x,at.y,at.z,6,0.1,0.1,0.1,0.2);
    }
    @Override public boolean isPickable() {return false;}
    @Override public boolean shouldRenderAtSqrDistance(double distance) {return distance<128*128;}
    @Override protected void addAdditionalSaveData(CompoundTag tag) {super.addAdditionalSaveData(tag);tag.put("Weapon",weapon().save(new CompoundTag()));tag.putFloat("Damage",damage);}
    @Override protected void readAdditionalSaveData(CompoundTag tag) {super.readAdditionalSaveData(tag);entityData.set(ITEM,ItemStack.of(tag.getCompound("Weapon")));damage=tag.getFloat("Damage");}

    // ------------------------------------------------------------------ the treasury
    private static List<Item> pool;
    /** every weapon item in the game, from every mod: what a treasury can hold */
    public static List<Item> treasury() {
        if(pool==null){
            var out=new ArrayList<Item>();
            for(var item:ForgeRegistries.ITEMS){
                if(item instanceof ProjectileWeaponItem)continue;
                try{if(local.primordial.affix.Imbuing.isWeapon(new ItemStack(item)))out.add(item);}catch(RuntimeException ignored){}
            }
            if(out.isEmpty())out.add(net.minecraft.world.item.Items.IRON_SWORD);
            pool=out;
        }
        return pool;
    }
    public static ItemStack randomTreasure(net.minecraft.util.RandomSource random) {var all=treasury();return new ItemStack(all.get(random.nextInt(all.size())));}
    /** what a weapon hits for when hurled: its own attack damage, kept within bounds */
    public static float damageOf(ItemStack stack) {
        double damage=1;
        try{for(var modifier:stack.getAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_DAMAGE))damage+=modifier.getAmount();}catch(RuntimeException ignored){}
        return (float)Mth.clamp(damage,MIN_DAMAGE,MAX_DAMAGE);
    }
}
