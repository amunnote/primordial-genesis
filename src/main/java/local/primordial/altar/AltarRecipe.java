package local.primordial.altar;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import local.primordial.Primordial;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;

/**
 * 炼金仪炼成配方：台面上的物品与全部材料完全一致（不分顺序、不多不少）时，用空手右键炼成。
 * JSON:
 * {"type": "primordial:altar", "knowledge": "primordial:xxx" (optional), "tier": 2,
 *  "main": {"item": "..."} (required: floats in the middle of the altar while the rest circle around it),
 *  "ingredients": [{"item": "..."}, {"item": "...", "count": 8}, {"tag": "..."}], "result": {"item": "...", "count": 1}}
 * The main material is one more ingredient on top of "ingredients".
 * "distinct": true (optional) — the materials around the main one must all be different (item + NBT).
 * "random_result": {"tag": "..."} (optional) — the product is one random item of these instead of "result", never the
 * same item as the main material, e.g. turning one affix material into another. "result" then only names it in JEI.
 * "copy_main": n (optional) — the product is n copies of the main material itself, NBT included (copying a recipe).
 * "imbue": true (optional) — the main material is a weapon and one of the others an affix material: the product is the
 * weapon with that affix forged into it (see Imbuing). "result" then only names it in JEI.
 */
public final class AltarRecipe implements Recipe<Container> {
    private final ResourceLocation id;
    private final ResourceLocation knowledge;
    private final int tier;
    private final Ingredient main;
    private final NonNullList<Ingredient> others;
    private final NonNullList<Ingredient> all;
    private final ItemStack result;
    /** when set, the materials around the main one must all differ from each other (item and NBT), e.g. "7 different enchanted books" */
    private final boolean distinct;
    /** when not empty, the product is drawn from these at random (other than the main material) */
    private final Ingredient randomResult;
    /** 0, or how many copies of the main material come out */
    private final int copies;
    /** forge the affix material on the table into the weapon that is the main material */
    private final boolean imbue;

    public AltarRecipe(ResourceLocation id,ResourceLocation knowledge,int tier,Ingredient main,NonNullList<Ingredient> others,ItemStack result,boolean distinct) {
        this(id,knowledge,tier,main,others,result,distinct,Ingredient.EMPTY);
    }
    public AltarRecipe(ResourceLocation id,ResourceLocation knowledge,int tier,Ingredient main,NonNullList<Ingredient> others,ItemStack result,boolean distinct,Ingredient randomResult) {
        this(id,knowledge,tier,main,others,result,distinct,randomResult,0,false);
    }
    public AltarRecipe(ResourceLocation id,ResourceLocation knowledge,int tier,Ingredient main,NonNullList<Ingredient> others,ItemStack result,
                       boolean distinct,Ingredient randomResult,int copies,boolean imbue) {
        this.id=id;this.knowledge=knowledge;this.tier=tier;this.main=main==null?Ingredient.EMPTY:main;this.others=others;this.result=result;
        this.distinct=distinct;this.randomResult=randomResult==null?Ingredient.EMPTY:randomResult;this.copies=Math.max(0,copies);this.imbue=imbue;
        this.all=NonNullList.create();
        if(!this.main.isEmpty())all.add(this.main);
        all.addAll(others);
    }
    /** null when anyone may perform it */
    public ResourceLocation knowledge() {return knowledge;}
    public int tier() {return tier;}
    public ItemStack result() {return result;}
    public boolean hasMain() {return !main.isEmpty();}
    public Ingredient main() {return main;}
    /** the ingredients other than the main material */
    public NonNullList<Ingredient> others() {return others;}
    public boolean distinct() {return distinct;}
    public boolean randomizes() {return !randomResult.isEmpty();}
    public Ingredient randomResult() {return randomResult;}
    public int copies() {return copies;}
    public boolean imbues() {return imbue;}
    /** what JEI and the recipe tree show coming out: the random pool, the copied or forged main material, or the result */
    public List<ItemStack> shownResults() {
        if(randomizes())return List.of(randomResult.getItems());
        if(copies>0||imbue)return java.util.Arrays.stream(main.getItems()).map(s->s.copyWithCount(Math.max(1,copies))).toList();
        return List.of(result);
    }

