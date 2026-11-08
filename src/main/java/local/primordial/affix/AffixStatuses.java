package local.primordial.affix;

import java.util.UUID;
import local.primordial.Primordial;
import local.primordial.network.MenuDropNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Independent combat statuses: these are deliberately not MobEffects, so milk and
 * effect-clearing accessories cannot remove them, and Sage's Gift does not rescale them.
 * Times are saved on the entity. The bonds that hold for a player ({@link AffixBonds}) and the Growth accessory's
 * stacks and countdown are kept and synced here too. */
@Mod.EventBusSubscriber(modid=Primordial.ID)
public final class AffixStatuses {
    private static final String MALICE="primordial:malice_ticks";
    private static final String DESATURATION="primordial:desaturation_ticks",DESATURATION_STACKS="primordial:desaturation_stacks";
    private static final String FEEBLE="primordial:feeble_ticks";
    private static final String BLOOD_PACT="primordial:blood_pact_ticks";
    private static final String BEFUDDLED="primordial:befuddled_ticks",BEFUDDLE_SEED="primordial:befuddle_seed";
    /** mob-only statuses, kept up by a nearby bearer */
    private static final String UNDYING="primordial:undying_ticks",UNDYING_USED="primordial:undying_used",ENLIGHTENED="primordial:enlightened_ticks";
    /** the Growth accessory's two tracks and the last hit given or taken; the bonds holding (a mask of AffixBonds.Bond bits) */
    private static final String GROWTH_ATTACK="primordial:growth_attack_stacks",GROWTH_HEALTH="primordial:growth_health_stacks",GROWTH_LAST="primordial:growth_last";
    private static final String BONDS="primordial:bonds";
    public static final int MALICE_TICKS=1200,DESATURATION_TICKS=1200,FEEBLE_TICKS=100,BLOOD_PACT_TICKS=1200,BEFUDDLED_TICKS=300,AURA_TICKS=100;
    /** on other creatures: malice from Malice's accessory (5 s), 失色 stacks from 褪色世界, 降智 from 天缚 */
    public static final int MOB_MALICE_TICKS=100,MOB_DESATURATION_TICKS=200,DESATURATION_STACK_MAX=5,MOB_BEFUDDLED_TICKS=60;
    /** Growth: five stacks a track at most; all gone after five seconds without a hit given or taken */
    public static final int GROWTH_MAX=5,GROWTH_IDLE=100;
    /** Feebleness: -20% attack speed and -20% damage dealt */
    public static final double FEEBLE_PENALTY=0.2;
    private static final UUID FEEBLE_SPEED=UUID.fromString("6f1a4c2e-5b7d-4e3a-9c1f-2d8e7a6b5c40");
    private static final String[] PLAYER_KEYS={MALICE,DESATURATION,FEEBLE,BLOOD_PACT,BEFUDDLED};
    private static int clientMalice,clientDesaturation,clientFeeble,clientBloodPact,clientBefuddled,clientBefuddleSeed;
    private static int clientGrowthAttack,clientGrowthHealth,clientGrowthLeft,clientBonds,clientUnquenchedLeft,clientWheelLeft;
    private AffixStatuses() {}
    public static int malice(Player player){return player.level().isClientSide?clientMalice:player.getPersistentData().getInt(MALICE);}
    /** a creature's malice; on the client only the local player's is known */
    public static int malice(LivingEntity entity){
        if(entity instanceof Player player)return malice(player);
        return entity.level().isClientSide?0:entity.getPersistentData().getInt(MALICE);
    }
    public static int desaturation(Player player){return player.level().isClientSide?clientDesaturation:player.getPersistentData().getInt(DESATURATION);}
    public static int bloodPact(Player player){return player.level().isClientSide?clientBloodPact:player.getPersistentData().getInt(BLOOD_PACT);}
    public static int befuddled(Player player){return player.level().isClientSide?clientBefuddled:player.getPersistentData().getInt(BEFUDDLED);}
    /** picks the shuffled controls; fixed for one bout of befuddlement */
    public static int befuddleSeed(Player player){return player.level().isClientSide?clientBefuddleSeed:player.getPersistentData().getInt(BEFUDDLE_SEED);}
    /** Befuddlement's controls: forward, back, left, right (0-3) each sent to a different one of the four */
    public static int[] shuffledControls(int seed) {
        var order=new java.util.ArrayList<>(java.util.List.of(0,1,2,3));var random=new java.util.Random(seed);
        while(true){
            java.util.Collections.shuffle(order,random);
            boolean moved=true;for(int i=0;i<4;i++)if(order.get(i)==i)moved=false;
            if(moved)break;
        }
        int[] to=new int[4];for(int i=0;i<4;i++)to[i]=order.get(i);return to;
    }
    /** any living entity can be feeble; on the client only the local player's time is known */
    public static int feeble(LivingEntity entity){
        if(entity.level().isClientSide)return entity instanceof Player?clientFeeble:0;
        return entity.getPersistentData().getInt(FEEBLE);
    }
    public static void applyMalice(Player player){apply(player,MALICE,MALICE_TICKS);}
    /**
     * Malice's accessory: a player struck by it gets the players' malice (monsters lock on to them) for this long; any other
     * creature gets the creatures' malice, the mirror of it: the monsters around turn on the marked one first.
     */
    public static void applyMalice(LivingEntity target,int ticks){apply(target,MALICE,ticks);}
    public static void applyDesaturation(Player player){apply(player,DESATURATION,DESATURATION_TICKS);}
    public static void applyFeeble(LivingEntity entity){apply(entity,FEEBLE,FEEBLE_TICKS);updateFeebleSpeed(entity);}
    public static void applyBloodPact(Player player){apply(player,BLOOD_PACT,BLOOD_PACT_TICKS);}
    public static void applyBefuddled(Player player){
        if(player.level().isClientSide)return;
        // a new bout shuffles the controls anew; renewing a running one keeps them
        if(player.getPersistentData().getInt(BEFUDDLED)<=0)player.getPersistentData().putInt(BEFUDDLE_SEED,player.getRandom().nextInt());
        apply(player,BEFUDDLED,BEFUDDLED_TICKS);
    }
    /** the negative statuses on an entity: every one of them, gone (the Purification Crystal) */
    public static void clearNegative(LivingEntity entity){
        if(entity.level().isClientSide)return;
        var data=entity.getPersistentData();boolean changed=false;
        for(String key:new String[]{MALICE,DESATURATION,DESATURATION_STACKS,FEEBLE,BLOOD_PACT,BEFUDDLED})if(data.getInt(key)>0){data.remove(key);changed=true;}
        if(!changed)return;
        updateFeebleSpeed(entity);
        if(entity instanceof ServerPlayer server)sync(server);
    }
    /** which status a flash shows: the order of PLAYER_KEYS (malice, desaturation, feeble, blood pact, befuddled) */
    public static final int FLASH_MALICE=0,FLASH_DESATURATION=1,FLASH_FEEBLE=2,FLASH_BLOOD_PACT=3,FLASH_BEFUDDLED=4;
    private static void apply(LivingEntity entity,String key,int ticks){
        if(entity.level().isClientSide)return;
        // the Purification Crystal: no negative status takes hold of its wearer
        if(local.primordial.effect.HarmfulEffectProtection.crystalActive(entity))return;
        // every one of these harms: on a player Time's accessory and the bonds shorten it (AffixBonds.negativeFactor)
        if(entity instanceof Player player)ticks=AffixBonds.negativeTicks(player,ticks);
        boolean fresh=entity.getPersistentData().getInt(key)<=0;
        entity.getPersistentData().putInt(key,Math.max(entity.getPersistentData().getInt(key),ticks));
        if(entity instanceof ServerPlayer server){
            sync(server);
            // a new status flashes across the screen; renewing one that is still running stays quiet
            if(fresh)MenuDropNetwork.statusFlash(server,java.util.Arrays.asList(PLAYER_KEYS).indexOf(key));
        }
    }

