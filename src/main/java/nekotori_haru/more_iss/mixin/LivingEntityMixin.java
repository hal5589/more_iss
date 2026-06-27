package nekotori_haru.more_iss.mixin;

import nekotori_haru.more_iss.registry.MoreIssConfig;
import nekotori_haru.more_iss.entity.EternalWizardEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Unique
    private float getDamageCap() {
        return MoreIssConfig.getDamageCap();
    }

    @Unique
    private boolean isDamageCapEnabled() {
        return MoreIssConfig.isDamageCapEnabled();
    }

    @Inject(
            method = "hurt",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onHurt(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;

        // EternalWizardEntity のみ対象
        if (!(self instanceof EternalWizardEntity)) {
            return;
        }

        // キャップが無効な場合は何もしない
        if (!isDamageCapEnabled()) {
            return;
        }

        float cap = getDamageCap();

        // ダメージがキャップ値を超えている場合のみ制限
        if (amount > cap) {
            // ⭐ デバッグログ（必要に応じて削除）
            // System.out.println("[EternalWizard] Damage capped: " + amount + " → " + cap);
            cir.setReturnValue(self.hurt(source, cap));
        }
    }
}