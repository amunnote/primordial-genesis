package local.primordial.smoke;

import local.primordial.compat.LegacyIds;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

/** Worlds from when the mod was angel_charm: old names read as new (LegacyIds). */
@GameTestHolder("primordial")
@PrefixGameTestTemplate(false)
public final class LegacyIdGameTests {
    @GameTest(template="empty") public static void oldNamesReadAsNew(GameTestHelper helper) {
        helper.assertTrue(LegacyIds.fix("angel_charm:angel_charm").equals("primordial:angel_charm"),"the Ascension Charm keeps its own name");
        helper.assertTrue(LegacyIds.fix("{\"translate\":\"entity.angel_charm.wandering_alchemist\"}").equals("{\"translate\":\"entity.primordial.wandering_alchemist\"}"),"a translation key in a name");
        helper.assertTrue(LegacyIds.fix("angel_charm_guardian_seats").equals("angel_charm_guardian_seats")&&LegacyIds.fix("minecraft:stone").equals("minecraft:stone"),"other names untouched");
        // an old item stack: its id, its data keys and the recipe it holds
        var tag=new CompoundTag();tag.putString("id","angel_charm:alchemy_recipe");tag.putByte("Count",(byte)1);
        var data=new CompoundTag();data.putString("angel_charm:recipe","angel_charm:basic_charm");
        var list=new ListTag();list.add(StringTag.valueOf("angel_charm:strength"));data.put("angel_charm:imbues",list);tag.put("tag",data);
        LegacyIds.migrate(tag);
        var stack=ItemStack.of(tag);
        helper.assertTrue(stack.is(local.primordial.Primordial.ALCHEMY_RECIPE.get()),"the old item is the new one, got "+stack);
        helper.assertTrue("primordial:basic_charm".equals(stack.getTag().getString("primordial:recipe"))&&"primordial:strength".equals(stack.getTag().getList("primordial:imbues",8).getString(0)),"its data renamed: "+stack.getTag());
        // registries answer to the old names
        helper.assertTrue(ForgeRegistries.ITEMS.getValue(new ResourceLocation("angel_charm","sage_heart"))==local.primordial.Primordial.SAGE_HEART.get(),"an item by its old name");
        helper.succeed();
    }
}
