package local.primordial.zhuque;
import net.minecraft.world.item.*;
    public final class KindlingTalisman extends Item {
        public KindlingTalisman(){super(new Properties().stacksTo(16));}
        @Override public net.minecraft.world.InteractionResultHolder<ItemStack> use(net.minecraft.world.level.Level l,net.minecraft.world.entity.player.Player p,net.minecraft.world.InteractionHand hand){
            var stack=p.getItemInHand(hand);var hit=p.pick(32,0,false);
            if(hit.getType()!=net.minecraft.world.phys.HitResult.Type.BLOCK||p.getCooldowns().isOnCooldown(this))return net.minecraft.world.InteractionResultHolder.pass(stack);
            var blockHit=(net.minecraft.world.phys.BlockHitResult)hit;
            var pos=blockHit.getBlockPos().relative(blockHit.getDirection());
            if(!l.mayInteract(p,pos)||!p.mayUseItemAt(pos,blockHit.getDirection(),stack)||!l.isEmptyBlock(pos)||!net.minecraft.world.level.block.BaseFireBlock.canBePlacedAt(l,pos,blockHit.getDirection()))return net.minecraft.world.InteractionResultHolder.pass(stack);
            if(!l.isClientSide){
                if(!l.setBlockAndUpdate(pos,net.minecraft.world.level.block.BaseFireBlock.getState(l,pos)))return net.minecraft.world.InteractionResultHolder.fail(stack);
                p.getCooldowns().addCooldown(this,60);if(!p.getAbilities().instabuild)stack.shrink(1);
            }return net.minecraft.world.InteractionResultHolder.sidedSuccess(stack,l.isClientSide);
        }
    }
