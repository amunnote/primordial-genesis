package local.primordial.effect;

import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import local.primordial.Primordial;
import local.primordial.affix.AffixState;
import local.primordial.alchemy.AlchemyKnowledge;
import local.primordial.item.WorldCoreItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * The three world cores: alchemical accessories made on a truth altar from the soul its realm's boss drops when it bore
 * seven or more affixes (the Warden for the Overworld, the Wither for the Nether, the Ender Dragon for the End). Killing
 * one also brings the epiphany of its core's recipe, which is found nowhere else.
 */
@Mod.EventBusSubscriber(modid=Primordial.ID)
public final class WorldCores {
    public enum Realm {
        OVERWORLD("overworld",Level.OVERWORLD,0x7FD35A),
        NETHER("nether",Level.NETHER,0xFF7A30),
        END("end",Level.END,0xC58BFF);
        public final String id;public final ResourceKey<Level> dimension;public final int color;
        Realm(String id,ResourceKey<Level> dimension,int color) {this.id=id;this.dimension=dimension;this.color=color;}
        public String soulId() {return id+"_soul";}
        public String coreId() {return id+"_core";}
        public ResourceLocation recipe() {return new ResourceLocation(Primordial.ID,coreId());}
        public EntityType<?> boss() {return switch(this){case OVERWORLD->EntityType.WARDEN;case NETHER->EntityType.WITHER;case END->EntityType.ENDER_DRAGON;};}
        public static Realm of(EntityType<?> type) {for(var realm:values())if(realm.boss()==type)return realm;return null;}
        /** is the wearer standing in this core's own world? */
        public boolean home(LivingEntity entity) {return entity.level().dimension()==dimension;}
    }
    public static final Map<Realm,RegistryObject<Item>> SOULS=new EnumMap<>(Realm.class),CORES=new EnumMap<>(Realm.class);
    /** the World Core, made from the three souls and the three realm cores; its recipe is carried by a Sin Lord */
    public static RegistryObject<Item> WORLD_CORE;
    public static final ResourceLocation WORLD_RECIPE=new ResourceLocation(Primordial.ID,"world_core");
    /** affixes (its boss affix counted) the realm's boss must bear to drop its soul */
    public static final int AFFIXES_REQUIRED=7;
    /** luck needed to wear a core */
    public static final double LUCK_REQUIRED=50,WORLD_LUCK_REQUIRED=80;
    /** the Nether core: extra damage to burning targets, doubled in the Nether */
    public static final float BURNING_BONUS=0.25F;
    /** the End core: chance to slip out of a hit (doubled in the End), and the ticks before it can again */
    public static final double DODGE_CHANCE=0.15;
    public static final int DODGE_COOLDOWN=60;
    static final String DODGE_KEY="primordial:end_core_dodge";
    private WorldCores() {}

    public static void register(DeferredRegister<Item> items) {
        for(var realm:Realm.values()){
            SOULS.put(realm,items.register(realm.soulId(),()->new Soul(realm)));
            CORES.put(realm,items.register(realm.coreId(),()->new WorldCoreItem(realm)));
            AlchemyKnowledge.registerEpiphanyOnly(realm.recipe());
        }
        WORLD_CORE=items.register("world_core",()->new WorldCoreItem(null));
        AlchemyKnowledge.registerBossOnly(WORLD_RECIPE);
    }

    /** a realm's boss bearing seven affixes, felled by a player: its soul, and (for a sage) the epiphany of its core */
    public static void drop(AffixState state,int affixes,Player killer,Collection<ItemEntity> drops) {
        var realm=Realm.of(state.mob.getType());
        if(realm==null||affixes<AFFIXES_REQUIRED)return;
        var mob=state.mob;
        var drop=new ItemEntity(mob.level(),mob.getX(),mob.getY(),mob.getZ(),new ItemStack(SOULS.get(realm).get()));
        drop.setDefaultPickUpDelay();drop.setGlowingTag(true);drops.add(drop);
        AlchemyKnowledge.epiphany(killer,realm.recipe());
    }

    /** wears this realm's core, or the World Core (null: the World Core only) */
    public static boolean wears(LivingEntity entity,Realm realm) {
        if(!(entity instanceof Player player))return false;
        return CuriosApi.getCuriosInventory(player).map(inventory->{
            for(var found:inventory.findCurios(stack->stack.getItem() instanceof WorldCoreItem core&&(core.world()||core.realm()==realm)))
                if(!found.slotContext().cosmetic())return true;
            return false;
        }).orElse(false);
    }
    /** a realm core's home bonus: in its own world, or anywhere for the World Core's wearer */
    public static boolean atHome(LivingEntity entity,Realm realm) {return realm.home(entity)||wears(entity,null);}

    /** the Nether core: burning foes take more, and a melee hit sets them alight */
    @SubscribeEvent public static void hurt(LivingHurtEvent event) {
        if(!(event.getSource().getEntity() instanceof Player player)||!wears(player,Realm.NETHER))return;
        var target=event.getEntity();
        if(target.isOnFire())event.setAmount(event.getAmount()*(1+BURNING_BONUS*(atHome(player,Realm.NETHER)?2:1)));
        if(event.getSource().getDirectEntity()==player)target.setSecondsOnFire(5);
    }
    /** the End core: no fall damage, and now and then a hit finds only an afterimage */
    @SubscribeEvent public static void attacked(LivingAttackEvent event) {
        if(!(event.getEntity() instanceof Player player)||player.level().isClientSide||!wears(player,Realm.END))return;
        var source=event.getSource();
        if(source.is(DamageTypeTags.IS_FALL)){event.setCanceled(true);return;}
    }

    /** slip up to 8 blocks away, as a chorus fruit would */
    static void blink(Player player) {
        if(!(player.level() instanceof ServerLevel level))return;
        double x=player.getX(),y=player.getY(),z=player.getZ();
        level.sendParticles(ParticleTypes.PORTAL,x,y+1,z,40,0.4,0.8,0.4,0.6);
        for(int i=0;i<16;i++){
            double tx=x+(player.getRandom().nextDouble()-0.5)*16,tz=z+(player.getRandom().nextDouble()-0.5)*16;
            double ty=Math.max(level.getMinBuildHeight(),Math.min(level.getMinBuildHeight()+level.getLogicalHeight()-1,y+player.getRandom().nextInt(9)-4));
            if(player.isPassenger())player.stopRiding();
            if(player.randomTeleport(tx,ty,tz,true))break;
        }
        player.resetFallDistance();
        level.playSound(null,x,y,z,SoundEvents.CHORUS_FRUIT_TELEPORT,SoundSource.PLAYERS,1F,1.2F);
        level.playSound(null,player.blockPosition(),SoundEvents.ENDERMAN_TELEPORT,SoundSource.PLAYERS,0.8F,1.4F);
    }

    /** a realm's soul: the alchemical heart of a core */
    public static final class Soul extends Item {
        private final Realm realm;
        Soul(Realm realm) {super(new Properties().rarity(Rarity.EPIC).fireResistant());this.realm=realm;}
        @Override public boolean isFoil(ItemStack stack) {return true;}
        @Override public void appendHoverText(ItemStack stack,Level level,List<Component> lines,TooltipFlag flag) {
            lines.add(Component.translatable("tooltip.primordial."+realm.soulId()).withStyle(style->style.withColor(realm.color).withItalic(true)));
            lines.add(Component.translatable("tooltip.primordial.world_soul.source",realm.boss().getDescription(),AFFIXES_REQUIRED).withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
