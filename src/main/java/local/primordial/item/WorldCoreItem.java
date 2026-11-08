package local.primordial.item;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.List;
import java.util.UUID;
import local.primordial.Primordial;
import local.primordial.effect.WorldCores;
import local.primordial.effect.WorldCores.Realm;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeMod;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/**
 * A world core, worn in an alchemical accessory slot with 50 luck. Its attributes and steady effects live here; its
 * reactions to hits in {@link WorldCores}. Each is stronger in its own world. With no realm it is the World Core, made
 * from all three: every realm core's powers, stronger, and in every world as if at home (80 luck).
 */
public final class WorldCoreItem extends Item implements ICurioItem {
    private final Realm realm;
    public WorldCoreItem(Realm realm) {
        super(new Properties().stacksTo(1).rarity(realm==null?AlchemyCharmItem.PRIMORDIAL:Rarity.EPIC).fireResistant());this.realm=realm;
    }
    /** null for the World Core */
    public Realm realm() {return realm;}
    public boolean world() {return realm==null;}

    @Override public boolean canEquip(SlotContext context,ItemStack stack) {
        return !context.cosmetic()&&Primordial.ALCHEMICAL_ACCESSORY_SLOT.equals(context.identifier())
            &&context.entity().getAttributeValue(Attributes.LUCK)>=(world()?WorldCores.WORLD_LUCK_REQUIRED:WorldCores.LUCK_REQUIRED);
    }
    @Override public boolean canEquipFromUse(SlotContext context,ItemStack stack) {return canEquip(context,stack);}
    @Override public Multimap<Attribute,AttributeModifier> getAttributeModifiers(SlotContext context,UUID uuid,ItemStack stack) {
        Multimap<Attribute,AttributeModifier> result=HashMultimap.create();
        if(context.cosmetic())return result;
        if(world()){
            String name="primordial.world_core";
            result.put(Attributes.MAX_HEALTH,new AttributeModifier(uuid,name+"_health",0.3,AttributeModifier.Operation.MULTIPLY_TOTAL));
            result.put(Attributes.ARMOR,new AttributeModifier(uuid,name+"_armor",6,AttributeModifier.Operation.ADDITION));
            result.put(Attributes.ARMOR_TOUGHNESS,new AttributeModifier(uuid,name+"_toughness",6,AttributeModifier.Operation.ADDITION));
            result.put(Attributes.ATTACK_DAMAGE,new AttributeModifier(uuid,name+"_attack",0.25,AttributeModifier.Operation.MULTIPLY_TOTAL));
            result.put(Attributes.MOVEMENT_SPEED,new AttributeModifier(uuid,name+"_speed",0.15,AttributeModifier.Operation.MULTIPLY_BASE));
            result.put(ForgeMod.BLOCK_REACH.get(),new AttributeModifier(uuid,name+"_reach",3,AttributeModifier.Operation.ADDITION));
            result.put(ForgeMod.ENTITY_REACH.get(),new AttributeModifier(uuid,name+"_entity_reach",3,AttributeModifier.Operation.ADDITION));
            return result;
        }
        String name="primordial."+realm.coreId();
        switch(realm){
            case OVERWORLD->{
                result.put(Attributes.MAX_HEALTH,new AttributeModifier(uuid,name+"_health",0.2,AttributeModifier.Operation.MULTIPLY_TOTAL));
                result.put(Attributes.ARMOR,new AttributeModifier(uuid,name+"_armor",4,AttributeModifier.Operation.ADDITION));
                result.put(Attributes.ARMOR_TOUGHNESS,new AttributeModifier(uuid,name+"_toughness",4,AttributeModifier.Operation.ADDITION));
            }
            case NETHER->result.put(Attributes.ATTACK_DAMAGE,new AttributeModifier(uuid,name+"_attack",0.15,AttributeModifier.Operation.MULTIPLY_TOTAL));
            case END->{
                result.put(Attributes.MOVEMENT_SPEED,new AttributeModifier(uuid,name+"_speed",0.1,AttributeModifier.Operation.MULTIPLY_BASE));
                result.put(ForgeMod.BLOCK_REACH.get(),new AttributeModifier(uuid,name+"_reach",2,AttributeModifier.Operation.ADDITION));
                result.put(ForgeMod.ENTITY_REACH.get(),new AttributeModifier(uuid,name+"_entity_reach",2,AttributeModifier.Operation.ADDITION));
            }
        }
        return result;
    }
    @Override public void curioTick(SlotContext context,ItemStack stack) {
        var entity=context.entity();
        if(context.cosmetic()||entity.level().isClientSide||!entity.isAlive()||entity.tickCount%20!=0)return;
        if(world()){
            entity.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED,60,1,true,false,true));
            entity.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE,60,0,true,false,true));
            if(entity.tickCount%40==0)entity.heal(entity.getMaxHealth()*0.03F);
            return;
        }
        switch(realm){
            case OVERWORLD->{
                entity.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED,60,1,true,false,true));
                if(WorldCores.atHome(entity,realm)&&entity.tickCount%40==0)entity.heal(entity.getMaxHealth()*0.02F);
            }
            case NETHER->entity.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE,60,0,true,false,true));
            case END->{}
        }
    }
    @Override public void onUnequip(SlotContext context,ItemStack replacement,ItemStack stack) {
        var entity=context.entity();
        if(!context.cosmetic()&&entity.getHealth()>entity.getMaxHealth())entity.setHealth(entity.getMaxHealth());
    }
    @Override public boolean isFoil(ItemStack stack) {return true;}
    @Override public List<Component> getAttributesTooltip(List<Component> tooltips,ItemStack stack) {return List.of();}
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> lines,TooltipFlag flag) {
        String key="tooltip.primordial."+(world()?"world_core":realm.coreId());
        int color=world()?0xFFD66A:realm.color;
        lines.add(Component.translatable("tooltip.primordial.world_core.requirement",(int)(world()?WorldCores.WORLD_LUCK_REQUIRED:WorldCores.LUCK_REQUIRED)).withStyle(ChatFormatting.DARK_GREEN));
        if(world()){
            lines.add(Component.translatable(key+".stats").withStyle(style->style.withColor(color)));
            lines.add(Component.translatable(key+".effect").withStyle(style->style.withColor(color)));
            lines.add(Component.translatable(key+".strike").withStyle(style->style.withColor(color)));
            lines.add(Component.translatable(key+".home").withStyle(ChatFormatting.GOLD));
            lines.add(Component.translatable("tooltip.primordial.affix_accessory.slot").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        lines.add(Component.translatable(key+".stats").withStyle(style->style.withColor(realm.color)));
        lines.add(Component.translatable(key+".effect").withStyle(style->style.withColor(realm.color)));
        lines.add(Component.translatable(key+".home").withStyle(ChatFormatting.GOLD));
        lines.add(Component.translatable("tooltip.primordial.affix_accessory.slot").withStyle(ChatFormatting.DARK_GRAY));
    }
}
