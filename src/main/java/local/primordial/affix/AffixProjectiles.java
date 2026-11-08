package local.primordial.affix;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import local.primordial.effect.Protection;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public final class AffixProjectiles {
    private static final String TARGET="primordial:causal_target";
    private static final Map<ServerLevel,Set<UUID>> TRACKED=new WeakHashMap<>();
    private AffixProjectiles() {}
    public static void join(Projectile projectile,ServerLevel level) {
        if(projectile.getPersistentData().hasUUID(TARGET)){prepare(projectile);TRACKED.computeIfAbsent(level,l->new HashSet<>()).add(projectile.getUUID());return;}
        // Causality's accessory: every shot its wearer looses finds its mark, bosses too (a wider look, or the last creature it hit)
        boolean causal=projectile.getOwner() instanceof net.minecraft.world.entity.player.Player shooter&&AffixAccessories.wears(shooter,MobAffix.CAUSALITY);
        if(projectile.getOwner() instanceof net.minecraft.world.entity.player.Player player&&(causal||local.primordial.item.AlchemyCharmItem.homing(player))){
            var target=aimedAt(projectile,player,level,causal?60:30);
            if(target==null&&causal){
                var last=player.getLastHurtMob();
                if(last!=null&&last.isAlive()&&!(last instanceof net.minecraft.world.entity.player.Player)&&last.distanceToSqr(player)<=64*64)target=last;
            }
            if(target==null)return;
            projectile.getPersistentData().putUUID(TARGET,target.getUUID());prepare(projectile);
            TRACKED.computeIfAbsent(level,l->new HashSet<>()).add(projectile.getUUID());return;
        }
        if(!(projectile.getOwner() instanceof Mob mob))return;var state=AffixState.get(mob);
        if(state==null||!state.has(MobAffix.CAUSALITY)||mob.getTarget()==null||AffixAccessories.blocks(mob.getTarget(),state,MobAffix.CAUSALITY))return;
        projectile.getPersistentData().putUUID(TARGET,mob.getTarget().getUUID());
        prepare(projectile);
        TRACKED.computeIfAbsent(level,l->new HashSet<>()).add(projectile.getUUID());
    }
    /** the creature a homing shot was aimed at: within 30 degrees of its flight and 64 blocks, never a player or the shooter's own pet */
    public static LivingEntity aimedAt(Projectile projectile,net.minecraft.world.entity.player.Player shooter,ServerLevel level) {return aimedAt(projectile,shooter,level,30);}
    public static LivingEntity aimedAt(Projectile projectile,net.minecraft.world.entity.player.Player shooter,ServerLevel level,double degrees) {
        Vec3 heading=projectile.getDeltaMovement();if(heading.lengthSqr()<1e-6)heading=shooter.getLookAngle();
        heading=heading.normalize();
        LivingEntity best=null;double bestScore=Double.MAX_VALUE,maxAngle=Math.toRadians(degrees);
        for(var living:level.getEntitiesOfClass(LivingEntity.class,projectile.getBoundingBox().inflate(64),
            e->e!=shooter&&e.isAlive()&&!(e instanceof net.minecraft.world.entity.player.Player)&&!(e instanceof net.minecraft.world.entity.decoration.ArmorStand)
                &&!(e instanceof net.minecraft.world.entity.OwnableEntity pet&&pet.getOwnerUUID()!=null&&pet.getOwnerUUID().equals(shooter.getUUID())))){
            Vec3 to=living.getBoundingBox().getCenter().subtract(projectile.position());double distance=to.length();
            if(distance>64||distance<1e-3)continue;
            double angle=Math.acos(Math.max(-1,Math.min(1,to.dot(heading)/distance)));if(angle>maxAngle)continue;
            double score=angle+distance*0.004;   // mostly the closest to the line of flight, nearer ones on a tie
            if(score<bestScore){bestScore=score;best=living;}
        }
        return best;
    }
    private static void prepare(Projectile projectile) {
        projectile.setNoGravity(true);
        // Canceling impact alone does not stop vanilla arrows becoming embedded while inside a wall.
        if(projectile instanceof net.minecraft.world.entity.projectile.AbstractArrow arrow)arrow.setNoPhysics(true);
    }
    public static boolean causal(Projectile projectile) {return projectile.getPersistentData().hasUUID(TARGET);}
    public static boolean intended(Projectile projectile,net.minecraft.world.entity.Entity entity) {
        return causal(projectile)&&projectile.getPersistentData().getUUID(TARGET).equals(entity.getUUID());
    }
    public static void tick(ServerLevel level) {
        var tracked=TRACKED.get(level);if(tracked==null)return;var iterator=tracked.iterator();
        while(iterator.hasNext()) {
            var entity=level.getEntity(iterator.next());if(!(entity instanceof Projectile projectile)||!projectile.isAlive()){iterator.remove();continue;}
            var target=level.getEntity(projectile.getPersistentData().getUUID(TARGET));
            if(!(target instanceof LivingEntity living)||!living.isAlive()||Protection.active(living)||projectile.tickCount>200||projectile.distanceToSqr(living)>512*512) {
                projectile.discard();iterator.remove();continue;
            }
            Vec3 delta=living.getBoundingBox().getCenter().subtract(projectile.position());
            if(delta.length()<1.2) {
                ((local.primordial.mixin.AffixProjectileAccess)projectile).angelAffixHit(new EntityHitResult(living));projectile.discard();iterator.remove();
            } else {
                prepare(projectile);double speed=Math.min(3,Math.max(0.7,projectile.getDeltaMovement().length()));
                projectile.setDeltaMovement(delta.normalize().scale(Math.min(speed,delta.length())));projectile.hasImpulse=true;
            }
        }
    }
}
