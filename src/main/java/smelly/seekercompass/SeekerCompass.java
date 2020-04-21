package smelly.seekercompass;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.Rarity;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

@Mod(value = SeekerCompass.MOD_ID)
public class SeekerCompass {
	public static final String MOD_ID = "seeker_compass";
	public static final Logger LOGGER = LogManager.getLogger(MOD_ID.toUpperCase());
	public static SeekerCompass instance;
	
	public static final DeferredRegister<Item> ITEMS = new DeferredRegister<>(ForgeRegistries.ITEMS, MOD_ID);
	public static final RegistryObject<Item> SEEKER_COMPASS = ITEMS.register("seeker_compass", () -> new SeekerCompassItem((new Item.Properties()).maxStackSize(1).rarity(Rarity.UNCOMMON).group(ItemGroup.TOOLS)));
	
	public SeekerCompass() {
		instance = this;
		
		final IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
		ITEMS.register(modEventBus);
		
		modEventBus.addListener(EventPriority.LOWEST, this::setupCommon);
	}
	
	void setupCommon(final FMLCommonSetupEvent event) {}
}