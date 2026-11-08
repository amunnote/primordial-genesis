package local.primordial.alchemy;

import com.google.gson.JsonObject;
import local.primordial.Primordial;
import local.primordial.mixin.CraftingMenuPlayerAccessor;
import local.primordial.mixin.InventoryMenuOwnerAccessor;
import local.primordial.mixin.TransientCraftingMenuAccessor;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;

/**
 * A shaped crafting-table recipe that only matches for a player who has learned the given alchemy recipe.
 * JSON: a normal "minecraft:crafting_shaped" body plus "type": "primordial:knowledge_shaped" and "knowledge": "<recipe id>".
 * Crafting without a player behind it (autocrafters, other mods' grids) never matches, so it can't be used to bypass the knowledge.
 */
public final class KnowledgeShapedRecipe extends ShapedRecipe {
    private final ResourceLocation knowledge;

    public KnowledgeShapedRecipe(ShapedRecipe base,ResourceLocation knowledge) {
        super(base.getId(),base.getGroup(),base.category(),base.getWidth(),base.getHeight(),base.getIngredients(),
            base.getResultItem(null),base.showNotification());
        this.knowledge=knowledge;
    }
    public ResourceLocation knowledge() {return knowledge;}

    @Override public boolean matches(CraftingContainer grid,Level level) {
        if(!super.matches(grid,level))return false;
        Player player=crafter(grid);
        return player!=null&&AlchemyKnowledge.knows(player,knowledge);
    }
    static Player crafter(CraftingContainer grid) {
        if(!(grid instanceof TransientCraftingContainer transientGrid))return null;
        var menu=((TransientCraftingMenuAccessor)transientGrid).angel_charm$menu();
        if(menu instanceof CraftingMenu crafting)return ((CraftingMenuPlayerAccessor)crafting).angel_charm$player();
        if(menu instanceof InventoryMenu inventory)return ((InventoryMenuOwnerAccessor)inventory).angel_charm$owner();
        return null;
    }
    @Override public RecipeSerializer<?> getSerializer() {return Primordial.KNOWLEDGE_SHAPED.get();}

    public static final class Serializer implements RecipeSerializer<KnowledgeShapedRecipe> {
        @Override public KnowledgeShapedRecipe fromJson(ResourceLocation id,JsonObject json) {
            return new KnowledgeShapedRecipe(RecipeSerializer.SHAPED_RECIPE.fromJson(id,json),
                new ResourceLocation(GsonHelper.getAsString(json,"knowledge")));
        }
        @Override public KnowledgeShapedRecipe fromNetwork(ResourceLocation id,FriendlyByteBuf buf) {
            ResourceLocation knowledge=buf.readResourceLocation();
            return new KnowledgeShapedRecipe(RecipeSerializer.SHAPED_RECIPE.fromNetwork(id,buf),knowledge);
        }
        @Override public void toNetwork(FriendlyByteBuf buf,KnowledgeShapedRecipe recipe) {
            buf.writeResourceLocation(recipe.knowledge);
            RecipeSerializer.SHAPED_RECIPE.toNetwork(buf,recipe);
        }
    }
}
