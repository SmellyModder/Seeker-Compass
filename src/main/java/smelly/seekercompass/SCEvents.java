package smelly.seekercompass;

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
		World world = event.getWorld();
		Entity target = event.getTarget();
		
		if (world.isRemote || target == null) return;
		
		PlayerEntity player = event.getPlayer();
		if (target instanceof LivingEntity) {
			LivingEntity livingEntity = (LivingEntity) target;
			if (livingEntity.isAlive()) {
				Hand hand = event.getHand();
				ItemStack itemstack = player.getHeldItem(hand);
				
				if (itemstack.getItem() == SeekerCompass.SEEKER_COMPASS.get() && SeekerCompassItem.isNotBroken(itemstack)) {
					CompoundNBT tag = itemstack.getTag();
					if (tag != null && tag.contains("TrackingEntity")) {
						Entity entity = ((ServerWorld) world).getEntityByUuid(NBTUtil.readUniqueId(tag.get("TrackingEntity")));
						
						if (entity == target) {
							tag.remove("TrackingEntity");
							tag.remove("EntityStatus");
							tag.remove("Rotations");
							player.world.playSound(null, target.func_233580_cy_(), SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 0.75F, 1.5F);
							
							Random rand = player.getRNG();
							for (int i = 0; i < 8; i++) {
								Vector3d targetPosition = target.getPositionVec();
								Vector3d position = targetPosition.add(rand.nextBoolean() ? -rand.nextFloat() : rand.nextFloat() * 1.25F, target.getEyeHeight(), rand.nextBoolean() ? -rand.nextFloat() : rand.nextFloat() * 1.25F);
								Vector3d motion = targetPosition.subtract(position.add(0.0F, target.getEyeHeight() * 0.35F, 0.0F)).scale(-0.5F);
								
								SeekerCompass.CHANNEL.send(PacketDistributor.ALL.with(() -> null), new MessageS2CParticle("seeker_compass:seeker_eyes", targetPosition.getX(), targetPosition.getY(), targetPosition.getZ(), motion.getX(), motion.getY(), motion.getZ()));
							}
							return;
						}
					}
					
					itemstack.getOrCreateTag().put("TrackingEntity", NBTUtil.func_240626_a_(target.getUniqueID()));
					player.addStat(Stats.ITEM_USED.get(itemstack.getItem()));
					player.swingArm(hand);
					player.world.playSound(null, target.func_233580_cy_(), SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 0.75F, 0.25F);
					
					Random rand = player.getRNG();
					Vector3d targetPosition = target.getPositionVec();
					for (int i = 0; i < 8; i++) {
						Vector3d position = targetPosition.add(rand.nextBoolean() ? -rand.nextFloat() : rand.nextFloat() * 1.25F, target.getEyeHeight(), rand.nextBoolean() ? -rand.nextFloat() : rand.nextFloat() * 1.25F);
						Vector3d motion = position.subtract(targetPosition.add(0.0F, target.getEyeHeight() * 0.35F, 0.0F)).scale(-0.5F);
						SeekerCompass.CHANNEL.send(PacketDistributor.ALL.with(() -> null), new MessageS2CParticle("seeker_compass:seeker_eyes", position.getX(), position.getY(), position.getZ(), motion.getX(), motion.getY(), motion.getZ()));
					}
				}
			}
		}
	}
	
	@SubscribeEvent
	public static void onEntitySpawned(EntityJoinWorldEvent event) {
		if (event.getWorld().isRemote) return;
		
		Entity entity = event.getEntity();
		if (entity instanceof ZombifiedPiglinEntity) {
			CompoundNBT nbt = entity.getPersistentData();
			if (!nbt.getBoolean(TAG_SPAWNED)) {
				ZombifiedPiglinEntity piglin = (ZombifiedPiglinEntity) entity;
				if (piglin.getItemStackFromSlot(EquipmentSlotType.OFFHAND).isEmpty() && piglin.getRNG().nextFloat() < 0.02F) {
					piglin.setItemStackToSlot(EquipmentSlotType.OFFHAND, new ItemStack(SeekerCompass.SEEKER_COMPASS.get()));
					piglin.setDropChance(EquipmentSlotType.OFFHAND, 2.0F);
				}
				nbt.putBoolean(TAG_SPAWNED, true);
			}
		}
	}
	
	@SubscribeEvent
	public static void onEntityTick(LivingEvent.LivingUpdateEvent event) {
		LivingEntity entity = event.getEntityLiving();
		ChunkPos chunkpos = new ChunkPos(entity.func_233580_cy_());
		CompoundNBT tag = entity.getPersistentData();
		
		if (!(entity.getEntityWorld() instanceof ServerWorld)) return;
		ServerWorld world = (ServerWorld) entity.getEntityWorld();
		
		if (tag.contains(TAG_CHUNK_UPDATE) && tag.getBoolean(TAG_CHUNK_UPDATE)) {
			if (tag.contains(TAG_PREV_CHUNK)) {
				long prevChunkLong = tag.getLong(TAG_PREV_CHUNK);
				ChunkPos prevChunkPos = new ChunkPos(ChunkPos.getX(prevChunkLong), ChunkPos.getZ(prevChunkLong));
				if (!chunkpos.equals(prevChunkPos)) {
					if (!isChunkForced(world, prevChunkPos)) {
						world.getChunkProvider().forceChunk(prevChunkPos, false);
					}
				}
			}
			
			if (tag.contains(TAG_CHUNK_TIMER)) {
				int timer = tag.getInt(TAG_CHUNK_TIMER);
				if (timer > 0) {
					world.getChunkProvider().forceChunk(chunkpos, true);
					tag.putInt(TAG_CHUNK_TIMER, timer - 1);
				} else {
					if (!isChunkForced(world, chunkpos)) {
						world.getChunkProvider().forceChunk(chunkpos, false);
					}
					tag.putBoolean(TAG_CHUNK_UPDATE, false);
				}
				tag.putLong(TAG_PREV_CHUNK, chunkpos.asLong());
			}
		}
	}
	
	/*
	 * Checks if the chunk(chunk to be unloaded) is a spawn chunk or forced already by the force chunk command
	 */
	public static boolean isChunkForced(ServerWorld world, ChunkPos pos) {
		ChunkPos spawnChunk = new ChunkPos(new BlockPos(world.getWorldInfo().getSpawnX(), 0, world.getWorldInfo().getSpawnZ()));
		Stream<ChunkPos> spawnChunks = ChunkPos.getAllInBox(spawnChunk, 11);
		
		for (long values : world.getForcedChunks()) {
			if (pos.equals(new ChunkPos(ChunkPos.getX(values), ChunkPos.getZ(values)))) {
				return true;
			}
		}
		
		if (spawnChunks.anyMatch(chunk -> chunk.equals(pos))) {
			return true;
		}
		
		return false;
	}
}