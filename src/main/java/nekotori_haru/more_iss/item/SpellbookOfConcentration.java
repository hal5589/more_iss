package nekotori_haru.more_iss.item;

import io.redspace.ironsspellbooks.api.item.curios.AffinityData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.item.SpellBook;
import io.redspace.ironsspellbooks.item.weapons.AttributeContainer;
import io.redspace.ironsspellbooks.util.ItemPropertiesHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import top.theillusivec4.curios.api.SlotContext;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SpellbookOfConcentration extends SpellBook {

    public SpellbookOfConcentration() {
        super(1, ItemPropertiesHelper.equipment()
                .stacksTo(1)
                .fireResistant()
                .rarity(Rarity.RARE));

        // 装備時効果（属性）
        this.withSpellbookAttributes(
                new AttributeContainer(AttributeRegistry.SPELL_POWER, 0.50,
                        AttributeModifier.Operation.MULTIPLY_BASE),
                new AttributeContainer(AttributeRegistry.COOLDOWN_REDUCTION, 0.30,
                        AttributeModifier.Operation.MULTIPLY_BASE),
                new AttributeContainer(AttributeRegistry.MAX_MANA, 300.0,
                        AttributeModifier.Operation.ADDITION)
        );
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        super.curioTick(slotContext, stack);

        // この魔導書が装備されている間、収録魔法のレベルを+3する
        applyLevelBoost(stack);
    }

    /**
     * 収録された魔法のレベルを+3する処理
     */
    private void applyLevelBoost(ItemStack stack) {
        ISpellContainer spellbookData = ISpellContainer.get(stack);

        if (spellbookData == null || spellbookData.isEmpty()) {
            if (!AffinityData.NONE.equals(AffinityData.getAffinityData(stack))) {
                AffinityData.set(stack, AffinityData.NONE);
            }
            return;
        }

        // 収録されている全魔法のResourceLocationとレベル+3をマップに変換
        Map<ResourceLocation, Integer> dynamicBonuses = spellbookData.getActiveSpells().stream()
                .filter(slot -> slot.getSpell() != null)
                .collect(Collectors.toMap(
                        slot -> slot.getSpell().getSpellResource(),
                        slot -> 3,  // +3
                        (existing, replacement) -> existing
                ));

        AffinityData nextAffinity = new AffinityData(dynamicBonuses);

        // 現在のAffinityDataと異なる場合のみ更新
        if (!nextAffinity.equals(AffinityData.getAffinityData(stack))) {
            AffinityData.set(stack, nextAffinity);
        }
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);

        tooltip.add(Component.translatable("item.more_iss.spellbook_of_concentration.boost")
                .withStyle(ChatFormatting.GREEN));
    }
}