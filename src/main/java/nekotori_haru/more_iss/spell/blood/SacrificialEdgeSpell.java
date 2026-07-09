package nekotori_haru.more_iss.spell.blood;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.RecastInstance;
import io.redspace.ironsspellbooks.entity.spells.blood_slash.BloodSlashProjectile;
import nekotori_haru.more_iss.registry.ModEffects;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SacrificialEdgeSpell extends AbstractSpell {

    private final ResourceLocation spellId = new ResourceLocation("more_iss", "sacrificial_edge");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.EPIC)
            .setSchoolResource(SchoolRegistry.BLOOD_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(60)
            .setAllowCrafting(false)
            .build();

    // 1スタックあたりの基礎倍率設定
    private final double base = 2.0;
    private final double perLevel = 0.5;

    public SacrificialEdgeSpell() {
        this.baseSpellPower = 2;
        this.spellPowerPerLevel = 0;
        this.baseManaCost = 100;
        this.manaCostPerLevel = 30;
    }

    private double getCustom(int level) {
        return base + ((level - 1) * perLevel);
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int level, LivingEntity caster) {
        // 🛠️ パターンB：基礎倍率 × 術者の魔法威力＆融合属性威力のマルチプライヤー
        double finalMultiplier = (caster != null)
                ? getCustom(level) * getSpellPower(level, caster)
                : getCustom(level);

        // 🔗 翻訳キー "ui.more_iss.sacrificial_edge.info": "ダメージ倍率: %1$sx" に完全連動
        return List.of(
                Component.translatable("ui.more_iss.sacrificial_edge.info", Utils.stringTruncation(finalMultiplier, 1))
        );
    }

    @Override public DefaultConfig getDefaultConfig() { return defaultConfig; }
    @Override public CastType getCastType() { return CastType.INSTANT; }
    @Override public ResourceLocation getSpellResource() { return spellId; }
    @Override public int getRecastCount(int level, @Nullable LivingEntity entity) { return 2; }

    @Override
    public void onCast(Level world, int level, LivingEntity entity, CastSource source, MagicData data) {
        var recasts = data.getPlayerRecasts();
        String spellIdString = getSpellId().toString(); // 型安全化

        if (!recasts.hasRecastForSpell(spellIdString)) {
            // 【1回目：誓約（生贄の儀式開始）】
            recasts.addRecast(new RecastInstance(spellIdString, level, 2, 600, source, null), data);

            if (!world.isClientSide) {
                // 1時間（72000ticks）の蓄積用出血デバフを付与
                entity.addEffect(new MobEffectInstance(ModEffects.SACRIFICIAL_BLEED.get(), 72000, 0, false, false, true));
            }
        } else {
            // 【2回目：解放（斬撃の射出）】
            float finalDamage = 5.0f; // 最低保証ダメージ

            if (entity.hasEffect(ModEffects.SACRIFICIAL_BLEED.get())) {
                // 🩸 自傷によって蓄積したアンプリファイア（スタック数）を取得
                // 0スタック（最初の1被弾）の時でも hits = 1 として計算をスタートさせるために +1 します
                int hits = entity.getEffect(ModEffects.SACRIFICIAL_BLEED.get()).getAmplifier() + 1;

                // 📈 【最終ダメージ計算式】
                // (スタック数 × レベル毎の基礎倍率) × 術者の魔法威力＆融合属性威力
                double calculatedDamage = (hits * getCustom(level)) * getSpellPower(level, entity);

                finalDamage = (float) Math.max(5.0, calculatedDamage);
            }

            if (!world.isClientSide && world instanceof ServerLevel) {
                BloodSlashProjectile slash = new BloodSlashProjectile(world, entity);

                // 🐛 弾のスポーン位置と視線方向への正確な射出（位置を目の少し下に微調整）
                slash.setPos(entity.getEyePosition().subtract(0, 0.1, 0));
                slash.shoot(entity.getLookAngle().x, entity.getLookAngle().y, entity.getLookAngle().z, 1.2F, 0.0F);

                slash.setDamage(finalDamage); // 確定したダメージを同期
                world.addFreshEntity(slash);
            }

            // 出血デバフを綺麗に解除
            entity.removeEffect(ModEffects.SACRIFICIAL_BLEED.get());
        }

        super.onCast(world, level, entity, source, data);
    }
}