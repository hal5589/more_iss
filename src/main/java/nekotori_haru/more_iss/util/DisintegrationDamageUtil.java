package nekotori_haru.more_iss.util;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.network.syncher.EntityDataAccessor;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * ディスインティグレーションの「真のダメージ」処理クラス。
 *
 * ── Mixin・fantasy_endingと絶対に競合しない根拠 ─────────────────
 *
 * 【根拠1】 @Mixin を一切使用しない。
 *   このクラスはバイトコードへのインジェクションを行わない。
 *   fantasy_ending / ISS / その他 Mixin mod との干渉は構造的に不可能。
 *
 * 【根拠2】 fantasy_ending の内部 API に一切依存しない。
 *   LivingEntityEC, EntityASMUtil, special_getHealth, setHealthDelta 等、
 *   fantasy_ending が提供する非公開 API には一切触れない。
 *   使用するのはバニラの LivingEntity / Entity / SynchedEntityData のみ。
 *
 * 【根拠3】 イベントリスナーを追加しない。
 *   クラス初期化（static ブロック）でリフレクションを解決するだけで、
 *   forge/fabric のイベントバスに何も登録しない。
 *
 * ── 「HP1耐え」問題の根本原因と対処 ─────────────────────────
 *
 * 原因：fantasy_ending は LivingEntity.getHealth() を ASM コアモッドで
 *       横取りし、内部の「healthDelta」値を加算して返す。
 *       また UomWither.actuallyHurt() 内では Math.min(damage, currentHP - threshold)
 *       で第2フェーズ移行前のHPをクランプする。
 *       → SynchedEntityData に 0 を書いても getHealth() がdeltaを加算して
 *         戻し、「まだ生きている」と判定されてしまう。
 *
 * 対処：SynchedEntityData への直書きに加え、kill()（SRG: m_21184_）を呼ぶ。
 *       kill() は内部で genericKill DamageSource を使って hurt(Float.MAX_VALUE)
 *       を呼ぶが、SynchedEntityData の生値が既に 0 なので、
 *       UomWither.actuallyHurt() が「currentHP=0」で計算した結果
 *       「finalHP = 0 - MAX_VALUE < 0 → 死亡フロー発火」となる。
 *       これは UomWither 専用ではなく、SynchedEntityData を信頼する
 *       すべてのエンティティに対して機能する汎用的な処理である。
 */
public class DisintegrationDamageUtil {

    // ── staticブロックで一度だけリフレクション解決 ─────────────

    /** LivingEntity.health フィールドへの VarHandle (フィールド直接読み書き)。 */
    private static VarHandle VIA_HEALTH;

    /** Entity.markHurt() への MethodHandle。 */
    private static MethodHandle VIA_MARK_HURT;

    /** LivingEntity.getHurtSound(DamageSource) への MethodHandle。 */
    private static MethodHandle VIA_GET_HURT_SOUND;

    /**
     * LivingEntity.kill() への MethodHandle（SRG: m_21184_）。
     *
     * die(DamageSource) ではなく kill() を使う理由：
     *   die() は UomWither でオーバーライドされており、
     *   finallyDeathTime / ENABLE_FINAL_SKILL チェックで除去を遅延させる。
     *   kill() も内部で die() を呼ぶが、SynchedEntityData に 0 を書いた後で
     *   呼ぶことで「HP 0 からさらにダメージ」という状態を作れる。
     *   これにより UomWither の HP クランプロジックを通り越して
     *   バニラの死亡フロー（LivingDeathEvent → loot drop → remove）が発火する。
     */
    private static MethodHandle VIA_KILL;

    /** LivingEntity.DATA_HEALTH_ID（SynchedEntityData 用キー）。 */
    @SuppressWarnings("rawtypes")
    private static EntityDataAccessor DATA_HEALTH_ID_REF;

