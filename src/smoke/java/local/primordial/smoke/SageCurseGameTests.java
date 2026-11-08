package local.primordial.smoke;

import java.util.ArrayList;
import local.primordial.Primordial;
import local.primordial.item.SageHeartItem;
import local.primordial.sage.SageHeart;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import top.theillusivec4.curios.api.CuriosApi;

@GameTestHolder("primordial")
@PrefixGameTestTemplate(false)
public final class SageCurseGameTests {
    private static void equip(Player player) {
        CuriosApi.getCuriosInventory(player).orElseThrow(IllegalStateException::new)
            .setEquippedCurio(Primordial.SAGE_SLOT,0,new ItemStack(Primordial.SAGE_HEART.get()));
        TestSlots.tick(player);
    }
    private static void unlock(Player player,boolean nether,boolean end) {
        CompoundTag saved=player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        saved.putBoolean("primordial:sage_nether_unlocked",nether);
        saved.putBoolean("primordial:sage_end_unlocked",end);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG,saved);
    }
    @GameTest(template="empty") public static void permanentUnlocksAndBaseCurse(GameTestHelper helper) {
        var player=helper.makeMockPlayer();equip(player);
        helper.assertTrue(SageHeart.active(player),"Gift is equipped");
        helper.assertTrue(SageHeart.unlocks(player)==0,"Neither milestone starts unlocked");
        player.getAttribute(Attributes.LUCK).setBaseValue(10);
        player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(40);
        player.getFoodData().setFoodLevel(3);
        // the level cannot pass the luck, and max health cannot pass the level
        player.experienceLevel=1000;
        SageHeart.maintain(player);
        int luck=(int)player.getAttributeValue(Attributes.LUCK);
        helper.assertTrue(player.experienceLevel==luck,"level capped at luck: level="+player.experienceLevel+" luck="+luck);
        helper.assertTrue(Math.abs(player.getMaxHealth()-Math.min(40,luck))<0.001,"health capped at the (capped) level: "+player.getMaxHealth());
        player.experienceLevel=6;SageHeart.maintain(player);
        helper.assertTrue(Math.abs(player.getMaxHealth()-6)<0.001,"level 6: max health 6, got "+player.getMaxHealth()
            +" protection="+local.primordial.effect.Protection.active(player));
        player.experienceLevel=0;SageHeart.maintain(player);
        helper.assertTrue(Math.abs(player.getMaxHealth()-1)<0.001,"level 0: max health 1, got "+player.getMaxHealth());
        helper.assertTrue(player.getFoodData().getFoodLevel()==20,"Base curse removes hunger");
        unlock(player,true,true);
        var respawned=helper.makeMockPlayer();SageHeart.copyUnlocks(player,respawned);
        helper.assertTrue(SageHeart.unlocks(respawned)==3,"Nether and End milestones survive player cloning");
        helper.succeed();
    }
    @GameTest(template="empty") public static void levelsSurviveRejoining(GameTestHelper helper) {
        var player=helper.makeMockPlayer();equip(player);
        var luck=player.getAttribute(Attributes.LUCK);
        luck.setBaseValue(0);player.experienceLevel=50;
        // just joined: the level and health curses wait
        SageHeart.settle(player);SageHeart.maintain(player);
        helper.assertTrue(player.experienceLevel==50,"while settling after a join the level is left alone: "+player.experienceLevel);
        helper.assertTrue(player.getMaxHealth()>1,"and max health is not cut to the level: "+player.getMaxHealth());
        player.getPersistentData().remove("primordial:sage_settle_until");
        // a charm worn whose luck is not on the player yet: its luck cannot be judged, the level stays
        var charm=new ItemStack(Primordial.CLEAR_WORLD_CHARM.get());
        var inv=CuriosApi.getCuriosInventory(player).orElseThrow(IllegalStateException::new);
        var slot=Primordial.ALCHEMICAL_SLOT;
        inv.setEquippedCurio(slot,0,charm);
        boolean gives=!SageHeart.luckSettled(player);
        if(gives){SageHeart.maintain(player);
            helper.assertTrue(player.experienceLevel==50,"a charm's luck not yet applied: the level is left alone: "+player.experienceLevel);}
        inv.setEquippedCurio(slot,0,ItemStack.EMPTY);TestSlots.tick(player);
        // once the luck is all there, the curse holds as before
        helper.assertTrue(SageHeart.luckSettled(player),"nothing worn but the heart: the luck is settled");
        luck.setBaseValue(10);SageHeart.maintain(player);
        helper.assertTrue(player.experienceLevel==(int)player.getAttributeValue(Attributes.LUCK),"settled: the level is capped at the luck: "+player.experienceLevel);
        helper.assertTrue(gives,"the charm gives luck, so the check was exercised");
        helper.succeed();
    }    @GameTest(template="empty") public static void tooltipShowsLockedAndUnlockedCurses(GameTestHelper helper) {
        var lines=new ArrayList<Component>();SageHeartItem.appendCurses(lines,true,0);
        helper.assertTrue(lines.size()==7,"Alt shows seven curses");
        helper.assertTrue(!lines.get(0).getSiblings().get(0).getStyle().isStrikethrough(),"First curse is active");
        helper.assertTrue(lines.get(3).getSiblings().get(0).getStyle().isStrikethrough(),"Nether curse is struck through before unlock");
        helper.assertTrue(lines.get(5).getSiblings().get(0).getStyle().isStrikethrough(),"End curse is struck through before unlock");
        lines.clear();SageHeartItem.appendCurses(lines,true,3);
        helper.assertTrue(!lines.get(3).getSiblings().get(0).getStyle().isStrikethrough()
            &&!lines.get(5).getSiblings().get(0).getStyle().isStrikethrough(),"Unlocked curses have no strikethrough");
        helper.succeed();
    }
    @GameTest(template="empty") public static void curseStagesChangeEffectDurations(GameTestHelper helper){
        var player=helper.makeMockPlayer();equip(player);
        var speed=SageHeart.effect(player,new MobEffectInstance(MobEffects.MOVEMENT_SPEED,200));
        var poison=SageHeart.effect(player,new MobEffectInstance(MobEffects.POISON,200));
        helper.assertTrue(speed.getDuration()==100&&poison.getDuration()==400,"Buff and debuff duration curses apply immediately");
        helper.assertTrue(!SageHeart.endUnlocked(player),"End curse is initially locked");
        unlock(player,false,true);
        helper.assertTrue(SageHeart.endUnlocked(player),"End curse unlock is stored permanently");
        helper.succeed();
    }
}