    public ItemStack craft(Container altar,net.minecraft.util.RandomSource random) {return craft(altar,random,java.util.Set.of(),null);}
    /**
     * The product for the items on the altar: the fixed result; copies of the main material; the weapon with the affix
     * forged in; or a random pick that is not the main material and not yet in {@code used} (what this round has given
     * already). Once nothing is left, a new round starts, and its first pick is still not {@code last}.
     * {@link #roundEnded} says whether that happened.
     */
    public ItemStack craft(Container altar,net.minecraft.util.RandomSource random,java.util.Set<net.minecraft.world.item.Item> used,net.minecraft.world.item.Item last) {
        roundEnded=false;
        ItemStack mainItem=mainStack(altar);
        if(copies>0)return mainItem.copyWithCount(copies);
        if(imbue)return local.primordial.affix.Imbuing.imbue(mainItem,imbueAffix(altar));
        if(!randomizes())return result.copy();
        List<ItemStack> choices=new ArrayList<>();
        for(ItemStack choice:randomResult.getItems())if(!choice.is(mainItem.getItem())&&!used.contains(choice.getItem()))choices.add(choice);
        if(choices.isEmpty()){
            // the round is over: everything again, except what came out last
            roundEnded=true;
            for(ItemStack choice:randomResult.getItems())if(!choice.is(mainItem.getItem())&&(last==null||!choice.is(last)))choices.add(choice);
        }
        if(choices.isEmpty())return mainItem.copyWithCount(1);
        return choices.get(random.nextInt(choices.size())).copyWithCount(result.getCount());
    }
    /** set by the last craft: its random pick began a new round */
    private boolean roundEnded;
    public boolean roundEnded() {return roundEnded;}
    private ItemStack mainStack(Container altar) {
        for(ItemStack stack:items(altar))if(main.test(stack))return stack;
        return ItemStack.EMPTY;
    }
    /** the affix of the material on the table, for an imbuing recipe */
    private String imbueAffix(Container altar) {
        for(ItemStack stack:items(altar)){var affix=local.primordial.affix.Imbuing.affixOf(stack);if(affix!=null)return affix;}
        return null;
    }
    private boolean imbueOk(Container altar) {
        return !imbue||local.primordial.affix.Imbuing.canImbue(mainStack(altar),imbueAffix(altar));
    }

    /** Every item on the altar must be used by exactly one ingredient, and every ingredient must be used. */
    @Override public boolean matches(Container altar,Level level) {
        List<ItemStack> items=items(altar);
        return items.size()==all.size()&&fits(items,all)&&distinctOk(items)&&imbueOk(altar);
    }
    /** the non-main items are pairwise different, if this recipe asks for that */
    private boolean distinctOk(List<ItemStack> items) {
        if(!distinct)return true;
        List<ItemStack> rest=new ArrayList<>();
        for(ItemStack stack:items)if(!main.test(stack))rest.add(stack);
        for(int i=0;i<rest.size();i++)for(int j=i+1;j<rest.size();j++)
            if(ItemStack.isSameItemSameTags(rest.get(i),rest.get(j)))return false;
        return true;
    }
    /**
     * For display while the altar is being filled: the index of the item that is this recipe's main material, given
     * that every item already on the altar belongs to this recipe; -1 if not.
     */
    public int mainIndex(List<ItemStack> items) {
        if(main.isEmpty()||items.size()>all.size())return -1;
        for(int i=0;i<items.size();i++){
            if(!main.test(items.get(i)))continue;
            List<ItemStack> rest=new ArrayList<>(items);rest.remove(i);
            if(fits(rest,others)&&distinctOk(items))return i;
        }
        return -1;
    }
    /** every single item on the altar: a pile of 3 counts as 3 items, since crafting uses all of it */
    private static List<ItemStack> items(Container altar) {
        List<ItemStack> items=new ArrayList<>();
        // past the largest altar no recipe can match; stop before a big pile makes the matching slow
        for(int i=0;i<altar.getContainerSize();i++){var stack=altar.getItem(i);
            for(int n=0;n<stack.getCount()&&items.size()<=AltarBlockEntity.MAX_CAPACITY;n++)items.add(stack);}
        return items;
    }
    /** can each item be given its own ingredient (ingredients may be left over)? small backtracking, altars hold at most 16 */
    private static boolean fits(List<ItemStack> items,List<Ingredient> ingredients) {
        return assign(0,items,ingredients,new boolean[ingredients.size()]);
    }
    private static boolean assign(int index,List<ItemStack> items,List<Ingredient> ingredients,boolean[] used) {
        if(index==items.size())return true;
        for(int i=0;i<ingredients.size();i++){
            if(used[i]||!ingredients.get(i).test(items.get(index)))continue;
            used[i]=true;
            if(assign(index+1,items,ingredients,used))return true;
            used[i]=false;
        }
        return false;
    }
    @Override public ItemStack assemble(Container altar,RegistryAccess access) {return result.copy();}
    @Override public boolean canCraftInDimensions(int width,int height) {return true;}
    @Override public ItemStack getResultItem(RegistryAccess access) {return result;}
    /** all ingredients, main material first */
    @Override public NonNullList<Ingredient> getIngredients() {return all;}
    @Override public ResourceLocation getId() {return id;}
    @Override public boolean isSpecial() {return true;} // keep it out of the vanilla recipe book
    @Override public RecipeSerializer<?> getSerializer() {return Primordial.ALTAR_RECIPE_SERIALIZER.get();}
    @Override public RecipeType<?> getType() {return Primordial.ALTAR_RECIPE_TYPE.get();}

