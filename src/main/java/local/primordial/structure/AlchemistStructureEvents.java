package local.primordial.structure;

import java.util.ArrayList;
import java.util.List;
import local.primordial.Primordial;
import local.primordial.affix.AffixDistribution;
import local.primordial.affix.AffixEligibility;
import local.primordial.affix.AffixMaterials;
import local.primordial.affix.AffixState;
import local.primordial.affix.BossAffix;
import local.primordial.item.AlchemyRecipeItem;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Entities placed by the alchemist buildings. Structure entities join the world as if loaded from disk, so the
 * normal affix roll never sees them; these handlers run first and set them up once.
 */
@Mod.EventBusSubscriber(modid=Primordial.ID)
public final class AlchemistStructureEvents {
    /** ForgeData on a template mob: {level, count, boss, name?} - the affixes it gets when it first joins */
    public static final String GUARDIAN="primordial:guardian";
    /** ForgeData flag on the camp's villager; replaced by "primordial:wandering_alchemist_ready" once it has its offers */
    public static final String ALCHEMIST="primordial:wandering_alchemist",ALCHEMIST_READY="primordial:wandering_alchemist_ready";
    static final String[] TIER_ONE={"breaking_edge_charm","spellbreaker_charm","hundred_forged_charm","crystal_vestment_charm"};
    static final String[] TIER_TWO={"conquest_charm","arcane_pact_charm","bulwark_pact_charm","astral_pact_charm","swift_pact_charm","swiftstring_pact_charm"};
    static final String[] HIGH_TIER={"truth_alchemy_altar","origin_ingot","origin_orb","primordial_alchemy_altar",
        "law_of_arms_charm","law_of_arcana_charm","law_of_gale_charm","law_of_tailwind_charm","unyielding_body_charm","arcane_harmony_charm"};
    private AlchemistStructureEvents() {}

    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void join(EntityJoinLevelEvent event) {
        if(!(event.getLevel() instanceof ServerLevel))return;
        if(event.getEntity() instanceof Villager villager&&villager.getPersistentData().getBoolean(ALCHEMIST))wanderingAlchemist(villager);
        else if(event.getEntity() instanceof Mob mob&&mob.getPersistentData().contains(GUARDIAN))guardian(mob);
    }

    public static void guardian(Mob mob) {
        var data=mob.getPersistentData();var spec=data.getCompound(GUARDIAN);data.remove(GUARDIAN);
        if(data.contains(AffixState.KEY))return;
        if(spec.contains("name")){mob.setCustomName(Component.translatable(spec.getString("name")));mob.setCustomNameVisible(true);}
        var random=mob.getRandom();
        BossAffix boss=spec.getBoolean("boss")?BossAffix.values()[random.nextInt(BossAffix.values().length)]:null;
        AffixState.assign(mob,spec.getInt("level"),AffixDistribution.choose(spec.getInt("count"),random.nextLong(),affix->AffixEligibility.usable(mob,affix)),boss);
        mob.setPersistenceRequired();
    }

    public static void wanderingAlchemist(Villager villager) {
        var data=villager.getPersistentData();data.remove(ALCHEMIST);
        if(data.getBoolean(ALCHEMIST_READY))return;
        data.putBoolean(ALCHEMIST_READY,true);
        villager.setCustomName(Component.translatable("entity.primordial.wandering_alchemist"));villager.setCustomNameVisible(true);
        villager.setPersistenceRequired();
        villager.setOffers(offers(villager.getRandom()));
    }

    static ItemStack essence(int count){return new ItemStack(Primordial.ALCHEMICAL_ESSENCE.get(),count);}
    static ItemStack recipe(String id){return AlchemyRecipeItem.create(new ResourceLocation(Primordial.ID,id));}
    static <T> List<T> pick(List<T> from,int count,RandomSource random){
        var pool=new ArrayList<>(from);var picked=new ArrayList<T>();
        while(picked.size()<count&&!pool.isEmpty())picked.add(pool.remove(random.nextInt(pool.size())));return picked;
    }
    public static MerchantOffers offers(RandomSource random) {
        var offers=new MerchantOffers();
        var materials=AffixMaterials.ITEMS.values().stream().map(r->r.get()).toList();
        var bossMaterials=AffixMaterials.BOSS_ITEMS.values().stream().map(r->r.get()).toList();
        // essence for the common recipes
        for(var id:pick(List.of(TIER_ONE),2,random))offers.add(new MerchantOffer(essence(12),ItemStack.EMPTY,recipe(id),1,0,0));
        // affix materials and essence for the pacts
        for(var id:pick(List.of(TIER_TWO),2,random))
            offers.add(new MerchantOffer(new ItemStack(materials.get(random.nextInt(materials.size())),2),essence(16),recipe(id),1,0,0));
        // a boss material for the rarest knowledge
        offers.add(new MerchantOffer(new ItemStack(bossMaterials.get(random.nextInt(bossMaterials.size()))),essence(24),
            recipe(HIGH_TIER[random.nextInt(HIGH_TIER.length)]),1,0,0));
        // buys affix materials, sells essence
        for(var material:pick(materials,3,random))offers.add(new MerchantOffer(new ItemStack(material,1),ItemStack.EMPTY,new ItemStack(Items.EMERALD,4),8,0,0));
        offers.add(new MerchantOffer(new ItemStack(Items.EMERALD,8),ItemStack.EMPTY,essence(1),6,0,0));
        return offers;
    }
}
