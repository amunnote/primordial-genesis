package local.primordial.item;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.List;
import java.util.UUID;
import local.primordial.Primordial;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/**
 * The Alchemist's Gift: an alchemical accessory (+5% all damage) that is also the Patchouli book
 * listing how every alchemy recipe is obtained and the affixes the reader has met.
 */
public final class AlchemistGiftItem extends Item implements ICurioItem {
    public static final ResourceLocation BOOK=new ResourceLocation(Primordial.ID,"alchemist_gift");
    public AlchemistGiftItem() {super(new Properties().stacksTo(1).rarity(Rarity.UNCOMMON));}
    @Override public boolean canEquip(SlotContext context,ItemStack stack) {
        return !context.cosmetic()&&Primordial.ALCHEMICAL_ACCESSORY_SLOT.equals(context.identifier());
    }
    // right-click opens the book; equip through the curios screen
    @Override public boolean canEquipFromUse(SlotContext context,ItemStack stack) {return false;}
    @Override public Multimap<Attribute,AttributeModifier> getAttributeModifiers(SlotContext context,UUID uuid,ItemStack stack) {
        Multimap<Attribute,AttributeModifier> result=HashMultimap.create();
        if(!context.cosmetic())result.put(Primordial.DAMAGE.get(),
            new AttributeModifier(uuid,"primordial.alchemist_gift_damage",0.05,AttributeModifier.Operation.MULTIPLY_TOTAL));
        return result;
    }
    /** below this level the wearer slowly gains experience: one point every 2 s (level 0 to 10 in about 5 minutes) */
    public static final int TUTOR_LEVEL=10,TUTOR_INTERVAL=40;
    private static final String TUTORED_AT="primordial:alchemist_gift_xp_at";
    /** the health it mends each second (a half heart) */
    public static final float MEND=1;
    private static final String MENDED_AT="primordial:alchemist_gift_mended_at";
    @Override public void curioTick(SlotContext context,ItemStack stack) {
        if(context.cosmetic()||!(context.entity() instanceof Player player)||player.level().isClientSide||player.isSpectator())return;
        if(player instanceof ServerPlayer server&&player.tickCount%20==0)local.primordial.guide.GuideEvents.updateGift(server,stack);
        // a point of health back each second, however many gifts are worn
        if(player.tickCount%20==0&&player.isAlive()&&player.getHealth()<player.getMaxHealth()){
            long second=player.level().getGameTime();var data=player.getPersistentData();
            if(data.getLong(MENDED_AT)!=second){data.putLong(MENDED_AT,second);player.heal(MEND);}
        }
        if(player.experienceLevel>=TUTOR_LEVEL||player.tickCount%TUTOR_INTERVAL!=0)return;
        // several gifts worn at once still teach at one pace
        long now=player.level().getGameTime();var data=player.getPersistentData();
        if(data.getLong(TUTORED_AT)==now)return;
        data.putLong(TUTORED_AT,now);
        player.giveExperiencePoints(1);
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand) {
        var stack=player.getItemInHand(hand);
        if(player instanceof ServerPlayer server) {
            if(net.minecraftforge.fml.ModList.get().isLoaded("patchouli"))local.primordial.compat.patchouli.PatchouliBook.open(server,BOOK);
            else local.primordial.guide.GuideNetwork.send(server,true);
            local.primordial.guide.GuideEvents.opened(server);
        }
        return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
    }
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> tooltip,TooltipFlag flag) {
        tooltip.add(Component.translatable("guide.primordial.open").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.primordial.alchemist_gift.intro").withStyle(ChatFormatting.GRAY,ChatFormatting.ITALIC));
        tooltip.add(Component.translatable("tooltip.primordial.alchemist_gift.slot").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.primordial.alchemist_gift.tutor",TUTOR_LEVEL,TUTOR_INTERVAL/20).withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.translatable("tooltip.primordial.alchemist_gift.mend").withStyle(ChatFormatting.RED));
        tooltip.add(Component.translatable("tooltip.primordial.alchemist_gift.read").withStyle(ChatFormatting.AQUA));
        if(stack.hasTag()&&stack.getTag().contains("GuideNext")){var next=Component.Serializer.fromJson(stack.getTag().getString("GuideNext"));if(next!=null)tooltip.add(next);}
    }
}
