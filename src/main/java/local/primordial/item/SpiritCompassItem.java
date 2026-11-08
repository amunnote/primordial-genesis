package local.primordial.item;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.*;
public final class SpiritCompassItem extends Item {
    public SpiritCompassItem(){super(new Properties().stacksTo(1).rarity(Rarity.RARE));}
    @Override public boolean isFoil(ItemStack stack){return true;}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        if(player instanceof net.minecraft.server.level.ServerPlayer p){
            var targets=local.primordial.guide.NextStep.candidates(p);
            if(p.isShiftKeyDown())p.getPersistentData().putString("GuideTarget",targets.get(0).id());
            String current=p.getPersistentData().getString("GuideTarget");int index=-1;
            for(int i=0;i<targets.size();i++)if(targets.get(i).id().equals(current))index=i;
            p.getPersistentData().putString("GuideTarget",targets.get((index+1)%targets.size()).id());
            local.primordial.guide.GuideEvents.refresh(p,true);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand),level.isClientSide);
    }
}
