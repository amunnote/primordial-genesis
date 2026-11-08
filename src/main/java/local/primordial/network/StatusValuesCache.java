package local.primordial.network;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraftforge.registries.ForgeRegistries;

/** The server's attribute values for the local player, as last sent for the status page (client side). */
public final class StatusValuesCache {
    /** one modifier on an attribute: what it comes from, how much, and its operation (0 add, 1 multiply base, 2 multiply total) */
    public record Source(net.minecraft.network.chat.Component label,double amount,int operation) {}
    public record Entry(ResourceLocation id,double value,boolean changed,double base,List<Source> sources) {}
    private static volatile Map<ResourceLocation,Entry> entries=Map.of();
    private StatusValuesCache() {}
    static void update(List<Entry> list) {
        var map=new ConcurrentHashMap<ResourceLocation,Entry>();for(var e:list)map.put(e.id(),e);entries=map;
    }
    public static void clear() {entries=Map.of();}
    public static boolean loaded() {return !entries.isEmpty();}
    /** the server value, or the fallback when it has not arrived */
    public static double value(Attribute attribute,double fallback) {
        var e=entries.get(ForgeRegistries.ATTRIBUTES.getKey(attribute));return e==null?fallback:e.value();
    }
    public static List<Entry> changed() {return entries.values().stream().filter(Entry::changed).toList();}
    /** what the server says makes up an attribute, or null when it has not arrived */
    public static Entry entry(Attribute attribute) {return entries.get(ForgeRegistries.ATTRIBUTES.getKey(attribute));}
}
