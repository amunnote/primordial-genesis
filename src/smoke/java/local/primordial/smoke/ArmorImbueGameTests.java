package local.primordial.smoke;

import local.primordial.Primordial;
import local.primordial.affix.AffixAccessories;
import local.primordial.affix.AffixMaterials;
import local.primordial.affix.Imbuing;
import local.primordial.affix.MobAffix;
import local.primordial.altar.AltarBlockEntity;
import local.primordial.altar.AltarCrafting;
import local.primordial.effect.ArmorImbueEvents;
import local.primordial.sage.SageHeart;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import top.theillusivec4.curios.api.CuriosApi;

/** Pseudo alchemical armor: forged on an altar, a power per material and no ward, and half the Sage's curse escaped. */
@GameTestHolder("primordial")
@PrefixGameTestTemplate(false)
public final class ArmorImbueGameTests {
    static ItemStack forged(net.minecraft.world.item.Item armor,String affix) {return Imbuing.imbue(new ItemStack(armor),affix);}
    /** put a piece on, with its attributes applied as a tick would */
    static void wear(Player player,EquipmentSlot slot,ItemStack stack) {
        var old=player.getItemBySlot(slot);
        if(!old.isEmpty())player.getAttributes().removeAttributeModifiers(old.getAttributeModifiers(slot));
        player.setItemSlot(slot,stack);
        player.getAttributes().addTransientAttributeModifiers(stack.getAttributeModifiers(slot));
    }

    @GameTest(template="empty") public static void armorForgesAtTheAltar(GameTestHelper helper) {
        var level=helper.getLevel();var player=helper.makeMockPlayer();
        var rel=new BlockPos(1,1,1);helper.setBlock(rel,Primordial.TRANSMUTATION_ALCHEMY_ALTAR.get());var altar=(AltarBlockEntity)helper.getBlockEntity(rel);
        altar.add(new ItemStack(Items.IRON_CHESTPLATE));altar.add(new ItemStack(AffixMaterials.ITEMS.get(MobAffix.STRENGTH).get()));
        for(int i=0;i<2;i++){altar.add(new ItemStack(Primordial.ALCHEMICAL_SOURCE_INGOT.get()));altar.add(new ItemStack(Primordial.ALCHEMICAL_SOURCE_STRING.get()));altar.add(new ItemStack(Primordial.ALCHEMICAL_ESSENCE.get()));}
        helper.assertTrue(AltarCrafting.tryCraft(level,helper.absolutePos(rel),player)==AltarCrafting.Result.CRAFTED,"armor forges on a transmutation altar");
        var chest=altar.items().get(0);
        helper.assertTrue(chest.is(Items.IRON_CHESTPLATE)&&Imbuing.imbues(chest).equals(java.util.List.of("strength"))&&Imbuing.pseudoTreasure(chest),"a strength chestplate, got "+chest.getTag());
        helper.assertTrue(!Imbuing.canImbue(chest,"void"),"one affix a piece");
        // held in the hand it is no weapon: no ward, no weapon power
        player.setItemInHand(InteractionHand.MAIN_HAND,chest);
        helper.assertTrue(!AffixAccessories.wears(player,MobAffix.STRENGTH),"armor in the hand gives no accessory's ward");
        player.setItemInHand(InteractionHand.MAIN_HAND,Imbuing.imbue(new ItemStack(Items.IRON_SWORD),"strength"));
        helper.assertTrue(AffixAccessories.wears(player,MobAffix.STRENGTH),"a forged weapon still does");
        // worn, it gives its power, and the same affix on more pieces stacks
        boolean found=false;
        for(var modifier:chest.getAttributeModifiers(EquipmentSlot.CHEST).get(Attributes.ATTACK_DAMAGE))if(Math.abs(modifier.getAmount()-2)<1e-6)found=true;
        helper.assertTrue(found,"a strength chestplate gives +2 attack in its slot");
        wear(player,EquipmentSlot.CHEST,chest);wear(player,EquipmentSlot.LEGS,forged(Items.IRON_LEGGINGS,"strength"));
        helper.assertTrue(ArmorImbueEvents.pieces(player,MobAffix.STRENGTH)==2,"two pieces carry it");
        helper.assertTrue(!AffixAccessories.wears(player,MobAffix.STRENGTH)||Imbuing.isWeapon(player.getMainHandItem()),"worn armor gives no ward of its own");
        helper.succeed();
    }

