package local.primordial.spirit;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import local.primordial.Primordial;
import local.primordial.alchemy.AlchemyKnowledge;
import local.primordial.item.AlchemyRecipeItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.common.crafting.StrictNBTIngredient;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * The Spirit Realm (灵界). Drinking an Inspiration potion gives Inspiration (灵感) for 3 minutes: affix materials drop
 * twice as often, and right-clicking a bed takes you into the realm instead of to sleep (Inspiration is spent on the way
 * in). There every creature is a spirit: tougher, with richer loot, more affix materials and more recipes; spirits of
 * any world's creatures appear anywhere, whatever the island. Drinking Inspiration again takes you home; falling off
 * the islands or dying there also does, with a minute of Spirit Shatter (灵体破碎, -20% to every attribute).
 * <p>
 * The first time in, a traveller arrives on the arrival island. Leaving by choice, the realm remembers where they stood
 * and takes them back there next time; thrown out any other way (a death, a fall), they come back on a random island up
 * to {@link #SCATTER_RADIUS} blocks from where they were thrown out.
 */
@Mod.EventBusSubscriber(modid=Primordial.ID)
public final class SpiritRealm {
    public static final ResourceKey<Level> LEVEL=ResourceKey.create(Registries.DIMENSION,new ResourceLocation(Primordial.ID,"spirit_realm"));
    public static final String RETURN="primordial:spirit_return",GO_HOME="primordial:spirit_go_home",SPIRIT="primordial:spirit";
    /** on the player: where they stood when they last left by choice, and whether they were thrown out instead */
    public static final String LEFT_AT="primordial:spirit_left_at",SCATTERED="primordial:spirit_scattered",SCATTERED_FROM="primordial:spirit_scattered_from";
    /** how far from where they were thrown out a scattered traveller may come back, in blocks */
    public static final int SCATTER_RADIUS=10000;
    public static final int INSPIRATION_TICKS=3600,SHATTER_TICKS=1200;

    static final DeferredRegister<MobEffect> EFFECTS=DeferredRegister.create(ForgeRegistries.MOB_EFFECTS,Primordial.ID);
    static final DeferredRegister<Potion> POTIONS=DeferredRegister.create(ForgeRegistries.POTIONS,Primordial.ID);
    static final DeferredRegister<com.mojang.serialization.Codec<? extends ChunkGenerator>> GENERATORS=DeferredRegister.create(Registries.CHUNK_GENERATOR,Primordial.ID);
    static final DeferredRegister<com.mojang.serialization.Codec<? extends BiomeSource>> BIOME_SOURCES=DeferredRegister.create(Registries.BIOME_SOURCE,Primordial.ID);
    static final DeferredRegister<com.mojang.serialization.Codec<? extends net.minecraftforge.common.loot.IGlobalLootModifier>> LOOT=
        DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS,Primordial.ID);
    static final DeferredRegister<net.minecraft.core.particles.ParticleType<?>> PARTICLES=DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES,Primordial.ID);
    /** motes of light drifting through the realm's air, and the wisps that rise off spirits */
    public static final RegistryObject<net.minecraft.core.particles.SimpleParticleType> SPIRIT_MOTE=PARTICLES.register("spirit_mote",()->new net.minecraft.core.particles.SimpleParticleType(false));
    public static final RegistryObject<net.minecraft.core.particles.SimpleParticleType> SPIRIT_WISP=PARTICLES.register("spirit_wisp",()->new net.minecraft.core.particles.SimpleParticleType(false));
    public static final RegistryObject<MobEffect> INSPIRATION=EFFECTS.register("inspiration",InspirationEffect::new);
    public static final RegistryObject<MobEffect> SPIRIT_SHATTER=EFFECTS.register("spirit_shatter",SpiritShatterEffect::new);
    public static final RegistryObject<Potion> INSPIRATION_POTION=POTIONS.register("inspiration",
        ()->new Potion("inspiration",new MobEffectInstance(INSPIRATION.get(),INSPIRATION_TICKS)));
    static {
        GENERATORS.register("spirit_islands",()->SpiritChunkGenerator.CODEC);
        BIOME_SOURCES.register("spirit_islands",()->SpiritBiomeSource.CODEC);
        LOOT.register("spirit_chest_recipe",SpiritLootModifier.CODEC);
    }
    private SpiritRealm() {}
    public static void register(IEventBus bus) {
        EFFECTS.register(bus);POTIONS.register(bus);GENERATORS.register(bus);BIOME_SOURCES.register(bus);LOOT.register(bus);PARTICLES.register(bus);
        bus.addListener(SpiritRealm::brewing);
    }
    /** awkward potion + alchemical essence = Inspiration */
    static void brewing(FMLCommonSetupEvent event) {
        event.enqueueWork(()->BrewingRecipeRegistry.addRecipe(
            StrictNBTIngredient.of(PotionUtils.setPotion(new ItemStack(Items.POTION),Potions.AWKWARD)),
            Ingredient.of(Primordial.ALCHEMICAL_ESSENCE.get()),
            PotionUtils.setPotion(new ItemStack(Items.POTION),INSPIRATION_POTION.get())));
    }

    public static boolean inRealm(Entity entity) {return entity.level().dimension()==LEVEL;}
    public static boolean inspired(LivingEntity entity) {return entity!=null&&INSPIRATION.isPresent()&&entity.hasEffect(INSPIRATION.get());}
    /** how much more often a killer gets affix materials: ×2 inspired, ×2 in the realm */
    public static double materialMultiplier(Player killer,LivingEntity victim) {
        double multiplier=1;
        if(inspired(killer))multiplier*=2;
        if(victim!=null&&inRealm(victim))multiplier*=2;
        return multiplier;
    }

    // ------------------------------------------------------------------ going in and coming back
    @SubscribeEvent(priority=EventPriority.HIGH) public static void spiritBed(PlayerInteractEvent.RightClickBlock event) {
        if(!(event.getEntity() instanceof ServerPlayer player)||!event.getLevel().getBlockState(event.getPos()).is(BlockTags.BEDS))return;
        if(inRealm(player)){
            event.setCanceled(true);event.setCancellationResult(InteractionResult.SUCCESS);
            player.displayClientMessage(Component.translatable("message.primordial.spirit.no_sleep").withStyle(ChatFormatting.LIGHT_PURPLE),true);
        }else if(inspired(player)&&!player.isShiftKeyDown()){
            // inspired, a bed leads into the realm; sneaking, it is only a bed (a full moon's night can still be slept through)
            event.setCanceled(true);event.setCancellationResult(InteractionResult.SUCCESS);
            enter(player);
        }
    }
    public static boolean enter(ServerPlayer player) {
        var realm=player.server.getLevel(LEVEL);
        if(realm==null)return false;
        var back=new CompoundTag();
        back.putString("dimension",player.level().dimension().location().toString());
        back.putDouble("x",player.getX());back.putDouble("y",player.getY());back.putDouble("z",player.getZ());back.putFloat("yaw",player.getYRot());
        player.getPersistentData().put(RETURN,back);
        var at=entryPoint(realm,player,player.getRandom());
        double x=at.x-0.5,y=at.y,z=at.z-0.5;
        var kept=kept(player);
        float yaw=kept.contains(LEFT_AT)&&!kept.getBoolean(SCATTERED)?kept.getCompound(LEFT_AT).getFloat("yaw"):player.getYRot();
        kept.remove(LEFT_AT);kept.remove(SCATTERED);kept.remove(SCATTERED_FROM);keep(player,kept);
        player.removeEffect(INSPIRATION.get());
        player.fallDistance=0;
        player.teleportTo(realm,x+0.5,y,z+0.5,yaw,player.getXRot());
        local.primordial.achievement.Achievements.award(player,local.primordial.achievement.Achievements.SPIRIT_REALM);
        realm.sendParticles(ParticleTypes.SOUL,x+0.5,y+1,z+0.5,40,0.5,1,0.5,0.05);
        realm.sendParticles(SPIRIT_MOTE.get(),x+0.5,y+1,z+0.5,60,1.2,1.2,1.2,0.02);
        realm.playSound(null,x,y,z,SoundEvents.ENDER_EYE_DEATH,SoundSource.PLAYERS,1F,0.6F);
        player.displayClientMessage(Component.translatable("message.primordial.spirit.enter").withStyle(ChatFormatting.LIGHT_PURPLE),false);
        return true;
    }
    /**
     * Where a traveller comes into the realm: somewhere at random after being thrown out; where they stood when they
     * last left by choice; otherwise (the first time) the arrival island.
     */
    public static net.minecraft.world.phys.Vec3 entryPoint(ServerLevel realm,Player player,net.minecraft.util.RandomSource random) {
        var data=kept(player);
        if(data.getBoolean(SCATTERED)){var from=data.getCompound(SCATTERED_FROM);
            var landing=randomLanding(realm,random,from.getDouble("x"),from.getDouble("z"));if(landing!=null)return landing;}
        else if(data.contains(LEFT_AT)){var left=data.getCompound(LEFT_AT);return new net.minecraft.world.phys.Vec3(left.getDouble("x"),left.getDouble("y"),left.getDouble("z"));}
        return arrivalPoint(realm);
    }
    /** a spot on a random island up to {@link #SCATTER_RADIUS} blocks from (x, z), standing on it; null if none was found */
    public static net.minecraft.world.phys.Vec3 randomLanding(ServerLevel realm,net.minecraft.util.RandomSource random,double fromX,double fromZ) {
        for(int tries=0;tries<40;tries++){
            double pa=random.nextDouble()*Math.PI*2,pr=Math.sqrt(random.nextDouble())*SCATTER_RADIUS;
            int cx=SpiritIslands.cellOf((int)Math.floor(fromX+Math.cos(pa)*pr)),cz=SpiritIslands.cellOf((int)Math.floor(fromZ+Math.sin(pa)*pr));
            var island=SpiritIslands.island(cx,cz);if(island==null)continue;
            double a=random.nextDouble()*Math.PI*2,r=random.nextDouble()*island.radius()*0.5;
            int x=(int)Math.floor(island.centerX()+Math.cos(a)*r),z=(int)Math.floor(island.centerZ()+Math.sin(a)*r);
            if(island.column(x,z)==null)continue;
            realm.getChunk(x>>4,z>>4);
            int y=realm.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z);
            // standing on the island itself, not on something floating over it (a terrace, the temple, the court)
            if(y<=realm.getMinBuildHeight()||y>island.column(x,z)[1]+12)continue;
            var feet=new BlockPos(x,y,z);
            if(!realm.getFluidState(feet.below()).isEmpty()||!realm.getBlockState(feet).getCollisionShape(realm,feet).isEmpty())continue;
            return new net.minecraft.world.phys.Vec3(x+0.5,y,z+0.5);
        }
        return null;
    }
    /** on leaving: by choice the realm keeps the spot for next time; thrown out, the next arrival is somewhere at random */
    public static void rememberLeaving(Player player,boolean shattered) {
        var data=kept(player);
        if(shattered){
            data.remove(LEFT_AT);data.putBoolean(SCATTERED,true);
            var from=new CompoundTag();from.putDouble("x",player.getX());from.putDouble("z",player.getZ());data.put(SCATTERED_FROM,from);
        }else{
            var left=new CompoundTag();
            left.putDouble("x",player.getX());left.putDouble("y",player.getY());left.putDouble("z",player.getZ());left.putFloat("yaw",player.getYRot());
            data.put(LEFT_AT,left);data.remove(SCATTERED);data.remove(SCATTERED_FROM);
        }
        keep(player,data);
    }
    /** where the realm keeps what it remembers of a player: carried across death and the End's exit like the rest */
    public static CompoundTag kept(Player player) {return player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);}
    static void keep(Player player,CompoundTag kept) {player.getPersistentData().put(Player.PERSISTED_NBT_TAG,kept);}
    /** where a traveller arrives: the middle of the arrival island, standing on it */
    public static net.minecraft.world.phys.Vec3 arrivalPoint(ServerLevel realm) {
        var island=SpiritIslands.arrival();
        int x=(int)Math.floor(island.centerX()),z=(int)Math.floor(island.centerZ());
        realm.getChunk(x>>4,z>>4);
        int y=realm.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z);
        if(y<=realm.getMinBuildHeight())y=island.top()+1;
        return new net.minecraft.world.phys.Vec3(x+0.5,y,z+0.5);
    }
    /** back to where the player came from; shattered when thrown out (a fall, a death) rather than leaving by choice */
    public static void returnHome(ServerPlayer player,boolean shattered) {
        var data=player.getPersistentData();var back=data.getCompound(RETURN);
        ServerLevel target=null;
        if(back.contains("dimension")){var key=ResourceLocation.tryParse(back.getString("dimension"));
            if(key!=null)target=player.server.getLevel(ResourceKey.create(Registries.DIMENSION,key));}
        double x,y,z;float yaw=back.getFloat("yaw");
        if(target==null||target.dimension()==LEVEL){
            target=player.server.overworld();var spawn=target.getSharedSpawnPos();
            x=spawn.getX()+0.5;y=target.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,spawn.getX(),spawn.getZ());z=spawn.getZ()+0.5;
        }else{x=back.getDouble("x");y=back.getDouble("y");z=back.getDouble("z");}
        data.remove(RETURN);data.remove(GO_HOME);
        if(inRealm(player))rememberLeaving(player,shattered);
        player.fallDistance=0;player.setDeltaMovement(0,0,0);
        player.teleportTo(target,x,y,z,yaw,player.getXRot());
        if(shattered)player.addEffect(new MobEffectInstance(SPIRIT_SHATTER.get(),SHATTER_TICKS));
        target.playSound(null,x,y,z,SoundEvents.SOUL_ESCAPE,SoundSource.PLAYERS,1F,1F);
        player.displayClientMessage(Component.translatable(shattered?"message.primordial.spirit.shattered":"message.primordial.spirit.return")
            .withStyle(shattered?ChatFormatting.RED:ChatFormatting.LIGHT_PURPLE),false);
    }
    /** drinking Inspiration inside the realm sends the drinker home (on the next tick, once the potion is done with) */
    @SubscribeEvent public static void spiritInspiredAgain(MobEffectEvent.Added event) {
        if(event.getEntity() instanceof ServerPlayer player&&inRealm(player)&&event.getEffectInstance().getEffect()==INSPIRATION.get())
            player.getPersistentData().putBoolean(GO_HOME,true);
    }
    @SubscribeEvent public static void spiritPlayerTick(TickEvent.PlayerTickEvent event) {
        if(event.phase!=TickEvent.Phase.END||!(event.player instanceof ServerPlayer player)||!inRealm(player))return;
        if(player.getPersistentData().getBoolean(GO_HOME)){returnHome(player,false);return;}
        if(player.getY()<player.level().getMinBuildHeight()-8&&!player.isCreative()&&!player.isSpectator()){returnHome(player,true);return;}
        if(player.tickCount%40==0)SpiritSpawner.spawnAround(player);
    }
    /** dying in the realm is not death: the spirit breaks and the player wakes at home, keeping everything */
    @SubscribeEvent(priority=EventPriority.HIGH) public static void spiritDeath(LivingDeathEvent event) {
        if(!(event.getEntity() instanceof ServerPlayer player)||!inRealm(player))return;
        event.setCanceled(true);
        ((local.primordial.mixin.HealthAccessor)player).angelSetDead(false);
        player.deathTime=0;player.setHealth(player.getMaxHealth()*0.5F);player.clearFire();
        for(var effect:new ArrayList<>(player.getActiveEffects()))
            if(effect.getEffect().getCategory()==MobEffectCategory.HARMFUL)player.removeEffect(effect.getEffect());
        returnHome(player,true);
    }

    // ------------------------------------------------------------------ spirits
    private static final UUID HEALTH=UUID.fromString("9c4e1f62-7a3b-4d58-8e21-5f6a7b8c9d01"),ATTACK=UUID.fromString("9c4e1f62-7a3b-4d58-8e21-5f6a7b8c9d02");
    private static final UUID ARMOR=UUID.fromString("9c4e1f62-7a3b-4d58-8e21-5f6a7b8c9d03"),SPEED=UUID.fromString("9c4e1f62-7a3b-4d58-8e21-5f6a7b8c9d04");
    /** every creature in the realm is a spirit: x1.5 health and attack, +4 armor, +10% speed */
    @SubscribeEvent(priority=EventPriority.LOW) public static void spiritJoin(EntityJoinLevelEvent event) {
        if(event.getLevel().isClientSide||event.getLevel().dimension()!=LEVEL||!(event.getEntity() instanceof LivingEntity living)||living instanceof Player
            ||living instanceof local.primordial.boss.SinEntity)return;
        if(living.getPersistentData().getBoolean(SPIRIT))return;
        living.getPersistentData().putBoolean(SPIRIT,true);
        modify(living,Attributes.MAX_HEALTH,HEALTH,0.5,AttributeModifier.Operation.MULTIPLY_TOTAL);
        modify(living,Attributes.ATTACK_DAMAGE,ATTACK,0.5,AttributeModifier.Operation.MULTIPLY_TOTAL);
        modify(living,Attributes.ARMOR,ARMOR,4,AttributeModifier.Operation.ADDITION);
        modify(living,Attributes.MOVEMENT_SPEED,SPEED,0.1,AttributeModifier.Operation.MULTIPLY_TOTAL);
        living.setHealth(living.getMaxHealth());
    }
    private static void modify(LivingEntity living,net.minecraft.world.entity.ai.attributes.Attribute attribute,UUID id,double amount,AttributeModifier.Operation operation) {
        var instance=living.getAttribute(attribute);
        if(instance!=null&&instance.getModifier(id)==null)instance.addPermanentModifier(new AttributeModifier(id,"primordial.spirit",amount,operation));
    }
    /** the realm's own spawner brings the spirits; its islands' biomes do not */
    @SubscribeEvent public static void spiritNoBiomeSpawns(MobSpawnEvent.FinalizeSpawn event) {
        if(event.getLevel().getLevel().dimension()==LEVEL&&(event.getSpawnType()==MobSpawnType.NATURAL||event.getSpawnType()==MobSpawnType.CHUNK_GENERATION))
            event.setSpawnCancelled(true);
    }
    /** richer spoils: twice the drops, and a chance at a recipe from any affixed spirit */
    @SubscribeEvent(priority=EventPriority.LOW) public static void spiritDrops(LivingDropsEvent event) {
        var dead=event.getEntity();
        if(dead.level().isClientSide||!inRealm(dead)||dead instanceof Player||dead instanceof local.primordial.boss.SinEntity)return;
        var extra=new ArrayList<ItemEntity>();
        for(var drop:event.getDrops()){
            var stack=drop.getItem();
            if(stack.getItem() instanceof AlchemyRecipeItem)continue;
            var copy=stack.copy();
            if(copy.getCount()*2<=copy.getMaxStackSize()){stack.setCount(stack.getCount()*2);drop.setItem(stack);}
            else extra.add(new ItemEntity(dead.level(),drop.getX(),drop.getY(),drop.getZ(),copy));
        }
        event.getDrops().addAll(extra);
        if(dead instanceof Mob mob&&event.isRecentlyHit()&&event.getSource().getEntity() instanceof Player
            &&local.primordial.affix.AffixState.get(mob)!=null&&dead.getRandom().nextDouble()<RECIPE_CHANCE){
            var recipe=realmRecipe(dead.getServer(),dead.getRandom());
            if(recipe!=null){var item=new ItemEntity(dead.level(),dead.getX(),dead.getY(),dead.getZ(),AlchemyRecipeItem.create(recipe));item.setDefaultPickUpDelay();event.getDrops().add(item);}
        }
    }
    /** an affixed spirit's chance of dropping a recipe */
    public static final double RECIPE_CHANCE=0.04;
    @SubscribeEvent public static void spiritExperience(LivingExperienceDropEvent event) {
        if(inRealm(event.getEntity())&&!(event.getEntity() instanceof Player)&&!(event.getEntity() instanceof local.primordial.boss.SinEntity))event.setDroppedExperience(event.getDroppedExperience()*2);
    }
    /** any alchemy recipe that can be found at random: the realm gives them whether or not they are known already */
    public static ResourceLocation realmRecipe(net.minecraft.server.MinecraftServer server,net.minecraft.util.RandomSource random) {
        if(server==null)return null;
        List<ResourceLocation> pool=AlchemyKnowledge.recipes().stream().filter(AlchemyKnowledge::findable).toList();
        return pool.isEmpty()?null:pool.get(random.nextInt(pool.size()));
    }
    /** a spirit: any creature but a player, in the realm */
    public static boolean isSpirit(Entity entity) {return entity instanceof LivingEntity&&!(entity instanceof Player)&&inRealm(entity);}
}
