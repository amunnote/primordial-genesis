package local.primordial.client;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Predicate;
import local.primordial.Primordial;
import local.primordial.alchemy.KnowledgeShapedRecipe;
import local.primordial.altar.AltarRecipe;
import local.primordial.mixin.SmithingTransformAccessor;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;

/**
 * For one item: every recipe that makes it and every recipe that uses it, from all recipe types (crafting, furnaces,
 * smithing, stonecutting, the alchemy altars...). Alchemy recipes the player has not learned come back locked.
 */
public final class RecipeMap {
    /** one input slot of a recipe: the items that fit it, and how many of that slot the recipe has */
    public record Input(List<ItemStack> choices,int count) {}
    /** a recipe as the map shows it: what goes in, where it is made, what comes out */
    public record Link(ResourceLocation id,ItemStack station,List<Input> inputs,List<ItemStack> outputs,boolean locked) {}
    public record Result(ItemStack focus,List<Link> madeFrom,List<Link> usedIn) {}
    private RecipeMap() {}

    public static Result of(ItemStack focus,RecipeManager recipes,RegistryAccess access,Predicate<ResourceLocation> knows) {
        List<Link> madeFrom=new ArrayList<>(),usedIn=new ArrayList<>();
        for(Recipe<?> recipe:recipes.getRecipes()){
            try{
                var ingredients=ingredients(recipe);
                var link=link(recipe,ingredients,access,knows);
                if(link==null)continue;
                if(link.outputs.stream().anyMatch(out->ItemStack.isSameItem(out,focus)))madeFrom.add(link);
                if(ingredients.stream().anyMatch(ingredient->!ingredient.isEmpty()&&ingredient.test(focus)))usedIn.add(link);
            }catch(RuntimeException ignored){
                // a modded recipe that cannot describe itself outside a real crafting grid; leave it off the map
            }
        }
        fragmentLinks(focus,recipes,knows,madeFrom,usedIn);
        // learned and ordinary recipes first, locked alchemy last
        madeFrom.sort((a,b)->Boolean.compare(a.locked,b.locked));
        usedIn.sort((a,b)->Boolean.compare(a.locked,b.locked));
        return new Result(focus.copyWithCount(1),madeFrom,usedIn);
    }

    private static final ResourceLocation FRAGMENT_LINK=new ResourceLocation(Primordial.ID,"recipe_fragmenting");
    /**
     * Breaking a learned recipe down into a fragment is hard-coded altar behaviour, not a recipe object, so the
     * recipe manager never lists it: add the link by hand. A learned recipe item points at its fragment on the
     * right (locked until learned); a fragment points back at every learned recipe of its tier on the left.
     */
    private static void fragmentLinks(ItemStack focus,RecipeManager recipes,Predicate<ResourceLocation> knows,
                                      List<Link> madeFrom,List<Link> usedIn) {
        if(focus.is(Primordial.ALCHEMY_RECIPE.get())){
            var recipe=local.primordial.item.AlchemyRecipeItem.recipe(focus);
            if(recipe==null)return;
            int tier=altarTier(recipes,recipe);
            if(tier<1||tier>local.primordial.alchemy.RecipeTiers.FRAGMENT_MAX)return;
            usedIn.add(new Link(FRAGMENT_LINK,altarItem(tier),
                List.of(new Input(List.of(focus.copyWithCount(1)),1)),
                List.of(local.primordial.item.RecipeFragmentItem.create(tier)),!knows.test(recipe)));
        }else if(focus.is(Primordial.RECIPE_FRAGMENT.get())){
            int tier=local.primordial.item.RecipeFragmentItem.tier(focus);
            if(tier<1||tier>local.primordial.alchemy.RecipeTiers.FRAGMENT_MAX)return;
            var choices=new ArrayList<ItemStack>();
            for(Recipe<?> r:recipes.getRecipes())
                if(r instanceof AltarRecipe altar&&altar.tier()==tier&&altar.knowledge()!=null&&knows.test(altar.knowledge()))
                    choices.add(local.primordial.item.AlchemyRecipeItem.create(altar.knowledge()));
            if(choices.isEmpty())return;
            madeFrom.add(new Link(FRAGMENT_LINK,altarItem(tier),
                List.of(new Input(List.copyOf(choices),1)),
                List.of(local.primordial.item.RecipeFragmentItem.create(tier)),false));
        }
    }
    private static int altarTier(RecipeManager recipes,ResourceLocation knowledge) {
        for(Recipe<?> r:recipes.getRecipes())
            if(r instanceof AltarRecipe altar&&knowledge.equals(altar.knowledge()))return altar.tier();
        return 0;
    }

