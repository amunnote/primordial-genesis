package local.primordial.affix;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import local.primordial.effect.InventoryProtection;
import local.primordial.effect.Protection;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import top.theillusivec4.curios.api.CuriosApi;

/** Server-world escrow and paired modifiers survive logout/restarts without duplicating items. */
public final class AffixLedger extends SavedData {
    private final List<CompoundTag> entries=new ArrayList<>();
    private final java.util.Map<UUID,CompoundTag> children=new java.util.HashMap<>();
    private final java.util.Set<UUID> retiredChildren=new java.util.HashSet<>();
    private static final UUID PROBE=UUID.fromString("c0b9d0e1-b107-4819-92b1-2c37b9720f01");
    public static AffixLedger get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(AffixLedger::load,AffixLedger::new,"angel_charm_affix_ledger");
    }
    public static AffixLedger load(CompoundTag tag) {
        var ledger=new AffixLedger();for(var entry:tag.getList("entries",Tag.TAG_COMPOUND))ledger.entries.add((CompoundTag)entry);
        for(var entry:tag.getList("children",Tag.TAG_COMPOUND)){var child=(CompoundTag)entry;ledger.children.put(child.getUUID("child"),child);}
        for(var entry:tag.getList("retiredChildren",Tag.TAG_STRING))try{ledger.retiredChildren.add(UUID.fromString(entry.getAsString()));}catch(IllegalArgumentException ignored){}
        return ledger;
    }
    @Override public CompoundTag save(CompoundTag tag) {
        var list=new ListTag();entries.forEach(list::add);tag.put("entries",list);
        var offspring=new ListTag();children.values().forEach(offspring::add);tag.put("children",offspring);
        var retired=new ListTag();retiredChildren.forEach(id->retired.add(net.minecraft.nbt.StringTag.valueOf(id.toString())));tag.put("retiredChildren",retired);return tag;
    }
    private static LivingEntity resolve(MinecraftServer server,UUID uuid) {
        for(var level:server.getAllLevels())if(level.getEntity(uuid) instanceof LivingEntity living)return living;return null;
    }
    private static Attribute attribute(String name) {
        return switch(name){case "health"->Attributes.MAX_HEALTH;case "armor"->Attributes.ARMOR;default->Attributes.ATTACK_DAMAGE;};
    }
    private static void modifier(LivingEntity entity,Attribute attribute,UUID id,double amount) {
        if(entity==null)return;var instance=entity.getAttribute(attribute);if(instance==null)return;
        var previous=instance.getModifier(id);
        if(previous!=null&&Math.abs(previous.getAmount()-amount)<0.000001)return;
        instance.removeModifier(id);if(amount!=0)instance.addTransientModifier(new AttributeModifier(id,"primordial.encounter",amount,AttributeModifier.Operation.ADDITION));
        if(entity.getHealth()>entity.getMaxHealth())entity.setHealth(entity.getMaxHealth());
    }
    private static double rawAddition(LivingEntity entity,Attribute attribute,double effectiveAmount) {
        var instance=entity.getAttribute(attribute);if(instance==null)return 0;
        double before=instance.getValue();instance.addTransientModifier(new AttributeModifier(PROBE,"primordial.probe",1,AttributeModifier.Operation.ADDITION));
        double scale=instance.getValue()-before;instance.removeModifier(PROBE);
        return scale>0.000001?effectiveAmount/scale:0;
    }
    public void steal(Mob boss,LivingEntity victim) {
        if(Protection.active(victim))return;
        String name=List.of("health","attack","armor").get(boss.getRandom().nextInt(3));var attribute=attribute(name);
        if(victim.getAttribute(attribute)==null||boss.getAttribute(attribute)==null)return;
        double value=victim.getAttributeValue(attribute),amount=Math.min(value*0.03,Math.max(0,value-(name.equals("health")?1:0)));
        if(amount<=0)return;double victimRaw=rawAddition(victim,attribute,-amount),bossRaw=rawAddition(boss,attribute,amount);
        if(victimRaw==0||bossRaw==0)return;
        CompoundTag entry=entries.stream().filter(e->!e.getBoolean("released")&&e.getString("kind").equals("steal")
            &&e.getUUID("boss").equals(boss.getUUID())&&e.getUUID("victim").equals(victim.getUUID())&&e.getString("stat").equals(name)).findFirst().orElse(null);
        if(entry==null){entry=new CompoundTag();entry.putString("kind","steal");entry.putString("stat",name);
            entry.putUUID("boss",boss.getUUID());entry.putUUID("victim",victim.getUUID());entry.putUUID("modifier",UUID.randomUUID());entries.add(entry);}
        entry.putDouble("victimAmount",entry.getDouble("victimAmount")+victimRaw);entry.putDouble("bossAmount",entry.getDouble("bossAmount")+bossRaw);
        modifier(victim,attribute,entry.getUUID("modifier"),entry.getDouble("victimAmount"));
        modifier(boss,attribute,entry.getUUID("modifier"),entry.getDouble("bossAmount"));setDirty();
    }
    private record Candidate(String slot,int index,ItemStack stack,Runnable remove,double armor,double attack) {}
    public void disable(Mob boss,LivingEntity victim) {
        if(Protection.active(victim))return;var candidates=new ArrayList<Candidate>();
        for(var slot:EquipmentSlot.values()) {
            ItemStack stack=victim.getItemBySlot(slot);if(stack.isEmpty())continue;
            double armor=contribution(stack,slot,Attributes.ARMOR),attack=contribution(stack,slot,Attributes.ATTACK_DAMAGE);
            candidates.add(new Candidate("equipment:"+slot.getName(),0,stack.copy(),()->victim.setItemSlot(slot,ItemStack.EMPTY),armor,attack));
        }
        CuriosApi.getCuriosInventory(victim).ifPresent(inventory->inventory.getCurios().forEach((id,handler)->{
            for(int index=0;index<handler.getStacks().getSlots();index++) {
                int slotIndex=index;ItemStack stack=handler.getStacks().getStackInSlot(index);if(stack.isEmpty())continue;
                candidates.add(new Candidate("curio:"+id,index,stack.copy(),()->handler.getStacks().setStackInSlot(slotIndex,ItemStack.EMPTY),0,0));
            }
        }));
        if(candidates.isEmpty())return;var selected=candidates.get(boss.getRandom().nextInt(candidates.size()));
        var entry=new CompoundTag();entry.putString("kind","disabled");entry.putUUID("boss",boss.getUUID());entry.putUUID("victim",victim.getUUID());
        entry.putUUID("modifier",UUID.randomUUID());entry.putString("slot",selected.slot);entry.putInt("index",selected.index);
        entry.put("stack",selected.stack.save(new CompoundTag()));entry.putDouble("armor",selected.armor);entry.putDouble("attack",selected.attack);
        entries.add(entry);setDirty();selected.remove.run();
        modifier(boss,Attributes.ARMOR,entry.getUUID("modifier"),selected.armor);
        modifier(boss,Attributes.ATTACK_DAMAGE,entry.getUUID("modifier"),selected.attack);
    }
    private static double contribution(ItemStack stack,EquipmentSlot slot,Attribute attribute) {
        double value=0;for(var modifier:stack.getAttributeModifiers(slot).get(attribute))
            if(modifier.getOperation()==AttributeModifier.Operation.ADDITION)value+=modifier.getAmount();return Math.max(0,value);
    }
    public void release(MinecraftServer server,UUID entity) {
        for(var entry:entries)if(entry.getUUID("boss").equals(entity)||entry.getUUID("victim").equals(entity))entry.putBoolean("released",true);
        refresh(server);setDirty();
    }
    public void refresh(MinecraftServer server) {
        var iterator=entries.iterator();
        while(iterator.hasNext()) {
            var entry=iterator.next();var boss=resolve(server,entry.getUUID("boss"));var victim=resolve(server,entry.getUUID("victim"));
            if(victim!=null&&Protection.active(victim))entry.putBoolean("released",true);
            var id=entry.getUUID("modifier");boolean released=entry.getBoolean("released");
            if(entry.getString("kind").equals("steal")) {
                var attribute=attribute(entry.getString("stat"));modifier(boss,attribute,id,released?0:entry.getDouble("bossAmount"));
                modifier(victim,attribute,id,released?0:entry.getDouble("victimAmount"));
                // Transient modifiers are not saved. A released record needs no unloaded-entity repair.
                if(released){iterator.remove();setDirty();}
            } else {
                modifier(boss,Attributes.ARMOR,id,released?0:entry.getDouble("armor"));
                modifier(boss,Attributes.ATTACK_DAMAGE,id,released?0:entry.getDouble("attack"));
                if(released&&victim!=null&&restore(victim,entry)){iterator.remove();setDirty();}
            }
        }
    }
    private static boolean restore(LivingEntity victim,CompoundTag entry) {
        ItemStack stack=ItemStack.of(entry.getCompound("stack"));if(stack.isEmpty())return true;
        String slot=entry.getString("slot");final boolean[] restored={false};
        InventoryProtection.userEdit(()->{
            if(slot.startsWith("equipment:")) {
                var equipment=EquipmentSlot.byName(slot.substring(10));
                if(victim.getItemBySlot(equipment).isEmpty()){victim.setItemSlot(equipment,stack);restored[0]=true;}
            } else if(slot.startsWith("curio:")) {
                CuriosApi.getCuriosInventory(victim).ifPresent(inventory->inventory.getStacksHandler(slot.substring(6)).ifPresent(handler->{
                    int index=entry.getInt("index");if(index<handler.getStacks().getSlots()&&handler.getStacks().getStackInSlot(index).isEmpty()) {
                        handler.getStacks().setStackInSlot(index,stack);restored[0]=true;
                    }
                }));
            }
            if(!restored[0]&&victim instanceof Player player) {
                player.getInventory().add(stack);if(!stack.isEmpty()) {
                    var dropped=InventoryProtection.userDrop(()->player.drop(stack.copy(),false,true));if(dropped!=null)stack.setCount(0);
                }
                restored[0]=stack.isEmpty();
                if(!restored[0])entry.put("stack",stack.save(new CompoundTag()));
            }
        });return restored[0];
    }
    public int activeEntries() {return entries.size();}
    public long summonCount(UUID owner) {return children.values().stream().filter(child->child.getUUID("owner").equals(owner)&&child.getBoolean("summoned")).count();}
    public void trackChild(UUID owner,Mob child,boolean summoned) {
        var entry=new CompoundTag();entry.putUUID("owner",owner);entry.putUUID("child",child.getUUID());entry.putBoolean("summoned",summoned);
        children.put(child.getUUID(),entry);setDirty();
    }
    public void forgetChild(UUID child) {if(children.remove(child)!=null|retiredChildren.remove(child))setDirty();}
    public boolean retiredChild(UUID child) {return retiredChildren.contains(child);}
    public void cleanupChildren(MinecraftServer server,UUID owner) {
        var ids=children.values().stream().filter(child->child.getUUID("owner").equals(owner)).map(child->child.getUUID("child")).toList();
        for(var id:ids){children.remove(id);retiredChildren.add(id);var child=resolve(server,id);if(child!=null)child.discard();}setDirty();
    }
}
