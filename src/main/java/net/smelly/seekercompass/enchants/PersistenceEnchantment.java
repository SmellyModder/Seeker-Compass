package net.smelly.seekercompass.enchants;

import net.minecraft.world.item.enchantment.Enchantment;

public class PersistenceEnchantment extends SeekerCompassEnchant {

	public PersistenceEnchantment() {
		super(Enchantment.Rarity.VERY_RARE);
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