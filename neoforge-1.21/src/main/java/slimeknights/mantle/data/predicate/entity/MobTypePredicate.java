package slimeknights.mantle.data.predicate.entity;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.record.RecordLoadable;

import java.util.Map;

/**
 * Predicate matching a group of mobs, such as {@link EntityTypeTags#UNDEAD}.
 * <p>
 * 1.20.5 deleted {@code MobType}, which this predicate matched on, and moved the five groups it named onto entity type
 * tags; upstream's own TODO on this class named that as the replacement. So the {@code mobs} field now holds an entity
 * type tag rather than the name of a Mantle registered mob type.
 * <p>
 * That is a datapack format change, but a narrow one. Mantle registered five mob types and three of them
 * ({@code undead}, {@code arthropod}, {@code illager}) are the names of the vanilla tags that replaced them, so those
 * files read unchanged and mean the same thing. The other two cannot: see {@link #REPLACED} for what they parse to now.
 * <p>
 * This predicate is now the same test as {@link LivingEntityPredicate#tag(TagKey)}, kept as its own type so existing
 * {@code mantle:mob_type} files keep loading.
 */
public record MobTypePredicate(TagKey<EntityType<?>> tag) implements LivingEntityPredicate {
  /**
   * The two mob types Mantle registered whose replacement is not a tag of the same name. Parsing one is an error rather
   * than a tag that silently matches nothing, since both would otherwise be valid resource locations naming no tag.
   */
  private static final Map<ResourceLocation,String> REPLACED = Map.of(
    ResourceLocation.withDefaultNamespace("water"), "use the entity type tag minecraft:aquatic",
    ResourceLocation.withDefaultNamespace("undefined"), "which meant an entity belonging to no mob type, and has no tag equivalent; invert a tag predicate over the group you meant to exclude");

  /** Tag loadable rejecting the two mob type names that would otherwise parse as an empty tag */
  private static final Loadable<TagKey<EntityType<?>>> TAG = Loadables.ENTITY_TYPE_TAG.validate((tag, error) -> {
    String replacement = REPLACED.get(tag.location());
    if (replacement != null) {
      throw error.create("'" + tag.location() + "' was a mob type before 1.21 and is not an entity type tag: " + replacement);
    }
    return tag;
  });

  /** Loader for a mob type predicate */
  public static final RecordLoadable<MobTypePredicate> LOADER = RecordLoadable.create(TAG.requiredField("mobs", MobTypePredicate::tag), MobTypePredicate::new);

  @Override
  public boolean matches(LivingEntity input) {
    return input.getType().is(tag);
  }

  @Override
  public RecordLoadable<? extends LivingEntityPredicate> getLoader() {
    return LOADER;
  }
}
