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
        // 🌟 新仕様：100%スタートでレベルごとに+10%の固定値
        float conversionRatePercent = 100.0f + (spellLevel - 1) * 10.0f;

        // 持続時間の計算（魔法威力依存のロマン仕様）
        int durationTicks = getDurationWithPower(spellLevel, caster);

        return List.of(
                // 1. 持続時間の表示（本家と同じく timeFromTicks を使用し、小数点以下1桁に丸める）
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(durationTicks, 1)),

                // 2. 変換率の表示
                // 🌟 本家の手法（stringTruncation）を取り入れつつ、言語ファイル不要で絶対にバグらないように literal で結合
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
            // エフェクトのレベル（amplifier）には、スペルレベル（0スタート）を引き渡す
            int effectAmplifier = spellLevel - 1;

            // 魔法威力が反映された動的な持続時間を取得
            int duration = getDurationWithPower(spellLevel, entity);

            // プレイヤーに氷の鎧バフを付与
            entity.addEffect(new MobEffectInstance(ModEffects.FROST_ARMOR.get(), duration, effectAmplifier, false, false, true));
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    // 持続時間の計算式（ベース20秒 + レベル毎5秒 + 魔法威力×10 ticks [0.5秒]）
    public static int getDurationWithPower(int spellLevel, LivingEntity caster) {
        float power = new FrostArmorSpell().getSpellPower(spellLevel, caster);
        int baseDuration = 400 + (spellLevel - 1) * 100; // レベルによる基礎秒数
        int powerBonus = (int)(power * 10); // 魔法威力ボーナス
        return baseDuration + powerBonus;
    }
}