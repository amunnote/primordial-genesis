package local.primordial.affix;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import local.primordial.Primordial;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.crafting.AbstractIngredient;
import net.minecraftforge.common.crafting.IIngredientSerializer;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Recipe ingredient {"type":"primordial:armor"}: any piece of armor, vanilla or modded, as {@link Imbuing#isArmor} judges it,
 * so affix materials forge into armor from other mods too.
 */
public final class ArmorIngredient extends AbstractIngredient {
    public static final ResourceLocation ID=new ResourceLocation(Primordial.ID,"armor");
    public static final ArmorIngredient INSTANCE=new ArmorIngredient();
    private ItemStack[] shown;
    private ArmorIngredient() {}

    @Override public boolean test(ItemStack stack) {return stack!=null&&Imbuing.isArmor(stack);}
    @Override public boolean isEmpty() {return false;}
    @Override public boolean isSimple() {return false;}
    /** every registered piece of armor, for JEI and the recipe tree */
    @Override public ItemStack[] getItems() {
        if(shown==null)shown=ForgeRegistries.ITEMS.getValues().stream().map(ItemStack::new).filter(Imbuing::isArmor).toArray(ItemStack[]::new);
        return shown;
    }
    @Override public IIngredientSerializer<ArmorIngredient> getSerializer() {return SERIALIZER;}
    @Override public JsonElement toJson() {var json=new JsonObject();json.addProperty("type",ID.toString());return json;}

    public static final IIngredientSerializer<ArmorIngredient> SERIALIZER=new IIngredientSerializer<>() {
        @Override public ArmorIngredient parse(FriendlyByteBuf buffer) {return INSTANCE;}
        @Override public ArmorIngredient parse(JsonObject json) {return INSTANCE;}
        @Override public void write(FriendlyByteBuf buffer,ArmorIngredient ingredient) {}
    };
}
