package local.primordial.boss;

import java.util.List;
import local.primordial.affix.BossAffix;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * A Sin Sigil (傲慢之印 and its six kin): dropped by an affixed boss of that sin bearing seven affixes, to a killer of
 * level 50 or more. Used on a fallen Sin Lord's statue of the same sin, it calls the lord back to life.
 */
public class SinSigilItem extends Item {
    public static final int LEVEL_REQUIRED=50,AFFIXES_REQUIRED=7;
    private final BossAffix sin;
    public SinSigilItem(BossAffix sin) {super(new Properties().stacksTo(16).rarity(Rarity.EPIC).fireResistant());this.sin=sin;}
    public BossAffix sin() {return sin;}
    @Override public boolean isFoil(ItemStack stack) {return true;}
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> tooltip,TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.primordial.sin_sigil.use",sin.label()).withStyle(s->s.withColor(sin.color())));
        tooltip.add(Component.translatable("tooltip.primordial.sin_sigil.source",AFFIXES_REQUIRED,LEVEL_REQUIRED).withStyle(ChatFormatting.DARK_GRAY));
    }
}
