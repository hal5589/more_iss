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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class FreischutzSpell extends AbstractSpell {

    private final ResourceLocation spellId = new ResourceLocation("more_iss", "freischutz");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.EPIC)
            .setSchoolResource(SynthesisSchoolRegistry.SYNTHESIS_RESOURCE) // 🔥変更
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
        double power = (caster != null)
                ? getSpellPower(spellLevel, caster)
                : this.baseSpellPower + ((spellLevel - 1) * this.spellPowerPerLevel);

        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(power, 1))
        );
    }

    @Override public CastType getCastType() { return CastType.INSTANT; }
    @Override public DefaultConfig getDefaultConfig() { return defaultConfig; }
    @Override public ResourceLocation getSpellResource() { return spellId; }
    @Override public int getRecastCount(int spellLevel, @Nullable LivingEntity entity) { return 7; }
    @Override public Optional<SoundEvent> getCastFinishSound() { return Optional.of(SoundRegistry.MAGIC_ARROW_RELEASE.get()); }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData data) {

        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {

            LivingEntity target = null;
            if (data.getAdditionalCastData() instanceof TargetEntityCastData t) {
                target = t.getTarget(serverLevel);
            }

            MagicArrowProjectile arrow = new MagicArrowProjectile(level, entity);
            arrow.setPos(entity.getEyePosition());
            arrow.setDamage(getSpellPower(spellLevel, entity)); // 🔥融合パワー

            if (target != null) {
                arrow.setHomingTarget(target);
            }

            level.addFreshEntity(arrow);
        }

        super.onCast(level, spellLevel, entity, castSource, data);
    }
}