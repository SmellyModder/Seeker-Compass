package net.smelly.seekercompass.enchants;

import net.minecraft.enchantment.Enchantment;

public class StalkingEnchantment extends SeekerCompassEnchant {

	public StalkingEnchantment() {
		super(Rarity.RARE);
	}

	@Override
	public boolean checkCompatibility(Enchantment ench) {
		return !(ench instanceof WarpingEnchantment || ench instanceof VoodooEnchantment || ench instanceof SummoningEnchantment) && super.checkCompatibility(ench);
	}

}
