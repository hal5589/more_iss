package nekotori_haru.more_iss.mixin;

import nekotori_haru.more_iss.registry.MoreIssConfig;
import nekotori_haru.more_iss.entity.EternalWizardEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    // setHealth(float) の引数を、呼び出し元(hurt/actuallyHurt/独自実装問わず)の
    // 直前の体力(getHealth())と比較するために必要。
    @Shadow
    public abstract float getHealth();

    @Unique
    private float getDamageCap() {
        return MoreIssConfig.getDamageCap();
    }

    @Unique
    private boolean isDamageCapEnabled() {
        return MoreIssConfig.isDamageCapEnabled();
    }

    // Mojangマッパー名のまま指定し、remap はデフォルト(true)のままにする。
    // 開発環境・本番環境どちらでも Mixin AP が生成する refmap を使って
    // 自動的に SRG 名 (m_6469_) へ変換されるのが正しい挙動。
    //
    // 【三段防御の1段目】
    // hurt() の入口で早期にキャップする。多くのケースはここで完結するが、
    // fantasy_ending のように LivingHurtEvent 内で event.setAmount() を使い
    // 後段でダメージを再計算する Mod には効かない場合がある。
    @ModifyVariable(
            method = "hurt", // hurt(DamageSource, float)
            at = @At("HEAD"),
            argsOnly = true
    )
    private float more_iss$capDamage(float amount, DamageSource source) {
        LivingEntity self = (LivingEntity) (Object) this;

        // EternalWizardEntity のみ対象
        if (!(self instanceof EternalWizardEntity)) {
            return amount;
        }

        // キャップが無効な場合は何もしない
        if (!isDamageCapEnabled()) {
            return amount;
        }

        float cap = getDamageCap();

        // ダメージがキャップ値を超えている場合のみ制限
        if (amount > cap) {
            // ⭐ デバッグログ（必要に応じて削除）
            // System.out.println("[EternalWizard] Damage capped: " + amount + " -> " + cap);
            return cap;
        }

        return amount;
    }

    // 【三段防御の3段目・最終防衛】
    // fantasy_ending の EntityActuallyHurt#actuallyHurt0 は hurt() も
    // LivingHurtEvent も経由せず、LivingEntity#setHealth(float) を直接呼んで
    // 体力を独自計算で減算してくる。これは vanilla の setHealth であり、
    // 誰がどう呼び出そうと最終的に必ず通過する唯一の共通経路。
    // setHealth の引数そのもの(新しい体力の絶対値)を、メソッド本体の内部実装に
    // 依存しない形で直接書き換える。
    //
    // 注意: これは setHealth による「回復」も含めて全呼び出しを通るため、
    // 減少方向(newHealth < currentHealth)のときだけ介入し、
    // 回復処理は一切妨げない。
    @ModifyVariable(
            method = "setHealth", // setHealth(float)
            at = @At("HEAD"),
            argsOnly = true
    )
    private float more_iss$capSetHealth(float newHealth) {
        LivingEntity self = (LivingEntity) (Object) this;

        if (!(self instanceof EternalWizardEntity)) {
            return newHealth;
        }

        if (!isDamageCapEnabled()) {
            return newHealth;
        }

        float currentHealth = this.getHealth();

        // 回復方向(増加・同値)はそのまま許可
        if (newHealth >= currentHealth) {
            return newHealth;
        }

        float cap = getDamageCap();
        float decrease = currentHealth - newHealth;

        if (decrease > cap) {
            // ⭐ デバッグログ（必要に応じて削除）
            // System.out.println("[EternalWizard] setHealth decrease capped: " + decrease + " -> " + cap);
            return currentHealth - cap;
        }

        return newHealth;
    }
}