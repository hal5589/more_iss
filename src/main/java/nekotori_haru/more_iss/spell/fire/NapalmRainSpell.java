package nekotori_haru.more_iss.spell.fire;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import nekotori_haru.more_iss.More_iss;
import nekotori_haru.more_iss.entity.NapalmBombEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

public class NapalmRainSpell extends AbstractSpell {

    // 🌟 【修正ポイント】1.20.6以降の推奨形式である fromNamespaceAndPath に適合させつつ、IDはそのまま維持
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(More_iss.MODID, "napalm_rain");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(SchoolRegistry.FIRE_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(20)
            .setAllowCrafting(true)
            .build();

    public NapalmRainSpell() {
        this.baseSpellPower = 1;
        this.spellPowerPerLevel = 1;
        this.manaCostPerLevel = 20;
        this.baseManaCost = 60;
        this.castTime = 20;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(getRadius(spellLevel, caster), 1)),
                // 🌟 【修正ポイント】ISS既存の「飛翔物の数」公式キーへ差し替え。変数やメソッド（getBombCount）は完全維持しています
                Component.translatable("ui.irons_spellbooks.projectile_count", getBombCount(spellLevel, caster))
        );
    }

    @Override public DefaultConfig getDefaultConfig() { return defaultConfig; }
    @Override public CastType getCastType() { return CastType.LONG; }
    @Override public ResourceLocation getSpellResource() { return spellId; }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundRegistry.FIREBALL_START.get());
    }

    @Override
    public void onCast(Level world, int spellLevel, LivingEntity caster, CastSource castSource, MagicData magicData) {
        if (!world.isClientSide && world instanceof ServerLevel) {
            int count = getBombCount(spellLevel, caster);
            float damage = getDamage(spellLevel, caster);
            float radius = getRadius(spellLevel, caster);

            for (int i = 0; i < count; i++) {
                // キャスターの周囲にランダムな水平方向へ放物線を描いて飛ばす
                double angle = world.random.nextDouble() * Math.PI * 2;
                double horizontalSpeed = 0.2 + world.random.nextDouble() * 0.3;
                double verticalSpeed = 0.3 + world.random.nextDouble() * 0.25;

                Vec3 velocity = new Vec3(
                        Math.cos(angle) * horizontalSpeed,
                        verticalSpeed,
                        Math.sin(angle) * horizontalSpeed
                );

                NapalmBombEntity bomb = new NapalmBombEntity(world, caster);
                bomb.setPos(caster.getX(), caster.getY() + 1.0, caster.getZ());
                bomb.setDeltaMovement(velocity);
                bomb.setDamage(damage);
                bomb.setExplosionRadius(radius);
                world.addFreshEntity(bomb);
            }
        }
        super.onCast(world, spellLevel, caster, castSource, magicData);
    }

    public float getDamage(int spellLevel, LivingEntity caster) {
        return 3 + 4 * getSpellPower(spellLevel, caster);
    }

    public float getRadius(int spellLevel, LivingEntity caster) {
        return 2.0f + getSpellPower(spellLevel, caster) * 0.5f;
    }

    public int getBombCount(int spellLevel, LivingEntity caster) {
        return 4 + (int) (getSpellPower(spellLevel, caster) * 2);
    }
}