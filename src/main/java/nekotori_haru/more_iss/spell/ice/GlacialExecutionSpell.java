package nekotori_haru.more_iss.spell.ice;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.Utils;
import nekotori_haru.more_iss.More_iss;
import nekotori_haru.more_iss.entity.GlacialSwordEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

public class GlacialExecutionSpell extends AbstractSpell {
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(More_iss.MODID, "glacial_execution");
    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.LEGENDARY)
            .setSchoolResource(SchoolRegistry.ICE_RESOURCE)
            .setMaxLevel(1)
            .setCooldownSeconds(45)
            .setAllowCrafting(true)
            .build();

    public GlacialExecutionSpell() {
        this.baseSpellPower = 15;
        this.baseManaCost = 100;
        this.spellPowerPerLevel = 3;
        this.manaCostPerLevel = 10;
        this.castTime = 40;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 1))
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
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.GLASS_BREAK);
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundEvents.ENDER_DRAGON_FLAP);
    }

    @Override
    public void onCast(Level world, int spellLevel, LivingEntity caster, CastSource castSource, MagicData magicData) {
        if (!world.isClientSide && world instanceof ServerLevel) {
            float damage = getDamage(spellLevel, caster);

            More_iss.LOGGER.info("GlacialExecutionSpell - SpellLevel: {}, Damage: {}", spellLevel, damage);

            Vec3 spawnPos = new Vec3(
                    caster.getX(),
                    caster.getY() + 3.5,
                    caster.getZ()
            );

            GlacialSwordEntity sword = new GlacialSwordEntity(world, caster);
            sword.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
            sword.setYRot(caster.getYRot());
            sword.setTargetXRot(0.0F);
            sword.setDamage(damage);

            world.addFreshEntity(sword);
        }
        super.onCast(world, spellLevel, caster, castSource, magicData);
    }

    // ⭐ NapalmRainSpell を参考にしたダメージ計算
    public float getDamage(int spellLevel, LivingEntity caster) {
        // 基本ダメージ + (レベル × スケーリング) + スペルパワー補正
        // 掛け算ではなく足し算で補正する
        float spellPower = getSpellPower(spellLevel, caster);
        return this.baseSpellPower + (spellLevel * this.spellPowerPerLevel) + spellPower;
    }
}