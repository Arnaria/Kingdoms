package arnaria.kingdoms.services.events;

import arnaria.kingdoms.callbacks.PlayerDeathCallback;
import arnaria.kingdoms.services.data.KingdomsData;
import arnaria.kingdoms.services.procedures.KingdomProcedures;
import arnaria.notifacaitonlib.NotificationManager;
import arnaria.notifacaitonlib.NotificationTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;

import java.util.UUID;

import static arnaria.kingdoms.Kingdoms.playerManager;
import static arnaria.kingdoms.Kingdoms.scoreboard;
import static arnaria.kingdoms.Kingdoms.settings;

public class RevolutionEvent extends Event {

    private final Team kingdomTeam;

    public RevolutionEvent(String kingdomId) {
        super(settings.REVOLUTION_DURATION, "REVOLUTION");

        this.kingdomTeam = scoreboard.getTeam(kingdomId);
        if (this.kingdomTeam != null) this.kingdomTeam.setFriendlyFireAllowed(true);

        for (UUID member : KingdomsData.getMembers(kingdomId)) {
            ServerPlayerEntity player = playerManager.getPlayer(member);
            if (player != null) this.addPlayer(player);
        }

        PlayerDeathCallback.EVENT.register((player, damageSource) -> {
            if (player.getUuid().equals(KingdomsData.getKing(kingdomId))) {
                Entity source = damageSource.getSource();

                if (source instanceof PlayerEntity) {
                    KingdomProcedures.updateKing(kingdomId, source.getUuid());

                    for (UUID member : KingdomsData.getMembers(kingdomId)) {
                        NotificationManager.send(member, source.getName() + " is the new king of " + kingdomId + "!", NotificationTypes.ACHIEVEMENT);
                    }
                    this.stopEvent();
                }
            }
            return ActionResult.PASS;
        });
    }

    @Override
    public void finish() {
        if (this.kingdomTeam != null) this.kingdomTeam.setFriendlyFireAllowed(false);

        for (PlayerEntity member : this.getMembers()) {
            NotificationManager.send(member.getUuid(), "The revolution has ended and the king remains in power", NotificationTypes.WARN);
        }
    }
}
