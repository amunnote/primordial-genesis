package local.primordial.mixin;

import local.primordial.entity.alchemical.AlchemicalBlaze;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** A blaze trails thick smoke; an alchemical one, sealed in its armillary, sheds only a few small flames, so its model shows. */
@Mixin(Blaze.class)
public abstract class BlazeSmokeMixin {
    @Redirect(method="aiStep",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"))
    private void angel$alchemicalFlames(Level level,ParticleOptions particle,double x,double y,double z,double dx,double dy,double dz) {
        if(!((Object)this instanceof AlchemicalBlaze blaze)){level.addParticle(particle,x,y,z,dx,dy,dz);return;}
        if(blaze.getRandom().nextInt(6)==0)level.addParticle(ParticleTypes.SMALL_FLAME,x,y,z,0,0.02,0);
    }
}
