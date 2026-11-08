package local.primordial.boss;

import java.util.List;
import local.primordial.Primordial;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * The seven Sin Lords of the Spirit Realm, after the seven classes of heroic spirits: Pride the Archer, Wrath the
 * Berserker, Envy the Assassin, Greed the Rider, Lust the Caster, Gluttony the Lancer and Sloth the Saber. Each sleeps
 * in a sanctum of its own far out in the realm (see {@link SinSanctums}). Also what they hurl and call up, their
 * particles, and Gluttony's curse.
 */
public final class SinBosses {
    static final DeferredRegister<MobEffect> EFFECTS=DeferredRegister.create(ForgeRegistries.MOB_EFFECTS,Primordial.ID);
    /** 噬伤: Gluttony's bite; while it lasts, nothing heals you */
    public static final RegistryObject<MobEffect> DEVOURED=EFFECTS.register("devoured",()->new MobEffect(MobEffectCategory.HARMFUL,0x5A2E1E){});

    public static final RegistryObject<EntityType<PrideLord>> PRIDE=boss("pride_lord",PrideLord::new,1.0F,3.4F);
    public static final RegistryObject<EntityType<WrathLord>> WRATH=boss("wrath_lord",WrathLord::new,1.8F,3.9F);
    public static final RegistryObject<EntityType<EnvyLord>> ENVY=boss("envy_lord",EnvyLord::new,0.7F,2.2F);
    public static final RegistryObject<EntityType<GreedLord>> GREED=boss("greed_lord",GreedLord::new,1.6F,2.8F);
    public static final RegistryObject<EntityType<LustLord>> LUST=boss("lust_lord",LustLord::new,0.8F,2.4F);
    public static final RegistryObject<EntityType<GluttonyLord>> GLUTTONY=boss("gluttony_lord",GluttonyLord::new,1.6F,3.0F);
    public static final RegistryObject<EntityType<SlothLord>> SLOTH=boss("sloth_lord",SlothLord::new,1.0F,2.8F);
    /** the Primordial Lord, above the seven, and the pillars it raises */
    public static final RegistryObject<EntityType<PrimordialLord>> PRIMORDIAL=boss("primordial_lord",PrimordialLord::new,1.4F,4.2F);
    /** the four guardians between the sins and the origin */
    public static final RegistryObject<EntityType<BaihuLord>> BAIHU=boss("baihu",BaihuLord::new,4.2F,4.2F);
    public static final RegistryObject<EntityType<XuanwuLord>> XUANWU=boss("xuanwu",XuanwuLord::new,6.9F,5.1F);
    public static final RegistryObject<EntityType<QinglongLord>> QINGLONG=boss("qinglong",QinglongLord::new,6.0F,5.0F);
    public static final RegistryObject<EntityType<ZhuqueLord>> ZHUQUE=boss("zhuque",ZhuqueLord::new,4.6F,4.6F);
    /** the First Sage, last of all */
    public static final RegistryObject<EntityType<FirstSage>> FIRST_SAGE=boss("first_sage",FirstSage::new,1.2F,3.6F);
    public static final RegistryObject<EntityType<SinPillar>> SIN_PILLAR=Primordial.ENTITIES.register("sin_pillar",
        ()->EntityType.Builder.<SinPillar>of(SinPillar::new,MobCategory.MISC).sized(1.7F,4.4F).fireImmune().noSummon().clientTrackingRange(10).build(Primordial.ID+":sin_pillar"));
    public static final RegistryObject<EntityType<EnvyShade>> ENVY_SHADE=Primordial.ENTITIES.register("envy_shade",
        ()->EntityType.Builder.<EnvyShade>of(EnvyShade::new,MobCategory.MONSTER).sized(0.7F,2.2F).clientTrackingRange(8).build(Primordial.ID+":envy_shade"));
    public static final RegistryObject<EntityType<TreasureWeapon>> TREASURE_WEAPON=Primordial.ENTITIES.register("treasure_weapon",
        ()->EntityType.Builder.<TreasureWeapon>of(TreasureWeapon::new,MobCategory.MISC).sized(0.4F,0.4F).clientTrackingRange(8).updateInterval(1).build(Primordial.ID+":treasure_weapon"));
    public static final RegistryObject<EntityType<TreasuryGate>> TREASURY_GATE=Primordial.ENTITIES.register("treasury_gate",
        ()->EntityType.Builder.<TreasuryGate>of(TreasuryGate::new,MobCategory.MISC).sized(0.5F,0.5F).clientTrackingRange(10).updateInterval(2)
            .noSave().noSummon().build(Primordial.ID+":treasury_gate"));
    public static final RegistryObject<EntityType<SinShockwave>> SHOCKWAVE=Primordial.ENTITIES.register("sin_shockwave",
        ()->EntityType.Builder.<SinShockwave>of(SinShockwave::new,MobCategory.MISC).sized(0.5F,0.5F).clientTrackingRange(10).updateInterval(4)
            .noSave().noSummon().build(Primordial.ID+":sin_shockwave"));
    public static final RegistryObject<EntityType<SinOrb>> SIN_ORB=Primordial.ENTITIES.register("sin_orb",
        ()->EntityType.Builder.<SinOrb>of(SinOrb::new,MobCategory.MISC).sized(0.4F,0.4F).clientTrackingRange(8).updateInterval(1).build(Primordial.ID+":sin_orb"));

