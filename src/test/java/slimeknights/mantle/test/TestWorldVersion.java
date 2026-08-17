package slimeknights.mantle.test;

import net.minecraft.SharedConstants;
import net.minecraft.WorldVersion;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.level.storage.DataVersion;

import java.util.Date;

/** World version instance used to satisfy {@link SharedConstants} during tests */
public class TestWorldVersion implements WorldVersion {
  public static final TestWorldVersion INSTANCE = new TestWorldVersion();
  private static final DataVersion DATA = new DataVersion(0, "mantle_test");

  @Override
  public DataVersion getDataVersion() {
    return DATA;
  }

  @Override
  public String getId() {
    return "mantle_test";
  }

  @Override
  public String getName() {
    return "Mantle Automated Test";
  }

  @Override
  public int getProtocolVersion() {
    return SharedConstants.getProtocolVersion();
  }

  @Override
  public int getPackVersion(PackType packType) {
    return 0;
  }

  @Override
  public Date getBuildTime() {
    return new Date();
  }

  @Override
  public boolean isStable() {
    return true;
  }
}
