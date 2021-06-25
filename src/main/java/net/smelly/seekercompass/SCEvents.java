package net.smelly.seekercompass;

import java.util.Random;
import java.util.stream.Stream;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.monster.ZombifiedPiglinEntity;
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
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.IWorldInfo;
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
	public static final String TAG_CHUNK_UPDATE = "seeker_compass:chunk_update";
	public static final String TAG_CHUNK_TIMER = "seeker_compass:chunk_timer";
	private static final String TAG_PREV_CHUNK = "seeker_compass:prev_chunk";
	
	@SubscribeEvent
	public static void trackEntity(PlayerInteractEvent.EntityInteract event) {
		World level = event.getWorld();
		Entity target = event.getTarget();
		
		if (level.isClientSide || target == null) return;
		
		PlayerEntity player = event.getPlayer();
		if (target instanceof LivingEntity) {
			LivingEntity livingEntity = (LivingEntity) target;
			if (livingEntity.isAlive()) {
				Hand hand = event.getHand();
				ItemStack itemstack = player.getItemInHand(hand);
				
				if (itemstack.getItem() == SeekerCompass.SEEKER_COMPASS.get() && SeekerCompassItem.isNotBroken(itemstack)) {
					CompoundNBT tag = itemstack.getTag();
					boolean hasTag = tag != null;
					if (hasTag && tag.getBoolean("TrackingOnly")) return;
					if (hasTag && tag.contains("TrackingEntity")) {
						Entity entity = ((ServerWorld) level).getEntity(NBTUtil.loadUUID(tag.get("TrackingEntity")));
						
						if (entity == target) {
							tag.remove("TrackingEntity");
							tag.remove("EntityStatus");
							tag.remove("Rotations");
							player.level.playSound(null, target.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 0.75F, 1.5F);
							
							Random rand = player.getRandom();
							for (int i = 0; i < 8; i++) {
								Vector3d targetPosition = target.position();
								Vector3d position = targetPosition.add(rand.nextBoolean() ? -rand.nextFloat() : rand.nextFloat() * 1.25F, target.getEyeHeight(), rand.nextBoolean() ? -rand.nextFloat() : rand.nextFloat() * 1.25F);
								Vector3d motion = targetPosition.subtract(position.add(0.0F, target.getEyeHeight() * 0.35F, 0.0F)).scale(-0.5F);
								
								SeekerCompass.CHANNEL.send(PacketDistributor.ALL.with(() -> null), new MessageS2CParticle("seeker_compass:seeker_eyes", targetPosition.x(), targetPosition.y(), targetPosition.z(), motion.x(), motion.y(), motion.z()));
							}
							return;
						}
					}
					
					itemstack.getOrCreateTag().put("TrackingEntity", NBTUtil.createUUID(target.getUUID()));
					player.awardStat(Stats.ITEM_USED.get(itemstack.getItem()));
					player.swing(hand);
					player.level.playSound(null, target.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 0.75F, 0.25F);
					
					Random rand = player.getRandom();
					Vector3d targetPosition = target.position();
					for (int i = 0; i < 8; i++) {
						Vector3d position = targetPosition.add(rand.nextBoolean() ? -rand.nextFloat() : rand.nextFloat() * 1.25F, target.getEyeHeight(), rand.nextBoolean() ? -rand.nextFloat() : rand.nextFloat() * 1.25F);
						Vector3d motion = position.subtract(targetPosition.add(0.0F, target.getEyeHeight() * 0.35F, 0.0F)).scale(-0.5F);
						SeekerCompass.CHANNEL.send(PacketDistributor.ALL.with(() -> null), new MessageS2CParticle("seeker_compass:seeker_eyes", position.x(), position.y(), position.z(), motion.x(), motion.y(), motion.z()));
					}
				}
			}
		}
	}
	
	@SubscribeEvent
	public static void onEntitySpawned(EntityJoinWorldEvent event) {
		if (event.getWorld().isClientSide) return;
		
		Entity entity = event.getEntity();
		if (entity instanceof ZombifiedPiglinEntity) {
			CompoundNBT nbt = entity.getPersistentData();
			if (!nbt.getBoolean(TAG_SPAWNED)) {
				ZombifiedPiglinEntity piglin = (ZombifiedPiglinEntity) entity;
				if (piglin.getItemBySlot(EquipmentSlotType.OFFHAND).isEmpty() && piglin.getRandom().nextFloat() < 0.02F) {
					piglin.setItemSlot(EquipmentSlotType.OFFHAND, new ItemStack(SeekerCompass.SEEKER_COMPASS.get()));
					piglin.setDropChance(EquipmentSlotType.OFFHAND, 2.0F);
				}
				nbt.putBoolean(TAG_SPAWNED, true);
			}
		}
	}
	
	@SubscribeEvent
	public static void onEntityTick(LivingEvent.LivingUpdateEvent event) {
		LivingEntity entity = event.getEntityLiving();
		ChunkPos chunkpos = new ChunkPos(entity.blockPosition());
		CompoundNBT tag = entity.getPersistentData();
		
		if (!(entity.level instanceof ServerWorld)) return;
		ServerWorld level = (ServerWorld) entity.level;
		
		if (tag.contains(TAG_CHUNK_UPDATE) && tag.getBoolean(TAG_CHUNK_UPDATE)) {
			if (tag.contains(TAG_PREV_CHUNK)) {
				long prevChunkLong = tag.getLong(TAG_PREV_CHUNK);
				ChunkPos prevChunkPos = new ChunkPos(ChunkPos.getX(prevChunkLong), ChunkPos.getZ(prevChunkLong));
				if (!chunkpos.equals(prevChunkPos)) {
					if (!isChunkForced(level, prevChunkPos)) {
						level.getChunkSource().updateChunkForced(prevChunkPos, false);
					}
				}
			}
			
			if (tag.contains(TAG_CHUNK_TIMER)) {
				int timer = tag.getInt(TAG_CHUNK_TIMER);
				if (timer > 0) {
					level.getChunkSource().updateChunkForced(chunkpos, true);
					tag.putInt(TAG_CHUNK_TIMER, timer - 1);
				} else {
					if (!isChunkForced(level, chunkpos)) {
						level.getChunkSource().updateChunkForced(chunkpos, false);
					}
					tag.putBoolean(TAG_CHUNK_UPDATE, false);
				}
				tag.putLong(TAG_PREV_CHUNK, chunkpos.toLong());
			}
		}
	}
	
	/*
	 * Checks if the chunk(chunk to be unloaded) is a spawn chunk or forced already by the force chunk command
	 */
	public static boolean isChunkForced(ServerWorld level, ChunkPos pos) {
		IWorldInfo levelData = level.getLevelData();
		ChunkPos spawnChunk = new ChunkPos(new BlockPos(levelData.getXSpawn(), 0, levelData.getZSpawn()));
		Stream<ChunkPos> spawnChunks = ChunkPos.rangeClosed(spawnChunk, 11);
		
		for (long values : level.getForcedChunks()) {
			if (pos.equals(new ChunkPos(ChunkPos.getX(values), ChunkPos.getZ(values)))) {
				return true;
			}
		}

		return spawnChunks.anyMatch(chunk -> chunk.equals(pos));
	}
}