    // ---- mob statuses ----
    public static int undying(LivingEntity mob){return mob.getPersistentData().getInt(UNDYING);}
    /** each mob is saved by Undying once */
    public static boolean undyingUsed(LivingEntity mob){return mob.getPersistentData().getBoolean(UNDYING_USED);}
    public static void applyUndying(LivingEntity mob){if(!undyingUsed(mob))mob.getPersistentData().putInt(UNDYING,AURA_TICKS);}
    public static void useUndying(LivingEntity mob){mob.getPersistentData().remove(UNDYING);mob.getPersistentData().putBoolean(UNDYING_USED,true);}
    public static int enlightened(LivingEntity mob){return mob.getPersistentData().getInt(ENLIGHTENED);}
    public static void applyEnlightened(LivingEntity mob){mob.getPersistentData().putInt(ENLIGHTENED,AURA_TICKS);}
    /** 失色 on a creature other than a player (褪色世界 lays it a stack at a time): a mark, the grey ash its only sign */
    public static boolean desaturated(LivingEntity mob){return !(mob instanceof Player)&&!mob.level().isClientSide&&mob.getPersistentData().getInt(DESATURATION)>0;}
    public static int desaturationStacks(LivingEntity mob){return desaturated(mob)?mob.getPersistentData().getInt(DESATURATION_STACKS):0;}
    public static void addDesaturationStack(LivingEntity mob){
        if(mob instanceof Player||mob.level().isClientSide)return;
        var data=mob.getPersistentData();
        data.putInt(DESATURATION_STACKS,Math.min(DESATURATION_STACK_MAX,desaturationStacks(mob)+1));
        data.putInt(DESATURATION,MOB_DESATURATION_TICKS);
    }
    /** 降智 on a creature other than a player (天缚): while it lasts it keeps turning from players to the monsters near it */
    public static int befuddledMob(LivingEntity mob){return mob instanceof Player||mob.level().isClientSide?0:mob.getPersistentData().getInt(BEFUDDLED);}
    public static void applyBefuddledMob(LivingEntity mob,int ticks){
        if(mob instanceof Player||mob.level().isClientSide)return;
        mob.getPersistentData().putInt(BEFUDDLED,Math.max(mob.getPersistentData().getInt(BEFUDDLED),ticks));
    }

