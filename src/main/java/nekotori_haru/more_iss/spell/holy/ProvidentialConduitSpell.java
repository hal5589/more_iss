package nekotori_haru.more_iss.spell.holy;

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

public class ProvidentialConduitSpell extends AbstractSpell {

    private final ResourceLocation spellId = new ResourceLocation("more_iss", "providential_conduit");

    private static final int BASE_DURATION_TICKS_LV1 = 5 * 20;
    private static final int DURATION_PER_LEVEL_TICKS = 2 * 20;

    private final float customBaseSpellPower = 1.0f;
    private final float customSpellPowerPerLevel = 0.1f;

    // ─── ツールチップおよび銘刻台GUIでの表示設定 ─────────────────────────────
    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        float generalPower = 1.0f;
        float holyPower = 1.0f;

        // 💡 解決：caster が null（銘刻台GUIなど）の場合は属性計算をスキップして基本値を表示
        if (caster != null) {
            generalPower = (float) caster.getAttributeValue(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER.get());
            holyPower = (float) caster.getAttributeValue(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.HOLY_SPELL_POWER.get());
        }

        float powerMultiplier = getCustomSpellPower(spellLevel) * generalPower * holyPower;
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
        this.manaCostPerLevel = 15;
        this.baseSpellPower = 0;
        this.spellPowerPerLevel = 0;
        this.castTime = 20;
        this.baseManaCost = 40;
    }

    private float getCustomSpellPower(int spellLevel) {
        return this.customBaseSpellPower + ((float)(spellLevel - 1) * this.customSpellPowerPerLevel);
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return this.defaultConfig;
    }

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

            float generalPower = (float) caster.getAttributeValue(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER.get());
            float holyPower = (float) caster.getAttributeValue(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.HOLY_SPELL_POWER.get());

            float powerMultiplier = getCustomSpellPower(spellLevel) * generalPower * holyPower;
            int durationTicks = calcDurationTicks(spellLevel, powerMultiplier);

            for (MobEffectInstance effectInstance : caster.getActiveEffects()) {
                if (!effectInstance.getEffect().isBeneficial()) continue;

                target.addEffect(new MobEffectInstance(
                        effectInstance.getEffect(),
                        durationTicks,
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
        return Math.round((float) getBaseDurationTicks(spellLevel) * powerMultiplier);
    }
}