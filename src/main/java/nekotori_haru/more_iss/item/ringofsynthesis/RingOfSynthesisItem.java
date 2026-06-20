package nekotori_haru.more_iss.item.ringofsynthesis;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import nekotori_haru.more_iss.More_iss;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.UUID;

public class RingOfSynthesisItem extends Item implements ICurioItem {

    private static final double MULTIPLIER = 0.10; // 5%

    // Iron's Spells 'n Spellbooks の全属性リスト
    private static final Attribute[] ATTRIBUTES = {
            AttributeRegistry.FIRE_SPELL_POWER.get(),
            AttributeRegistry.ICE_SPELL_POWER.get(),
            AttributeRegistry.LIGHTNING_SPELL_POWER.get(),
            AttributeRegistry.ENDER_SPELL_POWER.get(),
            AttributeRegistry.HOLY_SPELL_POWER.get(),
            AttributeRegistry.NATURE_SPELL_POWER.get(),
            AttributeRegistry.BLOOD_SPELL_POWER.get(),
            AttributeRegistry.EVOCATION_SPELL_POWER.get(),
            AttributeRegistry.ELDRITCH_SPELL_POWER.get()
    };

    public RingOfSynthesisItem(Properties properties) {
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

        for (Attribute attribute : ATTRIBUTES) {
            if (attribute != null) {
                modifiers.put(attribute, new AttributeModifier(
                        uuid,
                        More_iss.MODID + ":ring_of_synthesis",
                        MULTIPLIER,
                        AttributeModifier.Operation.MULTIPLY_BASE
                ));
            }
        }
        return modifiers;
    }


}