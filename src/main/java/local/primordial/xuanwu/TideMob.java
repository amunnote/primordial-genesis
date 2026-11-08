package local.primordial.xuanwu;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
public abstract class TideMob extends PathfinderMob {
    protected TideMob(EntityType<? extends TideMob> t,Level l){super(t,l);setPathfindingMalus(net.minecraft.world.level.pathfinder.BlockPathTypes.WATER,0);}
    @Override protected net.minecraft.world.entity.ai.navigation.PathNavigation createNavigation(Level l){return new net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation(this,l);}
    @Override public void aiStep(){super.aiStep();if(!level().isClientSide&&isInWater()&&!getNavigation().isDone()){
        var path=getNavigation().getPath();if(path!=null&&!path.isDone()){var to=path.getNextEntityPos(this).subtract(position());if(to.lengthSqr()>0.1)setDeltaMovement(getDeltaMovement().add(to.normalize().scale(0.025)));}
    }}
    @Override public boolean canBreatheUnderwater(){return true;}
    @Override public boolean isPushedByFluid(){return false;}
    @Override public void travel(Vec3 input){if(isEffectiveAi()&&isInWater()){moveRelative(0.04F,input);move(MoverType.SELF,getDeltaMovement());setDeltaMovement(getDeltaMovement().scale(0.9));}else super.travel(input);}
}
