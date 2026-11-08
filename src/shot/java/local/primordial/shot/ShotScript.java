package local.primordial.shot;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import local.primordial.Primordial;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Dev-only scene automation (built only with -PshotScript, never shipped). Once a world is open, it runs the lines of
 * automation/script.txt in the game directory, one per tick or after a wait, then renames the script to script.done:
 * <ul>
 * <li>{@code /command} runs a command as the player;</li>
 * <li>{@code wait N} waits N ticks;</li>
 * <li>{@code look YAW PITCH} turns the camera;</li>
 * <li>{@code hud on|off} shows or hides the HUD;</li>
 * <li>{@code shot NAME} saves the current frame to screenshots/NAME.png.</li>
 * </ul>
 * Nothing goes through the operating system's keyboard, so it works whichever window has the focus.
 */
@Mod.EventBusSubscriber(modid=Primordial.ID,value=Dist.CLIENT)
public final class ShotScript {
    private static final Deque<String> lines=new ArrayDeque<>();
    private static int wait,poll;
    private ShotScript() {}

    @SubscribeEvent public static void shotTick(TickEvent.ClientTickEvent event) {
        var mc=Minecraft.getInstance();
        if(event.phase!=TickEvent.Phase.END||mc.player==null||mc.level==null)return;
        if(lines.isEmpty()){
            if(++poll%20!=0)return;
            var script=mc.gameDirectory.toPath().resolve("automation/script.txt");
            if(!Files.exists(script))return;
            try{
                lines.addAll(Files.readAllLines(script,StandardCharsets.UTF_8));
                Files.move(script,script.resolveSibling("script.done"),java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }catch(Exception e){com.mojang.logging.LogUtils.getLogger().warn("shot script unreadable",e);}
            return;
        }
        if(wait>0){wait--;return;}
        while(!lines.isEmpty()){
            var line=lines.poll().trim();
            if(line.isEmpty()||line.startsWith("#"))continue;
            var parts=line.split("\\s+");
            switch(parts[0]){
                case "wait"->{wait=Integer.parseInt(parts[1]);return;}
                case "look"->{mc.player.setYRot(Float.parseFloat(parts[1]));mc.player.setXRot(Float.parseFloat(parts[2]));mc.player.yRotO=mc.player.getYRot();mc.player.xRotO=mc.player.getXRot();}
                case "hud"->mc.options.hideGui=parts[1].equals("off");
                case "hitboxes"->mc.getEntityRenderDispatcher().setRenderHitBoxes(parts[1].equals("on"));
                case "begin"->{
                    // begin skill N of the nearest Sin Lord, aimed at the nearest other creature (on the integrated server)
                    int index=Integer.parseInt(parts[1]);var server=mc.getSingleplayerServer();var uuid=mc.player.getUUID();
                    if(server!=null)server.execute(()->{
                        var player=server.getPlayerList().getPlayer(uuid);if(player==null)return;
                        var level=player.serverLevel();
                        var lords=level.getEntitiesOfClass(local.primordial.boss.SinBoss.class,player.getBoundingBox().inflate(48));
                        var targets=level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,player.getBoundingBox().inflate(48),
                            e->!(e instanceof local.primordial.boss.SinEntity)&&!(e instanceof net.minecraft.world.entity.player.Player));
                        if(lords.isEmpty()||targets.isEmpty())return;
                        lords.sort(java.util.Comparator.comparingDouble(player::distanceToSqr));targets.sort(java.util.Comparator.comparingDouble(player::distanceToSqr));
                        var lord=lords.get(0);if(index<lord.skills().size())lord.skills().get(index).start(lord,targets.get(0));
                    });
                }
                case "spirit"->{
                    // spirit enter | spirit potion | spirit where TAG (appends the player's place to automation/positions.txt)
                    var server=mc.getSingleplayerServer();var uuid=mc.player.getUUID();var dir=mc.gameDirectory.toPath();
                    if(server!=null)server.execute(()->{
                        var player=server.getPlayerList().getPlayer(uuid);if(player==null)return;
                        switch(parts[1]){
                            case "enter"->local.primordial.spirit.SpiritRealm.enter(player);
                            case "potion"->player.addEffect(new net.minecraft.world.effect.MobEffectInstance(local.primordial.spirit.SpiritRealm.INSPIRATION.get(),200));
                            default->{var data=local.primordial.spirit.SpiritRealm.kept(player);
                                var record=parts.length>2?parts[2]:"?";
                                record+=" "+player.level().dimension().location()+" "+String.format(java.util.Locale.ROOT,"%.1f %.1f %.1f",player.getX(),player.getY(),player.getZ())
                                    +" scattered="+data.getBoolean(local.primordial.spirit.SpiritRealm.SCATTERED)+" left="+data.getCompound(local.primordial.spirit.SpiritRealm.LEFT_AT)+"\n";
                                try{Files.writeString(dir.resolve("automation/positions.txt"),record,StandardCharsets.UTF_8,java.nio.file.StandardOpenOption.CREATE,java.nio.file.StandardOpenOption.APPEND);}catch(Exception ignored){}}
                        }
                    });
                }
                case "music"->{
                    // music TAG: which battle track is playing, appended to automation/positions.txt
                    var record=(parts.length>1?parts[1]:"?")+" "+local.primordial.client.BossMusic.describe()+"\n";
                    try{Files.writeString(mc.gameDirectory.toPath().resolve("automation/positions.txt"),record,StandardCharsets.UTF_8,java.nio.file.StandardOpenOption.CREATE,java.nio.file.StandardOpenOption.APPEND);}catch(Exception ignored){}
                }
                case "enchant"->{
                    // enchant TIER: an enchanting table opened by the player with a mystery recipe of that tier and an amethyst block in it
                    var server=mc.getSingleplayerServer();var uuid=mc.player.getUUID();int tier=parts.length>1?Integer.parseInt(parts[1]):1;
                    if(server!=null)server.execute(()->{
                        var player=server.getPlayerList().getPlayer(uuid);if(player==null)return;
                        var pos=player.blockPosition().relative(player.getDirection(),2);player.level().setBlockAndUpdate(pos,net.minecraft.world.level.block.Blocks.ENCHANTING_TABLE.defaultBlockState());
                        player.openMenu(new net.minecraft.world.SimpleMenuProvider((id,inventory,p)->new net.minecraft.world.inventory.EnchantmentMenu(id,inventory,net.minecraft.world.inventory.ContainerLevelAccess.create(player.level(),pos)),net.minecraft.network.chat.Component.translatable("container.enchant")));
                        var menu=player.containerMenu;menu.getSlot(0).set(local.primordial.item.MysteryRecipeItem.create(tier));menu.getSlot(1).set(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.AMETHYST_BLOCK,4));menu.slotsChanged(menu.getSlot(0).container);menu.broadcastChanges();
                        com.mojang.logging.LogUtils.getLogger().info("shot enchant: menu {} valid {} block {} at {} player {}",menu.getClass().getSimpleName(),menu.stillValid(player),player.level().getBlockState(pos),pos,player.blockPosition());
                    });
                }
                case "status"->mc.setScreen(new local.primordial.client.StatusScreen(null));
                case "mouse"->{
                    // mouse X Y: put the cursor at screen (GUI) coordinates, through the game window itself
                    double scale=mc.getWindow().getGuiScale();
                    org.lwjgl.glfw.GLFW.glfwSetCursorPos(mc.getWindow().getWindow(),Double.parseDouble(parts[1])*scale,Double.parseDouble(parts[2])*scale);
                }
                case "respawn"->{if(mc.player.isDeadOrDying()||mc.screen instanceof net.minecraft.client.gui.screens.DeathScreen){mc.player.respawn();mc.setScreen(null);}wait=20;return;}
                case "icons"->{   // icons SIZE ID...: each item as the inventory draws it, alone on a clear background
                    int size=Integer.parseInt(parts[1]);
                    for(int k=2;k<parts.length;k++){try{IconExport.export(mc,parts[k],size);}catch(Exception e){System.out.println("[icons] "+parts[k]+": "+e);}}
                    marker(mc.gameDirectory.toPath(),"icons_"+size);
                    return;
                }
                case "shot"->{
                    Screenshot.grab(mc.gameDirectory,parts[1]+".png",mc.getMainRenderTarget(),message->{});
                    marker(mc.gameDirectory.toPath(),parts[1]);
                    return;
                }
                default->{if(line.startsWith("/"))mc.player.connection.sendCommand(line.substring(1));return;}
            }
        }
    }
    private static void marker(Path dir,String name) {
        try{Files.writeString(dir.resolve("automation/last_shot.txt"),name,StandardCharsets.UTF_8);}catch(Exception ignored){}
    }
}
