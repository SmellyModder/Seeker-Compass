package smelly.seekercompass;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.stats.Stats;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = SeekerCompass.MOD_ID)
public class SCEvents {

	@SubscribeEvent
	public static void trackEntity(PlayerInteractEvent.EntityInteract event) {
		if(event.getWorld().isRemote || event.getTarget() == null) return;
		
		PlayerEntity player = event.getPlayer();
		Entity target = event.getTarget();
		if(target instanceof LivingEntity) {
			LivingEntity livingEntity = (LivingEntity) target;
			if(livingEntity.isAlive()) {
				Hand hand = event.getHand();
				ItemStack itemstack = player.getHeldItem(hand);
				
				boolean flag = itemstack.hasTag() && itemstack.getTag().contains("TrackingEntity") ? target.getUniqueID() != NBTUtil.readUniqueId(itemstack.getTag().getCompound("TrackingEntity")) : true;
				
				if(itemstack.getItem() == SeekerCompass.SEEKER_COMPASS.get() && flag && SeekerCompassItem.isNotBroken(itemstack) && player.isShiftKeyDown()) {
					itemstack.getOrCreateTag().put("TrackingEntity", NBTUtil.writeUniqueId(livingEntity.getUniqueID()));
					player.addStat(Stats.ITEM_USED.get(itemstack.getItem()));
					player.swingArm(hand);
					player.world.playSound(null, target.getPosition(), SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 0.75F, 0.25F);
				}
			}
		}
	}
	
}