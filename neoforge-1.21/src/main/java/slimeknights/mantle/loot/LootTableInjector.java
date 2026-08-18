package slimeknights.mantle.loot;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.conditions.ConditionalOps;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.LootTableLoadEvent;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.data.listener.IEarlyReloadListener;
import slimeknights.mantle.loot.LootTableInjection.LootPoolInjection;
import slimeknights.mantle.util.JsonHelper;

import java.io.IOException;
import java.io.Reader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Class handling injecting additional entries into loot tables
 * @apiNote  Conditions on an injector file are read through NeoForge's {@link ICondition} codec under the
 *           {@code neoforge:conditions} key, the same pattern {@code FluidContainerTransferManager} uses - see its
 *           package documentation for the rest of the delta from Forge's bare {@code conditions} key.
 */
public enum LootTableInjector implements IEarlyReloadListener {
  INSTANCE;

  /** Datapack folder for the injector */
  public static final String FOLDER = "mantle/loot_injectors";

  /** Initializes the loot table injector */
  public static void init() {
    NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, AddReloadListenerEvent.class, event -> {
      event.addListener(INSTANCE);
      INSTANCE.conditionOps = conditionOps(event.getRegistryAccess(), event.getConditionContext());
    });
    NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, LootTableLoadEvent.class, INSTANCE::lootTableLoad);
  }

  /** Ops used to test a file's conditions, rebuilt from the reload event every reload */
  private DynamicOps<JsonElement> conditionOps = conditionOps(RegistryAccess.EMPTY, ICondition.IContext.EMPTY);
  /** Map of injections to use on loot table load */
  private Map<ResourceKey<LootTable>,LootTableInjection> injections = Collections.emptyMap();

  @Override
  public void onResourceManagerReload(ResourceManager manager) {
    long time = System.nanoTime();
    Map<ResourceKey<LootTable>,LootTableInjection.Builder> builders = new HashMap<>();
    int loaded = 0;
    for (Entry<ResourceLocation,Resource> entry : manager.listResources(FOLDER, loc -> loc.getPath().endsWith(".json")).entrySet()) {
      try (Reader reader = entry.getValue().openAsReader()) {
        JsonObject json = GsonHelper.fromJson(JsonHelper.DEFAULT_GSON, reader, JsonObject.class);
        if (json != null) {
          // skip if empty for easy removals
          if (!json.keySet().isEmpty() && ICondition.conditionsMatched(conditionOps, json)) {
            // the builder allows us to merge from multiple sources, for efficiency
            // ensures a given table name and pool name both show just once
            LootTableInjection injection = LootTableInjection.LOADABLE.deserialize(json);
            LootTableInjection.Builder builder = builders.computeIfAbsent(injection.name(), id -> new LootTableInjection.Builder());
            for (LootPoolInjection pool : injection.pools()) {
              builder.addToPool(pool);
            }
            loaded++;
          }
        } else {
          Mantle.logger.error("Couldn't parse loot table injection from {} as it's null or empty", entry.getKey());
        }
      } catch (IllegalArgumentException | IOException | JsonParseException ex) {
        Mantle.logger.error("Couldn't parse loot injection from {}", entry.getKey(), ex);
      }
    }
    // build final map
    injections = builders.entrySet().stream().map(entry -> entry.getValue().build(entry.getKey()))
                         .collect(Collectors.toUnmodifiableMap(LootTableInjection::name, Function.identity()));
    // log timer
    Mantle.logger.info("Loaded {} loot table injectors injecting into {} tables in {} ms", loaded, injections.size(), (System.nanoTime() - time) / 1000000f);
  }

  /** Called on loot table load to handle the actual injection */
  private void lootTableLoad(LootTableLoadEvent event) {
    LootTableInjection injection = injections.get(event.getKey());
    if (injection != null) {
      Mantle.logger.debug("Injecting into {} pools in the table {}", injection.pools().size(), injection.name());
      LootTable table = event.getTable();
      for (LootPoolInjection pool : injection.pools()) {
        pool.inject(table);
      }
    }
  }

  /** Builds the condition ops for the given registries and context */
  private static DynamicOps<JsonElement> conditionOps(RegistryAccess registries, ICondition.IContext context) {
    return new ConditionalOps<>(RegistryOps.create(JsonOps.INSTANCE, registries), context);
  }
}
