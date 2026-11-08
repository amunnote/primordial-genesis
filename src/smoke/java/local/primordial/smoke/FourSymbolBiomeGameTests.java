package local.primordial.smoke;

import com.mojang.datafixers.util.Pair;
import java.util.List;
import local.primordial.qinglong.GuardianAnchors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** The four guardians' biomes are part of the overworld and can be found, and each guardian's court stands at its seat in its own land. */
@GameTestHolder("primordial")
@PrefixGameTestTemplate(false)
public final class FourSymbolBiomeGameTests {
    private static final List<ResourceKey<Biome>> BIOMES=List.of(local.primordial.qinglong.QinglongWorldgen.BIOME,local.primordial.baihu.BaihuWorldgen.BIOME,
        local.primordial.zhuque.ZhuqueWorldgen.BIOME,local.primordial.xuanwu.XuanwuWorldgen.BIOME);

    @GameTest(template="empty",batch="four_symbol_biomes",timeoutTicks=2400) public static void fourSymbolBiomesGenerate(GameTestHelper helper) {
        var level=helper.getLevel();
        var source=level.getChunkSource().getGenerator().getBiomeSource();
        var sampler=level.getChunkSource().randomState().sampler();
        var report=new StringBuilder("source "+source.getClass().getSimpleName());
        boolean all=true;
        for(var key:BIOMES){
            boolean possible=source.possibleBiomes().stream().anyMatch(h->h.is(key));
            Pair<BlockPos,Holder<Biome>> found=source.findClosestBiome3d(BlockPos.ZERO,6400,32,64,h->h.is(key),sampler,level);
            report.append("; ").append(key.location().getPath()).append(" possible=").append(possible).append(" at=").append(found==null?"none":found.getFirst().toShortString());
            all&=possible&&found!=null;
        }
        // how much of the land each covers, and in how many pieces: a 8192-block square sampled every 64 blocks
        int n=128,step=64;var grid=new int[n][n];
        for(int i=0;i<n;i++)for(int j=0;j<n;j++){
            var biome=source.getNoiseBiome(((i-n/2)*step)>>2,64>>2,((j-n/2)*step)>>2,sampler);
            grid[i][j]=-1;for(int b=0;b<BIOMES.size();b++)if(biome.is(BIOMES.get(b)))grid[i][j]=b;
        }
        for(int b=0;b<BIOMES.size();b++){
            int cells=0,pieces=0,largest=0;var seen=new boolean[n][n];
            for(int i=0;i<n;i++)for(int j=0;j<n;j++)if(grid[i][j]==b&&!seen[i][j]){
                pieces++;int size=0;var stack=new java.util.ArrayDeque<int[]>();stack.push(new int[]{i,j});seen[i][j]=true;
                while(!stack.isEmpty()){var c=stack.pop();size++;for(int[] d:new int[][]{{1,0},{-1,0},{0,1},{0,-1}}){int x=c[0]+d[0],z=c[1]+d[1];
                    if(x>=0&&z>=0&&x<n&&z<n&&!seen[x][z]&&grid[x][z]==b){seen[x][z]=true;stack.push(new int[]{x,z});}}}
                cells+=size;largest=Math.max(largest,size);
            }
            report.append("; ").append(BIOMES.get(b).location().getPath()).append(String.format(" cover=%.1f%% pieces=%d largest=%d blocks^2",100.0*cells/(n*n),pieces,(int)Math.sqrt(largest)*step));
        }
        // each court stands at its seat, in its own land
        var structures=level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        String[] courts={"mengzhang_court","jianbing_court","lingguang_court","zhiming_court"};
        for(int i=0;i<4;i++){
            var seat=GuardianAnchors.Seat.values()[i];var chunk=GuardianAnchors.seat(seat);
            var holder=structures.getHolder(ResourceKey.create(Registries.STRUCTURE,new ResourceLocation("primordial",courts[i])));
            var at=holder.isEmpty()?null:level.getChunkSource().getGenerator().findNearestMapStructure(level,net.minecraft.core.HolderSet.direct(holder.get()),BlockPos.ZERO,100,false);
            boolean here=at!=null&&chunk!=null&&at.getFirst().getX()>>4==chunk.x&&at.getFirst().getZ()>>4==chunk.z;
            // the biome the chunk itself took (another mod's wrapper may answer differently for a chunk not yet made)
            boolean land=chunk!=null&&level.getChunk(chunk.x,chunk.z).getNoiseBiome(chunk.getMiddleBlockX()>>2,80>>2,chunk.getMiddleBlockZ()>>2).is(seat.biome);
            report.append("; ").append(courts[i]).append(" at=").append(at==null?"none":at.getFirst().toShortString()).append(" seat=").append(here).append(" own land=").append(land);
            all&=here&&land;
        }
        // entering each land is an advancement the server has loaded (a wrong parent would drop it)
        for(var land:List.of("enter_qinglong_grove","enter_baihu_wastes","enter_zhuque_fields","enter_xuanwu_sea")){
            boolean loaded=level.getServer().getAdvancements().getAdvancement(new ResourceLocation("primordial","hunt/"+land))!=null;
            report.append("; ").append(land).append(" loaded=").append(loaded);all&=loaded;
        }
        System.out.println("[four symbol biomes] "+report);
        helper.assertTrue(all,report.toString());
        helper.succeed();
    }
}
