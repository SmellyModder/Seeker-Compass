package smelly.seekercompass;

import java.util.Random;
import java.util.stream.Stream;

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
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.network.PacketDistributor;

/**
 * @author SmellyModder(Luke Tonon)
 */
@EventBusSubscriber(modid = SeekerCompass.MOD_ID)
public class SCEvents {
	private static final String TAG_SPAWNED = "seeker_compass:pigman_spawned";
	public static final String TAG_POS = "seeker_compass:teleport_pos";
	public static final String TAG_CHUNK_UPDATE = "seeker_compass:chunk_update";
	public static final String TAG_CHUNK_TIMER = "seeker_compass:chunk_timer";
	public static final String TAG_PREV_CHUNK = "seeker_compass:prev_chunk";
	
	@SubscribeEvent
	public static void trackEntity(PlayerInteractEvent.EntityInteract event) {
		if(event.getWorld().isRemote || event.getTarget() == null) return;
		
		World world = event.getWorld();
		
		PlayerEntity player = event.getPlayer();
		Entity target = event.getTarget();
		if(target instanceof LivingEntity) {
			LivingEntity livingEntity = (LivingEntity) target;
			if(livingEntity.isAlive()) {
				Hand hand = event.getHand();
				ItemStack itemstack = player.getHeldItem(hand);
				
				if(itemstack.getItem() == SeekerCompass.SEEKER_COMPASS.get() && SeekerCompassItem.isNotBroken(itemstack)) {
					if(itemstack.hasTag() && itemstack.getTag().contains("TrackingEntity")) {
						Entity entity = ((ServerWorld) world).getEntityByUuid(NBTUtil.readUniqueId(itemstack.getTag().getCompound("TrackingEntity")));
						
						if(entity == target) {
							itemstack.getTag().remove("TrackingEntity");
							itemstack.getTag().remove("EntityStatus");
							itemstack.getTag().remove("Rotations");
							player.world.playSound(null, target.getPosition(), SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 0.75F, 1.5F);
							
							Random rand = player.getRNG();
							for(int i = 0; i < 8; i++) {
								Vec3d targetPosition = target.getPositionVec();
								Vec3d position = targetPosition.add(rand.nextBoolean() ? -rand.nextFloat() : rand.nextFloat() * 1.25F, target.getEyeHeight(), rand.nextBoolean() ? -rand.nextFloat() : rand.nextFloat() * 1.25F);
								Vec3d motion = targetPosition.subtract(position.add(0.0F, target.getEyeHeight() * 0.35F, 0.0F)).scale(-0.5F);
								
								SeekerCompass.CHANNEL.send(PacketDistributor.ALL.with(() -> null), new MessageS2CParticle("seeker_compass:seeker_eyes", targetPosition.getX(), targetPosition.getY(), targetPosition.getZ(), motion.getX(), motion.getY(), motion.getZ()));
							}
							return;
						}
					}
					
					itemstack.getOrCreateTag().put("TrackingEntity", NBTUtil.writeUniqueId(target.getUniqueID()));
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
	
	@SubscribeEvent
	public static void onEntityTick(LivingEvent.LivingUpdateEvent event) {
		LivingEntity entity = event.getEntityLiving();
		ChunkPos chunkpos = new ChunkPos(entity.getPosition());
		CompoundNBT tag = entity.getPersistentData();
		
		if(!(entity.getEntityWorld() instanceof ServerWorld)) return;
		ServerWorld world = (ServerWorld) entity.getEntityWorld();
		
		if(tag.contains(TAG_CHUNK_UPDATE) && tag.getBoolean(TAG_CHUNK_UPDATE)) {
			if(tag.contains(TAG_PREV_CHUNK)) {
				long prevChunkLong = tag.getLong(TAG_PREV_CHUNK);
				ChunkPos prevChunkPos = new ChunkPos(ChunkPos.getX(prevChunkLong), ChunkPos.getZ(prevChunkLong));
				if(!chunkpos.equals(prevChunkPos)) {
					if(!isChunkForced(world, prevChunkPos)) {
						world.getChunkProvider().forceChunk(prevChunkPos, false);
					}
				}
			}
			
			if(tag.contains(TAG_CHUNK_TIMER)) {
				int timer = tag.getInt(TAG_CHUNK_TIMER);
				if(timer > 0) {
					world.getChunkProvider().forceChunk(chunkpos, true);
					entity.getPersistentData().putInt(TAG_CHUNK_TIMER, timer - 1);
				} else {
					if(!isChunkForced(world, chunkpos)) {
						world.getChunkProvider().forceChunk(chunkpos, false);
					}
					entity.getPersistentData().putBoolean(TAG_CHUNK_UPDATE, false);
				}
				entity.getPersistentData().putLong(TAG_PREV_CHUNK, chunkpos.asLong());
			}
		}
	}
	
	/*
	 * Checks if the chunk(chunk to be unloaded) is a spawn chunk or forced already by the force chunk command
	 */
	public static boolean isChunkForced(ServerWorld world, ChunkPos pos) {
		ChunkPos spawnChunk = new ChunkPos(new BlockPos(world.getWorldInfo().getSpawnX(), 0, world.getWorldInfo().getSpawnZ()));
		Stream<ChunkPos> spawnChunks = ChunkPos.getAllInBox(spawnChunk, 11);
		
		for(long values : world.getForcedChunks()) {
			if(pos.equals(new ChunkPos(ChunkPos.getX(values), ChunkPos.getZ(values)))) {
				return true;
			}
		}
		
		if(spawnChunks.anyMatch(chunk -> chunk.equals(pos))) {
			return true;
		}
		
		return false;
	}
}