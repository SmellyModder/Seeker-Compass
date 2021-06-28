package net.smelly.seekercompass;

import com.google.gson.Gson;
import com.minecraftabnormals.abnormals_core.common.loot.modification.LootModifiers;
import com.minecraftabnormals.abnormals_core.common.loot.modification.modifiers.LootPoolEntriesModifier;
import com.minecraftabnormals.abnormals_core.core.util.modification.ConfiguredModifier;
import com.minecraftabnormals.abnormals_core.core.util.modification.ModifierDataProvider;
import com.minecraftabnormals.abnormals_core.core.util.modification.TargetedModifier;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.data.DataGenerator;
import net.minecraft.enchantment.EnchantmentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.*;
import net.minecraft.loot.ItemLootEntry;
import net.minecraft.loot.LootPredicateManager;
import net.minecraft.loot.RandomValueRange;
import net.minecraft.loot.functions.SetCount;
import net.minecraft.loot.functions.SetDamage;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DeferredWorkQueue;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.GatherDataEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.smelly.seekercompass.enchants.SCEnchants;
import net.smelly.seekercompass.modification.modifiers.*;
import net.smelly.seekercompass.network.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collections;

/**
 * @author SmellyModder(Luke Tonon)
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
	public static final RegistryObject<Item> SEEKER_COMPASS = ITEMS.register("seeker_compass", () -> new SeekerCompassItem((new Item.Properties()).stacksTo(1).durability(1200).rarity(Rarity.UNCOMMON).tab(ItemGroup.TAB_TOOLS)));

	public SeekerCompass() {
		instance = this;

		CHANNEL.registerMessage(0, S2CParticleMessage.class, S2CParticleMessage::serialize, S2CParticleMessage::deserialize, S2CParticleMessage::handle);
		CHANNEL.registerMessage(1, S2CUpdateStalkerMessage.class, S2CUpdateStalkerMessage::serialize, S2CUpdateStalkerMessage::deserialize, S2CUpdateStalkerMessage::handle);
		CHANNEL.registerMessage(2, C2SStopStalkingMessage.class, C2SStopStalkingMessage::serialize, C2SStopStalkingMessage::deserialize, C2SStopStalkingMessage::handle);
		CHANNEL.registerMessage(3, S2CUpdateStalkedMessage.class, S2CUpdateStalkedMessage::serialize, S2CUpdateStalkedMessage::deserialize, S2CUpdateStalkedMessage::handle);

		final IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
		ITEMS.register(modEventBus);
		SCEnchants.ENCHANTMENTS.register(modEventBus);
		SCLootModifiers.load();

		modEventBus.addListener(this::setupCommon);
		modEventBus.addListener(this::onGatherData);

		DistExecutor.runWhenOn(Dist.CLIENT, () -> () -> {
			modEventBus.addListener(EventPriority.LOWEST, this::setupClient);
		});
	}

	private void setupCommon(final FMLCommonSetupEvent event) {
		ItemGroup.TAB_TOOLS.setEnchantmentCategories(add(ItemGroup.TAB_TOOLS.getEnchantmentCategories(), SCEnchants.SEEKER_COMPASS));
	}

	private void onGatherData(GatherDataEvent event) {
		DataGenerator dataGenerator = event.getGenerator();
		if (event.includeServer()) {
			dataGenerator.addProvider(createLootModifierDataProvider(dataGenerator));
		}
	}

	@OnlyIn(Dist.CLIENT)
	private void setupClient(final FMLClientSetupEvent event) {
		DeferredWorkQueue.runLater(() -> {
			ItemModelsProperties.register(SEEKER_COMPASS.get(), new ResourceLocation("angle"), new IItemPropertyGetter() {
				private double rotation;
				private double rota;
				private long lastUpdateTick;

				@OnlyIn(Dist.CLIENT)
				public float call(ItemStack stack, @Nullable ClientWorld world, @Nullable LivingEntity livingEntity) {
					if (!SeekerCompassItem.isNotBroken(stack)) {
						return 0.0F;
					} else {
						if (livingEntity == null && !stack.isFramed()) {
							return 0.484375F;
						} else {
							boolean flag = livingEntity != null;
							Entity entity = flag ? livingEntity : stack.getFrame();
							if (world == null) {
								world = (ClientWorld) entity.level;
							}

							CompoundNBT tag = stack.getTag();
							if (tag != null && tag.contains("Rotations") && tag.contains("EntityStatus") && !stack.isFramed()) {
								return (float) SeekerCompassItem.positiveModulo(SeekerCompassItem.RotationData.read(tag.getCompound("Rotations")).rotation, 1.0F);
							} else {
								double randRotation = Math.random();

								if (flag) {
									randRotation = this.wobble(world, randRotation);
								}

								return (float) SeekerCompassItem.positiveModulo((float) randRotation, 1.0F);
							}
						}
					}
				}

				@OnlyIn(Dist.CLIENT)
				private double wobble(ClientWorld world, double rotation) {
					if (world.getGameTime() != this.lastUpdateTick) {
						this.lastUpdateTick = world.getGameTime();
						double d0 = rotation - this.rotation;
						d0 = SeekerCompassItem.positiveModulo(d0 + 0.5D, 1.0D) - 0.5D;
						this.rota += d0 * 0.1D;
						this.rota *= 0.8D;
						this.rotation = SeekerCompassItem.positiveModulo(this.rotation + this.rota, 1.0D);
					}

					return this.rotation;
				}
			});
			ItemModelsProperties.register(SEEKER_COMPASS.get(), new ResourceLocation("broken"), (stack, world, entity) -> SeekerCompassItem.isNotBroken(stack) ? 0.0F : 1.0F);
		});
	}

	private static EnchantmentType[] add(EnchantmentType[] array, EnchantmentType element) {
		int arrayLength = Array.getLength(array);
		Object newArrayObject = Array.newInstance(array.getClass().getComponentType(), arrayLength + 1);
		System.arraycopy(array, 0, newArrayObject, 0, arrayLength);
		array[array.length - 1] = element;
		return array;
	}

	private static ModifierDataProvider<LootTableLoadEvent, Gson, Pair<Gson, LootPredicateManager>> createLootModifierDataProvider(DataGenerator dataGenerator) {
		return LootModifiers.createDataProvider(dataGenerator, "Seeker Compass Loot Modifiers", MOD_ID,
				new ModifierDataProvider.ProviderEntry<>(
						new TargetedModifier<>(
								new ResourceLocation("gameplay/fishing/treasure"),
								Arrays.asList(
										new ConfiguredModifier<>(LootModifiers.ENTRIES_MODIFIER, new LootPoolEntriesModifier.Config(false, 0, Collections.singletonList(ItemLootEntry.lootTableItem(SEEKER_COMPASS.get()).apply(SetDamage.setDamage(new RandomValueRange(0.0F))).build()))),
										new ConfiguredModifier<>(SCLootModifiers.BIASED_ITEM_WEIGHT_MODIFIER, new BiasedItemWeightModifier.Config(0, 1, SEEKER_COMPASS))
								)
						)
				),
				new ModifierDataProvider.ProviderEntry<>(
						new TargetedModifier<>(
								new ResourceLocation("chests/nether_bridge"),
								Collections.singletonList(new ConfiguredModifier<>(LootModifiers.ENTRIES_MODIFIER, new LootPoolEntriesModifier.Config(false, 0, Collections.singletonList(ItemLootEntry.lootTableItem(SEEKER_COMPASS.get()).apply(SetCount.setCount(RandomValueRange.between(0.0F, 1.0F))).build()))))
						)
				)
		);
	}
}