    @GameTest(template="empty") public static void armorPowersWhenStruck(GameTestHelper helper) {
        var player=helper.makeMockSurvivalPlayer();
        var zombie=helper.spawn(net.minecraft.world.entity.EntityType.ZOMBIE,2,2,2);local.primordial.affix.AffixState.suppress(zombie);
        var blow=player.damageSources().mobAttack(zombie);
        // 限伤 on two pieces: no hit over 60% of max health
        wear(player,EquipmentSlot.HEAD,forged(Items.IRON_HELMET,"damage_cap"));wear(player,EquipmentSlot.FEET,forged(Items.IRON_BOOTS,"damage_cap"));
        var event=new LivingHurtEvent(player,blow,100);ArmorImbueEvents.armorStruck(event);
        helper.assertTrue(Math.abs(event.getAmount()-player.getMaxHealth()*0.6F)<0.01,"two pieces of Damage Cap: at most 60%, got "+event.getAmount());
        // 物抗 on one piece: physical harm -8%
        wear(player,EquipmentSlot.HEAD,forged(Items.IRON_HELMET,"physical_resistance"));wear(player,EquipmentSlot.FEET,new ItemStack(Items.IRON_BOOTS));
        event=new LivingHurtEvent(player,blow,10);ArmorImbueEvents.armorStruck(event);
        helper.assertTrue(Math.abs(event.getAmount()-9.2F)<0.01,"one piece of Physical Resistance: -8%, got "+event.getAmount());
        // 永生: a lethal blow cheated, once in its cooldown
        wear(player,EquipmentSlot.CHEST,forged(Items.IRON_CHESTPLATE,"undying"));
        player.setHealth(0.0001F);
        var death=new LivingDeathEvent(player,blow);ArmorImbueEvents.armorLethal(death);
        helper.assertTrue(death.isCanceled()&&player.getHealth()>=1&&player.hasEffect(net.minecraft.world.effect.MobEffects.REGENERATION),"Undying cheats death like a totem");
        death=new LivingDeathEvent(player,blow);ArmorImbueEvents.armorLethal(death);
        helper.assertTrue(!death.isCanceled(),"but not again before its cooldown");
        zombie.discard();
        helper.succeed();
    }

    @GameTest(template="empty") public static void pseudoTreasuresEscapeHalfTheCurse(GameTestHelper helper) {
        // weapons: a true treasure keeps all, a pseudo one 75%, anything else half
        helper.assertTrue(SageHeart.weaponShare(new ItemStack(Items.IRON_SWORD))==0.5F,"a plain sword: 50%");
        helper.assertTrue(SageHeart.weaponShare(Imbuing.imbue(new ItemStack(Items.IRON_SWORD),"void"))==0.75F,"a pseudo treasure: 75%");
        helper.assertTrue(SageHeart.weaponShare(forged(Items.IRON_CHESTPLATE,"void"))==0.5F,"a chestplate is no weapon");
        // armor: under the Nether's curse plain armor gives nothing, pseudo armor half
        var player=helper.makeMockPlayer();
        CuriosApi.getCuriosInventory(player).orElseThrow(IllegalStateException::new).setEquippedCurio(Primordial.SAGE_SLOT,0,new ItemStack(Primordial.SAGE_HEART.get()));
        TestSlots.tick(player);
        CompoundTag saved=player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        saved.putBoolean("primordial:sage_nether_unlocked",true);player.getPersistentData().put(Player.PERSISTED_NBT_TAG,saved);
        player.getAttribute(Attributes.LUCK).setBaseValue(1000);player.experienceLevel=100;
        wear(player,EquipmentSlot.CHEST,new ItemStack(Items.DIAMOND_CHESTPLATE));
        SageHeart.maintain(player);
        helper.assertTrue(player.getArmorValue()==0,"cursed: a plain diamond chestplate gives no armor, got "+player.getArmorValue());
        wear(player,EquipmentSlot.CHEST,forged(Items.DIAMOND_CHESTPLATE,"void"));
        SageHeart.maintain(player);
        helper.assertTrue(player.getArmorValue()==4,"cursed: a pseudo diamond chestplate keeps half its 8 armor, got "+player.getArmorValue());
        helper.succeed();
    }
}
