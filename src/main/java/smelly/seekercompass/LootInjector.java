package smelly.seekercompass;

import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTables;
import net.minecraft.loot.TableLootEntry;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * @author SmellyModder(Luke Tonon)
 */
@Mod.EventBusSubscriber(modid = SeekerCompass.MOD_ID)
public class LootInjector {
	@SubscribeEvent
	public static void onInjectLoot(LootTableLoadEvent event) {
		if (event.getName().equals(LootTables.CHESTS_NETHER_BRIDGE)) {
			event.getTable().addPool(buildLootBool("nether_fortress", 1, 0));
		}
	}
	
	private static LootPool buildLootBool(String name, int weight, int quality) {
		return LootPool.builder().addEntry(TableLootEntry.builder(new ResourceLocation(SeekerCompass.MOD_ID, "injections/" + name)).weight(1).quality(0)).name(name).build();
	}
}