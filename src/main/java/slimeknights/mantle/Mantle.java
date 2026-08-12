package slimeknights.mantle;

import net.minecraft.Util;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
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
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import slimeknights.mantle.command.MantleCommand;
import slimeknights.mantle.config.Config;
import slimeknights.mantle.datagen.MantleBlockTagProvider;
import slimeknights.mantle.datagen.MantleFluidTagProvider;
import slimeknights.mantle.datagen.MantleFluidTooltipProvider;
import slimeknights.mantle.datagen.MantleFluidTransferProvider;
import slimeknights.mantle.datagen.MantleMenuTagProvider;
import slimeknights.mantle.datagen.MantleTags;
import slimeknights.mantle.fluid.transfer.FluidContainerTransferManager;
import slimeknights.mantle.loot.LootTableInjector;
import slimeknights.mantle.loot.MantleLoot;
import slimeknights.mantle.network.MantleNetwork;
import slimeknights.mantle.util.OffhandCooldownTracker;
import slimeknights.mantle.recipe.MantleIngredients;
import slimeknights.mantle.recipe.MantleRecipes;
import slimeknights.mantle.recipe.condition.MantleConditions;
import slimeknights.mantle.recipe.helper.TagPreference;
import slimeknights.mantle.registration.MantleRegistrations;

import java.util.concurrent.CompletableFuture;

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
    MantleTags.init();

    // packets are registered in common setup as they always were; the channel itself is not built until
    // RegisterPayloadHandlersEvent, which NeoForge fires after every setup event
    modBus.addListener(EventPriority.NORMAL, false, FMLCommonSetupEvent.class, this::commonSetup);
    modBus.addListener(EventPriority.NORMAL, false, RegisterPayloadHandlersEvent.class, MantleNetwork.INSTANCE::registerPayloads);
    modBus.addListener(EventPriority.NORMAL, false, RegisterEvent.class, this::register);
    modBus.addListener(EventPriority.NORMAL, false, GatherDataEvent.class, this::gatherData);
    MantleConditions.init(modBus);
    MantleIngredients.init(modBus);
    MantleRecipes.init(modBus);
    MantleRegistrations.init(modBus);
    MantleLoot.init(modBus);
    OffhandCooldownTracker.init(modBus);

    // note: slimeknights.mantle.block.entity.InventoryBlockEntity#registerCapability is the RegisterCapabilitiesEvent
    // hook for item handler exposure - Mantle itself registers no InventoryBlockEntity subclass of its own, so there
    // is nothing to call it with yet; downstream mods call it once per block entity type from their own listener.
    // OffhandCooldownTracker, 1.20's other capability, is a data attachment now and registers itself above.

    // TODO(M-item): restore once slimeknights.mantle.item.LecternBookItem ports
    // NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, PlayerInteractEvent.RightClickBlock.class, LecternBookItem::interactWithBlock);

    // TODO(M-client): restore once slimeknights.mantle.client ports
    // if (FMLEnvironment.dist == Dist.CLIENT) {
    //   ClientEvents.onConstruct();
    // }
  }

  private void commonSetup(FMLCommonSetupEvent event) {
    MantleNetwork.registerPackets();
    MantleCommand.init();
    TagPreference.init();
    LootTableInjector.init();
  }

  /** Registers Mantle's own datagen providers */
  private void gatherData(GatherDataEvent event) {
    DataGenerator generator = event.getGenerator();
    boolean server = event.includeServer();
    boolean client = event.includeClient();
    PackOutput packOutput = generator.getPackOutput();
    CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();
    ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
    generator.addProvider(server, new MantleBlockTagProvider(packOutput, lookupProvider, existingFileHelper));
    generator.addProvider(server, new MantleFluidTagProvider(packOutput, lookupProvider, existingFileHelper));
    generator.addProvider(server, new MantleMenuTagProvider(packOutput, lookupProvider, existingFileHelper));
    generator.addProvider(server, new MantleFluidTransferProvider(packOutput));
    generator.addProvider(client, new MantleFluidTooltipProvider(packOutput));
  }

  private void register(RegisterEvent event) {
    ResourceKey<?> key = event.getRegistryKey();
    if (key == Registries.RECIPE_SERIALIZER) {
      // TODO(M-predicate/command): register() also wired the predicate loaders and the command argument type

      // fluid container transfer
      FluidContainerTransferManager.registerDefaults();
    }
    MantleConditions.registerLootConditions(event);
    MantleLoot.register(event);
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
