package local.primordial.spirit;

import java.util.ArrayList;
import java.util.List;
import local.primordial.Primordial;
import local.primordial.affix.AffixEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Spirits come from every world, whatever island they appear on: around each player in the realm, every two seconds,
 * a random land creature (monster, animal or ambient, from any dimension or mod; no bosses, nothing bound to water)
 * may rise on the island ground 16-40 blocks away, up to 24 nearby. Those left far from every player fade away.
 */
@Mod.EventBusSubscriber(modid=Primordial.ID)
public final class SpiritSpawner {
    public static final String SPAWNED="primordial:spirit_spawned";
    public static final int CAP=24;
    private static List<EntityType<?>> pool;
    private SpiritSpawner() {}

    /** every land creature a world could hold */
    public static List<EntityType<?>> pool() {
        if(pool==null){
            var out=new ArrayList<EntityType<?>>();
            for(var type:ForgeRegistries.ENTITY_TYPES){
                var category=type.getCategory();
                if(category!=MobCategory.MONSTER&&category!=MobCategory.CREATURE&&category!=MobCategory.AMBIENT)continue;
                if(!type.canSummon()||type.is(AffixEvents.BOSSES)||local.primordial.boss.SinBosses.isSinType(type)||type==EntityType.ENDER_DRAGON||type==EntityType.WITHER
                    ||type==EntityType.WARDEN||type==EntityType.GIANT||type==EntityType.ILLUSIONER||type==EntityType.ELDER_GUARDIAN)continue;
                out.add(type);
            }
            pool=out;
        }
        return pool;
    }
    public static void spawnAround(ServerPlayer player) {
        if(player.isSpectator()||!(player.level() instanceof ServerLevel level))return;
        long near=level.getEntitiesOfClass(Mob.class,player.getBoundingBox().inflate(64),m->m.getPersistentData().getBoolean(SPAWNED)).size();
        if(near>=CAP)return;
        var random=player.getRandom();var types=pool();if(types.isEmpty())return;
        for(int attempt=0;attempt<6;attempt++){
            double angle=random.nextDouble()*Math.PI*2,distance=16+random.nextDouble()*24;
            int x=(int)Math.floor(player.getX()+Math.cos(angle)*distance),z=(int)Math.floor(player.getZ()+Math.sin(angle)*distance);
            if(!level.hasChunk(x>>4,z>>4)||SinSanctums.near(x,z,SinSanctums.RADIUS+12))continue;
            int y=level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z);
            if(y<=level.getMinBuildHeight()||PrimordialTemple.within(new net.minecraft.world.phys.Vec3(x+0.5,y,z+0.5)))continue;   // and never in the Primordial Temple
            var ground=new BlockPos(x,y-1,z);
            if(!level.getBlockState(ground).isFaceSturdy(level,ground,net.minecraft.core.Direction.UP))continue;
            var type=types.get(random.nextInt(types.size()));
            var box=type.getAABB(x+0.5,y,z+0.5);
            if(!level.noCollision(box)||level.containsAnyLiquid(box))continue;
            if(!(type.create(level) instanceof Mob mob))continue;
            mob.moveTo(x+0.5,y,z+0.5,random.nextFloat()*360,0);
            mob.getPersistentData().putBoolean(SPAWNED,true);
            mob.finalizeSpawn(level,level.getCurrentDifficultyAt(ground),MobSpawnType.EVENT,null,null);
            if(level.addFreshEntity(mob))return;
        }
    }
    /** a spawned spirit with no player within 96 blocks fades */
    @SubscribeEvent public static void spiritFade(LivingEvent.LivingTickEvent event) {
        var entity=event.getEntity();
        if(entity.tickCount%100!=0||entity.level().isClientSide||!entity.getPersistentData().getBoolean(SPAWNED)||!SpiritRealm.inRealm(entity))return;
        if(entity.level().getNearestPlayer(entity,96)==null)entity.discard();
    }
}
