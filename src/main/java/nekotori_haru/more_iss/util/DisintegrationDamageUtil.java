package nekotori_haru.more_iss.util;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class DisintegrationDamageUtil {

    private static VarHandle VIA_HEALTH = null;
    private static MethodHandle VIA_MARK_HURT = null;
    private static MethodHandle VIA_GET_HURT_SOUND = null;
    private static MethodHandle VIA_DIE = null;
    private static EntityDataAccessor<Float> DATA_HEALTH_ID_REF = null;

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(LivingEntity.class, MethodHandles.lookup());

            try {
                VIA_HEALTH = lookup.findVarHandle(LivingEntity.class, "health", float.class);
            } catch (NoSuchFieldException e) {
                try {
                    VIA_HEALTH = lookup.findVarHandle(LivingEntity.class, "f_20959_", float.class);
                } catch (Exception ignored) {}
            }

            try {
                Method m = LivingEntity.class.getDeclaredMethod("getHurtSound", DamageSource.class);
                m.setAccessible(true);
                VIA_GET_HURT_SOUND = lookup.unreflect(m);
            } catch (NoSuchMethodException e) {
                try {
                    Method m = LivingEntity.class.getDeclaredMethod("m_7975_", DamageSource.class);
                    m.setAccessible(true);
                    VIA_GET_HURT_SOUND = lookup.unreflect(m);
                } catch (Exception ignored) {}
            }

            try {
                Method m = LivingEntity.class.getDeclaredMethod("die", DamageSource.class);
                m.setAccessible(true);
                VIA_DIE = lookup.unreflect(m);
            } catch (NoSuchMethodException e) {
                try {
                    Method m = LivingEntity.class.getDeclaredMethod("m_6668_", DamageSource.class);
                    m.setAccessible(true);
                    VIA_DIE = lookup.unreflect(m);
                } catch (Exception ignored) {}
            }

            try {
                Field f = LivingEntity.class.getDeclaredField("DATA_HEALTH_ID");
                f.setAccessible(true);
                DATA_HEALTH_ID_REF = (EntityDataAccessor<Float>) f.get(null);
            } catch (NoSuchFieldException e) {
                try {
                    Field f = LivingEntity.class.getDeclaredField("f_20961_");
                    f.setAccessible(true);
                    DATA_HEALTH_ID_REF = (EntityDataAccessor<Float>) f.get(null);
                } catch (Exception ignored) {}
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            MethodHandles.Lookup lookupEntity = MethodHandles.privateLookupIn(Entity.class, MethodHandles.lookup());
            try {
                Method m = Entity.class.getDeclaredMethod("markHurt");
                m.setAccessible(true);
                VIA_MARK_HURT = lookupEntity.unreflect(m);
            } catch (NoSuchMethodException e) {
                try {
                    Method m = Entity.class.getDeclaredMethod("m_6021_");
                    m.setAccessible(true);
                    VIA_MARK_HURT = lookupEntity.unreflect(m);
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean dealTrueDamage(LivingEntity target, DamageSource source, float amount, boolean bypassInvul) {
        if (target == null || target.isDeadOrDying() || target.isRemoved()) return false;

        try {
            if (bypassInvul) {
                target.invulnerableTime = 0;
            }

            float currentHealth = target.getHealth();
            if (VIA_HEALTH != null) {
                currentHealth = (float) VIA_HEALTH.get(target);
            }

            float finalHealth = currentHealth - amount;

            // 被弾ボイスの強制再生
            if (VIA_GET_HURT_SOUND != null && !target.level().isClientSide()) {
                SoundEvent hurtSound = (SoundEvent) VIA_GET_HURT_SOUND.invokeExact(target, source);
                if (hurtSound != null) {
                    target.level().playSound(
                            null,
                            target.getX(), target.getY(), target.getZ(),
                            hurtSound,
                            SoundSource.HOSTILE,
                            1.0F,
                            target.getVoicePitch()
                    );
                }
            }

            if (finalHealth <= 0f) {
                // ターゲットダミー専用の「dismantle」引きずり出し
                try {
                    Method dismantleMethod = target.getClass().getMethod("dismantle", boolean.class);
                    if (dismantleMethod != null) {
                        dismantleMethod.setAccessible(true);
                        dismantleMethod.invoke(target, true);
                        return true;
                    }
                } catch (NoSuchMethodException ignored) {}

                // 通常モブ用の正規・強制死亡ルート
                if (VIA_DIE != null) {
                    VIA_DIE.invokeExact(target, source);
                } else {
                    target.die(source);
                }

                if (!target.level().isClientSide()) {
                    target.remove(Entity.RemovalReason.KILLED);
                }
                target.setHealth(0f);
            } else {
                // HPフィールドの強制書き換え
                if (VIA_HEALTH != null) {
                    VIA_HEALTH.set(target, finalHealth);
                } else {
                    target.setHealth(finalHealth);
                }

                // クライアントデータへの同期
                if (DATA_HEALTH_ID_REF != null) {
                    target.getEntityData().set(DATA_HEALTH_ID_REF, finalHealth);
                }

                if (VIA_MARK_HURT != null) {
                    VIA_MARK_HURT.invokeExact((Entity) target);
                }

                // ── 【新規追加】被弾赤点滅アニメーションパケットの強制手動発行 ──
                // マイクラ内部イベントID「2」は「LivingEntityの被弾（赤点滅・ノックバック・のけぞり計算）」を司ります。
                // これにより、hurt()メソッドを経由しなくてもクライアント画面上でガタガタと激しくリアクションするようになります。
                if (!target.level().isClientSide()) {
                    target.level().broadcastEntityEvent(target, (byte) 2);
                }
            }

            return true;
        } catch (Throwable t) {
            t.printStackTrace();
            return false;
        }
    }
}