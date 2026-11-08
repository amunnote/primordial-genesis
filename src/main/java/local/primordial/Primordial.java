package local.primordial;

import local.primordial.item.AngelCharmItem;
import local.primordial.item.PurificationCrystalItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(Primordial.ID)
public final class Primordial {
    public static final String ID = "primordial";
    public static final String ALCHEMICAL_SLOT = "alchemical_charm";
    public static final String ALCHEMICAL_ACCESSORY_SLOT = "alchemical_accessory";
    public static final String SAGE_SLOT = "sage_heart";
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ID);
    static { local.primordial.affix.AffixMaterials.register(ITEMS); local.primordial.affix.AffixAccessories.register(ITEMS); local.primordial.effect.WorldCores.register(ITEMS); }
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, ID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, ID);
    public static final DeferredRegister<net.minecraft.world.entity.EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, ID);
    public static final RegistryObject<net.minecraft.world.entity.EntityType<local.primordial.entity.GemLightning>> GEM_LIGHTNING = ENTITIES.register("gem_lightning",
            () -> net.minecraft.world.entity.EntityType.Builder.<local.primordial.entity.GemLightning>of(local.primordial.entity.GemLightning::new, net.minecraft.world.entity.MobCategory.MISC)
                    .sized(0.1F, 0.1F).noSave().noSummon().clientTrackingRange(16).updateInterval(20).build(ID + ":gem_lightning"));
    /** Summon's accessory: its wearer's see-through clones */
    public static final RegistryObject<net.minecraft.world.entity.EntityType<local.primordial.entity.SummonClone>> SUMMON_CLONE = ENTITIES.register("summon_clone",
            () -> net.minecraft.world.entity.EntityType.Builder.<local.primordial.entity.SummonClone>of(local.primordial.entity.SummonClone::new, net.minecraft.world.entity.MobCategory.MISC)
                    .sized(0.6F, 1.8F).noSummon().clientTrackingRange(10).build(ID + ":summon_clone"));
    static { local.primordial.entity.alchemical.AlchemicalCreations.register(); local.primordial.boss.SinBosses.register(); local.primordial.qinglong.QinglongContent.register(); local.primordial.baihu.BaihuContent.register(); local.primordial.zhuque.ZhuqueContent.register(); local.primordial.xuanwu.XuanwuContent.register(); local.primordial.fauna.FaunaContent.register(); }
    public static final RegistryObject<Item> CHARM = ITEMS.register("angel_charm", AngelCharmItem::new);
    public static final RegistryObject<Item> PURIFICATION_CRYSTAL = ITEMS.register("purification_crystal", PurificationCrystalItem::new);
    public static final RegistryObject<Item> SAGE_HEART = ITEMS.register("sage_heart", local.primordial.item.SageHeartItem::new);
    public static final RegistryObject<Item> GEM_SWORD = ITEMS.register("gem_sword", local.primordial.item.GemSwordItem::new);
    /** the Gem Sword's parts */
    public static final RegistryObject<Item> IRREGULAR_CRYSTAL = ITEMS.register("irregular_crystal", () -> new local.primordial.item.GemPartItem("irregular_crystal", net.minecraft.world.item.Rarity.RARE));
    public static final RegistryObject<Item> PRIMORDIAL_HILT = ITEMS.register("primordial_hilt", () -> new local.primordial.item.GemPartItem("primordial_hilt", net.minecraft.world.item.Rarity.EPIC));
    /** cut from the irregular crystal on a stonecutter */
    public static final RegistryObject<Item> GEM_SWORD_BLADE = ITEMS.register("gem_sword_blade", () -> new local.primordial.item.GemPartItem("gem_sword_blade", net.minecraft.world.item.Rarity.EPIC));
    public static final RegistryObject<Item> LIFE_FLOWER = ITEMS.register("life_flower", local.primordial.item.LifeFlowerItem::new);
    public static final RegistryObject<Item> ALCHEMICAL_SOURCE_INGOT = ITEMS.register("alchemical_source_ingot", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> ALCHEMICAL_ESSENCE = ITEMS.register("alchemical_essence", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> ALCHEMICAL_SOURCE_STRING = ITEMS.register("alchemical_source_string", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Block> ALCHEMICAL_SOURCE_BLOCK = BLOCKS.register("alchemical_source_block", () ->
            new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(5.0F, 6.0F).requiresCorrectToolForDrops()));
    public static final RegistryObject<Item> ALCHEMICAL_SOURCE_BLOCK_ITEM = ITEMS.register("alchemical_source_block", () ->
            new BlockItem(ALCHEMICAL_SOURCE_BLOCK.get(), new Item.Properties()));
    public static final RegistryObject<Item> ALCHEMY_RECIPE = ITEMS.register("alchemy_recipe", local.primordial.item.AlchemyRecipeItem::new);
    /** what learned recipes break down into on an altar, and what seven of them make */
    public static final RegistryObject<Item> RECIPE_FRAGMENT = ITEMS.register("recipe_fragment", local.primordial.item.RecipeFragmentItem::new);
    public static final RegistryObject<Item> MYSTERY_RECIPE = ITEMS.register("mystery_recipe", local.primordial.item.MysteryRecipeItem::new);
    /** the stone enlightened mobs build with, which crumbles away */
    public static final RegistryObject<Block> ENLIGHTENED_STONE = BLOCKS.register("enlightened_stone", local.primordial.affix.EnlightenedStoneBlock::new);
    public static final DeferredRegister<net.minecraft.world.level.block.entity.BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, ID);
    public static final RegistryObject<Block> BASIC_ALCHEMY_ALTAR = BLOCKS.register("basic_alchemy_altar", () ->
            new local.primordial.altar.AlchemyAltarBlock(1, local.primordial.altar.AlchemyAltarBlock.BASIC_SHAPE, 13 / 16F, 7));
    public static final RegistryObject<Item> BASIC_ALCHEMY_ALTAR_ITEM = ITEMS.register("basic_alchemy_altar", () ->
            new BlockItem(BASIC_ALCHEMY_ALTAR.get(), new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON)));
    public static final RegistryObject<Block> TRANSMUTATION_ALCHEMY_ALTAR = BLOCKS.register("transmutation_alchemy_altar", () ->
            new local.primordial.altar.AlchemyAltarBlock(2, local.primordial.altar.AlchemyAltarBlock.TRANSMUTATION_SHAPE, 12.5F / 16F, 10));
    public static final RegistryObject<Item> TRANSMUTATION_ALCHEMY_ALTAR_ITEM = ITEMS.register("transmutation_alchemy_altar", () ->
            new local.primordial.alchemy.KnowledgeBlockItem(TRANSMUTATION_ALCHEMY_ALTAR.get(),
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE), local.primordial.alchemy.AlchemyKnowledge.TRANSMUTATION_ALTAR));
    public static final RegistryObject<Item> TRANSMUTATION_SOURCE = ITEMS.register("transmutation_source", local.primordial.item.TransmutationSourceItem::new);
    public static final RegistryObject<Block> PRIMORDIAL_GEM_ORE = BLOCKS.register("primordial_gem_ore", () ->
            new Block(BlockBehaviour.Properties.of().mapColor(MapColor.SAND).strength(4.5F, 9.0F).requiresCorrectToolForDrops()
                    .sound(net.minecraft.world.level.block.SoundType.ANCIENT_DEBRIS).lightLevel(state -> 3)));
    public static final RegistryObject<Item> PRIMORDIAL_GEM_ORE_ITEM = ITEMS.register("primordial_gem_ore", () ->
            new BlockItem(PRIMORDIAL_GEM_ORE.get(), new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON)));
    public static final RegistryObject<Block> TRUTH_ALCHEMY_ALTAR = BLOCKS.register("truth_alchemy_altar", () ->
            new local.primordial.altar.AlchemyAltarBlock(3, local.primordial.altar.AlchemyAltarBlock.TRUTH_SHAPE, 11 / 16F, 12));
    public static final RegistryObject<Item> TRUTH_ALCHEMY_ALTAR_ITEM = ITEMS.register("truth_alchemy_altar", () ->
            new local.primordial.alchemy.KnowledgeBlockItem(TRUTH_ALCHEMY_ALTAR.get(),
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC), local.primordial.alchemy.AlchemyKnowledge.TRUTH_ALTAR));
    public static final RegistryObject<Item> TRUTH_ESSENCE = ITEMS.register("truth_essence", () ->
            new local.primordial.alchemy.KnowledgeItem(new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE), local.primordial.alchemy.AlchemyKnowledge.TRUTH_ESSENCE));
    public static final RegistryObject<Item> PRIMORDIAL_GEM_POWDER = ITEMS.register("primordial_gem_powder", () ->
            new Item(new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON)));
    public static final RegistryObject<Block> PRIMORDIAL_ALCHEMY_ALTAR = BLOCKS.register("primordial_alchemy_altar", () ->
            new local.primordial.altar.AlchemyAltarBlock(4, local.primordial.altar.AlchemyAltarBlock.PRIMORDIAL_SHAPE, 11 / 16F, 15));
    public static final RegistryObject<Item> PRIMORDIAL_ALCHEMY_ALTAR_ITEM = ITEMS.register("primordial_alchemy_altar", () ->
            new local.primordial.alchemy.KnowledgeBlockItem(PRIMORDIAL_ALCHEMY_ALTAR.get(),
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC).fireResistant(), local.primordial.alchemy.AlchemyKnowledge.PRIMORDIAL_ALTAR));
    public static final RegistryObject<Item> ORIGIN_ORB = ITEMS.register("origin_orb", () ->
            new local.primordial.alchemy.KnowledgeItem(new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE), local.primordial.alchemy.AlchemyKnowledge.ORIGIN_ORB));
    public static final RegistryObject<Item> ORIGIN_INGOT = ITEMS.register("origin_ingot", () ->
            new local.primordial.alchemy.KnowledgeItem(new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC).fireResistant(), local.primordial.alchemy.AlchemyKnowledge.ORIGIN_INGOT));
    public static final RegistryObject<Item> CREATION_BOOK = ITEMS.register("creation_book", local.primordial.item.CreationBookItem::new);
    public static final RegistryObject<Item> BASIC_CHARM = ITEMS.register("basic_charm", () ->
            new local.primordial.item.AlchemyCharmItem(net.minecraft.world.item.Rarity.COMMON, 5, 0, 5, 0).luck(5));
    // finds in the overworld's chests (ItemLootModifier): two plain charms, a luckless accessory, and three things to use
    public static final RegistryObject<Item> WORN_TRAVELER_CHARM = ITEMS.register("worn_traveler_charm", () ->
            new local.primordial.item.AlchemyCharmItem(net.minecraft.world.item.Rarity.COMMON, 0, 0, 0, 0).luck(8));
    public static final RegistryObject<Item> IRON_VITALITY_CHARM = ITEMS.register("iron_vitality_charm", () ->
            new local.primordial.item.AlchemyCharmItem(net.minecraft.world.item.Rarity.COMMON, 0, 0, 0, 0).maxHealth(4).luck(2));
    public static final RegistryObject<Item> GLOWSTONE_EARRING = ITEMS.register("glowstone_earring", local.primordial.item.GlowstoneEarringItem::new);
    public static final RegistryObject<Item> SUBSTITUTE_DOLL = ITEMS.register("substitute_doll", local.primordial.item.SubstituteDollItem::new);
    public static final RegistryObject<Item> TORN_RECIPE_PAGE = ITEMS.register("torn_recipe_page", local.primordial.item.TornRecipePageItem::new);
    public static final RegistryObject<Item> APPRENTICE_PARCEL = ITEMS.register("apprentice_parcel", local.primordial.item.ApprenticeParcelItem::new);
    public static final RegistryObject<Item> BREAKING_EDGE_CHARM = ITEMS.register("breaking_edge_charm", () ->
            new local.primordial.item.AlchemyCharmItem(net.minecraft.world.item.Rarity.UNCOMMON, 0, 0.3, 0, 0).luck(10));
    public static final RegistryObject<Item> CONQUEST_CHARM = ITEMS.register("conquest_charm", () ->
            new local.primordial.item.AlchemyCharmItem(net.minecraft.world.item.Rarity.RARE, 0, 0.6, 0, 0.5).luck(30));
    public static final RegistryObject<Item> SPELLBREAKER_CHARM = ITEMS.register("spellbreaker_charm", () ->
            new local.primordial.item.AlchemyCharmItem(net.minecraft.world.item.Rarity.UNCOMMON, 0, 0, 0, 0).nonPhysical(0.3).magicAttacks().luck(10));
    public static final RegistryObject<Item> ARCANE_PACT_CHARM = ITEMS.register("arcane_pact_charm", () ->
            new local.primordial.item.AlchemyCharmItem(net.minecraft.world.item.Rarity.RARE, 0, 0, 0, 0).nonPhysical(0.6).magicAttacks().resistancePierce(0.3).luck(30));
    public static final RegistryObject<Item> HUNDRED_FORGED_CHARM = ITEMS.register("hundred_forged_charm", () ->
            new local.primordial.item.AlchemyCharmItem(net.minecraft.world.item.Rarity.UNCOMMON, 0, 0, 0, 0).physicalReduction(0.3).luck(10));
    public static final RegistryObject<Item> BULWARK_PACT_CHARM = ITEMS.register("bulwark_pact_charm", () ->
            new local.primordial.item.AlchemyCharmItem(net.minecraft.world.item.Rarity.RARE, 0, 0, 0, 0).physicalReduction(0.5).luck(30));
    public static final RegistryObject<Item> CRYSTAL_VESTMENT_CHARM = ITEMS.register("crystal_vestment_charm", () ->
            new local.primordial.item.AlchemyCharmItem(net.minecraft.world.item.Rarity.UNCOMMON, 0, 0, 0, 0).nonPhysicalReduction(0.3).luck(10));
    public static final RegistryObject<Item> ASTRAL_PACT_CHARM = ITEMS.register("astral_pact_charm", () ->
            new local.primordial.item.AlchemyCharmItem(net.minecraft.world.item.Rarity.RARE, 0, 0, 0, 0).nonPhysicalReduction(0.5).luck(30));
    public static final RegistryObject<Item> THOUSAND_BLADE_CHARM = ITEMS.register("thousand_blade_charm", () ->
            new local.primordial.item.AlchemyCharmItem(net.minecraft.world.item.Rarity.UNCOMMON, 0, 0, 0, 0).attackSpeed(0.3).luck(10));
    public static final RegistryObject<Item> SWIFT_PACT_CHARM = ITEMS.register("swift_pact_charm", () ->
            new local.primordial.item.AlchemyCharmItem(net.minecraft.world.item.Rarity.RARE, 0, 0, 0, 0).attackSpeed(0.6).feebleOnHit().luck(30));
    public static final RegistryObject<Item> QUICKSTRING_CHARM = ITEMS.register("quickstring_charm", () ->
            new local.primordial.item.AlchemyCharmItem(net.minecraft.world.item.Rarity.UNCOMMON, 0, 0, 0, 0).projectileBonus(0.3).luck(10));
    public static final RegistryObject<Item> SWIFTSTRING_PACT_CHARM = ITEMS.register("swiftstring_pact_charm", () ->
            new local.primordial.item.AlchemyCharmItem(net.minecraft.world.item.Rarity.RARE, 0, 0, 0, 0).drawSpeed(0.6).projectileBonus(0.5).infiniteAmmo().luck(30));
    // truth tier: made at the truth altar from a pact charm and affix materials
    public static final RegistryObject<Item> LAW_OF_ARMS_CHARM = ITEMS.register("law_of_arms_charm", () ->
            new local.primordial.item.AlchemyCharmItem(net.minecraft.world.item.Rarity.EPIC, 0, 1.0, 0, 0.8).luck(50));
    public static final RegistryObject<Item> LAW_OF_ARCANA_CHARM = ITEMS.register("law_of_arcana_charm", () ->
            new local.primordial.item.AlchemyCharmItem(net.minecraft.world.item.Rarity.EPIC, 0, 0, 0, 0).nonPhysical(1.0).magicAttacks().resistancePierce(0.8).luck(50));
    public static final RegistryObject<Item> LAW_OF_GALE_CHARM = ITEMS.register("law_of_gale_charm", () ->
            new local.primordial.item.AlchemyCharmItem(net.minecraft.world.item.Rarity.EPIC, 0, 0, 0, 0).attackSpeed(1.0).ignoreInvulnerability().luck(50));
    public static final RegistryObject<Item> LAW_OF_TAILWIND_CHARM = ITEMS.register("law_of_tailwind_charm", () ->
            new local.primordial.item.AlchemyCharmItem(net.minecraft.world.item.Rarity.EPIC, 0, 0, 0, 0).drawSpeed(1.0).projectileBonus(1.0).homing().luck(50));
    public static final RegistryObject<Item> UNYIELDING_BODY_CHARM = ITEMS.register("unyielding_body_charm", () ->
            new local.primordial.item.AlchemyCharmItem(net.minecraft.world.item.Rarity.EPIC, 0, 0, 0, 0).physicalReduction(0.7).luck(50));
    public static final RegistryObject<Item> ARCANE_HARMONY_CHARM = ITEMS.register("arcane_harmony_charm", () ->
            new local.primordial.item.AlchemyCharmItem(net.minecraft.world.item.Rarity.EPIC, 0, 0, 0, 0).nonPhysicalReduction(0.7).luck(50));
    // primordial tier: made at the primordial altar from law charms and the Sin Lords' materials
    public static final RegistryObject<Item> OMNI_AMPLIFY_CHARM = ITEMS.register("omni_amplify_charm", () ->
            new local.primordial.item.AlchemyCharmItem(local.primordial.item.AlchemyCharmItem.PRIMORDIAL, 0, 1.5, 0, 1.0).nonPhysical(1.5).resistancePierce(1.0).echo(0.5).primordial().luck(80));
    public static final RegistryObject<Item> FLEETING_ETERNITY_CHARM = ITEMS.register("fleeting_eternity_charm", () ->
            new local.primordial.item.AlchemyCharmItem(local.primordial.item.AlchemyCharmItem.PRIMORDIAL, 0, 0, 0, 0).attackSpeed(1.5).ignoreInvulnerability()
                    .drawSpeed(1.5).projectileBonus(1.5).homing().infiniteAmmo().stasis().primordial().luck(80));
    public static final RegistryObject<Item> UNBREAKABLE_CHARM = ITEMS.register("unbreakable_charm", () ->
            new local.primordial.item.AlchemyCharmItem(local.primordial.item.AlchemyCharmItem.PRIMORDIAL, 0, 0, 10, 0).physicalReduction(0.85).knockbackResist(1.0).hitCap(0.25).primordial().luck(80));
    public static final RegistryObject<Item> SPELL_ANNUL_CHARM = ITEMS.register("spell_annul_charm", () ->
            new local.primordial.item.AlchemyCharmItem(local.primordial.item.AlchemyCharmItem.PRIMORDIAL, 0, 0, 0, 0).nonPhysicalReduction(0.85).cleanse().reflect(0.3).primordial().luck(80));
    // supreme tier: made at the primordial altar from two primordial charms and the guardians' relics
    public static final RegistryObject<Item> CLEAR_WORLD_CHARM = ITEMS.register("clear_world_charm", () ->
            new local.primordial.item.AlchemyCharmItem(local.primordial.item.AlchemyCharmItem.SUPREME, 0, 2.0, 0, 1.0).nonPhysical(2.0).resistancePierce(1.0).echo(0.75)
                    .attackSpeed(2.0).ignoreInvulnerability().drawSpeed(2.0).projectileBonus(2.0).homing().infiniteAmmo().stasis().insight(0.2).supreme().luck(100));
    public static final RegistryObject<Item> INVIOLABLE_CHARM = ITEMS.register("inviolable_charm", () ->
            new local.primordial.item.AlchemyCharmItem(local.primordial.item.AlchemyCharmItem.SUPREME, 0, 0, 15, 0).allReduction(0.9)
                    .knockbackResist(1.0).hitCap(0.2).cleanse().reflect(0.4).ward(1200).supreme().luck(100));
    public static final RegistryObject<Item> SPIRIT_COMPASS = ITEMS.register("spirit_compass", local.primordial.item.SpiritCompassItem::new);
    public static final RegistryObject<Item> ALCHEMIST_GIFT = ITEMS.register("alchemist_gift", local.primordial.item.AlchemistGiftItem::new);
    // id kept as "basic_alchemy_altar" so altars already placed keep their items; shared by every altar tier
    public static final RegistryObject<net.minecraft.world.level.block.entity.BlockEntityType<local.primordial.altar.AltarBlockEntity>> BASIC_ALCHEMY_ALTAR_ENTITY =
            BLOCK_ENTITIES.register("basic_alchemy_altar", () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder
                    .of(local.primordial.altar.AltarBlockEntity::new, BASIC_ALCHEMY_ALTAR.get(), TRANSMUTATION_ALCHEMY_ALTAR.get(), TRUTH_ALCHEMY_ALTAR.get(), PRIMORDIAL_ALCHEMY_ALTAR.get()).build(null));
    public static final DeferredRegister<net.minecraft.world.item.crafting.RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, ID);
    public static final RegistryObject<net.minecraft.world.item.crafting.RecipeType<local.primordial.altar.AltarRecipe>> ALTAR_RECIPE_TYPE =
            RECIPE_TYPES.register("altar", () -> net.minecraft.world.item.crafting.RecipeType.simple(new net.minecraft.resources.ResourceLocation(ID, "altar")));
    public static final DeferredRegister<com.mojang.serialization.Codec<? extends net.minecraftforge.common.loot.IGlobalLootModifier>> LOOT_MODIFIERS =
            DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, ID);
    public static final RegistryObject<com.mojang.serialization.Codec<local.primordial.alchemy.AlchemyRecipeLootModifier>> ALCHEMY_RECIPE_LOOT =
            LOOT_MODIFIERS.register("alchemy_recipe_in_chests", local.primordial.alchemy.AlchemyRecipeLootModifier.CODEC);
    public static final RegistryObject<com.mojang.serialization.Codec<local.primordial.alchemy.ItemLootModifier>> ITEM_LOOT =
            LOOT_MODIFIERS.register("item_in_chests", local.primordial.alchemy.ItemLootModifier.CODEC);
    public static final DeferredRegister<net.minecraft.world.item.crafting.RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, ID);
    public static final RegistryObject<net.minecraft.world.item.crafting.RecipeSerializer<?>> KNOWLEDGE_SHAPED =
            RECIPE_SERIALIZERS.register("knowledge_shaped", local.primordial.alchemy.KnowledgeShapedRecipe.Serializer::new);
    public static final RegistryObject<net.minecraft.world.item.crafting.RecipeSerializer<local.primordial.altar.AltarRecipe>> ALTAR_RECIPE_SERIALIZER =
            RECIPE_SERIALIZERS.register("altar", local.primordial.altar.AltarRecipe.Serializer::new);
    public static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(ForgeRegistries.ATTRIBUTES, ID);
    public static final RegistryObject<Attribute> DAMAGE = ATTRIBUTES.register("all_damage", () ->
            new RangedAttribute("attribute.primordial.all_damage", 1, 0, 1024).setSyncable(true));
    public static final RegistryObject<Attribute> REDUCTION = ATTRIBUTES.register("damage_reduction", () ->
            new RangedAttribute("attribute.primordial.damage_reduction", 0, 0, 1024).setSyncable(true));

    public Primordial() {
        // a config from when the mod was angel_charm, carried over to its new name
        local.primordial.compat.LegacyIds.copyConfig();
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.COMMON,
                local.primordial.affix.AffixConfig.SPEC);
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.COMMON,
                local.primordial.qinglong.WorldConfig.SPEC, ID + "-world.toml");
        local.primordial.network.MenuDropNetwork.init();
        net.minecraftforge.common.crafting.CraftingHelper.register(local.primordial.affix.WeaponIngredient.ID, local.primordial.affix.WeaponIngredient.SERIALIZER);
        net.minecraftforge.common.crafting.CraftingHelper.register(local.primordial.affix.ArmorIngredient.ID, local.primordial.affix.ArmorIngredient.SERIALIZER);
        local.primordial.effect.AttributeCeiling.lift();
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        bus.addListener((net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent e) -> e.enqueueWork(local.primordial.effect.AttributeCeiling::lift));        ITEMS.register(bus);
        BLOCKS.register(bus);
        local.primordial.sound.ModSounds.register(bus);
        BLOCK_ENTITIES.register(bus);
        RECIPE_SERIALIZERS.register(bus);
        RECIPE_TYPES.register(bus);
        LOOT_MODIFIERS.register(bus);
        local.primordial.spirit.SpiritRealm.register(bus);
        local.primordial.entity.alchemical.ArcaneParticles.register(bus);
        local.primordial.boss.SinBosses.register(bus);
        local.primordial.qinglong.QinglongWorldgen.register(bus);
        local.primordial.baihu.BaihuWorldgen.register(bus);
        local.primordial.zhuque.ZhuqueWorldgen.register(bus);
        local.primordial.xuanwu.XuanwuWorldgen.register(bus);
        MENUS.register(bus);
        ENTITIES.register(bus);
        ATTRIBUTES.register(bus);
        bus.addListener((net.minecraftforge.event.entity.EntityAttributeCreationEvent e) ->
            e.put(SUMMON_CLONE.get(), local.primordial.entity.SummonClone.attributes().build()));
        bus.addListener((net.minecraftforge.event.entity.EntityAttributeModificationEvent e) -> {
            e.add(net.minecraft.world.entity.EntityType.PLAYER, DAMAGE.get());
            e.add(net.minecraft.world.entity.EntityType.PLAYER, REDUCTION.get());
        });
        // three creative tabs: what is worn, what is placed, and everything else
        DeferredRegister<CreativeModeTab> tabs = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ID);
        tabs.register("accessories", () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.primordial.accessories"))
                .icon(() -> CHARM.get().getDefaultInstance())
                .displayItems((parameters, output) -> {
                    output.accept(CHARM.get());output.accept(SAGE_HEART.get());output.accept(PURIFICATION_CRYSTAL.get());
                    output.accept(SPIRIT_COMPASS.get());output.accept(LIFE_FLOWER.get());output.accept(ALCHEMIST_GIFT.get());
                    output.accept(WORN_TRAVELER_CHARM.get());output.accept(IRON_VITALITY_CHARM.get());output.accept(GLOWSTONE_EARRING.get());
                    output.accept(BASIC_CHARM.get());output.accept(BREAKING_EDGE_CHARM.get());output.accept(CONQUEST_CHARM.get());output.accept(LAW_OF_ARMS_CHARM.get());
                    output.accept(SPELLBREAKER_CHARM.get());output.accept(ARCANE_PACT_CHARM.get());output.accept(LAW_OF_ARCANA_CHARM.get());
                    output.accept(HUNDRED_FORGED_CHARM.get());output.accept(BULWARK_PACT_CHARM.get());output.accept(UNYIELDING_BODY_CHARM.get());
                    output.accept(CRYSTAL_VESTMENT_CHARM.get());output.accept(ASTRAL_PACT_CHARM.get());output.accept(ARCANE_HARMONY_CHARM.get());
                    output.accept(OMNI_AMPLIFY_CHARM.get());output.accept(FLEETING_ETERNITY_CHARM.get());output.accept(UNBREAKABLE_CHARM.get());output.accept(SPELL_ANNUL_CHARM.get());output.accept(CLEAR_WORLD_CHARM.get());output.accept(INVIOLABLE_CHARM.get());
                    output.accept(THOUSAND_BLADE_CHARM.get());output.accept(SWIFT_PACT_CHARM.get());output.accept(LAW_OF_GALE_CHARM.get());
                    output.accept(QUICKSTRING_CHARM.get());output.accept(SWIFTSTRING_PACT_CHARM.get());output.accept(LAW_OF_TAILWIND_CHARM.get());
                    for (var accessory : local.primordial.affix.AffixAccessories.ITEMS.values()) output.accept(accessory.get());
                    for (var core : local.primordial.effect.WorldCores.CORES.values()) output.accept(core.get());
                    output.accept(local.primordial.effect.WorldCores.WORLD_CORE.get());output.accept(local.primordial.boss.SinBosses.SEVEN_SINS.get());
                }).build());
        tabs.register("blocks", () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.primordial.blocks"))
                .icon(() -> PRIMORDIAL_ALCHEMY_ALTAR_ITEM.get().getDefaultInstance())
                .withTabsBefore(new net.minecraft.resources.ResourceLocation(ID, "accessories"))
                .displayItems((parameters, output) -> {
                    output.accept(BASIC_ALCHEMY_ALTAR_ITEM.get());output.accept(TRANSMUTATION_ALCHEMY_ALTAR_ITEM.get());
                    output.accept(TRUTH_ALCHEMY_ALTAR_ITEM.get());output.accept(PRIMORDIAL_ALCHEMY_ALTAR_ITEM.get());
                    output.accept(local.primordial.qinglong.QinglongContent.CLOUDROCK_ITEM.get());
                    output.accept(local.primordial.baihu.BaihuContent.BLADESTONE_ITEM.get());
                    for(var item:java.util.List.of(local.primordial.xuanwu.XuanwuContent.ROCK_ITEM,local.primordial.xuanwu.XuanwuContent.SCALE,local.primordial.xuanwu.XuanwuContent.SHELL,local.primordial.xuanwu.XuanwuContent.TALISMAN))output.accept(item.get());
                    for(var item:local.primordial.fauna.FaunaContent.TAB)output.accept(item.get());
                    output.accept(local.primordial.zhuque.ZhuqueContent.LOG_ITEM.get());output.accept(local.primordial.zhuque.ZhuqueContent.LEAVES_ITEM.get());output.accept(local.primordial.zhuque.ZhuqueContent.FEATHER.get());output.accept(local.primordial.zhuque.ZhuqueContent.FLUFF.get());output.accept(local.primordial.zhuque.ZhuqueContent.TALISMAN.get());
                    output.accept(local.primordial.baihu.BaihuContent.SHARD.get());output.accept(local.primordial.baihu.BaihuContent.INGOT.get());output.accept(local.primordial.baihu.BaihuContent.PELT.get());
                    output.accept(ALCHEMICAL_SOURCE_BLOCK_ITEM.get());output.accept(PRIMORDIAL_GEM_ORE_ITEM.get());
                }).build());
        tabs.register("items", () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.primordial.items"))
                .icon(() -> ALCHEMICAL_ESSENCE.get().getDefaultInstance())
                .withTabsBefore(new net.minecraft.resources.ResourceLocation(ID, "blocks"))
                .displayItems((parameters, output) -> {
                    output.accept(GEM_SWORD.get());output.accept(IRREGULAR_CRYSTAL.get());output.accept(GEM_SWORD_BLADE.get());output.accept(PRIMORDIAL_HILT.get());output.accept(CREATION_BOOK.get());
                    output.accept(ALCHEMICAL_SOURCE_INGOT.get());output.accept(ALCHEMICAL_ESSENCE.get());output.accept(ALCHEMICAL_SOURCE_STRING.get());
                    output.accept(SUBSTITUTE_DOLL.get());output.accept(TORN_RECIPE_PAGE.get());output.accept(APPRENTICE_PARCEL.get());
                    output.accept(TRANSMUTATION_SOURCE.get());output.accept(TRUTH_ESSENCE.get());output.accept(PRIMORDIAL_GEM_POWDER.get());
                    output.accept(local.primordial.qinglong.QinglongContent.WHISKER.get());output.accept(local.primordial.qinglong.QinglongContent.ANTLER.get());output.accept(local.primordial.qinglong.QinglongContent.TALISMAN.get());
                    output.accept(ORIGIN_ORB.get());output.accept(ORIGIN_INGOT.get());
                    for (var material : local.primordial.affix.AffixMaterials.ITEMS.values()) output.accept(material.get());
                    for (var material : local.primordial.affix.AffixMaterials.BOSS_ITEMS.values()) output.accept(material.get());
                    for (var drop : local.primordial.entity.alchemical.AlchemicalCreations.drops()) output.accept(drop.get());
                    for (var egg : local.primordial.entity.alchemical.AlchemicalCreations.eggs()) output.accept(egg.get());
                    for (var egg : local.primordial.boss.SinBosses.eggs()) output.accept(egg.get());
                    output.accept(local.primordial.boss.SinBosses.PRIMORDIAL_EGG.get());
                    for (var sigil : local.primordial.boss.SinBosses.SIGILS.values()) output.accept(sigil.get());
                    output.accept(local.primordial.boss.SinBosses.PRIMORDIAL_HEART.get());output.accept(local.primordial.boss.SinBosses.PRIMORDIAL_SIGIL.get());
                    output.accept(local.primordial.boss.SinBosses.BAIHU_EGG.get());output.accept(local.primordial.boss.SinBosses.XUANWU_EGG.get());output.accept(local.primordial.boss.SinBosses.QINGLONG_EGG.get());output.accept(local.primordial.boss.SinBosses.ZHUQUE_EGG.get());output.accept(local.primordial.boss.SinBosses.FIRST_SAGE_EGG.get());
                    for (var relic : java.util.List.of(local.primordial.boss.SinBosses.BAIHU_FANG,local.primordial.boss.SinBosses.BAIHU_SIGIL,local.primordial.boss.SinBosses.XUANWU_SCUTE,local.primordial.boss.SinBosses.XUANWU_SIGIL,local.primordial.boss.SinBosses.QINGLONG_SCALE,local.primordial.boss.SinBosses.QINGLONG_SIGIL,local.primordial.boss.SinBosses.ZHUQUE_FEATHER,local.primordial.boss.SinBosses.ZHUQUE_SIGIL,local.primordial.boss.SinBosses.METAL_ESSENCE,local.primordial.boss.SinBosses.WATER_ESSENCE,local.primordial.boss.SinBosses.DIVINITY_SHARD,local.primordial.boss.SinBosses.PHILOSOPHER_STONE,local.primordial.boss.SinBosses.SAGE_SIGIL)) output.accept(relic.get());
                    for (var soul : local.primordial.effect.WorldCores.SOULS.values()) output.accept(soul.get());
                    for (int tier = 1; tier <= local.primordial.alchemy.RecipeTiers.FRAGMENT_MAX; tier++) output.accept(local.primordial.item.RecipeFragmentItem.create(tier));
                    for (int tier = 1; tier <= local.primordial.alchemy.RecipeTiers.FRAGMENT_MAX; tier++) output.accept(local.primordial.item.MysteryRecipeItem.create(tier));
                    for (var recipe : local.primordial.alchemy.AlchemyKnowledge.recipes())
                        output.accept(local.primordial.item.AlchemyRecipeItem.create(recipe));
                }).build());
        tabs.register(bus);
    }
}
