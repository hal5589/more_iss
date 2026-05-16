package nekotori_haru.more_iss.event;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import nekotori_haru.more_iss.registry.ModEffects;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "more_iss")
public class OverburstEvents {

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        DamageSource source = event.getSource();

        if (source.getEntity() instanceof LivingEntity attacker && attacker.hasEffect(ModEffects.OVERBURST.get())) {
            if (source.getDirectEntity() == attacker) {
                LivingEntity target = event.getEntity();
                MobEffectInstance effect = attacker.getEffect(ModEffects.OVERBURST.get());
                if (effect == null) return;

                int spellLevel = effect.getAmplifier() + 1;

                // 1. 魔法威力と物理基礎火力の取得
                double spellPower = attacker.getAttributeValue(AttributeRegistry.SPELL_POWER.get());
                float baseAttackDamage = (float) attacker.getAttributeValue(Attributes.ATTACK_DAMAGE);

                // 2. レベルに応じた基礎倍率 (Lv1:3.0, Lv2:5.0, Lv3:7.0)
                float baseMultiplier = 1.0f + (spellLevel * 2.0f);

                // 3. トータルダメージ = 基礎火力 * (基礎倍率 * 魔法威力)
                float totalDamage = baseAttackDamage * (baseMultiplier * (float)spellPower);

                // 4. 貫通割合の計算 (Lv1:20%, Lv2:40%, Lv3:60%)
                float penetrationRatio = spellLevel * 0.2f;
                float piercingAmount = totalDamage * penetrationRatio;
                float normalAmount = totalDamage - piercingAmount;

                // 5. 元の物理ダメージをキャンセルし、属性を分けて再適用
                event.setCanceled(true);
                target.hurt(target.damageSources().fellOutOfWorld(), piercingAmount); // 貫通分
                target.hurt(target.damageSources().magic(), normalAmount);           // 通常分（魔法扱い）

                // 6. 後処理
                ItemStack weapon = attacker.getMainHandItem();
                if (!weapon.isEmpty() && weapon.isDamageableItem()) {
                    weapon.hurtAndBreak(weapon.getMaxDamage(), attacker, (e) -> {
                        e.broadcastBreakEvent(InteractionHand.MAIN_HAND);
                    });
                }
                attacker.removeEffect(ModEffects.OVERBURST.get());
            }
        }
    }
}