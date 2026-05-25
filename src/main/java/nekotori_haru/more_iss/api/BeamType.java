package nekotori_haru.more_iss.api;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public enum BeamType {
    FLAME("flame_ray", 0xFF3300, 45.0D) { // 火：赤色
        @Override
        public void applyEffect(LivingEntity target, int level) {
            target.setSecondsOnFire(3 + level); // レベルに応じて炎上秒数が延長
        }
    },
    SOLAR("solar_ray", 0x7FFF00, 45.0D) { // 自然：まばゆい黄緑
        @Override
        public void applyEffect(LivingEntity target, int level) {
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, (4 + level) * 20, 0, false, true));
        }
    },
    HOLY("holy_ray", 0xFFFACD, 45.0D) { // 聖：神々しい白～オレンジ
        @Override
        public void applyEffect(LivingEntity target, int level) {
            if (target.isInvertedHealAndHarm()) { // アンデッド特攻
                target.hurt(target.damageSources().magic(), 6.0f + (level * 2.0f)); // 追加ダメージ
            }
        }
    },
    VOID("void_ray", 0x4B0082, 80.0D) { // エンダー：濃い紫色・長射程
        @Override
        public void applyEffect(LivingEntity target, int level) {
            // 特殊効果なし（長射程がメリット）
        }
    },
    SPECTRAL("spectral_ray", 0x87CEFA, 45.0D) { // 召喚：半透明の薄水色
        @Override
        public void applyEffect(LivingEntity target, int level) {
            // 完全停止システム用の特殊タグを付与（3秒間 = 60000msの消滅タイマー用数値を付与）
            target.addTag("more_iss.frozen_ai");
            target.getPersistentData().putInt("more_iss.frozen_ticks", 40 + (level * 20)); // レベルで停止時間延長
        }
    };

    private final String id;
    private final int color;
    private final double range;

    BeamType(String id, int color, double range) {
        this.id = id;
        this.color = color;
        this.range = range;
    }

    public String getId() { return id; }
    public int getColor() { return color; }
    public double getRange() { return range; }
    public abstract void applyEffect(LivingEntity target, int level);
}