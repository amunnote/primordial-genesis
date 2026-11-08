package local.primordial.spirit;

import com.google.common.base.Suppliers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.function.Supplier;
import local.primordial.item.AlchemyRecipeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;

/** Chests in the Spirit Realm may hold extra alchemy recipes, known or not: each place up to {@link #MAX_PER_CHEST} rolls its chance. */
public final class SpiritLootModifier extends LootModifier {
    public static final Supplier<Codec<SpiritLootModifier>> CODEC=Suppliers.memoize(()->RecordCodecBuilder.create(i->
        codecStart(i).and(Codec.FLOAT.fieldOf("chance").forGetter(m->m.chance)).apply(i,SpiritLootModifier::new)));
    /** recipes a chest in the realm may hold */
    public static final int MAX_PER_CHEST=3;
    private final float chance;
    public SpiritLootModifier(LootItemCondition[] conditions,float chance) {super(conditions);this.chance=chance;}

    @Override protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot,LootContext context) {
        if(!context.getQueriedLootTableId().getPath().startsWith("chests/")||context.getLevel().dimension()!=SpiritRealm.LEVEL)return loot;
        int held=(int)loot.stream().filter(s->s.getItem() instanceof AlchemyRecipeItem).count();
        for(int i=held;i<MAX_PER_CHEST;i++){
            if(context.getRandom().nextFloat()>=chance)continue;
            var recipe=SpiritRealm.realmRecipe(context.getLevel().getServer(),context.getRandom());
            if(recipe!=null)loot.add(AlchemyRecipeItem.create(recipe));
        }
        return loot;
    }
    @Override public Codec<? extends IGlobalLootModifier> codec() {return CODEC.get();}
}
