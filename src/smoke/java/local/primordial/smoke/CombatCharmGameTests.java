package local.primordial.smoke;

import local.primordial.Primordial;
import local.primordial.effect.AlchemistEvents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import top.theillusivec4.curios.api.CuriosApi;

@GameTestHolder("primordial")
@PrefixGameTestTemplate(false)
public final class CombatCharmGameTests {
    private static void wear(Player player,ItemStack stack) {
        TestSlots.wearCharm(player,stack);
    }
    private static Zombie target(GameTestHelper helper) {
        Zombie zombie=ArcaneCharmGameTests.plainZombie(helper);
        zombie.getAttribute(Attributes.ARMOR).setBaseValue(0);zombie.getAttribute(Attributes.MAX_HEALTH).setBaseValue(40);zombie.setHealth(40);
        return zombie;
    }
    private static void near(GameTestHelper helper,double actual,double expected,String what) {
        helper.assertTrue(Math.abs(actual-expected)<0.01,what+": expected "+expected+", got "+actual);
    }
    private static float arrowHit(GameTestHelper helper,Player shooter) {
        Zombie zombie=target(helper);var arrow=new Arrow(helper.getLevel(),shooter);
        zombie.hurt(shooter.damageSources().arrow(arrow,shooter),10);
        float lost=40-zombie.getHealth();zombie.discard();arrow.discard();return lost;
    }
    private static int arrows(Player player) {
        int n=0;for(var s:player.getInventory().items)if(s.is(Items.ARROW))n+=s.getCount();return n;
    }

