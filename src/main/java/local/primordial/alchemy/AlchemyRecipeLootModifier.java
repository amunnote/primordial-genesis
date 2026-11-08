package local.primordial.alchemy;

import com.google.common.base.Suppliers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Optional;
import java.util.function.Supplier;
import local.primordial.item.AlchemyRecipeItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;

/**
 * Adds an alchemy recipe to generated chest loot with the given chance — unless every player in the game has learned it
 * (except in the Spirit Realm, which gives recipes known or not, up to three to a chest).
 * Configured per recipe in data/primordial/loot_modifiers/*.json:
 *   "table": one loot table (e.g. "minecraft:chests/buried_treasure"); without it, any "chests/..." table.
 *   "dimension": only chests generated in that dimension (optional).
 */
public final class AlchemyRecipeLootModifier extends LootModifier {
    public static final Supplier<Codec<AlchemyRecipeLootModifier>> CODEC=Suppliers.memoize(()->RecordCodecBuilder.create(inst->
        codecStart(inst)
            .and(ResourceLocation.CODEC.fieldOf("recipe").forGetter(m->m.recipe))
            .and(ResourceLocation.CODEC.optionalFieldOf("table").forGetter(m->m.table))
            .and(ResourceLocation.CODEC.optionalFieldOf("dimension").forGetter(m->m.dimension))
            .and(Codec.FLOAT.fieldOf("chance").forGetter(m->m.chance))
            .apply(inst,AlchemyRecipeLootModifier::new)));
    private final ResourceLocation recipe;
    private final Optional<ResourceLocation> table,dimension;
    private final float chance;

    public AlchemyRecipeLootModifier(LootItemCondition[] conditions,ResourceLocation recipe,Optional<ResourceLocation> table,
                                     Optional<ResourceLocation> dimension,float chance) {
        super(conditions);this.recipe=recipe;this.table=table;this.dimension=dimension;this.chance=chance;
    }

    @Override protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot,LootContext context) {
        ResourceLocation id=context.getQueriedLootTableId();
        if(table.isPresent()?!table.get().equals(id):!id.getPath().startsWith("chests/"))return loot;
        var level=context.getLevel();
        if(dimension.isPresent()&&!level.dimension().location().equals(dimension.get()))return loot;
        // the Spirit Realm gives recipes whether or not they are known, and up to three to a chest
        boolean realm=level.dimension()==local.primordial.spirit.SpiritRealm.LEVEL;
        if(!realm&&AlchemyWorldKnowledge.get(level.getServer()).everyoneKnows(recipe))return loot;
        if(context.getRandom().nextFloat()>=chance)return loot;
        if(realm&&loot.stream().filter(s->s.getItem() instanceof AlchemyRecipeItem).count()<local.primordial.spirit.SpiritLootModifier.MAX_PER_CHEST){
            loot.add(AlchemyRecipeItem.create(recipe));return loot;}
        // At most one recipe per chest. Each recipe still rolls its own chance; when several succeed for the same
        // chest, each success replaces the kept one with probability 1/n, so every one of them is equally likely to stay.
        int successes;
        synchronized(SUCCESSES){successes=SUCCESSES.merge(context,1,Integer::sum);}
        int existing=-1;
        for(int i=0;i<loot.size();i++)if(loot.get(i).getItem() instanceof AlchemyRecipeItem){existing=i;break;}
        if(existing<0)loot.add(AlchemyRecipeItem.create(recipe));
        else if(context.getRandom().nextInt(Math.max(successes,2))==0)loot.set(existing,AlchemyRecipeItem.create(recipe));
        return loot;
    }
    /** recipe successes so far for one chest's loot roll (keyed by its loot context, dropped with it) */
    private static final java.util.Map<LootContext,Integer> SUCCESSES=new java.util.WeakHashMap<>();
    @Override public Codec<? extends IGlobalLootModifier> codec() {return CODEC.get();}
}
