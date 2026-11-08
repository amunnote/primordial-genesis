package local.primordial.entity.alchemical;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A creation's signature attack. It waits out its cooldown; once its target stands in reach (and in sight, if it must
 * be seen), it marks a spot and winds up, its warning drawn in glyphs where everyone can see it, then strikes. The wind-up
 * is the window to get clear. If the target dies or is lost meanwhile, the cast fizzles and comes back sooner.
 */
public final class ArcaneSkill {
    /** where the strike will land, chosen as the wind-up begins */
    public interface Mark {Vec3 at(Mob mob,LivingEntity target);}
    /** each tick of the wind-up, with the ticks left */
    public interface Telegraph {void tick(ServerLevel level,Mob mob,LivingEntity target,Vec3 mark,int left);}
    /** the strike */
    public interface Release {void release(ServerLevel level,Mob mob,LivingEntity target,Vec3 mark);}

    final int cooldown,windup;final double min,max;final boolean sight;
    final Mark mark;final Telegraph telegraph;final Release release;
    private int timer=40,left=-1;private Vec3 spot;private LivingEntity target;
    /** when set, it only begins while this holds (its cooldown still runs down meanwhile) */
    private java.util.function.Predicate<Mob> when;

    public ArcaneSkill(int cooldown,int windup,double min,double max,boolean sight,Mark mark,Telegraph telegraph,Release release) {
        this.cooldown=cooldown;this.windup=windup;this.min=min;this.max=max;this.sight=sight;
        this.mark=mark;this.telegraph=telegraph;this.release=release;
    }
    public boolean casting() {return left>=0;}
    public int cooldownLeft() {return timer;}
    /** called every server tick by its creation */
    public void tick(Mob mob) {tick(mob,true);}
    /**
     * Called every server tick; a creature with several skills lets only one begin at a time (mayStart false keeps
     * this one waiting once its cooldown is done). Returns whether it began its wind-up this tick.
     */
    public boolean tick(Mob mob,boolean mayStart) {
        boolean was=left>=0;
        tickInner(mob,mayStart);
        return !was&&left>=0;
    }
    /** let it come round again after this many ticks at most */
    public void hurry(int ticks) {if(left<0)timer=Math.min(timer,ticks);}
    /** keep it from coming round for at least this many ticks */
    public void hold(int ticks) {if(left<0)timer=Math.max(timer,ticks);}
    /** only begin while the condition holds */
    public ArcaneSkill when(java.util.function.Predicate<Mob> condition) {when=condition;return this;}
    private void tickInner(Mob mob,boolean mayStart) {
        if(!(mob.level() instanceof ServerLevel level))return;
        if(left>=0){
            if(target==null||!target.isAlive()||mob.isDeadOrDying()){left=-1;timer=cooldown/3;return;}
            if(left>0){telegraph.tick(level,mob,target,spot,left);left--;return;}
            left=-1;timer=cooldown;
            release.release(level,mob,target,spot);
            return;
        }
        if(timer>0){timer--;return;}
        if(!mayStart||when!=null&&!when.test(mob))return;
        var t=mob.getTarget();
        if(t==null||!t.isAlive()||!foe(mob,t))return;
        double d=mob.distanceTo(t);
        if(d<min||d>max||sight&&!mob.hasLineOfSight(t))return;
        target=t;spot=mark.at(mob,t);left=windup;
    }
    /** begin the wind-up now at a target, skipping the cooldown (for tests and scene scripts) */
    public void start(Mob mob,LivingEntity target) {this.target=target;spot=mark.at(mob,target);left=windup;timer=0;}
    /** cast at once, skipping cooldown and wind-up (for tests and commands) */
    public void cast(Mob mob,LivingEntity target) {
        if(mob.level() instanceof ServerLevel level){timer=cooldown;left=-1;release.release(level,mob,target,mark.at(mob,target));}
    }

