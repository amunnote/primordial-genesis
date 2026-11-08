package local.primordial.guide;
import local.primordial.alchemy.AlchemyKnowledge;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
/** Shared, conservative classification; findable recipes share the Spirit Realm random pool. */
public final class RecipeSource {
    public enum Kind { BOSS, EPIPHANY, REALM, UNKNOWN }
    public static Kind kind(ResourceLocation id){
        if(AlchemyKnowledge.bossOnly(id))return Kind.BOSS;
        if(AlchemyKnowledge.recipes().contains(id))return AlchemyKnowledge.findable(id)?Kind.REALM:Kind.EPIPHANY;
        return Kind.UNKNOWN;
    }
    public static Component describe(ResourceLocation id){return Component.translatable("guide.primordial.source."+kind(id).name().toLowerCase(java.util.Locale.ROOT));}
}
