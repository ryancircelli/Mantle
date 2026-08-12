package slimeknights.mantle.data.registry;

import com.google.gson.JsonSyntaxException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.loadable.record.SingletonLoader;
import slimeknights.mantle.data.registry.GenericLoaderRegistry.IHaveLoader;
import slimeknights.mantle.test.LoadableTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Round trip tests for the loader registry, which picks an implementation by a type key */
class GenericLoaderRegistryTest extends LoadableTest {
  /** Base type for the registry */
  private interface Shape extends IHaveLoader {}

  /** Implementation with a field */
  private record Sized(int size) implements Shape {
    static final RecordLoadable<Sized> LOADER = RecordLoadable.create(
      IntLoadable.ANY_FULL.requiredField("size", Sized::size), Sized::new);

    @Override
    public RecordLoadable<Sized> getLoader() {
      return LOADER;
    }
  }

  /** Implementation with no fields, which serializes compactly */
  private record Empty() implements Shape {
    static final RecordLoadable<Empty> LOADER = new SingletonLoader<>(new Empty());

    @Override
    public RecordLoadable<Empty> getLoader() {
      return LOADER;
    }
  }

  /** Builds a registry containing both implementations */
  private static GenericLoaderRegistry<Shape> registry(boolean compact) {
    GenericLoaderRegistry<Shape> registry = new GenericLoaderRegistry<>("Shape", compact);
    registry.register(new ResourceLocation("mantle", "sized"), Sized.LOADER);
    registry.register(new ResourceLocation("mantle", "empty"), Empty.LOADER);
    return registry;
  }

  @Test
  void registry_roundTrips() {
    GenericLoaderRegistry<Shape> registry = registry(false);
    assertRoundTrip(registry, new Sized(5));
    assertRoundTrip(registry, new Empty());
  }

  @Test
  void compactRegistry_roundTripsBothShapes() {
    GenericLoaderRegistry<Shape> registry = registry(true);
    assertRoundTrip(registry, new Sized(5));
    assertRoundTrip(registry, new Empty());
  }

  @Test
  void compactRegistry_writesTheCompactForm() {
    GenericLoaderRegistry<Shape> registry = registry(true);
    assertThat(toNbt(registry, new Empty())).isEqualTo(StringTag.valueOf("mantle:empty"));
    assertThat(toNbt(registry, new Sized(5))).isInstanceOf(CompoundTag.class);
  }

  @Test
  void registry_keepsNumericTypes() {
    CompoundTag tag = (CompoundTag)toNbt(registry(false), new Sized(5));
    assertThat(tag.get("type")).isEqualTo(StringTag.valueOf("mantle:sized"));
    assertThat(tag.get("size")).isEqualTo(IntTag.valueOf(5));
  }

  @Test
  void registry_rejectsUnknownType() {
    CompoundTag tag = new CompoundTag();
    tag.putString("type", "mantle:missing");
    assertThatThrownBy(() -> registry(false).convert(NbtOps.INSTANCE, tag, KEY))
      .isInstanceOf(JsonSyntaxException.class);
  }

  @Test
  void registry_rejectsNonObjectWhenNotCompact() {
    assertThatThrownBy(() -> registry(false).convert(NbtOps.INSTANCE, StringTag.valueOf("mantle:empty"), KEY))
      .isInstanceOf(JsonSyntaxException.class)
      .hasMessageContaining(KEY);
  }
}
