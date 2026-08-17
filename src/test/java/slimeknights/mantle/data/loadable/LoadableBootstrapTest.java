package slimeknights.mantle.data.loadable;

import com.google.gson.JsonPrimitive;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;
import slimeknights.mantle.test.BaseMcTest;

import static org.assertj.core.api.Assertions.assertThat;

/** Sanity check that the test source set can reach both plain and registry backed loadables */
class LoadableBootstrapTest extends BaseMcTest {
  @Test
  void primitiveLoadable_roundTripsThroughJson() {
    assertThat(Loadables.RESOURCE_LOCATION.serialize(Loadables.ITEM.getKey(Items.STICK))).isInstanceOf(JsonPrimitive.class);
  }

  @Test
  void registryLoadable_findsVanillaItem() {
    assertThat(Loadables.ITEM.convert(new JsonPrimitive("minecraft:stick"), "item")).isSameAs(Items.STICK);
  }
}
