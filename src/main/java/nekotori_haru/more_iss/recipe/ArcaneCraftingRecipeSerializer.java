package nekotori_haru.more_iss.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

public class ArcaneCraftingRecipeSerializer implements RecipeSerializer<ArcaneCraftingRecipe> {

    public static final ArcaneCraftingRecipeSerializer INSTANCE = new ArcaneCraftingRecipeSerializer();

    private ArcaneCraftingRecipeSerializer() {}

    @Override
    public ArcaneCraftingRecipe fromJson(ResourceLocation id, JsonObject json) {
        JsonArray arr = GsonHelper.getAsJsonArray(json, "ingredients");
        NonNullList<Ingredient> ingredients = NonNullList.withSize(9, Ingredient.EMPTY);

        // ⭕ 各スロットの拡張パラメータを格納する動的リスト
        List<ResourceLocation> requiredSpellIds = new ArrayList<>();
        List<Integer> requiredSpellLevels = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            requiredSpellIds.add(null);
            requiredSpellLevels.add(0);
        }

        for (int i = 0; i < 9 && i < arr.size(); i++) {
            JsonElement elem = arr.get(i);
            if (elem.isJsonObject()) {
                JsonObject obj = elem.getAsJsonObject();
                if (obj.entrySet().isEmpty()) {
                    ingredients.set(i, Ingredient.EMPTY);
                } else {
                    ingredients.set(i, Ingredient.fromJson(obj));

                    // ⭕ カスタムプロパティ "required_spell" と "required_level" の解析
                    if (obj.has("required_spell")) {
                        requiredSpellIds.set(i, new ResourceLocation(GsonHelper.getAsString(obj, "required_spell")));
                        requiredSpellLevels.set(i, GsonHelper.getAsInt(obj, "required_level", 1));
                    }
                }
            } else if (elem.isJsonPrimitive()) {
                ingredients.set(i, Ingredient.fromJson(elem));
            }
        }

        Ingredient catalyst = Ingredient.EMPTY;
        if (json.has("catalyst")) {
            JsonElement ce = json.get("catalyst");
            if (ce.isJsonObject()) catalyst = Ingredient.fromJson(ce);
        }

        boolean catalystConsume = GsonHelper.getAsBoolean(json, "catalyst_consume", false);
        ItemStack result = ItemStack.EMPTY;
        ResourceLocation spellId = null;
        int spellLevel = 1;

        if (json.has("result_spell")) {
            JsonObject spellObj = GsonHelper.getAsJsonObject(json, "result_spell");
            spellId = new ResourceLocation(GsonHelper.getAsString(spellObj, "id"));
            spellLevel = GsonHelper.getAsInt(spellObj, "level", 1);
        } else if (json.has("result")) {
            JsonObject resultObj = GsonHelper.getAsJsonObject(json, "result");
            String itemId = GsonHelper.getAsString(resultObj, "item");
            int count = GsonHelper.getAsInt(resultObj, "count", 1);
            var item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemId));
            if (item != null && item != net.minecraft.world.item.Items.AIR) result = new ItemStack(item, count);
        }

        return new ArcaneCraftingRecipe(id, ingredients, requiredSpellIds, requiredSpellLevels,
                catalyst, catalystConsume, result, spellId, spellLevel);
    }

    @Override
    public ArcaneCraftingRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        NonNullList<Ingredient> ingredients = NonNullList.withSize(size, Ingredient.EMPTY);
        for (int i = 0; i < size; i++) {
            ingredients.set(i, Ingredient.fromNetwork(buf));
        }

        // ⭕ ネットワーク経由での拡張パラメータ復元
        List<ResourceLocation> requiredSpellIds = new ArrayList<>();
        List<Integer> requiredSpellLevels = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            if (buf.readBoolean()) {
                requiredSpellIds.add(buf.readResourceLocation());
                requiredSpellLevels.add(buf.readVarInt());
            } else {
                requiredSpellIds.add(null);
                requiredSpellLevels.add(0);
            }
        }

        Ingredient catalyst = Ingredient.fromNetwork(buf);
        boolean catalystConsume = buf.readBoolean();
        ItemStack result = buf.readItem();

        ResourceLocation spellId = null;
        int spellLevel = 1;
        if (buf.readBoolean()) {
            spellId = buf.readResourceLocation();
            spellLevel = buf.readVarInt();
        }

        return new ArcaneCraftingRecipe(id, ingredients, requiredSpellIds, requiredSpellLevels,
                catalyst, catalystConsume, result, spellId, spellLevel);
    }

    @Override
    public void toNetwork(FriendlyByteBuf buf, ArcaneCraftingRecipe recipe) {
        buf.writeVarInt(recipe.getIngredients().size());
        for (Ingredient ing : recipe.getIngredients()) {
            ing.toNetwork(buf);
        }

        // ⭕ ネットワーク経由での拡張パラメータ送信
        for (int i = 0; i < recipe.getIngredients().size(); i++) {
            ResourceLocation rId = recipe.getRequiredSpellIds().get(i);
            if (rId != null) {
                buf.writeBoolean(true);
                buf.writeResourceLocation(rId);
                buf.writeVarInt(recipe.getRequiredSpellLevels().get(i));
            } else {
                buf.writeBoolean(false);
            }
        }

        recipe.getCatalyst().toNetwork(buf);
        buf.writeBoolean(recipe.isCatalystConsumed());
        buf.writeItem(recipe.getResult());

        if (recipe.getSpellId() != null) {
            buf.writeBoolean(true);
            buf.writeResourceLocation(recipe.getSpellId());
            buf.writeVarInt(recipe.getSpellLevel());
        } else {
            buf.writeBoolean(false);
        }
    }
}