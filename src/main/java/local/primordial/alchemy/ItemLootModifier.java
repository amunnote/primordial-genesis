package local.primordial.alchemy;

import com.google.common.base.Suppliers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * An item of this mod found in chests (data/primordial/loot_modifiers/*_in_chests.json): it is added to a chest of one of
 * the listed loot tables with this chance. A table ending in "/*" stands for every table under it (minecraft:chests/village/*).
 */
public final class ItemLootModifier extends LootModifier {
    public static final Supplier<Codec<ItemLootModifier>> CODEC=Suppliers.memoize(()->RecordCodecBuilder.create(inst->
        codecStart(inst)
            .and(ForgeRegistries.ITEMS.getCodec().fieldOf("item").forGetter(m->m.item))
            .and(Codec.INT.optionalFieldOf("count",1).forGetter(m->m.count))
            .and(Codec.STRING.listOf().fieldOf("tables").forGetter(m->m.tables))
            .and(Codec.FLOAT.fieldOf("chance").forGetter(m->m.chance))
            .apply(inst,ItemLootModifier::new)));
    private final Item item;private final int count;private final List<String> tables;private final float chance;

    public ItemLootModifier(LootItemCondition[] conditions,Item item,int count,List<String> tables,float chance) {
        super(conditions);this.item=item;this.count=count;this.tables=tables;this.chance=chance;
    }
    /** whether a loot table is one of these (or under one ending in "/*") */
    public static boolean matches(List<String> tables,ResourceLocation id) {
        String name=id.toString();
        for(String table:tables)if(table.endsWith("/*")?name.startsWith(table.substring(0,table.length()-1)):name.equals(table))return true;
        return false;
    }
    @Override protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot,LootContext context) {
        if(matches(tables,context.getQueriedLootTableId())&&context.getRandom().nextFloat()<chance)loot.add(new ItemStack(item,count));
        return loot;
    }
    @Override public Codec<? extends IGlobalLootModifier> codec() {return CODEC.get();}
}
