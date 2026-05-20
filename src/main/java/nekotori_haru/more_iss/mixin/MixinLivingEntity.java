package nekotori_haru.more_iss.mixin;

import nekotori_haru.more_iss.util.DisintegrationState;
import nekotori_haru.more_iss.util.IMoreIssLivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LivingEntity.class, priority = 1000)
public abstract class MixinLivingEntity implements IMoreIssLivingEntity {

    // 🌟 解決策：通常名 "die" と製品版本名 "m_6667_" を配列で同時指定。
    // 競合MODのフライングロードによるマッピング破損を完全に無効化します。
    @Inject(
            method = {"die", "m_6667_"},
            at = @At("HEAD"),
            remap = false
    )
    private void more_iss$onDeathStart(DamageSource source, CallbackInfo ci) {
        DisintegrationState.stopDamagePhase(((LivingEntity)(Object)this).getUUID());
    }

    @Override
    public boolean more_iss$isDisintegrating() {
        return DisintegrationState.isDisintegrating(((LivingEntity)(Object)this).getUUID());
    }
}