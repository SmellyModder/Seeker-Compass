package net.smelly.seekercompass.enchants;

public class PersistenceEnchantment extends SeekerCompassEnchant {

	public PersistenceEnchantment() {
		super(Rarity.VERY_RARE);
	}
	
	@Override
	public int getMinCost(int enchantmentLevel) {
		return 30;
	}
	
	@Override
	public int getMaxCost(int enchantmentLevel) {
		return 60;
	}

}