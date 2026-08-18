package slimeknights.mantle.test;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraftforge.network.NetworkHooks;
import org.junit.jupiter.api.BeforeAll;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * Base class for any test needing the vanilla registries, notably anything touching a registry backed loadable.
 * Bootstrapping is only done once per JVM as {@link Bootstrap#bootStrap()} guards itself.
 */
public class BaseMcTest {
  @BeforeAll
  static void setUpRegistries() {
    SharedConstants.setVersion(TestWorldVersion.INSTANCE);
    // network hooks static init reaches for a mod container we do not have, so keep it out of the bootstrap
    try (MockedStatic<NetworkHooks> mockNetwork = Mockito.mockStatic(NetworkHooks.class)) {
      Bootstrap.bootStrap();
    }
  }
}
