package net.smelly.seekercompass.modification.modifiers;

import com.minecraftabnormals.abnormals_core.common.loot.modification.LootModifiers;

public final class SCLootModifiers {
	public static final BiasedItemWeightModifier BIASED_ITEM_WEIGHT_MODIFIER = LootModifiers.REGISTRY.register("seeker_compass:biased_item_weight", new BiasedItemWeightModifier());

	public static void load() {}
}
