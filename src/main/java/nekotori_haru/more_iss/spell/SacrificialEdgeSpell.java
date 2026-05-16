package nekotori_haru.more_iss.spell;

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

@AutoSpellConfig
public class SacrificialEdgeSpell extends AbstractSpell {

    private final ResourceLocation spellId = new ResourceLocation("more_iss", "sacrificial_edge");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.EPIC)
            .setSchoolResource(SchoolRegistry.BLOOD_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(15)
            .setAllowCrafting(false)
            .build();

    // 💡 修正ポイント：指定された初期倍率「3」とレベルごとの加算「1.25」を設定
    private final double customBaseSpellPower = 2.0;      // 魔法Lv1時点での1Hitあたりの増加威力
    private final double customSpellPowerPerLevel = 0.5;  // 魔法Lvが1上がるごとの加算量

    public SacrificialEdgeSpell() {
        this.manaCostPerLevel = 5;
        // 親クラス側の制限を避けるためのダミー設定（他はintの時と同様）
        this.baseSpellPower = 0;
        this.spellPowerPerLevel = 0;
        this.castTime = 0;
        this.baseManaCost = 15;
    }

    // 💡 指定された通りの倍率スケールを計算するメソッド
    private double getCustomSpellPower(int spellLevel) {
        // Lv1 = 3.0 + (0 * 1.25) = 3.0
        // Lv2 = 3.0 + (1 * 1.25) = 4.25
        // Lv3 = 3.0 + (2 * 1.25) = 5.5
        return this.customBaseSpellPower + ((spellLevel - 1) * this.customSpellPowerPerLevel);
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.more_iss.sacrificial_edge.info", Utils.stringTruncation(getCustomSpellPower(spellLevel), 2))
        );
    }

    @Override
    public DefaultConfig getDefaultConfig() { return defaultConfig; }
    @Override
    public CastType getCastType() { return CastType.INSTANT; }
    @Override
    public ResourceLocation getSpellResource() { return spellId; }

    @Override
    public int getRecastCount(int spellLevel, @Nullable LivingEntity entity) {
        return 2;
    }

    @Override
    public void onCast(Level world, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        var recasts = playerMagicData.getPlayerRecasts();

        if (!recasts.hasRecastForSpell(getSpellId())) {
            recasts.addRecast(new RecastInstance(getSpellId(), spellLevel, getRecastCount(spellLevel, entity), 600, castSource, null), playerMagicData);

            if (!world.isClientSide) {
                entity.addEffect(new MobEffectInstance(
                        ModEffects.SACRIFICIAL_BLEED.get(),
                        72000,
                        0,
                        false, true, true
                ));
            }
        }
        else {
            if (entity.hasEffect(ModEffects.SACRIFICIAL_BLEED.get())) {
                MobEffectInstance effect = entity.getEffect(ModEffects.SACRIFICIAL_BLEED.get());

                if (effect != null) {
                    int hitCount = effect.getAmplifier();

                    // 修正された正確なdouble倍率を取得
                    double damagePerHit = getCustomSpellPower(spellLevel);

                    // 最終ダメージ ＝ 自傷回数 × 設定された倍率 (最低保証 5.0f)
                    float finalSlashDamage = (float) Math.max(5.0, (double) hitCount * damagePerHit);

                    if (!world.isClientSide) {
                        BloodSlashProjectile bloodSlash = new BloodSlashProjectile(world, entity);
                        bloodSlash.setPos(entity.getEyePosition());
                        bloodSlash.shoot(entity.getLookAngle());
                        bloodSlash.setDamage(finalSlashDamage);

                        world.addFreshEntity(bloodSlash);
                    }

                    entity.removeEffect(ModEffects.SACRIFICIAL_BLEED.get());
                }
            }
        }

        super.onCast(world, spellLevel, entity, castSource, playerMagicData);
    }
}