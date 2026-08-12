package slimeknights.mantle.data.gson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.resources.ResourceLocation;

/**
 * Holder for Mantle's shared gson instance.
 * @apiNote  This is the single instance behind {@link slimeknights.mantle.util.JsonHelper#DEFAULT_GSON}. It lives here
 *           rather than there because the data packages need it and {@code JsonHelper} depends on them, so the constant
 *           has to sit below both.
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
