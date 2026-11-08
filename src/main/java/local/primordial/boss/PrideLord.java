package local.primordial.boss;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import local.primordial.affix.BossAffix;
import local.primordial.entity.alchemical.ArcaneSkill;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 傲慢之主 · 黄金王 — Pride, the Archer. A golden king who will not stoop to touch the ground: he hovers well out of reach
 * and fights with his treasury alone, the strongest of the seven.
 * <ul>
 * <li>王之财宝 Gate of the Treasury: golden gates ripple open in two tiers behind him (six, ten once enraged), a weapon
 *     from his vault sliding out of each, point first; then they all fly at you. Any weapon in the game, other mods'
 *     included, each hitting as hard as the weapon itself.</li>
 * <li>天之锁 Chains of Heaven: four small gates open round you and chains reach out of them; when they close you are
 *     bound for two and a half seconds.</li>
 * <li>天穹齐射 Sky Volley (enraged): sixteen great gates open in a ring high over you and rain their weapons down.</li>
 * <li>Come within four blocks and he blinks away.</li>
 * </ul>
 */
public class PrideLord extends SinBoss {
    public static final double HEALTH=600;
    public static final int GATES=6,ENRAGED_GATES=10,VOLLEY=16,CHAIN_TICKS=50,GATE_WINDUP=22,CHAIN_WINDUP=16,VOLLEY_WINDUP=40;
    private final List<TreasuryGate> gates=new ArrayList<>(),chainGates=new ArrayList<>(),skyGates=new ArrayList<>();
    private final Map<UUID,Integer> chained=new HashMap<>();
    private int blink;

