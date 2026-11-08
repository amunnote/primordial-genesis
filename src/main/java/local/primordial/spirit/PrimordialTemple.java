package local.primordial.spirit;

import java.util.EnumSet;
import local.primordial.Primordial;
import local.primordial.affix.BossAffix;
import local.primordial.boss.SinBoss;
import local.primordial.boss.SinBosses;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 原初神殿 The Primordial Temple: a great white-and-gold floating temple high over the arrival island, centred on (0, 0)
 * of the Spirit Realm, where the Primordial Lord sleeps on the dais before its throne. Seven arches stand round its rim,
 * one in each sin's own stone, a brazier on each; a ring of light floats overhead.
 * <p>
 * It is raised whole, once per world, the first time anyone is in the realm (so worlds made before it get it too), with a
 * Primordial Gate on the arrival island to carry worthy players up and another at its south rim to carry them down.
 * <p>
 * Only a player who has fought each of the seven Sin Lords to its fall may enter: one who dealt the killing blow (the
 * kill statistic) or was within {@link #WITNESS} blocks when it fell. Anyone else who comes near is turned back to the
 * arrival island. Each player's braziers burn for the lords they have felled.
 */
@Mod.EventBusSubscriber(modid=Primordial.ID)
public final class PrimordialTemple {
    public static final int FLOOR=180,RADIUS=62,DEPTH=28,ARENA=36,COLONNADE=44,ARCHES=55,HALO=44,HALO_RADIUS=34,CLEAR=40;
    /** where the lord sleeps, where the gate sets you down, and the gate on the temple's rim */
    public static final BlockPos SEAT=new BlockPos(0,FLOOR+2,0),ENTRY=new BlockPos(0,FLOOR+1,53),RIM_GATE=new BlockPos(0,FLOOR+1,57);
    public static final double WITNESS=64;
    public static final String DEFEATED="primordial:sins_defeated",GUARDIANS="primordial:guardians_defeated",PRIMORDIAL="primordial:primordial_felled";
    private PrimordialTemple() {}

    // ------------------------------------------------------------------ who may enter
    /** the lords a player has felled or seen fall */
    public static EnumSet<BossAffix> defeated(Player player) {
        var out=EnumSet.noneOf(BossAffix.class);
        for(var entry:persisted(player).getList(DEFEATED,Tag.TAG_STRING)){var sin=BossAffix.byId(entry.getAsString());if(sin!=null)out.add(sin);}
        // those slain before the temple was known are found in the player's statistics
        if(player instanceof ServerPlayer server)for(var sin:BossAffix.values())
            if(server.getStats().getValue(Stats.ENTITY_KILLED.get(SinBosses.lordType(sin)))>0)out.add(sin);
        return out;
    }
    /** the guardians a player has felled or seen fall */
    public static EnumSet<Guardian> guardians(Player player) {
        var out=EnumSet.noneOf(Guardian.class);
        for(var entry:persisted(player).getList(GUARDIANS,Tag.TAG_STRING))for(var g:Guardian.values())if(g.id.equals(entry.getAsString()))out.add(g);
        if(player instanceof ServerPlayer server)for(var g:Guardian.values())
            if(server.getStats().getValue(Stats.ENTITY_KILLED.get(g.type()))>0)out.add(g);
        return out;
    }
    /** all seven Sin Lords felled: the guardians' terraces open */
    public static boolean sinsFelled(Player player) {return defeated(player).size()==BossAffix.values().length;}
    /** all seven Sin Lords felled: the temple opens */
    public static boolean qualified(Player player) {return sinsFelled(player);}
    /** the lords still to fell before a place opens, as a list of their names (the guardians only for the temple) */
    public static MutableComponent missing(Player player) {return missing(player,true);}
    public static MutableComponent missing(Player player,boolean guardiansToo) {
        var have=defeated(player);var text=Component.empty();boolean first=true;
        for(var sin:BossAffix.values())if(!have.contains(sin)){
            if(!first)text.append(Component.literal("、"));first=false;
            text.append(Component.translatable(SinBosses.lordType(sin).getDescriptionId()).withStyle(s->s.withColor(sin.color())));
        }
        return text;
    }
    public static void recordGuardian(Player player,Guardian guardian) {
        var data=player.getPersistentData();var persisted=data.getCompound(Player.PERSISTED_NBT_TAG);
        var list=persisted.getList(GUARDIANS,Tag.TAG_STRING);
        for(var entry:list)if(entry.getAsString().equals(guardian.id))return;
        list.add(StringTag.valueOf(guardian.id));persisted.put(GUARDIANS,list);data.put(Player.PERSISTED_NBT_TAG,persisted);
        local.primordial.achievement.Achievements.bossesFelled(player);
    }
    /** a guardian has fallen: everyone fighting near it has seen it through */
    public static void recordGuardianDefeat(ServerLevel level,Guardian guardian,Vec3 at) {
        for(var player:level.players())if(SinBoss.contender(player)&&player.position().distanceTo(at)<=WITNESS)recordGuardian(player,guardian);
    }
    /** the Primordial Lord felled (or seen to fall): the Court of Truth opens */
    public static boolean primordialFelled(Player player) {
        if(persisted(player).getBoolean(PRIMORDIAL))return true;
        return player instanceof ServerPlayer server&&server.getStats().getValue(Stats.ENTITY_KILLED.get(SinBosses.PRIMORDIAL.get()))>0;
    }
    public static void recordPrimordial(Player player) {
        var data=player.getPersistentData();var persisted=data.getCompound(Player.PERSISTED_NBT_TAG);
        persisted.putBoolean(PRIMORDIAL,true);data.put(Player.PERSISTED_NBT_TAG,persisted);
        local.primordial.achievement.Achievements.bossesFelled(player);
    }
    public static void recordPrimordialDefeat(ServerLevel level,Vec3 at) {
        for(var player:level.players())if(SinBoss.contender(player)&&player.position().distanceTo(at)<=WITNESS){
            recordPrimordial(player);local.primordial.achievement.Achievements.witnessedPrimordial(player);}
    }
    private static CompoundTag persisted(Player player) {return player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);}
    public static void record(Player player,BossAffix sin) {
        var data=player.getPersistentData();var persisted=data.getCompound(Player.PERSISTED_NBT_TAG);
        var list=persisted.getList(DEFEATED,Tag.TAG_STRING);
        for(var entry:list)if(entry.getAsString().equals(sin.id()))return;
        list.add(StringTag.valueOf(sin.id()));persisted.put(DEFEATED,list);data.put(Player.PERSISTED_NBT_TAG,persisted);
        if(player instanceof ServerPlayer server)sync(server);
        local.primordial.achievement.Achievements.bossesFelled(player);
    }
    /** a Sin Lord has fallen: everyone fighting near it has seen it through */
    public static void recordDefeat(ServerLevel level,BossAffix sin,Vec3 at) {
        for(var player:level.players())if(SinBoss.contender(player)&&player.position().distanceTo(at)<=WITNESS)record(player,sin);
    }
    public static void sync(ServerPlayer player) {
        int mask=0;for(var sin:defeated(player))mask|=1<<sin.ordinal();
        local.primordial.network.MenuDropNetwork.syncSinsDefeated(player,mask);
    }

    // ------------------------------------------------------------------ the shape of it
    /** the direction of each sin's arch (and of its pillar in the fight): Pride due north, the rest round in sin order */
    public static double angle(BossAffix sin) {return -Math.PI/2+Math.PI*2*sin.ordinal()/BossAffix.values().length;}
    /** where a sin's brazier burns, on top of its arch */
    public static Vec3 brazier(BossAffix sin) {
        double a=angle(sin);
        return new Vec3(Math.round(Math.cos(a)*ARCHES)+0.5,FLOOR+16,Math.round(Math.sin(a)*ARCHES)+0.5);
    }
    static int hash(int x,int y,int z) {int h=x*73856093^y*19349663^z*83492791;h^=h>>>13;h*=0x5bd1e995;return h^(h>>>15);}
    static BlockState s(Block block) {return block.defaultBlockState();}
    /** the block the temple puts at a spot, or null where it leaves the world alone */
    public static BlockState at(int x,int y,int z) {
        int dy=y-FLOOR;
        double r=Math.sqrt((double)x*x+(double)z*z),angle=Math.atan2(z,x);
        // the ring of light overhead, with a node of each sin's glass
        if(dy>=HALO-3&&dy<=HALO+3){
            double q=r-HALO_RADIUS,v=dy-HALO;
            if(q*q+v*v<=2.6){
                for(var sin:BossAffix.values()){double d=Math.abs(Math.IEEEremainder(angle-angle(sin),Math.PI*2))*HALO_RADIUS;
                    if(d<2.2)return s(q*q+v*v<0.8?Blocks.SEA_LANTERN:SinSanctums.palette(sin).glass());}
                return s(q*q+v*v<0.8?Blocks.GLOWSTONE:Blocks.WHITE_STAINED_GLASS);
            }
        }
        if(r>RADIUS+0.5)return null;
        // the rock it floats on, veined with amethyst
        if(dy<0){
            if(dy<-DEPTH||r>RADIUS*(1+dy/(DEPTH+1.0))+0.5)return null;
            if(Math.floorMod(hash(x,y,z),11)==0)return s(Blocks.AMETHYST_BLOCK);
            return s(dy==-1?Blocks.QUARTZ_BRICKS:Blocks.CALCITE);
        }
        if(dy==0)return floor(x,z,r,angle);
        var built=above(x,dy,z,r,angle);
        if(built!=null)return built;
        // the air within is cleared, of drifting lanterns and the like
        return dy<=CLEAR?s(Blocks.AIR):null;
    }
    static BlockState floor(int x,int z,double r,double angle) {
        if(r>RADIUS-1.5)return s(Blocks.CHISELED_QUARTZ_BLOCK);
        if(r>ARENA+0.5)return s(Math.floorMod(x,4)<2^Math.floorMod(z,4)<2?Blocks.SMOOTH_QUARTZ:Blocks.CALCITE);
        // the arena: gold rings, and seven spokes each inlaid with its sin's stone, lit at their ends
        if(r>=ARENA-0.5||r>=11.5&&r<12.5||r>=23.5&&r<24.5)return s(Blocks.GOLD_BLOCK);
        for(var sin:BossAffix.values()){
            double d=Math.abs(Math.IEEEremainder(angle-angle(sin),Math.PI*2))*r;
            if(r>7&&d<0.6)return s(r>ARENA-2.5?Blocks.SEA_LANTERN:SinSanctums.palette(sin).inlay());
        }
        return s(Math.floorMod(x+z,2)==0?Blocks.SMOOTH_QUARTZ:Blocks.QUARTZ_BRICKS);
    }
    static BlockState above(int x,int dy,int z,double r,double angle) {
        // a low parapet round the rim
        if(dy==1&&r>RADIUS-1.2)return s(Blocks.QUARTZ_BRICKS);
        // the dais and the throne behind it, to the north
        if(dy==1&&r<=6.5)return s(r>5.5?Blocks.GOLD_BLOCK:r<=1?Blocks.SEA_LANTERN:Blocks.QUARTZ_BRICKS);
        if(Math.abs(x)<=2&&z>=-6&&z<=-4){
            if(z==-6&&dy>=2&&dy<=10)return s(dy==10?Blocks.GOLD_BLOCK:dy==9?Blocks.SEA_LANTERN:Blocks.QUARTZ_PILLAR);
            if(dy==2)return s(Blocks.GOLD_BLOCK);
            if(dy==3&&z==-5&&Math.abs(x)<=1)return s(Blocks.QUARTZ_BRICKS);
            if(dy==3&&Math.abs(x)==2)return s(Blocks.GOLD_BLOCK);
        }
        // the colonnade: fourteen pillars capped with light
        for(int i=0;i<14;i++){
            double a=Math.PI*2*i/14+Math.PI/14;
            int px=(int)Math.round(Math.cos(a)*COLONNADE),pz=(int)Math.round(Math.sin(a)*COLONNADE);
            if(Math.abs(x-px)<=1&&Math.abs(z-pz)<=1&&dy>=1&&dy<=13)
                return s(dy==13?(x==px&&z==pz?Blocks.SEA_LANTERN:Blocks.CHISELED_QUARTZ_BLOCK):dy==1?Blocks.QUARTZ_BRICKS:Blocks.QUARTZ_PILLAR);
        }
        // the seven arches, each in its sin's stone, a brazier on top
        for(var sin:BossAffix.values()){
            double a=angle(sin),ca=Math.cos(a),sa=Math.sin(a);
            double radial=x*ca+z*sa-ARCHES,along=-x*sa+z*ca;
            if(Math.abs(radial)>1.5||Math.abs(along)>6.5)continue;
            var p=SinSanctums.palette(sin);
            boolean leg=Math.abs(Math.abs(along)-4.5)<=1.5;
            if(leg&&dy>=1&&dy<=12)return s(dy==12?p.cap():p.pillar());
            if(dy==13)return s(p.rim());
            if(dy==14)return s(Math.abs(along)<1.5?p.heart():p.cap());
            if(dy==15&&Math.abs(along)<0.5&&Math.abs(radial)<0.5)return s(p.heart());
        }
        return null;
    }

    // ------------------------------------------------------------------ raising it
    static final class Data extends SavedData {
        boolean built,sage;
        /** the guardians whose terraces stand (worlds from before the Azure Dragon and Vermilion Bird marked only "guardians") */
        final java.util.Set<String> guardians=new java.util.HashSet<>();
        static Data load(CompoundTag tag) {
            var data=new Data();data.built=tag.getBoolean("built");data.sage=tag.getBoolean("sage");
            if(tag.getBoolean("guardians")){data.guardians.add(Guardian.BAIHU.id);data.guardians.add(Guardian.XUANWU.id);}
            for(var entry:tag.getList("terraces",Tag.TAG_STRING))data.guardians.add(entry.getAsString());
            return data;
        }
        @Override public CompoundTag save(CompoundTag tag) {
            tag.putBoolean("built",built);tag.putBoolean("sage",sage);
            var list=new net.minecraft.nbt.ListTag();for(var id:guardians)list.add(StringTag.valueOf(id));tag.put("terraces",list);
            return tag;
        }
        boolean allGuardians() {for(var g:Guardian.values())if(!guardians.contains(g.id))return false;return true;}
    }
    static Data data(ServerLevel level) {return level.getDataStorage().computeIfAbsent(Data::load,Data::new,"angel_charm_primordial_temple");}
    public static boolean built(ServerLevel level) {return data(level).built;}
    /** raise the temple, the gates and the lord: once, in the realm */
    public static void build(ServerLevel level) {
        var data=data(level);if(data.built)return;
        data.built=true;data.setDirty();
        var pos=new BlockPos.MutableBlockPos();
        int y0=FLOOR-DEPTH,y1=FLOOR+HALO+3;
        for(int cx=(-RADIUS-1)>>4;cx<=(RADIUS+1)>>4;cx++)for(int cz=(-RADIUS-1)>>4;cz<=(RADIUS+1)>>4;cz++){
            level.getChunk(cx,cz);
            for(int x=cx<<4;x<(cx<<4)+16;x++)for(int z=cz<<4;z<(cz<<4)+16;z++){
                if((double)x*x+(double)z*z>(RADIUS+1.5)*(RADIUS+1.5))continue;
                for(int y=y0;y<=y1;y++){
                    var state=at(x,y,z);
                    if(state!=null)level.setBlock(pos.set(x,y,z),state,Block.UPDATE_CLIENTS);
                }
            }
        }
        level.setBlock(RIM_GATE,SinBosses.PRIMORDIAL_GATE.get().defaultBlockState(),Block.UPDATE_ALL);
        var arrival=arrivalGate(level,0,-6);
        level.setBlock(arrival,gate(PrimordialGateBlock.Target.TEMPLE),Block.UPDATE_ALL);
        var lord=SinBosses.PRIMORDIAL.get().create(level);
        if(lord!=null){
            lord.moveTo(SEAT.getX()+0.5,SEAT.getY(),SEAT.getZ()+0.5,0,0);
            lord.setHome(SEAT);
            level.addFreshEntity(lord);
        }
        for(var player:level.players())player.sendSystemMessage(Component.translatable("message.primordial.primordial_temple.risen").withStyle(s->s.withColor(0xFFF0C8).withBold(true)));
        level.playSound(null,arrival,SoundEvents.BEACON_ACTIVATE,SoundSource.BLOCKS,2F,0.6F);
    }
    static BlockState gate(PrimordialGateBlock.Target target) {return SinBosses.PRIMORDIAL_GATE.get().defaultBlockState().setValue(PrimordialGateBlock.TARGET,target);}
    /** raise each guardian's terrace, its gate and the guardian, once, in the realm (after the temple; in older worlds, whichever are missing) */
    public static void buildGuardians(ServerLevel level) {
        var data=data(level);if(data.allGuardians())return;
        var pos=new BlockPos.MutableBlockPos();
        for(var g:Guardian.values()){
            if(data.guardians.contains(g.id))continue;
            data.guardians.add(g.id);data.setDirty();
            int r=Guardian.RADIUS+1;
            for(int cx=(g.cx-r)>>4;cx<=(g.cx+r)>>4;cx++)for(int cz=(g.cz-r)>>4;cz<=(g.cz+r)>>4;cz++){
                level.getChunk(cx,cz);
                for(int x=cx<<4;x<(cx<<4)+16;x++)for(int z=cz<<4;z<(cz<<4)+16;z++)
                    for(int y=Guardian.FLOOR-Guardian.DEPTH;y<=Guardian.FLOOR+Guardian.CLEAR;y++){
                        var state=g.at(x,y,z);
                        if(state!=null)level.setBlock(pos.set(x,y,z),state,Block.UPDATE_CLIENTS);
                    }
            }
            level.setBlock(g.gate(),SinBosses.PRIMORDIAL_GATE.get().defaultBlockState(),Block.UPDATE_ALL);
            var lord=g.type().create(level);
            if(lord!=null){var seat=g.seat();lord.moveTo(seat.getX()+0.5,seat.getY(),seat.getZ()+0.5,g.entryYaw()+180,0);lord.setHome(seat);level.addFreshEntity(lord);}
            level.setBlock(arrivalGate(level,g.gateDx,g.gateDz),gate(PrimordialGateBlock.Target.of(g)),Block.UPDATE_ALL);
        }
        for(var player:level.players())player.sendSystemMessage(Component.translatable("message.primordial.guardians.risen").withStyle(s->s.withColor(0xBFE8FF).withBold(true)));
    }
    /** a gate on the arrival island, a few steps from where travellers arrive (the temple's to the north, each guardian's at its own offset) */
    public static BlockPos arrivalGate(ServerLevel level,int dx,int dz) {
        var island=SpiritIslands.arrival();
        int x=(int)Math.floor(island.centerX())+dx,z=(int)Math.floor(island.centerZ())+dz;
        level.getChunk(x>>4,z>>4);
        int y=level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z);
        if(y<=level.getMinBuildHeight())y=island.top()+1;
        return new BlockPos(x,y,z);
    }

    // ------------------------------------------------------------------ the way in and the way out
    /** is a spot within the temple's bounds: over its floor, or in the rock beneath */
    public static boolean within(Vec3 at) {
        double r=Math.sqrt(at.x*at.x+at.z*at.z),dy=at.y-FLOOR;
        if(dy>=-3)return r<RADIUS+6;
        return dy>=-DEPTH&&r<=RADIUS*(1+dy/(DEPTH+1.0))+3;
    }
    public static void up(ServerPlayer player,ServerLevel level) {
        player.teleportTo(level,ENTRY.getX()+0.5,ENTRY.getY(),ENTRY.getZ()+0.5,180,0);
        player.fallDistance=0;
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,ENTRY.getX()+0.5,ENTRY.getY()+1,ENTRY.getZ()+0.5,40,0.5,1,0.5,0.05);
        level.playSound(null,ENTRY,SoundEvents.BEACON_POWER_SELECT,SoundSource.PLAYERS,1.2F,1.4F);
        player.displayClientMessage(Component.translatable("message.primordial.primordial_gate.up").withStyle(s->s.withColor(0xFFF0C8)),true);
    }
    /** to a guardian's terrace, set down at its edge facing in */
    public static void toGuardian(ServerPlayer player,ServerLevel level,Guardian guardian) {
        var at=guardian.entry();
        player.teleportTo(level,at.getX()+0.5,at.getY(),at.getZ()+0.5,guardian.entryYaw(),0);
        player.fallDistance=0;
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,at.getX()+0.5,at.getY()+1,at.getZ()+0.5,40,0.5,1,0.5,0.05);
        level.playSound(null,at,SoundEvents.BEACON_POWER_SELECT,SoundSource.PLAYERS,1.2F,1.2F);
    }
    public static void down(ServerPlayer player,ServerLevel level) {
        var at=SpiritRealm.arrivalPoint(level);
        player.teleportTo(level,at.x,at.y,at.z,player.getYRot(),0);
        player.fallDistance=0;
        level.playSound(null,BlockPos.containing(at),SoundEvents.BEACON_DEACTIVATE,SoundSource.PLAYERS,1F,1.2F);
    }
    @SubscribeEvent public static void levelTick(TickEvent.LevelTickEvent event) {
        if(event.phase!=TickEvent.Phase.END||!(event.level instanceof ServerLevel level)||level.dimension()!=SpiritRealm.LEVEL)return;
        if(level.getGameTime()%40!=0||level.players().isEmpty())return;
        if(!built(level))build(level);
        else if(!data(level).sage){var data=data(level);data.sage=true;data.setDirty();SageSanctum.build(level);}
    }
    /** the unworthy are turned back */
    @SubscribeEvent public static void barrier(TickEvent.PlayerTickEvent event) {
        if(event.phase!=TickEvent.Phase.END||!(event.player instanceof ServerPlayer player)||player.tickCount%10!=0||!SpiritRealm.inRealm(player))return;
        if(player.isCreative()||player.isSpectator())return;
        var level0=player.serverLevel();
        // the Court of Truth wants the Primordial Lord felled
        if(SageSanctum.within(player.position())){
            if(primordialFelled(player))return;
            level0.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,player.getX(),player.getY()+1,player.getZ(),20,0.4,0.8,0.4,0.08);
            down(player,level0);
            player.displayClientMessage(Component.translatable("message.primordial.sage_gate.closed").withStyle(ChatFormatting.GOLD),false);
            return;
        }
        // the temple requires only the seven sins; old terraces remain accessible
        boolean temple=within(player.position());
        if(!temple||qualified(player))return;
        var level=player.serverLevel();
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,player.getX(),player.getY()+1,player.getZ(),20,0.4,0.8,0.4,0.08);
        down(player,level);
        player.displayClientMessage(Component.translatable("message.primordial.primordial_temple.barrier",missing(player,temple)).withStyle(ChatFormatting.GOLD),false);
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {if(event.getEntity() instanceof ServerPlayer player)sync(player);}
    @SubscribeEvent public static void travel(PlayerEvent.PlayerChangedDimensionEvent event) {if(event.getEntity() instanceof ServerPlayer player)sync(player);}
    /** kept across death */
    @SubscribeEvent public static void clone(PlayerEvent.Clone event) {
        var old=persisted(event.getOriginal()).getList(DEFEATED,Tag.TAG_STRING);
        for(var entry:old){var sin=BossAffix.byId(entry.getAsString());if(sin!=null)record(event.getEntity(),sin);}
        if(persisted(event.getOriginal()).getBoolean(PRIMORDIAL))recordPrimordial(event.getEntity());
        for(var entry:persisted(event.getOriginal()).getList(GUARDIANS,Tag.TAG_STRING))for(var g:Guardian.values())if(g.id.equals(entry.getAsString()))recordGuardian(event.getEntity(),g);
    }
}
