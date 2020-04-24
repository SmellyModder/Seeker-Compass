package smelly.seekercompass.enchants;

import net.minecraft.enchantment.Enchantment;

/**
 * @author SmellyModder(Luke Tonon)
 */
public class WarpingEnchantment extends SeekerCompassEnchant {

	public WarpingEnchantment() {
		super(Rarity.VERY_RARE);
	}
	
	@Override
	public int getMinEnchantability(int enchantmentLevel) {
		return 40;
	}

	@Override
	public int getMaxEnchantability(int enchantmentLevel) {
		return 60;
	}
	
	@Override
	protected boolean canApplyTogether(Enchantment ench) {
		return (ench instanceof VoodooEnchantment || ench instanceof SummoningEnchantment) ? false : super.canApplyTogether(ench);
	}

}