package nekotori_haru.more_iss.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import nekotori_haru.more_iss.blockentity.ArcaneCraftingTableBlockEntity;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

public class ArcaneCraftingRecipe
        implements Recipe<ArcaneCraftingTableBlockEntity.RecipeWrapper> {

    public static final String ID = "arcane_crafting";

    private final ResourceLocation id;

    /**
     * ingredients[0-7] : 周囲8スロット（スロット0〜7）の材料
     * ingredients[8]   : 中央スロット（スロット8）に置く材料
     * 合計9個
     */
    private final NonNullList<Ingredient> ingredients;
    private final Ingredient catalyst;
    private final boolean    catalystConsume;
    private final ItemStack  result;

    public ArcaneCraftingRecipe(ResourceLocation id,
                                NonNullList<Ingredient> ingredients,
                                Ingredient catalyst,
                                boolean catalystConsume,
                                ItemStack result) {
        this.id              = id;
        this.ingredients     = ingredients;
        this.catalyst        = catalyst;
        this.catalystConsume = catalystConsume;
        this.result          = result;
    }

    // ─── matches ──────────────────────────────────────────────────────────
    // スロット0〜7: 周囲材料チェック
    // スロット8   : 中央材料チェック（入力として扱う）
    // スロット9   : 触媒チェック
    @Override
    public boolean matches(ArcaneCraftingTableBlockEntity.RecipeWrapper inv,
                           Level level) {
        // 周囲8スロット (0-7)
        for (int i = 0; i < 8; i++) {
            Ingredient ing   = ingredients.get(i);
            ItemStack  stack = inv.getItem(i);
            if (ing.isEmpty()) {
                if (!stack.isEmpty()) return false;
            } else {
                if (!ing.test(stack)) return false;
            }
        }

        // 中央スロット (8) — ingredients[8] があれば照合
        if (ingredients.size() > 8) {
            Ingredient centerIng   = ingredients.get(8);
            ItemStack  centerStack = inv.getItem(8);
            if (!centerIng.isEmpty()) {
                if (!centerIng.test(centerStack)) return false;
            }
            // centerIng が EMPTY の場合はスロット8が空でも空でなくてもOK
            // （中央材料不要のレシピ）
        }

        // 触媒スロット (9)
        if (catalyst != null
                && catalyst != Ingredient.EMPTY
                && catalyst.getItems().length > 0) {
            if (!catalyst.test(inv.getItem(9))) return false;
        }

        return true;
    }

    // ─── assemble ─────────────────────────────────────────────────────────
    @Override
    public ItemStack assemble(ArcaneCraftingTableBlockEntity.RecipeWrapper inv,
                              RegistryAccess reg) {
        return result.copy();
    }

    // ─── その他 ───────────────────────────────────────────────────────────
    @Override public boolean canCraftInDimensions(int w, int h) { return true; }
    @Override public ItemStack getResultItem(RegistryAccess reg) { return result; }
    @Override public ResourceLocation getId()                    { return id; }
    @Override public RecipeSerializer<?> getSerializer()         { return ArcaneCraftingRecipeSerializer.INSTANCE; }
    @Override public RecipeType<?> getType()                     { return ArcaneCraftingRecipeType.INSTANCE; }

    public boolean isCatalystConsumed() { return catalystConsume; }
    public NonNullList<Ingredient> getIngredients() { return ingredients; }

    // ═══════════════════════════════════════════════════════════════════════
    // Serializer
    // ═══════════════════════════════════════════════════════════════════════
    public static class ArcaneCraftingRecipeSerializer
            implements RecipeSerializer<ArcaneCraftingRecipe> {

        public static final ArcaneCraftingRecipeSerializer INSTANCE =
                new ArcaneCraftingRecipeSerializer();

        @Override
        public ArcaneCraftingRecipe fromJson(ResourceLocation id, JsonObject json) {

            // ── ingredients (最大9個: 周囲8 + 中央1) ──────────────────────
            JsonArray arr = GsonHelper.getAsJsonArray(json, "ingredients");
            // 9個分のリスト（デフォルトはEMPTY）
            NonNullList<Ingredient> ingredients =
                    NonNullList.withSize(9, Ingredient.EMPTY);

            for (int i = 0; i < 9 && i < arr.size(); i++) {
                JsonElement elem = arr.get(i);
                if (elem.isJsonObject()) {
                    JsonObject obj = elem.getAsJsonObject();
                    if (obj.entrySet().isEmpty()) {
                        ingredients.set(i, Ingredient.EMPTY);
                    } else {
                        ingredients.set(i, Ingredient.fromJson(obj));
                    }
                } else if (elem.isJsonPrimitive()) {
                    String s = elem.getAsString();
                    if (s.equals("minecraft:air") || s.equals("air")) {
                        ingredients.set(i, Ingredient.EMPTY);
                    } else {
                        ingredients.set(i, Ingredient.fromJson(elem));
                    }
                }
            }

            // ── catalyst ──────────────────────────────────────────────────
            Ingredient catalyst = Ingredient.EMPTY;
            if (json.has("catalyst")) {
                JsonElement ce = json.get("catalyst");
                if (ce.isJsonObject()) {
                    catalyst = Ingredient.fromJson(ce);
                }
            }

            // ── catalyst_consume ──────────────────────────────────────────
            boolean catalystConsume = GsonHelper.getAsBoolean(
                    json, "catalyst_consume", false);

            // ── result ────────────────────────────────────────────────────
            JsonObject resultObj = GsonHelper.getAsJsonObject(json, "result");
            String itemId = GsonHelper.getAsString(resultObj, "item");
            int    count  = GsonHelper.getAsInt(resultObj, "count", 1);

            var item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemId));
            ItemStack result = (item != null && item != net.minecraft.world.item.Items.AIR)
                    ? new ItemStack(item, count)
                    : ItemStack.EMPTY;

            return new ArcaneCraftingRecipe(id, ingredients, catalyst,
                    catalystConsume, result);
        }

        @Override
        public ArcaneCraftingRecipe fromNetwork(ResourceLocation id,
                                                FriendlyByteBuf buf) {
            int size = buf.readVarInt();
            NonNullList<Ingredient> ingredients = NonNullList.withSize(size, Ingredient.EMPTY);
            for (int i = 0; i < size; i++) {
                ingredients.set(i, Ingredient.fromNetwork(buf));
            }
            Ingredient catalyst       = Ingredient.fromNetwork(buf);
            boolean    catalystConsume = buf.readBoolean();
            ItemStack  result          = buf.readItem();
            return new ArcaneCraftingRecipe(id, ingredients, catalyst,
                    catalystConsume, result);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, ArcaneCraftingRecipe recipe) {
            buf.writeVarInt(recipe.ingredients.size());
            for (Ingredient ing : recipe.ingredients) {
                ing.toNetwork(buf);
            }
            recipe.catalyst.toNetwork(buf);
            buf.writeBoolean(recipe.catalystConsume);
            buf.writeItem(recipe.result);
        }
    }
}