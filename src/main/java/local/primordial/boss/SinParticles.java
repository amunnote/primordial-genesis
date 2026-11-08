package local.primordial.boss;

import local.primordial.Primordial;
import local.primordial.entity.alchemical.ArcaneParticles;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Each Sin Lord's own particle, coloured by whoever sends it: Pride's golden glints, Wrath's embers, Envy's shadow wisps,
 * Greed's falling coins, Lust's drifting petals, Gluttony's void bubbles, Sloth's sleepy Zs.
 */
public final class SinParticles {
    static final DeferredRegister<ParticleType<?>> PARTICLES=DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES,Primordial.ID);
    public static final RegistryObject<ArcaneParticles.Type> GLINT=PARTICLES.register("pride_glint",ArcaneParticles.Type::new);
    /** Pride's finer motes: gold dust that spirals and shimmers, and soft regal flares */
    public static final RegistryObject<ArcaneParticles.Type> GOLD_DUST=PARTICLES.register("pride_gold_dust",ArcaneParticles.Type::new);
    public static final RegistryObject<ArcaneParticles.Type> FLARE=PARTICLES.register("pride_flare",ArcaneParticles.Type::new);
    /** Wrath's fiercer motes: sparks flung from blows that fall and burn out, and dark smoke rolling off its hide */
    public static final RegistryObject<ArcaneParticles.Type> SPARK=PARTICLES.register("wrath_spark",ArcaneParticles.Type::new);
    public static final RegistryObject<ArcaneParticles.Type> SMOKE=PARTICLES.register("wrath_smoke",ArcaneParticles.Type::new);
    public static final RegistryObject<ArcaneParticles.Type> EMBER=PARTICLES.register("wrath_ember",ArcaneParticles.Type::new);
    /** Envy's mirror shards: little glinting fragments that turn as they fly */
    public static final RegistryObject<ArcaneParticles.Type> SHARD=PARTICLES.register("envy_shard",ArcaneParticles.Type::new);
    public static final RegistryObject<ArcaneParticles.Type> WISP=PARTICLES.register("envy_wisp",ArcaneParticles.Type::new);
    public static final RegistryObject<ArcaneParticles.Type> COIN=PARTICLES.register("greed_coin",ArcaneParticles.Type::new);
    public static final RegistryObject<ArcaneParticles.Type> PETAL=PARTICLES.register("lust_petal",ArcaneParticles.Type::new);
    public static final RegistryObject<ArcaneParticles.Type> BUBBLE=PARTICLES.register("gluttony_bubble",ArcaneParticles.Type::new);
    public static final RegistryObject<ArcaneParticles.Type> DREAM=PARTICLES.register("sloth_dream",ArcaneParticles.Type::new);
    private SinParticles() {}
    public static void register(IEventBus bus) {PARTICLES.register(bus);}

    /** send count particles round a spot, spread out, all with the given motion */
    public static void send(ServerLevel level,ArcaneParticles.Type type,int colour,Vec3 at,int count,double spread,double vx,double vy,double vz) {
        var random=level.random;
        for(int i=0;i<count;i++)
            level.sendParticles(type.of(colour),at.x+(random.nextDouble()-0.5)*2*spread,at.y+(random.nextDouble()-0.5)*spread,at.z+(random.nextDouble()-0.5)*2*spread,0,vx,vy,vz,1);
    }
    /** a circle of particles on the ground */
    public static void ring(ServerLevel level,ArcaneParticles.Type type,int colour,Vec3 at,double radius,int count,double rise) {
        double turn=level.getGameTime()*0.05;
        for(int i=0;i<count;i++){double a=turn+Math.PI*2*i/count;level.sendParticles(type.of(colour),at.x+Math.cos(a)*radius,at.y+0.1,at.z+Math.sin(a)*radius,0,0,rise,0,1);}
    }
    /**
     * One particle seen from far: vanilla sends particles only to players within 32 blocks, which is too near for marks
     * drawn across a great arena or high over it; these go to everyone within 128.
     */
    public static void far(ServerLevel level,ArcaneParticles.Type type,int colour,double x,double y,double z,double vx,double vy,double vz) {
        var options=type.of(colour);
        for(var player:level.players())if(player.distanceToSqr(x,y,z)<128*128)level.sendParticles(player,options,true,x,y,z,0,vx,vy,vz,1);
    }
    public static void ringFar(ServerLevel level,ArcaneParticles.Type type,int colour,Vec3 at,double radius,int count,double rise) {
        double turn=level.getGameTime()*0.05;
        for(int i=0;i<count;i++){double a=turn+Math.PI*2*i/count;far(level,type,colour,at.x+Math.cos(a)*radius,at.y+0.1,at.z+Math.sin(a)*radius,0,rise,0);}
    }
    public static void lineFar(ServerLevel level,ArcaneParticles.Type type,int colour,Vec3 from,Vec3 to,double spacing,double rise) {
        var step=to.subtract(from);int n=(int)Math.max(1,step.length()/spacing);
        for(int i=0;i<=n;i++){var p=from.add(step.scale(i/(double)n));far(level,type,colour,p.x,p.y,p.z,0,rise,0);}
    }
    /** particles strung along a line */
    public static void line(ServerLevel level,ArcaneParticles.Type type,int colour,Vec3 from,Vec3 to,double spacing,double rise) {
        var step=to.subtract(from);int n=(int)Math.max(1,step.length()/spacing);
        for(int i=0;i<=n;i++){var p=from.add(step.scale(i/(double)n));level.sendParticles(type.of(colour),p.x,p.y,p.z,0,0,rise,0,1);}
    }
}
