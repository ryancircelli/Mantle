/**
 * Typed access to the data an item stack stores in its tag.
 *
 * <h2>Why not just use the tag</h2>
 * A stack's tag is one flat compound shared by vanilla, every mod and every command that ever touched the stack, and
 * {@link net.minecraft.world.item.ItemStack#getOrCreateTag()} hands all of it to anyone who asks. Nothing in that
 * arrangement records who owns an entry, so the ordinary mistakes are silent ones: two mods pick the same obvious name
 * and overwrite each other, a helper written for one item runs on another and clears an entry it does not own, or a
 * value is written with {@code putInt} in one place and read with {@code getString} in another and simply reads as the
 * empty string forever.
 * <p>
 * A {@link slimeknights.mantle.item.data.DataKey} replaces the reach-in with a name that has an owner and a type. The
 * name is namespaced, so it cannot collide with another mod, and it may be claimed only once, so it cannot collide with
 * another part of your own mod either. The type comes from a {@link slimeknights.mantle.data.loadable.Loadable}, which
 * already knows how to read and write the value, and so is the whole of the format the entry uses.
 *
 * <h2>Using it</h2>
 * Declare the key once, in the class which owns the data:
 * <pre>{@code
 * public static final DataKey<Block> TEXTURE = DataKey.of(Mantle.getResource("texture"), Loadables.BLOCK);
 * }</pre>
 * Read through a {@link slimeknights.mantle.item.data.DataView}, which parses only the entry asked for and gives null
 * rather than throwing on a stack whose data is missing or nonsense:
 * <pre>{@code
 * Block texture = DataView.of(stack).getOrDefault(TEXTURE, Blocks.AIR);
 * }</pre>
 * Write through a {@link slimeknights.mantle.item.data.DataEditor}, which collects the change and puts it on the stack
 * in one step, touching no entry other than the ones it was given:
 * <pre>{@code
 * DataEditor.edit().set(TEXTURE, block).apply(stack);
 * }</pre>
 *
 * <h2>When to still use the tag</h2>
 * Keys address one entry of a stack's tag each, so data that is not shaped that way is still the tag's business:
 * entries vanilla owns, which have their own accessors on {@link net.minecraft.world.item.ItemStack}; an open ended
 * map that other mods write into, where there is no type to fix; and anything not on an item stack at all. A key is
 * worth having wherever the answer to "what is in this entry" should be the same everywhere it is read.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
package slimeknights.mantle.item.data;

import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;
