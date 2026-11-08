package local.primordial.fauna;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;

/** A 萤雷瓶 set down: a lantern of glass whose firefly now and then throws a spark. */
public class FireflyJarBlock extends LanternBlock {
    public FireflyJarBlock(Properties properties) {super(properties);}
    @Override public void animateTick(BlockState state,Level level,BlockPos pos,RandomSource random) {
        if(random.nextInt(4)!=0)return;
        double y=pos.getY()+(state.getValue(HANGING)?0.35:0.25);
        level.addParticle(ParticleTypes.ELECTRIC_SPARK,pos.getX()+0.5+(random.nextDouble()-0.5)*0.25,y,pos.getZ()+0.5+(random.nextDouble()-0.5)*0.25,0,0.01,0);
    }
}
