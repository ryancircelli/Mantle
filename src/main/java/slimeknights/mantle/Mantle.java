package slimeknights.mantle;

import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import slimeknights.mantle.config.Config;

/**
 * Mantle
 *
 * Central mod object for Mantle
 *
 * @author Sunstrike <sun@sunstrike.io>
 */
@Mod(Mantle.modId)
public class Mantle {
  public static final String modId = "mantle";
  public static final Logger logger = LogManager.getLogger("Mantle");
  /** Namespace for common tags, "c" as of 1.21 (was "forge" pre-1.21) */
  public static final String COMMON = "c";

  /* Instance of this mod, used for grabbing prototype fields */
  public static Mantle instance;

  public Mantle(IEventBus modBus, ModContainer container) {
    container.registerConfig(ModConfig.Type.CLIENT, Config.CLIENT_SPEC);
    container.registerConfig(ModConfig.Type.SERVER, Config.SERVER_SPEC);

    instance = this;

    // TODO(M-fluid): restore once slimeknights.mantle.fluid.transfer ports - FluidContainerTransferManager.INSTANCE.init();
    // TODO(M-datagen): restore once slimeknights.mantle.datagen ports - MantleTags.init();

    // TODO(M-client): restore once slimeknights.mantle.client ports
    // bus.addListener(EventPriority.NORMAL, false, FMLCommonSetupEvent.class, this::commonSetup);
    // commonSetup used to call MantleNetwork.registerPackets(), MantleCommand.init(), OffhandCooldownTracker.init(),
    // TagPreference.init(), LootTableInjector.init() - all still behind the frontier (network/command/util/recipe/loot)

    // TODO(M-capability): restore once util/OffhandCooldownTracker ports (needs slimeknights.mantle.network)
    // bus.addListener(EventPriority.NORMAL, false, RegisterCapabilitiesEvent.class, this::registerCapabilities);

    // TODO(M-datagen): restore once slimeknights.mantle.datagen ports
    // bus.addListener(EventPriority.NORMAL, false, GatherDataEvent.class, this::gatherData);

    // TODO(M-recipe/loot/predicate/command): restore once those packages port - register() wired most of Mantle's
    // recipe conditions, ingredient serializers, fluid transfer deserializers, predicate loaders, block entity
    // signs, the command argument type, and the loot modifier registration onto RegisterEvent.
    // bus.addListener(EventPriority.NORMAL, false, RegisterEvent.class, this::register);

    // TODO(M-recipe): restore once slimeknights.mantle.recipe ports - MantleRecipes.init(modBus);

    // TODO(M-item): restore once slimeknights.mantle.item.LecternBookItem ports
    // NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, PlayerInteractEvent.RightClickBlock.class, LecternBookItem::interactWithBlock);

    // TODO(M-client): restore once slimeknights.mantle.client ports
    // if (FMLEnvironment.dist == Dist.CLIENT) {
    //   ClientEvents.onConstruct();
    // }
  }

  /**
   * Gets a resource location for Mantle
   * @param name  Name
   * @return  Resource location instance
   */
  public static ResourceLocation getResource(String name) {
    return ResourceLocation.fromNamespaceAndPath(modId, name);
  }

  /**
   * Gets a resource location for the common namespace, "c" as of 1.21.
   * @param name  Name
   * @return  Resource location instance
   */
  public static ResourceLocation commonResource(String name) {
    return ResourceLocation.fromNamespaceAndPath(COMMON, name);
  }

  /**
   * Makes a translation key for the given name
   * @param base  Base name, such as "block" or "gui"
   * @param name  Object name
   * @return  Translation key
   */
  public static String makeDescriptionId(String base, String name) {
    return Util.makeDescriptionId(base, getResource(name));
  }

  /**
   * Makes a translation text component for the given name
   * @param base  Base name, such as "block" or "gui"
   * @param name  Object name
   * @return  Translation key
   */
  public static MutableComponent makeComponent(String base, String name) {
    return Component.translatable(makeDescriptionId(base, name));
  }

  /**
   * Makes a translation text component for the given name
   * @param base  Base name, such as "block" or "gui"
   * @param name  Object name
   * @param args  Additional arguments to format strings
   * @return  Translation key
   */
  public static MutableComponent makeComponent(String base, String name, Object... args) {
    return Component.translatable(makeDescriptionId(base, name), args);
  }
}
