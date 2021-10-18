package arnaria.kingdoms.services.claims;

import arnaria.kingdoms.callbacks.BlockPlaceCallback;
import arnaria.kingdoms.interfaces.PlayerEntityInf;
import arnaria.kingdoms.services.data.KingdomsData;
import arnaria.notifacaitonlib.NotificationManager;
import arnaria.notifacaitonlib.NotificationTypes;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;

public class ClaimEvents {

    public static void register() {

        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (world.getRegistryKey().equals(World.OVERWORLD)) {
                if (state.getBlock() instanceof PlayerSkullBlock) return true;
                return ClaimManager.actionAllowedAt(pos, player);
            }
            return true;
        });

        BlockPlaceCallback.EVENT.register((world, player, pos, state, block, item) -> {
            if (world.getRegistryKey().equals(World.OVERWORLD)) {
                if (!ClaimManager.actionAllowedAt(pos, player)) return false;

                if (block instanceof BannerBlock bannerBlock) {
                    String kingdomId = ((PlayerEntityInf) player).getKingdomId();
                    if (!kingdomId.isEmpty()) {
                        NbtCompound nbt = item.getNbt();
                        if (nbt != null && nbt.getBoolean("IS_CLAIM_MARKER")) {
                            if (ClaimManager.placedFirstBanner(kingdomId) && !ClaimManager.validBannerPos(kingdomId, pos)) return false;
                            if (ClaimManager.canAffordBanner(kingdomId)) ClaimManager.addClaim(kingdomId, pos, bannerBlock);
                            else NotificationManager.send(player.getUuid(), "Your kingdom does not have enough xp", NotificationTypes.ERROR);
                        }
                    }
                }
            }
            return true;
        });
    }
}
