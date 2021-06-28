package net.smelly.seekercompass.enchants;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentType;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.smelly.seekercompass.SeekerCompass;

/**
 * @author SmellyModder(Luke Tonon)
 */
public class SCEnchants {
	public static final DeferredRegister<Enchantment> ENCHANTMENTS = DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, SeekerCompass.MOD_ID);
	public static final EnchantmentType SEEKER_COMPASS = EnchantmentType.create("SEEKER_COMPASS", (item) -> item == SeekerCompass.SEEKER_COMPASS.get());
	
	public static final RegistryObject<Enchantment> VOODOO = ENCHANTMENTS.register("voodoo", VoodooEnchantment::new);
	public static final RegistryObject<Enchantment> TRACKING = ENCHANTMENTS.register("tracking", TrackingEnchantment::new);
	public static final RegistryObject<Enchantment> WARPING = ENCHANTMENTS.register("warping", WarpingEnchantment::new);
	public static final RegistryObject<Enchantment> SUMMONING = ENCHANTMENTS.register("summoning", SummoningEnchantment::new);
	public static final RegistryObject<Enchantment> PERSISTENCE = ENCHANTMENTS.register("persistence", PersistenceEnchantment::new);
	public static final RegistryObject<Enchantment> STALKING = ENCHANTMENTS.register("stalking", StalkingEnchantment::new);
}