package slimeknights.mantle.data.loadable;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.data.loadable.record.RecordLoadable;

import java.util.List;
import java.util.stream.Stream;

/**
 * Implementation of a {@link MapCodec} using a {@link RecordLoadable}.
 * <p>
 * This is the shape consumed by anything composing codecs out of fields, notably
 * {@link com.mojang.serialization.codecs.RecordCodecBuilder} and {@link com.mojang.serialization.Codec#dispatch}.
 * The loadable reads and writes the caller's format directly, so no format conversion happens in either direction.
 * @param <T>  Object type
 * @see RecordLoadable#mapCodec()
 */
public final class LoadableMapCodec<T> extends MapCodec<T> {
  /** Error used when the ops wants compressed maps but we cannot list our keys */
  private static final String NO_KEYS = "Loadables cannot be written with compressed maps as they do not enumerate their keys";

  private final RecordLoadable<T> loadable;
  /** Keys this codec promises to read and write, or null if unknown. See {@link RecordLoadable#mapCodec(String...)}. */
  private final List<String> keys;

  public LoadableMapCodec(RecordLoadable<T> loadable) {
    this(loadable, null);
  }

  public LoadableMapCodec(RecordLoadable<T> loadable, List<String> keys) {
    this.loadable = loadable;
    this.keys = keys;
  }

  /** {@return the loadable backing this codec} */
  public RecordLoadable<T> loadable() {
    return loadable;
  }

  /**
   * {@inheritDoc}
   * @implNote  A loadable has no way to list the fields it reads: a field may write itself directly into the parent
   *            ({@link slimeknights.mantle.data.loadable.field.DirectField}), may choose between a key and the parent
   *            at runtime ({@link slimeknights.mantle.data.loadable.field.TryDirectField}), and a loadable written by
   *            hand simply reads a JSON object. The keys are therefore unknown unless the caller declared them, and
   *            this returns an empty stream, which is only consulted when the ops compresses maps. Both
   *            {@link #decode(DynamicOps, MapLike)} and {@link #encode(Object, DynamicOps, RecordBuilder)} refuse such
   *            an ops rather than let the resulting empty key compressor merge every field onto index zero.
   */
  @Override
  public <O> Stream<O> keys(DynamicOps<O> ops) {
    if (keys == null) {
      return Stream.empty();
    }
    return keys.stream().map(ops::createString);
  }

  /** {@return true if the ops cannot be used because it compresses maps and we do not know our keys} */
  private boolean cannotCompress(DynamicOps<?> ops) {
    return keys == null && ops.compressMaps();
  }

  @Override
  public <O> DataResult<T> decode(DynamicOps<O> ops, MapLike<O> input) {
    if (cannotCompress(ops)) {
      return DataResult.error(() -> NO_KEYS);
    }
    return ErrorFactory.catching(
      () -> loadable.deserialize(ops, input),
      e -> Mantle.logger.warn("Unable to decode {}", loadable, e));
  }

  @Override
  public <O> RecordBuilder<O> encode(T input, DynamicOps<O> ops, RecordBuilder<O> prefix) {
    if (cannotCompress(ops)) {
      return prefix.withErrorsFrom(DataResult.error(() -> NO_KEYS));
    }
    DataResult<RecordBuilder<O>> result = ErrorFactory.catching(
      () -> loadable.serialize(ops, input, prefix),
      e -> Mantle.logger.warn("Unable to encode {}", loadable, e));
    // on failure the builder is left as we found it, carrying the error so the eventual build fails
    return result.result().orElseGet(() -> prefix.withErrorsFrom(result));
  }

  @Override
  public String toString() {
    return "LoadableMapCodec[" + loadable + ']';
  }
}
