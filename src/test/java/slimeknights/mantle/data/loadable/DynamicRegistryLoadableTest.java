package slimeknights.mantle.data.loadable;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import org.junit.jupiter.api.Test;
import slimeknights.mantle.data.loadable.field.ContextKey;
import slimeknights.mantle.test.LoadableTest;
import slimeknights.mantle.util.typed.TypedMap;
import slimeknights.mantle.util.typed.TypedMapBuilder;

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
    assertThatThrownBy(() -> Loadables.ENCHANTMENT.convert(OPS, new com.google.gson.JsonObject(), KEY))
      .isInstanceOf(JsonSyntaxException.class)
      .hasMessageContaining("to be a string");
  }
}
