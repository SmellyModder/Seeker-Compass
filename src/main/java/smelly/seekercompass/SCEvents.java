package smelly.seekercompass;

import java.util.Random;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.monster.ZombiePigmanEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.stats.Stats;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.network.PacketDistributor;

@EventBusSubscriber(modid = SeekerCompass.MOD_ID)
public class SCEvents {
	private static final String TAG_SPAWNED = "seeker_compass:pigman_spawned";
	
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
					
					Random rand = player.getRNG();
					for(int i = 0; i < 8; i++) {
						Vec3d targetPosition = target.getPositionVec();
						Vec3d position = targetPosition.add(rand.nextBoolean() ? -rand.nextFloat() : rand.nextFloat() * 1.25F, target.getEyeHeight(), rand.nextBoolean() ? -rand.nextFloat() : rand.nextFloat() * 1.25F);
						Vec3d motion = position.subtract(targetPosition.add(0.0F, target.getEyeHeight() * 0.35F, 0.0F)).scale(-0.5F);
						
						SeekerCompass.CHANNEL.send(PacketDistributor.ALL.with(() -> null), new MessageS2CParticle("seeker_compass:seeker_eyes", position.getX(), position.getY(), position.getZ(), motion.getX(), motion.getY(), motion.getZ()));
					}
				}
			}
		}
	}
	
	@SubscribeEvent
	public static void onEntitySpawned(EntityJoinWorldEvent event) {
		if(event.getWorld().isRemote) return;
		
		Entity entity = event.getEntity();
		if(entity.getType() == EntityType.ZOMBIE_PIGMAN) {
			CompoundNBT nbt = entity.getPersistentData();
			if(!nbt.getBoolean(TAG_SPAWNED)) {
				ZombiePigmanEntity pigman = (ZombiePigmanEntity) entity;
				if(pigman.getItemStackFromSlot(EquipmentSlotType.OFFHAND).isEmpty() && pigman.getRNG().nextFloat() < 0.02F) {
					pigman.setItemStackToSlot(EquipmentSlotType.OFFHAND, new ItemStack(SeekerCompass.SEEKER_COMPASS.get()));
					pigman.setDropChance(EquipmentSlotType.OFFHAND, 2.0F);
				}
				nbt.putBoolean(TAG_SPAWNED, true);
			}
		}
	}
}