    public static final class Serializer implements RecipeSerializer<AltarRecipe> {
        @Override public AltarRecipe fromJson(ResourceLocation id,JsonObject json) {
            NonNullList<Ingredient> others=NonNullList.create();
            for(var element:GsonHelper.getAsJsonArray(json,"ingredients",new com.google.gson.JsonArray())){
                int count=element.isJsonObject()?GsonHelper.getAsInt(element.getAsJsonObject(),"count",1):1;
                Ingredient ingredient=Ingredient.fromJson(element,false);
                for(int i=0;i<count;i++)others.add(ingredient);
            }
            if(!json.has("main"))throw new com.google.gson.JsonParseException("Altar recipe needs a \"main\" material: "+id);
            Ingredient main=Ingredient.fromJson(json.get("main"),false);
            int total=others.size()+(main.isEmpty()?0:1);
            if(total==0||total>AltarBlockEntity.MAX_CAPACITY)
                throw new com.google.gson.JsonParseException("Altar recipe needs 1-"+AltarBlockEntity.MAX_CAPACITY+" ingredients: "+id);
            ResourceLocation knowledge=json.has("knowledge")?new ResourceLocation(GsonHelper.getAsString(json,"knowledge")):null;
            int tier=GsonHelper.getAsInt(json,"tier",1);
            Ingredient random=json.has("random_result")?Ingredient.fromJson(json.get("random_result"),false):Ingredient.EMPTY;
            return new AltarRecipe(id,knowledge,tier,main,others,ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json,"result")),
                GsonHelper.getAsBoolean(json,"distinct",false),random,GsonHelper.getAsInt(json,"copy_main",0),GsonHelper.getAsBoolean(json,"imbue",false));
        }
        @Override public AltarRecipe fromNetwork(ResourceLocation id,FriendlyByteBuf buf) {
            ResourceLocation knowledge=buf.readBoolean()?buf.readResourceLocation():null;
            int tier=buf.readVarInt();
            Ingredient main=buf.readBoolean()?Ingredient.fromNetwork(buf):Ingredient.EMPTY;
            int n=buf.readVarInt();
            NonNullList<Ingredient> others=NonNullList.create();
            for(int i=0;i<n;i++)others.add(Ingredient.fromNetwork(buf));
            ItemStack result=buf.readItem();
            boolean distinct=buf.readBoolean();
            Ingredient random=buf.readBoolean()?Ingredient.fromNetwork(buf):Ingredient.EMPTY;
            return new AltarRecipe(id,knowledge,tier,main,others,result,distinct,random,buf.readVarInt(),buf.readBoolean());
        }
        @Override public void toNetwork(FriendlyByteBuf buf,AltarRecipe recipe) {
            buf.writeBoolean(recipe.knowledge!=null);
            if(recipe.knowledge!=null)buf.writeResourceLocation(recipe.knowledge);
            buf.writeVarInt(recipe.tier);
            buf.writeBoolean(recipe.hasMain());
            if(recipe.hasMain())recipe.main.toNetwork(buf);
            buf.writeVarInt(recipe.others.size());
            for(Ingredient ingredient:recipe.others)ingredient.toNetwork(buf);
            buf.writeItem(recipe.result);
            buf.writeBoolean(recipe.distinct);
            buf.writeBoolean(recipe.randomizes());
            if(recipe.randomizes())recipe.randomResult.toNetwork(buf);
            buf.writeVarInt(recipe.copies);
            buf.writeBoolean(recipe.imbue);
        }
    }
}
