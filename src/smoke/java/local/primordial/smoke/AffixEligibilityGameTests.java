package local.primordial.smoke;

import local.primordial.affix.AffixDistribution;
import local.primordial.affix.AffixEligibility;
import local.primordial.affix.MobAffix;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("primordial")
@PrefixGameTestTemplate(false)
public final class AffixEligibilityGameTests {
    private static <T extends Mob> T mob(GameTestHelper helper,EntityType<T> type,net.minecraft.world.item.Item held) {
        T mob=helper.spawn(type,1,2,1);
        mob.setItemSlot(EquipmentSlot.MAINHAND,held==null?ItemStack.EMPTY:new ItemStack(held));
        return mob;
    }
    private static void expect(GameTestHelper helper,Mob mob,MobAffix affix,boolean usable) {
        helper.assertTrue(AffixEligibility.usable(mob,affix)==usable,
            mob.getType().getDescriptionId()+" holding "+mob.getMainHandItem()+": "+affix.id()+" should "+(usable?"":"not ")+"be usable");
    }
    @GameTest(template="empty") public static void affixesOnlyRollWhereTheyWork(GameTestHelper helper) {
        var zombie=mob(helper,EntityType.ZOMBIE,Items.IRON_SWORD);
        var archer=mob(helper,EntityType.SKELETON,Items.BOW);
        var unarmedSkeleton=mob(helper,EntityType.SKELETON,null);
        var drowned=mob(helper,EntityType.DROWNED,null);
        var tridentDrowned=mob(helper,EntityType.DROWNED,Items.TRIDENT);
        var pillager=mob(helper,EntityType.PILLAGER,Items.CROSSBOW);
        var ghast=mob(helper,EntityType.GHAST,null);
        var blaze=mob(helper,EntityType.BLAZE,null);
        var creeper=mob(helper,EntityType.CREEPER,null);
        var witch=mob(helper,EntityType.WITCH,null);

        expect(helper,zombie,MobAffix.CAUSALITY,false);
        expect(helper,drowned,MobAffix.CAUSALITY,false);
        expect(helper,unarmedSkeleton,MobAffix.CAUSALITY,false);
        expect(helper,creeper,MobAffix.CAUSALITY,false);
        for(Mob shooter:new Mob[]{archer,tridentDrowned,pillager,ghast,blaze,witch})expect(helper,shooter,MobAffix.CAUSALITY,true);

        expect(helper,zombie,MobAffix.STRENGTH,true);
        expect(helper,blaze,MobAffix.STRENGTH,true);
        for(Mob noMelee:new Mob[]{archer,pillager,tridentDrowned,ghast,creeper,witch})expect(helper,noMelee,MobAffix.STRENGTH,false);

        expect(helper,witch,MobAffix.MAGIC,false);
        expect(helper,witch,MobAffix.ARMOR_PIERCE,false);
        expect(helper,zombie,MobAffix.MAGIC,true);
        expect(helper,zombie,MobAffix.REFLECTION,true);

        // over many rolls a zombie never gets causality, and magic never comes with armor pierce
        for(long seed=0;seed<2000;seed++){
            var rolled=AffixDistribution.choose(8,seed,affix->AffixEligibility.usable(zombie,affix));
            helper.assertTrue(rolled.size()==8,"a full roll when enough affixes are usable");
            helper.assertTrue(!rolled.contains(MobAffix.CAUSALITY),"zombie rolled causality with seed "+seed);
            helper.assertTrue(!(rolled.contains(MobAffix.MAGIC)&&rolled.contains(MobAffix.ARMOR_PIERCE)),"magic with armor pierce, seed "+seed);
        }
        for(var m:new Mob[]{zombie,archer,unarmedSkeleton,drowned,tridentDrowned,pillager,ghast,blaze,creeper,witch})m.discard();
        helper.succeed();
    }
}
