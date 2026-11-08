package local.primordial.affix;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.EnumSet;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="primordial")
public final class AffixCommands {
    private AffixCommands() {}
    @SubscribeEvent public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("angel_affix").requires(source->source.hasPermission(2))
            .then(Commands.literal("inspect").then(Commands.argument("entity",EntityArgument.entity()).executes(context->{
                var entity=EntityArgument.getEntity(context,"entity");
                if(entity instanceof Mob mob){var state=AffixState.get(mob);
                    context.getSource().sendSuccess(()->(state==null?Component.translatable("commands.primordial.affix.none"):Component.literal(state.tag.toString())),false);return 1;}
                context.getSource().sendFailure(Component.translatable("commands.primordial.affix.need_mob"));return 0;
            })))
            .then(Commands.literal("restore").then(Commands.argument("entity",EntityArgument.entity()).executes(context->{
                var entity=EntityArgument.getEntity(context,"entity");var source=context.getSource();
                AffixLedger.get(source.getLevel()).release(source.getServer(),entity.getUUID());
                source.sendSuccess(()->Component.translatable("commands.primordial.affix.restored"),true);return 1;
            })))
            // apply an independent status: malice and desaturation to players, feeble to any creature
            .then(Commands.literal("status").then(Commands.argument("entity",EntityArgument.entity())
                .then(Commands.literal("malice").executes(context->status(context,"malice")))
                .then(Commands.literal("desaturation").executes(context->status(context,"desaturation")))
                .then(Commands.literal("feeble").executes(context->status(context,"feeble")))
                .then(Commands.literal("blood_pact").executes(context->status(context,"blood_pact")))
                .then(Commands.literal("befuddled").executes(context->status(context,"befuddled")))))
            .then(Commands.literal("set").then(Commands.argument("entity",EntityArgument.entity()).then(Commands.argument("level",IntegerArgumentType.integer(0))
                .then(Commands.argument("affixes",StringArgumentType.word()).executes(context->set(context,null))
                    .then(Commands.argument("boss",StringArgumentType.word()).executes(context->set(context,StringArgumentType.getString(context,"boss")))))))));
    }
    private static int status(com.mojang.brigadier.context.CommandContext<net.minecraft.commands.CommandSourceStack> context,String status) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var entity=EntityArgument.getEntity(context,"entity");
        if(status.equals("feeble")&&entity instanceof net.minecraft.world.entity.LivingEntity living)AffixStatuses.applyFeeble(living);
        else if(entity instanceof net.minecraft.world.entity.player.Player player){
            switch(status){
                case "malice" -> AffixStatuses.applyMalice(player);
                case "blood_pact" -> AffixStatuses.applyBloodPact(player);
                case "befuddled" -> AffixStatuses.applyBefuddled(player);
                default -> AffixStatuses.applyDesaturation(player);
            }
        }else{context.getSource().sendFailure(Component.translatable("commands.primordial.affix.players_only"));return 0;}
        context.getSource().sendSuccess(()->Component.translatable("commands.primordial.affix.status_applied",status),true);return 1;
    }
    private static int set(com.mojang.brigadier.context.CommandContext<net.minecraft.commands.CommandSourceStack> context,String bossId) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var entity=EntityArgument.getEntity(context,"entity");if(!(entity instanceof Mob mob)){context.getSource().sendFailure(Component.translatable("commands.primordial.affix.need_mob"));return 0;}
        var affixes=EnumSet.noneOf(MobAffix.class);String value=StringArgumentType.getString(context,"affixes");
        if(!value.equals("none"))for(String id:value.split(",")){var affix=MobAffix.byId(id);if(affix==null){context.getSource().sendFailure(Component.translatable("commands.primordial.affix.unknown",id));return 0;}affixes.add(affix);}
        BossAffix boss=bossId==null||bossId.equals("none")?null:BossAffix.byId(bossId);
        if(bossId!=null&&!bossId.equals("none")&&boss==null){context.getSource().sendFailure(Component.translatable("commands.primordial.affix.unknown_boss",bossId));return 0;}
        var old=AffixState.get(mob);if(old!=null) {
            AffixLedger.get(context.getSource().getLevel()).release(context.getSource().getServer(),mob.getUUID());
            try{mob.setCustomName(Component.Serializer.fromJson(old.tag.getString("name")));}catch(Exception ignored){}
        }
        AffixState.assign(mob,IntegerArgumentType.getInteger(context,"level"),affixes,boss);
        context.getSource().sendSuccess(()->Component.translatable("commands.primordial.affix.set",IntegerArgumentType.getInteger(context,"level")),true);return 1;
    }
}
