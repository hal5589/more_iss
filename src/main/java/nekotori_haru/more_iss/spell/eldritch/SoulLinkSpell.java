package nekotori_haru.more_iss.spell.eldritch;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.TargetEntityCastData;
import io.redspace.ironsspellbooks.spells.eldritch.AbstractEldritchSpell;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class SoulLinkSpell extends AbstractEldritchSpell {

    private final ResourceLocation spellId = new ResourceLocation("more_iss", "soul_link");

    private static final int BASE_DURATION_TICKS_LV1 = 5 * 20;
    private static final int DURATION_PER_LEVEL_TICKS = 2 * 20;

    private final float customBaseSpellPower = 1.0f;
    private final float customSpellPowerPerLevel = 0.1f;

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        int finalTicks;
        if (caster == null) {
            finalTicks = getBaseDurationTicks(spellLevel);
        } else {
            float generalPower = (float) caster.getAttributeValue(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER.get());
            float eldritchPower = (float) caster.getAttributeValue(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.ELDRITCH_SPELL_POWER.get());

            float powerMultiplier = getCustomSpellPower(spellLevel) * generalPower * eldritchPower;
            finalTicks = calcDurationTicks(spellLevel, powerMultiplier);
        }

        float finalSec = finalTicks / 20f;

        return List.of(
                Component.translatable("ui.more_iss.soul_link.duration", String.format("%.1f", finalSec))
        );
    }

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.EPIC)
            .setSchoolResource(SchoolRegistry.ELDRITCH_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(60)
            .build();

    public SoulLinkSpell() {
        this.manaCostPerLevel = 30;
        this.baseSpellPower = 0;
        this.spellPowerPerLevel = 0;
        this.castTime = 20;
        this.baseManaCost = 100;
    }

    private float getCustomSpellPower(int spellLevel) {
        return this.customBaseSpellPower + ((float)(spellLevel - 1) * this.customSpellPowerPerLevel);
    }

    @Override
    public DefaultConfig getDefaultConfig() { return this.defaultConfig; }
    @Override
    public CastType getCastType() { return CastType.LONG; }
    @Override
    public ResourceLocation getSpellResource() { return spellId; }

    @Override
    public @Nullable AnimationHolder getCastStartAnimation() { return AnimationHolder.none(); }
    @Override
    public @Nullable AnimationHolder getCastFinishAnimation() { return AnimationHolder.none(); }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity caster, MagicData playerMagicData) {
        if (!Utils.preCastTargetHelper(level, caster, playerMagicData, this, 32, .35f, false)) {
            if (caster instanceof ServerPlayer serverPlayer) {
                serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(
                        Component.translatable("ui.more_iss.soul_link.no_target").withStyle(ChatFormatting.RED)
                ));
            }
            return false;
        }

        if (level instanceof ServerLevel serverLevel && playerMagicData.getAdditionalCastData() instanceof TargetEntityCastData targetData) {
            LivingEntity target = targetData.getTarget(serverLevel);
            if (target == null || target.equals(caster)) {
                if (caster instanceof ServerPlayer serverPlayer) {
                    serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(
                            Component.translatable("ui.more_iss.soul_link.no_target").withStyle(ChatFormatting.RED)
                    ));
                }
                return false;
            }
        }

        return true;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource castSource, MagicData playerMagicData) {

        if (playerMagicData.getAdditionalCastData() instanceof TargetEntityCastData targetData) {
            LivingEntity target = targetData.getTarget((ServerLevel) level);

            if (target == null || target.equals(caster)) {
                super.onCast(level, spellLevel, caster, castSource, playerMagicData);
                return;
            }

            float generalPower = (float) caster.getAttributeValue(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER.get());
            float eldritchPower = (float) caster.getAttributeValue(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.ELDRITCH_SPELL_POWER.get());

            float powerMultiplier = getCustomSpellPower(spellLevel) * generalPower * eldritchPower;
            int durationTicks = calcDurationTicks(spellLevel, powerMultiplier);

            List<MobEffect> removedEffects = new ArrayList<>();
            List<MobEffectInstance> activeEffects = new ArrayList<>(caster.getActiveEffects());

            for (MobEffectInstance effectInstance : activeEffects) {
                if (effectInstance.getEffect().isBeneficial()) continue;

                target.addEffect(new MobEffectInstance(
                        effectInstance.getEffect(),
                        durationTicks,
                        effectInstance.getAmplifier(),
                        effectInstance.isAmbient(),
                        effectInstance.isVisible(),
                        effectInstance.showIcon()
                ));

                caster.removeEffect(effectInstance.getEffect());
                removedEffects.add(effectInstance.getEffect());
            }

            if (!removedEffects.isEmpty()) {
                // ⭕ 変更：新しいID「soul_protection」で動的に検索をかける
                java.util.Optional<MobEffect> soulLinkEffect = BuiltInRegistries.MOB_EFFECT.getOptional(
                        new ResourceLocation("more_iss", "soul_protection")
                );

                if (soulLinkEffect.isPresent()) {
                    caster.addEffect(new MobEffectInstance(soulLinkEffect.get(), durationTicks, 0, false, true, true));
                }

                nekotori_haru.more_iss.event.SoulLinkEventHandler.saveResistances(caster, removedEffects);
            }
        }

        super.onCast(level, spellLevel, caster, castSource, playerMagicData);
    }

    private int getBaseDurationTicks(int spellLevel) {
        return BASE_DURATION_TICKS_LV1 + (spellLevel - 1) * DURATION_PER_LEVEL_TICKS;
    }

    private int calcDurationTicks(int spellLevel, float powerMultiplier) {
        return Math.round((float) getBaseDurationTicks(spellLevel) * powerMultiplier);
    }
}