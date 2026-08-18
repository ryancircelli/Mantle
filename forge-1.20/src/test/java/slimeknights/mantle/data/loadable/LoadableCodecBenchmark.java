package slimeknights.mantle.data.loadable;

import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import slimeknights.mantle.data.loadable.field.LoadableField;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.primitive.StringLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.test.LoadableTest;

import java.util.List;
import java.util.function.LongSupplier;

/**
 * Times the codec view of a loadable against the JSON bridge it replaced, to show what a caller passing a format other
 * than gson pays for the conversion.
 * <p>
 * Disabled as a timing loop is not a correctness test and its numbers are meaningless on a loaded build machine. Run it
 * by hand with {@code ./gradlew test --tests '*LoadableCodecBenchmark' -Djunit.jupiter.conditions.deactivate=*}.
 */
@Disabled("benchmark, run by hand")
class LoadableCodecBenchmark extends LoadableTest {
  private static final int WARMUP = 20_000;
  private static final int RUNS = 200_000;

  /* A record of the shape and depth a recipe tends to have */

  private record Leaf(int a, int b, String c, List<Integer> list) {}

  private static final LoadableField<Integer,Leaf> A = IntLoadable.ANY_FULL.requiredField("a", Leaf::a);
  private static final LoadableField<Integer,Leaf> B = IntLoadable.ANY_FULL.requiredField("b", Leaf::b);
  private static final LoadableField<String,Leaf> C = StringLoadable.DEFAULT.requiredField("c", Leaf::c);
  private static final LoadableField<List<Integer>,Leaf> LIST = IntLoadable.ANY_FULL.list().requiredField("list", Leaf::list);
  private static final RecordLoadable<Leaf> LEAF = RecordLoadable.create(A, B, C, LIST, Leaf::new);

  private record Branch(Leaf first, Leaf second, List<Leaf> children, String name) {}

  private static final RecordLoadable<Branch> BRANCH = RecordLoadable.create(
    LEAF.requiredField("first", Branch::first),
    LEAF.requiredField("second", Branch::second),
    LEAF.list().requiredField("children", Branch::children),
    StringLoadable.DEFAULT.requiredField("name", Branch::name),
    Branch::new);

  private static Leaf leaf(int seed) {
    return new Leaf(seed, seed * 2, "leaf" + seed, List.of(seed, seed + 1, seed + 2));
  }

  private static final Branch VALUE = new Branch(leaf(1), leaf(2), List.of(leaf(3), leaf(4), leaf(5)), "branch");

  /** The bridge this PR removed: convert the caller's value into gson, then read the gson */
  private record BridgeCodec<T>(Loadable<T> loadable) implements Codec<T> {
    @Override
    public <O> DataResult<Pair<T,O>> decode(DynamicOps<O> ops, O input) {
      JsonElement json = ops.convertTo(JsonOps.INSTANCE, input);
      return ErrorFactory.catching(() -> loadable.convert(json, "codec")).map(value -> Pair.of(value, ops.empty()));
    }

    @Override
    public <O> DataResult<O> encode(T input, DynamicOps<O> ops, O prefix) {
      return ErrorFactory.catching(() -> JsonOps.INSTANCE.convertTo(ops, loadable.serialize(input)));
    }
  }

  /** Runs the supplier the configured number of times, returning nanoseconds per run */
  private static double time(String name, LongSupplier run) {
    for (int i = 0; i < WARMUP; i++) {
      run.getAsLong();
    }
    long start = System.nanoTime();
    long guard = 0;
    for (int i = 0; i < RUNS; i++) {
      guard += run.getAsLong();
    }
    double perRun = (System.nanoTime() - start) / (double)RUNS;
    System.out.printf("%-28s %8.0f ns/op  (guard %d)%n", name, perRun, guard);
    return perRun;
  }

  @Test
  void decodeThroughNbtOps() {
    Codec<Branch> nativeCodec = BRANCH.codec();
    Codec<Branch> bridgeCodec = new BridgeCodec<>(BRANCH);
    Tag tag = write(nativeCodec, NbtOps.INSTANCE, VALUE);

    double bridge = time("nbt decode, json bridge", () -> bridgeCodec.decode(NbtOps.INSTANCE, tag).result().orElseThrow().getFirst().hashCode());
    double direct = time("nbt decode, native", () -> nativeCodec.decode(NbtOps.INSTANCE, tag).result().orElseThrow().getFirst().hashCode());
    System.out.printf("native is %.2fx the bridge%n", bridge / direct);
  }

  @Test
  void encodeThroughNbtOps() {
    Codec<Branch> nativeCodec = BRANCH.codec();
    Codec<Branch> bridgeCodec = new BridgeCodec<>(BRANCH);

    double bridge = time("nbt encode, json bridge", () -> bridgeCodec.encodeStart(NbtOps.INSTANCE, VALUE).result().orElseThrow().hashCode());
    double direct = time("nbt encode, native", () -> nativeCodec.encodeStart(NbtOps.INSTANCE, VALUE).result().orElseThrow().hashCode());
    System.out.printf("native is %.2fx the bridge%n", bridge / direct);
  }

  @Test
  void decodeThroughJsonOps() {
    // the format the bridge was written for, where it should be a wash
    Codec<Branch> nativeCodec = BRANCH.codec();
    Codec<Branch> bridgeCodec = new BridgeCodec<>(BRANCH);
    JsonElement json = write(nativeCodec, JsonOps.INSTANCE, VALUE);

    double bridge = time("json decode, json bridge", () -> bridgeCodec.decode(JsonOps.INSTANCE, json).result().orElseThrow().getFirst().hashCode());
    double direct = time("json decode, native", () -> nativeCodec.decode(JsonOps.INSTANCE, json).result().orElseThrow().getFirst().hashCode());
    System.out.printf("native is %.2fx the bridge%n", bridge / direct);
  }
}
