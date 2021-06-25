package net.smelly.seekercompass.enchants;

import net.minecraft.enchantment.Enchantment;

/**
 * @author SmellyModder(Luke Tonon)
 */
public class SummoningEnchantment extends SeekerCompassEnchant {

	public SummoningEnchantment() {
		super(Rarity.RARE);
	}
	
	@Override
	public boolean canApplyTogether(Enchantment ench) {
		return (!(ench instanceof WarpingEnchantment) && !(ench instanceof VoodooEnchantment)) && super.canApplyTogether(ench);
	}

}