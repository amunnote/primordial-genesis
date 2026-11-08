package local.primordial.achievement;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import local.primordial.Primordial;
import local.primordial.affix.AffixEncounters;
import local.primordial.affix.AffixState;
import local.primordial.affix.BossAffix;
import local.primordial.affix.MobAffix;
import local.primordial.alchemy.AlchemyKnowledge;
import local.primordial.boss.FirstSage;
import local.primordial.boss.SinBoss;
import local.primordial.boss.SinBosses;
import local.primordial.effect.GemSwordPower;
import local.primordial.effect.Protection;
import local.primordial.item.AlchemyCharmItem;
import local.primordial.spirit.Guardian;
import local.primordial.spirit.PrimordialTemple;
import local.primordial.spirit.SageSanctum;
import local.primordial.spirit.SpiritIslands;
import local.primordial.spirit.SpiritRealm;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * The player-visible achievements: two tabs, 炼成之道 primordial:forge/* (25) and 讨伐之路 primordial:hunt/* (35).
 * Like the hidden known/ and affix/ advancements they have one impossible criterion and are awarded here, from hooks in
 * the systems they are about; awarding is all this class does, it never changes play. The 17 trunk nodes are
 * {@link #TRUNK} (ids starting trunk_); Sage's Gift grows by every advancement, these among them ({@link local.primordial.sage.SageProgress}).
 * <p>
 * What has to be counted over time is kept on the player under {@link Player#PERSISTED_NBT_TAG}; what the First Sage's
 * fight must remember (who wore the Ascension Charm in it) is kept on the Sage itself.
 */
@Mod.EventBusSubscriber(modid=Primordial.ID)
public final class Achievements {
    public static final String CRITERION="earned";
    // ------------------------------------------------------------------ 炼成之道
    public static final String FORGE_ROOT="forge/trunk_root",FIRST_FURNACE="forge/trunk_first_furnace",TRANSMUTATION="forge/trunk_transmutation",
        TRUTH="forge/trunk_truth",PRIMORDIAL="forge/trunk_primordial",ORIGIN_INGOT="forge/trunk_origin_ingot",WORLD_CORE="forge/trunk_world_core",
        ASCENSION="forge/trunk_ascension";
    public static final String STUDY_10="forge/study_10",STUDY_30="forge/study_30",STUDY_60="forge/study_60",STUDY_ALL="forge/study_all";
    public static final String FRAGMENT_FIRST="forge/fragment_first",FRAGMENT_MYSTERY="forge/fragment_mystery",FRAGMENT_LUCK="forge/fragment_luck";
    public static final String GEM_CRYSTAL="forge/gem_irregular_crystal",GEM_BLADE="forge/gem_blade",GEM_SWORD="forge/gem_sword",GEM_TWIN="forge/gem_twin";
    public static final String CHARM_FIRST="forge/charm_first",CHARM_LAW="forge/charm_law",CHARM_FOUR="forge/charm_primordial_four",
        CHARM_CLEAR_WORLD="forge/charm_clear_world",CHARM_INVIOLABLE="forge/charm_inviolable",CHARM_SAGE_HEART="forge/charm_sage_heart";
    // ------------------------------------------------------------------ 讨伐之路
    public static final String HUNT_ROOT="hunt/trunk_root",INSPIRATION="hunt/trunk_inspiration",SPIRIT_REALM="hunt/trunk_spirit_realm",
        FIRST_SIN="hunt/trunk_first_sin",SEVEN_SINS="hunt/trunk_seven_sins",FOUR_SYMBOLS="hunt/trunk_four_symbols",
        PRIMORDIAL_LORD="hunt/trunk_primordial_lord",COURT="hunt/trunk_court_of_truth",FIRST_SAGE="hunt/trunk_first_sage";
    public static final String CODEX_15="hunt/codex_15",CODEX_30="hunt/codex_30",LUCK_100="hunt/luck_100",SEVEN_AFFIX_KILL="hunt/seven_affix_kill",
        SIGIL_FIRST="hunt/sigil_first",SIGIL_SEVEN="hunt/sigil_seven",SOUL_FIRST="hunt/soul_first",SOUL_THREE="hunt/soul_three";
    public static final String STATUE_REVIVE="hunt/statue_revive",STATUE_REMATCH="hunt/statue_rematch",WITNESS="hunt/witness";
    /** the bonds (AffixBonds): one held, three held at once; collection nodes beside the codex, not trunk */
    public static final String BOND_FIRST="hunt/bond_first",BOND_THREE="hunt/bond_three";
    public static final String MORTAL="hunt/forbidden_mortal",FOUR_CIRCLES="hunt/forbidden_four_circles",OPUS_SURVIVOR="hunt/forbidden_opus_survivor",
        GEM_ANSWER="hunt/forbidden_gem_answer";
    /** the sin lords' (hunt/sin_<sin>) and the guardians' (hunt/symbol_<guardian>) own nodes */
    public static String sin(BossAffix sin) {return "hunt/sin_"+sin.id();}
    public static String symbol(Guardian guardian) {return "hunt/symbol_"+guardian.id;}

