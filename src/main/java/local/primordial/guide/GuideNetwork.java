package local.primordial.guide;
import java.util.*;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.*;
import local.primordial.network.MenuDropNetwork;
public final class GuideNetwork {
    public static void init(){
        // ids 13/14 on the shared MenuDropNetwork channel are GuideNetwork's alone; MenuDropNetwork's own messages use 0-12, 15, 16
        MenuDropNetwork.CHANNEL.registerMessage(13,Snapshot.class,Snapshot::encode,Snapshot::decode,Snapshot::handle,Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        MenuDropNetwork.CHANNEL.registerMessage(14,Request.class,(p,b)->b.writeVarInt(p.target()),b->new Request(b.readVarInt()),Request::handle,Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }
    public static void send(ServerPlayer p,boolean open){
        if(p.connection==null)return;   // no client to show it to (a fake player)
        var lines=new ArrayList<Component>();lines.add(NextStep.advice(p));
        for(var r:NextStep.rules(p))lines.add(Component.literal(r.done()?"✓ ":"○ ").append(r.text()));
        MenuDropNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(()->p),new Snapshot(open,lines));
    }
    public record Request(int target){static void handle(Request p,Supplier<NetworkEvent.Context> s){var c=s.get();c.enqueueWork(()->{var player=c.getSender();if(player!=null){
            if(p.target()>=0&&p.target()<NextStep.places().size()){
                var selected=NextStep.places().get(p.target());
                if(NextStep.sideQuest(selected.id())||NextStep.candidates(player).stream().anyMatch(t->t.id().equals(selected.id()))){player.getPersistentData().putString("GuideTarget",selected.id());GuideEvents.refresh(player,true);}
                else player.displayClientMessage(NextStep.t("track.unavailable"),true);
            }send(player,false);
        }});c.setPacketHandled(true);}}
    public record Snapshot(boolean open,List<Component> lines){
        static void encode(Snapshot p,FriendlyByteBuf b){b.writeBoolean(p.open);b.writeCollection(p.lines,FriendlyByteBuf::writeComponent);}
        static Snapshot decode(FriendlyByteBuf b){return new Snapshot(b.readBoolean(),b.readList(FriendlyByteBuf::readComponent));}
        static void handle(Snapshot p,Supplier<NetworkEvent.Context> s){var c=s.get();c.enqueueWork(()->net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,()->()->local.primordial.client.GuideScreen.receive(p)));c.setPacketHandled(true);}
    }
}
