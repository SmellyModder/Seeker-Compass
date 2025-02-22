package net.smelly.seekercompass;

import com.teamabnormals.blueprint.common.loot.modification.LootModifierProvider;
import com.teamabnormals.blueprint.common.loot.modification.modifiers.LootPoolsModifier;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.BinomialDistributionGenerator;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class SCLootModifierProvider extends LootModifierProvider {

	public SCLootModifierProvider(PackOutput output, CompletableFuture<Provider> provider) {
		super(SeekerCompass.MOD_ID, output, provider);
	}

	@Override
	protected void registerEntries(Provider provider) {
		LootPool seekerCompassRare = seekerCompass("seeker_compass_rare", 0.25F);
		this.entry("chests/nether_bridge")
				.selects(BuiltInLootTables.NETHER_BRIDGE)
				.addModifier(new LootPoolsModifier(List.of(seekerCompassRare), false));

		LootPool seekerCompassCommon = seekerCompass("seeker_compass_common", 0.75F);
		this.entry("chests/bastion_treasure")
				.selects(BuiltInLootTables.BASTION_TREASURE)
				.addModifier(new LootPoolsModifier(List.of(seekerCompassCommon), false));
	}

	private static LootPool seekerCompass(String name, float chance) {
		return LootPool.lootPool()
				.name(SeekerCompass.MOD_ID + ":" + name)
				.add(LootItem.lootTableItem(SeekerCompass.SEEKER_COMPASS.get()).apply(SetItemCountFunction.setCount(BinomialDistributionGenerator.binomial(1, chance))))
				.build();
	}

}
