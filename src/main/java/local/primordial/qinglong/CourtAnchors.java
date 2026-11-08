package local.primordial.qinglong;
import java.util.HashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
/** Permanent per-world ledger: killing/removing a boss never clears its court entry. */
public final class CourtAnchors extends SavedData {
    private final HashSet<Long> spawned=new HashSet<>();
    public static CourtAnchors get(ServerLevel l){return get(l,"mengzhang");}
    public static CourtAnchors get(ServerLevel l,String structure){return l.getDataStorage().computeIfAbsent(CourtAnchors::load,CourtAnchors::new,"angel_charm_"+structure+"_anchors");}
    public static CourtAnchors load(CompoundTag t){var d=new CourtAnchors();for(long p:t.getLongArray("Spawned"))d.spawned.add(p);return d;}
    public boolean contains(BlockPos p){return spawned.contains(p.asLong());}
    public void mark(BlockPos p){spawned.add(p.asLong());setDirty();}
    @Override public CompoundTag save(CompoundTag t){t.putLongArray("Spawned",spawned.stream().mapToLong(Long::longValue).toArray());return t;}
}
