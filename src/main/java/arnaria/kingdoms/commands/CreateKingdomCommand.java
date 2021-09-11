package arnaria.kingdoms.commands;

import arnaria.kingdoms.util.procedures.KingdomProcedures;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

public class CreateKingdomCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("create")
                .then(CommandManager.argument("Name", StringArgumentType.string())
                        .executes(context -> createNewKingdom(context, "Name"))));
    }

    private static int createNewKingdom(CommandContext<ServerCommandSource> context, String name) throws CommandSyntaxException {
        KingdomProcedures.createKingdom(name, context.getSource().getPlayer().getUuid());
        return 1;
    }
}