package slimeknights.mantle.data.gson;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.mojang.serialization.JsonOps;
import net.neoforged.neoforge.common.conditions.ICondition;

import java.lang.reflect.Type;

/**
 * Serializer for a NeoForge condition.
 * @apiNote  Conditions are codec based in 1.21, where Forge dispatched them through {@code CraftingHelper}; the
 *           serializer registry behind {@link ICondition#CODEC} is a static NeoForge registry, so no registry access is
 *           needed to read one.
 */
public class ConditionSerializer implements JsonDeserializer<ICondition>, JsonSerializer<ICondition> {
  public static final ConditionSerializer INSTANCE = new ConditionSerializer();

  private ConditionSerializer() {}

  @Override
  public ICondition deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
    return ICondition.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow(JsonParseException::new);
  }

  @Override
  public JsonElement serialize(ICondition condition, Type type, JsonSerializationContext context) {
    return ICondition.CODEC.encodeStart(JsonOps.INSTANCE, condition).getOrThrow(JsonParseException::new);
  }
}
