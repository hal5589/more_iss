package nekotori_haru.more_iss.util;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.EventPriority;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Disintegration 詠唱中ターゲット拘束マネージャー。
 * AT（AccessTransformer）の認識エラーを完全に回避するため、VarHandleで被弾フラグを制御します。
 */
public class DisintegrationTargetManager {

    private static final Map<UUID, LockEntry> LOCKED = new ConcurrentHashMap<>();
    private static boolean initialized = false;

    // hurtMarked フィールドに直接アクセスするための VarHandle
    private static VarHandle VIA_HURT_MARKED = null;

    static {
        try {
            // Entity クラスのプライベート検索権限を取得
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(Entity.class, MethodHandles.lookup());

            // Mojang名（hurtMarked）で検索、ダメならSRG名（f_19796_）でフォールバック
            try {
                VIA_HURT_MARKED = lookup.findVarHandle(Entity.class, "hurtMarked", boolean.class);
            } catch (NoSuchFieldException e) {
                try {
                    VIA_HURT_MARKED = lookup.findVarHandle(Entity.class, "f_19796_", boolean.class);
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static synchronized void init() {
        if (initialized) return;
        MinecraftForge.EVENT_BUS.addListener(
                EventPriority.HIGHEST,
                DisintegrationTargetManager::onLivingTick
        );
        initialized = true;
    }

    public static void lock(UUID targetUUID, Entity target) {
        if (target == null) return;
        LOCKED.put(targetUUID, new LockEntry(target.position()));
        if (target instanceof Mob mob) {
            mob.setNoAi(true);
        }
    }

    public static void release(UUID targetUUID) {
        LOCKED.remove(targetUUID);
    }

    public static boolean isLocked(UUID targetUUID) {
        return LOCKED.containsKey(targetUUID);
    }

    private static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) return;

        LockEntry entry = LOCKED.get(entity.getUUID());
        if (entry == null) return;

        // 死亡・削除されたら解除
        if (entity.isRemoved() || entity.isDeadOrDying()) {
            release(entity.getUUID());
            return;
        }

        // ① 速度をゼロに
        entity.setDeltaMovement(Vec3.ZERO);
        entity.fallDistance = 0f;

        // ② 座標ずれを検知して復元 (テレポート対策)
        Vec3 locked = entry.lockedPos;
        if (entity.position().distanceToSqr(locked) > 0.01) {
            entity.teleportTo(locked.x, locked.y, locked.z);
            entity.setDeltaMovement(Vec3.ZERO);

            // ③ プレイヤーならクライアントにも強制送信
            if (entity instanceof ServerPlayer sp) {
                sp.connection.teleport(
                        locked.x, locked.y, locked.z,
                        sp.getYRot(), sp.getXRot()
                );
            }
        }

        // ④ ノックバックフラグ（hurtMarked）をコンパイルチェックをバイパスして安全に消去
        if (VIA_HURT_MARKED != null) {
            VIA_HURT_MARKED.set((Entity) entity, false);
        }
    }

    private static class LockEntry {
        final Vec3 lockedPos;
        LockEntry(Vec3 lockedPos) { this.lockedPos = lockedPos; }
    }
}