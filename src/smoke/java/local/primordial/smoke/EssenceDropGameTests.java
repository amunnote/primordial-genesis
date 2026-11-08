package local.primordial.smoke;

import java.util.ArrayList;
import java.util.EnumSet;
import local.primordial.Primordial;
import local.primordial.affix.AffixEvents;
import local.primordial.affix.AffixState;
import local.primordial.affix.MobAffix;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("primordial")
@PrefixGameTestTemplate(false)
public final class EssenceDropGameTests {
    private static double essenceRate(GameTestHelper helper,int looting,int kills) {
        var zombie=ArcaneCharmGameTests.plainZombie(helper);
        AffixState.assign(zombie,10,EnumSet.of(MobAffix.VOID),null);
        int dropped=0;
        for(int i=0;i<kills;i++){
            var drops=new ArrayList<ItemEntity>();
            AffixEvents.essenceDrop(new LivingDropsEvent(zombie,zombie.damageSources().generic(),drops,looting,false));
            for(var d:drops)if(d.getItem().is(Primordial.ALCHEMICAL_ESSENCE.get()))dropped++;
        }
        zombie.discard();
        return dropped/(double)kills;
    }
    @GameTest(template="empty",timeoutTicks=400) public static void essenceDropRates(GameTestHelper helper) {
        double base=essenceRate(helper,0,4000),looting2=essenceRate(helper,2,4000);
        helper.assertTrue(Math.abs(base-0.5)<0.03,"affixed mob: 50% essence, got "+base);
        helper.assertTrue(Math.abs(looting2-0.7)<0.03,"Looting II: 70% essence, got "+looting2);

        var level=helper.getLevel();
        var table=level.getServer().getLootData().getLootTable(new ResourceLocation(Primordial.ID,"chests/alchemy_laboratory"));
        int chests=4000,withEssence=0;
        for(int i=0;i<chests;i++){
            var params=new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN,helper.absoluteVec(new net.minecraft.world.phys.Vec3(1,2,1))).create(LootContextParamSets.CHEST);
            if(table.getRandomItems(params).stream().anyMatch(s->s.is(Primordial.ALCHEMICAL_ESSENCE.get())))withEssence++;
        }
        double share=withEssence/(double)chests;
        helper.assertTrue(Math.abs(share-0.2)<0.03,"laboratory chest: 20% essence, got "+share);
        helper.succeed();
    }
}