    static {
        VIA_HEALTH = null;
        VIA_MARK_HURT = null;
        VIA_GET_HURT_SOUND = null;
        VIA_KILL = null;
        DATA_HEALTH_ID_REF = null;

        // ── LivingEntity 側のハンドルを取得 ──────────────────────
        try {
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(
                    LivingEntity.class, MethodHandles.lookup());

            // health フィールド：開発名 "health" / 難読化名 "f_20959_" を試みる
            try {
                VIA_HEALTH = lookup.findVarHandle(LivingEntity.class, "health", float.class);
            } catch (NoSuchFieldException e) {
                VIA_HEALTH = lookup.findVarHandle(LivingEntity.class, "f_20959_", float.class);
            }

            // getHurtSound: 開発名 / SRG名
            try {
                Method m = LivingEntity.class.getDeclaredMethod("getHurtSound", DamageSource.class);
                m.setAccessible(true);
                VIA_GET_HURT_SOUND = lookup.unreflect(m);
            } catch (NoSuchMethodException e) {
                Method m = LivingEntity.class.getDeclaredMethod("m_7975_", DamageSource.class);
                m.setAccessible(true);
                VIA_GET_HURT_SOUND = lookup.unreflect(m);
            }

            // kill(): 開発名 / SRG名
            try {
                Method m = LivingEntity.class.getDeclaredMethod("kill");
                m.setAccessible(true);
                VIA_KILL = lookup.unreflect(m);
            } catch (NoSuchMethodException e) {
                Method m = LivingEntity.class.getDeclaredMethod("m_21184_");
                m.setAccessible(true);
                VIA_KILL = lookup.unreflect(m);
            }

            // DATA_HEALTH_ID: 開発名 / SRG名（static フィールド）
            try {
                Field f = LivingEntity.class.getDeclaredField("DATA_HEALTH_ID");
                f.setAccessible(true);
                DATA_HEALTH_ID_REF = (EntityDataAccessor<?>) f.get(null);
            } catch (NoSuchFieldException e) {
                Field f = LivingEntity.class.getDeclaredField("f_20961_");
                f.setAccessible(true);
                DATA_HEALTH_ID_REF = (EntityDataAccessor<?>) f.get(null);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        // ── Entity 側のハンドルを取得 ──────────────────────────
        try {
            MethodHandles.Lookup lookupEntity = MethodHandles.privateLookupIn(
                    Entity.class, MethodHandles.lookup());

            try {
                Method m = Entity.class.getDeclaredMethod("markHurt");
                m.setAccessible(true);
                VIA_MARK_HURT = lookupEntity.unreflect(m);
            } catch (NoSuchMethodException e) {
                Method m = Entity.class.getDeclaredMethod("m_6021_");
                m.setAccessible(true);
                VIA_MARK_HURT = lookupEntity.unreflect(m);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ─────────────────────────────────────────────────────────────
    // メインメソッド
    // ─────────────────────────────────────────────────────────────

    /**
     * 対象エンティティに真のダメージを与える。
     *
     * 呼び出し元（DisintegrationState.onLivingTick）はシグネチャを変えていないので
     * DisintegrationState 側の変更は不要。
     *
     * @param target      ダメージを受けるエンティティ
     * @param source      ダメージソース（more_iss:disintegration）
     * @param amount      ダメージ量（0.003 + 3e-4 * elapsed) * maxHealth、最低1.0f）
     * @param bypassInvul 無敵時間を無視するか（DisintegrationState は常に true で渡す）
     * @return 処理が実行されたなら true
     */
    public static boolean dealTrueDamage(LivingEntity target, DamageSource source,
                                         float amount, boolean bypassInvul) {
        try {
            // [A] 無効エンティティは処理しない
            if (target == null || target.isRemoved()) return false;

            // [B] 無敵時間リセット（bypassInvul=true のとき）
            // LivingEntity.f_19802_ (invulnerableTime) への直接書き込み。
            // これはバニラフィールドなので fantasy_ending / ISS の Mixin と競合しない。
            if (bypassInvul) {
                target.invulnerableTime = 0;
            }

            // [C] SynchedEntityData の「生の」health 値を取得する
            // ── 重要 ──
            // fantasy_ending は getHealth() を ASM でフックしているため、
            // target.getHealth() は「生値 + delta」を返す。
            // VarHandle.get() はフィールドに直接アクセスするため、
            // ASM フックをバイパスして実際の SynchedEntityData の値を返す。
            float currentHealth;
            if (VIA_HEALTH != null) {
                currentHealth = (float) VIA_HEALTH.get(target);
            } else {
                // フォールバック（VarHandle 取得失敗時のみ）
                currentHealth = target.getHealth();
            }

            float finalHealth = currentHealth - amount;

            // [D] ダメージ音を再生（クライアントサイドはスキップ）
            playHurtSoundIfNeeded(target, source);

            if (finalHealth > 0.0f) {
                // ──────────────────────────────────────────
                // [E] 通常ダメージ（HPが残る場合）
                // ──────────────────────────────────────────
                writeHealthDirect(target, finalHealth);

                if (VIA_MARK_HURT != null) {
                    try {
                        VIA_MARK_HURT.invokeExact((Entity) target);
                    } catch (Throwable ignored) {}
                }
                if (!target.level().isClientSide()) {
                    target.level().broadcastEntityEvent(target, (byte) 2);
                }

            } else {
                // ──────────────────────────────────────────
                // [F] 致死ダメージ（HPが0以下になる場合）
                // ──────────────────────────────────────────

                // [F-1] "dismantle(boolean)" フックを試みる
                // ISS の AbstractSpell 等、一部のモブが持つ終了フックメソッド。
                // 存在すれば呼んで早期リターンする（従来の動作を維持）。
                try {
                    Method dismantle = target.getClass().getMethod("dismantle", boolean.class);
                    dismantle.setAccessible(true);
                    dismantle.invoke(target, true);
                    return true;
                } catch (NoSuchMethodException ignored) {
                    // dismantle がないモブ（UomWither を含む大半のボス）はここを通過
                }

                // [F-2] SynchedEntityData に 0 を直書きする
                // ──────────────────────────────────────────────────────
                // ここが「HP1耐え」を突破する核心。
                //
                // fantasy_ending の special_getHealth は
                //   「SynchedEntityData生値 + healthDelta」を返す。
                // healthDelta はフェーズ管理値で、通常は正の値である。
                // しかし SynchedEntityData 生値を 0 にした後で kill() を呼ぶと、
                // UomWither.actuallyHurt() は「現在HP = special_getHealth() =
                // 0 + delta」で計算するが、その後の die() 呼び出しは
                // バニラのフロー（isDead フラグ、setHealth(0)、LivingDeathEvent）
                // を起動するため、delta の加算に関わらず死亡処理が走る。
                //
                // より正確には：kill() → hurt(genericKill, MAX_VALUE) →
                // onLivingHurt でダメージ量確定 → actuallyHurt で HP 計算 →
                // この時点で SynchedEntityData 生値が 0 なので
                // special_getHealth は「0 + delta」≒ 小さい値を返す →
                // 「HP - MAX_VALUE」は確実に 0 以下 → die() 確定。
                // ──────────────────────────────────────────────────────
                writeHealthDirect(target, 0.0f);

                // [F-3] kill() で死亡フローを起動する
                // kill() はバニラの「通常死亡」ルートを辿るため、
                // LivingDeathEvent / ドロップ / アドバンスメント等が正常に発火する。
                // UomWither がどんな die() オーバーライドを持っていても、
                // HP が既に 0 なので死亡フラグは立つ。
                if (VIA_KILL != null) {
                    try {
                        VIA_KILL.invokeExact(target);
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }

                // [F-4] バニラ setHealth(0) を念のため呼ぶ
                // kill() の内部で呼ばれるはずだが、他の Mod の互換性のため明示的に呼ぶ。
                try {
                    target.setHealth(0.0f);
                } catch (Throwable ignored) {}

                // [F-5] フォールバック強制除去
                // kill() が何らかの理由で機能しなかった場合（UomWither が
                // finallyDeathTime で除去を遅延させている等）にのみ機能する。
                // 「既に removed でなければ KILLED で除去」という保険。
                if (!target.level().isClientSide() && !target.isRemoved()) {
                    target.remove(Entity.RemovalReason.KILLED);
                }
            }

            return true;

        } catch (Throwable t) {
            t.printStackTrace();
            return false;
        }
    }

    // ─────────────────────────────────────────────────────────────
    // プライベートユーティリティ
    // ─────────────────────────────────────────────────────────────

    /**
     * SynchedEntityData と VarHandle の両方を通じて health を書き込む。
     *
     * fantasy_ending は SynchedEntityData.set() を Mixin でフックしている可能性があるが、
     * VarHandle（フィールド直接書き込み）はそのフックをバイパスする。
     * 両方書くことで「フィールドの実値」と「ネットワーク同期値」を揃える。
     */
    @SuppressWarnings("unchecked")
    private static void writeHealthDirect(LivingEntity target, float value) {
        // VarHandle でフィールドに直書き（最優先。Mixin のフックを通らない）
        if (VIA_HEALTH != null) {
            try {
                VIA_HEALTH.set(target, value);
            } catch (Throwable ignored) {}
        }

        // SynchedEntityData にも書く（クライアント同期 / 他Modの読み取り互換）
        if (DATA_HEALTH_ID_REF != null) {
            try {
                target.getEntityData().set(DATA_HEALTH_ID_REF, value);
            } catch (Throwable ignored) {}
        }

        // バニラ setHealth も呼ぶ（他Mod互換性のため。失敗しても続行）
        try {
            target.setHealth(value);
        } catch (Throwable ignored) {}
    }

    /** ダメージ音を再生する（サーバーサイドのみ、失敗は無視）。 */
    private static void playHurtSoundIfNeeded(LivingEntity target, DamageSource source) {
        if (VIA_GET_HURT_SOUND == null) return;
        if (target.level().isClientSide()) return;

        try {
            SoundEvent hurtSound = (SoundEvent) VIA_GET_HURT_SOUND.invokeExact(target, source);
            if (hurtSound != null) {
                target.level().playSound(
                        null,
                        target.getX(), target.getY(), target.getZ(),
                        hurtSound, SoundSource.HOSTILE,
                        1.0f, target.getVoicePitch()
                );
            }
        } catch (Throwable ignored) {}
    }
}