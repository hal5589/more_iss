package nekotori_haru.more_iss.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import nekotori_haru.more_iss.recipe.ArcaneCraftingRecipe;

public class ArcaneCraftingCategory implements IRecipeCategory<ArcaneCraftingRecipe> {

    public static final RecipeType<ArcaneCraftingRecipe> TYPE =
            RecipeType.create("more_iss", "arcane_crafting", ArcaneCraftingRecipe.class);

    private final IDrawable background;
    private final IDrawable icon;

    public ArcaneCraftingCategory(IGuiHelper guiHelper) {
        // ArcaneCraftingTableのGUIテクスチャを使う
        // サイズはarcane_crafting_table.pngに合わせて調整が必要
        ResourceLocation guiTexture = new ResourceLocation("more_iss",
                "textures/gui/arcane_crafting_table.png");
        this.background = guiHelper.createDrawable(guiTexture, 0, 0, 176, 166);

        ItemStack iconItem = new ItemStack(
                ForgeRegistries.ITEMS.getValue(
                        new ResourceLocation("more_iss", "arcane_crafting_table")));
        this.icon = guiHelper.createDrawableItemStack(iconItem);
    }

    @Override
    public RecipeType<ArcaneCraftingRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("container.more_iss.arcane_crafting");
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder,
                          ArcaneCraftingRecipe recipe,
                          IFocusGroup focuses) {
        // 3×3 クラフトグリッド（9スロット）
        var ingredients = recipe.getIngredients();
        for (int i = 0; i < 9; i++) {
            int col = i % 3;
            int row = i / 3;
            if (i < ingredients.size() && !ingredients.get(i).isEmpty()) {
                builder.addSlot(RecipeIngredientRole.INPUT, 30 + col * 18, 17 + row * 18)
                        .addIngredients(ingredients.get(i));
            }
        }

        // 触媒スロット（catalystConsumeがtrueならINPUT、falseならCATALYST扱い）
        if (!recipe.getCatalyst().isEmpty()) {
            builder.addSlot(RecipeIngredientRole.INPUT, 30 + 3 * 18 + 8, 35)
                    .addIngredients(recipe.getCatalyst());
        }

        // 結果スロット
        builder.addSlot(RecipeIngredientRole.OUTPUT, 124, 35)
                .addItemStack(recipe.getResultItem());
    }
}