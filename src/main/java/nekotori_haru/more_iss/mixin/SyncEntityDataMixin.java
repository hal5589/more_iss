package nekotori_haru.more_iss.mixin;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import nekotori_haru.more_iss.util.DisintegrationState;

import java.lang.reflect.Field;

@Mixin(SynchedEntityData.class)
public class SyncEntityDataMixin {

    private static EntityDataAccessor<Float> CACHED_HEALTH_ACCESSOR = null;
    private static boolean HEALTH_ACCESSOR_FETCHED = false;

    @Shadow
    private net.minecraft.world.entity.Entity entity;

    @Inject(
            method = "set(Lnet/minecraft/network/syncher/EntityDataAccessor;Ljava/lang/Object;Z)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private <T> void onSet(EntityDataAccessor<T> accessor, T value, boolean force, CallbackInfo ci) {
        if (!(this.entity instanceof LivingEntity living)) return;
        if (living.level().isClientSide()) return;

        // エフェクトではなく、DisintegrationState の内部ブラックリストを直接照合
        if (!DisintegrationState.isDisintegrating(living.getUUID())) return;

        EntityDataAccessor<Float> healthAccessor = getHealthAccessor();
        if (healthAccessor == null || !accessor.equals(healthAccessor)) return;

        if (!(value instanceof Float newHealth)) return;

        float currentHealth = living.getHealth();

        // データの変更要求が現在のHPを上回る（＝回復）なら、パケット・データ書き込みそのものをHEADで却下
        if (newHealth > currentHealth) {
            ci.cancel();
        }
    }

    @Inject(
            method = "set(Lnet/minecraft/network/syncher/EntityDataAccessor;Ljava/lang/Object;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private <T> void onSet2(EntityDataAccessor<T> accessor, T value, CallbackInfo ci) {
        onSet(accessor, value, false, ci);
    }

    private static EntityDataAccessor<Float> getHealthAccessor() {
        if (!HEALTH_ACCESSOR_FETCHED) {
            HEALTH_ACCESSOR_FETCHED = true;
            try {
                Field f;
                try {
                    f = LivingEntity.class.getDeclaredField("DATA_HEALTH_ID");
                } catch (NoSuchFieldException e) {
                    f = LivingEntity.class.getDeclaredField("f_20961_");
                }
                f.setAccessible(true);
                @SuppressWarnings("unchecked")
                EntityDataAccessor<Float> accessor = (EntityDataAccessor<Float>) f.get(null);
                CACHED_HEALTH_ACCESSOR = accessor;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return CACHED_HEALTH_ACCESSOR;
    }
}