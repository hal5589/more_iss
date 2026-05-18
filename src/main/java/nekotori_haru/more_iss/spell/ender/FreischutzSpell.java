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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FreischutzSpell extends AbstractSpell {
    private final ResourceLocation spellId = new ResourceLocation("more_iss", "freischutz");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.EPIC)
            .setSchoolResource(SchoolRegistry.ENDER_RESOURCE)
            .setMaxLevel(3)
            .setCooldownSeconds(45)
            .setAllowCrafting(false)
            .build();

    public FreischutzSpell() {
        this.manaCostPerLevel = 40;
        this.baseSpellPower = 30;
        this.spellPowerPerLevel = 10;
        this.castTime = 0;
        this.baseManaCost = 150;
    }

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
        if (entity.hasEffect(ModEffects.RETRIBUTION.get())) {
            if (!level.isClientSide) {
                entity.sendSystemMessage(Component.translatable("ui.more_iss.freischutz.penalty_trigger"));
            }
            return false;
        }

        return Utils.preCastTargetHelper(level, entity, playerMagicData, this, 32, .35f);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        var recasts = playerMagicData.getPlayerRecasts();
        int durationTicks = (int) (getContractDurationSeconds(spellLevel) * 20);

        if (!recasts.hasRecastForSpell(getSpellId())) {
            recasts.addRecast(new RecastInstance(getSpellId(), spellLevel, getRecastCount(spellLevel, entity), durationTicks, castSource, null), playerMagicData);

            MobEffectInstance contract = new MobEffectInstance(ModEffects.DEMONIC_COVENANT.get(), durationTicks, 0, false, false, true);
            contract.setCurativeItems(new ArrayList<ItemStack>());
            entity.addEffect(contract);
        }
        else {
            RecastInstance recastInstance = recasts.getRecastInstance(getSpellId());
            if (recastInstance != null && recastInstance.getRemainingRecasts() <= 1) {
                if (entity.hasEffect(ModEffects.DEMONIC_COVENANT.get())) {
                    if (!level.isClientSide) {
                        entity.removeEffect(ModEffects.DEMONIC_COVENANT.get());

                        MobEffectInstance retribution = new MobEffectInstance(ModEffects.RETRIBUTION.get(), 1200, 0);
                        retribution.setCurativeItems(new ArrayList<ItemStack>());
                        entity.addEffect(retribution);

                        if (!entity.hasEffect(ModEffects.RETRIBUTION.get())) {
                            entity.setHealth(0.0f);
                            entity.hurt(level.damageSources().fellOutOfWorld(), Float.MAX_VALUE);
                            entity.sendSystemMessage(Component.translatable("ui.more_iss.freischutz.bypass_penalty"));
                        } else {
                            entity.sendSystemMessage(Component.translatable("ui.more_iss.freischutz.exhausted_penalty"));
                        }
                    }
                }
            }
        }

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

                // 💡 追記：矢にターゲットを設定して自動追尾（ホーミング）を有効化
                magicArrow.setHomingTarget(target);
            } else {
                magicArrow.shoot(entity.getLookAngle());
            }

            magicArrow.addTag("more_iss.freischutz_bullet");
            level.addFreshEntity(magicArrow);
        }

        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public void onRecastFinished(ServerPlayer player, RecastInstance recastInstance, RecastResult result, ICastDataSerializable castData) {
        if (result == RecastResult.TIMEOUT) {
            if (player.hasEffect(ModEffects.DEMONIC_COVENANT.get())) {
                player.removeEffect(ModEffects.DEMONIC_COVENANT.get());

                MobEffectInstance retribution = new MobEffectInstance(ModEffects.RETRIBUTION.get(), 1200, 0);
                retribution.setCurativeItems(new ArrayList<ItemStack>());
                player.addEffect(retribution);

                if (!player.hasEffect(ModEffects.RETRIBUTION.get())) {
                    player.setHealth(0.0f);
                    player.hurt(player.level().damageSources().fellOutOfWorld(), Float.MAX_VALUE);
                    player.sendSystemMessage(Component.translatable("ui.more_iss.freischutz.bypass_penalty"));
                }
            }
        }
        super.onRecastFinished(player, recastInstance, result, castData);
    }
}