package slimeknights.mantle.recipe.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.common.conditions.ICondition.IContext;
import org.junit.jupiter.api.Test;
import slimeknights.mantle.test.LoadableTest;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers Mantle's three conditions: the JSON each writes, and what each answers against a set of loaded tags.
 * <p>
 * The codecs are exercised directly rather than through {@link ICondition#CODEC}, as the dispatch that codec performs
 * needs the registry entry a unit test cannot supply; {@link MantleConditions} is what wires the two together.
 */
class TagConditionTest extends LoadableTest {
  private static final TagKey<Item> FILLED = ItemTags.PLANKS;
  private static final TagKey<Item> EMPTY = ItemTags.create(ResourceLocation.fromNamespaceAndPath("mantle", "nothing_at_all"));
  private static final TagKey<Fluid> FLUID_TAG = TagKey.create(Registries.FLUID, ResourceLocation.fromNamespaceAndPath("mantle", "some_fluid"));

  /** Context supplying a fixed set of tags, which is all any of these conditions reads */
  private static IContext context(Map<TagKey<?>,List<Item>> tags) {
    return new IContext() {
      @SuppressWarnings("unchecked")
      @Override
      public <T> Map<ResourceLocation,Collection<Holder<T>>> getAllTags(ResourceKey<? extends Registry<T>> registry) {
        return tags.entrySet().stream()
                   .filter(entry -> entry.getKey().registry().equals(registry))
                   .collect(Collectors.toMap(
                     entry -> entry.getKey().location(),
                     entry -> (Collection<Holder<T>>)(Collection<?>)entry.getValue().stream().map(Item::builtInRegistryHolder).toList()));
      }
    };
  }

  /** Writes a condition through its own map codec */
  private static <C extends ICondition> JsonElement write(MapCodec<C> codec, C condition) {
    return write(codec.codec(), JsonOps.INSTANCE, condition);
  }

  /** Reads a condition through its own map codec */
  private static <C extends ICondition> C read(MapCodec<C> codec, String json) {
    return success(codec.codec().parse(JsonOps.INSTANCE, JsonParser.parseString(json)));
  }


  /* Tag empty and filled */

  @Test
  void tagEmpty_writesTheTagAndOmitsTheItemRegistry() {
    assertThat(write(TagEmptyCondition.CODEC, new TagEmptyCondition<>(FILLED)))
      .isEqualTo(JsonParser.parseString("{\"tag\":\"minecraft:planks\"}"));
  }

  @Test
  void tagEmpty_writesANonItemRegistry() {
    assertThat(write(TagEmptyCondition.CODEC, new TagEmptyCondition<>(FLUID_TAG)))
      .isEqualTo(JsonParser.parseString("{\"registry\":\"minecraft:fluid\",\"tag\":\"mantle:some_fluid\"}"));
  }

  @Test
  void tagEmpty_defaultsToTheItemRegistry() {
    assertThat(read(TagEmptyCondition.CODEC, "{\"tag\":\"minecraft:planks\"}").getTag()).isEqualTo(FILLED);
  }

  @Test
  void tagEmpty_readsAnExplicitRegistry() {
    assertThat(read(TagEmptyCondition.CODEC, "{\"registry\":\"minecraft:fluid\",\"tag\":\"mantle:some_fluid\"}").getTag()).isEqualTo(FLUID_TAG);
  }

  @Test
  void tagEmpty_matchesOnlyAnEmptyTag() {
    IContext context = context(Map.of(FILLED, List.of(Items.OAK_PLANKS)));
    assertThat(new TagEmptyCondition<>(FILLED).test(context)).isFalse();
    assertThat(new TagEmptyCondition<>(EMPTY).test(context)).isTrue();
  }

  @Test
  void tagFilled_isTheExactInverse() {
    IContext context = context(Map.of(FILLED, List.of(Items.OAK_PLANKS)));
    assertThat(new TagFilledCondition<>(FILLED).test(context)).isTrue();
    assertThat(new TagFilledCondition<>(EMPTY).test(context)).isFalse();
  }

  @Test
  void tagFilled_sharesTheFormatOfTagEmpty() {
    assertThat(write(TagFilledCondition.CODEC, new TagFilledCondition<>(FILLED)))
      .isEqualTo(write(TagEmptyCondition.CODEC, new TagEmptyCondition<>(FILLED)));
  }

  @Test
  void tagConditions_ownTheirLootTypes() {
    // the reason recipe.condition no longer waits on loot.MantleLoot: the type is a map codec the class already has
    assertThat(TagEmptyCondition.LOOT_TYPE.codec()).isSameAs(TagEmptyCondition.CODEC);
    assertThat(TagFilledCondition.LOOT_TYPE.codec()).isSameAs(TagFilledCondition.CODEC);
  }


  /* Tag combination */

  private static final TagKey<Item> A = ItemTags.create(ResourceLocation.fromNamespaceAndPath("mantle", "a"));
  private static final TagKey<Item> B = ItemTags.create(ResourceLocation.fromNamespaceAndPath("mantle", "b"));
  private static final TagKey<Item> IGNORE = ItemTags.create(ResourceLocation.fromNamespaceAndPath("mantle", "ignore"));

  @Test
  void tagCombination_writesASingleTagCompactly() {
    assertThat(write(TagCombinationCondition.CODEC, TagCombinationCondition.intersection(A)))
      .isEqualTo(JsonParser.parseString("{\"match\":\"mantle:a\"}"));
  }

  @Test
  void tagCombination_writesSeveralTagsAsAnArrayWithTheIgnore() {
    assertThat(write(TagCombinationCondition.CODEC, TagCombinationCondition.match(IGNORE, A, B)))
      .isEqualTo(JsonParser.parseString("{\"match\":[\"mantle:a\",\"mantle:b\"],\"ignore\":\"mantle:ignore\"}"));
  }

  @Test
  void tagCombination_roundTripsThroughJson() {
    String json = "{\"registry\":\"minecraft:fluid\",\"match\":[\"mantle:a\",\"mantle:b\"]}";
    assertThat(write(TagCombinationCondition.CODEC, read(TagCombinationCondition.CODEC, json)))
      .isEqualTo(JsonParser.parseString(json));
  }

  @Test
  void tagCombination_matchesOnlyWhenTheIntersectionHasAnEntry() {
    assertThat(TagCombinationCondition.intersection(A, B).test(context(Map.of(A, List.of(Items.STICK), B, List.of(Items.STICK))))).isTrue();
    assertThat(TagCombinationCondition.intersection(A, B).test(context(Map.of(A, List.of(Items.STICK), B, List.of(Items.STONE))))).isFalse();
  }

  @Test
  void tagCombination_dropsIgnoredEntriesFromTheIntersection() {
    Map<TagKey<?>,List<Item>> tags = Map.of(A, List.of(Items.STICK), B, List.of(Items.STICK), IGNORE, List.of(Items.STICK));
    assertThat(TagCombinationCondition.intersection(A, B).test(context(tags))).isTrue();
    assertThat(TagCombinationCondition.match(IGNORE, A, B).test(context(tags))).isFalse();
  }
}
