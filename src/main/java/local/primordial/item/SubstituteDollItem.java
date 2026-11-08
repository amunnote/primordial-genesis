package local.primordial.item;

import java.util.List;
import local.primordial.Primordial;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 替身草人: carried anywhere in the inventory, it takes a blow that would kill its owner in their stead, once: it is used up
 * and they are left at 30% of their health. The last of the rescues (it waits for any other to have its turn); nothing
 * that passes all protection (the void, /kill) is turned aside by it.
 */
@Mod.EventBusSubscriber(modid=Primordial.ID)
public final class SubstituteDollItem extends Item {
    public static final float HEALTH=0.3F;
    public SubstituteDollItem() {super(new Properties().stacksTo(16).rarity(Rarity.UNCOMMON));}
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> tooltip,TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.primordial.substitute_doll",Math.round(HEALTH*100)).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.primordial.substitute_doll.once").withStyle(ChatFormatting.GRAY));
    }
    @SubscribeEvent(priority=EventPriority.LOW) public static void death(LivingDeathEvent event) {
        if(!(event.getEntity() instanceof ServerPlayer player)||event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY))return;
        var inventory=player.getInventory();
        for(int i=0;i<inventory.getContainerSize();i++){
            var stack=inventory.getItem(i);
            if(!stack.is(Primordial.SUBSTITUTE_DOLL.get()))continue;
            stack.shrink(1);
            event.setCanceled(true);
            player.setHealth(player.getMaxHealth()*HEALTH);
            player.clearFire();
            var level=(ServerLevel)player.level();
            level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,player.getX(),player.getY()+1,player.getZ(),40,0.5,0.8,0.5,0.3);
            level.sendParticles(ParticleTypes.CLOUD,player.getX(),player.getY()+0.5,player.getZ(),12,0.4,0.4,0.4,0.02);
            level.playSound(null,player.blockPosition(),SoundEvents.TOTEM_USE,SoundSource.PLAYERS,0.8F,1.3F);
            if(player.connection!=null)player.displayClientMessage(Component.translatable("message.primordial.substitute_doll").withStyle(ChatFormatting.GOLD),true);
            return;
        }
    }
}
