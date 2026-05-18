package nekotori_haru.more_iss.jei;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import mezz.jei.api.recipe.vanilla.IJeiAnvilRecipe;
import mezz.jei.api.registration.IRecipeRegistration;
import nekotori_haru.more_iss.More_iss;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

public class MoreIssAnvilCategory_FREISCHUTZ {

    public static void register(IRecipeRegistration registration) {
        ItemStack leftInput = createScrollWithSpell(SpellRegistry.MAGIC_ARROW_SPELL.get(), 10);
        ItemStack rightInput = createScrollWithSpell(SpellRegistry.MAGIC_MISSILE_SPELL.get(), 10);
        ItemStack output = createScrollWithSpell(More_iss.FREISCHUTZ.get(), 1);

        if (!leftInput.isEmpty() && !rightInput.isEmpty() && !output.isEmpty()) {
            IJeiAnvilRecipe recipe = registration.getVanillaRecipeFactory().createAnvilRecipe(
                    leftInput,
                    List.of(rightInput),
                    List.of(output)
            );

            registration.addRecipes(MoreIssAnvilCategory.TYPE, List.of(recipe));
        }
    }

    private static ItemStack createScrollWithSpell(AbstractSpell spell, int level) {
        // 💡 修正：getSpellId() が String 型であるため、直接文字列の比較を行います
        if (spell == null || spell.getSpellId() == null || spell.getSpellId().isEmpty() || spell.getSpellId().contains("none")) {
            return ItemStack.EMPTY;
        }
        net.minecraft.world.item.Item scrollItem = ForgeRegistries.ITEMS.getValue(new ResourceLocation("irons_spellbooks", "scroll"));
        if (scrollItem == null || scrollItem == Items.AIR) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = new ItemStack(scrollItem, 1);
        ISpellContainer.createScrollContainer(spell, level, stack);
        return stack;
    }
}