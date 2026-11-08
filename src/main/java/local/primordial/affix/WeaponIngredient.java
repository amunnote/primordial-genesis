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
 * Recipe ingredient {"type":"primordial:weapon"}: any weapon, vanilla or modded, as {@link Imbuing#isWeapon} judges it,
 * so affix materials forge into weapons from other mods even when they carry no vanilla weapon tag.
 */
public final class WeaponIngredient extends AbstractIngredient {
    public static final ResourceLocation ID=new ResourceLocation(Primordial.ID,"weapon");
    public static final WeaponIngredient INSTANCE=new WeaponIngredient();
    private ItemStack[] shown;
    private WeaponIngredient() {}

    @Override public boolean test(ItemStack stack) {return stack!=null&&Imbuing.isWeapon(stack);}
    @Override public boolean isEmpty() {return false;}
    @Override public boolean isSimple() {return false;}
    /** every registered weapon, for JEI and the recipe tree */
    @Override public ItemStack[] getItems() {
        if(shown==null)shown=ForgeRegistries.ITEMS.getValues().stream().map(ItemStack::new).filter(Imbuing::isWeapon).toArray(ItemStack[]::new);
        return shown;
    }
    @Override public IIngredientSerializer<WeaponIngredient> getSerializer() {return SERIALIZER;}
    @Override public JsonElement toJson() {var json=new JsonObject();json.addProperty("type",ID.toString());return json;}

    public static final IIngredientSerializer<WeaponIngredient> SERIALIZER=new IIngredientSerializer<>() {
        @Override public WeaponIngredient parse(FriendlyByteBuf buffer) {return INSTANCE;}
        @Override public WeaponIngredient parse(JsonObject json) {return INSTANCE;}
        @Override public void write(FriendlyByteBuf buffer,WeaponIngredient ingredient) {}
    };
}
