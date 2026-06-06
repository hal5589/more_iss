package nekotori_haru.more_iss.spell.ender;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.*;
import io.redspace.ironsspellbooks.entity.spells.magic_arrow.MagicArrowProjectile;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import nekotori_haru.more_iss.registry.ModEffects;
import nekotori_haru.more_iss.registry.SynthesisSchoolRegistry;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.*;

public class FreischutzSpell extends AbstractSpell {

    private final ResourceLocation spellId = new ResourceLocation("more_iss", "freischutz");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.EPIC)
            .setSchoolResource(SynthesisSchoolRegistry.SYNTHESIS_RESOURCE)
            .setMaxLevel(3)
            .setCooldownSeconds(45)
            .setAllowCrafting(false)
            .build();

    // ⏳ 悪魔の拘束時間（10秒 = 200 Ticks）※この間は撃ち尽くしてはいけない
    private static final int COVENANT_DURATION_TICKS = 200;
    // ⏳ 1発ごとの保持猶予時間（4秒 = 80 Ticks）※これを超えると時間切れペナルティ
    private static final int EACH_CAST_LIMIT_TICKS = 80;

    // リフレクション用のフィールドキャッシュ
    private static Field remainingTicksField = null;

    public FreischutzSpell() {
        this.manaCostPerLevel = 40;
        this.baseSpellPower = 15;
        this.spellPowerPerLevel = 7;
        this.castTime = 0;
        this.baseManaCost = 150;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        double power = (caster != null)
                ? getSpellPower(spellLevel, caster)
                : this.baseSpellPower + ((spellLevel - 1) * this.spellPowerPerLevel);

        String secondsString = String.valueOf((double) COVENANT_DURATION_TICKS / 20.0);

        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(power, 1)),
                Component.translatable("ui.more_iss.freischutz.contract_duration", secondsString)
        );
    }

    @Override public CastType getCastType() { return CastType.INSTANT; }
    @Override public DefaultConfig getDefaultConfig() { return defaultConfig; }
    @Override public ResourceLocation getSpellResource() { return spellId; }
    @Override public int getRecastCount(int spellLevel, @Nullable LivingEntity entity) { return 7; }
    @Override public Optional<SoundEvent> getCastFinishSound() { return Optional.of(SoundRegistry.MAGIC_ARROW_RELEASE.get()); }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        if (ModEffects.RETRIBUTION != null && entity.hasEffect(ModEffects.RETRIBUTION.get())) {
            if (!level.isClientSide) {
                entity.sendSystemMessage(Component.translatable("ui.more_iss.freischutz.penalty_trigger").withStyle(net.minecraft.ChatFormatting.RED));
            }
            return false;
        }
        return Utils.preCastTargetHelper(level, entity, playerMagicData, this, 64, .15f);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {

            LivingEntity target = null;
            var recasts = playerMagicData.getPlayerRecasts();
            String spellIdString = getSpellId().toString();

            if (playerMagicData.getAdditionalCastData() instanceof TargetEntityCastData targetEntityCastData) {
                target = targetEntityCastData.getTarget(serverLevel);
            }

            if (!recasts.hasRecastForSpell(spellIdString)) {
                // 【1発目：契約成立・装填】
                if (target != null) {
                    MultiTargetEntityCastData multiData = new MultiTargetEntityCastData();
                    multiData.addTarget(target.getUUID());

                    recasts.addRecast(new RecastInstance(spellIdString, spellLevel, getRecastCount(spellLevel, entity), EACH_CAST_LIMIT_TICKS, castSource, multiData), playerMagicData);

                    if (ModEffects.DEMONIC_COVENANT != null) {
                        entity.addEffect(new MobEffectInstance(ModEffects.DEMONIC_COVENANT.get(), COVENANT_DURATION_TICKS, 0, false, false, true));
                    }
                }
            } else {
                // 【2〜7発目：連射中】
                var instance = recasts.getRecastInstance(spellIdString);
                if (instance != null && instance.getCastData() instanceof MultiTargetEntityCastData targetingData) {
                    if (!targetingData.getTargets().isEmpty()) {
                        UUID targetUUID = targetingData.getTargets().iterator().next();
                        var foundEntity = serverLevel.getEntity(targetUUID);
                        if (foundEntity instanceof LivingEntity livingTarget) {
                            target = livingTarget;
                        }
                    }

                    // 🚨 【最後の7発目を発射する瞬間の判定】
                    if (instance.getRemainingRecasts() <= 1) {
                        // 10秒経っておらず、まだ「悪魔の契約」バフが残っている場合（早期撃ち尽くしペナルティ）
                        if (ModEffects.DEMONIC_COVENANT != null && entity.hasEffect(ModEffects.DEMONIC_COVENANT.get())) {
                            if (ModEffects.RETRIBUTION != null) {
                                entity.addEffect(new MobEffectInstance(ModEffects.RETRIBUTION.get(), 400, 0, false, false, true));
                                entity.sendSystemMessage(Component.translatable("ui.more_iss.freischutz.exhausted_penalty").withStyle(net.minecraft.ChatFormatting.DARK_RED));
                            }
                            entity.removeEffect(ModEffects.DEMONIC_COVENANT.get());
                        } else {
                            // 🌟 【安全解除ルートのlang化】
                            entity.sendSystemMessage(Component.translatable("ui.more_iss.freischutz.success_clear").withStyle(net.minecraft.ChatFormatting.GREEN));
                        }
                    } else {
                        // リフレクションによるタイマー直接上書き（4秒保持）
                        try {
                            if (remainingTicksField == null) {
                                remainingTicksField = RecastInstance.class.getDeclaredField("remainingTicks");
                                remainingTicksField.setAccessible(true);
                            }
                            remainingTicksField.setInt(instance, EACH_CAST_LIMIT_TICKS);
                        } catch (Exception e) {
                            try {
                                Field ticksField = RecastInstance.class.getDeclaredField("ticks");
                                ticksField.setAccessible(true);
                                ticksField.setInt(instance, EACH_CAST_LIMIT_TICKS);
                            } catch (Exception ignored) {}
                        }
                    }
                }
            }

            // 魔弾の射出
            float finalDamage = getSpellPower(spellLevel, entity);
            MagicArrowProjectile arrow = new MagicArrowProjectile(level, entity);
            Vec3 spawnPos = entity.getEyePosition().subtract(0, 0.15, 0);

            arrow.setPos(spawnPos);
            arrow.setDamage(finalDamage);

            if (target != null && !target.isDeadOrDying()) {
                arrow.setHomingTarget(target);
                Vec3 direction = target.getBoundingBox().getCenter().subtract(spawnPos).normalize();
                arrow.shoot(direction.x, direction.y, direction.z, 1.5F, 0.0F);
            } else {
                Vec3 look = entity.getLookAngle();
                arrow.shoot(look.x, look.y, look.z, 1.5F, 1.0F);
            }

            level.addFreshEntity(arrow);
        }

        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    // 🚨 【4秒放置・打ち尽くせなかった時のペナルティ】
    @Override
    public void onRecastFinished(ServerPlayer serverPlayer, RecastInstance recastInstance, RecastResult recastResult, ICastDataSerializable castData) {
        super.onRecastFinished(serverPlayer, recastInstance, recastResult, castData);

        if (recastResult == RecastResult.TIMEOUT) {
            if (ModEffects.DEMONIC_COVENANT != null) {
                serverPlayer.removeEffect(ModEffects.DEMONIC_COVENANT.get());
            }

            if (ModEffects.RETRIBUTION != null) {
                serverPlayer.addEffect(new MobEffectInstance(ModEffects.RETRIBUTION.get(), 1200, 0, false, false, true));
                // 🌟 【踏み倒しペナルティ（4秒放置）のlang化】
                serverPlayer.sendSystemMessage(Component.translatable("ui.more_iss.freischutz.bypass_penalty").withStyle(net.minecraft.ChatFormatting.RED));
            }
        }
    }

    @Override
    public ICastDataSerializable getEmptyCastData() {
        return new MultiTargetEntityCastData();
    }
}