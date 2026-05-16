package nekotori_haru.more_iss.mixin;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.gui.arcane_anvil.ArcaneAnvilMenu;
import io.redspace.ironsspellbooks.item.Scroll;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;

@Mixin(value = ArcaneAnvilMenu.class, remap = false)
public class MixinArcaneAnvilMenu {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String SPELL_BASE       = "irons_spellbooks:electrify";
    private static final int    LEVEL_BASE       = 3;
    private static final String SPELL_ADD        = "irons_spellbooks:heartstop";
    private static final int    LEVEL_ADD        = 5;
    private static final String SPELL_OUTPUT_STR = "more_iss:overburst_blood";
    private static final int    LEVEL_OUTPUT     = 1;
    private static final ResourceLocation SPELL_OUTPUT_RL = new ResourceLocation("more_iss", "overburst_blood");

    private static Field inputSlotsField  = null;
    private static Field resultSlotsField = null;

    static {
        try {
            inputSlotsField  = ItemCombinerMenu.class.getDeclaredField("inputSlots");
            resultSlotsField = ItemCombinerMenu.class.getDeclaredField("resultSlots");
            inputSlotsField.setAccessible(true);
            resultSlotsField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            LOGGER.error("[more_iss] MixinArcaneAnvilMenu: failed to reflect fields", e);
        }
    }

    @Inject(method = "createResult", at = @At("TAIL"))
    private void more_iss$injectCreateResult(CallbackInfo ci) {
        if (inputSlotsField == null || resultSlotsField == null) return;

        try {
            Container inputSlots  = (Container) inputSlotsField.get(this);
            Container resultSlots = (Container) resultSlotsField.get(this);

            ItemStack left  = inputSlots.getItem(0);
            ItemStack right = inputSlots.getItem(1);

            if (left.isEmpty() || right.isEmpty()) return;
            if (!(left.getItem() instanceof Scroll)) return;
            if (!(right.getItem() instanceof Scroll)) return;

            SpellData leftSpell  = ISpellContainer.get(left).getSpellAtIndex(0);
            SpellData rightSpell = ISpellContainer.get(right).getSpellAtIndex(0);

            if (leftSpell == null || rightSpell == null) return;

            AbstractSpell ls = leftSpell.getSpell();
            AbstractSpell rs = rightSpell.getSpell();
            if (ls == null || rs == null) return;

            String leftId  = ls.getSpellId();
            String rightId = rs.getSpellId();

            boolean match =
                    (SPELL_BASE.equals(leftId)  && leftSpell.getLevel()  == LEVEL_BASE
                            && SPELL_ADD.equals(rightId)   && rightSpell.getLevel() == LEVEL_ADD)
                            ||
                            (SPELL_ADD.equals(leftId)    && leftSpell.getLevel()  == LEVEL_ADD
                                    && SPELL_BASE.equals(rightId)  && rightSpell.getLevel() == LEVEL_BASE);

            if (!match) return;

            AbstractSpell outputSpell = SpellRegistry.getSpell(SPELL_OUTPUT_RL);
            if (outputSpell == null) {
                LOGGER.error("[more_iss] MixinArcaneAnvilMenu: spell not found: {}", SPELL_OUTPUT_RL);
                return;
            }

            ItemStack output = new ItemStack(ItemRegistry.SCROLL.get());
            ISpellContainer.createScrollContainer(outputSpell, LEVEL_OUTPUT, output);
            resultSlots.setItem(0, output);

        } catch (IllegalAccessException e) {
            LOGGER.error("[more_iss] MixinArcaneAnvilMenu: reflect access failed", e);
        }
    }
}