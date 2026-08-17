package slimeknights.mantle.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import slimeknights.mantle.test.BaseMcTest;
import sun.reflect.ReflectionFactory;

import javax.annotation.Nullable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers the two flow predicates {@link InvertedFluid} keeps its own copies of, {@code isCeilingHole} and
 * {@code canFluidPassThrough}. Both used to be calls into {@code FlowingFluid}'s private pair, reached through an
 * access transformer; lithium overwrites that pair with private methods of its own, so Mantle carries the logic
 * itself now. These tests pin the copies' behaviour, above all that the water hole check looks up rather than down.
 * <p>
 * The predicates are private, as nothing outside the class may call them and nothing may override them, so the test
 * reaches them reflectively. Looking them up by exact signature is itself part of the guard: were either turned back
 * into an override of the vanilla method, the lookup here would fail.
 */
class InvertedFluidTest extends BaseMcTest {
  /** Position the fluid flows from in every fixture */
  private static final BlockPos POS = new BlockPos(0, 64, 0);
  /** The space above {@link #POS}, which is where an inverted fluid tries to go */
  private static final BlockPos ABOVE = POS.above();
  /** A horizontal neighbor of {@link #POS}, for the side spread predicate */
  private static final BlockPos SIDE = POS.north();
  /** Occludes its own downward face and leaves its upward one open */
  private static final BlockState BOTTOM_SLAB = Blocks.OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
  /** The mirror of {@link #BOTTOM_SLAB}: occludes upward, open downward */
  private static final BlockState TOP_SLAB = Blocks.OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP);

  private static Method isCeilingHole;
  private static Method canFluidPassThrough;
  private static InvertedFluid fluid;

  @BeforeAll
  static void setUpFluid() {
    isCeilingHole = privateMethod(
      "isCeilingHole",
      BlockGetter.class, Fluid.class, BlockPos.class, BlockState.class, BlockPos.class, BlockState.class);
    canFluidPassThrough = privateMethod(
      "canFluidPassThrough",
      BlockGetter.class, Fluid.class, BlockPos.class, BlockState.class, Direction.class, BlockPos.class, BlockState.class, FluidState.class);
    fluid = allocateFluid();
  }

  /**
   * Builds an {@link InvertedFluid} without running a constructor, which is the only way to hold one in a plain unit
   * test: constructing a {@link Fluid} claims an intrusive holder in the fluid registry, and the bootstrap the test
   * base runs leaves that registry frozen. Both predicates read only their arguments, so an instance with no fields
   * set is enough for them, and any test that grew to need real state would fail loudly rather than quietly lie.
   */
  private static InvertedFluid allocateFluid() {
    try {
      Constructor<?> constructor = ReflectionFactory.getReflectionFactory()
        .newConstructorForSerialization(InvertedFluid.Flowing.class, Object.class.getDeclaredConstructor());
      return (InvertedFluid)constructor.newInstance();
    } catch (ReflectiveOperationException e) {
      throw new AssertionError("Failed to allocate an InvertedFluid", e);
    }
  }

  private static Method privateMethod(String name, Class<?>... parameters) {
    try {
      Method method = InvertedFluid.class.getDeclaredMethod(name, parameters);
      method.setAccessible(true);
      return method;
    } catch (NoSuchMethodException e) {
      throw new AssertionError("InvertedFluid must keep its own private " + name, e);
    }
  }

  private static boolean call(Method method, Object... arguments) {
    try {
      return (Boolean)method.invoke(fluid, arguments);
    } catch (IllegalAccessException e) {
      throw new AssertionError(e);
    } catch (InvocationTargetException e) {
      throw new AssertionError(e.getCause());
    }
  }

  private static boolean isCeilingHole(TestLevel level, BlockPos pos, BlockPos spreadPos) {
    return call(isCeilingHole, level, fluid, pos, level.getBlockState(pos), spreadPos, level.getBlockState(spreadPos));
  }

  private static boolean canFluidPassThrough(TestLevel level, BlockPos pos, Direction direction, BlockPos spreadPos) {
    return call(
      canFluidPassThrough, level, fluid, pos, level.getBlockState(pos), direction,
      spreadPos, level.getBlockState(spreadPos), level.getFluidState(spreadPos));
  }

  /* isCeilingHole, the upward mirror of FlowingFluid#isWaterHole */

  @Test
  void isCeilingHole_openSpaceAboveIsAHole() {
    assertThat(isCeilingHole(new TestLevel(), POS, ABOVE)).isTrue();
  }

  @Test
  void isCeilingHole_solidCeilingIsNotAHole() {
    assertThat(isCeilingHole(new TestLevel().set(ABOVE, Blocks.STONE), POS, ABOVE)).isFalse();
  }

  @Test
  void isCeilingHole_readsTheUpwardFaceRatherThanTheDownwardOne() {
    // the two half slabs are each other's mirror, so a predicate reading the wrong face answers them the wrong way
    // round: vanilla, testing DOWN, calls the bottom slab no hole and the top slab a hole. Inverted is the opposite.
    assertThat(isCeilingHole(new TestLevel().set(POS, BOTTOM_SLAB), POS, ABOVE)).isTrue();
    assertThat(isCeilingHole(new TestLevel().set(POS, TOP_SLAB), POS, ABOVE)).isFalse();
  }

  @Test
  void isCeilingHole_ceilingThatCannotHoldFluidIsNotAHole() {
    // a ladder is thin enough for the wall check to pass, but it is one of the blocks a fluid may not occupy
    assertThat(isCeilingHole(new TestLevel().set(ABOVE, Blocks.LADDER), POS, ABOVE)).isFalse();
  }

  /* canFluidPassThrough, a straight copy of the vanilla predicate as every caller supplies the direction */

  @Test
  void canFluidPassThrough_openSideIsPassable() {
    assertThat(canFluidPassThrough(new TestLevel(), POS, Direction.NORTH, SIDE)).isTrue();
  }

  @Test
  void canFluidPassThrough_solidSideIsNotPassable() {
    assertThat(canFluidPassThrough(new TestLevel().set(SIDE, Blocks.STONE), POS, Direction.NORTH, SIDE)).isFalse();
  }

  @Test
  void canFluidPassThrough_sideThatCannotHoldFluidIsNotPassable() {
    assertThat(canFluidPassThrough(new TestLevel().set(SIDE, Blocks.LADDER), POS, Direction.NORTH, SIDE)).isFalse();
  }

  @Test
  void canFluidPassThrough_ignoresTheVerticalFaces() {
    // the horizontal check must not care what is sealing the fluid in from above or below
    TestLevel level = new TestLevel().set(POS, TOP_SLAB).set(ABOVE, Blocks.STONE);
    assertThat(canFluidPassThrough(level, POS, Direction.NORTH, SIDE)).isTrue();
  }

  /** Bare {@link BlockGetter} over a map of block states, reading as air everywhere it has no entry */
  private static class TestLevel implements BlockGetter {
    private final Map<BlockPos,BlockState> blocks = new HashMap<>();

    TestLevel set(BlockPos pos, BlockState state) {
      this.blocks.put(pos.immutable(), state);
      return this;
    }

    TestLevel set(BlockPos pos, Block block) {
      return this.set(pos, block.defaultBlockState());
    }

    @Nullable
    @Override
    public BlockEntity getBlockEntity(BlockPos pos) {
      return null;
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
      return this.blocks.getOrDefault(pos, Blocks.AIR.defaultBlockState());
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
      return this.getBlockState(pos).getFluidState();
    }

    @Override
    public int getHeight() {
      return 384;
    }

    @Override
    public int getMinBuildHeight() {
      return -64;
    }
  }
}
