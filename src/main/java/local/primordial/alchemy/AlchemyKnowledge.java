package local.primordial.alchemy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import local.primordial.Primordial;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Alchemy recipes a player has learned by reading recipe items. Stored on the player (persisted across death),
 * keyed by recipe id. The recipe list itself lives in {@link #RECIPES}; add new alchemy recipes there.
 */
@Mod.EventBusSubscriber(modid=Primordial.ID)
public final class AlchemyKnowledge {
    private static final String KEY="primordial:alchemy_knowledge";
    /** Every alchemy recipe that can appear on a recipe item, in creative-tab order. Name: lang key "alchemy_recipe.<namespace>.<path>". */
    private static final Set<ResourceLocation> RECIPES=new LinkedHashSet<>();
    public static final ResourceLocation FINE_GOLD_INGOT=register(new ResourceLocation(Primordial.ID,"fine_gold_ingot"));
    public static final ResourceLocation TIDE_WARD_TALISMAN=register(new ResourceLocation(Primordial.ID,"tide_ward_talisman"));
    public static final ResourceLocation KINDLING_TALISMAN=register(new ResourceLocation(Primordial.ID,"kindling_talisman"));
    public static final ResourceLocation THUNDER_TALISMAN=register(new ResourceLocation(Primordial.ID,"thunder_talisman"));
    public static final ResourceLocation TRANSMUTATION_ALTAR=register(new ResourceLocation(Primordial.ID,"transmutation_alchemy_altar"));
    public static final ResourceLocation TRUTH_ALTAR=register(new ResourceLocation(Primordial.ID,"truth_alchemy_altar"));
    public static final ResourceLocation ORIGIN_ORB=register(new ResourceLocation(Primordial.ID,"origin_orb"));
    public static final ResourceLocation ORIGIN_INGOT=register(new ResourceLocation(Primordial.ID,"origin_ingot"));
    public static final ResourceLocation PRIMORDIAL_ALTAR=register(new ResourceLocation(Primordial.ID,"primordial_alchemy_altar"));
    /** learned by epiphany on first killing a mob with affixes */
    public static final ResourceLocation TRANSMUTATION_SOURCE=register(new ResourceLocation(Primordial.ID,"transmutation_source"));
    /** learned by epiphany once {@link #TRUTH_THRESHOLD} other recipes are known */
    public static final ResourceLocation TRUTH_ESSENCE=register(new ResourceLocation(Primordial.ID,"truth_essence"));
    public static final ResourceLocation BREAKING_EDGE_CHARM=register(new ResourceLocation(Primordial.ID,"breaking_edge_charm"));
    public static final ResourceLocation CONQUEST_CHARM=register(new ResourceLocation(Primordial.ID,"conquest_charm"));
    public static final ResourceLocation SPELLBREAKER_CHARM=register(new ResourceLocation(Primordial.ID,"spellbreaker_charm"));
    public static final ResourceLocation ARCANE_PACT_CHARM=register(new ResourceLocation(Primordial.ID,"arcane_pact_charm"));
    public static final ResourceLocation HUNDRED_FORGED_CHARM=register(new ResourceLocation(Primordial.ID,"hundred_forged_charm"));
    public static final ResourceLocation BULWARK_PACT_CHARM=register(new ResourceLocation(Primordial.ID,"bulwark_pact_charm"));
    public static final ResourceLocation CRYSTAL_VESTMENT_CHARM=register(new ResourceLocation(Primordial.ID,"crystal_vestment_charm"));
    public static final ResourceLocation ASTRAL_PACT_CHARM=register(new ResourceLocation(Primordial.ID,"astral_pact_charm"));
    public static final ResourceLocation SWIFT_PACT_CHARM=register(new ResourceLocation(Primordial.ID,"swift_pact_charm"));
    public static final ResourceLocation SWIFTSTRING_PACT_CHARM=register(new ResourceLocation(Primordial.ID,"swiftstring_pact_charm"));
    public static final ResourceLocation LAW_OF_ARMS_CHARM=register(new ResourceLocation(Primordial.ID,"law_of_arms_charm"));
    public static final ResourceLocation LAW_OF_ARCANA_CHARM=register(new ResourceLocation(Primordial.ID,"law_of_arcana_charm"));
    public static final ResourceLocation LAW_OF_GALE_CHARM=register(new ResourceLocation(Primordial.ID,"law_of_gale_charm"));
    public static final ResourceLocation LAW_OF_TAILWIND_CHARM=register(new ResourceLocation(Primordial.ID,"law_of_tailwind_charm"));
    public static final ResourceLocation UNYIELDING_BODY_CHARM=register(new ResourceLocation(Primordial.ID,"unyielding_body_charm"));
    public static final ResourceLocation ARCANE_HARMONY_CHARM=register(new ResourceLocation(Primordial.ID,"arcane_harmony_charm"));
    /** the primordial charms: their recipes are carried only by the Sin Lords, never found at random */
    private static final Set<ResourceLocation> BOSS_ONLY=new LinkedHashSet<>();
    public static ResourceLocation registerBossOnly(ResourceLocation id) {BOSS_ONLY.add(id);return register(id);}
    public static boolean bossOnly(ResourceLocation id) {return BOSS_ONLY.contains(id);}
    public static List<ResourceLocation> bossOnlyRecipes() {return List.copyOf(BOSS_ONLY);}
    /** the world cores: their recipes come only by epiphany, on felling the realm's boss */
    private static final Set<ResourceLocation> EPIPHANY_ONLY=new LinkedHashSet<>();
    public static ResourceLocation registerEpiphanyOnly(ResourceLocation id) {EPIPHANY_ONLY.add(id);return register(id);}
    public static boolean epiphanyOnly(ResourceLocation id) {return EPIPHANY_ONLY.contains(id);}
    /** can this recipe turn up at random (chests, the spirit realm)? */
    public static boolean findable(ResourceLocation id) {return !BOSS_ONLY.contains(id)&&!EPIPHANY_ONLY.contains(id);}
    public static final ResourceLocation OMNI_AMPLIFY_CHARM=registerBossOnly(new ResourceLocation(Primordial.ID,"omni_amplify_charm"));
    public static final ResourceLocation FLEETING_ETERNITY_CHARM=registerBossOnly(new ResourceLocation(Primordial.ID,"fleeting_eternity_charm"));
    public static final ResourceLocation UNBREAKABLE_CHARM=registerBossOnly(new ResourceLocation(Primordial.ID,"unbreakable_charm"));
    public static final ResourceLocation SPELL_ANNUL_CHARM=registerBossOnly(new ResourceLocation(Primordial.ID,"spell_annul_charm"));
    /** copying an unlearned recipe on a basic altar */
    public static final ResourceLocation ALCHEMY_RECIPE_COPY=register(new ResourceLocation(Primordial.ID,"alchemy_recipe_copy"));
    public static final int TRUTH_THRESHOLD=12;
    /** what the local player knows, as last sent by the server (client side only; read by JEI) */
    private static volatile Set<ResourceLocation> clientKnown=Set.of();
    private AlchemyKnowledge() {}

    public static void updateClient(List<ResourceLocation> known) {clientKnown=Set.copyOf(known);}
    public static boolean clientKnows(ResourceLocation recipe) {return recipe==null||clientKnown.contains(recipe);}

    public static ResourceLocation register(ResourceLocation id) {RECIPES.add(id);return id;}
    public static List<ResourceLocation> recipes() {return Collections.unmodifiableList(new ArrayList<>(RECIPES));}
    /** the recipes of the affix accessories, which alchemical creations drop; registered with {@link #registerAffixAccessory} */
    private static final Set<ResourceLocation> AFFIX_ACCESSORIES=new LinkedHashSet<>();
    public static ResourceLocation registerAffixAccessory(ResourceLocation id) {AFFIX_ACCESSORIES.add(id);return register(id);}
    public static List<ResourceLocation> affixAccessoryRecipes() {return List.copyOf(AFFIX_ACCESSORIES);}

    private static ListTag learned(Player player) {
        return player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getList(KEY,Tag.TAG_STRING);
    }
    private static void store(Player player,ListTag list) {
        var data=player.getPersistentData();
        CompoundTag persisted=data.getCompound(Player.PERSISTED_NBT_TAG);
        persisted.put(KEY,list);data.put(Player.PERSISTED_NBT_TAG,persisted);
        if(player instanceof net.minecraft.server.level.ServerPlayer server){AlchemyWorldKnowledge.record(server);sync(server);}
    }
    public static void sync(net.minecraft.server.level.ServerPlayer player) {
        local.primordial.network.MenuDropNetwork.syncAlchemyKnowledge(player,known(player));
        mirrorAdvancements(player);
    }
    /** each recipe has a hidden advancement primordial:known/<recipe>, held while it is known: the book lists those */
    public static boolean isKnownRecipeAdvancement(ResourceLocation advancement) {
        return advancement.getNamespace().equals(Primordial.ID)&&advancement.getPath().startsWith("known/");
    }
    static void mirrorAdvancements(net.minecraft.server.level.ServerPlayer player) {
        if(player.server==null)return;
        var all=player.server.getAdvancements();var known=Set.copyOf(known(player));boolean any=false;int learned=0;
        for(ResourceLocation id:RECIPES){
            if(known.contains(id))learned++;
            var advancement=all.getAdvancement(new ResourceLocation(id.getNamespace(),"known/"+id.getPath()));
            if(advancement==null)continue;
            boolean has=known.contains(id);any|=has;
            hold(player,advancement,has);
        }
        var root=all.getAdvancement(new ResourceLocation(Primordial.ID,"known/root"));
        if(root!=null)hold(player,root,any);
        local.primordial.achievement.Achievements.recipesKnown(player,learned,RECIPES.size());
    }
    private static void hold(net.minecraft.server.level.ServerPlayer player,net.minecraft.advancements.Advancement advancement,boolean done) {
        var progress=player.getAdvancements().getOrStartProgress(advancement);
        if(done&&!progress.isDone())for(String c:List.copyOf((java.util.Collection<String>)toList(progress.getRemainingCriteria())))player.getAdvancements().award(advancement,c);
        else if(!done&&progress.hasProgress())for(String c:toList(progress.getCompletedCriteria()))player.getAdvancements().revoke(advancement,c);
    }
    private static List<String> toList(Iterable<String> criteria) {List<String> out=new ArrayList<>();criteria.forEach(out::add);return out;}
    /** @return how many recipes were newly learned */
    public static int learnAll(Player player) {
        var list=learned(player);int added=0;
        for(ResourceLocation id:RECIPES)if(!knows(player,id)){list.add(StringTag.valueOf(id.toString()));added++;}
        if(added>0)store(player,list);
        return added;
    }
    public static boolean knows(Player player,ResourceLocation recipe) {
        var list=learned(player);
        for(int i=0;i<list.size();i++)if(list.getString(i).equals(recipe.toString()))return true;
        return false;
    }
    public static List<ResourceLocation> known(Player player) {
        var list=learned(player);List<ResourceLocation> out=new ArrayList<>();
        for(int i=0;i<list.size();i++){var id=ResourceLocation.tryParse(list.getString(i));if(id!=null)out.add(id);}
        return out;
    }
    /** @return false if the player already knew it. */
    public static boolean learn(Player player,ResourceLocation recipe) {
        if(knows(player,recipe))return false;
        var list=learned(player);list.add(StringTag.valueOf(recipe.toString()));store(player,list);
        checkTruthEpiphany(player);
        return true;
    }
    /**
     * Learn without a recipe item, announcing it in chat: "你从中顿悟，习得炼金配方（…）".
     * Only a sage (wearing Sage's Gift) can have an epiphany; does nothing if not, or if already known.
     */
    public static boolean epiphany(Player player,ResourceLocation recipe) {
        if(!local.primordial.sage.SageHeart.active(player)||!learn(player,recipe))return false;
        player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.primordial.recipe.epiphany",
            net.minecraft.network.chat.Component.translatable("alchemy_recipe."+recipe.getNamespace()+"."+recipe.getPath()))
            .withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE));
        return true;
    }
    /** also re-checked when the Sage's Gift goes on and at login, in case the threshold was reached while not a sage */
    public static void checkTruthEpiphany(Player player) {
        if(player.level().isClientSide||knows(player,TRUTH_ESSENCE))return;
        int others=0;
        for(ResourceLocation id:known(player))if(!id.equals(TRUTH_ESSENCE))others++;
        if(others>=TRUTH_THRESHOLD)epiphany(player,TRUTH_ESSENCE);
    }
    /** @return false if the player did not know it. */
    public static boolean forget(Player player,ResourceLocation recipe) {
        var list=learned(player);
        for(int i=0;i<list.size();i++)if(list.getString(i).equals(recipe.toString())){list.remove(i);store(player,list);return true;}
        return false;
    }
    public static void forgetAll(Player player) {store(player,new ListTag());}

    @SubscribeEvent public static void clone(PlayerEvent.Clone event) {
        var old=event.getOriginal().getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if(old.contains(KEY,Tag.TAG_LIST))store(event.getEntity(),old.getList(KEY,Tag.TAG_STRING).copy());
    }
}
