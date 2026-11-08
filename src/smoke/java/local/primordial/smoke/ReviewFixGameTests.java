package local.primordial.smoke;

import java.util.ArrayList;
import java.util.EnumSet;
import local.primordial.Primordial;
import local.primordial.affix.AffixState;
import local.primordial.affix.MobAffix;
import local.primordial.alchemy.AlchemyKnowledge;
import local.primordial.altar.AltarBlockEntity;
import local.primordial.altar.AltarCrafting;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("primordial")
@PrefixGameTestTemplate(false)
public final class ReviewFixGameTests {
    private static int affixTags(Component name) {
        int n=0;for(var part:name.getSiblings())
            if(part.getContents() instanceof TranslatableContents t&&t.getKey().equals("affix.primordial.tag"))n++;
        return n;
    }

    @GameTest(template="empty") public static void allDamageReductionApplies(GameTestHelper helper) {
        var player=helper.makeMockSurvivalPlayer();
        player.getAttribute(Primordial.REDUCTION.get()).addTransientModifier(new AttributeModifier("test",0.2,AttributeModifier.Operation.ADDITION));
        player.setHealth(20);player.hurt(player.damageSources().magic(),10);
        helper.assertTrue(Math.abs(20-player.getHealth()-8)<0.01,"20% all-damage reduction: 10 becomes 8, took "+(20-player.getHealth()));
        helper.succeed();
    }