    @GameTest(template="empty") public static void bladeCharmsSpeedAndFeebleness(GameTestHelper helper) {
        Player player=helper.makeMockSurvivalPlayer();
        double base=player.getAttributeValue(Attributes.ATTACK_SPEED);
        wear(player,new ItemStack(Primordial.THOUSAND_BLADE_CHARM.get()));
        near(helper,player.getAttributeValue(Attributes.ATTACK_SPEED),base*1.3,"thousand-blade resonance: +30% attack speed");
        wear(player,new ItemStack(Primordial.SWIFT_PACT_CHARM.get()));
        near(helper,player.getAttributeValue(Attributes.ATTACK_SPEED),base*1.6,"pact of swiftness: +60% attack speed");

        Zombie attacker=target(helper);
        player.invulnerableTime=0;player.hurt(attacker.damageSources().mobAttack(attacker),1);
        helper.assertTrue(local.primordial.affix.AffixStatuses.feeble(attacker)>0,"hitting the pact wearer makes the attacker feeble");
        // a feeble attacker deals 20% less
        Zombie victim=target(helper);
        victim.hurt(attacker.damageSources().mobAttack(attacker),10);
        near(helper,40-victim.getHealth(),8,"feeble attacker damage");
        attacker.discard();victim.discard();

        // feebleness also costs attack speed
        Player feeble=helper.makeMockSurvivalPlayer();double speed=feeble.getAttributeValue(Attributes.ATTACK_SPEED);
        local.primordial.affix.AffixStatuses.applyFeeble(feeble);
        near(helper,feeble.getAttributeValue(Attributes.ATTACK_SPEED),speed*0.8,"feeble attack speed");
        // an independent status: milk (clearing effects) leaves it alone
        feeble.removeAllEffects();
        helper.assertTrue(local.primordial.affix.AffixStatuses.feeble(feeble)==local.primordial.affix.AffixStatuses.FEEBLE_TICKS,"milk does not clear feebleness");
        // it wears off: 100 ticks later the attack speed is back
        for(int i=0;i<local.primordial.affix.AffixStatuses.FEEBLE_TICKS;i++)
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.TickEvent.PlayerTickEvent(net.minecraftforge.event.TickEvent.Phase.END,feeble));
        near(helper,feeble.getAttributeValue(Attributes.ATTACK_SPEED),speed,"feebleness wears off");
        helper.succeed();
    }

    @GameTest(template="empty") public static void archeryCharmsDamageDrawAndAmmo(GameTestHelper helper) {
        Player player=helper.makeMockSurvivalPlayer();
        near(helper,arrowHit(helper,player),10,"no charm, arrow");
        wear(player,new ItemStack(Primordial.QUICKSTRING_CHARM.get()));
        near(helper,arrowHit(helper,player),13,"quickstring sigil: +30% projectile damage");

        // without the pact a full draw uses an arrow
        var bow=new ItemStack(Items.BOW);player.setItemInHand(InteractionHand.MAIN_HAND,bow);player.getInventory().add(new ItemStack(Items.ARROW,5));
        bow.releaseUsing(helper.getLevel(),player,bow.getUseDuration()-20);
        helper.assertTrue(arrows(player)==4,"a plain shot uses an arrow, have "+arrows(player));
        helper.getLevel().getEntitiesOfClass(AbstractArrow.class,player.getBoundingBox().inflate(8),a->a.getOwner()==player).forEach(net.minecraft.world.entity.Entity::discard);

        wear(player,new ItemStack(Primordial.SWIFTSTRING_PACT_CHARM.get()));
        near(helper,arrowHit(helper,player),15,"pact of the swift string: +50% projectile damage");
        bow.releaseUsing(helper.getLevel(),player,bow.getUseDuration()-20);
        helper.assertTrue(arrows(player)==4,"the pact keeps the arrow, have "+arrows(player));
        var shot=helper.getLevel().getEntitiesOfClass(AbstractArrow.class,player.getBoundingBox().inflate(8),a->a.getOwner()==player);
        helper.assertTrue(!shot.isEmpty()&&shot.stream().allMatch(a->a.pickup!=AbstractArrow.Pickup.ALLOWED),"free arrows cannot be picked up");
        shot.forEach(net.minecraft.world.entity.Entity::discard);

        var crossbow=new ItemStack(Items.CROSSBOW);player.setItemInHand(InteractionHand.MAIN_HAND,crossbow);
        crossbow.releaseUsing(helper.getLevel(),player,crossbow.getUseDuration()-CrossbowItem.getChargeDuration(crossbow));
        helper.assertTrue(CrossbowItem.isCharged(crossbow),"crossbow loaded");
        helper.assertTrue(arrows(player)==4,"loading the crossbow keeps the arrow, have "+arrows(player));

        // +60% draw speed: 10 real ticks charge like 16
        helper.assertTrue(AlchemistEvents.drawRemaining(72000,10,0.6)==72000-16+1,"draw speed math");
        // and through the real use-item events: start drawing, then 10 ticks later the bow is 16 ticks charged
        var drawBow=new ItemStack(Items.BOW);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.entity.living.LivingEntityUseItemEvent.Start(player,drawBow,drawBow.getUseDuration()));
        player.getPersistentData().putLong("primordial:draw_start",helper.getLevel().getGameTime()-10);
        var tick=new net.minecraftforge.event.entity.living.LivingEntityUseItemEvent.Tick(player,drawBow,drawBow.getUseDuration()-10);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(tick);
        helper.assertTrue(tick.getDuration()==drawBow.getUseDuration()-16+1,"draw tick sped up, got "+tick.getDuration());

        var recipes=helper.getLevel().getRecipeManager();
        for(String id:new String[]{"swift_pact_charm","swiftstring_pact_charm"})
            helper.assertTrue(recipes.byKey(new ResourceLocation(Primordial.ID,id)).isPresent(),"recipe "+id+" loaded");
        helper.succeed();
    }

    @GameTest(template="empty") public static void noviceLibrariansSellCombatCharms(GameTestHelper helper) {
        var villager=helper.spawn(EntityType.VILLAGER,1,2,1);
        var listings=net.minecraft.world.entity.npc.VillagerTrades.TRADES.get(net.minecraft.world.entity.npc.VillagerProfession.LIBRARIAN).get(1);
        var mixed=java.util.Arrays.stream(listings).filter(l->l instanceof AlchemistEvents.CharmOrBook).findFirst().orElse(null);
        helper.assertTrue(mixed!=null,"the novice enchanted book trade carries the charms");
        helper.assertTrue(java.util.Arrays.stream(listings).noneMatch(AlchemistEvents::isEnchantedBookTrade),"no separate book trade left");
        int blade=0,string=0,books=0,rolls=6000;
        for(int i=0;i<rolls;i++){var offer=mixed.getOffer(villager,villager.getRandom());
            if(offer.getResult().is(Primordial.THOUSAND_BLADE_CHARM.get()))blade++;
            else if(offer.getResult().is(Primordial.QUICKSTRING_CHARM.get()))string++;
            else if(offer.getResult().is(Items.ENCHANTED_BOOK))books++;}
        // each charm is one entry beside every tradeable enchantment
        double expected=rolls/(double)(AlchemistEvents.CharmOrBook.enchantments()+2);
        helper.assertTrue(Math.abs(blade-expected)<expected*0.35&&Math.abs(string-expected)<expected*0.35,
            "each charm about "+(int)expected+" of "+rolls+", got "+blade+" and "+string);
        helper.assertTrue(books+blade+string==rolls,"everything else is an enchanted book");
        // a charm sells once and never restocks; books restock as usual
        net.minecraft.world.item.trading.MerchantOffer charmOffer=null,bookOffer=null;
        while(charmOffer==null||bookOffer==null){var o=mixed.getOffer(villager,villager.getRandom());
            if(AlchemistEvents.CharmOrBook.isCharmOffer(o))charmOffer=o;else bookOffer=o;}
        helper.assertTrue(charmOffer.getMaxUses()==1,"charm offer sells one");
        charmOffer.increaseUses();bookOffer.increaseUses();charmOffer.resetUses();bookOffer.resetUses();
        helper.assertTrue(charmOffer.isOutOfStock(),"charm offer does not restock");
        helper.assertTrue(!bookOffer.isOutOfStock(),"book offer restocks");
        var reloaded=new net.minecraft.world.item.trading.MerchantOffer(charmOffer.createTag());reloaded.resetUses();
        helper.assertTrue(reloaded.isOutOfStock(),"still no restock after saving and loading");
        villager.discard();
        helper.succeed();
    }
}
