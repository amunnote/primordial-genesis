package local.primordial.spirit;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 原初之门 The Primordial Gate: a pillar of white gold that cannot be broken. On the arrival island it carries a player
 * who has felled all seven Sin Lords up to the Primordial Temple (and tells anyone else which lords remain); on the
 * temple's rim it carries anyone back down.
 */
public class PrimordialGateBlock extends Block {
    /** where a gate on the arrival island leads (the gates up on the temple and the terraces all lead down) */
    public enum Target implements net.minecraft.util.StringRepresentable {
        TEMPLE,BAIHU,XUANWU,QINGLONG,ZHUQUE,SAGE;
        @Override public String getSerializedName() {return name().toLowerCase(java.util.Locale.ROOT);}
        /** the guardian it leads to, or null for the temple and the Court of Truth */
        public Guardian guardian() {return this==TEMPLE||this==SAGE?null:Guardian.valueOf(name());}
        public static Target of(Guardian guardian) {return valueOf(guardian.name());}
    }
    public static final net.minecraft.world.level.block.state.properties.EnumProperty<Target> TARGET=
        net.minecraft.world.level.block.state.properties.EnumProperty.create("target",Target.class);
    public PrimordialGateBlock() {
        super(BlockBehaviour.Properties.copy(Blocks.QUARTZ_BLOCK).mapColor(MapColor.GOLD).strength(-1,3600000).noLootTable().lightLevel(state->15));
        registerDefaultState(stateDefinition.any().setValue(TARGET,Target.TEMPLE));
    }
    @Override protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block,BlockState> builder) {builder.add(TARGET);}
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit) {
        if(level instanceof ServerLevel server&&player instanceof ServerPlayer traveller){
            // the gate to the Court of Truth, for those who have felled the Primordial Lord
            if(state.getValue(TARGET)==Target.SAGE){
                if(traveller.isCreative()||PrimordialTemple.primordialFelled(traveller))SageSanctum.up(traveller,server);
                else traveller.displayClientMessage(Component.translatable("message.primordial.sage_gate.closed").withStyle(ChatFormatting.GOLD),false);
            }
            // the gates up on the temple's rim and the terraces lead down; any other leads up
            else if(pos.getY()>=PrimordialTemple.FLOOR-PrimordialTemple.DEPTH)PrimordialTemple.down(traveller,server);
            else{
                var target=state.getValue(TARGET);boolean temple=target==Target.TEMPLE;
                boolean open=traveller.isCreative()||(temple?PrimordialTemple.qualified(traveller):PrimordialTemple.sinsFelled(traveller));
                if(!open)traveller.displayClientMessage(Component.translatable("message.primordial.primordial_gate.missing",PrimordialTemple.missing(traveller,temple)).withStyle(ChatFormatting.GOLD),false);
                else if(temple)PrimordialTemple.up(traveller,server);
                else PrimordialTemple.toGuardian(traveller,server,target.guardian());
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public void animateTick(BlockState state,Level level,BlockPos pos,RandomSource random) {
        double x=pos.getX()+0.5,y=pos.getY()+1,z=pos.getZ()+0.5;
        // each gate's light in its destination's colour
        if(random.nextInt(2)==0){var guardian=state.getValue(TARGET).guardian();int c=state.getValue(TARGET)==Target.SAGE?local.primordial.boss.FirstSage.RUBEDO:guardian==null?0xFFF0C8:guardian.colour;
            level.addParticle(local.primordial.boss.SinParticles.FLARE.get().of(c),x+(random.nextDouble()-0.5)*0.6,y+0.2+random.nextDouble()*0.6,z+(random.nextDouble()-0.5)*0.6,0,0.03,0);}
        level.addParticle(ParticleTypes.END_ROD,x+(random.nextDouble()-0.5)*0.8,y+random.nextDouble()*0.4,z+(random.nextDouble()-0.5)*0.8,0,0.04+random.nextDouble()*0.05,0);
        if(random.nextInt(3)==0)level.addParticle(ParticleTypes.ENCHANT,x,y+1.2,z,(random.nextDouble()-0.5)*2,-0.6,(random.nextDouble()-0.5)*2);
    }
    @Override public ItemStack getCloneItemStack(BlockGetter level,BlockPos pos,BlockState state) {return ItemStack.EMPTY;}
}