    public PrideLord(EntityType<? extends PrideLord> type,Level level) {
        super(type,level);
        setNoGravity(true);
        skills.add(new ArcaneSkill(55,GATE_WINDUP,0,40,true,(mob,target)->{openGates(target);return target.position();},
            (level1,mob,target,mark,left)->{for(var gate:gates)gate.aim(aimPoint(gate.position(),target));},
            (level1,mob,target,mark)->fireGates(level1,target)));
        skills.add(new ArcaneSkill(260,CHAIN_WINDUP,3,24,true,(mob,target)->{openChains(target);return target.position();},
            (level1,mob,target,mark,left)->{
                float reach=1-left/(float)CHAIN_WINDUP;
                for(var gate:chainGates){
                    gate.aim(target.position().add(0,1,0));
                    var from=gate.position();var to=from.lerp(target.position().add(0,1,0),reach);
                    SinParticles.line(level1,SinParticles.GOLD_DUST.get(),colour(),from,to,0.35,0);
                }
            },
            (level1,mob,target,mark)->{
                act(2,15);chained.put(target.getUUID(),CHAIN_TICKS);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,CHAIN_TICKS,6),this);
                for(var gate:chainGates)gate.fire();chainGates.clear();
                SinParticles.send(level1,SinParticles.FLARE.get(),colour(),target.position().add(0,1,0),6,0.4,0,0.02,0);
                sound(SoundEvents.CHAIN_PLACE,2F,0.6F);sound(SoundEvents.ANVIL_LAND,0.6F,1.8F);
            }));
        skills.add(new ArcaneSkill(300,VOLLEY_WINDUP,0,40,false,(mob,target)->{if(enraged())openSky(target.position());return target.position();},
            (level1,mob,target,mark,left)->{if(enraged()&&left%4==0)SinParticles.ring(level1,SinParticles.GOLD_DUST.get(),colour(),mark,7,28,0.06);},
            (level1,mob,target,mark)->{if(enraged())volley(level1,mark);}));
    }
    @Override public BossAffix sin() {return BossAffix.PRIDE;}
    @Override public String bossId() {return "pride_lord";}
    @Override protected BossEvent.BossBarColor barColour() {return BossEvent.BossBarColor.YELLOW;}
    @Override protected String[] actions() {return new String[]{"","gate","chain","volley"};}
    @Override protected SoundEvent wakeSound() {return SoundEvents.BEACON_ACTIVATE;}
    @Override public int experience() {return 700;}

    static Vec3 aimPoint(Vec3 from,LivingEntity target) {
        var centre=target.position().add(0,target.getBbHeight()*0.55,0);
        var lead=target.getDeltaMovement().multiply(1,0,1).scale(from.distanceTo(centre)/1.8);
        return centre.add(lead);
    }
    /** the treasury opens behind him: two tiers of gates in an arc, facing the foe */
    void openGates(LivingEntity target) {
        closeAll(gates);
        int count=enraged()?ENRAGED_GATES:GATES;
        var toward=target.position().subtract(position()).multiply(1,0,1);
        toward=toward.lengthSqr()<1e-4?new Vec3(0,0,1):toward.normalize();
        var side=new Vec3(-toward.z,0,toward.x);
        int upper=count/2,lower=count-upper;
        for(int tier=0;tier<2;tier++){
            int n=tier==0?lower:upper;
            for(int i=0;i<n;i++){
                double t=n==1?0:(i/(n-1.0)-0.5)*2;
                var at=position().add(side.scale(t*(tier==0?4.2:3.2))).add(toward.scale(-1.6+Math.abs(t)*0.9))
                    .add(0,(tier==0?2.0:4.6)+Math.cos(t*Math.PI*0.5)*0.8,0);
                gates.add(TreasuryGate.open(level(),at,aimPoint(at,target),TreasureWeapon.randomTreasure(getRandom()),GATE_WINDUP,tier==0?1F:0.85F));
            }
        }
        act(1,GATE_WINDUP+8);
        sound(SoundEvents.BEACON_POWER_SELECT,1.6F,1.5F);
    }
    void fireGates(ServerLevel level,LivingEntity target) {
        for(var gate:gates){
            if(gate.isRemoved())continue;
            var stack=gate.weapon().isEmpty()?TreasureWeapon.randomTreasure(getRandom()):gate.weapon();
            var from=gate.position().add(gate.facing().scale(0.4));
            TreasureWeapon.loose(level,this,stack,from,aimPoint(from,target),1.8,TreasureWeapon.damageOf(stack)*(enraged()?1.2F:1F),colour());
            gate.fire();
        }
        gates.clear();
        sound(SoundEvents.ILLUSIONER_CAST_SPELL,2F,1.4F);sound(SoundEvents.TRIDENT_THROW,2F,0.8F);
    }
    /** four small gates round the foe, chains reaching out of them */
    void openChains(LivingEntity target) {
        closeAll(chainGates);
        for(int i=0;i<4;i++){
            double a=Math.PI/2*i+getRandom().nextDouble()*0.5;
            var at=target.position().add(Math.cos(a)*3.5,1.4+(i%2)*0.8,Math.sin(a)*3.5);
            chainGates.add(TreasuryGate.open(level(),at,target.position().add(0,1,0),new ItemStack(Items.CHAIN),CHAIN_WINDUP,0.7F));
        }
        sound(SoundEvents.CHAIN_PLACE,1.5F,0.8F);
    }
    /** sixteen great gates in a ring high over a spot */
    void openSky(Vec3 mark) {
        closeAll(skyGates);
        for(int i=0;i<VOLLEY;i++){
            double a=Math.PI*2*i/VOLLEY;
            var at=mark.add(Math.cos(a)*8,13+(i%2)*1.5,Math.sin(a)*8);
            skyGates.add(TreasuryGate.open(level(),at,mark.add((getRandom().nextDouble()-0.5)*6,0,(getRandom().nextDouble()-0.5)*6),
                TreasureWeapon.randomTreasure(getRandom()),VOLLEY_WINDUP,1.4F));
        }
        act(3,VOLLEY_WINDUP+10);
        sound(SoundEvents.BEACON_ACTIVATE,3F,0.7F);
    }
    void volley(ServerLevel level,Vec3 mark) {
        for(var gate:skyGates){
            if(gate.isRemoved())continue;
            var to=mark.add((getRandom().nextDouble()-0.5)*12,0,(getRandom().nextDouble()-0.5)*12);
            var stack=gate.weapon().isEmpty()?TreasureWeapon.randomTreasure(getRandom()):gate.weapon();
            TreasureWeapon.loose(level,this,stack,gate.position(),to,1.6,TreasureWeapon.damageOf(stack),colour());
            gate.fire();
        }
        skyGates.clear();
        sound(SoundEvents.ILLUSIONER_CAST_SPELL,3F,0.8F);
    }
    static void closeAll(List<TreasuryGate> list) {for(var gate:list)if(!gate.isRemoved())gate.fire();list.clear();}
    public List<TreasuryGate> gates() {return gates;}
    public boolean isChained(LivingEntity e) {return chained.containsKey(e.getUUID());}

    @Override protected void fight(ServerLevel level) {
        var target=getTarget();
        var seat=Vec3.atBottomCenterOf(home());
        // hover: a dozen blocks from the target and above it, or over his seat
        Vec3 want;
        if(target!=null){
            var away=position().subtract(target.position()).multiply(1,0,1);
            away=away.lengthSqr()<1e-3?new Vec3(1,0,0):away.normalize();
            want=target.position().add(away.scale(12)).add(0,5,0);
            if(want.distanceTo(seat)>ARENA_RADIUS-4)want=seat.add(want.subtract(seat).normalize().scale(ARENA_RADIUS-4)).add(0,5,0);
            getLookControl().setLookAt(target,30,30);
            setYRot((float)(Mth.atan2(target.getZ()-getZ(),target.getX()-getX())*Mth.RAD_TO_DEG)-90);yBodyRot=getYRot();
        }else want=seat.add(0,4,0);
        // while his gates are open he holds still, so they stay behind him
        var move=gates.isEmpty()?want.subtract(position()).scale(0.06):Vec3.ZERO;
        if(move.length()>0.45)move=move.normalize().scale(0.45);
        setDeltaMovement(move);
        // too close: blink away
        if(blink>0)blink--;
        if(target!=null&&blink==0&&distanceTo(target)<4){
            blink=80;closeAll(gates);
            var away=position().subtract(target.position()).multiply(1,0,1).normalize();
            var to=target.position().add(away.scale(10)).add(0,4,0);
            if(to.distanceTo(seat)>ARENA_RADIUS)to=seat.add(0,5,0);
            SinParticles.send(level,SinParticles.FLARE.get(),colour(),position().add(0,1.5,0),8,0.6,0,0.02,0);
            SinParticles.send(level,SinParticles.GOLD_DUST.get(),colour(),position().add(0,1.5,0),30,0.8,0,0.06,0);
            teleportTo(to.x,to.y,to.z);
            SinParticles.send(level,SinParticles.FLARE.get(),colour(),to.add(0,1.5,0),8,0.6,0,0.02,0);
            sound(SoundEvents.ENDERMAN_TELEPORT,1.5F,1.6F);
        }
        // the chained cannot move: golden chains hold them from four sides
        for(var it=chained.entrySet().iterator();it.hasNext();){
            var e=it.next();
            if(level.getEntity(e.getKey()) instanceof LivingEntity bound&&bound.isAlive()&&e.getValue()>0){
                e.setValue(e.getValue()-1);
                bound.setDeltaMovement(0,Math.min(0,bound.getDeltaMovement().y),0);bound.hurtMarked=true;
                if(e.getValue()%3==0)for(int i=0;i<4;i++){double a=Math.PI/2*i+Math.PI/4;
                    SinParticles.line(level,SinParticles.GOLD_DUST.get(),colour(),bound.position().add(Math.cos(a)*3,1.8,Math.sin(a)*3),bound.position().add(0,1,0),0.3,0);}
            }else it.remove();
        }
    }
    @Override protected void onEnrage(ServerLevel level) {
        for(var player:bar.getPlayers())player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.primordial.pride_lord.enrage").withStyle(s->s.withColor(colour())),false);
        SinParticles.send(level,SinParticles.FLARE.get(),colour(),position().add(0,2,0),16,1.5,0,0.04,0);
    }
    @Override protected void onRest() {closeAll(gates);closeAll(chainGates);closeAll(skyGates);chained.clear();}
    @Override public void die(net.minecraft.world.damagesource.DamageSource source) {super.die(source);onRest();}
    @Override protected void ambient() {
        // gold dust drifting off him, and now and then a glint on his armour
        var random=getRandom();
        if(random.nextInt(2)==0){
            double a=random.nextDouble()*Math.PI*2,r=0.6+random.nextDouble()*0.8;
            level().addParticle(SinParticles.GOLD_DUST.get().of(colour()),getX()+Math.cos(a)*r,getY()+0.5+random.nextDouble()*2.6,getZ()+Math.sin(a)*r,0,0.015,0);
        }
        if(random.nextInt(9)==0)
            level().addParticle(SinParticles.GLINT.get().of(0xFFF4C8),getX()+(random.nextDouble()-0.5)*1.2,getY()+1+random.nextDouble()*2,getZ()+(random.nextDouble()-0.5)*1.2,0,0,0);
    }
}
