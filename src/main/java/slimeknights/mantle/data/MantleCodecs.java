package slimeknights.mantle.data;

import com.mojang.serialization.Codec;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntries;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctions;

import java.util.List;

/**
 * This class contains codecs for various vanilla things that we need to use in codecs.
 * @apiNote  Every constant here existed because Forge had moved the thing to gson serializers rather than codecs. 1.21
 *           gives all three a real codec, so these are now aliases for the vanilla ones and the class exists only so
 *           call sites do not have to change. It can go away whenever the campaign wants to touch them.
 */
public class MantleCodecs {
  /** Codec for loot pool entries */
  public static final Codec<LootPoolEntryContainer> LOOT_ENTRY = LootPoolEntries.CODEC;
  /** Codec for loot functions */
  public static final Codec<LootItemFunction[]> LOOT_FUNCTIONS = LootItemFunctions.ROOT_CODEC.listOf()
    .xmap(list -> list.toArray(LootItemFunction[]::new), List::of);
  /** Codec for ingredients, handling NeoForge ingredient types */
  public static final Codec<Ingredient> INGREDIENT = Ingredient.CODEC;
}
