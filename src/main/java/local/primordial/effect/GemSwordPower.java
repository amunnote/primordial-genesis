package local.primordial.effect;

import java.util.List;
import local.primordial.Primordial;
import local.primordial.mixin.HealthAccessor;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;

public final class GemSwordPower {
    public static final double RADIUS=32.0;
    private static final int MAX_BOLTS=64;
    private static final String PREVIOUS_MODE="primordial:gem_sword_previous_mode";
    private static final ThreadLocal<Integer> SLAYING=ThreadLocal.withInitial(()->0);
    private GemSwordPower() {}
    /** True while a Gem Sword kill is being applied; affix handlers stand down for its duration. */
    public static boolean slaying() {return SLAYING.get()>0;}

    /** Kills the target outright. Only the Ascension Charm stops it. */
    public static boolean slay(Player attacker,Entity target) {
        if(target.level().isClientSide||target==attacker||!(target instanceof LivingEntity living)
            ||living instanceof ArmorStand||Protection.active(living))return false;
        int depth=SLAYING.get();SLAYING.set(depth+1);
        try {kill(attacker,living);} finally {if(depth==0)SLAYING.remove();else SLAYING.set(depth);}
        return true;
    }
    private static void kill(Player attacker,LivingEntity living) {
        DamageSource source=new DamageSource(living.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
            .getHolderOrThrow(DamageTypes.GENERIC_KILL),attacker);
        // generic_kill bypasses armour, resistance, invulnerability and totems while still crediting the attacker.
        living.invulnerableTime=0;
        living.hurt(source,Float.MAX_VALUE);
        HealthAccessor state=(HealthAccessor)living;
        if(living.getHealth()>0||!state.angelIsDead()) {
            // Something cancelled the damage or clamped the health: write the health directly.
            living.setHealth(0);
            if(living.getHealth()>0)living.getEntityData().set(HealthAccessor.angelHealthKey(),0.0F);
            if(!state.angelIsDead())living.die(source);
        }
        // Death was cancelled or the entity revived itself: remove it. Players cannot be removed safely.
        if(!(living instanceof Player)&&!living.isRemoved()&&(living.getHealth()>0||!state.angelIsDead()))
            living.remove(Entity.RemovalReason.KILLED);
    }

    /** Strikes everything in range. Normal mode only hits hostile creatures. */
    public static int burst(ServerPlayer player,boolean slaughter) {
        ServerLevel level=player.serverLevel();
        List<LivingEntity> targets=level.getEntitiesOfClass(LivingEntity.class,player.getBoundingBox().inflate(RADIUS),
            entity->entity!=player&&entity.isAlive()&&!entity.isSpectator()&&!(entity instanceof ArmorStand)
                &&entity.distanceToSqr(player)<=RADIUS*RADIUS&&!Protection.active(entity)
                &&(slaughter||hostile(entity,player)));
        int bolts=0;
        for(LivingEntity target:targets) {
            if(bolts<MAX_BOLTS) {
                var bolt=Primordial.GEM_LIGHTNING.get().create(level);
                if(bolt!=null) {bolt.moveTo(target.getX(),target.getY(),target.getZ());level.addFreshEntity(bolt);bolts++;}
            }
            slay(player,target);
        }
        return targets.size();
    }
    private static boolean hostile(LivingEntity entity,Player player) {
        return entity instanceof Enemy||entity instanceof Mob mob&&mob.getTarget()==player;
    }

    public static boolean carries(Player player) {
        var inventory=player.getInventory();
        for(int slot=0;slot<inventory.getContainerSize();slot++)
            if(inventory.getItem(slot).is(Primordial.GEM_SWORD.get()))return true;
        return false;
    }
    public static void toggleSpectator(ServerPlayer player) {
        if(!carries(player))return;
        var data=player.getPersistentData();
        GameType current=player.gameMode.getGameModeForPlayer();
        if(current==GameType.SPECTATOR) {
            player.setGameMode(GameType.byName(data.getString(PREVIOUS_MODE),GameType.SURVIVAL));
        } else {
            data.putString(PREVIOUS_MODE,current.getName());
            player.setGameMode(GameType.SPECTATOR);
        }
    }
}
