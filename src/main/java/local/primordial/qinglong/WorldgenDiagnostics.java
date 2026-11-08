package local.primordial.qinglong;

import java.util.List;
import local.primordial.Primordial;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * /angel_biomes: why the Four Symbols' biomes can or cannot be found in this world. It names the overworld's biome
 * source, whether the four biomes can generate at all, and the nearest of
 * each to the one who asks, and the seats of the four courts; "seat <guardian>" and "land <guardian>" go there. For
 * players testing without access to the log.
 */
@Mod.EventBusSubscriber(modid=Primordial.ID)
public final class WorldgenDiagnostics {
    /** bumped whenever the biome placement changes, so a screenshot tells which build is running */
    public static final String PLACEMENT="v4 (guardian lands, no TerraBlender)";
    private static final List<ResourceKey<Biome>> BIOMES=List.of(QinglongWorldgen.BIOME,local.primordial.baihu.BaihuWorldgen.BIOME,
        local.primordial.zhuque.ZhuqueWorldgen.BIOME,local.primordial.xuanwu.XuanwuWorldgen.BIOME);
    private WorldgenDiagnostics() {}

    @SubscribeEvent public static void register(RegisterCommandsEvent event) {
        var command=Commands.literal("angel_biomes").requires(source->source.hasPermission(2)).executes(context->report(context.getSource()));
        // /angel_biomes seat <guardian>: to the court; /angel_biomes land <guardian>: to the nearest of its land
        for(var seat:GuardianAnchors.Seat.values()){String name=seat.name().toLowerCase(java.util.Locale.ROOT);
            command.then(Commands.literal("seat").then(Commands.literal(name).executes(context->toSeat(context.getSource(),seat))));
            command.then(Commands.literal("land").then(Commands.literal(name).executes(context->toLand(context.getSource(),seat))));}
        event.getDispatcher().register(command);
    }

    private static int report(CommandSourceStack source) {
        var level=source.getServer().overworld();
        var generator=level.getChunkSource().getGenerator();var biomes=generator.getBiomeSource();
        say(source,"[四象群系诊断] 放置规则 "+PLACEMENT,ChatFormatting.GOLD);
        say(source,"主世界生成器 "+generator.getClass().getSimpleName()+"，群系源 "+biomes.getClass().getSimpleName(),ChatFormatting.GRAY);
        for(var seat:GuardianAnchors.Seat.values()){var at=GuardianAnchors.seat(seat);
            say(source,"首领场地 "+seat.name().toLowerCase(java.util.Locale.ROOT)+"："+(at==null?"没有（主世界不是多噪声地形）":at.getMiddleBlockX()+" ~ "+at.getMiddleBlockZ()),at==null?ChatFormatting.RED:ChatFormatting.AQUA);}
        var origin=BlockPos.containing(source.getPosition());
        for(var key:BIOMES){
            String name=Component.translatable("biome."+key.location().getNamespace()+"."+key.location().getPath()).getString();
            if(biomes.possibleBiomes().stream().noneMatch(h->h.is(key))){
                say(source,name+"：这个世界的群系源里没有它（主世界地形被其他 mod 或数据包换成了非多噪声的群系源）",ChatFormatting.RED);continue;
            }
            var found=biomes.findClosestBiome3d(origin,6400,32,64,h->h.is(key),level.getChunkSource().randomState().sampler(),level);
            if(found==null)say(source,name+"：可以生成，但 6400 格内没有",ChatFormatting.YELLOW);
            else{var at=found.getFirst();say(source,name+"：最近在 "+at.getX()+" "+at.getY()+" "+at.getZ()+"（"+(int)Math.sqrt(at.distSqr(origin))+" 格）",ChatFormatting.GREEN);}
        }
        return 1;
    }
    private static int toSeat(CommandSourceStack source,GuardianAnchors.Seat seat) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var at=GuardianAnchors.seat(seat);if(at==null){say(source,"这个世界没有首领场地",ChatFormatting.RED);return 0;}
        var player=source.getPlayerOrException();var level=source.getServer().overworld();
        int x=at.getMiddleBlockX(),z=at.getMiddleBlockZ();level.getChunk(x>>4,z>>4);
        player.teleportTo(level,x+0.5,level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,x,z)+12,z+0.5,player.getYRot(),player.getXRot());
        return 1;
    }
    private static int toLand(CommandSourceStack source,GuardianAnchors.Seat seat) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var player=source.getPlayerOrException();var level=source.getServer().overworld();
        var found=level.getChunkSource().getGenerator().getBiomeSource().findClosestBiome3d(player.blockPosition(),6400,32,64,h->h.is(seat.biome),level.getChunkSource().randomState().sampler(),level);
        if(found==null){say(source,"6400 格内没有",ChatFormatting.YELLOW);return 0;}
        int x=found.getFirst().getX(),z=found.getFirst().getZ();level.getChunk(x>>4,z>>4);
        player.teleportTo(level,x+0.5,level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,x,z)+12,z+0.5,player.getYRot(),player.getXRot());
        return 1;
    }
    private static void say(CommandSourceStack source,String text,ChatFormatting colour) {source.sendSuccess(()->Component.literal(text).withStyle(colour),false);}
}
