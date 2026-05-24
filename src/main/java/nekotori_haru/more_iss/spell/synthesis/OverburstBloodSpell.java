package nekotori_haru.more_iss.spell.synthesis;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import nekotori_haru.more_iss.registry.ModEffects;
import nekotori_haru.more_iss.registry.SynthesisSchoolRegistry;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;

public class OverburstBloodSpell extends AbstractSpell {

    private final ResourceLocation spellId = new ResourceLocation("more_iss", "overburst_blood");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.LEGENDARY)
            .setSchoolResource(SynthesisSchoolRegistry.SYNTHESIS_RESOURCE)
            .setMaxLevel(3)
            .setCooldownSeconds(120)
            .setAllowCrafting(false)
            .build();

    public OverburstBloodSpell() {
        this.baseSpellPower = 1;
        this.baseManaCost = 400;
        this.manaCostPerLevel = 200;
        this.castTime = 40;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int level, LivingEntity caster) {
        float power = getSpellPower(level, caster);
        float multiplier = 1.0f + (level * 2.0f);
        int totalDamagePercent = (int)(power * multiplier * 100);

        return List.of(
                Component.translatable("ui.more_iss.damage_multiplier", totalDamagePercent)
        );
    }

    @Override public ResourceLocation getSpellResource() { return spellId; }
    @Override public DefaultConfig getDefaultConfig() { return defaultConfig; }
    @Override public CastType getCastType() { return CastType.LONG; }

    // 💡 修正：確実に存在するシンボルに差し替え、コンパイルエラーを解消
    @Override
    public AnimationHolder getCastStartAnimation() { return SpellAnimations.CHARGE_RAISED_HAND; }
    @Override
    public AnimationHolder getCastFinishAnimation() { return SpellAnimations.TOUCH_GROUND_ANIMATION; }

    @Override
    public void onCast(Level world, int level, LivingEntity entity, CastSource source, MagicData data) {

        if (!world.isClientSide) {
            // HPを1にする
            entity.setHealth(1.0f);

            float power = getSpellPower(level, entity);
            float multiplier = 1.0f + (level * 2.0f);

            // 1. 純粋なダメージ上昇％を算出 (例: 300)
            int totalDamagePercent = (int)(power * multiplier * 100);

            // 2. イベント側の解凍ルールに合わせてデータをパッキング
            // [ダメージ％] を10倍して上位桁にし、一の位に [現在の魔法レベル] を結合する
            // 例: ダメージ300% で レベル1 の場合 ──> 300 * 10 + 1 = 3001
            int packedData = (totalDamagePercent * 10) + level;

            // 3. 安全装置（通信のショート型上限 32767 を超えないように丸める）
            int safeAmplifier = Math.min(packedData, 32000);

            // イベントクラスが待つ特製パックデータをアンプリファイアに乗せて付与
            entity.addEffect(new MobEffectInstance(ModEffects.OVERBURST.get(), 200, safeAmplifier));
        }

        super.onCast(world, level, entity, source, data);
    }
}