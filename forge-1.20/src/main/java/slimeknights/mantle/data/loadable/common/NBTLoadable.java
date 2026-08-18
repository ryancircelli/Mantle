package slimeknights.mantle.data.loadable.common;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.FriendlyByteBuf;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.data.loadable.OpsHelper;
import slimeknights.mantle.data.loadable.field.LoadableField;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.util.typed.TypedMap;

import javax.annotation.Nullable;
import java.util.function.Function;

/** Loadable for reading NBT, converting from a JSON object to a tag.*/
public enum NBTLoadable implements RecordLoadable<CompoundTag> {
  /** Disallows reading NBT from a string in the Forge style*/
  DISALLOW_STRING,
  /** Allows reading NBT from a string in the forge style */
  ALLOW_STRING;

  /** Converts a value of an arbitrary format into a tag, skipping the conversion when it already is one */
  private static <O> Tag toNbt(DynamicOps<O> ops, O input) {
    if (input instanceof Tag tag) {
      return tag;
    }
    return ops.convertTo(NbtOps.INSTANCE, input);
  }

  /** Converts a tag into a compound, throwing if it is the wrong type */
  private static CompoundTag asCompound(Tag tag, String key) {
    if (tag instanceof CompoundTag compound) {
      return compound;
    }
    throw new JsonSyntaxException("Expected " + key + " to be an object");
  }

  @Override
  public CompoundTag deserialize(JsonObject json, TypedMap context) {
    return deserialize(JsonOps.INSTANCE, OpsHelper.getMap(JsonOps.INSTANCE, json, "[root]"), context);
  }

  @Override
  public <O> CompoundTag deserialize(DynamicOps<O> ops, MapLike<O> map, TypedMap context) {
    return asCompound(toNbt(ops, OpsHelper.toValue(ops, map)), "[root]");
  }

  @Override
  public CompoundTag convert(JsonElement element, String key, TypedMap context) {
    return convert(JsonOps.INSTANCE, element, key, context);
  }

  @Override
  public <O> CompoundTag convert(DynamicOps<O> ops, O input, String key, TypedMap context) {
    if (this == ALLOW_STRING && !OpsHelper.isMap(ops, input)) {
      try {
        return TagParser.parseTag(OpsHelper.getString(ops, input, key));
      } catch (CommandSyntaxException e) {
        throw new JsonSyntaxException("Invalid NBT Entry: ", e);
      }
    }
    return asCompound(toNbt(ops, input), key);
  }

  @Override
  public JsonObject serialize(CompoundTag object) {
    return serialize(JsonOps.INSTANCE, object).getAsJsonObject();
  }

  @SuppressWarnings("unchecked")  // safe, the tag is the value type when writing to NbtOps
  @Override
  public <O> O serialize(DynamicOps<O> ops, CompoundTag object) {
    if (ops == NbtOps.INSTANCE) {
      return (O)object;
    }
    return NbtOps.INSTANCE.convertTo(ops, object);
  }

  @Override
  public void serialize(CompoundTag object, JsonObject json) {
    json.entrySet().addAll(serialize(object).entrySet());
  }

  @Override
  public <O> RecordBuilder<O> serialize(DynamicOps<O> ops, CompoundTag object, RecordBuilder<O> builder) {
    for (String key : object.getAllKeys()) {
      Tag value = object.get(key);
      if (value != null) {
        builder = builder.add(key, NbtOps.INSTANCE.convertTo(ops, value));
      }
    }
    return builder;
  }

  @Override
  public CompoundTag decode(FriendlyByteBuf buffer, TypedMap context) {
    CompoundTag tag = buffer.readNbt();
    if (tag == null) {
      return new CompoundTag();
    }
    return tag;
  }

  @Override
  public void encode(FriendlyByteBuf buffer, CompoundTag object) {
    buffer.writeNbt(object);
  }

  @Override
  public <P> LoadableField<CompoundTag,P> nullableField(String key, Function<P,CompoundTag> getter) {
    return new NullableNBTField<>(this, key, getter);
  }


  /** Special implementation of nullable field to compact the buffer since it natively handles nullable NBT */
  private record NullableNBTField<P>(Loadable<CompoundTag> loadable, String key, Function<P,CompoundTag> getter) implements LoadableField<CompoundTag,P> {
    @Nullable
    @Override
    public CompoundTag get(JsonObject json, String key, TypedMap context) {
      return loadable.getOrDefault(json, key, null, context);
    }

    @Nullable
    @Override
    public <O> CompoundTag get(DynamicOps<O> ops, MapLike<O> map, String key, TypedMap context) {
      return loadable.getOrDefault(ops, map, key, null, context);
    }

    @Override
    public void serialize(P parent, JsonObject json) {
      CompoundTag nbt = getter.apply(parent);
      if (nbt != null) {
        json.add(key, loadable.serialize(nbt));
      }
    }

    @Override
    public <O> RecordBuilder<O> serialize(DynamicOps<O> ops, P parent, RecordBuilder<O> builder) {
      CompoundTag nbt = getter.apply(parent);
      if (nbt != null) {
        return builder.add(key, loadable.serialize(ops, nbt));
      }
      return builder;
    }

    @Nullable
    @Override
    public CompoundTag decode(FriendlyByteBuf buffer, TypedMap context) {
      return buffer.readNbt();
    }

    @Override
    public void encode(FriendlyByteBuf buffer, P parent) {
      buffer.writeNbt(getter.apply(parent));
    }
  }
}
