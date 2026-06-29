package nekotori_haru.more_iss.mixin;

import nekotori_haru.more_iss.entity.EternalWizardEntity;
import nekotori_haru.more_iss.registry.MoreIssConfig;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.lang.reflect.Field;

/**
 * 【三段防御の4段目・根本対策】
 *
 * fantasy_ending の EntityASMUtil は ASM でバイトコードを直接書き換え、
 * LivingEntity#getHealth() の戻り値に「独自のデルタ値」を加算する形に
 * 改造している。このデルタは SynchedEntityData の専用フィールド
 * (EntityASMUtil.FE_GET_HEALTH_DATA) に格納され、addDelta(...) 経由で
 * SynchedEntityData#set(accessor, value) によって直接書き込まれる。
 *
 * これは vanilla の LivingEntity#setHealth(float) を一切経由しないため、
 * setHealth へのMixin(三段防御の3段目)では検出も制限もできない。
 *
 * 対策として、SynchedEntityData#set(EntityDataAccessor, Object) 自体を
 * フックし、書き込み先のアクセサが FE_GET_HEALTH_DATA であり、かつ
 * 対象 Entity が EternalWizardEntity の場合のみ、書き込まれる値(デルタ)が
 * 「現在のデルタからどれだけ減るか」を計算し、cap を超える減少を防ぐ。
 *
 * 注意:
 *  - fantasy_ending がロードされていない環境でもクラッシュしないよう、
 *    リフレクション解決は失敗を許容し、解決できない場合は何もしない
 *    (素通りさせる = フェイルセーフ)。
 *  - FE_GET_HEALTH_DATA は static フィールドだが実行時に動的代入される
 *    ため、毎回 getstatic 相当のリフレクション取得を行う
 *    (キャッシュは安全性を優先し、ここでは行わない)。
 */
// 注意: alltheleaks 等の互換性Modも SynchedEntityData に対して
// 同名クラス "SynchedEntityDataMixin" を別パッケージで適用しているが、
// パッケージが異なるため衝突しない。Mixin優先度はデフォルト(1000)のまま。
@Mixin(SynchedEntityData.class)
public abstract class SynchedEntityDataMixin {

    @Unique
    private static Class<?> more_iss$entityASMUtilClass;

    @Unique
    private static Field more_iss$feGetHealthDataField;

    @Unique
    private static boolean more_iss$reflectionInitAttempted = false;

    @Unique
    private static void more_iss$initReflection() {
        if (more_iss$reflectionInitAttempted) {
            return;
        }
        more_iss$reflectionInitAttempted = true;
        try {
            more_iss$entityASMUtilClass = Class.forName("com.mega.uom.util.entity.EntityASMUtil");
            more_iss$feGetHealthDataField = more_iss$entityASMUtilClass.getField("FE_GET_HEALTH_DATA");
        } catch (Throwable t) {
            // fantasy_ending が無い、もしくは内部クラス名が変わった場合は
            // 何もせず素通りさせる(フェイルセーフ)。
            more_iss$entityASMUtilClass = null;
            more_iss$feGetHealthDataField = null;
        }
    }

    @Unique
    private static EntityDataAccessor<?> more_iss$getFeHealthDataAccessor() {
        more_iss$initReflection();
        if (more_iss$feGetHealthDataField == null) {
            return null;
        }
        try {
            return (EntityDataAccessor<?>) more_iss$feGetHealthDataField.get(null);
        } catch (Throwable t) {
            return null;
        }
    }

    @ModifyVariable(
            method = "set",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0 // set(EntityDataAccessor<T> accessor, T value) の Object型引数(value)で最初に出てくるもの
    )
    private Object more_iss$capHealthDeltaWrite(Object value, EntityDataAccessor<?> accessor) {
        // FE_GET_HEALTH_DATA 以外への書き込みは無関係なので即リターン
        EntityDataAccessor<?> feHealthAccessor = more_iss$getFeHealthDataAccessor();
        if (feHealthAccessor == null || accessor != feHealthAccessor) {
            return value;
        }

        // value は Float (デルタの新しい値) のはず
        if (!(value instanceof Float)) {
            return value;
        }

        SynchedEntityData self = (SynchedEntityData) (Object) this;
        Entity entity;
        try {
            entity = ((SynchedEntityDataAccessor) self).more_iss$getEntity();
        } catch (Throwable t) {
            return value;
        }

        if (!(entity instanceof EternalWizardEntity)) {
            return value;
        }

        if (!MoreIssConfig.isDamageCapEnabled()) {
            return value;
        }

        float newDelta = (Float) value;

        // 現在のデルタを読む(同じアクセサから現在値を取得)
        float currentDelta;
        try {
            Object current = self.get(feHealthAccessor);
            currentDelta = (current instanceof Float) ? (Float) current : 0.0f;
        } catch (Throwable t) {
            currentDelta = 0.0f;
        }

        // デルタが減る方向(=体力が減る方向)のときだけ介入する。
        // 増える方向(回復扱い)はそのまま許可。
        float deltaDecrease = currentDelta - newDelta;
        if (deltaDecrease <= 0) {
            return value;
        }

        float cap = MoreIssConfig.getDamageCap();
        if (deltaDecrease > cap) {
            float cappedDelta = currentDelta - cap;
            // System.out.println("[EternalWizard][DEBUG] FE health delta capped: " + deltaDecrease + " -> " + cap);
            return cappedDelta;
        }

        return value;
    }
}