    /** the two trunks, 8 + 9 */
    public static final Set<ResourceLocation> TRUNK=Set.of(
        id(FORGE_ROOT),id(FIRST_FURNACE),id(TRANSMUTATION),id(TRUTH),id(PRIMORDIAL),id(ORIGIN_INGOT),id(WORLD_CORE),id(ASCENSION),
        id(HUNT_ROOT),id(INSPIRATION),id(SPIRIT_REALM),id(FIRST_SIN),id(SEVEN_SINS),id(FOUR_SYMBOLS),id(PRIMORDIAL_LORD),id(COURT),id(FIRST_SAGE));
    public static boolean isTrunk(ResourceLocation advancement) {return advancement!=null&&TRUNK.contains(advancement);}

    /** on the player: counts and collections kept toward achievements */
    static final String REVIVED_FELLED="primordial:ach_revived_felled",SIGILS="primordial:ach_sigils",SOULS="primordial:ach_souls",
        PRIMORDIAL_CHARMS="primordial:ach_primordial_charms";
    /** on a lord woken from its statue, until it falls */
    static final String REVIVED="primordial:ach_revived";
    /** on the First Sage: the players who wore the Ascension Charm near it during the current fight */
    static final String ASCENDED="primordial:ach_ascended";
    public static final int REMATCHES=5;
    private Achievements() {}