    // ---- Growth's accessory, on its wearer ----
    public static int growthAttack(Player player){return player.level().isClientSide?clientGrowthAttack:player.getPersistentData().getInt(GROWTH_ATTACK);}
    public static int growthHealth(Player player){return player.level().isClientSide?clientGrowthHealth:player.getPersistentData().getInt(GROWTH_HEALTH);}
    /** ticks until every Growth stack is lost, 0 without stacks */
    public static int growthLeft(Player player){
        if(player.level().isClientSide)return clientGrowthLeft;
        if(growthAttack(player)+growthHealth(player)<=0)return 0;
        return (int)Math.max(0,GROWTH_IDLE-(player.level().getGameTime()-player.getPersistentData().getLong(GROWTH_LAST)));
    }
    /** one more stack on the attack track (a hit given) or the health track (a hit taken); the countdown starts over */
    public static void growthStep(Player player,boolean attack){
        if(player.level().isClientSide)return;
        var data=player.getPersistentData();String key=attack?GROWTH_ATTACK:GROWTH_HEALTH;
        data.putInt(key,Math.min(GROWTH_MAX,data.getInt(key)+1));data.putLong(GROWTH_LAST,player.level().getGameTime());
        if(player instanceof ServerPlayer server)sync(server);
    }
    public static void clearGrowth(Player player){
        var data=player.getPersistentData();boolean had=data.getInt(GROWTH_ATTACK)+data.getInt(GROWTH_HEALTH)>0;
        data.remove(GROWTH_ATTACK);data.remove(GROWTH_HEALTH);data.remove(GROWTH_LAST);
        if(had&&player instanceof ServerPlayer server)sync(server);
    }

    // ---- bonds ----
    public static int bonds(Player player){return player.level().isClientSide?clientBonds:player.getPersistentData().getInt(BONDS);}
    /** the lifesaving bonds' cooldowns, shown counting down on the status page */
    public static int unquenchedLeft(Player player){return player.level().isClientSide?clientUnquenchedLeft:local.primordial.effect.AffixBondEvents.unquenchedLeft(player);}
    public static int wheelLeft(Player player){return player.level().isClientSide?clientWheelLeft:local.primordial.effect.AffixBondEvents.wheelLeft(player);}