    @GameTest(template="empty") public static void altarResultsSplitAndPilesCountFully(GameTestHelper helper) {
        var level=helper.getLevel();
        BlockPos pos=helper.absolutePos(new BlockPos(1,2,1));
        level.setBlockAndUpdate(pos,Primordial.BASIC_ALCHEMY_ALTAR.get().defaultBlockState());
        var altar=(AltarBlockEntity)level.getBlockEntity(pos);
        var player=helper.makeMockPlayer();
        AlchemyKnowledge.learn(player,AlchemyKnowledge.TRANSMUTATION_SOURCE);
        altar.add(new ItemStack(Primordial.ALCHEMICAL_ESSENCE.get()));
        for(var item:new net.minecraft.world.item.Item[]{Items.COAL,Items.COPPER_INGOT,Items.IRON_INGOT,Items.GOLD_INGOT,Items.DIAMOND,Items.REDSTONE,Items.LAPIS_LAZULI})
            altar.add(new ItemStack(item));
        helper.assertTrue(AltarCrafting.tryCraft(level,pos,player)==AltarCrafting.Result.CRAFTED,"transmutation source crafted");
        helper.assertTrue(altar.items().size()==3&&altar.items().stream().allMatch(s->s.getCount()==1&&s.is(Primordial.TRANSMUTATION_SOURCE.get())),
            "three sources, one per place, got "+altar.items());

        // a pile of 3 sources is 3 items: it no longer passes for the single source the Essence of Truth needs
        var truth=level.getRecipeManager().byKey(new ResourceLocation(Primordial.ID,"truth_essence")).orElseThrow();
        var books=new ArrayList<ItemStack>();
        for(var e:new net.minecraft.world.item.enchantment.Enchantment[]{Enchantments.SHARPNESS,Enchantments.SMITE,Enchantments.UNBREAKING,
            Enchantments.MENDING,Enchantments.POWER_ARROWS,Enchantments.FIRE_ASPECT,Enchantments.MOB_LOOTING})
            books.add(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(e,1)));
        books.add(new ItemStack(local.primordial.entity.alchemical.AlchemicalCreations.RUNE_EMBER.get()));
        var pile=new ArrayList<>(books);pile.add(new ItemStack(Primordial.TRANSMUTATION_SOURCE.get(),3));
        var single=new ArrayList<>(books);single.add(new ItemStack(Primordial.TRANSMUTATION_SOURCE.get()));
        helper.assertTrue(!((net.minecraft.world.item.crafting.Recipe<net.minecraft.world.Container>)truth).matches(new SimpleContainer(pile.toArray(ItemStack[]::new)),level),
            "a pile of 3 sources does not count as one");
        helper.assertTrue(((net.minecraft.world.item.crafting.Recipe<net.minecraft.world.Container>)truth).matches(new SimpleContainer(single.toArray(ItemStack[]::new)),level),
            "one source still matches");
        level.removeBlock(pos,false);
        helper.succeed();
    }

    @GameTest(template="empty") public static void convertedMobsKeepAffixesOnce(GameTestHelper helper) {
        var husk=helper.spawn(EntityType.HUSK,1,2,1);
        AffixState.assign(husk,10,EnumSet.of(MobAffix.REGENERATION,MobAffix.SPACE),null);
        var zombie=husk.convertTo(EntityType.ZOMBIE,true);
        helper.assertTrue(zombie!=null,"converted");
        net.minecraftforge.event.ForgeEventFactory.onLivingConvert(husk,zombie);
        var state=AffixState.get(zombie);
        helper.assertTrue(state!=null&&state.has(MobAffix.REGENERATION)&&state.has(MobAffix.SPACE)&&state.affixes.size()==2,"affixes carried over");
        helper.assertTrue(affixTags(zombie.getCustomName())==2,"each affix named once, got "+zombie.getCustomName());
        var base=zombie.getCustomName().getSiblings().get(0).getContents();
        helper.assertTrue(base instanceof TranslatableContents t&&t.getKey().equals("entity.minecraft.zombie"),"named as a zombie, not a husk: "+zombie.getCustomName());

        // causality only works for shooters: a skeleton's causality is dropped when it becomes a stray? no, strays shoot too; a husk's is dropped
        var husk2=helper.spawn(EntityType.HUSK,1,2,1);
        AffixState.assign(husk2,10,EnumSet.of(MobAffix.CAUSALITY,MobAffix.VOID),null);
        var zombie2=husk2.convertTo(EntityType.ZOMBIE,true);
        net.minecraftforge.event.ForgeEventFactory.onLivingConvert(husk2,zombie2);
        var state2=AffixState.get(zombie2);
        helper.assertTrue(state2!=null&&state2.has(MobAffix.VOID)&&!state2.has(MobAffix.CAUSALITY),"unusable affixes are dropped on conversion");
        zombie.discard();zombie2.discard();
        helper.succeed();
    }

    @GameTest(template="empty") public static void transmutationDropsAffixName(GameTestHelper helper) {
        var zombie=helper.spawn(EntityType.ZOMBIE,1,2,1);
        AffixState.assign(zombie,10,EnumSet.of(MobAffix.VOID),null);
        Mob made=local.primordial.alchemy.Transmutation.transmute(helper.getLevel(),zombie);
        helper.assertTrue(made!=null,"transmuted");
        helper.assertTrue(made.getCustomName()==null||affixTags(made.getCustomName())==0
            ||AffixState.get(made)!=null&&affixTags(made.getCustomName())==AffixState.get(made).affixes.size(),"no leftover affix name, got "+made.getCustomName());
        // a player's name tag survives the transmutation
        var named=helper.spawn(EntityType.ZOMBIE,1,2,1);
        named.setCustomName(Component.literal("Bob"));
        AffixState.assign(named,10,EnumSet.of(MobAffix.VOID),null);
        Mob made2=local.primordial.alchemy.Transmutation.transmute(helper.getLevel(),named);
        helper.assertTrue(made2!=null&&made2.getCustomName()!=null&&made2.getCustomName().getString().startsWith("Bob"),"given name kept, got "+(made2==null?null:made2.getCustomName()));
        made.discard();made2.discard();
        helper.succeed();
    }

    @GameTest(template="empty") public static void splitClonesDropNoEssence(GameTestHelper helper) {
        var clone=helper.spawn(EntityType.ZOMBIE,1,2,1);
        AffixState.assign(clone,10,EnumSet.of(MobAffix.SPLIT),local.primordial.affix.BossAffix.GREED);   // a boss always drops essence
        clone.getPersistentData().getCompound(AffixState.KEY).putBoolean("clone",true);
        var drops=new ArrayList<ItemEntity>();
        var event=new net.minecraftforge.event.entity.living.LivingDropsEvent(clone,clone.damageSources().generic(),drops,3,true);
        local.primordial.affix.AffixEvents.essenceDrop(event);
        helper.assertTrue(drops.isEmpty(),"a split copy drops nothing, got "+drops.size());
        clone.discard();
        helper.succeed();
    }
}
