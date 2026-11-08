package local.primordial.boss;

import local.primordial.affix.BossAffix;
import local.primordial.entity.alchemical.ArcaneSkill;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The statue a fallen Sin Lord leaves where it died: a dark pedestal with the lord frozen in stone upon it. It cannot be
 * broken. Right-clicked with that sin's sigil, it is spent and the lord rises again, awake, and returns to its sanctum.
 */
public class SinStatueBlock extends BaseEntityBlock {
    public SinStatueBlock() {
        super(BlockBehaviour.Properties.copy(Blocks.POLISHED_BLACKSTONE).mapColor(MapColor.COLOR_BLACK).strength(-1,3600000).noLootTable());
    }
    @Override public RenderShape getRenderShape(BlockState state) {return RenderShape.MODEL;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) {return new Statue(pos,state);}

    /** raise a statue for a fallen lord, as near where it fell as there is room */
    public static BlockPos raise(ServerLevel level,SinBoss lord) {
        if(local.primordial.spirit.Guardian.of(lord.getType())!=null)return null;
        var pos=lord.blockPosition();
        for(int i=0;i<6&&!level.getBlockState(pos).canBeReplaced();i++)pos=pos.above();
        if(!level.getBlockState(pos).canBeReplaced())return null;
        level.setBlockAndUpdate(pos,SinBosses.STATUE.get().defaultBlockState());
        if(level.getBlockEntity(pos) instanceof Statue statue){statue.set(lord.sin(),lord instanceof PrimordialLord,lord.home(),lord.yBodyRot);statue.lord(lord.getType());}
        ArcaneSkill.burst(level,lord,Vec3.atCenterOf(pos).add(0,1,0),30,0.3);
        level.playSound(null,pos,SoundEvents.STONE_PLACE,SoundSource.BLOCKS,2F,0.5F);
        return pos;
    }

    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit) {
        if(!(level.getBlockEntity(pos) instanceof Statue statue)||statue.lordType()==null)return InteractionResult.PASS;
        if(local.primordial.spirit.Guardian.of(statue.lordType())!=null){
            if(!level.isClientSide)player.displayClientMessage(Component.translatable("message.primordial.guardian.migrated"),false);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        var stack=player.getItemInHand(hand);
        var reviver=SinBosses.reviver(statue.lordType());
        if(reviver==null||!stack.is(reviver)){
            if(!level.isClientSide&&reviver!=null)player.displayClientMessage(Component.translatable("message.primordial.sin_statue.need",
                Component.translatable(reviver.getDescriptionId())).withStyle(s->s.withColor(statue.colour())),true);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if(level instanceof ServerLevel server){
            if(!player.getAbilities().instabuild)stack.shrink(1);
            var lord=revive(server,pos,statue);
            if(lord!=null)local.primordial.achievement.Achievements.statueRevived(player,lord);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    /** the statue crumbles and its lord rises in its place */
    public static SinBoss revive(ServerLevel level,BlockPos pos,Statue statue) {
        var type=statue.lordType();
        if(type==null||local.primordial.spirit.Guardian.of(type)!=null)return null;
        var lord=type.create(level);
        if(lord==null)return null;
        var home=statue.home()==null?pos:statue.home();
        level.removeBlock(pos,false);
        lord.moveTo(pos.getX()+0.5,pos.getY(),pos.getZ()+0.5,statue.yaw(),0);
        lord.setHome(home);
        level.addFreshEntity(lord);
        lord.wake();
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION,pos.getX()+0.5,pos.getY()+1,pos.getZ()+0.5,3,0.5,0.5,0.5,0);
        level.playSound(null,pos,SoundEvents.WITHER_SPAWN,SoundSource.HOSTILE,1.5F,0.8F);
        for(var p:level.players())if(p.distanceToSqr(Vec3.atCenterOf(pos))<96*96)
            p.sendSystemMessage(Component.translatable("message.primordial.sin_statue.revive",lord.getDisplayName()).withStyle(s->s.withColor(statue.colour()).withBold(true)));
        return lord;
    }

    /** what a statue remembers: whose it is, where that lord's sanctum seat was, and which way it faced */
    public static class Statue extends BlockEntity {
        private BossAffix sin;private boolean primordial;private BlockPos home;private float yaw;
        /** whose statue it is, for lords that are neither a sin's nor the Primordial Lord */
        private net.minecraft.resources.ResourceLocation lord;
        public void lord(net.minecraft.world.entity.EntityType<?> type) {
            if(sin!=null||primordial)return;
            lord=net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(type);setChanged();
            if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);
        }
        public Statue(BlockPos pos,BlockState state) {super(SinBosses.STATUE_ENTITY.get(),pos,state);}
        public BossAffix sin() {return sin;}
        /** the Primordial Lord's statue, which has no sin */
        public boolean primordial() {return primordial;}
        /** whose statue it is, or null for a blank one */
        public net.minecraft.world.entity.EntityType<? extends SinBoss> lordType() {
            if(primordial)return SinBosses.PRIMORDIAL.get();
            if(sin!=null)return SinBosses.lordType(sin);
            if(lord!=null){
                var type=net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getValue(lord);
                if(type!=null&&type!=net.minecraft.world.entity.EntityType.PIG&&SinBosses.reviver(type)!=null){
                    @SuppressWarnings("unchecked") var boss=(net.minecraft.world.entity.EntityType<? extends SinBoss>)type;return boss;
                }
            }
            return null;
        }
        public int colour() {
            var guardian=local.primordial.spirit.Guardian.of(lordType());
            return primordial?PrimordialLord.COLOUR:sin!=null?sin.color():guardian!=null?guardian.colour:0xFFFFFF;
        }
        public BlockPos home() {return home;}
        public float yaw() {return yaw;}
        public void set(BossAffix sin,boolean primordial,BlockPos home,float yaw) {
            this.sin=sin;this.primordial=primordial;this.home=home;this.yaw=yaw;setChanged();
            if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);
        }
        @Override protected void saveAdditional(CompoundTag tag) {
            super.saveAdditional(tag);
            if(sin!=null)tag.putString("Sin",sin.id());
            if(primordial)tag.putBoolean("Primordial",true);
            if(lord!=null)tag.putString("Lord",lord.toString());
            if(home!=null)tag.put("Home",NbtUtils.writeBlockPos(home));
            tag.putFloat("Yaw",yaw);
        }
        @Override public void load(CompoundTag tag) {
            super.load(tag);
            sin=BossAffix.byId(tag.getString("Sin"));
            primordial=tag.getBoolean("Primordial");
            lord=tag.contains("Lord")?net.minecraft.resources.ResourceLocation.tryParse(tag.getString("Lord")):null;
            home=tag.contains("Home")?NbtUtils.readBlockPos(tag.getCompound("Home")):null;
            yaw=tag.getFloat("Yaw");
        }
        @Override public CompoundTag getUpdateTag() {return saveWithoutMetadata();}
        @Override public Packet<ClientGamePacketListener> getUpdatePacket() {return ClientboundBlockEntityDataPacket.create(this);}
        @Override public AABB getRenderBoundingBox() {return new AABB(worldPosition).inflate(3,5,3).move(0,3,0);}
    }
    /** a statue has no other way to be found */
    @Override public net.minecraft.world.item.ItemStack getCloneItemStack(BlockGetter level,BlockPos pos,BlockState state) {return net.minecraft.world.item.ItemStack.EMPTY;}
}
