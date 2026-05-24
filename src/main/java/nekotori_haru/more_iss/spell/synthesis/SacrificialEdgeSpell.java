package nekotori_haru.more_iss.spell.synthesis;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.capabilities.magic.RecastInstance;
import io.redspace.ironsspellbooks.entity.spells.blood_slash.BloodSlashProjectile;
import nekotori_haru.more_iss.registry.ModEffects;
import nekotori_haru.more_iss.registry.SynthesisSchoolRegistry;
import net.minecraft.network.chat.*;
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
            .setSchoolResource(SynthesisSchoolRegistry.SYNTHESIS_RESOURCE) // 🔥変更
            .setMaxLevel(5)
            .setCooldownSeconds(60)
            .setAllowCrafting(false)
            .build();

    private final double base = 2.0;
    private final double perLevel = 0.5;

    public SacrificialEdgeSpell() {
        this.baseSpellPower = 0;
        this.spellPowerPerLevel = 0;
    }

    private double getCustom(int level) {
        return base + ((level - 1) * perLevel);
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int level, LivingEntity caster) {
        double power = (caster != null)
                ? getCustom(level) * getSpellPower(level, caster)
                : getCustom(level);

        return List.of(
                Component.literal("Damage: " + power)
        );
    }

    @Override public DefaultConfig getDefaultConfig() { return defaultConfig; }
    @Override public CastType getCastType() { return CastType.INSTANT; }
    @Override public ResourceLocation getSpellResource() { return spellId; }

    @Override
    public int getRecastCount(int level, @Nullable LivingEntity entity) {
        return 2;
    }

    @Override
    public void onCast(Level world, int level, LivingEntity entity, CastSource source, MagicData data) {

        var recasts = data.getPlayerRecasts();

        if (!recasts.hasRecastForSpell(getSpellId())) {

            recasts.addRecast(new RecastInstance(getSpellId(), level, 2, 600, source, null), data);

            if (!world.isClientSide) {
                entity.addEffect(new MobEffectInstance(ModEffects.SACRIFICIAL_BLEED.get(), 72000, 0));
            }

        } else {

            int hits = entity.getEffect(ModEffects.SACRIFICIAL_BLEED.get()).getAmplifier();

            float damage = (float) Math.max(
                    5.0,
                    (hits * getCustom(level)) * getSpellPower(level, entity) // 🔥完全統一
            );

            if (!world.isClientSide) {
                BloodSlashProjectile slash = new BloodSlashProjectile(world, entity);
                slash.setPos(entity.getEyePosition());
                slash.shoot(entity.getLookAngle());
                slash.setDamage(damage);
                world.addFreshEntity(slash);
            }

            entity.removeEffect(ModEffects.SACRIFICIAL_BLEED.get());
        }

        super.onCast(world, level, entity, source, data);
    }
}