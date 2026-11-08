package local.primordial.alchemy;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Collection;
import local.primordial.Primordial;
import local.primordial.item.AlchemyRecipeItem;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.ItemHandlerHelper;

/** /angel_alchemy give|learn|forget|list — OP tools for alchemy recipes and player knowledge. */
@Mod.EventBusSubscriber(modid=Primordial.ID)
public final class AlchemyCommands {
    private AlchemyCommands() {}
    @SubscribeEvent public static void register(RegisterCommandsEvent event) {
        var recipe=Commands.argument("recipe",ResourceLocationArgument.id())
            .suggests((c,b)->SharedSuggestionProvider.suggestResource(AlchemyKnowledge.recipes(),b));
        event.getDispatcher().register(Commands.literal("angel_alchemy").requires(s->s.hasPermission(2))
            .then(Commands.literal("give").then(Commands.argument("targets",EntityArgument.players()).then(recipe.executes(c->{
                var id=ResourceLocationArgument.getId(c,"recipe");
                for(ServerPlayer p:EntityArgument.getPlayers(c,"targets"))ItemHandlerHelper.giveItemToPlayer(p,AlchemyRecipeItem.create(id));
                return ok(c,Component.translatable("commands.primordial.alchemy.given",id.toString()));
            }))))
            .then(Commands.literal("learn").then(Commands.argument("targets",EntityArgument.players()).then(
                Commands.argument("recipe",ResourceLocationArgument.id())
                    .suggests((c,b)->SharedSuggestionProvider.suggestResource(AlchemyKnowledge.recipes(),b)).executes(c->{
                var id=ResourceLocationArgument.getId(c,"recipe");
                for(ServerPlayer p:players(c))AlchemyKnowledge.learn(p,id);
                return ok(c,Component.translatable("commands.primordial.alchemy.learned",id.toString()));
            }))))
            .then(Commands.literal("forget").then(Commands.argument("targets",EntityArgument.players())
                .then(Commands.literal("all").executes(c->{
                    for(ServerPlayer p:players(c))AlchemyKnowledge.forgetAll(p);
                    return ok(c,Component.translatable("commands.primordial.alchemy.forgot_all"));
                }))
                .then(Commands.argument("recipe",ResourceLocationArgument.id())
                    .suggests((c,b)->SharedSuggestionProvider.suggestResource(AlchemyKnowledge.recipes(),b)).executes(c->{
                    var id=ResourceLocationArgument.getId(c,"recipe");
                    for(ServerPlayer p:players(c))AlchemyKnowledge.forget(p,id);
                    return ok(c,Component.translatable("commands.primordial.alchemy.forgot",id.toString()));
                }))))
            .then(Commands.literal("list").then(Commands.argument("target",EntityArgument.player()).executes(c->{
                var p=EntityArgument.getPlayer(c,"target");
                var known=AlchemyKnowledge.known(p);
                return ok(c,Component.translatable("commands.primordial.alchemy.known",p.getScoreboardName(),known.size(),known.toString()));
            }))));
    }
    private static Collection<ServerPlayer> players(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        return EntityArgument.getPlayers(c,"targets");
    }
    private static int ok(CommandContext<CommandSourceStack> c,Component text) {
        c.getSource().sendSuccess(()->text,true);
        return 1;
    }
}
