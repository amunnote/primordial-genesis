package local.primordial.effect;

import java.util.ArrayList;
import java.util.UUID;
import local.primordial.Primordial;
import local.primordial.affix.AffixCombat;
import local.primordial.affix.AffixEvents;
import local.primordial.affix.BossAffix;
import local.primordial.affix.Imbuing;
import local.primordial.affix.MobAffix;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LootingLevelEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Pseudo alchemical treasures (伪-炼金宝具): weapons with affix materials forged in. Ordinary affixes act through
 * AffixAccessories (as if the accessory were worn) and, for the three whose accessories give attributes, through the
 * weapon's main-hand modifiers here. Boss affixes give these powers to whoever wields it:
 * Pride +50% damage to targets with a smaller share of health left; Greed +1 Looting and 5% life steal; Sloth slows and
 * tires what it hits; Envy may steal a beneficial effect; Wrath +10% damage per 10% of the wielder's health missing;
 * Gluttony feeds on kills; Lust executes non-boss targets left under 15% health.
 */
@Mod.EventBusSubscriber(modid=Primordial.ID)
public final class ImbueEvents {
    private static final UUID STRENGTH_ADD=UUID.fromString("7a0c4e21-3b5d-4f6a-8c9e-1d2f3a4b5c61"),STRENGTH_MUL=UUID.fromString("7a0c4e21-3b5d-4f6a-8c9e-1d2f3a4b5c62");
    private static final UUID KNOCKBACK=UUID.fromString("7a0c4e21-3b5d-4f6a-8c9e-1d2f3a4b5c63"),REACH=UUID.fromString("7a0c4e21-3b5d-4f6a-8c9e-1d2f3a4b5c64");
    private ImbueEvents() {}

    /** the sin powers held: those forged into the main-hand weapon, or all seven for the Seven Sins' wearer */
    private static java.util.EnumSet<BossAffix> boss(LivingEntity entity) {
        if(!(entity instanceof Player player))return java.util.EnumSet.noneOf(BossAffix.class);
        return sins(player)?java.util.EnumSet.allOf(BossAffix.class):Imbuing.heldWeaponBoss(player.getMainHandItem());
    }
    /** the Seven Sins strengthen every sin power */
    private static boolean sins(Player player) {return local.primordial.boss.SevenSinsItem.wears(player);}

    /** the Seven Sins' ward: less damage from the Sin Lords and their kin */
    @SubscribeEvent public static void sinWard(LivingHurtEvent event) {
        if(event.getSource().getEntity() instanceof local.primordial.boss.SinEntity&&event.getEntity() instanceof Player player&&sins(player))
            event.setAmount(event.getAmount()*local.primordial.boss.SevenSinsItem.SIN_WARD);
    }

