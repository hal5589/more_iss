package nekotori_haru.more_iss.recipe;

import com.google.gson.JsonObject;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class AnvilCraftRecipe implements Recipe<Container> {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, "more_iss");

    public static final RegistryObject<RecipeSerializer<AnvilCraftRecipe>> SERIALIZER =
            SERIALIZERS.register("anvil_craft", Serializer::new);

    public static final RecipeType<AnvilCraftRecipe> TYPE = new RecipeType<>() {
        @Override
        public String toString() { return "more_iss:anvil_craft"; }
    };

    private final ResourceLocation id;
    private final ItemStack baseIngredient;
    private final ItemStack modifierIngredient;
    private final ItemStack outputResult;

    public AnvilCraftRecipe(ResourceLocation id, ItemStack baseIngredient, ItemStack modifierIngredient, ItemStack outputResult) {
        this.id = id;
        this.baseIngredient = baseIngredient;
        this.modifierIngredient = modifierIngredient;
        this.outputResult = outputResult;
    }

    /**
     * 金床の2つのスロットのアイテムが、レシピの条件（IDおよびNBT）を満たしているかチェック
     */
    public boolean matchesItems(ItemStack slot1, ItemStack slot2) {
        return (isMatch(this.baseIngredient, slot1) && isMatch(this.modifierIngredient, slot2)) ||
                (isMatch(this.baseIngredient, slot2) && isMatch(this.modifierIngredient, slot1));
    }

    private boolean isMatch(ItemStack recipeStack, ItemStack inputStack) {
        if (recipeStack.getItem() != inputStack.getItem()) return false;
        // レシピ側にNBT指定がある場合のみ、入力アイテムのNBTと一致するか（部分一致/包含関係）を検証する
        if (recipeStack.hasTag()) {
            if (!inputStack.hasTag()) return false;
            CompoundTag recipeTag = recipeStack.getTag();
            CompoundTag inputTag = inputStack.getTag();
            if (recipeTag != null && inputTag != null) {
                // 入力されたNBTがレシピのNBTをすべて含んでいるかチェック
                for (String key : recipeTag.getAllKeys()) {
                    if (!recipeTag.get(key).equals(inputTag.get(key))) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public ItemStack getOutputCopy() {
        return this.outputResult.copy();
    }

    @Override public ResourceLocation getId() { return this.id; }
    @Override public RecipeSerializer<?> getSerializer() { return SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return TYPE; }

    @Override public boolean matches(Container pContainer, Level pLevel) { return false; }
    @Override public ItemStack assemble(Container pContainer, RegistryAccess pRegistryAccess) { return ItemStack.EMPTY; }
    @Override public boolean canCraftInDimensions(int pWidth, int pHeight) { return true; }
    @Override public ItemStack getResultItem(RegistryAccess pRegistryAccess) { return ItemStack.EMPTY; }

    // --- Serializer (ShapedRecipeなどのバニラ処理を参考にした安全なNBTパース) ---
    public static class Serializer implements RecipeSerializer<AnvilCraftRecipe> {

        @Override
        public AnvilCraftRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
            ItemStack base = parseItemStack(GsonHelper.getAsJsonObject(json, "base_item"));
            ItemStack modifier = parseItemStack(GsonHelper.getAsJsonObject(json, "modifier_item"));
            ItemStack output = parseItemStack(GsonHelper.getAsJsonObject(json, "output_item"));
            return new AnvilCraftRecipe(recipeId, base, modifier, output);
        }

        private ItemStack parseItemStack(JsonObject obj) {
            ResourceLocation itemId = new ResourceLocation(GsonHelper.getAsString(obj, "item"));
            net.minecraft.world.item.Item item = ForgeRegistries.ITEMS.getValue(itemId);
            int count = GsonHelper.getAsInt(obj, "count", 1);
            ItemStack stack = new ItemStack(item, count);

            if (obj.has("nbt")) {
                try {
                    // JSON内のNBT構造（オブジェクト/文字列問わず）をCompoundTagに直接変換してアイテムに付与
                    CompoundTag nbt = TagParser.parseTag(obj.get("nbt").toString());
                    stack.setTag(nbt);
                } catch (CommandSyntaxException e) {
                    throw new com.google.gson.JsonSyntaxException("Invalid NBT entry: " + e.getMessage());
                }
            }
            return stack;
        }

        @Override
        public AnvilCraftRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
            return new AnvilCraftRecipe(recipeId, buffer.readItem(), buffer.readItem(), buffer.readItem());
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, AnvilCraftRecipe recipe) {
            buffer.writeItem(recipe.baseIngredient);
            buffer.writeItem(recipe.modifierIngredient);
            buffer.writeItem(recipe.outputResult);
        }
    }
}