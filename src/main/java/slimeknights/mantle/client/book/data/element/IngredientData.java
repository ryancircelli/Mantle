package slimeknights.mantle.client.book.data.element;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import slimeknights.mantle.client.book.repository.BookRepository;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class IngredientData implements IDataElement {
  public SizedIngredient[] ingredients = new SizedIngredient[0];
  public String action;

  private transient String error;
  private transient NonNullList<ItemStack> items;
  private transient boolean customData;

  public NonNullList<ItemStack> getItems() {
    return this.items;
  }

  public static IngredientData getItemStackData(ItemStack stack) {
    IngredientData data = new IngredientData();
    data.items = NonNullList.withSize(1, stack);
    data.customData = true;

    return data;
  }

  public static IngredientData getItemStackData(NonNullList<ItemStack> items) {
    IngredientData data = new IngredientData();
    data.items = items;
    data.customData = true;

    return data;
  }

  @Override
  public void load(BookRepository source) {
    if (this.customData) {
      return;
    }

    ArrayList<ItemStack> stacks = new ArrayList<>();
    for(SizedIngredient ingredient : ingredients) {
      if(ingredient == null) {
        continue;
      }

      stacks.addAll(List.of(ingredient.getItems()));
    }

    if(ingredients == null || stacks.isEmpty() || !StringUtil.isNullOrEmpty(error)) {
      items = NonNullList.withSize(1, getMissingItem());
      return;
    }

    items = NonNullList.of(getMissingItem(), stacks.toArray(new ItemStack[0]));
  }

  private ItemStack getMissingItem() {
    return getMissingItem(this.error);
  }

  private ItemStack getMissingItem(String error) {
    ItemStack missingItem = new ItemStack(Items.BARRIER);

    missingItem.set(DataComponents.CUSTOM_NAME, Component.literal("Error Loading Item"));
    if (!StringUtil.isNullOrEmpty(error)) {
      missingItem.set(DataComponents.LORE, new ItemLore(List.of(
        Component.literal("Error:"),
        Component.literal(error)
      )));
    }

    return missingItem;
  }

  public static class Deserializer implements JsonDeserializer<IngredientData> {
    @Override
    public IngredientData deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
      IngredientData data = new IngredientData();

      if(json.isJsonArray()) {
        JsonArray array = json.getAsJsonArray();
        data.ingredients = new SizedIngredient[array.size()];

        for(int i = 0; i < array.size(); i++) {
          try {
            data.ingredients[i] = readIngredient(array.get(i));
          } catch (Exception e) {
            data.ingredients[i] = SizedIngredient.of(data.getMissingItem(e.getMessage()).getItem(), 1);
          }
        }

        return data;
      }

      try {
        data.ingredients = new SizedIngredient[]{ readIngredient(json) };
      } catch (Exception e) {
        data.error = e.getMessage();
        return data;
      }

      if(json.isJsonObject()) {
        JsonObject object = json.getAsJsonObject();
        if (object.has("action")) {
          JsonElement action = object.get("action");
          if (action.isJsonPrimitive()) {
            JsonPrimitive primitive = action.getAsJsonPrimitive();
            if (primitive.isString()) {
              data.action = primitive.getAsString();
            }
          }
        }
      }

      return data;
    }

    /**
     * Reads a single sized ingredient. A bare string is a shorthand for an item id at count 1, matching every
     * book JSON currently shipped. A JSON object falls back to {@link SizedIngredient#FLAT_CODEC} - the same
     * shape every other 1.21 Mantle recipe uses ({@code {"item": "...", "count": 3}}, count defaulting to 1) -
     * which is a format change from whatever Forge's now-deleted {@code SizedIngredient#deserialize} accepted.
     * No bundled book page exercises the object form, so this is unverified against real content, but is
     * consistent with {@code SizedIngredientLoadable.FLAT} used everywhere else in this codebase.
     */
    private SizedIngredient readIngredient(JsonElement json) {
      if(json.isJsonPrimitive()) {
        JsonPrimitive primitive = json.getAsJsonPrimitive();

        if(primitive.isString()) {
          Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(primitive.getAsString()));
          return SizedIngredient.of(item, 1);
        }
      }

      if(!json.isJsonObject()) {
        throw new JsonParseException("Must be an array, string or JSON object");
      }

      return SizedIngredient.FLAT_CODEC.parse(JsonOps.INSTANCE, json).getOrThrow(JsonParseException::new);
    }
  }
}
