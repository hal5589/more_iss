package nekotori_haru.more_iss.recipe;

import com.google.gson.*;
import nekotori_haru.more_iss.blockentity.ArcaneCraftingTableBlockEntity;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;

public class ArcaneCraftingRecipe implements Recipe<ArcaneCraftingTableBlockEntity.RecipeWrapper> {

    public static final String ID = "arcane_crafting";

    private final ResourceLocation id;
    private final NonNullList<Ingredient> ingredients;
    private final Ingredient catalyst;
    private final boolean catalystConsume;
    private final ItemStack result;

    public ArcaneCraftingRecipe(ResourceLocation id,
                                NonNullList<Ingredient> ingredients,
                                Ingredient catalyst,
                                boolean catalystConsume,
                                ItemStack result) {
        this.id = id;
        this.ingredients = ingredients;
        this.catalyst = catalyst;
        this.catalystConsume = catalystConsume;
        this.result = result;
    }

    @Override
    public boolean matches(ArcaneCraftingTableBlockEntity.RecipeWrapper inv, Level level) {
        // 円形スロット(0-7)のチェック
        for (int i = 0; i < 8; i++) {
            if (!ingredients.get(i).test(inv.getItem(i))) {
                return false;
            }
        }

        // 触媒スロット(9)のチェック
        if (this.catalyst != null && this.catalyst != Ingredient.EMPTY) {
            if (this.catalyst.getItems().length > 0) {
                if (!this.catalyst.test(inv.getItem(9))) {
                    return false;
                }
            }
        }

        // 出力スロット(8)のチェック
        ItemStack outputSlot = inv.getItem(8);
        ItemStack resultItem = this.result;

        if (!outputSlot.isEmpty() &&
                !ItemStack.isSameItemSameTags(outputSlot, resultItem)) {
            return false;
        }

        return true;
    }

    @Override
    public ItemStack assemble(ArcaneCraftingTableBlockEntity.RecipeWrapper inv, RegistryAccess access) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess access) {
        return result;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        // ⭕ 修正: More_iss を経由せず、内部のクラスから直接シリアライザーを返します
        return ArcaneCraftingRecipeSerializer.INSTANCE;
    }

    @Override
    public RecipeType<?> getType() {
        // ⭕ 修正: More_iss を経由せず、専用のTypeクラスから直接タイプを返します
        return ArcaneCraftingRecipeType.INSTANCE;
    }

    public boolean isCatalystConsumed() {
        return catalystConsume;
    }

    // =========================================================================
    //  Serializer (JSONパース処理)
    // =========================================================================
    public static class ArcaneCraftingRecipeSerializer implements RecipeSerializer<ArcaneCraftingRecipe> {
        public static final ArcaneCraftingRecipeSerializer INSTANCE = new ArcaneCraftingRecipeSerializer();

        @Override
        public ArcaneCraftingRecipe fromJson(ResourceLocation id, JsonObject json) {
            JsonArray ingredientsArray = GsonHelper.getAsJsonArray(json, "ingredients");
            NonNullList<Ingredient> ingredients = NonNullList.withSize(8, Ingredient.EMPTY);

            for (int i = 0; i < 8; i++) {
                if (i < ingredientsArray.size()) {
                    JsonElement elem = ingredientsArray.get(i);
                    if (elem.isJsonObject()) {
                        JsonObject obj = elem.getAsJsonObject();
                        if (obj.entrySet().isEmpty()) {
                            ingredients.set(i, Ingredient.EMPTY);
                        } else {
                            ingredients.set(i, Ingredient.fromJson(obj));
                        }
                    } else if (elem.isJsonPrimitive()) {
                        String itemName = elem.getAsString();
                        if (itemName.equals("minecraft:air") || itemName.equals("air")) {
                            ingredients.set(i, Ingredient.EMPTY);
                        } else {
                            ResourceLocation itemId = new ResourceLocation(itemName);
                            var item = ForgeRegistries.ITEMS.getValue(itemId);
                            if (item != null && item != Items.AIR) {
                                ingredients.set(i, Ingredient.of(item));
                            } else {
                                ingredients.set(i, Ingredient.EMPTY);
                            }
                        }
                    }
                }
            }

            Ingredient catalyst = Ingredient.EMPTY;
            if (json.has("catalyst")) {
                JsonElement catalystElem = json.get("catalyst");
                if (catalystElem.isJsonObject()) {
                    catalyst = Ingredient.fromJson(catalystElem);
                }
            }
            boolean catalystConsume = GsonHelper.getAsBoolean(json, "catalyst_consume", false);

            JsonObject resultObj = GsonHelper.getAsJsonObject(json, "result");
            ResourceLocation itemId = new ResourceLocation(GsonHelper.getAsString(resultObj, "item"));
            int count = GsonHelper.getAsInt(resultObj, "count", 1);
            var item = ForgeRegistries.ITEMS.getValue(itemId);
            ItemStack result = (item != null && item != Items.AIR) ? new ItemStack(item, count) : ItemStack.EMPTY;

            return new ArcaneCraftingRecipe(id, ingredients, catalyst, catalystConsume, result);
        }

        @Nullable
        @Override
        public ArcaneCraftingRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            NonNullList<Ingredient> ingredients = NonNullList.withSize(8, Ingredient.EMPTY);
            for (int i = 0; i < 8; i++) {
                ingredients.set(i, Ingredient.fromNetwork(buf));
            }
            Ingredient catalyst = Ingredient.fromNetwork(buf);
            boolean catalystConsume = buf.readBoolean();
            ItemStack result = buf.readItem();
            return new ArcaneCraftingRecipe(id, ingredients, catalyst, catalystConsume, result);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, ArcaneCraftingRecipe recipe) {
            for (Ingredient ing : recipe.ingredients) {
                ing.toNetwork(buf);
            }
            recipe.catalyst.toNetwork(buf);
            buf.writeBoolean(recipe.catalystConsume);
            buf.writeItem(recipe.result);
        }
    }
}