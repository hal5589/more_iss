package nekotori_haru.more_iss.event;

import io.redspace.ironsspellbooks.entity.spells.blood_needle.BloodNeedle;
import nekotori_haru.more_iss.registry.ModEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;

@Mod.EventBusSubscriber(modid = "more_iss")
public class SacrificialEdgeEvents {

    private static final ThreadLocal<Boolean> IS_UPDATING = ThreadLocal.withInitial(() -> false);
    private static final Random RANDOM = new Random();

    @SubscribeEvent
    public static void onSacrificialAbsorb(LivingDamageEvent event) {
        if (IS_UPDATING.get()) return;

        LivingEntity entity = event.getEntity();
        if (entity == null || entity.level().isClientSide) return;

        if (entity.hasEffect(ModEffects.SACRIFICIAL_BLEED.get())) {
            MobEffectInstance currentEffect = entity.getEffect(ModEffects.SACRIFICIAL_BLEED.get());
            if (currentEffect == null) return;

            if (event.getAmount() <= 0) return;

            try {
                IS_UPDATING.set(true);

                int currentHitCount = currentEffect.getAmplifier();
                int duration = currentEffect.getDuration();

                int newHitCount = currentHitCount + 1;
                if (newHitCount > 255) newHitCount = 255;

                // 💡 時間は何もいじらずそのまま引き継ぎ（これで高速ドットバグは完璧に直ります）
                entity.addEffect(new MobEffectInstance(
                        ModEffects.SACRIFICIAL_BLEED.get(),
                        duration,
                        newHitCount,
                        currentEffect.isAmbient(),
                        currentEffect.isVisible(),
                        currentEffect.showIcon()
                ));

            } finally {
                IS_UPDATING.set(false);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity == null) return;

        Level world = entity.level();
        if (world.isClientSide) return;

        if (event.getSource().getMsgId().equals("sacrificial_purge") || entity.hasEffect(ModEffects.SACRIFICIAL_BLEED.get())) {
            MobEffectInstance bleedEffect = entity.getEffect(ModEffects.SACRIFICIAL_BLEED.get());

            if (bleedEffect != null) {
                int hitCount = bleedEffect.getAmplifier();

                // 💡 修正：自殺だろうが他殺だろうが、新設したマーカーバフの強度からレベルを安全に100%引っこ抜く
                int spellLevel = 1;
                if (entity.hasEffect(ModEffects.SACRIFICIAL_MARKER.get())) {
                    MobEffectInstance markerEffect = entity.getEffect(ModEffects.SACRIFICIAL_MARKER.get());
                    if (markerEffect != null) {
                        spellLevel = markerEffect.getAmplifier();
                    }
                }

                LivingEntity attacker = null;
                if (event.getSource().getEntity() instanceof LivingEntity) {
                    attacker = (LivingEntity) event.getSource().getEntity();
                }

                if (attacker == null) {
                    attacker = entity;
                }

                double generalPower = attacker.getAttributeValue(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER.get());
                double bloodPower = attacker.getAttributeValue(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.BLOOD_SPELL_POWER.get());

                double levelMultiplier = 2.0 + ((spellLevel - 1) * 0.5);
                float baseDamage = (float) Math.max(5.0, ((double) hitCount * levelMultiplier) * generalPower * bloodPower);
                float needleDamage = baseDamage / 5.0f;

                // 💡 マーカーバフに保存された正確なレベルに基づいて本数を決定（Lv5なら25本）
                int needleCount = spellLevel * 5;

                for (int i = 0; i < needleCount; i++) {
                    BloodNeedle needle = new BloodNeedle(world, entity);
                    needle.setDamage(needleDamage);

                    if (attacker != null) {
                        needle.setOwner(attacker);
                    }

                    Vec3 spawnPos = entity.getEyePosition().add(0, 0.5, 0);
                    needle.moveTo(spawnPos);

                    float u = RANDOM.nextFloat();
                    float v = RANDOM.nextFloat();
                    float theta = u * 2.0f * (float) Math.PI;
                    float phi = (float) Math.acos(2.0f * v - 1.0f);

                    double x = Math.sin(phi) * Math.cos(theta);
                    double y = Math.sin(phi) * Math.sin(theta);
                    double z = Math.cos(phi);

                    Vec3 launchDirection = new Vec3(x, y, z).normalize();

                    needle.shoot(launchDirection);
                    world.addFreshEntity(needle);
                }
            }
        }
    }
}