    /** the attribute-giving accessories (Strength, Physical Resistance, Enlightenment) also work forged into a weapon */
    @SubscribeEvent public static void imbuedAttributes(ItemAttributeModifierEvent event) {
        if(event.getSlotType()!=EquipmentSlot.MAINHAND)return;
        var affixes=Imbuing.heldWeaponOrdinary(event.getItemStack());
        if(affixes.contains(MobAffix.STRENGTH)){
            event.addModifier(Attributes.ATTACK_DAMAGE,new AttributeModifier(STRENGTH_ADD,"primordial.imbued_strength",3,AttributeModifier.Operation.ADDITION));
            event.addModifier(Attributes.ATTACK_DAMAGE,new AttributeModifier(STRENGTH_MUL,"primordial.imbued_strength_percent",0.1,AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
        if(affixes.contains(MobAffix.PHYSICAL_RESISTANCE))
            event.addModifier(Attributes.KNOCKBACK_RESISTANCE,new AttributeModifier(KNOCKBACK,"primordial.imbued_knockback",0.5,AttributeModifier.Operation.ADDITION));
        if(affixes.contains(MobAffix.ENLIGHTENMENT))
            event.addModifier(net.minecraftforge.common.ForgeMod.BLOCK_REACH.get(),new AttributeModifier(REACH,"primordial.imbued_reach",1.5,AttributeModifier.Operation.ADDITION));
    }

    // ------------------------------------------------------------------ boss powers
    @SubscribeEvent public static void imbuedHurt(LivingHurtEvent event) {
        if(event.getEntity().level().isClientSide||AffixCombat.secondary()||!(event.getSource().getEntity() instanceof Player player))return;
        var powers=boss(player);if(powers.isEmpty())return;
        var victim=event.getEntity();float amount=event.getAmount();boolean sins=sins(player);
        if(powers.contains(BossAffix.PRIDE)&&victim.getHealth()/victim.getMaxHealth()<player.getHealth()/player.getMaxHealth())amount*=sins?2F:1.5F;
        if(powers.contains(BossAffix.WRATH)){int steps=(int)((1-player.getHealth()/player.getMaxHealth())*10);amount*=1+(sins?0.15F:0.1F)*Math.max(0,steps);}
        event.setAmount(amount);
    }
    @SubscribeEvent(priority=EventPriority.LOW) public static void imbuedDamage(LivingDamageEvent event) {
        var victim=event.getEntity();
        if(victim.level().isClientSide||AffixCombat.secondary()||event.getAmount()<=0||!(event.getSource().getEntity() instanceof Player player)||victim==player)return;
        var powers=boss(player);if(powers.isEmpty())return;boolean sins=sins(player);
        if(powers.contains(BossAffix.GREED))player.heal(event.getAmount()*(sins?0.1F:0.05F));
        if(powers.contains(BossAffix.SLOTH)){
            victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,sins?80:60,sins?2:1),player);
            victim.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN,sins?80:60,sins?2:1),player);
        }
        if(powers.contains(BossAffix.ENVY)&&player.getRandom().nextFloat()<(sins?0.3F:0.15F)){
            for(var effect:new ArrayList<>(victim.getActiveEffects()))if(effect.getEffect().getCategory()==MobEffectCategory.BENEFICIAL){
                victim.removeEffect(effect.getEffect());player.addEffect(new MobEffectInstance(effect));break;}
        }
        if(powers.contains(BossAffix.LUST)&&!AffixEvents.isBoss(victim)&&!(victim instanceof Player)
            &&victim.getHealth()-event.getAmount()<victim.getMaxHealth()*(sins?0.2F:0.15F)){
            event.setAmount(Math.max(event.getAmount(),victim.getHealth()+victim.getAbsorptionAmount()+1));
        }
    }
    @SubscribeEvent public static void imbuedKill(LivingDeathEvent event) {
        if(event.getEntity().level().isClientSide||!(event.getSource().getEntity() instanceof Player player))return;
        if(boss(player).contains(BossAffix.GLUTTONY)){
            boolean sins=sins(player);
            player.heal(event.getEntity().getMaxHealth()*(sins?0.2F:0.1F));
            player.getFoodData().eat(sins?4:2,0.4F);
        }
    }
    @SubscribeEvent public static void imbuedLooting(LootingLevelEvent event) {
        if(event.getDamageSource()!=null&&event.getDamageSource().getEntity() instanceof Player player&&boss(player).contains(BossAffix.GREED))
            event.setLootingLevel(event.getLootingLevel()+(sins(player)?2:1));
    }

    // ------------------------------------------------------------------ tooltip
    @SubscribeEvent(priority=EventPriority.HIGH) public static void imbuedTooltip(ItemTooltipEvent event) {
        var imbues=Imbuing.imbues(event.getItemStack());if(imbues.isEmpty())return;
        var lines=event.getToolTip();
        var tags=Component.empty();
        for(var id:imbues){
            if(id.startsWith(Imbuing.BOSS_PREFIX)){var boss=BossAffix.byId(id.substring(Imbuing.BOSS_PREFIX.length()));if(boss!=null)tags.append(boss.tag());}
            else{var affix=MobAffix.byId(id);if(affix!=null)tags.append(affix.tag());}
        }
        int at=Math.min(1,lines.size());
        if(Imbuing.isArmor(event.getItemStack())){
            // armor: a power for each material, no ward
            lines.add(at,Component.translatable("tooltip.primordial.armor_imbued.title").withStyle(ChatFormatting.GOLD));
            lines.add(at+1,Component.translatable("tooltip.primordial.imbued.affixes",tags).withStyle(ChatFormatting.GRAY));
            int line=at+2;
            for(var id:imbues)lines.add(line++,Component.translatable("tooltip.primordial.armor_imbued."+id.replace(Imbuing.BOSS_PREFIX,"boss_")).withStyle(ChatFormatting.DARK_PURPLE));
            lines.add(line,Component.translatable("tooltip.primordial.armor_imbued.hint").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        lines.add(at,Component.translatable("tooltip.primordial.imbued.title").withStyle(ChatFormatting.GOLD));
        lines.add(at+1,Component.translatable("tooltip.primordial.imbued.affixes",tags).withStyle(ChatFormatting.GRAY));
        lines.add(at+2,Component.translatable("tooltip.primordial.imbued.hint").withStyle(ChatFormatting.DARK_GRAY));
        for(var id:imbues)if(id.startsWith(Imbuing.BOSS_PREFIX))
            lines.add(at+3,Component.translatable("tooltip.primordial.imbued.boss."+id.substring(Imbuing.BOSS_PREFIX.length())).withStyle(ChatFormatting.DARK_PURPLE));
    }
}
