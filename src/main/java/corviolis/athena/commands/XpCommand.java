package corviolis.athena.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import corviolis.athena.services.data.KingdomsData;
import corviolis.athena.services.procedures.KingdomProcedures;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;

public class XpCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("xp")
                .then(CommandManager.literal("add")
                        .then(CommandManager.argument("team", StringArgumentType.string())
                            .then(CommandManager.argument("amount", IntegerArgumentType.integer())
                                    .executes(context -> add(context, StringArgumentType.getString(context, "team"), IntegerArgumentType.getInteger(context, "amount")))
                            )
                    )
                )

                .then(CommandManager.literal("view")
                        .then(CommandManager.argument("team", StringArgumentType.string())
                                .executes(context -> view(context, StringArgumentType.getString(context, "team")))
                        )
                )
        );
    }

    private static int view(CommandContext<ServerCommandSource> context, String kingdomId) {
        ServerCommandSource source = context.getSource();
        source.sendFeedback(new LiteralText(kingdomId + " has " + KingdomsData.getXp(kingdomId) + " xp"), false);
        return 1;
    }

    private static int add(CommandContext<ServerCommandSource> context, String kingdomId, int amount) {
        ServerCommandSource source = context.getSource();
        KingdomProcedures.addXp(kingdomId, amount);
        source.sendFeedback(new LiteralText("Added " + amount + " xp to " + kingdomId), false);
        return 1;
    }
}