    /** every item that some recipe makes or uses, for the recipe manager it was built from */
    private static java.lang.ref.WeakReference<RecipeManager> indexedFor=new java.lang.ref.WeakReference<>(null);
    private static java.util.Set<net.minecraft.world.item.Item> indexed=java.util.Set.of();
    /** does this item have a recipe tree at all (is it made by, or used in, any recipe)? */
    public static synchronized boolean hasAny(ItemStack stack,RecipeManager recipes,RegistryAccess access) {
        // recipe items and fragments reach the map through the fragment conversion, not a recipe object
        if(stack.is(Primordial.ALCHEMY_RECIPE.get())||stack.is(Primordial.RECIPE_FRAGMENT.get()))return true;
        if(indexedFor.get()!=recipes){
            var items=new java.util.HashSet<net.minecraft.world.item.Item>();
            for(Recipe<?> recipe:recipes.getRecipes()){
                try{
                    var ingredients=ingredients(recipe);
                    var link=link(recipe,ingredients,access,id->true);
                    if(link==null)continue;
                    for(var out:link.outputs())items.add(out.getItem());
                    for(var input:link.inputs())for(var choice:input.choices())items.add(choice.getItem());
                }catch(RuntimeException ignored){}
            }
            indexed=items;indexedFor=new java.lang.ref.WeakReference<>(recipes);
        }
        return indexed.contains(stack.getItem());
    }

    /** the inputs in order: an altar's main material first; a smithing upgrade's template, base and addition */
    static List<Ingredient> ingredients(Recipe<?> recipe) {
        if(recipe instanceof SmithingTransformRecipe smithing){
            var parts=(SmithingTransformAccessor)smithing;
            return List.of(parts.angel$template(),parts.angel$base(),parts.angel$addition());
        }
        return recipe.getIngredients();
    }
    static Link link(Recipe<?> recipe,List<Ingredient> ingredients,RegistryAccess access,Predicate<ResourceLocation> knows) {
        ItemStack station=recipe.getToastSymbol();
        List<ItemStack> outputs;
        ResourceLocation knowledge=null;
        if(recipe instanceof AltarRecipe altar){
            station=altarItem(altar.tier());knowledge=altar.knowledge();
            outputs=altar.shownResults();
        }else{
            outputs=List.of(recipe.getResultItem(access));
            if(recipe instanceof KnowledgeShapedRecipe shaped)knowledge=shaped.knowledge();
        }
        outputs=outputs.stream().filter(stack->!stack.isEmpty()).toList();
        if(outputs.isEmpty())return null;
        // the same ingredient in several slots becomes one input with a count
        var grouped=new LinkedHashMap<String,Input>();
        for(var ingredient:ingredients){
            if(ingredient.isEmpty())continue;
            var choices=List.of(ingredient.getItems());
            if(choices.isEmpty())continue;
            String key=choices.stream().map(stack->stack.getItem().toString()+stack.getTag()).reduce("",(a,b)->a+"|"+b);
            grouped.merge(key,new Input(choices,1),(a,b)->new Input(a.choices,a.count+1));
        }
        if(grouped.isEmpty())return null;
        return new Link(recipe.getId(),station,List.copyOf(grouped.values()),outputs,knowledge!=null&&!knows.test(knowledge));
    }
    static ItemStack altarItem(int tier) {
        return switch(Math.max(1,Math.min(4,tier))){
            case 1->new ItemStack(Primordial.BASIC_ALCHEMY_ALTAR_ITEM.get());
            case 2->new ItemStack(Primordial.TRANSMUTATION_ALCHEMY_ALTAR_ITEM.get());
            case 3->new ItemStack(Primordial.TRUTH_ALCHEMY_ALTAR_ITEM.get());
            default->new ItemStack(Primordial.PRIMORDIAL_ALCHEMY_ALTAR_ITEM.get());
        };
    }
}
