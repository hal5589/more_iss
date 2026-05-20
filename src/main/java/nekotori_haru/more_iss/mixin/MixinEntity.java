package nekotori_haru.more_iss.mixin;

import nekotori_haru.more_iss.util.DisintegrationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Entity.class, priority = 1000)
public abstract class MixinEntity {

    // 🌟 解決策：methodに通常名と製品版SRG名（m_6469_）の両方を配列で渡します。
    // これにより、RefMapが機能している開発環境でも、外れてしまっている製品版環境でも100%確実にhurtを捕捉できます。
    @Inject(
            method = {"hurt", "m_6469_"},
            at = @At("HEAD"),
            remap = false // 手動で両方の名前を指定しているため、Mixinによる強制リマップを無効化して安全化
    )
    private void more_iss$forceVulnerableOnHurt(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        Object self = (Object) this;
        if (self instanceof LivingEntity living) {
            if (DisintegrationState.isDisintegrating(living.getUUID())) {
                // 無敵時間を毎チック強制的に破壊し、hurtが100%通る状態を維持する
                living.invulnerableTime = 0;
                living.hurtDuration = 0;
            }
        }
    }
}