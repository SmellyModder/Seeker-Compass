package smelly.seekercompass;

import java.util.Set;

import com.google.common.collect.Sets;

import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.loot.LootPool;
import net.minecraft.world.storage.loot.LootTable;
import net.minecraft.world.storage.loot.LootTables;
import net.minecraft.world.storage.loot.TableLootEntry;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * @author SmellyModder(Luke Tonon)
 */
@Mod.EventBusSubscriber(modid = SeekerCompass.MOD_ID)
public class LootInjector {
	private static final Set<ResourceLocation> NETHER_FORTRESS_INJECTION = Sets.newHashSet(LootTables.CHESTS_NETHER_BRIDGE);
	
	@SubscribeEvent
	public static void onInjectLoot(LootTableLoadEvent event) {
		ResourceLocation name = event.getName();
		LootTable table = event.getTable();
		if(NETHER_FORTRESS_INJECTION.contains(name)) {
			table.addPool(buildLootBool("nether_fortress", 1, 0));
		}
	}
	
	private static LootPool buildLootBool(String name, int weight, int quality) {
		return LootPool.builder().addEntry(TableLootEntry.builder(new ResourceLocation(SeekerCompass.MOD_ID, "injections/" + name)).weight(1).quality(0)).name(name).build();
	}
}