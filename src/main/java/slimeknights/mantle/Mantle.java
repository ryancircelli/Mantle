package slimeknights.mantle;

import net.minecraft.Util;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import slimeknights.mantle.config.Config;
import slimeknights.mantle.fluid.transfer.FluidContainerTransferManager;
import slimeknights.mantle.network.MantleNetwork;
import slimeknights.mantle.recipe.MantleIngredients;
import slimeknights.mantle.recipe.MantleRecipes;
import slimeknights.mantle.recipe.condition.MantleConditions;
import slimeknights.mantle.recipe.helper.TagPreference;

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

    FluidContainerTransferManager.INSTANCE.init();
    // TODO(M-datagen): restore once slimeknights.mantle.datagen ports - MantleTags.init();

    // packets are registered in common setup as they always were; the channel itself is not built until
    // RegisterPayloadHandlersEvent, which NeoForge fires after every setup event
    modBus.addListener(EventPriority.NORMAL, false, FMLCommonSetupEvent.class, this::commonSetup);
    modBus.addListener(EventPriority.NORMAL, false, RegisterPayloadHandlersEvent.class, MantleNetwork.INSTANCE::registerPayloads);
    modBus.addListener(EventPriority.NORMAL, false, RegisterEvent.class, this::register);
    MantleConditions.init(modBus);
    MantleIngredients.init(modBus);
    MantleRecipes.init(modBus);

    // TODO(M-capability): restore once util/OffhandCooldownTracker ports (needs slimeknights.mantle.network)
    // bus.addListener(EventPriority.NORMAL, false, RegisterCapabilitiesEvent.class, this::registerCapabilities);

    // TODO(M-datagen): restore once slimeknights.mantle.datagen ports
    // bus.addListener(EventPriority.NORMAL, false, GatherDataEvent.class, this::gatherData);

    // TODO(M-item): restore once slimeknights.mantle.item.LecternBookItem ports
    // NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, PlayerInteractEvent.RightClickBlock.class, LecternBookItem::interactWithBlock);

    // TODO(M-client): restore once slimeknights.mantle.client ports
    // if (FMLEnvironment.dist == Dist.CLIENT) {
    //   ClientEvents.onConstruct();
    // }
  }

  private void commonSetup(FMLCommonSetupEvent event) {
    MantleNetwork.registerPackets();
    TagPreference.init();
    // TODO(M-command/util/loot): common setup also called MantleCommand.init(), OffhandCooldownTracker.init()
    // and LootTableInjector.init() - all still behind the frontier
  }

  private void register(RegisterEvent event) {
    ResourceKey<?> key = event.getRegistryKey();
    if (key == Registries.RECIPE_SERIALIZER) {
      // TODO(M-loot/predicate/command): register() also wired the predicate loaders, block entity signs,
      // the command argument type and the loot modifier

      // fluid container transfer
      FluidContainerTransferManager.registerDefaults();
    }
    MantleConditions.registerLootConditions(event);
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
