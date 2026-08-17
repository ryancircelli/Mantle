package slimeknights.mantle.item.data;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.primitive.StringLoadable;
import slimeknights.mantle.test.BaseMcTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Tests the naming rules and the uniqueness guard of {@link DataKey} */
class DataKeyTest extends BaseMcTest {
  /** Namespace used by this class, kept apart from the other tests as a name may only be claimed once per JVM */
  private static ResourceLocation id(String path) {
    return new ResourceLocation("mantle_test", "key_" + path);
  }

  private static final DataKey<String> NAME = DataKey.of(id("name"), StringLoadable.DEFAULT);
  private static final DataKey<String> LEGACY = DataKey.ofLegacyName("mantle_test_key_legacy", StringLoadable.DEFAULT);

  @Test
  void of_storesUnderTheNamespacedName() {
    assertThat(NAME.getName()).isEqualTo("mantle_test:key_name");
  }

  @Test
  void of_keepsTheLoadable() {
    assertThat(NAME.getLoadable()).isSameAs(StringLoadable.DEFAULT);
  }

  @Test
  void of_namesCannotCollideWithVanilla() {
    // the whole guarantee against vanilla is that a namespaced name has a colon and no entry vanilla reads does
    assertThat(NAME.getName()).contains(":");
  }

  @Test
  void ofLegacyName_storesUnderTheNameAsGiven() {
    assertThat(LEGACY.getName()).isEqualTo("mantle_test_key_legacy");
  }

  @Test
  void of_rejectsANameAlreadyClaimed() {
    ResourceLocation id = id("duplicate");
    DataKey.of(id, StringLoadable.DEFAULT);
    assertThatThrownBy(() -> DataKey.of(id, IntLoadable.ANY_FULL))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("mantle_test:key_duplicate");
  }

  @Test
  void of_rejectsANameAlreadyClaimedByTheSameLoadable() {
    // two keys with the same name are two owners of one entry no matter how alike they look, so both are rejected
    ResourceLocation id = id("duplicate_same");
    DataKey.of(id, StringLoadable.DEFAULT);
    assertThatThrownBy(() -> DataKey.of(id, StringLoadable.DEFAULT))
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void ofLegacyName_rejectsANameClaimedByANamespacedKey() {
    // both factories name entries of the same tag, so they share one set of claims
    assertThatThrownBy(() -> DataKey.ofLegacyName("mantle_test:key_name", StringLoadable.DEFAULT))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("mantle_test:key_name");
  }

  @Test
  void ofLegacyName_rejectsAVanillaName() {
    assertThatThrownBy(() -> DataKey.ofLegacyName("Damage", IntLoadable.ANY_FULL))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("vanilla");
    assertThatThrownBy(() -> DataKey.ofLegacyName("Unbreakable", StringLoadable.DEFAULT))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("vanilla");
    assertThatThrownBy(() -> DataKey.ofLegacyName("display", StringLoadable.DEFAULT))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("vanilla");
  }

  @Test
  void ofLegacyName_rejectsAnEmptyName() {
    assertThatThrownBy(() -> DataKey.ofLegacyName("", StringLoadable.DEFAULT))
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void keys_areDistinctPerName() {
    // there is at most one key per name, so identity is all equality needs to be
    assertThat(NAME).isNotEqualTo(LEGACY);
    assertThat(DataKey.of(id("distinct"), StringLoadable.DEFAULT)).isNotEqualTo(NAME);
  }

  @Test
  void toString_namesTheKey() {
    assertThat(NAME).hasToString("DataKey['mantle_test:key_name']");
  }
}
