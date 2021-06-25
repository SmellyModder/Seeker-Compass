package net.smelly.seekercompass.enchants;

import net.minecraft.enchantment.Enchantment;

/**
 * @author SmellyModder(Luke Tonon)
 */
public class WarpingEnchantment extends SeekerCompassEnchant {

	public WarpingEnchantment() {
		super(Rarity.VERY_RARE);
	}
	
	@Override
	public int getMinCost(int enchantmentLevel) {
		return 40;
	}

	@Override
	public int getMaxCost(int enchantmentLevel) {
		return 60;
	}
	
	@Override
	protected boolean checkCompatibility(Enchantment ench) {
		return (!(ench instanceof VoodooEnchantment) && !(ench instanceof SummoningEnchantment)) && super.checkCompatibility(ench);
	}

}