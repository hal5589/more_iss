package nekotori_haru.more_iss.spell.ender;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.RecastInstance;
import io.redspace.ironsspellbooks.capabilities.magic.RecastResult;
import io.redspace.ironsspellbooks.capabilities.magic.TargetEntityCastData;
import io.redspace.ironsspellbooks.entity.spells.magic_arrow.MagicArrowProjectile;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import nekotori_haru.more_iss.registry.ModEffects;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

@AutoSpellConfig
public class FreischutzSpell extends AbstractSpell {
    private final ResourceLocation spellId = new ResourceLocation("more_iss", "freischutz");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.EPIC)
            .setSchoolResource(SchoolRegistry.ENDER_RESOURCE)
            .setMaxLevel(3)
            .setCooldownSeconds(45)
            .build();

    public FreischutzSpell() {
        this.manaCostPerLevel = 40;
        this.baseSpellPower = 30;
        this.spellPowerPerLevel = 10;
        this.castTime = 0;
        this.baseManaCost = 150;
    }

    // ─── ツールチップおよび銘刻台GUIでの表示設定 ─────────────────────────────
    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        double power = this.baseSpellPower + ((spellLevel - 1) * this.spellPowerPerLevel);

        if (caster != null) {
            power = getSpellPower(spellLevel, caster);
        }

        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(power, 1)),
                Component.translatable("ui.more_iss.freischutz.contract_duration", Utils.stringTruncation(getContractDurationSeconds(spellLevel), 1))
        );
    }

    private float getContractDurationSeconds(int spellLevel) {
        return 10.0f - ((spellLevel - 1) * 1.5f);
    }

    @Override public CastType getCastType() { return CastType.INSTANT; }
    @Override public DefaultConfig getDefaultConfig() { return defaultConfig; }
    @Override public ResourceLocation getSpellResource() { return spellId; }
    @Override public int getRecastCount(int spellLevel, @Nullable LivingEntity entity) { return 7; }
    @Override public Optional<SoundEvent> getCastFinishSound() { return Optional.of(SoundRegistry.MAGIC_ARROW_RELEASE.get()); }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        // 「報い」デバフ（最大体力低下）がかかっている間は、再発動不可（警告メッセージのみ、即死ダメージはなし）
        if (entity.hasEffect(ModEffects.RETRIBUTION.get())) {
            if (!level.isClientSide) {
                entity.sendSystemMessage(Component.translatable("ui.more_iss.freischutz.penalty_trigger"));
            }
            return false;
        }

        return Utils.preCastTargetHelper(level, entity, playerMagicData, this, 32, .35f);
    }

    // ─── コアロジック ────────────────────────────────────────────
    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        var recasts = playerMagicData.getPlayerRecasts();
        int durationTicks = (int) (getContractDurationSeconds(spellLevel) * 20);

        // 初回発動時：リキャストインスタンスを生成し、悪魔の契約デバフを付与
        if (!recasts.hasRecastForSpell(getSpellId())) {
            recasts.addRecast(new RecastInstance(getSpellId(), spellLevel, getRecastCount(spellLevel, entity), durationTicks, castSource, null), playerMagicData);
            entity.addEffect(new MobEffectInstance(ModEffects.DEMONIC_COVENANT.get(), durationTicks, 0, false, false, true));
        }
        // 2発目以降のリキャスト処理中
        else {
            RecastInstance recastInstance = recasts.getRecastInstance(getSpellId());
            // 💡 解決：「悪魔の契約デバフがまだ残っている期間中」かつ「これが最後の1発」の時だけ報いを下す
            if (recastInstance != null && recastInstance.getRemainingRecasts() <= 1) {
                if (entity.hasEffect(ModEffects.DEMONIC_COVENANT.get())) {
                    if (!level.isClientSide) {
                        // 契約デバフを消去し、最大体力低下の「報い」デバフ（1分間=1200ticks）を付与
                        entity.removeEffect(ModEffects.DEMONIC_COVENANT.get());
                        entity.addEffect(new MobEffectInstance(ModEffects.RETRIBUTION.get(), 1200, 0));
                        entity.sendSystemMessage(Component.translatable("ui.more_iss.freischutz.exhausted_penalty"));
                    }
                }
            }
        }

        // 矢の生成・発射
        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            LivingEntity target = null;
            if (playerMagicData.getAdditionalCastData() instanceof TargetEntityCastData castTargetingData) {
                target = castTargetingData.getTarget(serverLevel);
            }

            MagicArrowProjectile magicArrow = new MagicArrowProjectile(level, entity);
            magicArrow.setPos(entity.position().add(0, entity.getEyeHeight() - magicArrow.getBoundingBox().getYsize() * .5f, 0).add(entity.getForward()));
            magicArrow.setDamage(getSpellPower(spellLevel, entity));

            if (target != null && target.isAlive()) {
                net.minecraft.world.phys.Vec3 direction = target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(magicArrow.position()).normalize();
                magicArrow.shoot(direction);
                magicArrow.addTag("freischutz_target_id:" + target.getId());
            } else {
                magicArrow.shoot(entity.getLookAngle());
            }

            magicArrow.addTag("more_iss.freischutz_bullet");
            level.addFreshEntity(magicArrow);
        }

        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    // ─── リキャスト終了時の処理 ────────────────────────────────────
    @Override
    public void onRecastFinished(ServerPlayer player, RecastInstance recastInstance, RecastResult result, ICastDataSerializable castData) {
        // 弾を残したまま契約時間が終了（TIMEOUT）した場合も、契約を解除して「報い」デバフを付与
        if (result == RecastResult.TIMEOUT) {
            if (player.hasEffect(ModEffects.DEMONIC_COVENANT.get())) {
                player.removeEffect(ModEffects.DEMONIC_COVENANT.get());
                player.addEffect(new MobEffectInstance(ModEffects.RETRIBUTION.get(), 1200, 0));
            }
        }
        super.onRecastFinished(player, recastInstance, result, castData);
    }
}