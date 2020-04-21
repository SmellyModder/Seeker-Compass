package smelly.seekercompass.enchants;

public class TrackingEnchant extends SeekerCompassEnchant {

	public TrackingEnchant() {
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