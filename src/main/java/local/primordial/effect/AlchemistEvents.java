package local.primordial.effect;

import local.primordial.Primordial;
import local.primordial.affix.AffixCombat;
import local.primordial.affix.AffixEncounters;
import local.primordial.affix.AffixState;
import local.primordial.item.AlchemyCharmItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Alchemist's Gift handout, affix encounters for its book, the arcane charms' non-physical bonus and the defensive charms. */
@Mod.EventBusSubscriber(modid=Primordial.ID)
public final class AlchemistEvents {
    public static final String GIFT_GIVEN="primordial:alchemist_gift_given";
    private AlchemistEvents() {}

    @SubscribeEvent public static void alchemistGiftOnFirstLogin(PlayerEvent.PlayerLoggedInEvent event) {
        var player=event.getEntity();var data=player.getPersistentData();
        CompoundTag persisted=data.getCompound(Player.PERSISTED_NBT_TAG);
        if(persisted.getBoolean(GIFT_GIVEN))return;
        // keep trying on later logins if the inventory is full now
        if(player.getInventory().add(new net.minecraft.world.item.ItemStack(Primordial.ALCHEMIST_GIFT.get()))){
            persisted.putBoolean(GIFT_GIVEN,true);data.put(Player.PERSISTED_NBT_TAG,persisted);
            local.primordial.guide.GuideEvents.gift(player);
        }
    }
    @SubscribeEvent public static void alchemistGiftFlagOnClone(PlayerEvent.Clone event) {
        if(!event.getOriginal().getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getBoolean(GIFT_GIVEN))return;
        var data=event.getEntity().getPersistentData();var persisted=data.getCompound(Player.PERSISTED_NBT_TAG);
        persisted.putBoolean(GIFT_GIVEN,true);data.put(Player.PERSISTED_NBT_TAG,persisted);
    }

    /** dealt damage to an affixed mob */
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void affixEncounterOnDamage(LivingDamageEvent event) {
        if(event.getAmount()>0&&event.getSource().getEntity() instanceof ServerPlayer player&&event.getEntity() instanceof Mob mob)
            AffixEncounters.record(player,AffixState.get(mob));
    }
    /** attacked by an affixed mob, whether or not the hit lands */
    @SubscribeEvent(priority=EventPriority.LOWEST,receiveCanceled=true) public static void affixEncounterOnAttack(LivingAttackEvent event) {
        if(event.getEntity() instanceof ServerPlayer player&&event.getSource().getEntity() instanceof Mob mob)
            AffixEncounters.record(player,AffixState.get(mob));
    }

