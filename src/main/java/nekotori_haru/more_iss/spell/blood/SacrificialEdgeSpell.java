package nekotori_haru.more_iss.spell.blood;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.RecastInstance;
import io.redspace.ironsspellbooks.entity.spells.blood_slash.BloodSlashProjectile;
import nekotori_haru.more_iss.registry.ModEffects;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
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

    private final double customBaseSpellPower = 2.0;
    private final double customSpellPowerPerLevel = 0.5;

    public SacrificialEdgeSpell() {
        this.manaCostPerLevel = 25;
        this.baseSpellPower = 0;
        this.spellPowerPerLevel = 0;
        this.castTime = 0;
        this.baseManaCost = 100
        ;
    }

    private double getCustomSpellPower(int spellLevel) {
        return this.customBaseSpellPower + ((spellLevel - 1) * this.customSpellPowerPerLevel);
    }

    // ─── ツールチップおよび銘刻台GUIでの表示設定 ─────────────────────────────
    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        double generalPower = 1.0;
        double bloodPower = 1.0;

        // 💡 解決：caster が null（銘刻台GUIなど）の場合は属性計算をスキップして基本値を表示
        if (caster != null) {
            generalPower = caster.getAttributeValue(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER.get());
            bloodPower = caster.getAttributeValue(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.BLOOD_SPELL_POWER.get());
        }

        double finalPower = getCustomSpellPower(spellLevel) * generalPower * bloodPower;
        return List.of(
                Component.translatable("ui.more_iss.sacrificial_edge.info", Utils.stringTruncation(finalPower, 2))
        );
    }

    @Override
    public DefaultConfig getDefaultConfig() { return this.defaultConfig; }

    @Override
    public CastType getCastType() { return CastType.INSTANT; }
    @Override
    public ResourceLocation getSpellResource() { return spellId; }

    @Override
    public int getRecastCount(int spellLevel, @Nullable LivingEntity entity) {
        return 2;
    }

    // ─── コアロジック ────────────────────────────────────────────
    @Override
    public void onCast(Level world, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        var recasts = playerMagicData.getPlayerRecasts();

        if (!recasts.hasRecastForSpell(getSpellId())) {
            recasts.addRecast(new RecastInstance(getSpellId(), spellLevel, getRecastCount(spellLevel, entity), 600, castSource, null), playerMagicData);

            if (!world.isClientSide) {
                // ① 通常の自傷ドットデバフ（時間はバニラのまま、強度は0からスタート）
                entity.addEffect(new MobEffectInstance(
                        ModEffects.SACRIFICIAL_BLEED.get(),
                        72000,
                        0,
                        false, true, true
                ));

                // ② 魔法レベルを記録するためだけの非表示マーカーバフ
                entity.addEffect(new MobEffectInstance(
                        ModEffects.SACRIFICIAL_MARKER.get(),
                        72000,
                        spellLevel,
                        false, false, false
                ));
            }
        }
        else {
            if (entity.hasEffect(ModEffects.SACRIFICIAL_BLEED.get())) {
                MobEffectInstance effect = entity.getEffect(ModEffects.SACRIFICIAL_BLEED.get());

                if (effect != null) {
                    int hitCount = effect.getAmplifier();

                    double damagePerHit = getCustomSpellPower(spellLevel);

                    double generalPower = entity.getAttributeValue(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER.get());
                    double bloodPower = entity.getAttributeValue(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.BLOOD_SPELL_POWER.get());
                    float finalSlashDamage = (float) Math.max(5.0, ((double) hitCount * damagePerHit) * generalPower * bloodPower);

                    if (!world.isClientSide) {
                        BloodSlashProjectile bloodSlash = new BloodSlashProjectile(world, entity);
                        bloodSlash.setPos(entity.getEyePosition());
                        bloodSlash.shoot(entity.getLookAngle());
                        bloodSlash.setDamage(finalSlashDamage);

                        world.addFreshEntity(bloodSlash);
                    }

                    entity.removeEffect(ModEffects.SACRIFICIAL_BLEED.get());
                    entity.removeEffect(ModEffects.SACRIFICIAL_MARKER.get());
                }
            }
        }

        super.onCast(world, spellLevel, entity, castSource, playerMagicData);
    }
}