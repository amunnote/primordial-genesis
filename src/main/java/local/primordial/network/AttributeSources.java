package local.primordial.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * Where a player's attribute comes from, for the status page: each modifier on it named by its source. What the player
 * wears or holds is named by the item, what is in a Curios slot likewise, an active effect by the effect; anything else
 * by its modifier's name, through the lang key attribute_source.&lt;name&gt; (the raw name when there is none).
 */
public final class AttributeSources {
    private AttributeSources() {}
    public static List<StatusValuesCache.Source> describe(ServerPlayer player,AttributeInstance instance) {
        var attribute=instance.getAttribute();
        Map<UUID,Component> named=new HashMap<>();
        for(var slot:EquipmentSlot.values()){
            var stack=player.getItemBySlot(slot);if(stack.isEmpty())continue;
            for(var modifier:stack.getAttributeModifiers(slot).get(attribute))named.putIfAbsent(modifier.getId(),stack.getHoverName());
        }
        CuriosApi.getCuriosInventory(player).ifPresent(inventory->{for(var found:inventory.findCurios(stack->!stack.isEmpty())){
            var context=found.slotContext();var uuid=CuriosApi.getSlotUuid(context);
            for(var modifier:CuriosApi.getAttributeModifiers(context,uuid,found.stack()).get(attribute))named.putIfAbsent(modifier.getId(),found.stack().getHoverName());
        }});
        for(var effect:player.getActiveEffects())
            for(var entry:effect.getEffect().getAttributeModifiers().entrySet())if(entry.getKey()==attribute)
                named.putIfAbsent(entry.getValue().getId(),Component.translatable("screen.primordial.status.source.effect",Component.translatable(effect.getDescriptionId())));
        var out=new ArrayList<StatusValuesCache.Source>();
        for(var modifier:instance.getModifiers()){
            var label=named.get(modifier.getId());
            if(label==null)label=Component.translatableWithFallback("attribute_source."+modifier.getName().replace(' ','_'),modifier.getName());
            out.add(new StatusValuesCache.Source(label,modifier.getAmount(),modifier.getOperation().toValue()));
        }
        return out;
    }
}
