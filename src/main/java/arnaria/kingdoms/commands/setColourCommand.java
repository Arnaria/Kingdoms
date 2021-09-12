package arnaria.kingdoms.commands;

import arnaria.kingdoms.interfaces.PlayerEntityInf;
import arnaria.kingdoms.services.procedures.KingdomProcedures;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.ColorArgumentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.Formatting;

public class setColourCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("colour")
                .then(CommandManager.argument("Colour", ColorArgumentType.color())
                        .executes(context -> setKingdomColour(context, ColorArgumentType.getColor(context, "Colour")))));
    }

    public static int setKingdomColour(CommandContext<ServerCommandSource> context, Formatting colour) throws CommandSyntaxException {
        PlayerEntity player = context.getSource().getPlayer();
        KingdomProcedures.setColor(((PlayerEntityInf) player).getKingdomId(), colour);
        return 1;
    }
}
