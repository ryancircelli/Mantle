package slimeknights.mantle.data.gson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.resources.ResourceLocation;

/**
 * Holder for Mantle's shared gson instance.
 * @apiNote  {@link #DEFAULT} is {@code slimeknights.mantle.util.JsonHelper#DEFAULT_GSON}, which cannot be called from
 *           the data packages while that class still depends on the unported network package. When network lands,
 *           JsonHelper's constant should become an alias of this one rather than a second instance; this is the same
 *           split M4 made for {@code ResourceLocationLoadable#parse}.
 */
public class MantleGson {
  private MantleGson() {}

  /** Default GSON instance, use instead of creating a new instance unless you need additional type adapters */
  public static final Gson DEFAULT = new GsonBuilder()
    .registerTypeAdapter(ResourceLocation.class, new ResourceLocation.Serializer())
    .setPrettyPrinting()
    .disableHtmlEscaping()
    .create();
}
