package slimeknights.mantle.recipe;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import org.junit.jupiter.api.Test;
import slimeknights.mantle.recipe.cooking.BlastingResultRecipe;
import slimeknights.mantle.recipe.cooking.CampfireResultRecipe;
import slimeknights.mantle.recipe.cooking.SmeltingResultRecipe;
import slimeknights.mantle.recipe.cooking.SmokingResultRecipe;
import slimeknights.mantle.recipe.crafting.ShapedFallbackRecipe;
import slimeknights.mantle.recipe.crafting.ShapedRetexturedRecipe;
import slimeknights.mantle.recipe.helper.ItemOutput;
import slimeknights.mantle.recipe.helper.LoadableRecipeSerializer;
import slimeknights.mantle.test.LoadableTest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Covers every Mantle recipe serializer through the two views 1.21 asks a serializer for: a
 * {@link com.mojang.serialization.MapCodec} for the datapack and a {@link net.minecraft.network.codec.StreamCodec} for
 * the network.
 * <p>
 * Recipes have no equality, so a JSON round trip is asserted as parse then write giving the file back, which pins the
 * datapack format itself rather than only the fields the test remembered to check.
 */
class RecipeSerializerTest extends LoadableTest {
  /** Parses a recipe through its serializer's map codec, as the recipe manager does */
  private static <T extends Recipe<?>> T parse(RecipeSerializer<T> serializer, String json) {
    return success(serializer.codec().codec().parse(JsonOps.INSTANCE, JsonParser.parseString(json)));
  }

  /** Writes a recipe through its serializer's map codec, as datagen does */
  private static <T extends Recipe<?>> JsonElement write(RecipeSerializer<T> serializer, T recipe) {
    return write(serializer.codec().codec(), JsonOps.INSTANCE, recipe);
  }

  /** Asserts that parsing the given file then writing it back gives the same file */
  private static <T extends Recipe<?>> T assertJsonRoundTrip(RecipeSerializer<T> serializer, String json) {
    T recipe = parse(serializer, json);
    assertThat(write(serializer, recipe)).as("json round trip").isEqualTo(JsonParser.parseString(json));
    return recipe;
  }

  /** Asserts the recipe survives the network, returning the decoded copy for field checks */
  private static <T extends Recipe<?>> T assertNetworkRoundTrip(RecipeSerializer<T> serializer, T recipe) {
    RegistryFriendlyByteBuf buffer = buffer();
    serializer.streamCodec().encode(buffer, recipe);
    T read = serializer.streamCodec().decode(buffer);
    assertThat(buffer.readableBytes()).as("network round trip must consume the whole buffer").isZero();
    return read;
  }


  /* Cooking, the four serializers built straight out of a loadable */

  private static final String SMELTING_JSON = """
    {"group":"test_group","category":"food","ingredient":{"item":"minecraft:apple"},"result":{"item":"minecraft:golden_apple","count":2},"experience":0.5,"cooking_time":150}""";

  @Test
  void smelting_roundTripsThroughJson() {
    RecipeSerializer<SmeltingResultRecipe> serializer = LoadableRecipeSerializer.of(SmeltingResultRecipe.LOADABLE);
    SmeltingResultRecipe recipe = assertJsonRoundTrip(serializer, SMELTING_JSON);
    assertThat(recipe.getGroup()).isEqualTo("test_group");
    assertThat(recipe.category()).isEqualTo(CookingBookCategory.FOOD);
    assertThat(recipe.getExperience()).isEqualTo(0.5f);
    assertThat(recipe.getCookingTime()).isEqualTo(150);
    assertThat(recipe.getResult().get().getItem()).isEqualTo(Items.GOLDEN_APPLE);
    assertThat(recipe.getResult().getCount()).isEqualTo(2);
  }

  @Test
  void smelting_roundTripsThroughNetwork() {
    RecipeSerializer<SmeltingResultRecipe> serializer = LoadableRecipeSerializer.of(SmeltingResultRecipe.LOADABLE);
    SmeltingResultRecipe read = assertNetworkRoundTrip(serializer, parse(serializer, SMELTING_JSON));
    assertThat(read.getGroup()).isEqualTo("test_group");
    assertThat(read.getCookingTime()).isEqualTo(150);
    assertThat(read.getResult().get().getItem()).isEqualTo(Items.GOLDEN_APPLE);
    assertThat(read.getIngredients().get(0).test(new ItemStack(Items.APPLE))).isTrue();
  }

