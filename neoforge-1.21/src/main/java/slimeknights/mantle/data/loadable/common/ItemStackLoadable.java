package slimeknights.mantle.data.loadable.common;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import io.netty.handler.codec.EncoderException;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import slimeknights.mantle.data.loadable.ErrorFactory;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.OpsHelper;
import slimeknights.mantle.data.loadable.field.LoadableField;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.util.typed.TypedMap;

import javax.annotation.Nullable;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Loadable for an item stack.
 * @apiNote  The variants named {@code NBT} carry a {@link DataComponentPatch} in 1.21 rather than a compound tag, under
 *           a field named {@code components} rather than {@code nbt}. An item's extra data is components now, so the
 *           payload is a different shape either way; naming the field as vanilla's own item stack format does is the
 *           only reading of it which is not actively misleading. See {@link DataComponentsLoadable}.
 */
@SuppressWarnings("unused")  // API
public class ItemStackLoadable {
  private ItemStackLoadable() {}

  /* reused lambdas */
  /** Getter for an item from a stack */
  private static final Function<ItemStack,Item> ITEM_GETTER = ItemStack::getItem;
  /** Maps an item stack that may be empty to a strictly not empty one */
  private static final BiFunction<ItemStack,ErrorFactory,ItemStack> NOT_EMPTY = (stack, error) -> {
    if (stack.isEmpty()) {
      throw error.create("ItemStack cannot be empty");
    }
    return stack;
  };

  /* fields */
  /** Field for an optional item */
  private static final LoadableField<Item,ItemStack> ITEM = Loadables.ITEM.defaultField("item", Items.AIR, false, ITEM_GETTER);
  /** Field for item stack count that allows empty */
  private static final LoadableField<Integer,ItemStack> COUNT = IntLoadable.FROM_ZERO.defaultField("count", 1, true, ItemStack::getCount);
  /** Field for the components which differ from the item's defaults */
  private static final LoadableField<DataComponentPatch,ItemStack> COMPONENTS = DataComponentsLoadable.INSTANCE.defaultField("components", DataComponentPatch.EMPTY, false, ItemStack::getComponentsPatch);


  /* Optional */
  /** Single item which may be empty with a count of 1 */
  public static final Loadable<ItemStack> OPTIONAL_ITEM = Loadables.ITEM.flatXmap(item -> makeStack(item, 1, null), ITEM_GETTER);
  /** Loadable for a stack that may be empty with variable count */
  public static final RecordLoadable<ItemStack> OPTIONAL_STACK = RecordLoadable.create(ITEM, COUNT, (item, count) -> makeStack(item, count, null))
                                                                               .compact(OPTIONAL_ITEM, stack -> stack.getCount() == 1);
  /** Loadable for a stack that may be empty with NBT and a count of 1 */
  public static final RecordLoadable<ItemStack> OPTIONAL_ITEM_NBT = NBTStack.FIXED_COUNT;
  /** Loadable for a stack that may be empty with variable count and NBT */
  public static final RecordLoadable<ItemStack> OPTIONAL_STACK_NBT = NBTStack.READ_COUNT;

  /* Required */
  /** Single item which may not be empty with a count of 1 */
  public static final Loadable<ItemStack> REQUIRED_ITEM = notEmpty(OPTIONAL_ITEM);
  /** Loadable for a stack that may not be empty with variable count */
  public static final RecordLoadable<ItemStack> REQUIRED_STACK = notEmpty(OPTIONAL_STACK);
  /** Loadable for a stack that may not be empty with NBT and a count of 1 */
  public static final RecordLoadable<ItemStack> REQUIRED_ITEM_NBT = notEmpty(OPTIONAL_ITEM_NBT);
  /** Loadable for a stack that may not be empty with variable count and NBT */
  public static final RecordLoadable<ItemStack> REQUIRED_STACK_NBT = notEmpty(OPTIONAL_STACK_NBT);


  /* Helpers */

