package local.primordial.network;

import java.util.Optional;
import java.util.function.Supplier;
import local.primordial.effect.InventoryProtection;
import local.primordial.effect.Protection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class MenuDropNetwork {
    public static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(
        new ResourceLocation("primordial","menu_drop"),()->"19","19"::equals,"19"::equals);
    private MenuDropNetwork() {}
    public static void init() {
        local.primordial.guide.GuideNetwork.init();
        CHANNEL.registerMessage(0,Request.class,Request::encode,Request::decode,Request::handle,
            Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(1,TemplateRequest.class,TemplateRequest::encode,TemplateRequest::decode,TemplateRequest::handle,
            Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(3,SageCount.class,SageCount::encode,SageCount::decode,SageCount::handle,
            Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(4,GemSpectator.class,GemSpectator::encode,GemSpectator::decode,GemSpectator::handle,
            Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(5,AffixStatus.class,AffixStatus::encode,AffixStatus::decode,AffixStatus::handle,
            Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(6,AlchemyKnowledgeSync.class,AlchemyKnowledgeSync::encode,AlchemyKnowledgeSync::decode,AlchemyKnowledgeSync::handle,
            Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(7,StatusRequest.class,StatusRequest::encode,StatusRequest::decode,StatusRequest::handle,
            Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(8,StatusValues.class,StatusValues::encode,StatusValues::decode,StatusValues::handle,
            Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(9,GrowthSync.class,GrowthSync::encode,GrowthSync::decode,GrowthSync::handle,
            Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(10,StatusFlash.class,StatusFlash::encode,StatusFlash::decode,StatusFlash::handle,
            Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(11,SealSync.class,SealSync::encode,SealSync::decode,SealSync::handle,
            Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        // id 16, not 13: ids 13/14 on this shared channel belong to GuideNetwork (Snapshot/Request) — 13 used to be registered twice
        CHANNEL.registerMessage(16,GoldenBlood.class,GoldenBlood::encode,GoldenBlood::decode,GoldenBlood::handle,
            Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(12,SinsDefeated.class,SinsDefeated::encode,SinsDefeated::decode,SinsDefeated::handle,
            Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        // (13 and 14 are also taken by GuideNetwork on this channel)
        CHANNEL.registerMessage(15,OpenSpacePack.class,OpenSpacePack::encode,OpenSpacePack::decode,OpenSpacePack::handle,
            Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(17,BossBarState.class,BossBarState::encode,BossBarState::decode,BossBarState::handle,
            Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }
    /** The status page asks for the server's attribute values: many attributes (attack damage among them) are not synced. */
    public record StatusRequest() {
        static void encode(StatusRequest packet,FriendlyByteBuf buffer) {}
        static StatusRequest decode(FriendlyByteBuf buffer) {return new StatusRequest();}
        static void handle(StatusRequest packet,Supplier<NetworkEvent.Context> supplier) {
            var context=supplier.get();
            context.enqueueWork(()->{var player=context.getSender();if(player!=null)CHANNEL.send(PacketDistributor.PLAYER.with(()->player),StatusValues.of(player));});
            context.setPacketHandled(true);
        }
    }
    public static void requestStatus() {CHANNEL.sendToServer(new StatusRequest());}
    /** every attribute the player has: id, value, and whether anything changed it */
    public record StatusValues(java.util.List<StatusValuesCache.Entry> entries) {
        static StatusValues of(ServerPlayer player) {
            var list=new java.util.ArrayList<StatusValuesCache.Entry>();
            for(var attribute:net.minecraftforge.registries.ForgeRegistries.ATTRIBUTES){
                var instance=player.getAttribute(attribute);if(instance==null)continue;
                boolean changed=Math.abs(instance.getValue()-attribute.getDefaultValue())>0.00001||!instance.getModifiers().isEmpty();
                list.add(new StatusValuesCache.Entry(net.minecraftforge.registries.ForgeRegistries.ATTRIBUTES.getKey(attribute),instance.getValue(),changed,
                    instance.getBaseValue(),changed?AttributeSources.describe(player,instance):java.util.List.of()));
            }
            return new StatusValues(list);
        }
        static void encode(StatusValues packet,FriendlyByteBuf buffer) {
            buffer.writeCollection(packet.entries,(b,e)->{b.writeResourceLocation(e.id());b.writeDouble(e.value());b.writeBoolean(e.changed());b.writeDouble(e.base());
                b.writeCollection(e.sources(),(bb,s)->{bb.writeComponent(s.label());bb.writeDouble(s.amount());bb.writeByte(s.operation());});});
        }
        static StatusValues decode(FriendlyByteBuf buffer) {
            return new StatusValues(buffer.readList(b->new StatusValuesCache.Entry(b.readResourceLocation(),b.readDouble(),b.readBoolean(),b.readDouble(),
                b.readList(bb->new StatusValuesCache.Source(bb.readComponent(),bb.readDouble(),bb.readByte())))));
        }
        static void handle(StatusValues packet,Supplier<NetworkEvent.Context> supplier) {
            var context=supplier.get();context.enqueueWork(()->StatusValuesCache.update(packet.entries));
            context.setPacketHandled(true);
        }
    }
    /** The alchemy recipes this player knows, so the client (JEI) can tell what to reveal. */
    public record AlchemyKnowledgeSync(java.util.List<ResourceLocation> known) {
        static void encode(AlchemyKnowledgeSync packet,FriendlyByteBuf buffer) {buffer.writeCollection(packet.known,FriendlyByteBuf::writeResourceLocation);}
        static AlchemyKnowledgeSync decode(FriendlyByteBuf buffer) {return new AlchemyKnowledgeSync(buffer.readList(FriendlyByteBuf::readResourceLocation));}
        static void handle(AlchemyKnowledgeSync packet,Supplier<NetworkEvent.Context> supplier) {
            var context=supplier.get();context.enqueueWork(()->local.primordial.alchemy.AlchemyKnowledge.updateClient(packet.known));
            context.setPacketHandled(true);
        }
    }
    /** Which Sin Lords this player has felled, one bit each, so the Primordial Temple's braziers burn for them. */
    public record SinsDefeated(int mask) {
        static void encode(SinsDefeated packet,FriendlyByteBuf buffer) {buffer.writeVarInt(packet.mask);}
        static SinsDefeated decode(FriendlyByteBuf buffer) {return new SinsDefeated(buffer.readVarInt());}
        static void handle(SinsDefeated packet,Supplier<NetworkEvent.Context> supplier) {
            var context=supplier.get();
            context.enqueueWork(()->net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                ()->()->local.primordial.client.PrimordialTempleClient.defeated(packet.mask)));
            context.setPacketHandled(true);
        }
    }
    public static void syncSinsDefeated(ServerPlayer player,int mask) {
        if(player.connection!=null)CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new SinsDefeated(mask));
    }
    public static void syncAlchemyKnowledge(ServerPlayer player,java.util.List<ResourceLocation> known) {
        if(player.connection!=null)CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new AlchemyKnowledgeSync(known));
    }
    public record GoldenBlood(int entity,float amount) {
        static void encode(GoldenBlood packet,FriendlyByteBuf buffer){buffer.writeVarInt(packet.entity);buffer.writeFloat(packet.amount);}
        static GoldenBlood decode(FriendlyByteBuf buffer){return new GoldenBlood(buffer.readVarInt(),buffer.readFloat());}
        static void handle(GoldenBlood packet,Supplier<NetworkEvent.Context> supplier){
            var context=supplier.get();
            context.enqueueWork(()->net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                ()->()->local.primordial.client.SageHud.goldenBlood(packet.entity,packet.amount)));
            context.setPacketHandled(true);
        }
    }
    /** a boss bar's own look: which boss it is and the state it shows beside its health (SinBoss.BarLook) */
    public record BossBarState(java.util.UUID bar,String boss,int entity,int flags,int count,float meter,int accent) {
        static void encode(BossBarState p,FriendlyByteBuf b){b.writeUUID(p.bar);b.writeUtf(p.boss,64);b.writeVarInt(p.entity);b.writeVarInt(p.flags);b.writeVarInt(p.count);b.writeFloat(p.meter);b.writeInt(p.accent);}
        static BossBarState decode(FriendlyByteBuf b){return new BossBarState(b.readUUID(),b.readUtf(64),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readFloat(),b.readInt());}
        static void handle(BossBarState p,Supplier<NetworkEvent.Context> supplier){
            var context=supplier.get();
            context.enqueueWork(()->net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                ()->()->local.primordial.client.BossBars.update(p.bar,p.boss,p.entity,p.flags,p.count,p.meter,p.accent)));
            context.setPacketHandled(true);
        }
    }
    public static void sendBossBar(ServerPlayer player,java.util.UUID bar,String boss,int entity,local.primordial.boss.SinBoss.BarLook look){
        if(player.connection!=null)CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new BossBarState(bar,boss,entity,look.flags(),look.count(),look.meter(),look.accent()));
    }
    public static void syncGoldenBlood(ServerPlayer player,float amount){
        if(player.connection!=null)CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new GoldenBlood(player.getId(),amount));
    }
    public record SageCount(int count,int unlocks) {
        static void encode(SageCount packet,FriendlyByteBuf buffer) {buffer.writeVarInt(packet.count);buffer.writeByte(packet.unlocks);}
        static SageCount decode(FriendlyByteBuf buffer) {return new SageCount(buffer.readVarInt(),buffer.readUnsignedByte());}
        static void handle(SageCount packet,Supplier<NetworkEvent.Context> supplier) {
            var context=supplier.get();context.enqueueWork(()->local.primordial.sage.SageProgress.updateClientStatus(packet.count,packet.unlocks));
            context.setPacketHandled(true);
        }
    }
    public static void syncSage(ServerPlayer player,int count,int unlocks) {
        if(player.connection!=null)CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new SageCount(count,unlocks));
    }
    /** the independent statuses, the Growth accessory's stacks and countdown, and the bonds holding (a bit mask) */
    public record AffixStatus(int malice,int desaturation,int feeble,int bloodPact,int befuddled,int befuddleSeed,
        int growthAttack,int growthHealth,int growthLeft,int bonds,int unquenchedLeft,int wheelLeft) {
        static void encode(AffixStatus packet,FriendlyByteBuf buffer){buffer.writeVarInt(packet.malice);buffer.writeVarInt(packet.desaturation);buffer.writeVarInt(packet.feeble);
            buffer.writeVarInt(packet.bloodPact);buffer.writeVarInt(packet.befuddled);buffer.writeInt(packet.befuddleSeed);
            buffer.writeByte(packet.growthAttack);buffer.writeByte(packet.growthHealth);buffer.writeVarInt(packet.growthLeft);buffer.writeVarInt(packet.bonds);
            buffer.writeVarInt(packet.unquenchedLeft);buffer.writeVarInt(packet.wheelLeft);}
        static AffixStatus decode(FriendlyByteBuf buffer){return new AffixStatus(buffer.readVarInt(),buffer.readVarInt(),buffer.readVarInt(),buffer.readVarInt(),buffer.readVarInt(),buffer.readInt(),
            buffer.readUnsignedByte(),buffer.readUnsignedByte(),buffer.readVarInt(),buffer.readVarInt(),buffer.readVarInt(),buffer.readVarInt());}
        static void handle(AffixStatus packet,Supplier<NetworkEvent.Context> supplier){
            var context=supplier.get();context.enqueueWork(()->local.primordial.affix.AffixStatuses.clientSync(packet.malice,packet.desaturation,packet.feeble,
                packet.bloodPact,packet.befuddled,packet.befuddleSeed,packet.growthAttack,packet.growthHealth,packet.growthLeft,packet.bonds,
                packet.unquenchedLeft,packet.wheelLeft));
            context.setPacketHandled(true);
        }
    }
    /** the Curios screen's button: open the Space accessory's pack (the server checks it is worn) */
    public record OpenSpacePack() {
        static void encode(OpenSpacePack packet,FriendlyByteBuf buffer) {}
        static OpenSpacePack decode(FriendlyByteBuf buffer) {return new OpenSpacePack();}
        static void handle(OpenSpacePack packet,Supplier<NetworkEvent.Context> supplier) {
            var context=supplier.get();
            context.enqueueWork(()->{var player=context.getSender();if(player!=null)local.primordial.effect.SpacePack.open(player);});
            context.setPacketHandled(true);
        }
    }
    public static void openSpacePack() {CHANNEL.sendToServer(new OpenSpacePack());}
    public static void syncAffixStatuses(ServerPlayer player,AffixStatus status){
        if(player.connection!=null)CHANNEL.send(PacketDistributor.PLAYER.with(()->player),status);
    }
    /** a negative status just took hold: show it big on the player's screen */
    public record StatusFlash(int kind) {
        static void encode(StatusFlash packet,FriendlyByteBuf buffer){buffer.writeVarInt(packet.kind);}
        static StatusFlash decode(FriendlyByteBuf buffer){return new StatusFlash(buffer.readVarInt());}
        static void handle(StatusFlash packet,Supplier<NetworkEvent.Context> supplier){
            var context=supplier.get();
            context.enqueueWork(()->net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                ()->()->local.primordial.client.StatusFlashOverlay.show(packet.kind)));
            context.setPacketHandled(true);
        }
    }
    public static void statusFlash(ServerPlayer player,int kind){
        if(kind>=0&&player.connection!=null)CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new StatusFlash(kind));
    }
    /** a mob's affixes were sealed for this many ticks: its nameplate strikes them out meanwhile */
    public record SealSync(int entity,int ticks) {
        static void encode(SealSync packet,FriendlyByteBuf buffer){buffer.writeVarInt(packet.entity);buffer.writeVarInt(packet.ticks);}
        static SealSync decode(FriendlyByteBuf buffer){return new SealSync(buffer.readVarInt(),buffer.readVarInt());}
        static void handle(SealSync packet,Supplier<NetworkEvent.Context> supplier){
            var context=supplier.get();
            context.enqueueWork(()->net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                ()->()->local.primordial.client.AffixClient.sealed(packet.entity,packet.ticks)));
            context.setPacketHandled(true);
        }
    }
    public static void syncSeal(net.minecraft.world.entity.Entity entity,int ticks){
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(()->entity),new SealSync(entity.getId(),ticks));
    }
    /** a Growth mob's size, for rendering and the client's copy of its hitbox */
    public record GrowthSync(int entity,float growth) {
        static void encode(GrowthSync packet,FriendlyByteBuf buffer){buffer.writeVarInt(packet.entity);buffer.writeFloat(packet.growth);}
        static GrowthSync decode(FriendlyByteBuf buffer){return new GrowthSync(buffer.readVarInt(),buffer.readFloat());}
        static void handle(GrowthSync packet,Supplier<NetworkEvent.Context> supplier){
            var context=supplier.get();
            context.enqueueWork(()->net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                ()->()->local.primordial.affix.AffixGrowth.clientSync(packet.entity,packet.growth)));
            context.setPacketHandled(true);
        }
    }
    public static void syncGrowth(net.minecraft.world.entity.Entity entity,float growth){
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(()->entity),new GrowthSync(entity.getId(),growth));
    }
    public static void syncGrowth(ServerPlayer player,net.minecraft.world.entity.Entity entity,float growth){
        if(player.connection!=null)CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new GrowthSync(entity.getId(),growth));
    }
    public record GemSpectator() {
        static void encode(GemSpectator packet,FriendlyByteBuf buffer) {}
        static GemSpectator decode(FriendlyByteBuf buffer) { return new GemSpectator(); }
        static void handle(GemSpectator packet,Supplier<NetworkEvent.Context> supplier) {
            var context=supplier.get();
            context.enqueueWork(()->{
                ServerPlayer player=context.getSender();
                if(player!=null)local.primordial.effect.GemSwordPower.toggleSpectator(player);
            });
            context.setPacketHandled(true);
        }
    }
    public record TemplateRequest(ItemStack stack) {
        static void encode(TemplateRequest request,FriendlyByteBuf buffer) {buffer.writeItem(request.stack);}
        static TemplateRequest decode(FriendlyByteBuf buffer) {return new TemplateRequest(buffer.readItem());}
        static void handle(TemplateRequest request,Supplier<NetworkEvent.Context> supplier) {
            var context=supplier.get();
            context.enqueueWork(()->dropTemplate(context.getSender(),request));context.setPacketHandled(true);
        }
    }
    public static void dropTemplate(ServerPlayer player,TemplateRequest request) {
        var stack=request.stack;
        if(player==null||!player.isCreative()||!Protection.active(player)||player.containerMenu!=player.inventoryMenu
            ||stack.isEmpty()||stack.getCount()>Math.min(64,stack.getMaxStackSize())
            ||!stack.isItemEnabled(player.level().enabledFeatures()))return;
        InventoryProtection.userDrop(()->player.drop(stack.copy(),false,true));
    }
    public record Request(int menuId,int slot,boolean all) {
        static void encode(Request request,FriendlyByteBuf buffer) {
            buffer.writeVarInt(request.menuId);buffer.writeVarInt(request.slot);buffer.writeBoolean(request.all);
        }
        static Request decode(FriendlyByteBuf buffer) {return new Request(buffer.readVarInt(),buffer.readVarInt(),buffer.readBoolean());}
        static void handle(Request request,Supplier<NetworkEvent.Context> supplier) {
            var context=supplier.get();
            context.enqueueWork(()->drop(context.getSender(),request));context.setPacketHandled(true);
        }
    }
    public static void drop(ServerPlayer player,Request request) {
        if(player==null||!Protection.active(player))return;
        var menu=player.containerMenu;
        if(menu.containerId!=request.menuId||!menu.stillValid(player)||request.slot<0||request.slot>=menu.slots.size())return;
        var slot=menu.getSlot(request.slot);
        if(!slot.hasItem()||!slot.mayPickup(player))return;
        InventoryProtection.userDrop(()->{
            var stack=slot.safeTake(request.all?slot.getItem().getCount():1,Integer.MAX_VALUE,player);
            if(!stack.isEmpty()&&player.drop(stack,false,true)==null) {
                var remaining=slot.getItem();
                if(remaining.isEmpty())slot.set(stack);
                else if(net.minecraft.world.item.ItemStack.isSameItemSameTags(remaining,stack))remaining.grow(stack.getCount());
            }
            menu.broadcastChanges();return null;
        });
    }
}
