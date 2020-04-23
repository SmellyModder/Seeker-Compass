package smelly.seekercompass;

import java.lang.reflect.Array;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.enchantment.EnchantmentType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.Rarity;
import net.minecraft.util.ResourceLocation;
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

/**
 * @author SmellyModder(Luke Tonon)
 */
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
		
		modEventBus.addListener(this::setupCommon);
	}
	
	void setupCommon(final FMLCommonSetupEvent event) {
		ItemGroup.TOOLS.setRelevantEnchantmentTypes(add(ItemGroup.TOOLS.getRelevantEnchantmentTypes(), SCEnchants.SEEKER_COMPASS));
	}
	
	public static EnchantmentType[] add(EnchantmentType[] array, EnchantmentType element) {
		EnchantmentType[] newArray = array;
		int arrayLength = Array.getLength(newArray);
		Object newArrayObject = Array.newInstance(newArray.getClass().getComponentType(), arrayLength + 1);
		System.arraycopy(array, 0, newArrayObject, 0, arrayLength);
		newArray[newArray.length - 1] = element;
		return newArray;
	}
}