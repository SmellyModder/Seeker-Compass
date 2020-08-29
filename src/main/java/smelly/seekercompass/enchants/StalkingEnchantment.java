package smelly.seekercompass.enchants;

import net.minecraft.enchantment.Enchantment;

public final class StalkingEnchantment extends SeekerCompassEnchant {

    public StalkingEnchantment() {
        super(Rarity.RARE);
    }

    @Override
    public boolean canApplyTogether(Enchantment ench) {
        return !(ench instanceof WarpingEnchantment || ench instanceof VoodooEnchantment || ench instanceof SummoningEnchantment) && super.canApplyTogether(ench);
    }

}