    // ------------------------------------------------------------------ shared pieces
    /** what a creation's magic may hurt: the living, but not its own kind, other monsters, or players beyond harm */
    public static boolean foe(Mob mob,LivingEntity e) {
        if(e==mob||!e.isAlive()||e instanceof AlchemicalCreation||e instanceof Enemy||e instanceof local.primordial.boss.SinEntity)return false;
        return !(e instanceof Player player)||!player.isCreative()&&!player.isSpectator();
    }
    /** its foes within a radius of a spot (and within height above and below) */
    public static List<LivingEntity> foesAround(Mob mob,Vec3 at,double radius,double height) {
        var out=new ArrayList<LivingEntity>();
        for(var e:mob.level().getEntitiesOfClass(LivingEntity.class,new AABB(at,at).inflate(radius,height,radius)))
            if(foe(mob,e)&&horizontal(e.position(),at)<=radius+e.getBbWidth()/2)out.add(e);
        return out;
    }
    public static double horizontal(Vec3 a,Vec3 b) {double dx=a.x-b.x,dz=a.z-b.z;return Math.sqrt(dx*dx+dz*dz);}
    /** thrown away from a spot (or up, from right on it), the motion reaching players too */
    public static void knock(LivingEntity e,Vec3 from,double outward,double up) {
        var away=new Vec3(e.getX()-from.x,0,e.getZ()-from.z);
        away=away.lengthSqr()<1e-4?Vec3.ZERO:away.normalize().scale(outward*(1-e.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE)));
        e.setDeltaMovement(e.getDeltaMovement().add(away.x,up,away.z));
        e.hurtMarked=true;
    }
    public static DamageSource melee(Mob mob) {return mob.damageSources().mobAttack(mob);}
    public static DamageSource magic(Mob mob) {return mob.damageSources().indirectMagic(mob,mob);}
    public static int colour(Mob mob) {
        if(mob instanceof local.primordial.boss.SinEntity sin)return sin.colour();
        return mob instanceof AlchemicalCreation c?ArcaneParticles.colour(c.creationId()):0xFFFFFF;
    }

    // ------------------------------------------------------------------ particles everyone sees
    public static void glyph(ServerLevel level,Mob mob,double x,double y,double z,double vx,double vy,double vz) {
        level.sendParticles(ArcaneParticles.GLYPH.get().of(colour(mob)),x,y,z,0,vx,vy,vz,1);
    }
    public static void spark(ServerLevel level,Mob mob,double x,double y,double z,double vx,double vy,double vz) {
        level.sendParticles(ArcaneParticles.SPARK.get().of(colour(mob)),x,y,z,0,vx,vy,vz,1);
    }
    /** a circle of glyphs on the ground, drifting up */
    public static void ring(ServerLevel level,Mob mob,Vec3 at,double radius,int count,double rise) {
        double turn=level.getGameTime()*0.05;
        for(int i=0;i<count;i++){double a=turn+Math.PI*2*i/count;glyph(level,mob,at.x+Math.cos(a)*radius,at.y+0.1,at.z+Math.sin(a)*radius,0,rise,0);}
    }
    /** sparks strung along a line */
    public static void line(ServerLevel level,Mob mob,Vec3 from,Vec3 to,double spacing) {
        var step=to.subtract(from);int n=(int)Math.max(1,step.length()/spacing);
        for(int i=0;i<=n;i++){var p=from.add(step.scale(i/(double)n));spark(level,mob,p.x,p.y,p.z,0,0.01,0);}
    }
    /** sparks flung out from a spot */
    public static void burst(ServerLevel level,Mob mob,Vec3 at,int count,double speed) {
        var random=level.random;
        for(int i=0;i<count;i++){
            var d=new Vec3(random.nextGaussian(),random.nextGaussian()*0.6+0.4,random.nextGaussian()).normalize().scale(speed);
            spark(level,mob,at.x,at.y,at.z,d.x,d.y,d.z);
        }
    }
    public static void vanilla(ServerLevel level,ParticleOptions particle,Vec3 at,int count,double spread,double speed) {
        level.sendParticles(particle,at.x,at.y,at.z,count,spread,spread*0.5,spread,speed);
    }
}