    @SubscribeEvent public static void arcaneNonPhysicalBonus(LivingHurtEvent event) {
        if(event.getEntity().level().isClientSide||!(event.getSource().getEntity() instanceof Player player)
            ||AffixCombat.physical(event.getSource())||!local.primordial.affix.AffixDamage.mitigable(event.getSource()))return;
        double bonus=AlchemyCharmItem.nonPhysicalBonus(player);
        if(bonus>0)event.setAmount((float)Math.min(Float.MAX_VALUE,event.getAmount()*(1+bonus)));
    }
    /** archery charms: the wearer's projectiles hit harder */
    @SubscribeEvent public static void charmProjectileBonus(LivingHurtEvent event) {
        if(event.getEntity().level().isClientSide||!event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_PROJECTILE)
            ||!(event.getSource().getEntity() instanceof Player player))return;
        double bonus=AlchemyCharmItem.projectileBonus(player);
        if(bonus>0)event.setAmount((float)Math.min(Float.MAX_VALUE,event.getAmount()*(1+bonus)));
    }
    /** Pact of Swiftness: whoever hurts the wearer becomes feeble; feeble attackers deal less damage */
    @SubscribeEvent public static void charmFeebleness(LivingHurtEvent event) {
        var victim=event.getEntity();
        if(victim.level().isClientSide||!(event.getSource().getEntity() instanceof net.minecraft.world.entity.LivingEntity attacker)||attacker==victim)return;
        if(local.primordial.affix.AffixStatuses.feeble(attacker)>0&&local.primordial.affix.AffixDamage.scalable(event.getSource()))event.setAmount((float)(event.getAmount()*(1-local.primordial.affix.AffixStatuses.FEEBLE_PENALTY)));
        if(victim instanceof Player&&AlchemyCharmItem.feebleOnHit(victim))
            local.primordial.affix.AffixStatuses.applyFeeble(attacker);
    }
    private static final String DRAW_START="primordial:draw_start";
    /** Pact of the Swift String: bows and crossbows charge faster (runs on both sides so the pull animation matches) */
    @SubscribeEvent public static void charmDrawStart(net.minecraftforge.event.entity.living.LivingEntityUseItemEvent.Start event) {
        var user=event.getEntity();
        if(event.getItem().getItem() instanceof net.minecraft.world.item.ProjectileWeaponItem&&AlchemyCharmItem.drawSpeed(user)>0)
            user.getPersistentData().putLong(DRAW_START,user.level().getGameTime());
        else user.getPersistentData().remove(DRAW_START);
    }
    @SubscribeEvent public static void charmDrawTick(net.minecraftforge.event.entity.living.LivingEntityUseItemEvent.Tick event) {
        var user=event.getEntity();var data=user.getPersistentData();
        if(!data.contains(DRAW_START)||!(event.getItem().getItem() instanceof net.minecraft.world.item.ProjectileWeaponItem))return;
        double speed=AlchemyCharmItem.drawSpeed(user);if(speed<=0)return;
        event.setDuration(Math.min(event.getDuration(),drawRemaining(event.getItem().getUseDuration(),user.level().getGameTime()-data.getLong(DRAW_START),speed)));
    }
    /** use ticks left so that, after this tick's countdown, the charge stands at elapsed*(1+speed) */
    public static int drawRemaining(int useDuration,long elapsed,double speed) {
        return (int)Math.max(1,useDuration-Math.floor(elapsed*(1+speed))+1);
    }
    /** free arrows must not be picked back up (tridents keep their pickup) */
    @SubscribeEvent public static void charmFreeArrows(net.minecraftforge.event.entity.EntityJoinLevelEvent event) {
        if(event.getLevel().isClientSide||!(event.getEntity() instanceof net.minecraft.world.entity.projectile.AbstractArrow arrow)
            ||arrow instanceof net.minecraft.world.entity.projectile.ThrownTrident||arrow.pickup!=net.minecraft.world.entity.projectile.AbstractArrow.Pickup.ALLOWED)return;
        if(arrow.getOwner() instanceof Player player&&AlchemyCharmItem.infiniteAmmo(player))
            arrow.pickup=net.minecraft.world.entity.projectile.AbstractArrow.Pickup.CREATIVE_ONLY;
    }
    /** Law of the Gale: the wearer's hits land even while the target is still flashing from the last one */
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void charmIgnoreInvulnerability(LivingAttackEvent event) {
        var victim=event.getEntity();
        if(victim.level().isClientSide||!(event.getSource().getEntity() instanceof Player player)||player==victim)return;
        if(victim.invulnerableTime>0&&AlchemyCharmItem.ignoresInvulnerability(player))victim.invulnerableTime=0;
    }
    /**
     * Novice librarians: the two entry-level combat charms join the enchanted book trade's pool, one entry each
     * beside every tradeable enchantment, so the "book" a librarian offers is sometimes a charm.
     */
    @SubscribeEvent public static void charmLibrarianTrades(net.minecraftforge.event.village.VillagerTradesEvent event) {
        if(event.getType()!=net.minecraft.world.entity.npc.VillagerProfession.LIBRARIAN)return;
        var novice=event.getTrades().get(1);
        // the vanilla class is package-private; runtime class names are Mojang's, so match by name
        for(int i=0;i<novice.size();i++)if(isEnchantedBookTrade(novice.get(i)))novice.set(i,new CharmOrBook(novice.get(i)));
    }
    public static boolean isEnchantedBookTrade(net.minecraft.world.entity.npc.VillagerTrades.ItemListing listing) {
        return listing.getClass().getSimpleName().equals("EnchantBookForEmeralds");
    }
    public record CharmOrBook(net.minecraft.world.entity.npc.VillagerTrades.ItemListing book) implements net.minecraft.world.entity.npc.VillagerTrades.ItemListing {
        public static java.util.List<net.minecraftforge.registries.RegistryObject<net.minecraft.world.item.Item>> charms() {
            return java.util.List.of(Primordial.THOUSAND_BLADE_CHARM,Primordial.QUICKSTRING_CHARM);
        }
        /** how many entries the vanilla trade draws its enchantment from */
        public static int enchantments() {
            int n=0;for(var e:net.minecraftforge.registries.ForgeRegistries.ENCHANTMENTS)if(e.isTradeable())n++;return n;
        }
        @Override public net.minecraft.world.item.trading.MerchantOffer getOffer(net.minecraft.world.entity.Entity trader,net.minecraft.util.RandomSource random) {
            var charms=charms();int roll=random.nextInt(enchantments()+charms.size());
            if(roll>=charms.size())return book.getOffer(trader,random);
            // one sale, never restocked (see CharmTradeRestockMixin)
            return new net.minecraft.world.item.trading.MerchantOffer(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.EMERALD,24),
                new net.minecraft.world.item.ItemStack(charms.get(roll).get()),1,5,0.05F);
        }
        public static boolean isCharmOffer(net.minecraft.world.item.trading.MerchantOffer offer) {
            var result=offer.getResult();
            for(var charm:charms())if(charm.isPresent()&&result.is(charm.get()))return true;
            return false;
        }
    }
    /** Omni Amplify: each physical hit the wearer lands is echoed a tick later by a magic hit of half its weight */
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void charmEcho(LivingHurtEvent event) {
        var victim=event.getEntity();
        if(victim.level().isClientSide||!(event.getSource().getEntity() instanceof Player player)||player==victim||!AffixCombat.physical(event.getSource()))return;
        double echo=AlchemyCharmItem.echo(player);
        if(echo<=0||event.getAmount()<=0||!(victim.level() instanceof net.minecraft.server.level.ServerLevel level))return;
        float amount=(float)(event.getAmount()*echo);
        var server=level.getServer();
        server.tell(new net.minecraft.server.TickTask(server.getTickCount()+1,()->{
            if(!victim.isAlive()||!player.isAlive())return;
            victim.invulnerableTime=0;
            victim.hurt(player.damageSources().indirectMagic(player,player),amount);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANTED_HIT,victim.getX(),victim.getY(0.6),victim.getZ(),10,0.3,0.4,0.3,0.2);
        }));
    }
    /** Fleeting Eternity: whatever the wearer hits is held still for a second */
    @SubscribeEvent public static void charmStasis(LivingHurtEvent event) {
        var victim=event.getEntity();
        if(victim.level().isClientSide||!(event.getSource().getEntity() instanceof Player player)||player==victim||!AlchemyCharmItem.stasis(player))return;
        victim.setDeltaMovement(0,Math.min(0,victim.getDeltaMovement().y),0);victim.hurtMarked=true;
        victim.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN,20,5,false,false),player);
        if(victim.level() instanceof net.minecraft.server.level.ServerLevel level)
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,victim.getX(),victim.getY(0.5),victim.getZ(),6,0.3,0.4,0.3,0.01);
    }
    /** Unbreakable Principle: no single physical hit takes more than a share of the wearer's maximum health */
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void charmHitCap(LivingDamageEvent event) {
        var wearer=event.getEntity();
        if(wearer.level().isClientSide||!(wearer instanceof Player)||!AffixCombat.physical(event.getSource())
            ||local.primordial.affix.AffixBonds.uncapped(wearer,event.getSource()))return;
        double cap=AlchemyCharmItem.hitCap(wearer);
        if(cap>0)event.setAmount((float)Math.min(event.getAmount(),wearer.getMaxHealth()*cap));
    }
    /** Spell Annulment: no harmful effect takes hold of the wearer */
    @SubscribeEvent public static void charmCleanse(net.minecraftforge.event.entity.living.MobEffectEvent.Applicable event) {
        var wearer=event.getEntity();
        if(wearer.level().isClientSide||!(wearer instanceof Player)||event.getEffectInstance().getEffect().getCategory()!=net.minecraft.world.effect.MobEffectCategory.HARMFUL)return;
        if(AlchemyCharmItem.cleanse(wearer))event.setResult(net.minecraftforge.eventbus.api.Event.Result.DENY);
    }
    private static boolean reflecting;
    /** Spell Annulment: a share of non-physical harm goes back to whoever dealt it */
    @SubscribeEvent(priority=EventPriority.HIGH) public static void charmReflect(LivingHurtEvent event) {
        var wearer=event.getEntity();
        if(reflecting||wearer.level().isClientSide||!(wearer instanceof Player)||AffixCombat.physical(event.getSource())
            ||!(event.getSource().getEntity() instanceof net.minecraft.world.entity.LivingEntity attacker)||attacker==wearer)return;
        double share=AlchemyCharmItem.reflect(wearer);
        if(share<=0||event.getAmount()<=0)return;
        reflecting=true;
        try{attacker.hurt(wearer.damageSources().indirectMagic(wearer,wearer),(float)(event.getAmount()*share));}
        finally{reflecting=false;}
    }
    /** Transparent World: now and then a hit sees clean through its target, two and a half times the harm */
    public static final float INSIGHT_MULTIPLIER=2.5F;
    @SubscribeEvent(priority=EventPriority.LOW) public static void charmInsight(LivingHurtEvent event) {
        var victim=event.getEntity();
        if(victim.level().isClientSide||!(event.getSource().getEntity() instanceof Player player)||player==victim||event.getAmount()<=0
            ||!local.primordial.affix.AffixDamage.scalable(event.getSource()))return;
        double chance=AlchemyCharmItem.insight(player);
        if(chance<=0||player.getRandom().nextDouble()>=chance)return;
        event.setAmount(event.getAmount()*INSIGHT_MULTIPLIER);
        if(victim.level() instanceof net.minecraft.server.level.ServerLevel level){
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,victim.getX(),victim.getY(0.6),victim.getZ(),14,0.2,0.4,0.2,0.15);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT,victim.getX(),victim.getY(0.6),victim.getZ(),20,0.4,0.5,0.4,0.4);
            level.playSound(null,victim.getX(),victim.getY(),victim.getZ(),net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_BREAK,net.minecraft.sounds.SoundSource.PLAYERS,1.2F,1.6F);
        }
    }
    /** Untouched by Any Law: a killing blow is refused, once in a while; the wearer is left at 30% health and shielded for three seconds */
    public static final String WARD_KEY="primordial:ward_ready_at";
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void charmWard(net.minecraftforge.event.entity.living.LivingDeathEvent event) {
        if(!(event.getEntity() instanceof Player wearer)||wearer.level().isClientSide||event.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY))return;
        int cooldown=AlchemyCharmItem.ward(wearer);if(cooldown<=0)return;
        long now=wearer.level().getGameTime();var data=wearer.getPersistentData();
        if(now<data.getLong(WARD_KEY))return;
        data.putLong(WARD_KEY,now+cooldown);
        event.setCanceled(true);
        ((local.primordial.mixin.HealthAccessor)wearer).angelSetDead(false);wearer.deathTime=0;
        wearer.setHealth(wearer.getMaxHealth()*0.3F);
        wearer.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE,60,4,false,true));
        wearer.invulnerableTime=60;
        if(wearer.level() instanceof net.minecraft.server.level.ServerLevel level){
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.TOTEM_OF_UNDYING,wearer.getX(),wearer.getY(0.6),wearer.getZ(),40,0.5,0.8,0.5,0.4);
            level.playSound(null,wearer.getX(),wearer.getY(),wearer.getZ(),net.minecraft.sounds.SoundEvents.BEACON_ACTIVATE,net.minecraft.sounds.SoundSource.PLAYERS,1.5F,1.6F);
        }
        wearer.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.primordial.charm.ward",cooldown/20).withStyle(net.minecraft.ChatFormatting.AQUA),true);
    }
    /** defensive charms: the wearer takes less physical or non-physical damage */
    @SubscribeEvent public static void charmDamageReduction(LivingHurtEvent event) {
        var wearer=event.getEntity();
        if(wearer.level().isClientSide||!(wearer instanceof Player)||!local.primordial.affix.AffixDamage.mitigable(event.getSource()))return;
        double reduction=Math.max(AlchemyCharmItem.allReduction(wearer),
            AffixCombat.physical(event.getSource())?AlchemyCharmItem.physicalReduction(wearer):AlchemyCharmItem.nonPhysicalReduction(wearer));
        if(reduction>0)event.setAmount((float)(event.getAmount()*(1-reduction)));
    }
}
