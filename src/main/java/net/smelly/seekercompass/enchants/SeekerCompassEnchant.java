package net.smelly.seekercompass.enchants;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * @author SmellyModder(Luke Tonon)
 */
public abstract class SeekerCompassEnchant extends Enchantment {

	public SeekerCompassEnchant(Rarity rarityIn) {
		super(rarityIn, SCEnchants.SEEKER_COMPASS, new EquipmentSlot[] {EquipmentSlot.MAINHAND});
	}
	
	@Override
	public boolean isTreasureOnly() {
		return true;
	}

}