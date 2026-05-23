package nekotori_haru.more_iss.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import nekotori_haru.more_iss.blockentity.ArcaneCraftingTableBlockEntity;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
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
import org.jetbrains.annotations.Nullable;

public class ArcaneCraftingRecipe
        implements Recipe<ArcaneCraftingTableBlockEntity.RecipeWrapper> {

    public static final String ID = "arcane_crafting";

    private final ResourceLocation id;
    private final NonNullList<Ingredient> ingredients;

    // 🔥 NBT 検証情報（各スロット用）
    private final NonNullList<CompoundTag> ingredientNBTs;
    private final NonNullList<String> requiredSpells;  // required_spell
    private final NonNullList<Integer> requiredLevels;  // required_level

    private final Ingredient catalyst;
    private final CompoundTag catalystNBT;
    private final boolean catalystConsume;

    // 🔥 結果タイプ
    private final ItemStack result;
    private final String resultSpellId;
    private final int resultSpellLevel;

    public ArcaneCraftingRecipe(ResourceLocation id,
                                NonNullList<Ingredient> ingredients,
                                NonNullList<CompoundTag> ingredientNBTs,
                                NonNullList<String> requiredSpells,
                                NonNullList<Integer> requiredLevels,
                                Ingredient catalyst,
                                CompoundTag catalystNBT,
                                boolean catalystConsume,
                                ItemStack result,
                                String resultSpellId,
                                int resultSpellLevel) {
        this.id = id;
        this.ingredients = ingredients;
        this.ingredientNBTs = ingredientNBTs;
        this.requiredSpells = requiredSpells;
        this.requiredLevels = requiredLevels;
        this.catalyst = catalyst;
        this.catalystNBT = catalystNBT;
        this.catalystConsume = catalystConsume;
        this.result = result;
        this.resultSpellId = resultSpellId;
        this.resultSpellLevel = resultSpellLevel;
    }

    // ─── matches ──────────────────────────────────────────────────────────
    @Override
    public boolean matches(ArcaneCraftingTableBlockEntity.RecipeWrapper inv, Level level) {
        // 周囲8スロット (0-7) + 中央スロット (8)
        for (int i = 0; i < 9; i++) {
            Ingredient ing = ingredients.get(i);
            ItemStack stack = inv.getItem(i);

            // アイテムが空でないかチェック
            if (ing.isEmpty()) {
                if (!stack.isEmpty()) return false;
                continue;
            }

            // アイテム種別チェック
            if (!ing.test(stack)) return false;

            // 🔥 NBT タグチェック（スクロールの場合）
            CompoundTag nbt = ingredientNBTs.get(i);
            if (nbt != null && !nbt.isEmpty()) {
                if (!stack.hasTag()) return false;
                CompoundTag stackTag = stack.getTag();
                if (stackTag == null || !nbtMatches(stackTag, nbt)) {
                    return false;
                }
            }

            // 🔥 Required Spell チェック（スクロール限定）
            String requiredSpell = requiredSpells.get(i);
            if (requiredSpell != null && !requiredSpell.isEmpty()) {
                if (!isScrollWithSpell(stack, requiredSpell, requiredLevels.get(i))) {
                    return false;
                }
            }
        }

        // 触媒スロット (9)
        if (catalyst != null && catalyst != Ingredient.EMPTY && catalyst.getItems().length > 0) {
            ItemStack catalystStack = inv.getItem(9);
            if (!catalyst.test(catalystStack)) return false;

            // 触媒の NBT チェック
            if (catalystNBT != null && !catalystNBT.isEmpty()) {
                if (!catalystStack.hasTag()) return false;
                CompoundTag stackTag = catalystStack.getTag();
                if (stackTag == null || !nbtMatches(stackTag, catalystNBT)) {
                    return false;
                }
            }
        }

        return true;
    }

    // 🔥 NBT が一致しているか（部分一致：recipeNBT が stackTag に含まれている）
    private boolean nbtMatches(CompoundTag stackTag, CompoundTag recipeNBT) {
        for (String key : recipeNBT.getAllKeys()) {
            if (!stackTag.contains(key)) return false;
            if (!stackTag.get(key).equals(recipeNBT.get(key))) {
                return false;
            }
        }
        return true;
    }

    // 🔥 スクロールが指定された魔法を持っているか
    private boolean isScrollWithSpell(ItemStack stack, String requiredSpellId, int requiredLevel) {
        // irons_spellbooks:scroll アイテムであることを確認
        if (!stack.getItem().toString().contains("scroll")) {
            return false;
        }

        CompoundTag tag = stack.getTag();
        if (tag == null) return false;

        // ISB_spell.data.id と base_lvl を確認
        if (tag.contains("ISB_spell")) {
            CompoundTag spellTag = tag.getCompound("ISB_spell");
            if (spellTag.contains("data")) {
                CompoundTag dataTag = spellTag.getCompound("data");
                String spellId = dataTag.getString("id");
                int level = dataTag.getInt("base_lvl");

                return spellId.equals(requiredSpellId) && level >= requiredLevel;
            }
        }
        return false;
    }

    // ─── assemble ─────────────────────────────────────────────────────────
    @Override
    public ItemStack assemble(ArcaneCraftingTableBlockEntity.RecipeWrapper inv, RegistryAccess reg) {
        // 🔥 result_spell がある場合、魔法スクロールを生成
        if (resultSpellId != null && !resultSpellId.isEmpty()) {
            // irons_spellbooks:scroll アイテムを取得
            var scrollItem = ForgeRegistries.ITEMS.getValue(new ResourceLocation("irons_spellbooks:scroll"));

            if (scrollItem != null) {
                ItemStack spellScroll = new ItemStack(scrollItem);
                CompoundTag tag = new CompoundTag();

                CompoundTag spellTag = new CompoundTag();
                CompoundTag dataTag = new CompoundTag();
                dataTag.putString("id", resultSpellId);
                dataTag.putInt("base_lvl", resultSpellLevel);
                spellTag.put("data", dataTag);
                tag.put("ISB_spell", spellTag);

                spellScroll.setTag(tag);
                return spellScroll;
            }
        }

        // 通常のアイテム結果
        return result.copy();
    }

    // ─── その他 ───────────────────────────────────────────────────────────
    @Override public boolean canCraftInDimensions(int w, int h) { return true; }
    @Override public ItemStack getResultItem(RegistryAccess reg) { return result; }
    @Override public ResourceLocation getId() { return id; }
    @Override public RecipeSerializer<?> getSerializer() { return ArcaneCraftingRecipeSerializer.INSTANCE; }
    @Override public RecipeType<?> getType() { return ArcaneCraftingRecipeType.INSTANCE; }

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
                        // Ingredient 解析
                        if (obj.has("item")) {
                            ingredients.set(i, Ingredient.fromJson(obj));
                        }

                        // 🔥 NBT 解析
                        if (obj.has("nbt")) {
                            String nbtStr = obj.get("nbt").getAsString();
                            try {
                                ingredientNBTs.set(i, TagParser.parseTag(nbtStr));
                            } catch (Exception e) {
                                System.err.println("Failed to parse NBT: " + nbtStr);
                            }
                        }

                        // 🔥 Required Spell 解析
                        if (obj.has("required_spell")) {
                            requiredSpells.set(i, obj.get("required_spell").getAsString());
                        }
                        if (obj.has("required_level")) {
                            requiredLevels.set(i, obj.get("required_level").getAsInt());
                        }
                    }
                }
            }

            // ── catalyst ──────────────────────────────────────────────────
            Ingredient catalyst = Ingredient.EMPTY;
            CompoundTag catalystNBT = new CompoundTag();
            if (json.has("catalyst")) {
                JsonElement ce = json.get("catalyst");
                if (ce.isJsonObject()) {
                    JsonObject catalystObj = ce.getAsJsonObject();
                    catalyst = Ingredient.fromJson(catalystObj);

                    if (catalystObj.has("nbt")) {
                        String nbtStr = catalystObj.get("nbt").getAsString();
                        try {
                            catalystNBT = TagParser.parseTag(nbtStr);
                        } catch (Exception e) {
                            System.err.println("Failed to parse catalyst NBT: " + nbtStr);
                        }
                    }
                }
            }

            // ── catalyst_consume ──────────────────────────────────────────
            boolean catalystConsume = GsonHelper.getAsBoolean(json, "catalyst_consume", false);

            // 🔥 ── result_spell ───────────────────────────────────────────
            String resultSpellId = "";
            int resultSpellLevel = 1;
            ItemStack result = ItemStack.EMPTY;

            if (json.has("result_spell")) {
                JsonObject resultSpellObj = json.getAsJsonObject("result_spell");
                resultSpellId = GsonHelper.getAsString(resultSpellObj, "id");
                resultSpellLevel = GsonHelper.getAsInt(resultSpellObj, "level", 1);
            } else if (json.has("result")) {
                // ── result（通常アイテム） ────────────────────────────────
                JsonObject resultObj = GsonHelper.getAsJsonObject(json, "result");
                String itemId = GsonHelper.getAsString(resultObj, "item");
                int count = GsonHelper.getAsInt(resultObj, "count", 1);

                var item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemId));
                result = (item != null && item != net.minecraft.world.item.Items.AIR)
                        ? new ItemStack(item, count)
                        : ItemStack.EMPTY;
            }

            return new ArcaneCraftingRecipe(id, ingredients, ingredientNBTs, requiredSpells, requiredLevels,
                    catalyst, catalystNBT, catalystConsume, result, resultSpellId, resultSpellLevel);
        }

        @Override
        public ArcaneCraftingRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            int size = buf.readVarInt();
            NonNullList<Ingredient> ingredients = NonNullList.withSize(size, Ingredient.EMPTY);
            for (int i = 0; i < size; i++) {
                ingredients.set(i, Ingredient.fromNetwork(buf));
            }

            NonNullList<CompoundTag> ingredientNBTs = NonNullList.withSize(size, new CompoundTag());
            for (int i = 0; i < size; i++) {
                if (buf.readBoolean()) {
                    ingredientNBTs.set(i, buf.readNbt());
                }
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
            buf.writeVarInt(recipe.ingredients.size());
            for (Ingredient ing : recipe.ingredients) {
                ing.toNetwork(buf);
            }

            for (CompoundTag nbt : recipe.ingredientNBTs) {
                buf.writeBoolean(!nbt.isEmpty());
                if (!nbt.isEmpty()) {
                    buf.writeNbt(nbt);
                }
            }

            for (int i = 0; i < recipe.requiredSpells.size(); i++) {
                buf.writeUtf(recipe.requiredSpells.get(i));
                buf.writeInt(recipe.requiredLevels.get(i));
            }

            recipe.catalyst.toNetwork(buf);
            buf.writeBoolean(!recipe.catalystNBT.isEmpty());
            if (!recipe.catalystNBT.isEmpty()) {
                buf.writeNbt(recipe.catalystNBT);
            }
            buf.writeBoolean(recipe.catalystConsume);
            buf.writeItem(recipe.result);
            buf.writeUtf(recipe.resultSpellId);
            buf.writeInt(recipe.resultSpellLevel);
        }
    }
}