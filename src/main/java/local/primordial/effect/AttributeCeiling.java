package local.primordial.effect;

import local.primordial.mixin.RangedAttributeAccessor;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;

/**
 * Vanilla holds max health to 1024, too little for the realm's greatest bosses. The ceiling is lifted to a million, as
 * Attribute Fix and its kin do. It is only ever raised, never lowered: if another mod has set it higher, that stands,
 * and lifting it again after every mod has loaded keeps it up whichever order they ran in. Applied on both sides, so
 * clients do not clamp the health they are sent.
 */
public final class AttributeCeiling {
    public static final double MAX_HEALTH=1_000_000;
    private AttributeCeiling() {}
    public static void lift() {
        if(Attributes.MAX_HEALTH instanceof RangedAttribute ranged){
            var bounds=(RangedAttributeAccessor)ranged;
            if(bounds.angelMaxValue()<MAX_HEALTH)bounds.angelSetMaxValue(MAX_HEALTH);
        }
    }
    public static double maxHealth() {return Attributes.MAX_HEALTH instanceof RangedAttribute ranged?((RangedAttributeAccessor)ranged).angelMaxValue():1024;}
}
