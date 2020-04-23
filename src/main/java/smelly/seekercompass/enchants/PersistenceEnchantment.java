package smelly.seekercompass.enchants;

public class PersistenceEnchantment extends SeekerCompassEnchant {

	public PersistenceEnchantment() {
		super(Rarity.VERY_RARE);
	}
	
	@Override
	public int getMinEnchantability(int enchantmentLevel) {
		return 30;
	}
	
	@Override
	public int getMaxEnchantability(int enchantmentLevel) {
		return 60;
	}

}