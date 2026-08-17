package slimeknights.mantle.client.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.mojang.datafixers.util.Either;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import org.junit.jupiter.api.Test;
import slimeknights.mantle.client.model.connected.ConnectedModel;
import slimeknights.mantle.client.model.connected.ConnectedModelRegistry;
import slimeknights.mantle.client.model.util.ColoredBlockModel;
import slimeknights.mantle.client.model.util.ColoredBlockModel.ColorData;
import slimeknights.mantle.client.model.util.MantleItemLayerModel;
import slimeknights.mantle.client.model.util.SimpleBlockModel;
import slimeknights.mantle.test.BaseMcTest;

import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Parse level coverage of the model loaders: everything reachable from a model JSON without a baking context, an
 * atlas, or a GL context, which is the whole of {@link slimeknights.mantle.client.model} that can be tested headless.
 * Baking needs a GL context and is not covered here.
 * <p>
 * These loaders are {@link JsonDeserializer}s rather than codecs, so they need a live {@link Gson} to supply the
 * {@link JsonDeserializationContext} they read nested block elements through. {@link #GSON} below is
 * the vanilla element adapters plus one adapter per loader, which is exactly what
 * {@link net.neoforged.neoforge.client.model.ExtendedBlockModelDeserializer} hands a loader at runtime, minus the
 * geometry loader registry that only exists in a loaded game.
 */
class ModelLoaderTest extends BaseMcTest {
  private static final Gson GSON = new GsonBuilder()
    .registerTypeAdapter(BlockElement.class, new BlockElement.Deserializer())
    .registerTypeAdapter(BlockElementFace.class, new BlockElementFace.Deserializer())
    .registerTypeAdapter(BlockFaceUV.class, new BlockFaceUV.Deserializer())
    .registerTypeAdapter(SimpleBlockModel.class, (JsonDeserializer<SimpleBlockModel>)
      (json, type, context) -> SimpleBlockModel.deserialize(json.getAsJsonObject(), context))
    .registerTypeAdapter(ColoredBlockModel.class, (JsonDeserializer<ColoredBlockModel>)
      (json, type, context) -> ColoredBlockModel.deserialize(json.getAsJsonObject(), context))
    .registerTypeAdapter(ConnectedModel.class, (JsonDeserializer<ConnectedModel>)
      (json, type, context) -> ConnectedModel.deserialize(json.getAsJsonObject(), context))
    .registerTypeAdapter(MantleItemLayerModel.class, (JsonDeserializer<MantleItemLayerModel>)
      (json, type, context) -> MantleItemLayerModel.deserialize(json.getAsJsonObject(), context))
    .registerTypeAdapter(NBTKeyModel.class, (JsonDeserializer<NBTKeyModel>)
      (json, type, context) -> NBTKeyModel.deserialize(json.getAsJsonObject(), context))
    .registerTypeAdapter(ContextCapture.class, (JsonDeserializer<ContextCapture>)
      (json, type, context) -> new ContextCapture(context))
    .create();

  /** Marker type whose only job is to hand a live deserialization context back out of {@link #GSON} */
  private record ContextCapture(JsonDeserializationContext context) {}

  /**
   * A live deserialization context from {@link #GSON}, for the loader helpers that take one directly rather than
   * being reachable as a Gson type of their own.
   */
  private static final JsonDeserializationContext GSON_CONTEXT = GSON.fromJson("{}", ContextCapture.class).context();

  /** Parses a model JSON string through the loader registered for the given type */
  private static <T> T parse(Class<T> type, String json) {
    return GSON.fromJson(json, type);
  }

  /** Parses a JSON string into an object, for the loaders taking a bare {@link JsonObject} */
  private static JsonObject object(String json) {
    return JsonParser.parseString(json).getAsJsonObject();
  }

  /** A one element, one face model body, reused by most fixtures below */
  private static final String ONE_ELEMENT = """
    "elements": [{
      "from": [0, 0, 0], "to": [16, 16, 16],
      "faces": { "north": { "texture": "#side", "cullface": "north" } }
    }]""";


  /* SimpleBlockModel */

  @Test
  void simpleBlockModel_readsParentTexturesAndElements() {
    SimpleBlockModel model = parse(SimpleBlockModel.class, """
      {
        "parent": "minecraft:block/cube_all",
        "textures": { "side": "mantle:block/side", "end": "#side" },
        %s
      }""".formatted(ONE_ELEMENT));
    assertThat(model.getParentLocation()).isEqualTo(ResourceLocation.fromNamespaceAndPath("minecraft", "block/cube_all"));

    // a texture is either a material or a reference to another texture name, and the two spellings must not be mixed up
    Map<String,Either<Material,String>> textures = model.getTextures();
    assertThat(textures).containsOnlyKeys("side", "end");
    assertThat(textures.get("side").left()).contains(new Material(InventoryMenu.BLOCK_ATLAS, ResourceLocation.fromNamespaceAndPath("mantle", "block/side")));
    assertThat(textures.get("end").right()).contains("side");

    List<BlockElement> elements = model.getElements();
    assertThat(elements).hasSize(1);
    BlockElementFace face = elements.get(0).faces.get(Direction.NORTH);
    // BlockElementFace is a record in 1.21, so these are accessor calls rather than field reads
    assertThat(face.texture()).isEqualTo("#side");
    assertThat(face.cullForDirection()).isEqualTo(Direction.NORTH);
  }

  @Test
  void getModelElements_acceptsASingleElementWithoutAnArray() {
    // the bare helper takes either shape; note deserialize itself does not, it demands an array before calling in.
    // The object form exists for callers reading an element list out of some other key.
    List<BlockElement> elements = SimpleBlockModel.getModelElements(GSON_CONTEXT, JsonParser.parseString("""
      {
        "from": [0, 0, 0], "to": [16, 16, 16],
        "faces": { "up": { "texture": "#top" } }
      }"""), "elements");
    assertThat(elements).hasSize(1);
    assertThat(elements.get(0).faces.get(Direction.UP).cullForDirection()).isNull();
  }

  @Test
  void simpleBlockModel_defaultsEverythingAway() {
    SimpleBlockModel model = parse(SimpleBlockModel.class, "{}");
    assertThat(model.getParentLocation()).isNull();
    assertThat(model.getTextures()).isEmpty();
    assertThat(model.getElements()).isEmpty();
  }

  @Test
  void simpleBlockModel_rejectsNonArrayNonObjectElements() {
    assertThatThrownBy(() -> parse(SimpleBlockModel.class, "{\"elements\": \"nope\"}"))
      .isInstanceOf(JsonSyntaxException.class)
      .hasMessageContaining("elements");
  }


  /* ColoredBlockModel */

  @Test
  void coloredBlockModel_readsColorsAlongsideTheBaseModel() {
    ColoredBlockModel model = parse(ColoredBlockModel.class, """
      {
        "textures": { "side": "mantle:block/side" },
        "colors": [{ "color": "40FF8000", "luminosity": 12, "uvlock": true }, {}],
        %s
      }""".formatted(ONE_ELEMENT));
    // the base model still parses, colors are additive
    assertThat(model.getElements()).hasSize(1);
    assertThat(model.getColorData()).containsExactly(
      new ColorData(0x40FF8000, 12, true),
      ColorData.DEFAULT);
  }

  @Test
  void coloredBlockModel_defaultsColorsToEmpty() {
    assertThat(parse(ColoredBlockModel.class, "{}").getColorData()).isEmpty();
  }

  @Test
  void colorData_uvLockFallsBackToTheBlockState() {
    // null means "whatever the block state says", which is the only way to spell "inherit"
    assertThat(ColorData.DEFAULT.isUvLock(true)).isTrue();
    assertThat(ColorData.DEFAULT.isUvLock(false)).isFalse();
    assertThat(new ColorData(-1, -1, false).isUvLock(true)).isFalse();
    assertThat(new ColorData(-1, -1, true).isUvLock(false)).isTrue();
  }


  /* MantleItemLayerModel */

  @Test
  void itemLayerModel_readsLayerData() {
    MantleItemLayerModel model = parse(MantleItemLayerModel.class, """
      {
        "layers": [
          { "color": "FF0000", "luminosity": 15, "no_tint": true, "render_type": "minecraft:cutout" },
          {}
        ]
      }""");
    assertThat(model).isNotNull();
    // LayerData is what carries the JSON, and it is a record, so it compares directly
    assertThat(MantleItemLayerModel.LayerData.LIST_LOADABLE.getOrDefault(object("""
      { "layers": [{ "color": "FF0000", "luminosity": 15, "no_tint": true, "render_type": "minecraft:cutout" }, {}] }"""), "layers", List.of()))
      .containsExactly(
        new MantleItemLayerModel.LayerData(0xFFFF0000, 15, true, ResourceLocation.withDefaultNamespace("cutout")),
        MantleItemLayerModel.LayerData.DEFAULT);
  }

  @Test
  void itemLayerModel_defaultsLayersToEmpty() {
    assertThat(parse(MantleItemLayerModel.class, "{}")).isNotNull();
  }


  /* NBTKeyModel */

  @Test
  void nbtKeyModel_requiresAKey() {
    assertThatThrownBy(() -> parse(NBTKeyModel.class, "{}"))
      .isInstanceOf(JsonSyntaxException.class)
      .hasMessageContaining("nbt_key");
  }

  @Test
  void nbtKeyModel_readsKeyAndExtraTextures() {
    assertThat(parse(NBTKeyModel.class, """
      { "nbt_key": "some_key", "extra_textures_key": "mantle:extra" }""")).isNotNull();
  }


  /* RetexturedModel */

  @Test
  void retexturedModel_readsBothSpellingsOfTheNameList() {
    assertThat(RetexturedModel.getRetexturedNames(object("{\"retextured\": \"texture\"}"))).containsExactly("texture");
    assertThat(RetexturedModel.getRetexturedNames(object("{\"retextured\": [\"a\", \"b\"]}"))).containsExactlyInAnyOrder("a", "b");
  }

  @Test
  void retexturedModel_rejectsAMissingOrEmptyNameList() {
    assertThatThrownBy(() -> RetexturedModel.getRetexturedNames(object("{}")))
      .isInstanceOf(JsonSyntaxException.class);
    assertThatThrownBy(() -> RetexturedModel.getRetexturedNames(object("{\"retextured\": []}")))
      .isInstanceOf(JsonSyntaxException.class)
      .hasMessageContaining("at least one");
  }


  /* ConnectedModel */

  private static final String CONNECTION = """
    "connection": { "textures": { "side": "cornerless_full" } }""";

  @Test
  void connectedModel_readsTexturesSidesAndPredicate() {
    assertThat(parse(ConnectedModel.class, """
      {
        "textures": { "side": "mantle:block/side" },
        "connection": {
          "textures": { "side": "cornerless_full" },
          "sides": ["north", "south"],
          "predicate": "pane"
        },
        %s
      }""".formatted(ONE_ELEMENT))).isNotNull();
  }

  @Test
  void connectedModel_defaultsSidesAndPredicate() {
    assertThat(parse(ConnectedModel.class, "{%s}".formatted(CONNECTION))).isNotNull();
  }

  @Test
  void connectedModel_rejectsAnEmptyTextureList() {
    assertThatThrownBy(() -> parse(ConnectedModel.class, "{\"connection\": {\"textures\": {}}}"))
      .isInstanceOf(JsonSyntaxException.class)
      .hasMessageContaining("at least one texture");
  }

  @Test
  void connectedModel_rejectsUnknownTypesPredicatesAndSides() {
    assertThatThrownBy(() -> parse(ConnectedModel.class, "{\"connection\": {\"textures\": {\"side\": \"nope\"}}}"))
      .isInstanceOf(JsonSyntaxException.class)
      .hasMessageContaining("Unknown connection type");
    assertThatThrownBy(() -> parse(ConnectedModel.class, "{\"connection\": {\"textures\": {\"side\": \"cornerless_full\"}, \"predicate\": \"nope\"}}"))
      .isInstanceOf(JsonSyntaxException.class)
      .hasMessageContaining("Unknown connection predicate");
    assertThatThrownBy(() -> parse(ConnectedModel.class, "{\"connection\": {\"textures\": {\"side\": \"cornerless_full\"}, \"sides\": [\"sideways\"]}}"))
      .hasMessageContaining("Invalid side");
  }


  /* ConnectedModelRegistry, the table both the model and its builder read */

  /** Index into a connection type's suffix table, NSWE as the model itself computes it */
  private static int key(Direction... connected) {
    int index = 0;
    for (Direction direction : connected) {
      index |= 1 << direction.get2DDataValue();
    }
    return index;
  }

  @Test
  void connectionType_cornerlessFullNamesEachCombination() {
    String[] suffixes = ConnectedModelRegistry.deserializeType(JsonParser.parseString("\"cornerless_full\""), "type");
    assertThat(suffixes).hasSize(16);
    assertThat(suffixes[key()]).isEmpty();
    // NSWE maps to udlr in name order, and the name is built in that order regardless of the order the sides are set
    assertThat(suffixes[key(Direction.NORTH)]).isEqualTo("u");
    assertThat(suffixes[key(Direction.EAST, Direction.WEST)]).isEqualTo("lr");
    assertThat(suffixes[key(Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST)]).isEqualTo("udlr");
  }

  @Test
  void connectionType_horizontalAndVerticalNameTheirThreeStates() {
    String[] horizontal = ConnectedModelRegistry.deserializeType(JsonParser.parseString("\"horizontal\""), "type");
    assertThat(horizontal[key()]).isEmpty();
    assertThat(horizontal[key(Direction.WEST)]).isEqualTo("right");
    assertThat(horizontal[key(Direction.EAST)]).isEqualTo("left");
    assertThat(horizontal[key(Direction.WEST, Direction.EAST)]).isEqualTo("middle");

    String[] vertical = ConnectedModelRegistry.deserializeType(JsonParser.parseString("\"vertical\""), "type");
    assertThat(vertical[key()]).isEmpty();
    assertThat(vertical[key(Direction.NORTH)]).isEqualTo("bottom");
    assertThat(vertical[key(Direction.SOUTH)]).isEqualTo("top");
    assertThat(vertical[key(Direction.NORTH, Direction.SOUTH)]).isEqualTo("middle");
  }

  @Test
  void connectionPredicate_blockComparesBlocksOnly() {
    BiPredicate<BlockState,BlockState> predicate = ConnectedModelRegistry.getPredicate("block");
    BlockState stone = Blocks.STONE.defaultBlockState();
    assertThat(predicate.test(stone, Blocks.STONE.defaultBlockState())).isTrue();
    // a different state of the same block still connects, a different block does not
    assertThat(predicate.test(Blocks.OAK_SLAB.defaultBlockState(), Blocks.OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP))).isTrue();
    assertThat(predicate.test(stone, Blocks.DIRT.defaultBlockState())).isFalse();
  }

  @Test
  void connectionPredicate_paneSeparatesCenterOnlyPanesFromConnectedOnes() {
    BiPredicate<BlockState,BlockState> predicate = ConnectedModelRegistry.getPredicate("pane");
    BlockState centerOnly = Blocks.GLASS_PANE.defaultBlockState();
    BlockState connected = centerOnly.setValue(PipeBlock.NORTH, true);
    assertThat(predicate.test(centerOnly, centerOnly)).isTrue();
    assertThat(predicate.test(connected, connected)).isTrue();
    // the whole point of the pane predicate: a lone post does not connect to a pane that already has arms
    assertThat(predicate.test(centerOnly, connected)).isFalse();
    assertThat(predicate.test(connected, Blocks.WHITE_STAINED_GLASS_PANE.defaultBlockState().setValue(PipeBlock.NORTH, true))).isFalse();
  }

  @Test
  void connectionPredicate_unknownNameFallsBackToBlockButFailsToDeserialize() {
    // getPredicate is the lenient lookup used at runtime, deserializePredicate is the strict one used while parsing
    assertThat(ConnectedModelRegistry.getPredicate("nope")).isNotNull();
    assertThatThrownBy(() -> ConnectedModelRegistry.deserializePredicate(object("{\"predicate\": \"nope\"}"), "predicate"))
      .isInstanceOf(JsonSyntaxException.class);
    assertThatThrownBy(() -> ConnectedModelRegistry.deserializeType(JsonParser.parseString("\"nope\""), "type"))
      .isInstanceOf(JsonSyntaxException.class);
  }
}
