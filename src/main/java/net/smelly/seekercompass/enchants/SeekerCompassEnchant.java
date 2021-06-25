package net.smelly.seekercompass.enchants;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.inventory.EquipmentSlotType;

/**
 * @author SmellyModder(Luke Tonon)
 */
public abstract class SeekerCompassEnchant extends Enchantment {

	public SeekerCompassEnchant(Rarity rarityIn) {
		super(rarityIn, SCEnchants.SEEKER_COMPASS, new EquipmentSlotType[] {EquipmentSlotType.MAINHAND});
	}
	
	@Override
	public boolean isTreasureOnly() {
		return true;
	}

}