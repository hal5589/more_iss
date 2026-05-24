package nekotori_haru.more_iss.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.registries.ForgeRegistries;

public class ArcaneCraftingRecipeSerializer implements RecipeSerializer<ArcaneCraftingRecipe> {

    public static final ArcaneCraftingRecipeSerializer INSTANCE = new ArcaneCraftingRecipeSerializer();

    public ArcaneCraftingRecipeSerializer() {}

    @Override
    public ArcaneCraftingRecipe fromJson(ResourceLocation id, JsonObject json) {
        JsonArray arr = GsonHelper.getAsJsonArray(json, "ingredients");
        NonNullList<Ingredient> ingredients = NonNullList.withSize(9, Ingredient.EMPTY);
        NonNullList<CompoundTag> ingredientNBTs = NonNullList.withSize(9, new CompoundTag());
        NonNullList<String> requiredSpells = NonNullList.withSize(9, "");
        NonNullList<Integer> requiredLevels = NonNullList.withSize(9, 1);

        for (int i = 0; i < 9 && i < arr.size(); i++) {
            JsonElement elem = arr.get(i);
            if (elem.isJsonObject()) {
                JsonObject obj = elem.getAsJsonObject();
                if (!obj.entrySet().isEmpty()) {
                    if (obj.has("item")) {
                        ingredients.set(i, Ingredient.fromJson(obj));
                    }
                    if (obj.has("nbt")) {
                        try {
                            ingredientNBTs.set(i, TagParser.parseTag(obj.get("nbt").getAsString()));
                        } catch (Exception e) {
                            System.err.println("Failed to parse NBT: " + obj.get("nbt").getAsString());
                        }
                    }
                    if (obj.has("required_spell")) {
                        requiredSpells.set(i, obj.get("required_spell").getAsString());
                    }
                    if (obj.has("required_level")) {
                        requiredLevels.set(i, obj.get("required_level").getAsInt());
                    }
                }
            }
        }

        Ingredient catalyst = Ingredient.EMPTY;
        CompoundTag catalystNBT = new CompoundTag();
        if (json.has("catalyst")) {
            JsonElement ce = json.get("catalyst");
            if (ce.isJsonObject()) {
                JsonObject catalystObj = ce.getAsJsonObject();
                catalyst = Ingredient.fromJson(catalystObj);
                if (catalystObj.has("nbt")) {
                    try {
                        catalystNBT = TagParser.parseTag(catalystObj.get("nbt").getAsString());
                    } catch (Exception e) {
                        System.err.println("Failed to parse catalyst NBT: " + catalystObj.get("nbt").getAsString());
                    }
                }
            }
        }

        boolean catalystConsume = GsonHelper.getAsBoolean(json, "catalyst_consume", false);

        String resultSpellId = "";
        int resultSpellLevel = 1;
        ItemStack result = ItemStack.EMPTY;

        if (json.has("result_spell")) {
            JsonObject spellObj = json.getAsJsonObject("result_spell");
            resultSpellId = GsonHelper.getAsString(spellObj, "id");
            resultSpellLevel = GsonHelper.getAsInt(spellObj, "level", 1);
        } else if (json.has("result")) {
            JsonObject resultObj = GsonHelper.getAsJsonObject(json, "result");
            String itemId = GsonHelper.getAsString(resultObj, "item");
            int count = GsonHelper.getAsInt(resultObj, "count", 1);
            var item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemId));
            result = (item != null && item != net.minecraft.world.item.Items.AIR)
                    ? new ItemStack(item, count) : ItemStack.EMPTY;
        }

        return new ArcaneCraftingRecipe(id, ingredients, ingredientNBTs, requiredSpells, requiredLevels,
                catalyst, catalystNBT, catalystConsume, result, resultSpellId, resultSpellLevel);
    }

    @Override
    public ArcaneCraftingRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        NonNullList<Ingredient> ingredients = NonNullList.withSize(size, Ingredient.EMPTY);
        for (int i = 0; i < size; i++) ingredients.set(i, Ingredient.fromNetwork(buf));

        NonNullList<CompoundTag> ingredientNBTs = NonNullList.withSize(size, new CompoundTag());
        for (int i = 0; i < size; i++) {
            if (buf.readBoolean()) ingredientNBTs.set(i, buf.readNbt());
        }

        NonNullList<String> requiredSpells = NonNullList.withSize(size, "");
        NonNullList<Integer> requiredLevels = NonNullList.withSize(size, 1);
        for (int i = 0; i < size; i++) {
            requiredSpells.set(i, buf.readUtf());
            requiredLevels.set(i, buf.readInt());
        }

        Ingredient catalyst = Ingredient.fromNetwork(buf);
        CompoundTag catalystNBT = buf.readBoolean() ? buf.readNbt() : new CompoundTag();
        boolean catalystConsume = buf.readBoolean();
        ItemStack result = buf.readItem();
        String resultSpellId = buf.readUtf();
        int resultSpellLevel = buf.readInt();

        return new ArcaneCraftingRecipe(id, ingredients, ingredientNBTs, requiredSpells, requiredLevels,
                catalyst, catalystNBT, catalystConsume, result, resultSpellId, resultSpellLevel);
    }

    @Override
    public void toNetwork(FriendlyByteBuf buf, ArcaneCraftingRecipe recipe) {
        buf.writeVarInt(recipe.getIngredients().size());
        for (Ingredient ing : recipe.getIngredients()) ing.toNetwork(buf);

        for (CompoundTag nbt : recipe.getIngredientNBTs()) {
            buf.writeBoolean(!nbt.isEmpty());
            if (!nbt.isEmpty()) buf.writeNbt(nbt);
        }

        for (int i = 0; i < recipe.getRequiredSpells().size(); i++) {
            buf.writeUtf(recipe.getRequiredSpells().get(i));
            buf.writeInt(recipe.getRequiredLevels().get(i));
        }

        recipe.getCatalyst().toNetwork(buf);
        buf.writeBoolean(!recipe.getCatalystNBT().isEmpty());
        if (!recipe.getCatalystNBT().isEmpty()) buf.writeNbt(recipe.getCatalystNBT());
        buf.writeBoolean(recipe.isCatalystConsumed());
        buf.writeItem(recipe.getResultItem());
        buf.writeUtf(recipe.getResultSpellId());
        buf.writeInt(recipe.getResultSpellLevel());
    }
}