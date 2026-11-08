package local.primordial.mixin;
import java.util.ArrayDeque;
import local.primordial.sage.SageHeart;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(Player.class)
public abstract class SagePlayerMixin {
    @Unique private final ArrayDeque<Float> sageHealthBefore=new ArrayDeque<>();
    @Inject(method="actuallyHurt",at=@At("HEAD"))
    private void sageBefore(DamageSource source,float amount,CallbackInfo ci){var player=(Player)(Object)this;sageHealthBefore.push(!player.level().isClientSide&&SageHeart.active(player)&&SageHeart.netherUnlocked(player)?player.getHealth():-1F);}
    @Inject(method="actuallyHurt",at=@At("RETURN"))
    private void sageAfter(DamageSource source,float amount,CallbackInfo ci){float before=sageHealthBefore.pop();if(before>=0&&source.getEntity() instanceof LivingEntity attacker&&attacker!=(Object)this)attacker.heal(Math.max(0,before-((Player)(Object)this).getHealth()));}
    @Inject(method={"giveExperiencePoints","giveExperienceLevels"},at=@At("RETURN"))
    private void sageExperience(int amount,CallbackInfo ci){SageHeart.capExperience((Player)(Object)this);}
    @ModifyVariable(method="giveExperiencePoints",at=@At("HEAD"),argsOnly=true)
    private int sageAllowedExperience(int amount){return SageHeart.allowedExperience((Player)(Object)this,amount);}
    @Inject(method="canEat",at=@At("HEAD"),cancellable=true)
    private void sageEat(boolean always,CallbackInfoReturnable<Boolean> callback){if(SageHeart.active((Player)(Object)this))callback.setReturnValue(true);}
}
