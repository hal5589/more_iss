package nekotori_haru.more_iss.mixin;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.gui.arcane_anvil.ArcaneAnvilMenu;
import io.redspace.ironsspellbooks.item.Scroll;
import nekotori_haru.more_iss.More_iss;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// あなたのModのメインクラスが「More_iss」または「MoreIssMod」等、環境に合わせて適宜インポートしてください
// import nekotori_haru.more_iss.More_iss;

@Mixin(ArcaneAnvilMenu.class)
public class MixinArcaneAnvilMenu_overburst {
    @Inject(method = "createResult", at = @At("TAIL"))
    private void onCreateResult(CallbackInfo ci) {
        ArcaneAnvilMenu self = (ArcaneAnvilMenu) (Object) this;

        Container inputSlots = ((ItemCombinerMenuAccessor) self).getInputSlots();
        createRecipe(inputSlots, SpellRegistry.CHARGE_SPELL.get(), 3, SpellRegistry.HEARTSTOP_SPELL.get(), 5, More_iss.OVERBURST_BLOOD.get(), 1);
    }

    @Unique
    private void createRecipe(Container inputSlots, AbstractSpell spell1, int spell1Level, AbstractSpell spell2, int spell2Level, AbstractSpell outputSpell, int outputSpellLevel) {
        ArcaneAnvilMenu self = (ArcaneAnvilMenu) (Object) this;

        ItemStack slotItem1 = inputSlots.getItem(0);
        ItemStack slotItem2 = inputSlots.getItem(1);

        if (slotItem1.isEmpty() || slotItem2.isEmpty()) return;

        if (slotItem1.getItem() instanceof Scroll && slotItem2.getItem() instanceof Scroll) {
            SpellData spellData1 = ISpellContainer.get(slotItem1).getSpellAtIndex(0);
            SpellData spellData2 = ISpellContainer.get(slotItem2).getSpellAtIndex(0);

            if (spellData1.getSpell().equals(spell1) && spellData2.getSpell().equals(spell2)) {
                if (!(spellData1.getLevel() == spell1Level || spellData2.getLevel() == spell2Level)) return;
                ItemStack result = slotItem1.copy();
                result.setCount(1);
                ISpellContainer.createScrollContainer(outputSpell, outputSpellLevel, result);
                ((ItemCombinerMenuAccessor) self).getResultSlots().setItem(0, result);
            }
        }
    }
}