  /** Makes an item stack from the given parameters */
  private static ItemStack makeStack(Item item, int count, @Nullable DataComponentPatch components) {
    if (item == Items.AIR || count == 0) {
      return ItemStack.EMPTY;
    }
    ItemStack stack = new ItemStack(item, count);
    if (components != null && !components.isEmpty()) {
      stack.applyComponents(components);
    }
    return stack;
  }

  /** Creates a non-empty variant of the loadable */
  public static Loadable<ItemStack> notEmpty(Loadable<ItemStack> loadable) {
    return loadable.validate(NOT_EMPTY);
  }

  /** Creates a non-empty variant of the loadable */
  public static RecordLoadable<ItemStack> notEmpty(RecordLoadable<ItemStack> loadable) {
    return loadable.validate(NOT_EMPTY);
  }

  /** Loadable for an item stack with components, requires special logic for the compact form */
  private enum NBTStack implements RecordLoadable<ItemStack> {
    /** Reads count from JSON */
    READ_COUNT,
    /** Count is always 1 */
    FIXED_COUNT;


    /* General JSON */

    @Override
    public ItemStack deserialize(JsonObject json, TypedMap context) {
      int count = 1;
      if (this == READ_COUNT) {
        count = COUNT.get(json, context);
      }
      return makeStack(ITEM.get(json, context), count, COMPONENTS.get(json, context));
    }

    @Override
    public <O> ItemStack deserialize(DynamicOps<O> ops, MapLike<O> map, TypedMap context) {
      int count = 1;
      if (this == READ_COUNT) {
        count = COUNT.get(ops, map, context);
      }
      return makeStack(ITEM.get(ops, map, context), count, COMPONENTS.get(ops, map, context));
    }

    @Override
    public void serialize(ItemStack stack, JsonObject json) {
      ITEM.serialize(stack, json);
      if (this == READ_COUNT) {
        COUNT.serialize(stack, json);
      }
      COMPONENTS.serialize(stack, json);
    }

    @Override
    public <O> RecordBuilder<O> serialize(DynamicOps<O> ops, ItemStack stack, RecordBuilder<O> builder) {
      builder = ITEM.serialize(ops, stack, builder);
      if (this == READ_COUNT) {
        builder = COUNT.serialize(ops, stack, builder);
      }
      return COMPONENTS.serialize(ops, stack, builder);
    }


    /* Compact form */

    @Override
    public ItemStack convert(JsonElement element, String key, TypedMap context) {
      return convert(JsonOps.INSTANCE, element, key, context);
    }

    @Override
    public <O> ItemStack convert(DynamicOps<O> ops, O input, String key, TypedMap context) {
      if (!OpsHelper.isMap(ops, input) && !OpsHelper.isList(ops, input)) {
        return OPTIONAL_ITEM.convert(ops, input, key, context);
      }
      return RecordLoadable.super.convert(ops, input, key, context);
    }

    @Override
    public JsonElement serialize(ItemStack stack) {
      return serialize(JsonOps.INSTANCE, stack);
    }

    @Override
    public <O> O serialize(DynamicOps<O> ops, ItemStack stack) {
      if ((this == FIXED_COUNT || stack.getCount() == 1) && stack.isComponentsPatchEmpty()) {
        return OPTIONAL_ITEM.serialize(ops, stack);
      }
      return RecordLoadable.super.serialize(ops, stack);
    }


    /* Buffer */

    @Override
    public ItemStack decode(RegistryFriendlyByteBuf buffer, TypedMap context) {
      Item item = ITEM.decode(buffer, context);
      int count = 1;
      if (this == READ_COUNT) {
        count = COUNT.decode(buffer, context);
      }
      // the patch has to be read whether or not the stack is empty, as it is on the buffer either way
      DataComponentPatch components = COMPONENTS.decode(buffer, context);
      return makeStack(item, count, components);
    }

    @Override
    public void encode(RegistryFriendlyByteBuf buffer, ItemStack stack) throws EncoderException {
      ITEM.encode(buffer, stack);
      if (this == READ_COUNT) {
        COUNT.encode(buffer, stack);
      }
      COMPONENTS.encode(buffer, stack);
    }
  }
}