    public static final RegistryObject<Item> PRIDE_EGG=egg("pride_lord",PRIDE,0xFFDF7E,0xFFFFFF);
    public static final RegistryObject<Item> WRATH_EGG=egg("wrath_lord",WRATH,0x3A0E12,0xE63248);
    public static final RegistryObject<Item> ENVY_EGG=egg("envy_lord",ENVY,0x10241A,0x43C982);
    public static final RegistryObject<Item> GREED_EGG=egg("greed_lord",GREED,0x5A3A0A,0xF5A623);
    public static final RegistryObject<Item> LUST_EGG=egg("lust_lord",LUST,0x3A1030,0xF66ABC);
    public static final RegistryObject<Item> GLUTTONY_EGG=egg("gluttony_lord",GLUTTONY,0x2A1A10,0xBA9162);
    public static final RegistryObject<Item> SLOTH_EGG=egg("sloth_lord",SLOTH,0x1E2030,0x9294BA);
    public static final RegistryObject<Item> PRIMORDIAL_EGG=egg("primordial_lord",PRIMORDIAL,0xF4F0E6,0xE8C25A);
    public static final RegistryObject<Item> BAIHU_EGG=egg("baihu",BAIHU,0xF4F4F0,0x2A2A30);
    public static final RegistryObject<Item> XUANWU_EGG=egg("xuanwu",XUANWU,0x14242E,0x3AB8D8);
    public static final RegistryObject<Item> QINGLONG_EGG=egg("qinglong",QINGLONG,0x1E5A48,0xE8C25A);
    public static final RegistryObject<Item> ZHUQUE_EGG=egg("zhuque",ZHUQUE,0xC8281E,0xFFC84A);
    public static final RegistryObject<Item> FIRST_SAGE_EGG=egg("first_sage",FIRST_SAGE,0x2A1A4A,0xE8C25A);
    /** what the guardians drop, and the sigils that call them back */
    public static final RegistryObject<Item> BAIHU_FANG=Primordial.ITEMS.register("baihu_fang",()->new RelicItem("baihu_fang",BaihuLord.COLOUR,net.minecraft.world.item.Rarity.EPIC,false));
    public static final RegistryObject<Item> BAIHU_SIGIL=Primordial.ITEMS.register("baihu_sigil",()->new RelicItem("baihu_sigil",BaihuLord.COLOUR,net.minecraft.world.item.Rarity.EPIC,true));
    public static final RegistryObject<Item> XUANWU_SCUTE=Primordial.ITEMS.register("xuanwu_scute",()->new RelicItem("xuanwu_scute",XuanwuLord.COLOUR,net.minecraft.world.item.Rarity.EPIC,false));
    public static final RegistryObject<Item> XUANWU_SIGIL=Primordial.ITEMS.register("xuanwu_sigil",()->new RelicItem("xuanwu_sigil",XuanwuLord.COLOUR,net.minecraft.world.item.Rarity.EPIC,true));
    public static final RegistryObject<Item> QINGLONG_SCALE=Primordial.ITEMS.register("qinglong_scale",()->new RelicItem("qinglong_scale",QinglongLord.COLOUR,net.minecraft.world.item.Rarity.EPIC,false));
    public static final RegistryObject<Item> QINGLONG_SIGIL=Primordial.ITEMS.register("qinglong_sigil",()->new RelicItem("qinglong_sigil",QinglongLord.COLOUR,net.minecraft.world.item.Rarity.EPIC,true));
    public static final RegistryObject<Item> ZHUQUE_FEATHER=Primordial.ITEMS.register("zhuque_feather",()->new RelicItem("zhuque_feather",ZhuqueLord.COLOUR,net.minecraft.world.item.Rarity.EPIC,false));
    public static final RegistryObject<Item> ZHUQUE_SIGIL=Primordial.ITEMS.register("zhuque_sigil",()->new RelicItem("zhuque_sigil",ZhuqueLord.COLOUR,net.minecraft.world.item.Rarity.EPIC,true));
    /** the guardians' essences, one a fight: the hearts of the supreme charms */
    public static final RegistryObject<Item> METAL_ESSENCE=Primordial.ITEMS.register("metal_essence",()->new RelicItem("metal_essence",BaihuLord.COLOUR,local.primordial.item.AlchemyCharmItem.PRIMORDIAL,true));
    public static final RegistryObject<Item> WATER_ESSENCE=Primordial.ITEMS.register("water_essence",()->new RelicItem("water_essence",XuanwuLord.COLOUR,local.primordial.item.AlchemyCharmItem.PRIMORDIAL,true));
    /** 神格碎片: a shard of the Primordial Lord's divinity, two a fight; the Ascension Charm's heart */
    public static final RegistryObject<Item> DIVINITY_SHARD=Primordial.ITEMS.register("divinity_shard",()->new RelicItem("divinity_shard",PrimordialLord.COLOUR,local.primordial.item.AlchemyCharmItem.SUPREME,true));
    /** 贤者之石: the First Sage's stone, the Gem Sword's heart (the First Sage is still to come) */
    public static final RegistryObject<Item> PHILOSOPHER_STONE=Primordial.ITEMS.register("philosopher_stone",()->new RelicItem("philosopher_stone",0xE8283A,local.primordial.item.AlchemyCharmItem.SUPREME,true));
    /** 贤者之印: dropped by the First Sage, it wakes its statue */
    public static final RegistryObject<Item> SAGE_SIGIL=Primordial.ITEMS.register("first_sage_sigil",()->new RelicItem("first_sage_sigil",FirstSage.RUBEDO,local.primordial.item.AlchemyCharmItem.SUPREME,true));
    /** the Primordial Lord's heart, which it drops, and its sigil, made from the heart and the seven sigils */
    public static final RegistryObject<Item> PRIMORDIAL_HEART=Primordial.ITEMS.register("primordial_heart",()->new PrimordialSigilItem(true));
    public static final RegistryObject<Item> PRIMORDIAL_SIGIL=Primordial.ITEMS.register("primordial_sigil",()->new PrimordialSigilItem(false));
    /** learned only by epiphany, on seeing the Primordial Lord fall */
    public static final net.minecraft.resources.ResourceLocation PRIMORDIAL_SIGIL_RECIPE=
        local.primordial.alchemy.AlchemyKnowledge.registerEpiphanyOnly(new net.minecraft.resources.ResourceLocation(Primordial.ID,"primordial_sigil"));
    /** the seven sigils, by sin */
    public static final java.util.Map<local.primordial.affix.BossAffix,RegistryObject<Item>> SIGILS=new java.util.EnumMap<>(local.primordial.affix.BossAffix.class);
    static {for(var sin:local.primordial.affix.BossAffix.values())SIGILS.put(sin,Primordial.ITEMS.register(sigilId(sin),()->new SinSigilItem(sin)));}
    public static String sigilId(local.primordial.affix.BossAffix sin) {return sin.id()+"_sigil";}
    /** the Seven Sins accessory, made from the seven sigils; its recipe is carried by a Sin Lord */
    public static final RegistryObject<Item> SEVEN_SINS=Primordial.ITEMS.register("seven_sins",SevenSinsItem::new);
    public static final net.minecraft.resources.ResourceLocation SEVEN_SINS_RECIPE=
        local.primordial.alchemy.AlchemyKnowledge.registerBossOnly(new net.minecraft.resources.ResourceLocation(Primordial.ID,"seven_sins"));
    // (after the Seven Sins: the order in which boss-only recipes are registered decides which Sin Lord carries which)
    /** the supreme charms' recipes: the White Tiger carries Transparent World, the Black Tortoise Untouched by Any Law */
    public static final net.minecraft.resources.ResourceLocation CLEAR_WORLD_RECIPE=
        local.primordial.alchemy.AlchemyKnowledge.registerBossOnly(new net.minecraft.resources.ResourceLocation(Primordial.ID,"clear_world_charm"));
    public static final net.minecraft.resources.ResourceLocation INVIOLABLE_RECIPE=
        local.primordial.alchemy.AlchemyKnowledge.registerBossOnly(new net.minecraft.resources.ResourceLocation(Primordial.ID,"inviolable_charm"));
    /** the Ascension Charm's recipe, carried by the Primordial Lord (after the supreme charms, for the same reason) */
    public static final net.minecraft.resources.ResourceLocation ASCENSION_RECIPE=
        local.primordial.alchemy.AlchemyKnowledge.registerBossOnly(new net.minecraft.resources.ResourceLocation(Primordial.ID,"angel_charm"));
    /** the Gem Sword's recipe, to be carried by the First Sage (registered last, for the same reason) */
    public static final net.minecraft.resources.ResourceLocation GEM_SWORD_RECIPE=
        local.primordial.alchemy.AlchemyKnowledge.registerBossOnly(new net.minecraft.resources.ResourceLocation(Primordial.ID,"gem_sword"));
    /** the irregular crystal's recipe, carried by the Azure Dragon (registered last, for the same reason) */
    public static final net.minecraft.resources.ResourceLocation IRREGULAR_CRYSTAL_RECIPE=
        local.primordial.alchemy.AlchemyKnowledge.registerBossOnly(new net.minecraft.resources.ResourceLocation(Primordial.ID,"irregular_crystal"));
    /** a fallen lord's statue */
    static final DeferredRegister<net.minecraft.world.level.block.Block> BLOCKS=DeferredRegister.create(ForgeRegistries.BLOCKS,Primordial.ID);
    static final DeferredRegister<net.minecraft.world.level.block.entity.BlockEntityType<?>> BLOCK_ENTITIES=DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES,Primordial.ID);
    public static final RegistryObject<net.minecraft.world.level.block.Block> STATUE=BLOCKS.register("sin_statue",SinStatueBlock::new);
    /** the gates to and from the Primordial Temple */
    public static final RegistryObject<net.minecraft.world.level.block.Block> PRIMORDIAL_GATE=BLOCKS.register("primordial_gate",local.primordial.spirit.PrimordialGateBlock::new);
    public static final RegistryObject<net.minecraft.world.level.block.entity.BlockEntityType<SinStatueBlock.Statue>> STATUE_ENTITY=BLOCK_ENTITIES.register("sin_statue",
        ()->net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(SinStatueBlock.Statue::new,STATUE.get()).build(null));
    /** the lord of a sin */
    public static EntityType<? extends SinBoss> lordType(local.primordial.affix.BossAffix sin) {
        return switch(sin){
            case PRIDE->PRIDE.get();case GREED->GREED.get();case SLOTH->SLOTH.get();case ENVY->ENVY.get();
            case WRATH->WRATH.get();case GLUTTONY->GLUTTONY.get();case LUST->LUST.get();
        };
    }
    /** what wakes a fallen lord's statue */
    public static Item reviver(EntityType<?> type) {
        if(type==null)return null;
        if(type==PRIMORDIAL.get())return PRIMORDIAL_SIGIL.get();
        if(type==BAIHU.get())return BAIHU_SIGIL.get();
        if(type==XUANWU.get())return XUANWU_SIGIL.get();
        if(type==QINGLONG.get())return QINGLONG_SIGIL.get();
        if(type==ZHUQUE.get())return ZHUQUE_SIGIL.get();
        if(type==FIRST_SAGE.get())return SAGE_SIGIL.get();
        for(var sin:local.primordial.affix.BossAffix.values())if(lordType(sin)==type)return SIGILS.get(sin).get();
        return null;
    }
    public static List<RegistryObject<Item>> eggs() {return List.of(PRIDE_EGG,WRATH_EGG,ENVY_EGG,GREED_EGG,LUST_EGG,GLUTTONY_EGG,SLOTH_EGG);}
    public static List<RegistryObject<? extends EntityType<? extends SinBoss>>> lords() {return List.of(PRIDE,GREED,SLOTH,ENVY,WRATH,GLUTTONY,LUST);}

    private SinBosses() {}
    /** loads the registrations above; called while Primordial's registers are being filled */
    public static void register() {}
    public static void register(IEventBus bus) {EFFECTS.register(bus);SinParticles.register(bus);BLOCKS.register(bus);BLOCK_ENTITIES.register(bus);}

    private static <T extends SinBoss> RegistryObject<EntityType<T>> boss(String id,EntityType.EntityFactory<T> factory,float width,float height) {
        return Primordial.ENTITIES.register(id,()->EntityType.Builder.of(factory,MobCategory.MONSTER).sized(width,height).fireImmune()
            .clientTrackingRange(12).build(Primordial.ID+":"+id));
    }
    private static RegistryObject<Item> egg(String id,RegistryObject<? extends EntityType<? extends Mob>> type,int base,int spots) {
        return Primordial.ITEMS.register(id+"_spawn_egg",()->new ForgeSpawnEggItem(type,base,spots,new Item.Properties()));
    }
    /**
     * The primordial charm recipe a lord carries, if any. Each world deals the four primordial recipes out to four of
     * the six lords other than Pride, chosen by its seed: the same lords every time in that world, different ones in another.
     */
    public static net.minecraft.resources.ResourceLocation primordialRecipe(net.minecraft.server.MinecraftServer server,local.primordial.affix.BossAffix sin) {
        if(server==null||sin==local.primordial.affix.BossAffix.PRIDE)return null;
        var lords=new java.util.ArrayList<local.primordial.affix.BossAffix>();
        for(var s:local.primordial.affix.BossAffix.values())if(s!=local.primordial.affix.BossAffix.PRIDE)lords.add(s);
        java.util.Collections.shuffle(lords,new java.util.Random(server.overworld().getSeed()^0x5157A11C4A5EL));
        var recipes=local.primordial.alchemy.AlchemyKnowledge.bossOnlyRecipes();
        int at=lords.indexOf(sin);
        return at>=0&&at<recipes.size()?recipes.get(at):null;
    }
    /** a Sin Lord's or its creatures' type: never spawned among the realm's random spirits */
    public static boolean isSinType(EntityType<?> type) {
        var key=ForgeRegistries.ENTITY_TYPES.getKey(type);
        return key!=null&&key.getNamespace().equals(Primordial.ID)&&(key.getPath().endsWith("_lord")||GUARDED.contains(key.getPath()));
    }
    /** the guardians and the Sage, which are not named as lords, and the lords' creatures */
    private static final java.util.Set<String> GUARDED=java.util.Set.of("envy_shade","sin_pillar","baihu","xuanwu","qinglong","zhuque","first_sage");
    static AttributeSupplier.Builder lord(double health,double armor,double speed,double attack) {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,health).add(Attributes.ARMOR,armor).add(Attributes.ARMOR_TOUGHNESS,6)
            .add(Attributes.MOVEMENT_SPEED,speed).add(Attributes.ATTACK_DAMAGE,attack).add(Attributes.KNOCKBACK_RESISTANCE,0.85)
            .add(Attributes.FOLLOW_RANGE,48).add(Attributes.ATTACK_KNOCKBACK,0.8);
    }

    @Mod.EventBusSubscriber(modid=Primordial.ID,bus=Mod.EventBusSubscriber.Bus.MOD)
    public static final class SinBossSetup {
        @SubscribeEvent public static void createSinLordAttributes(EntityAttributeCreationEvent event) {
            event.put(PRIDE.get(),lord(PrideLord.HEALTH,14,0.3,4).build());
            event.put(WRATH.get(),lord(400,12,0.3,14).add(Attributes.KNOCKBACK_RESISTANCE,1).build());
            event.put(ENVY.get(),lord(360,8,0.36,10).build());
            event.put(GREED.get(),lord(400,12,0.38,9).build());
            event.put(LUST.get(),lord(360,6,0.3,6).build());
            event.put(GLUTTONY.get(),lord(420,14,0.3,12).build());
            event.put(SLOTH.get(),lord(420,16,0.22,15).build());
            event.put(PRIMORDIAL.get(),lord(PrimordialLord.HEALTH,20,0.28,14).add(Attributes.KNOCKBACK_RESISTANCE,1).add(Attributes.FOLLOW_RANGE,64).build());
            event.put(SIN_PILLAR.get(),SinPillar.attributes().build());
            event.put(BAIHU.get(),lord(BaihuLord.HEALTH,4,0.36,22).add(Attributes.ARMOR_TOUGHNESS,0).add(Attributes.KNOCKBACK_RESISTANCE,0.6).build());
            event.put(XUANWU.get(),lord(XuanwuLord.HEALTH,12,0.16,10).add(Attributes.KNOCKBACK_RESISTANCE,1).add(Attributes.FOLLOW_RANGE,48).build());
            event.put(QINGLONG.get(),lord(QinglongLord.HEALTH,8,0.3,14).add(Attributes.KNOCKBACK_RESISTANCE,1).build());
            event.put(ZHUQUE.get(),lord(ZhuqueLord.HEALTH,6,0.3,16).add(Attributes.FLYING_SPEED,0.6).build());
            event.put(FIRST_SAGE.get(),lord(FirstSage.HEALTH,18,0.28,16).add(Attributes.ARMOR_TOUGHNESS,8).add(Attributes.KNOCKBACK_RESISTANCE,1).add(Attributes.FOLLOW_RANGE,64).build());
            event.put(ENVY_SHADE.get(),Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,12).add(Attributes.MOVEMENT_SPEED,0.34)
                .add(Attributes.ATTACK_DAMAGE,5).add(Attributes.FOLLOW_RANGE,32).build());
        }
    }
    @Mod.EventBusSubscriber(modid=Primordial.ID)
    public static final class SinBossEvents {
        /** Gluttony's curse: no healing at all while it lasts */
        @SubscribeEvent public static void devouredHeal(LivingHealEvent event) {
            if(DEVOURED.isPresent()&&event.getEntity().hasEffect(DEVOURED.get()))event.setCanceled(true);
        }
    }
}
