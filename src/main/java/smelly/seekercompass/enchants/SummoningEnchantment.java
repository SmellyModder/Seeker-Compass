package smelly.seekercompass.enchants;

import net.minecraft.enchantment.Enchantment;

/**
 * @author SmellyModder(Luke Tonon)
 */
public class SummoningEnchantment extends SeekerCompassEnchant {

	public SummoningEnchantment(Rarity rarity) {
		super(Rarity.RARE);
	}
	
	@Override
	public boolean canApplyTogether(Enchantment ench) {
		return (ench instanceof WarpingEnchantment || ench instanceof VoodooEnchantment) ? false : super.canApplyTogether(ench);
	}

}