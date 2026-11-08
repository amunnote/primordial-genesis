package local.primordial.smoke;

import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.UUID;
import local.primordial.affix.AffixBounty;
import local.primordial.affix.AffixMaterials;
import local.primordial.affix.AffixState;
import local.primordial.affix.MobAffix;
import local.primordial.effect.MoonTide;
import local.primordial.network.AttributeSources;
import local.primordial.spirit.SpiritRealm;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** The moon's tide, richer spoils for more affixes, and where the status page says an attribute comes from. */
@GameTestHolder("primordial")
@PrefixGameTestTemplate(false)
public final class MoonAndBountyGameTests {
    @GameTest(template="empty",batch="moon_tide") public static void monstersFollowTheMoon(GameTestHelper helper) {
        var level=helper.getLevel();
        level.setDayTime(18000);   // day 0, at night: a full moon
        helper.assertTrue(MoonTide.fullMoon(level)&&MoonTide.extraAffixes(level)==1,"day 0 is a full moon, and it gives one more affix");
        var zombie=helper.spawn(EntityType.ZOMBIE,2,2,2);AffixState.suppress(zombie);
        helper.assertTrue(Math.abs(zombie.getMaxHealth()-28)<0.01,"full moon: a zombie has 20 x 1.4, got "+zombie.getMaxHealth());
        helper.assertTrue(Math.abs(zombie.getAttributeValue(Attributes.ATTACK_DAMAGE)-3*1.25)<0.01,"and 25% more attack");
        // the full moon's night fills the players with Inspiration
        var player=helper.makeMockSurvivalPlayer();
        level.setDayTime(4*24000L+18000);   // day 4: a new moon
        MoonTide.apply(zombie);
        helper.assertTrue(Math.abs(zombie.getMaxHealth()-17)<0.01&&Math.abs(zombie.getHealth()-17)<0.01,"new moon: 20 x 0.85, its health kept in proportion, got "+zombie.getHealth()+"/"+zombie.getMaxHealth());
        helper.assertTrue(MoonTide.extraAffixes(level)==0&&!SpiritRealm.inspired(player),"no extra affix, nothing given");
        level.setDayTime(TestWorldSetup.NEUTRAL_TIME);
        MoonTide.apply(zombie);
        helper.assertTrue(Math.abs(zombie.getMaxHealth()-20)<0.01,"a quarter moon: as it is");
        zombie.discard();
        helper.succeed();
    }

    @GameTest(template="empty") public static void moreAffixesRicherSpoils(GameTestHelper helper) {
        var player=helper.makeMockSurvivalPlayer();
        var zombie=helper.spawn(EntityType.ZOMBIE,2,2,2);
        var state=AffixState.assign(zombie,10,EnumSet.of(MobAffix.REGENERATION,MobAffix.SPLIT,MobAffix.VOID,MobAffix.SLOWNESS,MobAffix.HEALING),null);
        helper.assertTrue(AffixBounty.weight(state)==5,"five affixes weigh five");
        var drops=new ArrayList<ItemEntity>();
        drops.add(new ItemEntity(helper.getLevel(),zombie.getX(),zombie.getY(),zombie.getZ(),new ItemStack(Items.ROTTEN_FLESH,10)));
        var material=new ItemStack(AffixMaterials.ITEMS.get(MobAffix.VOID).get());
        drops.add(new ItemEntity(helper.getLevel(),zombie.getX(),zombie.getY(),zombie.getZ(),material));
        var event=new LivingDropsEvent(zombie,player.damageSources().playerAttack(player),drops,0,true);
        AffixBounty.bountyDrops(event);
        int flesh=0,materials=0;
        for(var drop:event.getDrops()){if(drop.getItem().is(Items.ROTTEN_FLESH))flesh+=drop.getItem().getCount();if(drop.getItem().is(material.getItem()))materials+=drop.getItem().getCount();}
        helper.assertTrue(flesh>=15,"ten rotten flesh and five affixes: at least half again, got "+flesh);
        helper.assertTrue(materials==1,"affix materials keep their own odds, got "+materials);
        zombie.discard();
        helper.succeed();
    }

    @GameTest(template="empty") public static void statusPageNamesSources(GameTestHelper helper) {
        var player=new net.minecraft.server.level.ServerPlayer(helper.getLevel().getServer(),helper.getLevel(),new GameProfile(UUID.randomUUID(),"status_test"));
        // the bare player has no connection for the effect packet: put the effect and its modifiers on by hand
        player.getActiveEffectsMap().put(MobEffects.DAMAGE_BOOST,new MobEffectInstance(MobEffects.DAMAGE_BOOST,200,0));
        MobEffects.DAMAGE_BOOST.addAttributeModifiers(player,player.getAttributes(),0);
        var sword=new ItemStack(Items.DIAMOND_SWORD);
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,sword);
        player.getAttributes().addTransientAttributeModifiers(sword.getAttributeModifiers(net.minecraft.world.entity.EquipmentSlot.MAINHAND));
        var sources=AttributeSources.describe(player,player.getAttribute(Attributes.ATTACK_DAMAGE));
        helper.assertTrue(sources.size()==2,"attack damage: the sword and Strength, got "+sources.size());
        boolean effect=false,item=false;
        for(var source:sources){
            if(source.label().getContents() instanceof TranslatableContents t&&t.getKey().equals("screen.primordial.status.source.effect"))effect=true;
            if(source.label().getString().equals(sword.getHoverName().getString()))item=true;
        }
        helper.assertTrue(effect&&item,"one named by the effect, one by the sword");
        helper.succeed();
    }
}