  @Test
  void cooking_theFourTypesDifferOnlyInDefaultCookingTime() {
    String minimal = """
      {"ingredient":{"item":"minecraft:apple"},"result":"minecraft:golden_apple"}""";
    assertThat(parse(LoadableRecipeSerializer.of(SmeltingResultRecipe.LOADABLE), minimal).getCookingTime()).isEqualTo(200);
    assertThat(parse(LoadableRecipeSerializer.of(BlastingResultRecipe.LOADABLE), minimal).getCookingTime()).isEqualTo(100);
    assertThat(parse(LoadableRecipeSerializer.of(SmokingResultRecipe.LOADABLE), minimal).getCookingTime()).isEqualTo(100);
    assertThat(parse(LoadableRecipeSerializer.of(CampfireResultRecipe.LOADABLE), minimal).getCookingTime()).isEqualTo(600);
  }

  @Test
  void cooking_writesATagResultAsATagPreference() {
    RecipeSerializer<SmeltingResultRecipe> serializer = LoadableRecipeSerializer.of(SmeltingResultRecipe.LOADABLE);
    assertJsonRoundTrip(serializer, """
      {"category":"misc","ingredient":{"item":"minecraft:apple"},"result":{"tag":"c:ingots/gold","count":3},"cooking_time":200}""");
  }


  /* Crafting */

  private static final String FALLBACK_JSON = """
    {"category":"misc","key":{"#":{"item":"minecraft:stick"}},"pattern":["##","##"],"result":{"id":"minecraft:crafting_table","count":1},"alternatives":["mantle:one","mantle:two"]}""";

  @Test
  void shapedFallback_roundTripsThroughJson() {
    ShapedFallbackRecipe recipe = assertJsonRoundTrip(new ShapedFallbackRecipe.Serializer(), FALLBACK_JSON);
    assertThat(recipe.getWidth()).isEqualTo(2);
    assertThat(recipe.getHeight()).isEqualTo(2);
  }

  @Test
  void shapedFallback_carriesItsAlternativesOverTheNetwork() {
    ShapedFallbackRecipe.Serializer serializer = new ShapedFallbackRecipe.Serializer();
    ShapedFallbackRecipe read = assertNetworkRoundTrip(serializer, parse(serializer, FALLBACK_JSON));
    assertThat(read.getIngredients()).hasSize(4);
    // the alternatives are the whole point of the recipe, so they must survive the sync the pattern also survives
    assertThat(read.getAlternatives()).containsExactly(
      ResourceLocation.fromNamespaceAndPath("mantle", "one"), ResourceLocation.fromNamespaceAndPath("mantle", "two"));
  }

  private static final String RETEXTURED_JSON = """
    {"category":"building","key":{"#":{"item":"minecraft:stick"},"B":{"item":"minecraft:stone"}},"pattern":["#B#"],"result":{"id":"minecraft:crafting_table","count":1},"texture":"B","match_all":true}""";

  @Test
  void shapedRetextured_roundTripsThroughJson() {
    ShapedRetexturedRecipe recipe = assertJsonRoundTrip(new ShapedRetexturedRecipe.Serializer(), RETEXTURED_JSON);
    // the texture is the ingredient the symbol names, not merely the first input
    assertThat(recipe.getTexture().test(new ItemStack(Items.STONE))).isTrue();
    assertThat(recipe.getTexture().test(new ItemStack(Items.STICK))).isFalse();
  }

  @Test
  void shapedRetextured_syncsTheResolvedTextureRatherThanItsSymbol() {
    ShapedRetexturedRecipe.Serializer serializer = new ShapedRetexturedRecipe.Serializer();
    ShapedRetexturedRecipe read = assertNetworkRoundTrip(serializer, parse(serializer, RETEXTURED_JSON));
    // the client has no key map, so the ingredient itself crosses
    assertThat(read.getTexture().test(new ItemStack(Items.STONE))).isTrue();
    assertThat(read.getTexture().test(new ItemStack(Items.STICK))).isFalse();
  }

