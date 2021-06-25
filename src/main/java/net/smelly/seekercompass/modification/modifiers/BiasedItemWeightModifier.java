package net.smelly.seekercompass.modification.modifiers;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.minecraftabnormals.abnormals_core.common.loot.modification.modifiers.ILootModifier;
import com.mojang.datafixers.util.Pair;
import net.minecraft.item.Item;
import net.minecraft.loot.*;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Field;
import java.util.List;
import java.util.function.Supplier;

import static com.minecraftabnormals.abnormals_core.common.loot.modification.modifiers.LootPoolEntriesModifier.ENTRIES;
import static com.minecraftabnormals.abnormals_core.common.loot.modification.modifiers.LootPoolsModifier.POOLS;

public final class BiasedItemWeightModifier implements ILootModifier<BiasedItemWeightModifier.Config> {
	private static final Field LOOT_ENTRY_ITEM = ObfuscationReflectionHelper.findField(ItemLootEntry.class, "field_186368_a");
	private static final Field WEIGHT = ObfuscationReflectionHelper.findField(StandaloneLootEntry.class, "field_216158_e");

	@SuppressWarnings("unchecked")
	@Override
	public void modify(LootTableLoadEvent event, BiasedItemWeightModifier.Config config) {
		try {
			List<LootEntry> lootEntries = (List<LootEntry>) ENTRIES.get(((List<LootPool>) POOLS.get(event.getTable())).get(config.index));
			Item biasedItem = config.biasedItem.get();
			int bias = config.bias;
			lootEntries.forEach(lootEntry -> {
				if (lootEntry instanceof ItemLootEntry) {
					ItemLootEntry itemLootEntry = ((ItemLootEntry) lootEntry);
					try {
						Item item = (Item) LOOT_ENTRY_ITEM.get(itemLootEntry);
						if (item != biasedItem) {
							WEIGHT.set(itemLootEntry, (int) WEIGHT.get(itemLootEntry) + bias);
						}
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					}
				}
			});
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	@Override
	public JsonElement serialize(BiasedItemWeightModifier.Config config, Gson gson) throws JsonParseException {
		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("index", config.index);
		jsonObject.addProperty("bias", config.bias);
		jsonObject.addProperty("biased_item", String.valueOf(config.biasedItem.get().getRegistryName()));
		return jsonObject;
	}

	@Override
	public BiasedItemWeightModifier.Config deserialize(JsonElement jsonElement, Pair<Gson, LootPredicateManager> gsonLootPredicateManagerPair) throws JsonParseException {
		JsonObject jsonObject = jsonElement.getAsJsonObject();
		String itemName = jsonObject.get("biased_item").getAsString();
		Item biasedItem = ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemName));
		if (biasedItem == null) {
			throw new JsonParseException("Unknown item: " + itemName);
		}
		return new Config(jsonObject.get("index").getAsInt(), jsonObject.get("bias").getAsInt(), () -> biasedItem);
	}

	public static final class Config {
		private final int index;
		private final int bias;
		private final Supplier<Item> biasedItem;

		public Config(int index, int bias, Supplier<Item> biasedItem) {
			this.index = index;
			this.bias = bias;
			this.biasedItem = biasedItem;
		}
	}
}
