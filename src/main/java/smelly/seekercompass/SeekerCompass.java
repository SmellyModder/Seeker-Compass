package smelly.seekercompass;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.Rarity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import smelly.seekercompass.enchants.SCEnchants;

@Mod(value = SeekerCompass.MOD_ID)
public class SeekerCompass {
	public static final String MOD_ID = "seeker_compass";
	public static final Logger LOGGER = LogManager.getLogger(MOD_ID.toUpperCase());
	public static final String NETWORK_PROTOCOL = "SC1";
	public static SeekerCompass instance;
	
	public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder.named(new ResourceLocation(MOD_ID, "net"))
		.networkProtocolVersion(() -> NETWORK_PROTOCOL)
		.clientAcceptedVersions(NETWORK_PROTOCOL::equals)
		.serverAcceptedVersions(NETWORK_PROTOCOL::equals)
		.simpleChannel();
	
	public static final DeferredRegister<Item> ITEMS = new DeferredRegister<>(ForgeRegistries.ITEMS, MOD_ID);
	public static final RegistryObject<Item> SEEKER_COMPASS = ITEMS.register("seeker_compass", () -> new SeekerCompassItem((new Item.Properties()).maxStackSize(1).rarity(Rarity.UNCOMMON).group(ItemGroup.TOOLS)));
	
	public SeekerCompass() {
		instance = this;
		
		CHANNEL.messageBuilder(MessageS2CParticle.class, 0)
		.encoder(MessageS2CParticle::serialize).decoder(MessageS2CParticle::deserialize)
		.consumer(MessageS2CParticle::handle)
		.add();
		
		final IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
		ITEMS.register(modEventBus);
		SCEnchants.ENCHANTMENTS.register(modEventBus);
		
		modEventBus.addListener(EventPriority.LOWEST, this::setupCommon);
	}
	
	void setupCommon(final FMLCommonSetupEvent event) {}
}