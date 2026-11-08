package local.primordial.smoke;

import com.mojang.authlib.GameProfile;
import java.util.EnumSet;
import java.util.UUID;
import local.primordial.Primordial;
import local.primordial.affix.AffixEncounters;
import local.primordial.affix.AffixState;
import local.primordial.affix.BossAffix;
import local.primordial.affix.MobAffix;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import top.theillusivec4.curios.api.CuriosApi;

@GameTestHolder("primordial")
@PrefixGameTestTemplate(false)
public final class ArcaneCharmGameTests {
    private static void wear(Player player,String slot,ItemStack stack) {
        CuriosApi.getCuriosInventory(player).orElseThrow(IllegalStateException::new).setEquippedCurio(slot,0,stack);
        // Curios applies the slot's attribute changes on the wearer's next tick
        MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent(player));
    }
    private interface Setup {void apply(Zombie zombie);}
    /** a zombie without affixes, whatever players nearby would have rolled for it */
    static Zombie plainZombie(GameTestHelper helper) {
        Zombie zombie=helper.spawn(EntityType.ZOMBIE,1,2,1);
        AffixState.assign(zombie,0,EnumSet.noneOf(MobAffix.class),null);
        return zombie;
    }
    /** health lost by a fresh zombie (20 armor, 40 health) when the player deals 10 */
    private static float hit(GameTestHelper helper,Player player,Setup setup) {
        Zombie zombie=plainZombie(helper);
        zombie.getAttribute(Attributes.ARMOR).setBaseValue(20);zombie.getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(0);
        zombie.getAttribute(Attributes.MAX_HEALTH).setBaseValue(40);
        setup.apply(zombie);zombie.setHealth(zombie.getMaxHealth());
        float before=zombie.getHealth();
        zombie.hurt(player.damageSources().playerAttack(player),10);
        float lost=before-zombie.getHealth();zombie.discard();
        return lost;
    }
    private static void near(GameTestHelper helper,float actual,double expected,String what) {
        helper.assertTrue(Math.abs(actual-expected)<0.01,what+": expected "+expected+", got "+actual);
    }

    @GameTest(template="empty") public static void arcaneCharmsConvertBoostAndPierce(GameTestHelper helper) {
        Player player=helper.makeMockPlayer();
        Setup plain=z->{};
        Setup resistanceII=z->z.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,200,1));
        // affix level 30: magic resistance 30%; the affix itself adds +2 armor, irrelevant to magic damage
        Setup magicResistant=z->AffixState.assign(z,30,EnumSet.of(MobAffix.MAGIC_RESISTANCE),null);

        near(helper,hit(helper,player,plain),4,"no charm: 20 armor leaves 4 of 10");

        TestSlots.wearCharm(player,new ItemStack(Primordial.SPELLBREAKER_CHARM.get()));
        near(helper,hit(helper,player,plain),13,"spellbreaker: magic ignores armor, +30% non-physical");
        near(helper,hit(helper,player,resistanceII),13*15/25.0,"spellbreaker vs Resistance II: full 40% resisted");
        near(helper,hit(helper,player,magicResistant),13*0.7,"spellbreaker vs 30% magic resistance");

        TestSlots.wearCharm(player,new ItemStack(Primordial.ARCANE_PACT_CHARM.get()));
        near(helper,hit(helper,player,plain),16,"arcane pact: +60% non-physical");
        near(helper,hit(helper,player,resistanceII),16*18/25.0,"arcane pact vs Resistance II: 30% of it ignored");
        near(helper,hit(helper,player,magicResistant),16*0.79,"arcane pact vs 30% magic resistance: 30% of it ignored");

        var recipes=helper.getLevel().getRecipeManager();
        for(String id:new String[]{"spellbreaker_charm","arcane_pact_charm","alchemist_gift"})
            helper.assertTrue(recipes.byKey(new ResourceLocation(Primordial.ID,id)).isPresent(),"recipe "+id+" loaded");
        helper.succeed();
    }

    @GameTest(template="empty") public static void alchemistGiftGivenWornAndBook(GameTestHelper helper) {
        Player player=helper.makeMockPlayer();
        local.primordial.effect.AlchemistEvents.alchemistGiftOnFirstLogin(new PlayerEvent.PlayerLoggedInEvent(player));
        helper.assertTrue(player.getInventory().contains(new ItemStack(Primordial.ALCHEMIST_GIFT.get())),"first login hands out the Alchemist's Gift");
        player.getInventory().clearContent();
        local.primordial.effect.AlchemistEvents.alchemistGiftOnFirstLogin(new PlayerEvent.PlayerLoggedInEvent(player));
        helper.assertTrue(!player.getInventory().contains(new ItemStack(Primordial.ALCHEMIST_GIFT.get())),"only once");

        // the alchemical accessory slots come with Sage's Gift
        wear(player,Primordial.SAGE_SLOT,new ItemStack(Primordial.SAGE_HEART.get()));
        wear(player,Primordial.ALCHEMICAL_ACCESSORY_SLOT,new ItemStack(Primordial.ALCHEMIST_GIFT.get()));
        double damage=player.getAttributeValue(Primordial.DAMAGE.get());
        helper.assertTrue(Math.abs(damage-1.05)<1e-6,"Alchemist's Gift: +5% all damage, got "+damage);
        // all-damage applies without the Strength Charm: an unarmored zombie takes 10.5 from 10
        Zombie zombie=plainZombie(helper);
        zombie.getAttribute(Attributes.ARMOR).setBaseValue(0);zombie.getAttribute(Attributes.MAX_HEALTH).setBaseValue(40);zombie.setHealth(40);
        zombie.hurt(player.damageSources().playerAttack(player),10);
        near(helper,40-zombie.getHealth(),10.5,"all-damage bonus applied");
        zombie.discard();

        if(net.minecraftforge.fml.ModList.get().isLoaded("patchouli"))try{
            // internal registry: the API has no lookup for a book's item
            var registry=Class.forName("vazkii.patchouli.common.book.BookRegistry");
            var books=(java.util.Map<?,?>)registry.getField("books").get(registry.getField("INSTANCE").get(null));
            Object book=books.get(local.primordial.item.AlchemistGiftItem.BOOK);
            helper.assertTrue(book!=null,"Patchouli registered the alchemist_gift book");
            var item=(ItemStack)book.getClass().getMethod("getBookItem").invoke(book);
            helper.assertTrue(item.is(Primordial.ALCHEMIST_GIFT.get()),"the book's item is the gift, got "+item);
        }catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
        helper.succeed();
    }

    @GameTest(template="empty") public static void affixEncountersUnlockBookEntries(GameTestHelper helper) {
        // a bare server player: Forge refuses advancements to fake players, and a logged-in mock needs a network channel
        var player=new net.minecraft.server.level.ServerPlayer(helper.getLevel().getServer(),helper.getLevel(),new GameProfile(UUID.randomUUID(),"affix_test"));
        for(var affix:MobAffix.values())
            helper.assertTrue(player.server.getAdvancements().getAdvancement(AffixEncounters.advancement(affix.id()))!=null,"advancement for "+affix.id());
        for(var affix:BossAffix.values())
            helper.assertTrue(player.server.getAdvancements().getAdvancement(AffixEncounters.advancement(affix.id()))!=null,"advancement for "+affix.id());
        helper.assertTrue(!AffixEncounters.met(player,"void"),"not met before the fight");

        // dealing damage records the affixes
        Zombie zombie=plainZombie(helper);
        AffixState.assign(zombie,5,EnumSet.of(MobAffix.REGENERATION,MobAffix.SPLIT),BossAffix.WRATH);  // no retaliating affixes: the bare player has no connection for effect packets
        zombie.hurt(player.damageSources().playerAttack(player),1);
        helper.assertTrue(AffixEncounters.met(player,"regeneration")&&AffixEncounters.met(player,"split")&&AffixEncounters.met(player,"wrath"),"hitting it records all its affixes");
        helper.assertTrue(!AffixEncounters.met(player,"void"),"other affixes stay unknown");
        helper.assertTrue(AffixEncounters.met(player,"root"),"the compendium root is done with its first affix");
        var grown=player.getPersistentData().getCompound(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG).getList(local.primordial.sage.SageProgress.KEY,net.minecraft.nbt.Tag.TAG_STRING);
        for(int i=0;i<grown.size();i++)helper.assertTrue(!grown.getString(i).startsWith("primordial:affix/"),"affix advancements do not feed Sage's Gift growth: "+grown.getString(i));
        zombie.discard();

        // being attacked records them too (fake players are invulnerable, so post the attack event directly)
        Zombie attacker=plainZombie(helper);
        AffixState.assign(attacker,5,EnumSet.of(MobAffix.VOID),null);
        MinecraftForge.EVENT_BUS.post(new LivingAttackEvent(player,attacker.damageSources().mobAttack(attacker),1));
        helper.assertTrue(AffixEncounters.met(player,"void"),"being attacked records its affixes");
        attacker.discard();
        helper.succeed();
    }
}
