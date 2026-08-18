package slimeknights.mantle.data.loadable.common;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.OpsHelper;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.util.typed.TypedMap;

import java.util.Optional;

/** Loadable reading block state properties from JSON */
public enum BlockStateLoadable implements RecordLoadable<BlockState> {
  /** Serializes all state properties */
  ALL {
    @Override
    protected <T extends Comparable<T>> void serializeProperty(BlockState serialize, Property<T> property, BlockState defaultState, JsonObject json) {
      json.addProperty(property.getName(), property.getName(serialize.getValue(property)));
    }
  },
  /** Serializes any properties different from the default state */
  DIFFERENCE {
    @Override
    protected <T extends Comparable<T>> void serializeProperty(BlockState serialize, Property<T> property, BlockState defaultState, JsonObject json) {
      T value = serialize.getValue(property);
      if (!value.equals(defaultState.getValue(property))) {
        json.addProperty(property.getName(), property.getName(value));
      }
    }
  };

  @Override
  public BlockState convert(JsonElement element, String key, TypedMap context) {
    return convert(JsonOps.INSTANCE, element, key, context);
  }

  @Override
  public <O> BlockState convert(DynamicOps<O> ops, O input, String key, TypedMap context) {
    // a string means parse the block with default properties
    if (ops.getStringValue(input).result().isPresent()) {
      return Loadables.BLOCK.convert(ops, input, key, context).defaultBlockState();
    }
    return RecordLoadable.super.convert(ops, input, key, context);
  }

  /**
   * Sets the property
   * @param state     State before changes
   * @param property  Property to set
   * @param name      Value name
   * @param <T>  Type of property
   * @return  State with the property
   * @throws JsonSyntaxException  if the property has no element with the given name
   */
  private static <T extends Comparable<T>> BlockState setValue(BlockState state, Property<T> property, String name) {
    Optional<T> value = property.getValue(name);
    if (value.isPresent()) {
      return state.setValue(property, value.get());
    }
    throw new JsonSyntaxException("Property " + property + " does not contain value " + name);
  }

  @Override
  public BlockState deserialize(JsonObject json, TypedMap context) {
    return deserialize(JsonOps.INSTANCE, OpsHelper.getMap(JsonOps.INSTANCE, json, "[root]"), context);
  }

  @Override
  public <O> BlockState deserialize(DynamicOps<O> ops, MapLike<O> map, TypedMap context) {
    Block block = Loadables.BLOCK.getIfPresent(ops, map, "block", context);
    BlockState state = block.defaultBlockState();
    O properties = map.get("properties");
    if (properties != null) {
      StateDefinition<Block,BlockState> definition = block.getStateDefinition();
      for (Pair<O,O> entry : OpsHelper.getMap(ops, properties, "properties").entries().toList()) {
        String key = OpsHelper.getString(ops, entry.getFirst(), "properties");
        Property<?> property = definition.getProperty(key);
        if (property == null) {
          throw new JsonSyntaxException("Property " + key + " does not exist in block " + block);
        }
        state = setValue(state, property, OpsHelper.getString(ops, entry.getSecond(), key));
      }
    }
    return state;
  }

  @Override
  public JsonElement serialize(BlockState state) {
    return serialize(JsonOps.INSTANCE, state);
  }

  @Override
  public <O> O serialize(DynamicOps<O> ops, BlockState state) {
    Block block = state.getBlock();
    if (this == DIFFERENCE && state == block.defaultBlockState()) {
      return Loadables.BLOCK.serialize(ops, block);
    }
    return RecordLoadable.super.serialize(ops, state);
  }

  /** Serializes the property if it differs in the default state */
  protected abstract <T extends Comparable<T>> void serializeProperty(BlockState serialize, Property<T> property, BlockState defaultState, JsonObject json);

  @Override
  public void serialize(BlockState state, JsonObject json) {
    Block block = state.getBlock();
    json.add("block", Loadables.BLOCK.serialize(block));
    BlockState defaultState = block.defaultBlockState();
    JsonObject properties = new JsonObject();
    for (Property<?> property : block.getStateDefinition().getProperties()) {
      serializeProperty(state, property, defaultState, properties);
    }
    if (properties.size() > 0) {
      json.add("properties", properties);
    }
  }

  @Override
  public <O> RecordBuilder<O> serialize(DynamicOps<O> ops, BlockState state, RecordBuilder<O> builder) {
    // properties are chosen by the gson variant, as the property values are strings in every format
    JsonObject properties = new JsonObject();
    Block block = state.getBlock();
    BlockState defaultState = block.defaultBlockState();
    for (Property<?> property : block.getStateDefinition().getProperties()) {
      serializeProperty(state, property, defaultState, properties);
    }
    builder = builder.add("block", Loadables.BLOCK.serialize(ops, block));
    if (properties.size() > 0) {
      builder = builder.add("properties", ops.createMap(properties.entrySet().stream().map(
        entry -> Pair.of(ops.createString(entry.getKey()), ops.createString(entry.getValue().getAsString())))));
    }
    return builder;
  }

  @Override
  public BlockState decode(FriendlyByteBuf buffer, TypedMap context) {
    return Block.stateById(buffer.readVarInt());
  }

  @Override
  public void encode(FriendlyByteBuf buffer, BlockState object) {
    buffer.writeVarInt(Block.getId(object));
  }
}
