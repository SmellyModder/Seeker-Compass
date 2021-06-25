package net.smelly.seekercompass.enchants;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.MendingEnchantment;

/**
 * @author SmellyModder(Luke Tonon)
 */
public class VoodooEnchantment extends SeekerCompassEnchant {

	public VoodooEnchantment() {
		super(Rarity.RARE);
	}
	
	@Override
	public int getMinEnchantability(int enchantmentLevel) {
		return 20;
	}
	
	@Override
	public int getMaxEnchantability(int enchantmentLevel) {
		return 60;
	}
	
	@Override
	public int getMaxLevel() {
		return 3;
	}
	
	@Override
	public boolean canApplyTogether(Enchantment ench) {
		return (!(ench instanceof MendingEnchantment) && !(ench instanceof WarpingEnchantment) && !(ench instanceof SummoningEnchantment)) && super.canApplyTogether(ench);
	}

}