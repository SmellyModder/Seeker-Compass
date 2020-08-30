package smelly.seekercompass;

import java.lang.reflect.Array;

import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.enchantment.EnchantmentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.IItemPropertyGetter;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemModelsProperties;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Rarity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DeferredWorkQueue;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import smelly.seekercompass.SeekerCompassItem.RotationData;
import smelly.seekercompass.enchants.SCEnchants;
import smelly.seekercompass.network.MessageC2SResetStalker;
import smelly.seekercompass.network.MessageS2CParticle;
import smelly.seekercompass.network.MessageSC2UpdateStalker;

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
	public static final RegistryObject<Item> SEEKER_COMPASS = ITEMS.register("seeker_compass", () -> new SeekerCompassItem((new Item.Properties()).maxDamage(1200).rarity(Rarity.UNCOMMON).group(ItemGroup.TOOLS)));
	
	public SeekerCompass() {
		instance = this;

		CHANNEL.registerMessage(0, MessageS2CParticle.class, MessageS2CParticle::serialize, MessageS2CParticle::deserialize, MessageS2CParticle::handle);
		CHANNEL.registerMessage(1, MessageSC2UpdateStalker.class, MessageSC2UpdateStalker::serialize, MessageSC2UpdateStalker::deserialize, MessageSC2UpdateStalker::handle);
		CHANNEL.registerMessage(2, MessageC2SResetStalker.class, MessageC2SResetStalker::serialize, MessageC2SResetStalker::deserialize, MessageC2SResetStalker::handle);

		final IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
		ITEMS.register(modEventBus);
		SCEnchants.ENCHANTMENTS.register(modEventBus);
		
		modEventBus.addListener(this::setupCommon);
		
		DistExecutor.runWhenOn(Dist.CLIENT, () -> () -> {
			modEventBus.addListener(EventPriority.LOWEST, this::setupClient);
		});
	}
	
	private void setupCommon(final FMLCommonSetupEvent event) {
		ItemGroup.TOOLS.setRelevantEnchantmentTypes(add(ItemGroup.TOOLS.getRelevantEnchantmentTypes(), SCEnchants.SEEKER_COMPASS));
	}
	
	@OnlyIn(Dist.CLIENT)
	private void setupClient(final FMLClientSetupEvent event) {
		DeferredWorkQueue.runLater(() -> {
			ItemModelsProperties.func_239418_a_(SEEKER_COMPASS.get(), new ResourceLocation("angle"), new IItemPropertyGetter() {
				private double rotation;
				private double rota;
				private long lastUpdateTick;
				
				@OnlyIn(Dist.CLIENT)
				public float call(ItemStack stack, @Nullable ClientWorld world, @Nullable LivingEntity livingEntity) {
					if (!SeekerCompassItem.isNotBroken(stack)) {
						return 0.0F;
					} else {
						if (livingEntity == null && !stack.isOnItemFrame()) {
							return 0.484375F;
						} else {
							boolean flag = livingEntity != null;
							Entity entity = (Entity)(flag ? livingEntity : stack.getItemFrame());
							if (world == null) {
								world = (ClientWorld) entity.world;
							}
							
							CompoundNBT tag = stack.getTag();
							if (tag != null && tag.contains("Rotations") && tag.contains("EntityStatus") && !stack.isOnItemFrame()) {
								return (float) SeekerCompassItem.positiveModulo(getSCRotation(stack), 1.0F);
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
					if(world.getGameTime() != this.lastUpdateTick) {
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
			ItemModelsProperties.func_239418_a_(SEEKER_COMPASS.get(), new ResourceLocation("broken"), (stack, world, entity) -> SeekerCompassItem.isNotBroken(stack) ? 0.0F : 1.0F);
		});
	}
	
	private static double getSCRotation(ItemStack stack) {
		return RotationData.read(stack.getTag().getCompound("Rotations")).rotation;
	}
	
	private static EnchantmentType[] add(EnchantmentType[] array, EnchantmentType element) {
		EnchantmentType[] newArray = array;
		int arrayLength = Array.getLength(newArray);
		Object newArrayObject = Array.newInstance(newArray.getClass().getComponentType(), arrayLength + 1);
		System.arraycopy(array, 0, newArrayObject, 0, arrayLength);
		newArray[newArray.length - 1] = element;
		return newArray;
	}
}