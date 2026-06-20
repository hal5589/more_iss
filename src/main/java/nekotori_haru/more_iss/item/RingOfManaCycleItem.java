package nekotori_haru.more_iss.item;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.UUID;

public class RingOfManaCycleItem extends Item implements ICurioItem {

    private static final UUID MANA_REGEN_UUID = UUID.fromString("a1b2c3d4-5e6f-7a8b-9c0d-1e2f3a4b5c6d");
    private static final UUID SPELL_POWER_UUID = UUID.fromString("b2c3d4e5-6f7a-8b9c-0d1e-2f3a4b5c6d7e");
    private static final UUID COOLDOWN_UUID = UUID.fromString("c3d4e5f6-7a8b-9c0d-1e2f-3a4b5c6d7e8f");

    public RingOfManaCycleItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        return slotContext.identifier().equals("ring");
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(
            SlotContext slotContext, UUID uuid, ItemStack stack) {

        Multimap<Attribute, AttributeModifier> modifiers = ArrayListMultimap.create();

        modifiers.put(AttributeRegistry.MANA_REGEN.get(),
                new AttributeModifier(MANA_REGEN_UUID,
                        "mana_cycle_regen",
                        1.0,
                        AttributeModifier.Operation.MULTIPLY_BASE));

        modifiers.put(AttributeRegistry.SPELL_POWER.get(),
                new AttributeModifier(SPELL_POWER_UUID,
                        "mana_cycle_power",
                        -0.15,
                        AttributeModifier.Operation.MULTIPLY_BASE));

        modifiers.put(AttributeRegistry.COOLDOWN_REDUCTION.get(),
                new AttributeModifier(COOLDOWN_UUID,
                        "mana_cycle_cooldown",
                        -1.0,
                        AttributeModifier.Operation.MULTIPLY_BASE));

        return modifiers;
    }
}