package nekotori_haru.more_iss.effect;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraftforge.common.ForgeMod;

import java.util.UUID;

public class ManaFurnaceEffect extends MobEffect {

    public static final UUID SPELL_POWER_UUID =
            UUID.fromString("fa7c1d3e-2b09-4a56-8f3d-1c7e9b0a2d4f");
    public static final UUID COOLDOWN_UUID =
            UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
    public static final UUID MANA_REGEN_UUID =
            UUID.fromString("b2c3d4e5-f6a7-8901-bcde-f23456789012");

    private static final double BASE_SPELL_POWER = 3.0;
    private static final double BASE_COOLDOWN = 10000.0;

    private static final double PER_LEVEL_SPELL_POWER = 1.0;
    private static final double PER_LEVEL_COOLDOWN = 10000.0;

    public ManaFurnaceEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xFF6A00);
    }

    @Override
    public void addAttributeModifiers(LivingEntity entity, AttributeMap attributeMap, int amplifier) {
        addModifier(attributeMap, AttributeRegistry.SPELL_POWER.get(),
                SPELL_POWER_UUID, getSpellPowerBonus(amplifier));

        addModifier(attributeMap, AttributeRegistry.COOLDOWN_REDUCTION.get(),
                COOLDOWN_UUID, getCooldownBonus(amplifier));

    }

    @Override
    public void removeAttributeModifiers(LivingEntity entity, AttributeMap attributeMap, int amplifier) {
        removeModifier(attributeMap, AttributeRegistry.SPELL_POWER.get(), SPELL_POWER_UUID);
        removeModifier(attributeMap, AttributeRegistry.COOLDOWN_REDUCTION.get(), COOLDOWN_UUID);
        removeModifier(attributeMap, AttributeRegistry.MANA_REGEN.get(), MANA_REGEN_UUID);
    }

    private void addModifier(AttributeMap map, Attribute attribute, UUID uuid, double value) {
        var instance = map.getInstance(attribute);
        if (instance != null && instance.getModifier(uuid) == null) {
            instance.addPermanentModifier(
                    new AttributeModifier(uuid, "Mana Furnace", value, AttributeModifier.Operation.MULTIPLY_BASE)
            );
        }
    }

    private void removeModifier(AttributeMap map, Attribute attribute, UUID uuid) {
        var instance = map.getInstance(attribute);
        if (instance != null) {
            instance.removeModifier(uuid);
        }
    }

    private double getSpellPowerBonus(int amplifier) {
        return BASE_SPELL_POWER + amplifier * PER_LEVEL_SPELL_POWER;
    }

    private double getCooldownBonus(int amplifier) {
        return BASE_COOLDOWN + amplifier * PER_LEVEL_COOLDOWN;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return false;
    }
}