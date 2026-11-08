package local.primordial.compat;

import com.mojang.logging.LogUtils;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.regex.Pattern;
import local.primordial.Primordial;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistry;
import net.minecraftforge.registries.RegisterEvent;
import org.slf4j.Logger;

/**
 * Worlds from when this mod was called angel_charm. Its id is now primordial; what an old world holds under the old
 * name is read as the new:
 * <ul>
 * <li>every registry entry of ours answers to its old name too (an alias);</li>
 * <li>every NBT read from disk has its old names turned new as it is read (NbtLegacyMixin): chunks with their block
 *     palettes, biomes and structures, entities and players with their items and data, level.dat, saved data;</li>
 * <li>before the levels are made, the players' advancements and stats are renamed, and the Spirit Realm's folder moves;</li>
 * <li>the old config file is copied to the new name.</li>
 * </ul>
 * The data files the mod names itself (angel_charm_*.dat) keep their names.
 */
@Mod.EventBusSubscriber(modid=Primordial.ID,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class LegacyIds {
    public static final String OLD="angel_charm";
    private static final Pattern KEY=Pattern.compile("\\.angel_charm(?=[.\"\\]])");
    private static final Logger LOGGER=LogUtils.getLogger();
    private LegacyIds() {}

    /** an old name made new: angel_charm:x is primordial:x, and so are translation keys like item.angel_charm.x */
    public static String fix(String s) {
        if(s.indexOf(OLD)<0)return s;
        if(s.equals("mod:"+OLD))return "mod:"+Primordial.ID;   // the data pack level.dat lists
        return KEY.matcher(s.replace(OLD+":",Primordial.ID+":")).replaceAll("."+Primordial.ID);
    }

    /** turn every old name in a tag new, in place */
    public static void migrate(Tag tag) {
        if(tag instanceof CompoundTag compound){
            for(var key:new ArrayList<>(compound.getAllKeys())){
                var value=compound.get(key);
                // a registry snapshot's aliases map the old names to the new on purpose: left as they are
                if(key.equals("aliases")&&value instanceof ListTag)continue;
                if(value instanceof StringTag string){var fixed=fix(string.getAsString());if(!fixed.equals(string.getAsString()))compound.putString(key,fixed);}
                else migrate(value);
                var newKey=fix(key);
                if(!newKey.equals(key)){var moved=compound.get(key);compound.remove(key);compound.put(newKey,moved);}
            }
        }else if(tag instanceof ListTag list){
            for(int i=0;i<list.size();i++){
                var value=list.get(i);
                if(value instanceof StringTag string){var fixed=fix(string.getAsString());if(!fixed.equals(string.getAsString()))list.set(i,StringTag.valueOf(fixed));}
                else migrate(value);
            }
        }
    }

    /** our entries answer to their old names: added as they are registered, so the frozen registries keep them */
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void aliases(RegisterEvent event) {
        if(!(event.getForgeRegistry() instanceof ForgeRegistry<?> registry))return;
        for(var key:new ArrayList<>(registry.getKeys()))
            if(key.getNamespace().equals(Primordial.ID))registry.addAlias(new ResourceLocation(OLD,key.getPath()),key);
    }

    /** the old config, copied to its new name once (called before the config is registered) */
    public static void copyConfig() {
        var dir=FMLPaths.CONFIGDIR.get();
        copyOnce(dir.resolve(OLD+"-common.toml"),dir.resolve(Primordial.ID+"-common.toml"));
    }

    /** before the levels are made: the Spirit Realm's folder, and the players' advancements and stats */
    public static void migrateWorld(MinecraftServer server) {
        var root=server.getWorldPath(LevelResource.ROOT);
        var oldDims=root.resolve("dimensions").resolve(OLD);var newDims=root.resolve("dimensions").resolve(Primordial.ID);
        try{
            if(Files.isDirectory(oldDims)&&!Files.exists(newDims)){Files.move(oldDims,newDims);LOGGER.info("Moved {} to {}",oldDims,newDims);}
        }catch(Exception e){LOGGER.warn("Could not move the old Spirit Realm folder",e);}
        for(var folder:new String[]{"advancements","stats"}){
            var dir=root.resolve(folder);if(!Files.isDirectory(dir))continue;
            try(var files=Files.list(dir)){
                for(Path file:(Iterable<Path>)files.filter(f->f.toString().endsWith(".json"))::iterator){
                    var text=Files.readString(file,StandardCharsets.UTF_8);
                    if(text.contains(OLD+":"))Files.writeString(file,text.replace(OLD+":",Primordial.ID+":"),StandardCharsets.UTF_8);
                }
            }catch(Exception e){LOGGER.warn("Could not rename the old ids in {}",dir,e);}
        }
    }

    private static void copyOnce(Path from,Path to) {
        try{if(Files.exists(from)&&!Files.exists(to)){Files.copy(from,to);LOGGER.info("Copied {} to {}",from,to);}}
        catch(Exception e){LOGGER.warn("Could not copy {}",from,e);}
    }
}
