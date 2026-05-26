package nekotori_haru.more_iss.spell.ice;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import nekotori_haru.more_iss.More_iss;
import nekotori_haru.more_iss.registry.ModEffects;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

public class FrostArmorSpell extends AbstractSpell {
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(More_iss.MODID, "frost_armor");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(SchoolRegistry.ICE_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(30)
            .build();

    public FrostArmorSpell() {
        this.manaCostPerLevel = 20;
        this.baseSpellPower = 6;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 40;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        float conversionRatePercent = 100.0f + (spellLevel - 1) * 10.0f;
        int durationTicks = getDurationWithPower(spellLevel, caster);

        return List.of(
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(durationTicks, 1)),
                Component.literal("変換率: " + Utils.stringTruncation(conversionRatePercent, 1) + "%")
        );
    }

    @Override public CastType getCastType() { return CastType.INSTANT; }
    @Override public DefaultConfig getDefaultConfig() { return defaultConfig; }
    @Override public ResourceLocation getSpellResource() { return spellId; }
    @Override public Optional<SoundEvent> getCastFinishSound() { return Optional.of(SoundRegistry.ICE_BLOCK_CAST.get()); }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (!level.isClientSide) {
            // 🌟 識別バグ対策：レベルを100倍（レベル5なら500）にして、通常のバフ計算から隔離する
            int secureAmplifier = spellLevel * 100;
            int duration = getDurationWithPower(spellLevel, entity);

            entity.addEffect(new MobEffectInstance(ModEffects.FROST_ARMOR.get(), duration, secureAmplifier, false, false, true));
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    public static int getDurationWithPower(int spellLevel, LivingEntity caster) {
        float power = new FrostArmorSpell().getSpellPower(spellLevel, caster);
        int baseDuration = 400 + (spellLevel - 1) * 100;
        int powerBonus = (int)(power * 10);
        return baseDuration + powerBonus;
    }
}