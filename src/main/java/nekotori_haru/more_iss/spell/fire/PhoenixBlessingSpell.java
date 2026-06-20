package nekotori_haru.more_iss.spell.fire;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.Utils;
import nekotori_haru.more_iss.More_iss;
import nekotori_haru.more_iss.registry.ModEffects;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

@AutoSpellConfig
public class PhoenixBlessingSpell extends AbstractSpell {
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(More_iss.MODID, "phoenix_blessing");
    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.LEGENDARY)
            .setSchoolResource(SchoolRegistry.FIRE_RESOURCE)
            .setMaxLevel(3)
            .setCooldownSeconds(660)
            .setAllowCrafting(false)
            .build();

    public PhoenixBlessingSpell() {
        this.baseSpellPower = 1;
        this.baseManaCost = 300;
        this.spellPowerPerLevel = 0;
        this.manaCostPerLevel = 100;
        this.castTime = 20;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.revival_count", spellLevel),
                Component.translatable("ui.irons_spellbooks.duration", Utils.stringTruncation(getDurationSeconds(), 0))
        );
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return defaultConfig;
    }

    @Override
    public CastType getCastType() {
        return CastType.LONG;
    }

    @Override
    public ResourceLocation getSpellResource() {
        return spellId;
    }

    @Override
    public Optional<net.minecraft.sounds.SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.FIREWORK_ROCKET_LAUNCH);
    }

    @Override
    public Optional<net.minecraft.sounds.SoundEvent> getCastFinishSound() {
        return Optional.of(SoundEvents.TOTEM_USE);
    }

    @Override
    public void onCast(Level world, int spellLevel, LivingEntity caster, CastSource castSource, MagicData magicData) {
        if (world.isClientSide) return;

        // 継続時間: 10分 = 12000tick
        int duration = 12000;

        // アンプリファイア = レベル - 1（0=1回, 1=2回, 2=3回）
        int amplifier = spellLevel - 1;

        // 既存のエフェクトを削除して上書き
        caster.removeEffect(ModEffects.PHOENIX_BLESSING.get());

        // エフェクト付与
        caster.addEffect(new MobEffectInstance(ModEffects.PHOENIX_BLESSING.get(), duration, amplifier, false, true, true));

        // 効果音とパーティクル
        world.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0f, 1.0f);

        if (world instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    net.minecraft.core.particles.ParticleTypes.FLAME,
                    caster.getX(), caster.getY() + 1, caster.getZ(),
                    50, 0.5, 1, 0.5, 0.1
            );
            serverLevel.sendParticles(
                    net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,
                    caster.getX(), caster.getY() + 1, caster.getZ(),
                    30, 0.5, 1, 0.5, 0.1
            );
        }

        super.onCast(world, spellLevel, caster, castSource, magicData);
    }

    private int getDurationSeconds() {
        return 600;  // 10分
    }
}