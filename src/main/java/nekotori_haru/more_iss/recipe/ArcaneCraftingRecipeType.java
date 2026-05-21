package nekotori_haru.more_iss.recipe;

import net.minecraft.world.item.crafting.RecipeType;

public class ArcaneCraftingRecipeType implements RecipeType<ArcaneCraftingRecipe> {

    public static final ArcaneCraftingRecipeType INSTANCE = new ArcaneCraftingRecipeType();

    private ArcaneCraftingRecipeType() {}

    @Override
    public String toString() {
        return "more_iss:arcane_crafting";
    }
}