    static ResourceLocation id(String path) {return new ResourceLocation(Primordial.ID,path);}
    public static void award(Player player,String path) {
        if(!(player instanceof ServerPlayer server)||server instanceof FakePlayer||server.server==null)return;
        var advancement=server.server.getAdvancements().getAdvancement(id(path));
        if(advancement!=null&&!server.getAdvancements().getOrStartProgress(advancement).isDone())
            server.getAdvancements().award(advancement,CRITERION);
    }
    private static Item item(String path) {return ForgeRegistries.ITEMS.getValue(id(path));}
    private static CompoundTag persisted(Player player) {return player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);}
    /** add bits to a mask kept on the player; returns the mask after */
    private static int collect(Player player,String key,int bits) {
        var data=player.getPersistentData();var kept=data.getCompound(Player.PERSISTED_NBT_TAG);
        int before=kept.getInt(key),after=before|bits;
        if(after!=before){kept.putInt(key,after);data.put(Player.PERSISTED_NBT_TAG,kept);}
        return after;
    }

    // ------------------------------------------------------------------ the bonds (AffixStatuses, when the bonds held change)
    public static void bonds(Player player,int held) {
        if(held>=1)award(player,BOND_FIRST);
        if(held>=3)award(player,BOND_THREE);
    }

    // ------------------------------------------------------------------ the altars (AltarCrafting)
    /** a recipe performed on an altar of this tier (1 basic .. 4 primordial), giving this */
    public static void altarCrafted(Player player,int altarTier,ItemStack result) {
        switch(altarTier){case 1->award(player,FIRST_FURNACE);case 2->award(player,TRANSMUTATION);case 3->award(player,TRUTH);case 4->award(player,PRIMORDIAL);default->{}}
        if(result.is(Primordial.ORIGIN_INGOT.get()))award(player,ORIGIN_INGOT);
        if(result.is(item("world_core")))award(player,WORLD_CORE);
        if(result.is(Primordial.CHARM.get()))award(player,ASCENSION);
        if(result.is(Primordial.IRREGULAR_CRYSTAL.get()))award(player,GEM_CRYSTAL);
        if(result.is(Primordial.GEM_SWORD.get()))award(player,GEM_SWORD);
        if(result.is(Primordial.CLEAR_WORLD_CHARM.get()))award(player,CHARM_CLEAR_WORLD);
        if(result.is(Primordial.INVIOLABLE_CHARM.get()))award(player,CHARM_INVIOLABLE);
        if(result.is(Primordial.MYSTERY_RECIPE.get()))award(player,FRAGMENT_MYSTERY);
        primordialCharm(player,result);
    }
    /** learned recipes broken into fragments */
    public static void fragmented(Player player) {award(player,FRAGMENT_FIRST);}
    /** a mystery recipe opened on an enchanting table (it only ever becomes a recipe not yet learned) */
    public static void mysteryOpened(Player player) {award(player,FRAGMENT_LUCK);}
    /** a Gem Sword Blade taken from a stonecutter */
    public static void bladeCut(Player player) {award(player,GEM_BLADE);}

    // ------------------------------------------------------------------ recipes known (AlchemyKnowledge)
    public static void recipesKnown(Player player,int known,int all) {
        if(known>=10)award(player,STUDY_10);
        if(known>=30)award(player,STUDY_30);
        if(known>=60)award(player,STUDY_60);
        if(all>0&&known>=all)award(player,STUDY_ALL);
    }

    // ------------------------------------------------------------------ affixes met (the affix/ advancements)
    @SubscribeEvent public static void advancement(AdvancementEvent.AdvancementEarnEvent event) {
        if(event.getEntity() instanceof ServerPlayer player&&AffixEncounters.isEncounter(event.getAdvancement().getId()))affixesMet(player);
    }
    public static void affixesMet(ServerPlayer player) {
        int met=0;
        for(var affix:MobAffix.values())if(AffixEncounters.met(player,affix.id()))met++;
        for(var sin:BossAffix.values())if(AffixEncounters.met(player,sin.id()))met++;
        if(met>0)award(player,HUNT_ROOT);
        if(met>=15)award(player,CODEX_15);
        if(met>=30)award(player,CODEX_30);
    }
    /** a mob carrying seven affixes of its own (its sin counts as one, Amplify's loans do not), slain by a player */
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void death(LivingDeathEvent event) {
        if(event.isCanceled()||!(event.getEntity() instanceof Mob mob)||mob.level().isClientSide)return;
        var state=AffixState.get(mob);if(state==null)return;
        int own=state.originalBoss!=null?1:0;
        for(var affix:state.affixes)if(!state.temporary.contains(affix))own++;
        if(own<7)return;
        Player killer=event.getSource().getEntity() instanceof Player p?p:mob.getLastHurtByMob() instanceof Player q?q:null;
        if(killer!=null)award(killer,SEVEN_AFFIX_KILL);
    }

    // ------------------------------------------------------------------ the Spirit Realm
    /** drinking a Potion of Inspiration */
    @SubscribeEvent public static void drink(LivingEntityUseItemEvent.Finish event) {
        if(event.getEntity() instanceof ServerPlayer player&&event.getItem().is(Items.POTION)&&SpiritRealm.INSPIRATION_POTION.isPresent()
            &&net.minecraft.world.item.alchemy.PotionUtils.getPotion(event.getItem())==SpiritRealm.INSPIRATION_POTION.get())
            award(player,INSPIRATION);
    }
    /** carried up into the Court of Truth (SageSanctum.up) */
    public static void enteredCourt(Player player) {award(player,COURT);}

    // ------------------------------------------------------------------ the lords' falls (PrimordialTemple's records, SinBoss.die)
    /** re-read what the temple remembers of a player's fights */
    public static void bossesFelled(Player player) {
        var sins=PrimordialTemple.defeated(player);
        for(var sin:sins)award(player,sin(sin));
        if(!sins.isEmpty())award(player,FIRST_SIN);
        if(sins.size()==BossAffix.values().length)award(player,SEVEN_SINS);
        var guardians=PrimordialTemple.guardians(player);
        for(var guardian:guardians)award(player,symbol(guardian));
        if(guardians.size()==Guardian.values().length)award(player,FOUR_SYMBOLS);
        if(PrimordialTemple.primordialFelled(player))award(player,PRIMORDIAL_LORD);
        if(player instanceof ServerPlayer server&&server.getStats().getValue(Stats.ENTITY_KILLED.get(SinBosses.FIRST_SAGE.get()))>0)award(player,FIRST_SAGE);
    }
    /** within the witness range when the Primordial Lord fell (PrimordialTemple.recordPrimordialDefeat) */
    public static void witnessedPrimordial(Player player) {award(player,WITNESS);}
    /** a lord woken from its statue by this player's sigil */
    public static void statueRevived(Player player,SinBoss lord) {
        award(player,STATUE_REVIVE);
        if(lord!=null)lord.getPersistentData().putBoolean(REVIVED,true);
    }
    /** any lord, guardian or the Sage has fallen: if it had been woken from a statue, its fall counts toward 旧敌新战 */
    public static void bossFelled(ServerLevel level,SinBoss boss) {
        var data=boss.getPersistentData();
        if(!data.getBoolean(REVIVED))return;
        data.remove(REVIVED);   // once, however often die() runs
        for(var player:fighters(level,boss)){
            var kept=player.getPersistentData();var persisted=kept.getCompound(Player.PERSISTED_NBT_TAG);
            int felled=persisted.getInt(REVIVED_FELLED)+1;
            persisted.putInt(REVIVED_FELLED,felled);kept.put(Player.PERSISTED_NBT_TAG,persisted);
            if(felled>=REMATCHES)award(player,STATUE_REMATCH);
        }
    }
    /** those who saw a lord through to its fall, by the temple's own rule: in the fight, within its witness range */
    static List<ServerPlayer> fighters(ServerLevel level,SinBoss boss) {
        var out=new ArrayList<ServerPlayer>();
        for(var player:level.players())if(SinBoss.contender(player)&&player.position().distanceTo(boss.position())<=PrimordialTemple.WITNESS)out.add(player);
        return out;
    }

    // ------------------------------------------------------------------ the First Sage's fight
    /** each tick of its fight: remember everyone near it wearing the Ascension Charm */
    public static void sageFight(FirstSage sage,ServerLevel level) {
        var list=sage.getPersistentData().getList(ASCENDED,Tag.TAG_STRING);boolean changed=false;
        for(var player:level.players())if(SinBoss.contender(player)&&player.position().distanceTo(sage.position())<=PrimordialTemple.WITNESS
            &&Protection.active(player)&&!contains(list,player.getUUID())){list.add(StringTag.valueOf(player.getUUID().toString()));changed=true;}
        if(changed)sage.getPersistentData().put(ASCENDED,list);
    }
    /** the fight given up (the Sage rests and heals): it starts over */
    public static void sageReset(FirstSage sage) {sage.getPersistentData().remove(ASCENDED);}
    private static boolean contains(ListTag list,UUID id) {
        String s=id.toString();for(int i=0;i<list.size();i++)if(list.getString(i).equals(s))return true;return false;
    }
    /** the ward shattered: all four circles purified within this one whitening */
    public static void circlesPurified(FirstSage sage,ServerLevel level) {for(var player:fighters(level,sage))award(player,FOUR_CIRCLES);}
    /** struck by the Final Transmutation (the hurt went through) and still alive */
    public static void opusSurvived(Player player) {if(player.isAlive()&&!player.isDeadOrDying())award(player,OPUS_SURVIVOR);}
    /** the Great Work is done */
    public static void firstSageFelled(ServerLevel level,FirstSage sage,DamageSource source) {
        sageFight(sage,level);   // whoever wears it at the very end, too
        var ascended=sage.getPersistentData().getList(ASCENDED,Tag.TAG_STRING);
        for(var player:fighters(level,sage)){
            award(player,FIRST_SAGE);
            if(!contains(ascended,player.getUUID()))award(player,MORTAL);
        }
        // the killing blow given with the Gem Sword: its every hit is a slaying (GemSwordPower), so the blow that killed
        // was the sword's exactly when the death comes while a slaying is under way
        if(source.getEntity() instanceof ServerPlayer killer&&(GemSwordPower.slaying()
            ||source.getDirectEntity()==killer&&killer.getMainHandItem().is(Primordial.GEM_SWORD.get())))award(killer,GEM_ANSWER);
        sageReset(sage);
    }

    // ------------------------------------------------------------------ what a player has, wears and where they are
    private static final int SCAN_TICKS=20;
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event) {
        if(event.phase!=TickEvent.Phase.END||!(event.player instanceof ServerPlayer player)||player.tickCount%SCAN_TICKS!=0)return;
        scan(player);
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if(!(event.getEntity() instanceof ServerPlayer player))return;
        affixesMet(player);bossesFelled(player);scan(player);
    }
    static void scan(ServerPlayer player) {
        if(player.level().dimension()==net.minecraft.world.level.Level.OVERWORLD&&player.level().getBiome(player.blockPosition()).is(local.primordial.qinglong.QinglongWorldgen.BIOME))award(player,"hunt/enter_qinglong_grove");
        if(player.level().dimension()==net.minecraft.world.level.Level.OVERWORLD&&player.level().getBiome(player.blockPosition()).is(local.primordial.baihu.BaihuWorldgen.BIOME))award(player,"hunt/enter_baihu_wastes");
        if(player.level().dimension()==net.minecraft.world.level.Level.OVERWORLD&&player.level().getBiome(player.blockPosition()).is(local.primordial.zhuque.ZhuqueWorldgen.BIOME))award(player,"hunt/enter_zhuque_fields");
        if(player.level().dimension()==net.minecraft.world.level.Level.OVERWORLD&&player.level().getBiome(player.blockPosition()).is(local.primordial.xuanwu.XuanwuWorldgen.BIOME))award(player,"hunt/enter_xuanwu_sea");
        if(persisted(player).getBoolean(local.primordial.effect.AlchemistEvents.GIFT_GIVEN))award(player,FORGE_ROOT);
        var carried=new ArrayList<ItemStack>();
        var inventory=player.getInventory();
        for(int i=0;i<inventory.getContainerSize();i++){var stack=inventory.getItem(i);if(!stack.isEmpty())carried.add(stack);}
        var worn=new ArrayList<ItemStack>();
        CuriosApi.getCuriosInventory(player).ifPresent(inv->{for(var result:inv.findCurios(stack->!stack.isEmpty()))worn.add(result.stack());});
        carried.addAll(worn);
        boolean ascension=false,gemSword=false;int sigils=0,souls=0;
        var sinSigils=BossAffix.values();var soulItems=new Item[]{item("overworld_soul"),item("nether_soul"),item("end_soul")};
        for(var stack:carried){
            if(stack.is(Primordial.ALCHEMIST_GIFT.get()))award(player,FORGE_ROOT);
            if(stack.is(Primordial.SAGE_HEART.get()))award(player,CHARM_SAGE_HEART);
            if(stack.is(Primordial.CHARM.get()))ascension=true;
            if(stack.is(Primordial.GEM_SWORD.get()))gemSword=true;
            for(int i=0;i<sinSigils.length;i++)if(stack.is(SinBosses.SIGILS.get(sinSigils[i]).get()))sigils|=1<<i;
            for(int i=0;i<soulItems.length;i++)if(soulItems[i]!=null&&soulItems[i]!=Items.AIR&&stack.is(soulItems[i]))souls|=1<<i;
            primordialCharm(player,stack);
        }
        if(ascension&&gemSword)award(player,GEM_TWIN);
        for(var stack:worn){
            if(stack.getItem() instanceof AlchemyCharmItem)award(player,CHARM_FIRST);
            if(LAW_CHARMS.contains(ForgeRegistries.ITEMS.getKey(stack.getItem())))award(player,CHARM_LAW);
        }
        if(sigils!=0||persisted(player).getInt(SIGILS)!=0){
            int all=collect(player,SIGILS,sigils);
            award(player,SIGIL_FIRST);
            if(all==(1<<sinSigils.length)-1)award(player,SIGIL_SEVEN);
        }
        if(souls!=0||persisted(player).getInt(SOULS)!=0){
            int all=collect(player,SOULS,souls);
            award(player,SOUL_FIRST);
            if(all==7)award(player,SOUL_THREE);
        }
        if(player.getAttributeValue(Attributes.LUCK)>=100)award(player,LUCK_100);
        if(SpiritRealm.inRealm(player)){
            var island=SpiritIslands.islandAt(player.getBlockX(),player.getBlockZ());
            if(island!=null&&SpiritIslands.isOrigin(island.cellX(),island.cellZ()))award(player,SPIRIT_REALM);
            if(SageSanctum.within(player.position()))award(player,COURT);
        }
        bossesFelled(player);
    }
    /** the tier 3 charms (the recipes needing a Truth altar): the four laws and the two tier 3 defences */
    static final Set<ResourceLocation> LAW_CHARMS=Set.of(id("law_of_arms_charm"),id("law_of_arcana_charm"),id("law_of_gale_charm"),
        id("law_of_tailwind_charm"),id("unyielding_body_charm"),id("arcane_harmony_charm"));
    /** the four primordial (tier 4) charms, kept as they are had: they are spent on the supreme ones, so need not be held at once */
    static void primordialCharm(Player player,ItemStack stack) {
        int bit=stack.is(Primordial.OMNI_AMPLIFY_CHARM.get())?1:stack.is(Primordial.FLEETING_ETERNITY_CHARM.get())?2
            :stack.is(Primordial.UNBREAKABLE_CHARM.get())?4:stack.is(Primordial.SPELL_ANNUL_CHARM.get())?8:0;
        if(bit==0)return;
        if(collect(player,PRIMORDIAL_CHARMS,bit)==15)award(player,CHARM_FOUR);
    }
}
