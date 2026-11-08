package local.primordial.alchemy;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import local.primordial.Primordial;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Per-world record of every player who has joined this save and which alchemy recipes each of them knows.
 * Lets loot generation tell when a recipe is known by everyone in the game right now (the players online), so it
 * stops appearing; it comes back as soon as someone who has not learned it joins.
 */
@Mod.EventBusSubscriber(modid=Primordial.ID)
public final class AlchemyWorldKnowledge extends SavedData {
    private static final String NAME="angel_charm_alchemy_knowledge";
    private final Map<UUID,Set<ResourceLocation>> players=new HashMap<>();
    /** who counts as in the game besides the server's online players (members marked present by hand, e.g. in tests); not saved */
    private final Set<UUID> present=new HashSet<>();
    private MinecraftServer server;

    public static AlchemyWorldKnowledge get(MinecraftServer server) {
        var data=server.overworld().getDataStorage().computeIfAbsent(AlchemyWorldKnowledge::load,AlchemyWorldKnowledge::new,NAME);
        data.server=server;
        return data;
    }
    /** Mirror a player's current knowledge (also registers them as a member of this world). */
    public static void record(ServerPlayer player) {
        get(player.server).record(player.getUUID(),AlchemyKnowledge.known(player));
    }
    public void record(UUID player,java.util.Collection<ResourceLocation> known) {
        players.put(player,new HashSet<>(known));
        setDirty();
    }
    public void markPresent(UUID player) {present.add(player);}
    public void markAbsent(UUID player) {present.remove(player);}
    /** True when someone is in the game and every one of them (the online players) knows the recipe. */
    public boolean everyoneKnows(ResourceLocation recipe) {
        boolean anyone=false;
        if(server!=null)for(ServerPlayer player:server.getPlayerList().getPlayers()){
            anyone=true;if(!AlchemyKnowledge.knows(player,recipe))return false;}
        for(UUID id:present){
            if(server!=null&&server.getPlayerList().getPlayer(id)!=null)continue;
            anyone=true;var known=players.get(id);if(known==null||!known.contains(recipe))return false;}
        return anyone;
    }

    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if(event.getEntity() instanceof ServerPlayer player){record(player);AlchemyKnowledge.sync(player);AlchemyKnowledge.checkTruthEpiphany(player);}
    }

    private static AlchemyWorldKnowledge load(CompoundTag tag) {
        var data=new AlchemyWorldKnowledge();
        CompoundTag all=tag.getCompound("players");
        for(String key:all.getAllKeys()){
            Set<ResourceLocation> known=new HashSet<>();
            ListTag list=all.getList(key,Tag.TAG_STRING);
            for(int i=0;i<list.size();i++){var id=ResourceLocation.tryParse(list.getString(i));if(id!=null)known.add(id);}
            try{data.players.put(UUID.fromString(key),known);}catch(IllegalArgumentException ignored){}
        }
        return data;
    }
    @Override public CompoundTag save(CompoundTag tag) {
        CompoundTag all=new CompoundTag();
        players.forEach((uuid,known)->{
            ListTag list=new ListTag();
            for(ResourceLocation id:known)list.add(StringTag.valueOf(id.toString()));
            all.put(uuid.toString(),list);
        });
        tag.put("players",all);
        return tag;
    }
}
