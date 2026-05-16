package nekotori_haru.more_iss.jei;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.recipe.vanilla.IJeiAnvilRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

@JeiPlugin
public class MoreIssJeiPlugin implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid() {
        return new ResourceLocation("more_iss", "jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        // 💡 クラス名を正しい MoreIssAnvilCategory に修正
        registration.addRecipeCategories(new MoreIssAnvilCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        // ─── レシピ1: オーバーバーストブラッド（元コードのまま完璧に維持） ───
        ItemStack chargeScroll = createScrollWithSpell(SpellRegistry.CHARGE_SPELL.get(), 3);
        ItemStack heartstopScroll = createScrollWithSpell(SpellRegistry.HEARTSTOP_SPELL.get(), 5);
        ItemStack outputScroll = createScrollWithSpell(SpellRegistry.getSpell(new ResourceLocation("more_iss", "overburst_blood")), 1);

        if (!chargeScroll.isEmpty() && !heartstopScroll.isEmpty() && !outputScroll.isEmpty()) {
            IJeiAnvilRecipe myCustomAnvilRecipe = registration.getVanillaRecipeFactory().createAnvilRecipe(
                    chargeScroll,
                    List.of(heartstopScroll),
                    List.of(outputScroll)
            );
            registration.addRecipes(MoreIssAnvilCategory.TYPE, List.of(myCustomAnvilRecipe));
        }

        // ─── レシピ2: サクリファシアル・エッジ（別ファイル呼び出し、こちらもスペルを修正） ───
        MoreIssAnvilCategory_SACRIFICIAL_EDGE.register(registration);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        net.minecraft.world.item.Item arcaneAnvilItem = ForgeRegistries.ITEMS.getValue(
                new ResourceLocation("irons_spellbooks", "arcane_anvil")
        );
        if (arcaneAnvilItem != null && arcaneAnvilItem != Items.AIR) {
            registration.addRecipeCatalyst(new ItemStack(arcaneAnvilItem), MoreIssAnvilCategory.TYPE);
        }
    }

    private ItemStack createScrollWithSpell(AbstractSpell spell, int level) {
        if (spell == null || spell.getSpellId().equals("irons_spellbooks:none")) {
            return ItemStack.EMPTY;
        }
        net.minecraft.world.item.Item scrollItem = ForgeRegistries.ITEMS.getValue(new ResourceLocation("irons_spellbooks", "scroll"));
        if (scrollItem == null || scrollItem == Items.AIR) return ItemStack.EMPTY;
        ItemStack stack = new ItemStack(scrollItem, 1);
        ISpellContainer.createScrollContainer(spell, level, stack);
        return stack;
    }
}