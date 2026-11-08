package local.primordial.entity.alchemical;

import net.minecraft.world.entity.Mob;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * An alchemical creation: an overworld monster rebuilt by alchemists, with that monster's weak points mended and no fear
 * of daylight. Animated with GeckoLib from assets/primordial/{geo,animations,textures}/entity/&lt;id&gt;.*.
 * Every creation has the animations "animation.&lt;id&gt;.idle", ".walk" and ".attack".
 */
public interface AlchemicalCreation extends GeoEntity {
    /** the entity id path, which also names its model, animations and texture */
    String creationId();
    /** working its magic: its circle and halo blaze up and glyphs pour off it. By default, while it is hunting something */
    default boolean channeling() {return this instanceof Mob mob&&mob.isAggressive();}

    static AnimatableInstanceCache cache(AlchemicalCreation creation) {return GeckoLibUtil.createInstanceCache(creation);}

    /** walking or standing, plus a swing whenever the mob attacks */
    static <T extends Mob&AlchemicalCreation> void movementAndAttack(T mob,AnimatableManager.ControllerRegistrar controllers) {
        String id=mob.creationId();
        RawAnimation idle=RawAnimation.begin().thenLoop("animation."+id+".idle");
        RawAnimation walk=RawAnimation.begin().thenLoop("animation."+id+".walk");
        RawAnimation attack=RawAnimation.begin().thenPlay("animation."+id+".attack");
        controllers.add(new AnimationController<>(mob,"move",4,state->{
            // the gait keeps pace with how fast it actually moves
            state.setControllerSpeed(state.isMoving()?Math.min(2.2F,0.5F+state.getLimbSwingAmount()*1.8F):1F);
            return state.setAndContinue(state.isMoving()?walk:idle);
        }));
        controllers.add(new AnimationController<>(mob,"attack",2,state->{
            if(state.getAnimatable().swinging)return state.setAndContinue(attack);
            state.getController().forceAnimationReset();
            return PlayState.STOP;
        }));
    }
}
