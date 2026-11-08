package local.primordial.sage;

import java.util.EnumSet;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

/** Native attack goals keep their own melee, ranged and explosive behavior. */
public final class SageTargetGoal extends Goal {
    private final Mob mob;
    private Player target;
    private boolean wasOrderedSitting;
    public SageTargetGoal(Mob mob){this.mob=mob;setFlags(EnumSet.of(Flag.TARGET));}
    @Override public boolean canUse(){
        double distance=32*32;target=null;
        for(var player:mob.level().players()){
            double candidate=mob.distanceToSqr(player);
            if(candidate<distance&&SageHeart.enemyOf(mob,player)&&mob.getSensing().hasLineOfSight(player)){target=player;distance=candidate;}
        }
        return target!=null;
    }
    @Override public boolean canContinueToUse(){return target!=null&&SageHeart.enemyOf(mob,target)&&mob.distanceToSqr(target)<=32*32;}
    @Override public void start(){
        if(mob instanceof net.minecraft.world.entity.TamableAnimal pet){wasOrderedSitting=pet.isOrderedToSit();pet.setOrderedToSit(false);pet.setInSittingPose(false);}
        mob.setTarget(target);
    }
    @Override public void tick(){mob.setTarget(target);}
    @Override public void stop(){if(mob.getTarget()==target)mob.setTarget(null);target=null;
        if(wasOrderedSitting&&mob instanceof net.minecraft.world.entity.TamableAnimal pet)pet.setOrderedToSit(true);
        wasOrderedSitting=false;
    }
}
