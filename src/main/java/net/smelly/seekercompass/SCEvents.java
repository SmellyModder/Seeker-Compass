package net.smelly.seekercompass;

import com.teamabnormals.blueprint.core.util.NetworkUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.smelly.seekercompass.particles.SCParticles;

import java.util.stream.Stream;

/**
 * @author SmellyModder (Luke Tonon)
 */
@EventBusSubscriber(modid = SeekerCompass.MOD_ID)
public final class SCEvents {
	public static final String TAG_CHUNK_UPDATE = "seeker_compass:chunk_update";
	public static final String TAG_CHUNK_TIMER = "seeker_compass:chunk_timer";
	private static final String TAG_PREV_CHUNK = "seeker_compass:prev_chunk";

	@SubscribeEvent
	public static void trackEntity(PlayerInteractEvent.EntityInteract event) {
		Level level = event.getLevel();
		Entity target = event.getTarget();

		if (level.isClientSide || target == null) return;

		Player player = event.getEntity();
		if (target instanceof LivingEntity livingEntity) {
			if (livingEntity.isAlive()) {
				InteractionHand hand = event.getHand();
				ItemStack itemstack = player.getItemInHand(hand);

				if (itemstack.getItem() == SeekerCompass.SEEKER_COMPASS.get() && SeekerCompassItem.isNotBroken(itemstack)) {
					CompoundTag tag = itemstack.getTag();
					boolean hasTag = tag != null;
					if (hasTag && tag.getBoolean(SeekerCompassItem.TRACKING_ONLY)) return;
					RandomSource rand = player.getRandom();
					Vec3 targetPosition = target.position();
					var dimension = level.dimension();
					if (hasTag && tag.contains(SeekerCompassItem.TRACKING_TAG)) {
						Entity entity = ((ServerLevel) level).getEntity(tag.getUUID(SeekerCompassItem.TRACKING_TAG));

						if (entity == target) {
							tag.remove(SeekerCompassItem.TRACKING_TAG);
							tag.remove(SeekerCompassItem.ENTITY_TAG);
							player.level().playSound(null, target.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.75F, 1.5F);
							spawnSeekerParticles(targetPosition, rand, target, dimension, true);
							return;
						}
					}

					(tag = itemstack.getOrCreateTag()).put(SeekerCompassItem.TRACKING_TAG, NbtUtils.createUUID(target.getUUID()));
					tag.putBoolean(SeekerCompassItem.DISABLED_USE_FOR_TICK, true);

					player.awardStat(Stats.ITEM_USED.get(itemstack.getItem()));
					player.swing(hand);
					player.level().playSound(null, target.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.75F, 0.25F);
					spawnSeekerParticles(targetPosition, rand, target, dimension, false);
				}
			}
		}
	}

	@SubscribeEvent
	public static void onEntitySpawned(MobSpawnEvent.FinalizeSpawn event) {
		double compassChance = SCConfig.COMMON.zombifiedPiglinCompassChance;
		if (compassChance <= 0.0F) return;
		if (event.getEntity() instanceof ZombifiedPiglin piglin && piglin.getItemBySlot(EquipmentSlot.OFFHAND).isEmpty() && piglin.getRandom().nextFloat() <= compassChance) {
			piglin.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(SeekerCompass.SEEKER_COMPASS.get()));
			piglin.setDropChance(EquipmentSlot.OFFHAND, 2.0F);
		}
	}

	@SubscribeEvent
	public static void onEntityTick(LivingEvent.LivingTickEvent event) {
		LivingEntity entity = event.getEntity();
		if (entity instanceof Player) return;
		ChunkPos chunkpos = new ChunkPos(entity.blockPosition());
		CompoundTag tag = entity.getPersistentData();
		if (!(entity.level() instanceof ServerLevel level)) return;
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

	public static void spawnSeekerParticles(Vec3 targetPosition, RandomSource rand, Entity target, ResourceKey<Level> dimension, boolean invert) {
		for (int i = 0; i < 8; i++) {
			Vec3 position = targetPosition.add((rand.nextBoolean() ? -rand.nextFloat() : rand.nextFloat()) * 1.25F, target.getEyeHeight(), (rand.nextBoolean() ? -rand.nextFloat() : rand.nextFloat()) * 1.25F);
			Vec3 motion = position.subtract(targetPosition.add(0.0F, target.getEyeHeight() * 0.35F, 0.0F)).scale(invert ? 0.5F : -0.5F);
			NetworkUtil.spawnParticle(SCParticles.SEEKER_EYES.getId().toString(), dimension, position.x(), position.y(), position.z(), motion.x(), motion.y(), motion.z());
		}
	}

	/*
	 * Checks if the chunk (chunk to be unloaded) is a spawn chunk or forced already by the force chunk command
	 */
	public static boolean isChunkForced(ServerLevel level, ChunkPos pos) {
		if (level.getForcedChunks().contains(pos.toLong())) return true;
		var levelData = level.getLevelData();
		ChunkPos spawnChunk = new ChunkPos(new BlockPos(levelData.getXSpawn(), 0, levelData.getZSpawn()));
		Stream<ChunkPos> spawnChunks = ChunkPos.rangeClosed(spawnChunk, 11);
		return spawnChunks.anyMatch(chunk -> chunk.equals(pos));
	}
}