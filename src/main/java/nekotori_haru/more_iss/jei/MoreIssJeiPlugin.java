package nekotori_haru.more_iss.jei;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import nekotori_haru.more_iss.More_iss;
import nekotori_haru.more_iss.recipe.ArcaneCraftingRecipe;
import nekotori_haru.more_iss.recipe.ArcaneCraftingRecipeType;
import nekotori_haru.more_iss.registry.ModBlocks;

import java.util.ArrayList;
import java.util.List;

@JeiPlugin
@SuppressWarnings("deprecation")
public class MoreIssJeiPlugin implements IModPlugin {

    private static final ResourceLocation PLUGIN_ID = new ResourceLocation(More_iss.MODID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_ID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper guiHelper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(new ArcaneCraftingCategory(guiHelper));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        if (Minecraft.getInstance().level == null) return;

        List<ArcaneCraftingRecipe> recipes = new ArrayList<>();
        try {
            RecipeManager rm = Minecraft.getInstance().level.getRecipeManager();
            for (Recipe<?> raw : rm.getRecipes()) {
                if (raw instanceof ArcaneCraftingRecipe r) recipes.add(r);
            }
            if (recipes.isEmpty()) {
                List<ArcaneCraftingRecipe> fallback =
                        rm.getAllRecipesFor(ArcaneCraftingRecipeType.INSTANCE);
                if (fallback != null) recipes.addAll(fallback);
            }
            registration.addRecipes(ArcaneCraftingCategory.TYPE, recipes);
        } catch (Throwable t) {
            More_iss.LOGGER.error("[more_iss] JEI recipe registration failed", t);
        }
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(
                new ItemStack(ModBlocks.ARCANE_CRAFTING_TABLE_ITEM.get()),
                ArcaneCraftingCategory.TYPE);
    }

    // -------------------------------------------------------
    // カテゴリクラス
    // -------------------------------------------------------
    public static class ArcaneCraftingCategory implements IRecipeCategory<ArcaneCraftingRecipe> {

        public static final RecipeType<ArcaneCraftingRecipe> TYPE =
                RecipeType.create(More_iss.MODID, "arcane_crafting", ArcaneCraftingRecipe.class);

        private static final int[][] CIRCLE_POS = {
                { 80, 14  },  // 0: 上
                { 120, 31 },  // 1: 右上
                { 136, 71 },  // 2: 右
                { 120, 111},  // 3: 右下
                { 80, 127 },  // 4: 下
                { 40, 111 },  // 5: 左下
                { 23, 71  },  // 6: 左
                { 40, 31  },  // 7: 左上
        };
        private static final int CENTER_X  = 80;
        private static final int CENTER_Y  = 71;
        private static final int CATALYST_X = 152;
        private static final int CATALYST_Y = 127;

        private static final int OUTPUT_X  = 80;
        private static final int OUTPUT_Y  = 96;

        private final IDrawable background;
        private final IDrawable icon;

        public ArcaneCraftingCategory(IGuiHelper guiHelper) {
            ResourceLocation texture = new ResourceLocation(
                    More_iss.MODID, "textures/gui/arcane_crafting_table.png");
            this.background = guiHelper.createDrawable(texture, 0, 0, 176, 150);
            this.icon = guiHelper.createDrawableItemStack(
                    new ItemStack(ModBlocks.ARCANE_CRAFTING_TABLE_ITEM.get()));
        }

        @Override
        public RecipeType<ArcaneCraftingRecipe> getRecipeType() { return TYPE; }

        @Override
        public Component getTitle() {
            return Component.translatable("container.more_iss.arcane_crafting");
        }

        @Override
        public IDrawable getBackground() { return background; }

        @Override
        public IDrawable getIcon() { return icon; }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder,
                              ArcaneCraftingRecipe recipe,
                              IFocusGroup focuses) {
            try {
                NonNullList<Ingredient> ingredients  = recipe.getIngredients();
                NonNullList<String>     reqSpells    = recipe.getRequiredSpells();
                NonNullList<Integer>    reqLevels    = recipe.getRequiredLevels();

                for (int i = 0; i <= 8; i++) {
                    int[] pos = (i < 8) ? CIRCLE_POS[i] : new int[]{ CENTER_X, CENTER_Y };

                    if (i >= ingredients.size() || ingredients.get(i).isEmpty()) continue;

                    String  spellId = (reqSpells != null && i < reqSpells.size())
                            ? reqSpells.get(i) : null;
                    int     spellLv = (reqLevels != null && i < reqLevels.size()
                            && reqLevels.get(i) != null)
                            ? reqLevels.get(i) : 1;

                    List<ItemStack> stacks = buildIngredientStacks(
                            ingredients.get(i), spellId, spellLv);

                    builder.addSlot(RecipeIngredientRole.INPUT, pos[0], pos[1])
                            .addItemStacks(stacks);
                }

                if (!recipe.getCatalyst().isEmpty()) {
                    builder.addSlot(RecipeIngredientRole.INPUT, CATALYST_X, CATALYST_Y)
                            .addIngredients(recipe.getCatalyst());
                }

                ItemStack output = buildOutputStack(recipe);
                if (!output.isEmpty()) {
                    builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_X, OUTPUT_Y)
                            .addItemStack(output);
                }

            } catch (Throwable t) {
                More_iss.LOGGER.error("[more_iss] JEI layout error", t);
            }
        }

        // ── 動的ロジック ───────────────────────────────────
        private static List<ItemStack> buildIngredientStacks(
                Ingredient ingredient, String spellId, int spellLevel) {

            ItemStack[] raw = ingredient.getItems();
            if (raw == null || raw.length == 0) return List.of(ItemStack.EMPTY);

            if (spellId == null || spellId.isEmpty()) {
                return List.of(raw[0].copy());
            }

            List<ItemStack> result = new ArrayList<>();
            for (ItemStack base : raw) {
                if (base == null || base.isEmpty()) continue;
                ItemStack stack = base.copy();
                if (stack.is(ItemRegistry.SCROLL.get())) {
                    injectSpell(stack, new ResourceLocation(spellId), spellLevel);
                }
                result.add(stack);
            }
            return result.isEmpty() ? List.of(ItemStack.EMPTY) : result;
        }

        private static ItemStack buildOutputStack(ArcaneCraftingRecipe recipe) {
            String resultSpellId = recipe.getResultSpellId();
            if (resultSpellId != null && !resultSpellId.isEmpty()) {
                int level = recipe.getResultSpellLevel();
                ItemStack scroll = new ItemStack(ItemRegistry.SCROLL.get());
                injectSpell(scroll, new ResourceLocation(resultSpellId), level);
                return scroll;
            }

            ItemStack item = recipe.getResultItem(
                    Minecraft.getInstance().level.registryAccess());
            return item != null ? item : ItemStack.EMPTY;
        }

        private static void injectSpell(ItemStack scroll,
                                        ResourceLocation spellId, int level) {
            try {
                AbstractSpell spell = SpellRegistry.getSpell(spellId);
                if (spell != null) {
                    ISpellContainer.createScrollContainer(spell, level, scroll);
                }
            } catch (Throwable t) {
                More_iss.LOGGER.warn("[more_iss] Failed to inject spell {} into scroll: {}",
                        spellId, t.getMessage());
            }
        }
    }
}