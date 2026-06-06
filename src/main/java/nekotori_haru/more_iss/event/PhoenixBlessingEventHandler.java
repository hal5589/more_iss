package nekotori_haru.more_iss.event;

import nekotori_haru.more_iss.More_iss;
import nekotori_haru.more_iss.registry.ModEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = More_iss.MODID)
public class PhoenixBlessingEventHandler {

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();

        if (entity.level().isClientSide) return;

        MobEffectInstance effect = entity.getEffect(ModEffects.PHOENIX_BLESSING.get());
        if (effect == null) return;

        int amplifier = effect.getAmplifier();

        More_iss.LOGGER.info("PhoenixBlessing triggered! Amplifier: {}, Remaining revives: {}", amplifier, amplifier + 1);

        // 死亡キャンセル
        event.setCanceled(true);

        // ⭐ 体力を4に設定
        entity.setHealth(4.0f);

        // ⭐ 再生II (5秒 = 100tick)
        entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1, false, false));

        // ⭐ 耐性V (1秒 = 20tick, アンプリファイア4で耐性V)
        entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20, 4, false, false));

        // ⭐ 耐火 (30秒 = 600tick)
        entity.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 600, 0, false, false));

        // ⭐ 音を鳴らす（エンダードラゴンの咆哮 + トーテム音）
        entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 1.0f, 0.8f);
        entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0f, 1.0f);

        // パーティクル演出（派手に）
        if (entity.level() instanceof ServerLevel serverLevel) {
            // トーテムパーティクル
            serverLevel.sendParticles(
                    net.minecraft.core.particles.ParticleTypes.TOTEM_OF_UNDYING,
                    entity.getX(), entity.getY() + 1, entity.getZ(),
                    50, 0.5, 1, 0.5, 0.1
            );
            // 炎の爆発
            serverLevel.sendParticles(
                    net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER,
                    entity.getX(), entity.getY() + 0.5, entity.getZ(),
                    1, 0, 0, 0, 0
            );
            // 炎の渦
            for (int i = 0; i < 360; i += 30) {
                double rad = Math.toRadians(i);
                double x = entity.getX() + Math.cos(rad) * 2;
                double z = entity.getZ() + Math.sin(rad) * 2;
                serverLevel.sendParticles(
                        net.minecraft.core.particles.ParticleTypes.FLAME,
                        x, entity.getY() + 0.5, z,
                        5, 0.1, 0.5, 0.1, 0.05
                );
            }
            // 大量の火の粉
            serverLevel.sendParticles(
                    net.minecraft.core.particles.ParticleTypes.FLAME,
                    entity.getX(), entity.getY() + 0.5, entity.getZ(),
                    150, 1.5, 1.5, 1.5, 0.1
            );
            // ソウルファイア
            serverLevel.sendParticles(
                    net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,
                    entity.getX(), entity.getY() + 1, entity.getZ(),
                    100, 1, 2, 1, 0.1
            );
        }

        // 蘇生回数を減らす
        int newAmplifier = amplifier - 1;

        if (newAmplifier >= 0) {
            int remainingDuration = effect.getDuration();
            entity.removeEffect(ModEffects.PHOENIX_BLESSING.get());
            entity.addEffect(new MobEffectInstance(ModEffects.PHOENIX_BLESSING.get(), remainingDuration, newAmplifier, false, true, true));
            More_iss.LOGGER.info("PhoenixBlessing - Revives remaining: {}", newAmplifier + 1);
        } else {
            entity.removeEffect(ModEffects.PHOENIX_BLESSING.get());
            More_iss.LOGGER.info("PhoenixBlessing - No revives left, effect removed");
        }
    }
}