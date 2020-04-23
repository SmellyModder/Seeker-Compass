package smelly.seekercompass.enchants;

/**
 * @author SmellyModder(Luke Tonon)
 */
public class TrackingEnchantment extends SeekerCompassEnchant {

	public TrackingEnchantment() {
		super(Rarity.UNCOMMON);
	}
	
	@Override
	public int getMinEnchantability(int enchantmentLevel) {
		return 10;
	}

	@Override
	public int getMaxEnchantability(int enchantmentLevel) {
		return 30;
	}

}