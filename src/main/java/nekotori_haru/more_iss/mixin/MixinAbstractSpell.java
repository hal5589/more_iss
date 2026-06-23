package nekotori_haru.more_iss.mixin;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import nekotori_haru.more_iss.registry.ModEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = AbstractSpell.class, remap = false)
public abstract class MixinAbstractSpell {

    @Inject(
            method = "attemptInitiateCast(Lnet/minecraft/world/item/ItemStack;ILnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lio/redspace/ironsspellbooks/api/spells/CastSource;ZLjava/lang/String;)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onAttemptInitiateCast(ItemStack stack, int spellLevel, Level level, Player player,
                                       CastSource castSource, boolean isClientPredicted, String slot,
                                       CallbackInfoReturnable<Boolean> cir) {
        if (ModEffects.OBLIVION != null && player.hasEffect(ModEffects.OBLIVION.get())) {
            cir.setReturnValue(false);

            // サーバー側からの呼び出しの時だけアクションバーに表示（クライアント側経由での二重表示を防ぐ）
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.displayClientMessage(
                        Component.translatable("ui.more_iss.oblivion.blocked")
                                .withStyle(ChatFormatting.DARK_PURPLE),
                        true // true = アクションバー（ホットバー上部）表示
                );
            }
        }
    }
}