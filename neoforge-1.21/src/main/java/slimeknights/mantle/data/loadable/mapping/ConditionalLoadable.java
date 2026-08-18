package slimeknights.mantle.data.loadable.mapping;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.neoforge.common.conditions.ConditionalOps;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.common.conditions.ICondition.IContext;
import slimeknights.mantle.data.loadable.ErrorFactory;
import slimeknights.mantle.data.loadable.field.ContextKey;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.registry.GenericLoaderRegistry;
import slimeknights.mantle.data.registry.GenericLoaderRegistry.IHaveLoader;
import slimeknights.mantle.util.typed.TypedMap;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Loadable allowing a load time condition check to change which object is used.
 * Automatically used in {@link GenericLoaderRegistry} for all types, though you will have to manually provide datagen using {@link ConditionalObject}.
 * @param registry  Loader registry to fetch nested objects.
 * @param defaultIfFalse  Value to use if the condition fails and no false object is provided, typically should be an empty object. If null, a false object must be specified.
 * @apiNote  NeoForge's conditions became codec based in 1.21, so the conditions are read with
 *           {@link ICondition#LIST_CODEC} rather than through {@code CraftingHelper}. The {@link IContext} still comes
 *           from {@link ContextKey#CONDITION_CONTEXT}, defaulting to {@link IContext#TAGS_INVALID} as in 1.20; NeoForge
 *           itself now threads the context through the ops as a {@link ConditionalOps}, which this loadable cannot read
 *           since its own entry points are the gson ones, so a caller decoding through a {@link ConditionalOps} still
 *           has to put the context in the loadable context as well.
 *           <p>
 *           The conditions stay under a plain {@code conditions} key rather than NeoForge's
 *           {@link ConditionalOps#DEFAULT_CONDITIONS_KEY}: this is Mantle's own nested if_true/if_false format, not the
 *           file level condition wrapper NeoForge reads, so the two never meet.
 */
public record ConditionalLoadable<T extends IHaveLoader>(GenericLoaderRegistry<T> registry, @Nullable T defaultIfFalse) implements RecordLoadable<T> {
  /** Key holding the conditions to test */
  private static final String CONDITIONS = "conditions";

  /** Reads the condition context, preferring the loadable context and falling back to the ops */
  private static IContext conditionContext(TypedMap context) {
    IContext conditionContext = context.get(ContextKey.CONDITION_CONTEXT);
    return conditionContext != null ? conditionContext : IContext.TAGS_INVALID;
  }

  /** Tests every condition in the given field, treating a missing field as passing */
  private static boolean processConditions(JsonObject json, IContext conditionContext) {
    if (!json.has(CONDITIONS)) {
      return true;
    }
    List<ICondition> conditions = ICondition.LIST_CODEC.parse(JsonOps.INSTANCE, json.get(CONDITIONS)).getOrThrow(ErrorFactory.JSON_SYNTAX_ERROR::create);
    for (ICondition condition : conditions) {
      if (!condition.test(conditionContext)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public T deserialize(JsonObject json, TypedMap context) {
    // allow passing in the condition context via the loadable context
    // if missing, assume tags are invalid
    IContext conditionContext = conditionContext(context);
    // if the condition matches, use the true value
    if (processConditions(json, conditionContext)) {
      return registry.getIfPresent(json, "if_true");
    }
    // loader can define a default instance for false if they have one. Otherwise false is required.
    if (defaultIfFalse != null) {
      return registry.getOrDefault(json, "if_false", defaultIfFalse, context);
    }
    return registry.getIfPresent(json, "if_false", context);
  }

  @SuppressWarnings("unchecked") // loader is invalid if not
  @Override
  public void serialize(T object, JsonObject json) {
    ConditionalObject<T> conditional = (ConditionalObject<T>) object;
    json.add(CONDITIONS, ICondition.LIST_CODEC.encodeStart(JsonOps.INSTANCE, List.of(conditional.conditions())).getOrThrow(ErrorFactory.RUNTIME::create));
    json.add("if_true", registry.serialize(conditional.ifTrue()));
    T ifFalse = conditional.ifFalse();
    if (ifFalse != defaultIfFalse) {
      json.add("if_false", registry.serialize(ifFalse));
    }
  }

  @Override
  public T decode(RegistryFriendlyByteBuf buffer, TypedMap context) {
    throw new UnsupportedOperationException("Conditional loadable should always resolve to a specific instance. This should never happen.");
  }

  @Override
  public void encode(RegistryFriendlyByteBuf buffer, T value) {
    throw new UnsupportedOperationException("Conditional loadable should always resolve to a specific instance. This should never happen.");
  }

  /** Interface for the serializable version of {@link ConditionalLoadable} */
  public interface ConditionalObject<T> extends IHaveLoader {
    /** Conditions on the object */
    ICondition[] conditions();

    /** Object to use when conditions are true */
    T ifTrue();

    /** Object to use when conditions are false */
    T ifFalse();
  }
}
