package slimeknights.mantle.loot;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.primitive.StringLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Record holding a list of entries to inject into the given loot table
 */
public record LootTableInjection(ResourceKey<LootTable> name, List<LootPoolInjection> pools) {
  public static final RecordLoadable<LootTableInjection> LOADABLE = RecordLoadable.create(
    Loadables.resourceKey(Registries.LOOT_TABLE).requiredField("name", LootTableInjection::name),
    LootPoolInjection.LOADABLE.list(1).requiredField("pools", LootTableInjection::pools),
    LootTableInjection::new);

  /**
   * Record holding a list of entries to inject into the given pool
   */
  public record LootPoolInjection(String name, List<LootPoolEntryContainer> entries) {
    public static final RecordLoadable<LootPoolInjection> LOADABLE = RecordLoadable.create(
      StringLoadable.DEFAULT.requiredField("name", LootPoolInjection::name),
      Loadables.LOOT_ENTRY.list(1).requiredField("entries", LootPoolInjection::entries),
      LootPoolInjection::new);

    /**
     * Injects this into the given loot pool
     * @apiNote  {@link LootPool#entries} is a {@code List} as of 1.21 (an array pre-1.21), so this appends by
     *           building a combined list rather than {@code Arrays.copyOf}/{@code System.arraycopy}.
     */
    public void inject(LootTable table) {
      LootPool pool = table.getPool(name);
      //noinspection ConstantConditions method is annotated wrongly
      if (pool != null) {
        List<LootPoolEntryContainer> combined = new ArrayList<>(pool.entries.size() + entries.size());
        combined.addAll(pool.entries);
        combined.addAll(entries);
        pool.entries = List.copyOf(combined);
      } else {
        Mantle.logger.warn("Failed to inject loot into {} pool {}", table.getLootTableId(), name);
      }
    }
  }

  /** Builder instance for a loot table injection */
  public static class Builder {
    private final Map<String,List<LootPoolEntryContainer>> pools = new LinkedHashMap<>();

    /** Inserts the given entries into the pool */
    @CanIgnoreReturnValue
    public Builder addToPool(String name, LootPoolEntryContainer... entries) {
      Collections.addAll(pools.computeIfAbsent(name, n -> new ArrayList<>()), entries);
      return this;
    }

    /** Inserts the given entries into the pool */
    @CanIgnoreReturnValue
    public Builder addToPool(LootPoolInjection injection) {
      pools.computeIfAbsent(injection.name, n -> new ArrayList<>()).addAll(injection.entries);
      return this;
    }

    /** Builds the list of injections */
    public LootTableInjection build(ResourceKey<LootTable> name) {
      return new LootTableInjection(name, pools.entrySet().stream().map(entry -> new LootPoolInjection(entry.getKey(), List.copyOf(entry.getValue()))).toList());
    }
  }
}
