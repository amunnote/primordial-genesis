package local.primordial.affix;

import local.primordial.Primordial;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Affixes a player has met (dealt damage to, or was attacked by, a mob carrying them).
 * Each is a hidden advancement primordial:affix/&lt;id&gt; — the Alchemist's Gift book unlocks its entry from it.
 * They have a display only because the client is told about an advancement once it is visible; they do not count
 * toward Sage's Gift growth.
 */
public final class AffixEncounters {
    public static final String CRITERION="encountered";
    private AffixEncounters() {}
    public static boolean isEncounter(ResourceLocation advancement) {
        return advancement.getNamespace().equals(Primordial.ID)&&advancement.getPath().startsWith("affix/");
    }
    public static ResourceLocation advancement(String affixId) {return new ResourceLocation(Primordial.ID,"affix/"+affixId);}
    public static void record(ServerPlayer player,AffixState state) {
        if(state==null||state.affixes.isEmpty()&&state.originalBoss==null)return;
        // the tab root must be done too, or the client drops its children
        award(player,"root");
        for(var affix:state.affixes)award(player,affix.id());
        if(state.originalBoss!=null)award(player,state.originalBoss.id());
    }
    public static boolean met(ServerPlayer player,String affixId) {
        var advancement=player.server.getAdvancements().getAdvancement(advancement(affixId));
        return advancement!=null&&player.getAdvancements().getOrStartProgress(advancement).isDone();
    }
    private static void award(ServerPlayer player,String affixId) {
        var advancement=player.server.getAdvancements().getAdvancement(advancement(affixId));
        if(advancement!=null&&!player.getAdvancements().getOrStartProgress(advancement).isDone())
            player.getAdvancements().award(advancement,CRITERION);
    }
}
