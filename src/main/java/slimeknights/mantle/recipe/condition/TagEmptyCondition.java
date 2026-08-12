package slimeknights.mantle.recipe.condition;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import net.neoforged.neoforge.common.conditions.ICondition;

/** Condition that checks when a tag is empty. Same as {@link net.neoforged.neoforge.common.conditions.TagEmptyCondition} but for any registry */
public class TagEmptyCondition<T> extends TagCondition<T> implements LootItemCondition {
  /** Codec used both as the {@link ICondition} codec and as the body of {@link #LOOT_TYPE} */
  public static final MapCodec<TagEmptyCondition<?>> CODEC = makeCodec(TagEmptyCondition::new);
  /** Loot condition type, registered by {@link MantleConditions} */
  public static final LootItemConditionType LOOT_TYPE = new LootItemConditionType(CODEC);

  public TagEmptyCondition(TagKey<T> tag) {
    super(tag);
  }

  public TagEmptyCondition(ResourceKey<? extends Registry<T>> registry, ResourceLocation name) {
    this(TagKey.create(registry, name));
  }

  @Override
  public MapCodec<? extends ICondition> codec() {
    return CODEC;
  }

  @Override
  public LootItemConditionType getType() {
    return LOOT_TYPE;
  }

  @Override
  public boolean test(IContext context) {
    return context.getTag(tag).isEmpty();
  }

  @Override
  public boolean test(LootContext context) {
    Registry<T> registry = registry(context);
    return registry != null && !registry.getTagOrEmpty(tag).iterator().hasNext();
  }
}
