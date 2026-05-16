package nekotori_haru.more_iss.spell;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.TargetEntityCastData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

@AutoSpellConfig
public class ProvidentialConduitSpell extends AbstractSpell {

    private final ResourceLocation spellId = new ResourceLocation("more_iss", "providential_conduit");

    // 基準時間: lv1=5秒(100tick)、レベルごとに+2秒(40tick) → Lv10で23秒
    private static final int BASE_DURATION_TICKS_LV1 = 5 * 20;
    private static final int DURATION_PER_LEVEL_TICKS = 2 * 20;

    // 💡 解決策: 小数点を扱える独自の魔法威力（倍率）変数を定義
    private final float customBaseSpellPower = 1.0f;       // 基礎パワー (1.0 = 100%)
    private final float customSpellPowerPerLevel = 0.1f;   // 1レベルごとに +10% (0.1f) ずつ上昇

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        // 独自に計算した小数点パワー（%倍率）を時間計算に渡す
        float powerMultiplier = getCustomSpellPower(spellLevel);
        int finalTicks = calcDurationTicks(spellLevel, powerMultiplier);
        float finalSec = finalTicks / 20f;

        return List.of(
                Component.translatable("ui.more_iss.providential_conduit.duration",
                        String.format("%.1f", finalSec))
        );
    }

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.UNCOMMON)
            .setSchoolResource(SchoolRegistry.HOLY_RESOURCE)
            .setMaxLevel(10)
            .setCooldownSeconds(60)
            .build();

    public ProvidentialConduitSpell() {
        this.manaCostPerLevel = 1;
        // 💡 エラーの起きていたシステム側の変数は 0 を代入して安全に黙らせます
        this.baseSpellPower = 0;
        this.spellPowerPerLevel = 0;
        this.castTime = 20; // 1秒
        this.baseManaCost = 40;
    }

    // 💡 独自の float（小数点）ベースの魔法威力を計算するメソッド
    private float getCustomSpellPower(int spellLevel) {
        // Lv1  = 1.0 + (0 * 0.1) = 1.0  (100%)
        // Lv10 = 1.0 + (9 * 0.1) = 1.9  (190%)
        return this.customBaseSpellPower + ((float)(spellLevel - 1) * this.customSpellPowerPerLevel);
    }

    @Override
    public DefaultConfig getDefaultConfig() { return defaultConfig; }
    @Override
    public CastType getCastType() { return CastType.LONG; }
    @Override
    public ResourceLocation getSpellResource() { return spellId; }

    @Override
    public @Nullable AnimationHolder getCastStartAnimation() { return AnimationHolder.none(); }
    @Override
    public @Nullable AnimationHolder getCastFinishAnimation() { return AnimationHolder.none(); }

    // ─── ターゲット選択 ───────────────────────────────────────────

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity caster, MagicData playerMagicData) {
        if (!Utils.preCastTargetHelper(level, caster, playerMagicData, this, 32, .35f, false)) {
            playerMagicData.setAdditionalCastData(new TargetEntityCastData(caster));
            if (caster instanceof ServerPlayer serverPlayer) {
                serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(
                        Component.translatable("ui.irons_spellbooks.spell_target_success_self",
                                this.getDisplayName(serverPlayer)).withStyle(ChatFormatting.GREEN)
                ));
            }
        }
        return true;
    }

    // ─── コアロジック ────────────────────────────────────────────

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource castSource, MagicData playerMagicData) {

        if (playerMagicData.getAdditionalCastData() instanceof TargetEntityCastData targetData) {
            LivingEntity target = targetData.getTarget((ServerLevel) level);
            if (target == null) target = caster;

            // 💡 独自メソッドから精確な float の％倍率を取得
            float powerMultiplier = getCustomSpellPower(spellLevel);
            int durationTicks = calcDurationTicks(spellLevel, powerMultiplier);

            // キャスター自身の有益バフをすべてターゲットに付与
            for (MobEffectInstance effectInstance : caster.getActiveEffects()) {
                if (!effectInstance.getEffect().isBeneficial()) continue;

                target.addEffect(new MobEffectInstance(
                        effectInstance.getEffect(),
                        durationTicks, // %で綺麗に補正された持続時間
                        effectInstance.getAmplifier(),
                        effectInstance.isAmbient(),
                        effectInstance.isVisible(),
                        effectInstance.showIcon()
                ));
            }
        }

        super.onCast(level, spellLevel, caster, castSource, playerMagicData);
    }

    // ─── 時間計算 ─────────────────────────────────────────────────

    private int getBaseDurationTicks(int spellLevel) {
        return BASE_DURATION_TICKS_LV1 + (spellLevel - 1) * DURATION_PER_LEVEL_TICKS;
    }

    private int calcDurationTicks(int spellLevel, float powerMultiplier) {
        // 例：Lv10のベース 23秒（460 ticks）× 独自威力 1.9倍 ＝ 43.7秒（874 ticks）
        return Math.round((float) getBaseDurationTicks(spellLevel) * powerMultiplier);
    }
}