  @Test
  void shapedRetextured_rejectsATextureSymbolMissingFromTheKey() {
    assertThatThrownBy(() -> parse(new ShapedRetexturedRecipe.Serializer(), """
      {"key":{"#":{"item":"minecraft:stick"}},"pattern":["##"],"result":{"id":"minecraft:crafting_table"},"texture":"B"}"""))
      .hasMessageContaining("symbol 'B'");
  }

  @Test
  void shapedRetextured_cannotWriteARecipeReadFromTheNetwork() {
    ShapedRetexturedRecipe.Serializer serializer = new ShapedRetexturedRecipe.Serializer();
    ShapedRetexturedRecipe read = assertNetworkRoundTrip(serializer, parse(serializer, RETEXTURED_JSON));
    // same rule vanilla's own shaped recipe follows: the key and pattern do not cross the network
    assertThat(error(serializer.codec().codec().encodeStart(JsonOps.INSTANCE, read))).contains("Cannot encode unpacked recipe");
  }

  @Test
  void shapedRetextured_builtInMemoryCarriesItsTexture() {
    // the shape a datagen provider produces, which never goes through the key map
    ShapedRecipePattern pattern = ShapedRecipePattern.of(Map.of('#', Ingredient.of(Items.STICK)), List.of("##"));
    ShapedRetexturedRecipe recipe = new ShapedRetexturedRecipe(
      new ShapedRecipe("", CraftingBookCategory.MISC, pattern, new ItemStack(Items.CRAFTING_TABLE)),
      '#', Ingredient.of(Items.STICK), false);
    assertThat(write(new ShapedRetexturedRecipe.Serializer(), recipe).getAsJsonObject().get("texture").getAsString()).isEqualTo("#");
  }


  /* Serializer plumbing */

  @Test
  void typeAware_serializersOverOneLoadableEachCarryTheirOwnType() {
    // the mechanism replacing 1.20's type-in-the-context: one serializer instance per type, sharing a loadable
    var smelting = LoadableRecipeSerializer.of(SmeltingResultRecipe.LOADABLE, () -> RecipeType.SMELTING);
    var blasting = LoadableRecipeSerializer.of(BlastingResultRecipe.LOADABLE, () -> RecipeType.BLASTING);
    assertThat(smelting.getType()).isEqualTo(RecipeType.SMELTING);
    assertThat(blasting.getType()).isEqualTo(RecipeType.BLASTING);
    // and both still parse the same file, differing only in the type they hand their recipes
    assertThat(parse(smelting, SMELTING_JSON).getCookingTime()).isEqualTo(150);
    assertThat(parse(blasting, SMELTING_JSON).getCookingTime()).isEqualTo(150);
  }

  @Test
  void loadableSerializer_reusesItsCodecs() {
    // the codecs are built lazily so an overridden context is complete, but only once. streamCodec() is exempt by
    // design: it wraps the cached stream codec in a fresh logging decorator per call.
    RecipeSerializer<SmeltingResultRecipe> serializer = LoadableRecipeSerializer.of(SmeltingResultRecipe.LOADABLE);
    assertThat(serializer.codec()).isSameAs(serializer.codec());
    assertThat(serializer.streamCodec()).isNotNull();
  }

  @Test
  void loggingSerializer_wrapsANetworkFailureWithTheSerializerName() {
    RecipeSerializer<SmeltingResultRecipe> serializer = LoadableRecipeSerializer.of(SmeltingResultRecipe.LOADABLE);
    // an empty buffer cannot supply the group string, so decoding must fail loudly rather than silently
    assertThatThrownBy(() -> serializer.streamCodec().decode(buffer()))
      .hasMessageContaining("Error reading recipe from packet");
  }

  @Test
  void itemOutput_isStillReachableAsARecipeResult() {
    // a tag output writes components rather than nbt
    ItemOutput output = ItemOutput.fromTag(ItemTags.PLANKS, 2, null);
    assertThat(output.serialize(true).toString()).isEqualTo("{\"tag\":\"minecraft:planks\",\"count\":2}");
  }
}