    private static void updateFeebleSpeed(LivingEntity entity){
        var speed=entity.getAttribute(Attributes.ATTACK_SPEED);if(speed==null)return;
        boolean feeble=entity.getPersistentData().getInt(FEEBLE)>0;
        if(feeble&&speed.getModifier(FEEBLE_SPEED)==null)
            speed.addTransientModifier(new AttributeModifier(FEEBLE_SPEED,"primordial.feeble",-FEEBLE_PENALTY,AttributeModifier.Operation.MULTIPLY_TOTAL));
        else if(!feeble&&speed.getModifier(FEEBLE_SPEED)!=null)speed.removeModifier(FEEBLE_SPEED);
    }
    public static void clientSync(int malice,int desaturation,int feeble,int bloodPact,int befuddled,int befuddleSeed,
        int growthAttack,int growthHealth,int growthLeft,int bonds,int unquenchedLeft,int wheelLeft){
        clientMalice=malice;clientDesaturation=desaturation;clientFeeble=feeble;clientBloodPact=bloodPact;clientBefuddled=befuddled;clientBefuddleSeed=befuddleSeed;
        clientGrowthAttack=growthAttack;clientGrowthHealth=growthHealth;clientGrowthLeft=growthLeft;clientBonds=bonds;
        clientUnquenchedLeft=unquenchedLeft;clientWheelLeft=wheelLeft;
    }
    /** between syncs the client counts the Growth countdown down itself, so it reads smoothly */
    public static void clientTick(){if(clientGrowthLeft>0)clientGrowthLeft--;if(clientUnquenchedLeft>0)clientUnquenchedLeft--;if(clientWheelLeft>0)clientWheelLeft--;}
    public static void sync(ServerPlayer player){
        MenuDropNetwork.syncAffixStatuses(player,new MenuDropNetwork.AffixStatus(malice(player),desaturation(player),feeble(player),
            bloodPact(player),befuddled(player),befuddleSeed(player),growthAttack(player),growthHealth(player),growthLeft(player),bonds(player),
            unquenchedLeft(player),wheelLeft(player)));
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event){if(event.getEntity() instanceof ServerPlayer player)sync(player);}
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent event){if(event.getEntity() instanceof ServerPlayer player)sync(player);}
    @SubscribeEvent public static void clone(PlayerEvent.Clone event){
        // Only dimensional transfers preserve combat statuses. Death always clears them.
        if(!event.isWasDeath()){
            for(String key:PLAYER_KEYS)
                event.getEntity().getPersistentData().putInt(key,event.getOriginal().getPersistentData().getInt(key));
            event.getEntity().getPersistentData().putInt(BEFUDDLE_SEED,event.getOriginal().getPersistentData().getInt(BEFUDDLE_SEED));
        }
    }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;
        var player=event.player;
        if(player.level().isClientSide)return;
        var data=player.getPersistentData();
        boolean ended=false;
        for(String key:PLAYER_KEYS)if(data.getInt(key)>0){data.putInt(key,data.getInt(key)-1);if(data.getInt(key)==0)ended=true;}
        updateFeebleSpeed(player);
        // the bonds: compared with what is worn every tick (an enum set, nothing to it)
        int bonds=player.isAlive()?AffixBonds.mask(AffixAccessories.worn(player)):0;
        boolean bondsChanged=bonds!=data.getInt(BONDS);
        if(bondsChanged){data.putInt(BONDS,bonds);local.primordial.achievement.Achievements.bonds(player,Integer.bitCount(bonds));}
        if(player.tickCount%5==0&&player.level() instanceof net.minecraft.server.level.ServerLevel level&&!player.isSpectator())particles(player,level);
        // sync every second, and at once when a status runs out (befuddled controls must not linger) or the bonds change
        if(player instanceof ServerPlayer server&&(player.tickCount%20==0||ended||bondsChanged))sync(server);
    }
    /** other creatures carry feebleness, malice, 失色, 降智 and the mob auras */
    @SubscribeEvent public static void mobTick(LivingEvent.LivingTickEvent event){
        var entity=event.getEntity();
        if(entity instanceof Player||entity.level().isClientSide)return;
        var data=entity.getPersistentData();
        for(String key:new String[]{FEEBLE,UNDYING,ENLIGHTENED,MALICE,DESATURATION,BEFUDDLED}){
            if(!data.contains(key))continue;
            int left=data.getInt(key)-1;
            if(left>0)data.putInt(key,left);else{data.remove(key);if(key.equals(DESATURATION))data.remove(DESATURATION_STACKS);}
        }
        updateFeebleSpeed(entity);
        if(entity.tickCount%20==0&&entity.isAlive()&&entity.level() instanceof net.minecraft.server.level.ServerLevel level){
            if(data.getInt(MALICE)>0)drawMalice(entity,level);
            if(data.getInt(BEFUDDLED)>0&&entity instanceof Mob mob)befuddle(mob,level);
        }
        if(entity.tickCount%5==0&&entity.level() instanceof net.minecraft.server.level.ServerLevel level&&!entity.isInvisible())particles(entity,level);
    }
    /** the monsters that may be turned by a status: hostile, not a boss */
    private static boolean turnable(Mob mob){
        return mob.isAlive()&&mob instanceof Enemy&&!AffixEvents.isBoss(mob)&&!(mob instanceof local.primordial.boss.SinEntity);
    }
    /** creatures' malice: every monster within 16 blocks goes for the marked one, unless it is already on one so marked */
    private static void drawMalice(LivingEntity marked,net.minecraft.server.level.ServerLevel level){
        for(var mob:level.getEntitiesOfClass(Mob.class,marked.getBoundingBox().inflate(16),m->m!=marked&&turnable(m)&&m.distanceToSqr(marked)<=256)){
            var current=mob.getTarget();
            if(current==marked||current!=null&&!(current instanceof Player)&&current.isAlive()&&malice(current)>0)continue;
            mob.setTarget(marked);
        }
    }
    /** creatures' 降智: a mob hunting a player turns on another monster near it instead */
    private static void befuddle(Mob mob,net.minecraft.server.level.ServerLevel level){
        if(!(mob.getTarget() instanceof Player)||AffixEvents.isBoss(mob)||mob instanceof local.primordial.boss.SinEntity)return;
        var others=level.getEntitiesOfClass(Mob.class,mob.getBoundingBox().inflate(10),m->m!=mob&&turnable(m));
        if(others.isEmpty())return;
        mob.setTarget(others.get(mob.getRandom().nextInt(others.size())));mob.getNavigation().stop();
    }

    // ---- what each status looks like, seen by everyone nearby ----
    private static net.minecraft.core.particles.DustParticleOptions dust(int rgb,float size){
        return new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(((rgb>>16)&255)/255F,((rgb>>8)&255)/255F,(rgb&255)/255F),size);
    }
    private static final net.minecraft.core.particles.DustParticleOptions MALICE_DUST=dust(0x9A1CC8,1.0F),FEEBLE_DUST=dust(0xA89A6A,0.8F),
        BLOOD_DUST=dust(0xB0101C,1.1F),BEFUDDLED_DUST=dust(0xE0A8F8,0.9F),UNDYING_DUST=dust(0xFFD860,0.9F);
    static void particles(LivingEntity entity,net.minecraft.server.level.ServerLevel level){
        var data=entity.getPersistentData();
        double x=entity.getX(),y=entity.getY(),z=entity.getZ(),w=entity.getBbWidth()*0.5,h=entity.getBbHeight();
        // malice: a purple haze rising off the body
        if(data.getInt(MALICE)>0)level.sendParticles(MALICE_DUST,x,y+h*0.5,z,2,w,h*0.3,w,0);
        // desaturation: grey ash drifting down
        if(data.getInt(DESATURATION)>0)level.sendParticles(net.minecraft.core.particles.ParticleTypes.WHITE_ASH,x,y+h*0.8,z,3,w,h*0.2,w,0);
        // feeble: dull dust sagging at the feet
        if(data.getInt(FEEBLE)>0)level.sendParticles(FEEBLE_DUST,x,y+0.2,z,2,w,0.1,w,0);
        // blood pact: drops of blood around the body
        if(data.getInt(BLOOD_PACT)>0){level.sendParticles(BLOOD_DUST,x,y+h*0.6,z,2,w,h*0.3,w,0);
            if(entity.tickCount%20==0)level.sendParticles(net.minecraft.core.particles.ParticleTypes.DRIPPING_LAVA,x,y+h*0.9,z,1,w,0.1,w,0);}
        // befuddled: motes circling above the head
        if(data.getInt(BEFUDDLED)>0){double t=level.getGameTime()*0.35;
            for(int i=0;i<3;i++){double a=t+i*Math.PI*2/3;
                level.sendParticles(BEFUDDLED_DUST,x+Math.cos(a)*0.45,y+h+0.35,z+Math.sin(a)*0.45,1,0,0,0,0);}}
        // undying (mobs): a faint golden glimmer while it can still be saved
        if(data.getInt(UNDYING)>0&&entity.tickCount%10==0)level.sendParticles(UNDYING_DUST,x,y+h*0.5,z,1,w,h*0.3,w,0);
        // enlightened (mobs): runes drawn in toward it
        if(data.getInt(ENLIGHTENED)>0&&entity.tickCount%10==0)level.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT,x,y+h+0.5,z,3,0.4,0.2,0.4,0.5);
    }
}
