package nekotori_haru.more_iss.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.vanilla.IJeiAnvilRecipe;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

public class MoreIssAnvilCategory implements IRecipeCategory<IJeiAnvilRecipe> {
    // あなたのMod専用の「秘術の金床」レシピタイプを独自に定義
    public static final RecipeType<IJeiAnvilRecipe> TYPE = RecipeType.create("more_iss", "arcane_anvil", IJeiAnvilRecipe.class);

    private final IDrawable background;
    private final IDrawable icon;

    public MoreIssAnvilCategory(IGuiHelper guiHelper) {
        // Minecraft標準の金床GUI画像を直接指定
        ResourceLocation minecraftAnvilGui = new ResourceLocation("minecraft", "textures/gui/container/anvil.png");

        // anvil.pngの中から、スロットが綺麗に収まる部分を切り出す
        this.background = guiHelper.createDrawable(minecraftAnvilGui, 15, 44, 145, 22);

        // カテゴリのアイコンを「秘術の金床」に固定
        net.minecraft.world.item.Item arcaneAnvil = ForgeRegistries.ITEMS.getValue(new ResourceLocation("irons_spellbooks", "arcane_anvil"));
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(arcaneAnvil != null ? arcaneAnvil : net.minecraft.world.item.Items.ANVIL));
    }

    @Override
    public RecipeType<IJeiAnvilRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        // 💡 変更：ハードコードを廃止し、言語ファイルから動的に読み込むように設定
        return Component.translatable("container.more_iss.arcane_anvil_crafting");
    }

    @Override
    public IDrawable getBackground() {
        return this.background;
    }

    @Override
    public IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, IJeiAnvilRecipe recipe, IFocusGroup focuses) {
        // 切り出した背景のスロット座標に合わせて、アイテムスロットの判定位置を配置
        builder.addSlot(RecipeIngredientRole.INPUT, 12, 3).addItemStacks(recipe.getLeftInputs());
        builder.addSlot(RecipeIngredientRole.INPUT, 61, 3).addItemStacks(recipe.getRightInputs());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 119, 3).addItemStacks(recipe.getOutputs());
    }
}