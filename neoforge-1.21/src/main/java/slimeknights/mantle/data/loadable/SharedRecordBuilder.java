package slimeknights.mantle.data.loadable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.RecordBuilder;
import slimeknights.mantle.data.loadable.record.RecordLoadable;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

/**
 * Record builder holding the fields written so far in the order they were written, the arbitrary format counterpart of
 * the single {@link JsonObject} every field of a record shares on the gson path.
 * <p>
 * A {@link RecordBuilder} is write only, so a field handed one cannot tell what the fields before it wrote. That
 * breaks the serialization contract of {@link RecordLoadable}, where a later field is free to read and edit the work
 * of an earlier one. This builder collects the fields itself, hands them to any field asking through
 * {@link OpsHelper#serializeJson(DynamicOps, RecordBuilder, Consumer)}, and copies the finished set into the builder
 * it wraps when built.
 * <p>
 * As with any record builder, callers must chain the returned instance rather than assume it was modified in place;
 * the fields are only handed to the wrapped builder by {@link #build(Object)}.
 * @param <T>  Format of the builder
 * @see OpsHelper#sharedBuilder(DynamicOps)
 */
class SharedRecordBuilder<T> implements RecordBuilder<T> {
  private final DynamicOps<T> ops;
  /** Builder receiving the fields once this one is built */
  private final RecordBuilder<T> target;
  /** Fields written so far, iterating in the order the fields were written */
  private final Map<String,T> fields = new LinkedHashMap<>();
  /** Errors and lifecycle picked up from failed writes, carried into the eventual build */
  private DataResult<Unit> state = DataResult.success(Unit.INSTANCE, Lifecycle.stable());

  SharedRecordBuilder(DynamicOps<T> ops, RecordBuilder<T> target) {
    this.ops = ops;
    this.target = target;
  }

  @Override
  public DynamicOps<T> ops() {
    return ops;
  }


  /* Writing */

  @Override
  public RecordBuilder<T> add(String key, T value) {
    fields.put(key, value);
    return this;
  }

  @Override
  public RecordBuilder<T> add(String key, DataResult<T> value) {
    value.result().ifPresent(present -> fields.put(key, present));
    return withErrorsFrom(value);
  }

  @Override
  public RecordBuilder<T> add(T key, T value) {
    return addKey(ops.getStringValue(key), DataResult.success(value));
  }

  @Override
  public RecordBuilder<T> add(T key, DataResult<T> value) {
    return addKey(ops.getStringValue(key), value);
  }

  @Override
  public RecordBuilder<T> add(DataResult<T> key, DataResult<T> value) {
    return addKey(key.flatMap(ops::getStringValue), value);
  }

  /** Shared implementation of the adders taking a key which may have failed to resolve */
  private RecordBuilder<T> addKey(DataResult<String> key, DataResult<T> value) {
    key.result().ifPresent(present -> add(present, value));
    return withErrorsFrom(key);
  }

  @Override
  public RecordBuilder<T> withErrorsFrom(DataResult<?> result) {
    state = state.flatMap(unit -> result.map(value -> unit));
    return this;
  }

  @Override
  public RecordBuilder<T> setLifecycle(Lifecycle lifecycle) {
    state = state.setLifecycle(lifecycle);
    return this;
  }

  @Override
  public RecordBuilder<T> mapError(UnaryOperator<String> onError) {
    state = state.mapError(onError);
    return this;
  }

  /**
   * Hands every field collected so far to the builder this one wraps, emptying this builder.
   * @return  Wrapped builder containing the fields, for chaining
   */
  RecordBuilder<T> flush() {
    RecordBuilder<T> built = target;
    for (Entry<String,T> entry : fields.entrySet()) {
      built = built.add(entry.getKey(), entry.getValue());
    }
    fields.clear();
    return built.withErrorsFrom(state);
  }

  @Override
  public DataResult<T> build(T prefix) {
    return flush().build(prefix);
  }


  /* Reading */

  /** {@return the value written for the given key, or null if no field wrote it} */
  @Nullable
  public T get(String key) {
    return fields.get(key);
  }

  /** {@return the fields written so far as a JSON object} */
  private JsonObject toJson() {
    JsonObject json = new JsonObject();
    for (Entry<String,T> entry : fields.entrySet()) {
      json.add(entry.getKey(), OpsHelper.toJson(ops, entry.getValue()));
    }
    return json;
  }

  /**
   * Runs a gson serializer against the fields written so far, then takes back whatever it left behind. Gives a
   * serializer written against gson the same view of the record the gson path would have given it.
   * @param serializer  Serializer to run, receiving the fields written so far
   * @return  This builder, for chaining
   */
  public RecordBuilder<T> addJson(Consumer<JsonObject> serializer) {
    JsonObject json = toJson();
    JsonObject before = json.deepCopy();
    serializer.accept(json);
    // take back only what the serializer touched, so a field it left alone keeps the form its own writer chose
    for (Entry<String,JsonElement> entry : json.entrySet()) {
      if (!entry.getValue().equals(before.get(entry.getKey()))) {
        fields.put(entry.getKey(), OpsHelper.fromJson(ops, entry.getValue()));
      }
    }
    // a serializer is equally free to drop a field an earlier one wrote
    for (String key : before.keySet()) {
      if (!json.has(key)) {
        fields.remove(key);
      }
    }
    return this;
  }

  @Override
  public String toString() {
    return "SharedRecordBuilder" + fields.keySet();
  }
}
