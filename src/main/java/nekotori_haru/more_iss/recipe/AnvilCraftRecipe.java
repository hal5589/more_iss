package nekotori_haru.more_iss.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.RegistryAccess;
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
    private final String baseSpell;
    private final int baseLevel;
    private final String additionalSpell;
    private final int additionalLevel;
    private final String outputSpell;
    private final int outputLevel;

    public AnvilCraftRecipe(ResourceLocation id, String baseSpell, int baseLevel, String additionalSpell, int additionalLevel, String outputSpell, int outputLevel) {
        this.id = id;
        this.baseSpell = baseSpell;
        this.baseLevel = baseLevel;
        this.additionalSpell = additionalSpell;
        this.additionalLevel = additionalLevel;
        this.outputSpell = outputSpell;
        this.outputLevel = outputLevel;
    }

    /**
     * IDとレベルが両方とも満たされているかチェックする（順不同対応）
     */
    public boolean matchesSpells(String id1, int lvl1, String id2, int lvl2) {
        // パターンA: 1つ目のスロットが base_spell、2つ目のスロットが additional_spell
        boolean patternA = this.baseSpell.equals(id1) && lvl1 >= this.baseLevel &&
                this.additionalSpell.equals(id2) && lvl2 >= this.additionalLevel;

        // パターンB: 2つ目のスロットが base_spell、1つ目のスロットが additional_spell
        boolean patternB = this.baseSpell.equals(id2) && lvl2 >= this.baseLevel &&
                this.additionalSpell.equals(id1) && lvl1 >= this.additionalLevel;

        return patternA || patternB;
    }

    public String getOutputSpellId() { return this.outputSpell; }
    public int getOutputLevel() { return this.outputLevel; }

    @Override public ResourceLocation getId() { return this.id; }
    @Override public RecipeSerializer<?> getSerializer() { return SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return TYPE; }

    @Override public boolean matches(Container pContainer, Level pLevel) { return false; }
    @Override public ItemStack assemble(Container pContainer, RegistryAccess pRegistryAccess) { return ItemStack.EMPTY; }
    @Override public boolean canCraftInDimensions(int pWidth, int pHeight) { return true; }
    @Override public ItemStack getResultItem(RegistryAccess pRegistryAccess) { return ItemStack.EMPTY; }

    // --- Serializer ---
    public static class Serializer implements RecipeSerializer<AnvilCraftRecipe> {

        @Override
        public AnvilCraftRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
            // base_spell の解析
            JsonObject baseObj = GsonHelper.getAsJsonObject(json, "base_spell");
            String baseSpell = GsonHelper.getAsString(baseObj, "spell");
            int baseLevel = GsonHelper.getAsInt(baseObj, "level", 1);

            // additional_spells 配列の解析
            JsonArray additionalArray = GsonHelper.getAsJsonArray(json, "additional_spells");
            String additionalSpell = "";
            int additionalLevel = 1;
            if (additionalArray.size() > 0) {
                JsonObject firstAdditional = additionalArray.get(0).getAsJsonObject();
                additionalSpell = GsonHelper.getAsString(firstAdditional, "spell");
                additionalLevel = GsonHelper.getAsInt(firstAdditional, "level", 1);
            }

            // output_spell の解析
            JsonObject outputObj = GsonHelper.getAsJsonObject(json, "output_spell");
            String outputSpell = GsonHelper.getAsString(outputObj, "spell");
            int outputLevel = GsonHelper.getAsInt(outputObj, "level", 1);

            return new AnvilCraftRecipe(recipeId, baseSpell, baseLevel, additionalSpell, additionalLevel, outputSpell, outputLevel);
        }

        @Override
        public AnvilCraftRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
            return new AnvilCraftRecipe(
                    recipeId,
                    buffer.readUtf(), buffer.readVarInt(),
                    buffer.readUtf(), buffer.readVarInt(),
                    buffer.readUtf(), buffer.readVarInt()
            );
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, AnvilCraftRecipe recipe) {
            buffer.writeUtf(recipe.baseSpell);
            buffer.writeVarInt(recipe.baseLevel);
            buffer.writeUtf(recipe.additionalSpell);
            buffer.writeVarInt(recipe.additionalLevel);
            buffer.writeUtf(recipe.outputSpell);
            buffer.writeVarInt(recipe.outputLevel);
        }
    }
}