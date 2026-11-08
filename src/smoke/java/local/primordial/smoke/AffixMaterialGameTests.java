package local.primordial.smoke;

import java.util.ArrayList;
import java.util.EnumSet;
import local.primordial.affix.AffixMaterials;
import local.primordial.affix.AffixState;
import local.primordial.affix.BossAffix;
import local.primordial.affix.MobAffix;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("primordial")
@PrefixGameTestTemplate(false)
public final class AffixMaterialGameTests {
    /** looting high enough to make the ordinary roll certain */
    static final int SURE=100;
    static net.minecraft.world.entity.player.Player player(GameTestHelper helper,double luck,int level) {
        var player=helper.makeMockSurvivalPlayer();
        player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.LUCK).setBaseValue(luck);
        player.experienceLevel=level;
        return player;
    }
    private static boolean bossMaterial(ItemEntity e){return AffixMaterials.BOSS_ITEMS.values().stream().anyMatch(r->e.getItem().is(r.get()));}

    @GameTest(template="empty") public static void affixMaterialsDrop(GameTestHelper helper) {
        for(var affix:MobAffix.values())helper.assertTrue(AffixMaterials.ITEMS.get(affix).isPresent(),"material for "+affix.id());
        var lucky=player(helper,20,20);var veryLucky=player(helper,100,20);

        var zombie=helper.spawn(EntityType.ZOMBIE,1,2,1);
        var affixes=EnumSet.of(MobAffix.SPACE,MobAffix.VOID,MobAffix.RUIN);
        var state=AffixState.assign(zombie,10,affixes,null);
        var drops=new ArrayList<ItemEntity>();
        AffixMaterials.drop(state,SURE,null,drops);
        helper.assertTrue(drops.isEmpty(),"no materials without a player kill");

        // at full chance: exactly one material, for one of its own affixes, and over many kills each of them
        var seen=EnumSet.noneOf(MobAffix.class);
        for(int i=0;i<200;i++){var list=new ArrayList<ItemEntity>();AffixMaterials.drop(state,SURE,lucky,list);
            helper.assertTrue(list.size()==1,"one material per kill, got "+list.size());
            for(var affix:affixes)if(list.get(0).getItem().is(AffixMaterials.ITEMS.get(affix).get()))seen.add(affix);}
        helper.assertTrue(seen.equals(affixes),"picked among its affixes, saw "+seen);

        // 5%, +1.5% per Looting level
        helper.assertTrue(Math.abs(AffixMaterials.chance(0)-0.05)<1e-9&&Math.abs(AffixMaterials.chance(3)-0.095)<1e-9,"material odds");
        int dropped=0,kills=20000;
        for(int i=0;i<kills;i++){var list=new ArrayList<ItemEntity>();AffixMaterials.drop(state,0,lucky,list);dropped+=list.size();}
        double rate=dropped/(double)kills;
        helper.assertTrue(rate>0.04&&rate<0.06,"base chance near 5% per mob, got "+rate);

        // the gates: 20 luck and level 20
        for(var gated:new net.minecraft.world.entity.player.Player[]{player(helper,19.9,20),player(helper,20,19)})
            for(int i=0;i<100;i++){var list=new ArrayList<ItemEntity>();AffixMaterials.drop(state,SURE,gated,list);
                helper.assertTrue(list.isEmpty(),"under 20 luck or level 20: nothing, got "+list.size());}

        var boss=helper.spawn(EntityType.ZOMBIE,1,2,1);
        var bossState=AffixState.assign(boss,10,EnumSet.of(MobAffix.CURSE,MobAffix.SEAL),BossAffix.GREED);
        for(var affix:BossAffix.values())helper.assertTrue(AffixMaterials.BOSS_ITEMS.get(affix).isPresent(),"material for "+affix.id());
        for(int i=0;i<20;i++){var list=new ArrayList<ItemEntity>();AffixMaterials.drop(bossState,0,veryLucky,list);
            helper.assertTrue(list.size()==2,"a boss always drops one affix material plus its boss material, got "+list.size());
            helper.assertTrue(list.stream().anyMatch(e->e.getItem().is(AffixMaterials.BOSS_ITEMS.get(BossAffix.GREED).get())),"greed boss drops greed gold marrow");}
        var midBoss=new ArrayList<ItemEntity>();AffixMaterials.drop(bossState,0,lucky,midBoss);
        helper.assertTrue(midBoss.size()==1&&midBoss.stream().noneMatch(AffixMaterialGameTests::bossMaterial),"luck 20: one affix material, no boss material");
        var lowLevel=new ArrayList<ItemEntity>();AffixMaterials.drop(bossState,0,player(helper,100,10),lowLevel);
        helper.assertTrue(lowLevel.size()==1&&bossMaterial(lowLevel.get(0)),"the boss material needs only the luck");
        var noKill=new ArrayList<ItemEntity>();AffixMaterials.drop(bossState,0,null,noKill);
        helper.assertTrue(noKill.isEmpty(),"bosses drop nothing without a player kill");

        zombie.getPersistentData().getCompound(AffixState.KEY).putBoolean("clone",true);
        var clone=AffixState.get(zombie);var none=new ArrayList<ItemEntity>();
        AffixMaterials.drop(clone,SURE,veryLucky,none);
        helper.assertTrue(none.isEmpty(),"split clones drop nothing");
        zombie.discard();boss.discard();
        helper.succeed();
    }
}
