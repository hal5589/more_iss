package nekotori_haru.more_iss.spell.blood;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import nekotori_haru.more_iss.registry.ModEffects;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;

@AutoSpellConfig
public class OverburstBloodSpell extends AbstractSpell {
    private final ResourceLocation spellId = new ResourceLocation("more_iss", "overburst_blood");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.LEGENDARY)
            .setSchoolResource(SchoolRegistry.BLOOD_RESOURCE)
            .setMaxLevel(3)
            .setCooldownSeconds(120)
            .setAllowCrafting(false)
            .build();

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        float spellPower = getSpellPower(spellLevel, caster);

        // レベルごとの基礎倍率 (Lv1:3.0, Lv2:5.0, Lv3:7.0)
        float baseMultiplier = 1.0f + (spellLevel * 2.0f);
        int totalDamagePercent = (int) (spellPower * baseMultiplier * 100);

        // 貫通割合 (Lv1:20%, Lv2:40%, Lv3:60%)
        int penetrationPercent = spellLevel * 20;

        return List.of(
                Component.translatable("ui.more_iss.damage_multiplier", totalDamagePercent),
                Component.translatable("ui.more_iss.penetration_ratio", penetrationPercent),
                Component.translatable("ui.irons_spellbooks.effect_length", Utils.timeFromTicks(200, 1))
        );
    }

    public OverburstBloodSpell() {
        this.baseSpellPower = 1;
        this.baseManaCost = 400;
        this.manaCostPerLevel = 200;
        this.castTime = 40;
    }

    @Override
    public ResourceLocation getSpellResource() {
        return spellId;
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
    public void onCast(Level world, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (!world.isClientSide) {
            entity.setHealth(1.0f);

            // 💡 修正：ハートストップを10秒（200 ticks）固定に
            int heartstopDuration = 200;
            entity.addEffect(new MobEffectInstance(MobEffectRegistry.HEARTSTOP.get(), heartstopDuration, 0));

            // UIと同じ「魔法威力 × 基礎倍率」の％数値を計算
            float spellPower = getSpellPower(spellLevel, entity);
            float baseMultiplier = 1.0f + (spellLevel * 2.0f);
            int totalDamagePercent = (int) (spellPower * baseMultiplier * 100);

            // データパッキングしてエフェクト付与（10秒 = 200 ticks 固定）
            int packedData = (totalDamagePercent * 10) + spellLevel;
            entity.addEffect(new MobEffectInstance(ModEffects.OVERBURST.get(), 200, packedData));

            world.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                    SoundRegistry.HEARTSTOP_CAST.get(), entity.getSoundSource(), 2.0f, 0.8f);
        }
        super.onCast(world, spellLevel, entity, castSource, playerMagicData);
    }
    }