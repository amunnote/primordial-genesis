package local.primordial.smoke;

import local.primordial.Primordial;
import local.primordial.sage.SageProgress;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import top.theillusivec4.curios.api.CuriosApi;

@GameTestHolder("primordial")
@PrefixGameTestTemplate(false)
public final class SageLuckGameTests {
    private static double luckGrowth(Player player) {
        for(AttributeModifier m:player.getAttribute(Attributes.LUCK).getModifiers(AttributeModifier.Operation.MULTIPLY_TOTAL))
            if(m.getName().equals("sage_advancement_luck"))return m.getAmount();
        return 0;
    }
    @GameTest(template="empty") public static void eachAdvancementAddsOnePercentLuck(GameTestHelper helper) {
        Player player=helper.makeMockPlayer();
        var curios=CuriosApi.getCuriosInventory(player).orElseThrow(IllegalStateException::new);
        curios.setEquippedCurio(Primordial.SAGE_SLOT,0,new ItemStack(Primordial.SAGE_HEART.get()));
        // every advancement counts but this mod's hidden mirrors; the growth is capped at +33%
        ListTag earned=new ListTag();
        for(int i=0;i<20;i++)earned.add(StringTag.valueOf("test:adv_"+i));
        earned.add(StringTag.valueOf("primordial:known/origin_orb"));earned.add(StringTag.valueOf("primordial:affix/void"));
        var persisted=player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        persisted.put(SageProgress.KEY,earned);player.getPersistentData().put(Player.PERSISTED_NBT_TAG,persisted);
        SageProgress.refresh(player);
        helper.assertTrue(SageProgress.count(player)==20&&Math.abs(luckGrowth(player)-0.20)<1e-9,"20 advancements (the mirrors left out): +20% luck, got "+luckGrowth(player));
        for(int i=20;i<50;i++)earned.add(StringTag.valueOf("test:adv_"+i));
        persisted.put(SageProgress.KEY,earned);player.getPersistentData().put(Player.PERSISTED_NBT_TAG,persisted);
        SageProgress.refresh(player);
        helper.assertTrue(Math.abs(luckGrowth(player)-0.33)<1e-9,"50 advancements: luck capped at +33% like damage, got "+luckGrowth(player));        curios.setEquippedCurio(Primordial.SAGE_SLOT,0,ItemStack.EMPTY);
        SageProgress.refresh(player);
        helper.assertTrue(luckGrowth(player)==0,"No growth without the gift");
        helper.succeed();
    }
}
