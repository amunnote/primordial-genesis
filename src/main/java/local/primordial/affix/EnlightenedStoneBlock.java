package local.primordial.affix;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 启迪之石: the stone an enlightened mob sets down to climb or bridge. It holds for {@link #LIFETIME} ticks, then
 * crumbles away (its scheduled tick is saved with the chunk, so it goes even after a restart), and drops nothing.
 */
public class EnlightenedStoneBlock extends Block {
    public static final int LIFETIME=600;
    public EnlightenedStoneBlock() {super(BlockBehaviour.Properties.copy(Blocks.COBBLESTONE).strength(1.2F,3F).noLootTable());}
    @Override public void onPlace(BlockState state,Level level,BlockPos pos,BlockState old,boolean moving) {
        super.onPlace(state,level,pos,old,moving);
        if(!level.isClientSide&&!old.is(this))level.scheduleTick(pos,this,LIFETIME);
    }
    @Override public void tick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random) {
        level.removeBlock(pos,false);
        level.sendParticles(ParticleTypes.ENCHANT,pos.getX()+0.5,pos.getY()+0.5,pos.getZ()+0.5,12,0.3,0.3,0.3,0.4);
        level.sendParticles(ParticleTypes.POOF,pos.getX()+0.5,pos.getY()+0.5,pos.getZ()+0.5,4,0.2,0.2,0.2,0.01);
        level.playSound(null,pos,SoundEvents.STONE_BREAK,SoundSource.BLOCKS,0.6F,1.4F);
    }
}
