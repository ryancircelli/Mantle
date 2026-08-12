package slimeknights.mantle.util;

import net.minecraftforge.common.util.LazyOptional;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link LogicHelper#orElseNull(LazyOptional)} is the resolution primitive {@link CapabilityHelper} delegates to for
 * every overload, so its present/absent behavior is covered here directly: a plain {@link LazyOptional} needs no
 * Forge capability registration to construct, unlike the {@code ForgeCapabilities} constants
 * {@link CapabilityHelper} passes it, which only resolve inside a running Forge instance and so cannot be exercised
 * by this plain JUnit suite.
 */
class LogicHelperTest {
  @Test
  void orElseNull_present_returnsValue() {
    LazyOptional<String> present = LazyOptional.of(() -> "value");

    assertThat(LogicHelper.orElseNull(present)).isEqualTo("value");
  }

  @Test
  void orElseNull_empty_returnsNull() {
    LazyOptional<String> empty = LazyOptional.empty();

    assertThat(LogicHelper.orElseNull(empty)).isNull();
  }

  @Test
  void orElseNull_invalidated_returnsNull() {
    // mirrors a capability provider swapping/unloading out from under a resolved LazyOptional
    LazyOptional<String> invalidated = LazyOptional.of(() -> "value");
    invalidated.invalidate();

    assertThat(LogicHelper.orElseNull(invalidated)).isNull();
  }
}
