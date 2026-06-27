package nekotori_haru.more_iss.event;

import nekotori_haru.more_iss.entity.EternalWizardEntity;
import nekotori_haru.more_iss.registry.MoreIssConfig;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * EternalWizardEntity のダメージキャップを「最終防衛」するハンドラ。
 *
 * 経緯:
 *  - 当初 Mixin (@ModifyVariable on LivingEntity#hurt) でキャップしていたが、
 *    fantasy_ending の ModAttributes#livingDamage が LivingHurtEvent 内で
 *    event.setAmount(...) により耐性無視の再計算ダメージを上書きしてくるため、
 *    自分のキャップが意味を失っていた。
 *  - LivingHurtEvent はリスナーの EventPriority 順に呼ばれ、setAmount は
 *    イベントオブジェクトの値を直接書き換える。よって「最後に setAmount した者が勝つ」。
 *  - 対策として、自分も LivingHurtEvent のリスナーとして登録し、
 *    優先度を LOWEST にすることで他Mod（fantasy_ending含む）の処理が
 *    全て終わった後に確実に再キャップする。
 *
 * 注意:
 *  - これは「他Modの処理を遮断/無効化」するものではなく、
 *    「自分が一番最後に最終調整する」という穏当な解決策。
 *  - 他Modが LivingHurtEvent をキャンセルした場合はそもそもダメージが
 *    発生しないため、このハンドラは isCanceled() のときは何もしない。
 */
@Mod.EventBusSubscriber(modid = "more_iss")
public class DamageCapEventHandler {

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = false)
    public static void onLivingHurtFinal(LivingHurtEvent event) {
        LivingEntity self = event.getEntity();

        // EternalWizardEntity のみ対象
        if (!(self instanceof EternalWizardEntity)) {
            return;
        }

        // キャップが無効な場合は何もしない
        if (!MoreIssConfig.isDamageCapEnabled()) {
            return;
        }

        float cap = MoreIssConfig.getDamageCap();
        float amount = event.getAmount();

        // この時点で fantasy_ending 等、他Modの LivingHurtEvent リスナーは
        // 既に全て処理済み（自分が LOWEST のため）。ここで最終的にキャップする。
        if (amount > cap) {
            event.setAmount(cap);
        }
    }
}