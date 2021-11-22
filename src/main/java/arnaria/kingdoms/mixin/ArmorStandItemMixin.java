package arnaria.kingdoms.mixin;

import arnaria.kingdoms.services.claims.NewClaimManager;
import net.minecraft.item.ArmorStandItem;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.ActionResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ArmorStandItem.class)
public class ArmorStandItemMixin {

    @Inject(method = "useOnBlock", at = @At("HEAD"), cancellable = true)
    public void canPlaceOn(ItemUsageContext context, CallbackInfoReturnable<ActionResult> cir) {
        if (!NewClaimManager.actionAllowedAt(context.getBlockPos(), context.getPlayer())) {
            cir.setReturnValue(ActionResult.FAIL);
        }
    }
}
