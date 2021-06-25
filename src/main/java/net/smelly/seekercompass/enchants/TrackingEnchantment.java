package net.smelly.seekercompass.enchants;

/**
 * @author SmellyModder(Luke Tonon)
 */
public class TrackingEnchantment extends SeekerCompassEnchant {

	public TrackingEnchantment() {
		super(Rarity.UNCOMMON);
	}
	
	@Override
	public int getMinCost(int enchantmentLevel) {
		return 10;
	}

	@Override
	public int getMaxCost(int enchantmentLevel) {
		return 30;
	}

}