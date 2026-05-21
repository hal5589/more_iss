package nekotori_haru.more_iss.recipe;

import com.google.gson.*;
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
import nekotori_haru.more_iss.blockentity.ArcaneCraftingTableBlockEntity;

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
        // ★修正: this. を付けて正しくフィールドに代入
        this.id = id;
        this.ingredients = ingredients;
        this.catalyst = catalyst;
        this.catalystConsume = catalystConsume;
        this.result = result;
    }

    @Override
    public boolean matches(ArcaneCraftingTableBlockEntity.RecipeWrapper inv, Level level) {
        for (int i = 0; i < 8; i++) {
            Ingredient ing = ingredients.get(i);
            ItemStack itemInSlot = inv.getItem(i);

            if (ing.isEmpty() || ing.test(ItemStack.EMPTY)) {
                if (!itemInSlot.isEmpty()) return false;
            } else {
                if (!ing.test(itemInSlot)) return false;
            }
        }
        if (!catalyst.isEmpty()) {
            if (!catalyst.test(inv.getItem(9))) return false;
        }
        return true;
    }

    @Override
    public ItemStack assemble(ArcaneCraftingTableBlockEntity.RecipeWrapper inv, RegistryAccess registryAccess) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int w, int h) { return true; }

    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) {
        return result;
    }

    @Override
    public ResourceLocation getId() { return id; }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ArcaneCraftingRecipeSerializer.INSTANCE;
    }

    @Override
    public RecipeType<?> getType() {
        return ArcaneCraftingRecipeType.INSTANCE;
    }

    public NonNullList<Ingredient> getIngredients() { return ingredients; }
    public Ingredient getCatalyst() { return catalyst; }
    public boolean isCatalystConsumed() { return catalystConsume; }

    public static class ArcaneCraftingRecipeSerializer implements RecipeSerializer<ArcaneCraftingRecipe> {

        public static final ArcaneCraftingRecipeSerializer INSTANCE = new ArcaneCraftingRecipeSerializer();

        @Override
        public ArcaneCraftingRecipe fromJson(ResourceLocation id, JsonObject json) {
            JsonArray ingArray = GsonHelper.getAsJsonArray(json, "ingredients");
            NonNullList<Ingredient> ingredients = NonNullList.withSize(8, Ingredient.of(Items.AIR));
            for (int i = 0; i < Math.min(ingArray.size(), 8); i++) {
                JsonElement el = ingArray.get(i);
                if (el.isJsonObject() && el.getAsJsonObject().size() == 0) {
                    ingredients.set(i, Ingredient.of(Items.AIR));
                } else {
                    ingredients.set(i, Ingredient.fromJson(el));
                }
            }

            Ingredient catalyst = Ingredient.EMPTY;
            if (json.has("catalyst")) {
                catalyst = Ingredient.fromJson(json.get("catalyst"));
            }

            boolean catalystConsume = GsonHelper.getAsBoolean(json, "catalyst_consume", false);

            JsonObject resultObj = GsonHelper.getAsJsonObject(json, "result");
            ResourceLocation itemId = new ResourceLocation(GsonHelper.getAsString(resultObj, "item"));
            int count = GsonHelper.getAsInt(resultObj, "count", 1);
            ItemStack result = new ItemStack(ForgeRegistries.ITEMS.getValue(itemId), count);

            return new ArcaneCraftingRecipe(id, ingredients, catalyst, catalystConsume, result);
        }

        @Nullable
        @Override
        public ArcaneCraftingRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            NonNullList<Ingredient> ingredients = NonNullList.withSize(8, Ingredient.of(Items.AIR));
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