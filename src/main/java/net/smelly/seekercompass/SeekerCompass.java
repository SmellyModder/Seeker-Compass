package net.smelly.seekercompass;

import com.teamabnormals.blueprint.core.util.item.CreativeModeTabContentsPopulator;
import net.minecraft.client.renderer.item.CompassItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.GlobalPos;
import net.minecraft.data.DataGenerator;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.IConfigSpec;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.smelly.seekercompass.enchants.SCEnchants;
import net.smelly.seekercompass.network.*;
import net.smelly.seekercompass.particles.SCParticles;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Optional;

/**
 * @author SmellyModder (Luke Tonon)
 */
@SuppressWarnings("deprecation")
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

	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
	public static final RegistryObject<Item> SEEKER_COMPASS = ITEMS.register("seeker_compass", () -> new SeekerCompassItem((new Item.Properties()).stacksTo(1).durability(1200).rarity(Rarity.UNCOMMON)));

	public SeekerCompass() {
		instance = this;

		CHANNEL.registerMessage(0, S2CUpdateStalkerMessage.class, S2CUpdateStalkerMessage::serialize, S2CUpdateStalkerMessage::deserialize, S2CUpdateStalkerMessage::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
		CHANNEL.registerMessage(1, C2SStopStalkingMessage.class, C2SStopStalkingMessage::serialize, C2SStopStalkingMessage::deserialize, C2SStopStalkingMessage::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
		CHANNEL.registerMessage(2, S2CUpdateStalkedMessage.class, S2CUpdateStalkedMessage::serialize, S2CUpdateStalkedMessage::deserialize, S2CUpdateStalkedMessage::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));

		final IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
		ITEMS.register(modEventBus);
		SCEnchants.ENCHANTMENTS.register(modEventBus);
		SCParticles.PARTICLES.register(modEventBus);

		modEventBus.addListener(this::setupCommon);
		modEventBus.addListener(this::onGatherData);

		modEventBus.addListener((ModConfigEvent event) -> {
			IConfigSpec<?> spec = event.getConfig().getSpec();
			if (spec == SCConfig.COMMON_SPEC) {
				SCConfig.COMMON.load();
			} else if (spec == SCConfig.CLIENT_SPEC) {
				SCConfig.CLIENT.load();
			}
		});

		ModLoadingContext context = ModLoadingContext.get();
		context.registerConfig(ModConfig.Type.CLIENT, SCConfig.CLIENT_SPEC);
		context.registerConfig(ModConfig.Type.COMMON, SCConfig.COMMON_SPEC);

		DistExecutor.runWhenOn(Dist.CLIENT, () -> () -> {
			modEventBus.addListener(EventPriority.LOWEST, this::setupClient);
		});
	}

	private void setupCommon(final FMLCommonSetupEvent event) {
	}

	private void onGatherData(GatherDataEvent event) {
		DataGenerator dataGenerator = event.getGenerator();
		dataGenerator.addProvider(event.includeServer(), new SCLootModifierProvider(dataGenerator.getPackOutput(), event.getLookupProvider()));
	}

	@OnlyIn(Dist.CLIENT)
	private void setupClient(final FMLClientSetupEvent event) {
		CreativeModeTabContentsPopulator.mod(MOD_ID)
				.tab(CreativeModeTabs.TOOLS_AND_UTILITIES)
				.addItemsAfter(Ingredient.of(Items.COMPASS), SEEKER_COMPASS);

		event.enqueueWork(() -> {
			ItemProperties.register(SEEKER_COMPASS.get(), new ResourceLocation("angle"), new CompassItemPropertyFunction((level, stack, entity) -> {
				if (!SeekerCompassItem.isNotBroken(stack)) return null;
				CompoundTag tag = stack.getOrCreateTag();
				if (!tag.contains(SeekerCompassItem.ENTITY_TAG, 10)) return null;
				var status = SeekerCompassItem.EntityStatusData.read(tag.getCompound(SeekerCompassItem.ENTITY_TAG));
				return GlobalPos.of(level.dimension(), status.pos());
			}));
			ItemProperties.register(SEEKER_COMPASS.get(), new ResourceLocation("broken"), (stack, level, entity, id) -> SeekerCompassItem.isNotBroken(stack) ? 0.0F : 1.0F);
		});
	}
}