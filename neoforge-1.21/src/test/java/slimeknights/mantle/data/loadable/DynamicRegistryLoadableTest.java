package slimeknights.mantle.data.loadable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import org.junit.jupiter.api.Test;
import slimeknights.mantle.data.loadable.array.ArrayLoadable;
import slimeknights.mantle.data.loadable.field.ContextKey;
import slimeknights.mantle.data.loadable.primitive.FloatLoadable;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.primitive.StringLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.test.LoadableTest;
import slimeknights.mantle.util.typed.TypedMap;
import slimeknights.mantle.util.typed.TypedMapBuilder;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests {@link slimeknights.mantle.data.loadable.common.DynamicRegistryLoadable} through
 * {@link Loadables#ENCHANTMENT}, the datapack registry loadable Mantle actually ships.
 * <p>
 * The registries come from {@link VanillaRegistries#createLookup()}, which is how datagen builds the datapack
 * registries without a world; {@link slimeknights.mantle.test.BaseMcTest#registryAccess()} deliberately has only the
 * static ones, so it is what the "no registries" cases read with.
 */
class DynamicRegistryLoadableTest extends LoadableTest {
  /** Datapack registries, built once as the lookup is not cheap */
  private static final HolderLookup.Provider REGISTRIES = VanillaRegistries.createLookup();
  /** Ops carrying the registries, which is what every datapack load supplies */
  private static final RegistryOps<JsonElement> OPS = REGISTRIES.createSerializationContext(JsonOps.INSTANCE);
  /** Context carrying the registries, for a caller which has them but not the ops */
  private static final TypedMap CONTEXT = TypedMapBuilder.builder().put(ContextKey.REGISTRY_ACCESS, REGISTRIES).build();

  /** {@return the sharpness enchantment holder} */
  private static Holder<Enchantment> sharpness() {
    return REGISTRIES.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS);
  }

  /** {@return the given enchantment holder} */
  private static Holder<Enchantment> enchantment(ResourceKey<Enchantment> key) {
    return REGISTRIES.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
  }

  /** {@return a JSON object of the given enchantment name to level}, the map form read below */
  private static JsonObject levels(String name, int level) {
    JsonObject json = new JsonObject();
    json.addProperty(name, level);
    return json;
  }

  @Test
  void writing_needsNoRegistries() {
    // a holder knows its own key, so serializing works on the plainest ops there is
    assertThat(Loadables.ENCHANTMENT.serialize(sharpness())).isEqualTo(new JsonPrimitive("minecraft:sharpness"));
  }

  @Test
  void reading_resolvesThroughRegistryOps() {
    Holder<Enchantment> read = Loadables.ENCHANTMENT.convert(OPS, new JsonPrimitive("minecraft:sharpness"), KEY);
    assertThat(read).isEqualTo(sharpness());
  }

  @Test
  void reading_resolvesThroughTheContextKey() {
    // plain ops, registries supplied alongside instead: OpsHelper#withRegistries is what bridges the two
    Holder<Enchantment> read = Loadables.ENCHANTMENT.convert(JsonOps.INSTANCE, new JsonPrimitive("minecraft:sharpness"), KEY, CONTEXT);
    assertThat(read).isEqualTo(sharpness());
  }

  @Test
  void reading_withoutRegistriesNamesEveryRouteToThem() {
    assertThatThrownBy(() -> Loadables.ENCHANTMENT.convert(new JsonPrimitive("minecraft:sharpness"), KEY))
      .isInstanceOf(JsonSyntaxException.class)
      .hasMessageContaining(KEY)
      .hasMessageContaining("minecraft:enchantment")
      .hasMessageContaining("RegistryOps")
      .hasMessageContaining("ContextKey.REGISTRY_ACCESS");
  }

  @Test
  void reading_unknownIdNamesTheRegistryAndTheId() {
    assertThatThrownBy(() -> Loadables.ENCHANTMENT.convert(OPS, new JsonPrimitive("mantle:not_an_enchantment"), KEY))
      .isInstanceOf(JsonSyntaxException.class)
      .hasMessageContaining("minecraft:enchantment")
      .hasMessageContaining("mantle:not_an_enchantment");
  }

  @Test
  void reading_nonStringFailsAsAString() {
    assertThatThrownBy(() -> Loadables.ENCHANTMENT.convert(OPS, new JsonObject(), KEY))
      .isInstanceOf(JsonSyntaxException.class)
      .hasMessageContaining("to be a string");
  }


  /* Map keys
   * A datapack registry loadable is a StringLoadable, so it may key a map as readily as it may fill a field. Both
   * positions must reach the registries by all three routes; the key side has no route of its own.
   */

  /** Map keyed by an enchantment, the shape of the {@code enchantments} field on Tinkers' break block fluid effect */
  private static final Loadable<Map<Holder<Enchantment>,Integer>> LEVELS = Loadables.ENCHANTMENT.mapWithValues(IntLoadable.FROM_ONE, 0);

  @Test
  void mapKey_resolvesThroughRegistryOps() {
    assertThat(LEVELS.convert(OPS, levels("minecraft:sharpness", 3), KEY)).isEqualTo(Map.of(sharpness(), 3));
  }

  @Test
  void mapKey_resolvesThroughTheContextKey() {
    assertThat(LEVELS.convert(JsonOps.INSTANCE, levels("minecraft:sharpness", 3), KEY, CONTEXT)).isEqualTo(Map.of(sharpness(), 3));
  }

  @Test
  void mapKey_resolvesThroughTheContextKeyOnTheGsonPath() {
    // the path a reload listener takes: gson elements with the reload's registries alongside in the context
    assertThat(LEVELS.convert((JsonElement)levels("minecraft:sharpness", 3), KEY, CONTEXT)).isEqualTo(Map.of(sharpness(), 3));
  }

  @Test
  void mapKey_resolvesInAnotherFormat() {
    // nothing about the key position is gson specific, so NBT must resolve identically
    Tag tag = LEVELS.serialize(NbtOps.INSTANCE, Map.of(sharpness(), 3));
    assertThat(LEVELS.convert(NbtOps.INSTANCE, tag, KEY, CONTEXT)).isEqualTo(Map.of(sharpness(), 3));
  }

  @Test
  void mapKey_writingNeedsNoRegistries() {
    assertThat(LEVELS.serialize(Map.of(sharpness(), 3))).isEqualTo(levels("minecraft:sharpness", 3));
  }

  @Test
  void mapKey_withoutRegistriesNamesEveryRouteToThem() {
    assertThatThrownBy(() -> LEVELS.convert((JsonElement)levels("minecraft:sharpness", 3), KEY))
      .isInstanceOf(JsonSyntaxException.class)
      .hasMessageContaining("minecraft:enchantment")
      .hasMessageContaining("RegistryOps")
      .hasMessageContaining("ContextKey.REGISTRY_ACCESS");
  }

  @Test
  void mapKey_unknownIdNamesTheRegistryAndTheId() {
    assertThatThrownBy(() -> LEVELS.convert(OPS, levels("mantle:not_an_enchantment", 3), KEY))
      .isInstanceOf(JsonSyntaxException.class)
      .hasMessageContaining("minecraft:enchantment")
      .hasMessageContaining("mantle:not_an_enchantment");
  }


  /* Element positions
   * The other shapes which could drop the ops or the context around a nested loadable. Each reads through both
   * routes, as an element of a collection is no more entitled to them than a map key is.
   */

  @Test
  void listElement_resolvesThroughBothRoutes() {
    Loadable<List<Holder<Enchantment>>> list = Loadables.ENCHANTMENT.list();
    JsonArray json = new JsonArray();
    json.add("minecraft:sharpness");
    assertThat(list.convert(OPS, json, KEY)).isEqualTo(List.of(sharpness()));
    assertThat(list.convert(JsonOps.INSTANCE, json, KEY, CONTEXT)).isEqualTo(List.of(sharpness()));
  }

  @Test
  void compactElement_resolvesThroughBothRoutes() {
    // a compact collection is a single element where a list was expected, so it takes a different branch than the above
    Loadable<Set<Holder<Enchantment>>> set = Loadables.ENCHANTMENT.set(ArrayLoadable.COMPACT);
    JsonPrimitive json = new JsonPrimitive("minecraft:sharpness");
    assertThat(set.convert(OPS, json, KEY)).isEqualTo(Set.of(sharpness()));
    assertThat(set.convert(JsonOps.INSTANCE, json, KEY, CONTEXT)).isEqualTo(Set.of(sharpness()));
  }

  @Test
  void mapValue_resolvesThroughBothRoutes() {
    Loadable<Map<String,Holder<Enchantment>>> map = StringLoadable.DEFAULT.mapWithValues(Loadables.ENCHANTMENT);
    JsonObject json = new JsonObject();
    json.addProperty("best", "minecraft:sharpness");
    assertThat(map.convert(OPS, json, KEY)).isEqualTo(Map.of("best", sharpness()));
    assertThat(map.convert(JsonOps.INSTANCE, json, KEY, CONTEXT)).isEqualTo(Map.of("best", sharpness()));
  }


  /* Fixtures
   * The two files which found this: the break block effect nested in Tinkers' molten diamond and molten emerald fluid
   * effects, whose enchantment keyed map is the only such map in either mod. The record below carries that effect's
   * two loadable fields verbatim, and both objects are the shipped JSON, read the way FluidEffectManager reads them:
   * a gson object with the reload's registries in the context and no registry ops anywhere.
   */

  /** Stand in for Tinkers' break block fluid effect, holding its two fields in its declaration order */
  private record BreakBlock(float hardness, Map<Holder<Enchantment>,Integer> enchantments) {
    static final RecordLoadable<BreakBlock> LOADABLE = RecordLoadable.create(
      FloatLoadable.FROM_ZERO.requiredField("hardness", BreakBlock::hardness),
      LEVELS.defaultField("enchantments", Map.of(), BreakBlock::enchantments),
      BreakBlock::new);
  }

  /** Reads the effect object of a shipped fluid effect file, minus the type key the loader registry consumes */
  private static BreakBlock breakBlock(String json) {
    return BreakBlock.LOADABLE.deserialize(GsonHelper.parse(json), CONTEXT);
  }

  @Test
  void fixture_moltenDiamondGrantsFortune() {
    assertThat(breakBlock("""
      {
        "type": "tconstruct:break_block",
        "enchantments": {
          "minecraft:fortune": 3
        },
        "hardness": 10.0
      }"""))
      .isEqualTo(new BreakBlock(10, Map.of(enchantment(Enchantments.FORTUNE), 3)));
  }

  @Test
  void fixture_moltenEmeraldGrantsSilkTouch() {
    assertThat(breakBlock("""
      {
        "type": "tconstruct:break_block",
        "enchantments": {
          "minecraft:silk_touch": 1
        },
        "hardness": 10.0
      }"""))
      .isEqualTo(new BreakBlock(10, Map.of(enchantment(Enchantments.SILK_TOUCH), 1)));
  }
}
