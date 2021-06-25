package net.smelly.seekercompass;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import com.mojang.datafixers.util.Pair;

import net.minecraft.block.Blocks;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.PacketDistributor;
import net.smelly.seekercompass.advancements.SCCriteriaTriggers;
import net.smelly.seekercompass.enchants.SCEnchants;

/**
 * @author SmellyModder(Luke Tonon)
 */
public class SeekerCompassItem extends Item {
	private static final String VOODOO_TAG = "Voodoo";
	private static final String TRACKING_TAG = "TrackingEntity";
	private static final String ENTITY_TAG = "EntityStatus";
	private static final String ROTATIONS_TAG = "Rotations";
	private static final String TRACKING_ONLY = "TrackingOnly";
	
	public SeekerCompassItem(Properties properties) {
		super(properties);
	}

	@Override
	public void inventoryTick(ItemStack stack, World world, Entity entity, int itemSlot, boolean isSelected) {
		if (!world.isClientSide && isNotBroken(stack)) {
			CompoundNBT tag = stack.getTag();
			
			if (world.getGameTime() % 20 == 0 && entity instanceof PlayerEntity && tag != null && tag.contains(TRACKING_TAG)) {
				PlayerEntity player = (PlayerEntity) entity;
				stack.hurtAndBreak(1, player, (living) -> living.broadcastBreakEvent(player.getUsedItemHand()));
			}
			
			if (tag != null && tag.contains(VOODOO_TAG)) {
				VoodooData data = getVoodooData(tag);
				if (data.timer > 0) {
					tag.put(VOODOO_TAG, VoodooData.write(new VoodooData(data.timesUsed, data.timer - 1)));
				} else {
					if (data.timesUsed >= 9) {
						tag.put(VOODOO_TAG, VoodooData.write(new VoodooData(0, 0)));
					}
				}
			}
			
			if (tag != null && tag.contains(TRACKING_TAG)) {
				Entity trackingEntity = this.getEntity((ServerWorld) world, stack);
				if (trackingEntity != null) {
					tag.put(ENTITY_TAG, EntityStatusData.write(trackingEntity));
					
					CompoundNBT persistantData = trackingEntity.getPersistentData();
					persistantData.putBoolean(SCEvents.TAG_CHUNK_UPDATE, true);
					persistantData.putInt(SCEvents.TAG_CHUNK_TIMER, 20);
					
					if (EnchantmentHelper.getItemEnchantmentLevel(SCEnchants.PERSISTENCE.get(), stack) > 0 && trackingEntity instanceof MobEntity) {
						((MobEntity) trackingEntity).setPersistenceRequired();
					}
				} else if(tag.contains(ENTITY_TAG)) {
					EntityStatusData data = EntityStatusData.read(tag.getCompound(ENTITY_TAG));
					ChunkPos chunkpos = new ChunkPos(data.pos);
					if (!SCEvents.isChunkForced((ServerWorld) world, chunkpos)) {
						world.getChunkSource().updateChunkForced(chunkpos, false);
					}
					
					tag.put(ENTITY_TAG, EntityStatusData.writeMissingEntity(data));
				}
				
				if (tag.contains(ROTATIONS_TAG)) {
					RotationData rotations = RotationData.read(tag.getCompound(ROTATIONS_TAG));
					
					double turn;
					if (tag.contains(ENTITY_TAG)) {
						double yaw = entity.yRot;
						yaw = positiveModulo(yaw / 360.0D, 1.0D);
						double angle = this.getAngleToTrackedEntity(stack, entity) / (double) ((float)Math.PI * 2F);
						turn = 0.5D - (yaw - 0.25D - angle);
					} else {
						turn = Math.random();
					}
					
					Pair<Long, double[]> rotationData = this.wobble(world, turn, rotations.lastUpdateTick, rotations.rotation, rotations.rota);
					rotations = new RotationData(rotationData.getSecond()[0], rotationData.getSecond()[1], rotationData.getFirst());
					
					tag.put(ROTATIONS_TAG, RotationData.write(rotations));
				} else {
					RotationData rotations = new RotationData(0.0F, 0.0F, 0L);
					
					double turn;
					if (tag.contains(ENTITY_TAG)) {
						double yaw = entity.yRot;
						yaw = positiveModulo(yaw / 360.0D, 1.0D);
						double angle = this.getAngleToTrackedEntity(stack, entity) / (double) ((float)Math.PI * 2F);
						turn = 0.5D - (yaw - 0.25D - angle);
					} else {
						turn = Math.random();
					}
					
					Pair<Long, double[]> rotationData = this.wobble(world, turn, rotations.lastUpdateTick, rotations.rotation, rotations.rota);
					rotations = new RotationData(rotationData.getSecond()[0], rotationData.getSecond()[1], rotationData.getFirst());
					
					tag.put(ROTATIONS_TAG, RotationData.write(rotations));
				}
			}
		}
	}
	
	@OnlyIn(Dist.CLIENT)
	@Override
	public void appendHoverText(ItemStack stack, World world, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
		CompoundNBT tag = stack.getTag();
		if (SeekerCompassItem.isNotBroken(stack) && tag != null && !tag.getBoolean(TRACKING_ONLY) && tag.contains(TRACKING_TAG) && tag.contains(ENTITY_TAG)) {
			EntityStatusData status = EntityStatusData.read(tag.getCompound(ENTITY_TAG));
			
			tooltip.add(new TranslationTextComponent("tooltip.seeker_compass.tracking_entity"));
			
			tooltip.add((new TranslationTextComponent("tooltip.seeker_compass.entity_type").withStyle(TextFormatting.GRAY)).append(new StringTextComponent(I18n.get(status.entityType))));
			tooltip.add((new TranslationTextComponent("tooltip.seeker_compass.entity_name").withStyle(TextFormatting.GRAY)).append(new StringTextComponent(status.entityName)));
			
			boolean alive = status.isAlive;
			TextFormatting color = alive ? TextFormatting.GREEN : TextFormatting.RED;
			String aliveString = String.valueOf(alive); 
			aliveString = aliveString.substring(0,1).toUpperCase() + aliveString.substring(1).toLowerCase();
			
			tooltip.add((new TranslationTextComponent("tooltip.seeker_compass.alive").withStyle(TextFormatting.GRAY)).append(new StringTextComponent(aliveString).withStyle(color)));
			
			tooltip.add((new TranslationTextComponent("tooltip.seeker_compass.health").withStyle(TextFormatting.GRAY)).append(new StringTextComponent(String.valueOf(status.health)).withStyle(TextFormatting.GREEN)));
		
			if (EnchantmentHelper.getItemEnchantmentLevel(SCEnchants.TRACKING.get(), stack) > 0) {
				tooltip.add((new TranslationTextComponent("tooltip.seeker_compass.blockpos").withStyle(TextFormatting.GRAY)).append(new StringTextComponent(status.pos.toShortString())));
				tooltip.add((new TranslationTextComponent("tooltip.seeker_compass.standing_on").withStyle(TextFormatting.GRAY)).append(new StringTextComponent(I18n.get(world.getBlockState(status.pos.below()).getBlock().getDescriptionId()))));
				ResourceLocation biomeKey = world.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY).getKey(world.getBiome(status.pos));
				tooltip.add(new TranslationTextComponent("tooltip.seeker_compass.biome").withStyle(TextFormatting.GRAY).append(new TranslationTextComponent(biomeKey != null ? "biome." + biomeKey.getNamespace() + "." + biomeKey.getPath() : "Unknown")));
			}
		}
	}
	
	@Override
	public ActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
		ItemStack stack = player.getItemInHand(hand);
		CompoundNBT tag = stack.getTag();
		boolean hasTag = tag != null;
		if (hasTag && tag.getBoolean(TRACKING_ONLY)) return ActionResult.fail(stack);
		if (isNotBroken(stack) && hasTag && tag.contains(TRACKING_TAG)) {
			int level = EnchantmentHelper.getItemEnchantmentLevel(SCEnchants.VOODOO.get(), stack);
			if (level > 0 && !getTargetEntity(player, 8).isPresent()) {
				if (tag.contains(VOODOO_TAG) && getVoodooData(tag).timer > 0 && !player.isCreative()) {
					if (!world.isClientSide) {
						player.sendMessage(new TranslationTextComponent("message.seeker_compass.voodoo_cooldown").append((new StringTextComponent(String.valueOf(getVoodooData(tag).timer)).withStyle(TextFormatting.GOLD))), player.getUUID());
					}
					return ActionResult.fail(stack);
				}
				
				if (world instanceof ServerWorld) {
					Entity entity = this.getEntity((ServerWorld) world, stack);
					if (entity != null && entity.hurt(DamageSource.playerAttack(player).bypassArmor().setMagic(), 1.5F + level)) {
						SCCriteriaTriggers.VOODOO_MAGIC.trigger((ServerPlayerEntity) player);
							
						Random rand = player.getRandom();
						Vector3d targetPosition = entity.position();
						for (int i = 0; i < 8; i++) {
							Vector3d position = targetPosition.add(rand.nextBoolean() ? -rand.nextFloat() : rand.nextFloat() * 1.25F, entity.getEyeHeight(), rand.nextBoolean() ? -rand.nextFloat() : rand.nextFloat() * 1.25F);
							Vector3d motion = position.subtract(targetPosition.add(0.0F, entity.getEyeHeight() * 0.35F, 0.0F)).scale(-0.5F);
							SeekerCompass.CHANNEL.send(PacketDistributor.ALL.with(() -> null), new MessageS2CParticle("seeker_compass:seeker_eyes", position.x(), position.y(), position.z(), motion.x(), motion.y(), motion.z()));
						}

						if (!player.isCreative()) {
							int damage = MathHelper.clamp(stack.getDamageValue() + 400, 0, stack.getMaxDamage() - 1);
							stack.setDamageValue(damage);

							if (damage == stack.getMaxDamage() - 1) {
								player.playNotifySound(SoundEvents.SHIELD_BREAK, SoundCategory.PLAYERS, 0.5F, 1.5F);
							}

							VoodooData data = getVoodooData(tag);
							int newTimesUsed = data.timesUsed + 1;
							if (newTimesUsed >= 9) {
								stack.getTag().put(VOODOO_TAG, VoodooData.write(new VoodooData(9, 12000)));
							} else {
								stack.getTag().put(VOODOO_TAG, VoodooData.write(new VoodooData(newTimesUsed, 0)));
							}
						}
					}
				}
				return ActionResult.consume(stack);
			} else if (EnchantmentHelper.getItemEnchantmentLevel(SCEnchants.WARPING.get(), stack) > 0 && !getTargetEntity(player, 8).isPresent()) {
				if (world instanceof ServerWorld) {
					Entity entity = this.getEntity((ServerWorld) world, stack);

					if (entity != null) {
						Vector3d pos = entity.position();
						double x = pos.x();
						double y = pos.y();
						double z = pos.z();

						if (player.randomTeleport(x, y, z, false)) {
							player.fallDistance = 0.0F;
							world.playSound(null, x, y, z, SoundEvents.SHULKER_TELEPORT, SoundCategory.PLAYERS, 1.0F, 1.0F);
							SeekerCompass.CHANNEL.send(PacketDistributor.ALL.with(() -> null), new MessageS2CParticle("seeker_compass:seeker_warp", player.getX(), player.getY(), player.getZ(), 0.0F, 0.0F, 0.0F));

							if (!player.isCreative()) {
								if (player.getRandom().nextFloat() < 0.25F) {
									stack.shrink(1);
								}
								stack.setDamageValue(1200);
							}

							return ActionResult.success(stack);
						}
					}
				}
			}
		}
		return super.use(world, player, hand);
	}

	@Override
	public ActionResultType useOn(ItemUseContext context) {
		ItemStack stack = context.getItemInHand();
		CompoundNBT tag = stack.getTag();
		boolean hasTag = tag != null;
		if (hasTag && tag.getBoolean(TRACKING_ONLY)) return ActionResultType.FAIL;
		PlayerEntity player = context.getPlayer();
		World world = context.getLevel();
		BlockPos placingPos = context.getClickedPos().above();
		if (isNotBroken(stack) && EnchantmentHelper.getItemEnchantmentLevel(SCEnchants.SUMMONING.get(), stack) > 0 && hasTag && tag.contains(TRACKING_TAG)) {
			if (world instanceof ServerWorld) {
				Entity trackedEntity = this.getEntity((ServerWorld) world, stack);
				if (trackedEntity instanceof TameableEntity || SCTags.EntityTags.SUMMONABLES.contains(trackedEntity.getType())) {
					if (((LivingEntity) trackedEntity).randomTeleport(placingPos.getX() + 0.5F, placingPos.getY(), placingPos.getZ() + 0.5F, false)) {
						world.playSound(null, placingPos.getX(), placingPos.getY(), placingPos.getZ(), SoundEvents.SHULKER_TELEPORT, SoundCategory.PLAYERS, 1.0F, 1.0F);
						SeekerCompass.CHANNEL.send(PacketDistributor.ALL.with(() -> null), new MessageS2CParticle("seeker_compass:seeker_warp", trackedEntity.getX(), trackedEntity.getY(), trackedEntity.getZ(), 0.0F, 0.0F, 0.0F));

						if (!player.isCreative()) {
							int damage = MathHelper.clamp(stack.getDamageValue() + 300, 0, stack.getMaxDamage() - 1);
							stack.setDamageValue(damage);

							if (damage == stack.getMaxDamage() - 1) {
								player.playNotifySound(SoundEvents.SHIELD_BREAK, SoundCategory.PLAYERS, 0.5F, 1.5F);
							}
						}
						return ActionResultType.SUCCESS;
					}
				}
			}
		} else {
			if (tag == null || !tag.contains(TRACKING_TAG)) {
				boolean creative = player.isCreative();
				if (world.getBlockState(placingPos.below()).getBlock() == Blocks.OBSIDIAN && (player.experienceLevel >= 10 || creative)) {
					if (!creative) {
						player.experienceLevel -= 10;
					}

					world.playSound(null, placingPos.getX(), placingPos.getY(), placingPos.getZ(), SoundEvents.PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.75F, 1.0F);

					if (world instanceof ServerWorld) {
						ServerWorld serverWorld = (ServerWorld) world;
						SCCriteriaTriggers.JOHN_CENA.trigger((ServerPlayerEntity) player);

						for (ServerPlayerEntity players : serverWorld.players()) {
							for (int i = 0; i < players.inventory.getContainerSize(); i++) {
								ItemStack itemstack = players.inventory.getItem(i);
								if (!itemstack.isEmpty() && itemstack.getItem() == this && itemstack.hasTag() && tag.contains(TRACKING_TAG) && player == this.getEntity(serverWorld, itemstack)) {
									tag.remove(TRACKING_TAG);
									tag.remove(ENTITY_TAG);
									tag.remove(ROTATIONS_TAG);

									Random rand = player.getRandom();
									Vector3d targetPosition = players.position();
									for (int i2 = 0; i2 < 8; i2++) {
										Vector3d position = targetPosition.add(rand.nextBoolean() ? -rand.nextFloat() : rand.nextFloat() * 1.25F, players.getEyeHeight(), rand.nextBoolean() ? -rand.nextFloat() : rand.nextFloat() * 1.25F);
										Vector3d motion = targetPosition.subtract(position.add(0.0F, players.getEyeHeight() * 0.35F, 0.0F)).scale(-0.5F);
										SeekerCompass.CHANNEL.send(PacketDistributor.ALL.with(() -> null), new MessageS2CParticle("seeker_compass:seeker_eyes", targetPosition.x(), targetPosition.y(), targetPosition.z(), motion.x(), motion.y(), motion.z()));
									}

									player.level.playSound(null, players.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 0.75F, 1.5F);
								}
							}
						}
					}
					stack.shrink(1);
				}
			}
		}
		return super.useOn(context);
	}

	@Override
	public boolean isEnchantable(ItemStack stack) {
		return !stack.hasTag() || !stack.getTag().getBoolean(TRACKING_ONLY);
	}

	@Override
	public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
		return false;
	}

	@Override
	public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
		return repair.getItem() == Items.MAGMA_CREAM;
	}

	@Override
	public int getRGBDurabilityForDisplay(ItemStack stack) {
		return 16743936;
	}

	public static double positiveModulo(double numerator, double denominator) {
		return (numerator % denominator + denominator) % denominator;
	}

	public static boolean isNotBroken(ItemStack stack) {
		return stack.getDamageValue() < stack.getMaxDamage() - 1;
	}

	private Entity getEntity(ServerWorld world, ItemStack stack) {
		return world.getEntity(NBTUtil.loadUUID(stack.getTag().get(TRACKING_TAG)));
	}

	private static VoodooData getVoodooData(CompoundNBT tag) {
		return VoodooData.read((tag).getCompound(VOODOO_TAG));
	}

	private double getAngleToTrackedEntity(ItemStack stack, Entity entity) {
		EntityStatusData data = EntityStatusData.read(stack.getTag().getCompound(ENTITY_TAG));
		BlockPos pos = data.pos;
		return Math.atan2((double) pos.getZ() - entity.getZ(), (double) pos.getX() - entity.getX());
	}
	
	private Pair<Long, double[]> wobble(World world, double angle, long lastUpdateTickIn, double rotationIn, double rotaIn) {
		long lastUpdateTick = lastUpdateTickIn;
		double rotation = rotationIn;
		double rota = rotaIn;
		
		if(world.getGameTime() != lastUpdateTick) {
			lastUpdateTick = world.getGameTime();
			double d0 = angle - rotation;
			d0 = positiveModulo(d0 + 0.5D, 1.0D) - 0.5D;
			rota += d0 * 0.1D;
			rota *= 0.8D;
			rotation = positiveModulo(rotation + rota, 1.0D);
		}

		return Pair.of(lastUpdateTick, new double[] {rotation, rota});
	}
	
	private static Optional<Entity> getTargetEntity(Entity entityIn, int distance) {
		Vector3d Vector3d = entityIn.getEyePosition(1.0F);
		Vector3d Vector3d1 = entityIn.getViewVector(1.0F).scale(distance);
		Vector3d Vector3d2 = Vector3d.add(Vector3d1);
		AxisAlignedBB axisalignedbb = entityIn.getBoundingBox().expandTowards(Vector3d1).inflate(1.0D);
		int i = distance * distance;
		Predicate<Entity> predicate = (p_217727_0_) -> !p_217727_0_.isSpectator() && p_217727_0_.canBeCollidedWith();
		EntityRayTraceResult entityraytraceresult = rayTraceEntities(entityIn, Vector3d, Vector3d2, axisalignedbb, predicate, i);
		if (entityraytraceresult == null) {
			return Optional.empty();
		} else {
			return Vector3d.distanceToSqr(entityraytraceresult.getLocation()) > (double)i ? Optional.empty() : Optional.of(entityraytraceresult.getEntity());
		}
	}
	
	@Nullable
	private static EntityRayTraceResult rayTraceEntities(Entity player, Vector3d p_221273_1_, Vector3d p_221273_2_, AxisAlignedBB p_221273_3_, Predicate<Entity> p_221273_4_, double p_221273_5_) {
		World world = player.level;
		double d0 = p_221273_5_;
		Entity entity = null;
		Vector3d vector3d = null;

		for (Entity entity1 : world.getEntities(player, p_221273_3_)) {
			AxisAlignedBB axisalignedbb = entity1.getBoundingBox().inflate(entity1.getPickRadius());
			Optional<Vector3d> optional = axisalignedbb.clip(p_221273_1_, p_221273_2_);
			if (axisalignedbb.contains(p_221273_1_)) {
				if (d0 >= 0.0D) {
					entity = entity1;
					vector3d = optional.orElse(p_221273_1_);
					d0 = 0.0D;
				}
			} else if (optional.isPresent()) {
				Vector3d vector3d1 = optional.get();
				double d1 = p_221273_1_.distanceToSqr(vector3d1);
				if (d1 < d0 || d0 == 0.0D) {
					if (entity1.getRootVehicle() == player.getRootVehicle() && !entity1.canRiderInteract()) {
						if (d0 == 0.0D) {
							entity = entity1;
							vector3d = vector3d1;
						}
					} else {
						entity = entity1;
						vector3d = vector3d1;
						d0 = d1;
					}
				}
			}
		}

		return entity == null ? null : new EntityRayTraceResult(entity, vector3d);
	}

	static class EntityStatusData {
		public final boolean isAlive;
		public final float health;
		public final String entityType;
		public final String entityName;
		public final BlockPos pos;
		
		public EntityStatusData(boolean isAlive, float health, String entityType, String entityName, BlockPos pos) {
			this.isAlive = isAlive;
			this.health = health;
			this.entityType = entityType;
			this.entityName = entityName;
			this.pos = pos;
		}
		
		public static EntityStatusData read(CompoundNBT compound) {
			return new EntityStatusData(compound.getBoolean("Alive"), compound.getFloat("Health"), compound.getString("EntityType"), compound.getString("EntityName"), NBTUtil.readBlockPos(compound.getCompound("Pos")));
		}
		
		public static CompoundNBT write(Entity trackingEntity) {
			CompoundNBT tag = new CompoundNBT();
			tag.putBoolean("Alive", trackingEntity.isAlive());
			tag.putString("EntityType", trackingEntity.getType().getDescriptionId());
			tag.putString("EntityName", trackingEntity.getName().getString());
			
			if (trackingEntity instanceof LivingEntity) {
				tag.putFloat("Health", ((LivingEntity) trackingEntity).getHealth());
			}
			tag.put("Pos", NBTUtil.writeBlockPos(trackingEntity.blockPosition()));
			return tag;
		}
		
		public static CompoundNBT writeMissingEntity(EntityStatusData status) {
			CompoundNBT tag = new CompoundNBT();
			tag.putBoolean("Alive", false);
			tag.putString("EntityType", status.entityType);
			tag.putString("EntityName", status.entityName);
			tag.putFloat("Health", 0.0F);
			tag.put("Pos", NBTUtil.writeBlockPos(status.pos));
			return tag;
		}
	}
	
	static class RotationData {
		public final double rotation;
		private final double rota;
		private final long lastUpdateTick;
		
		public RotationData(double rotation, double rota, long lastUpdateTick) {
			this.rotation = rotation;
			this.rota = rota;
			this.lastUpdateTick = lastUpdateTick;
		}
		
		public static RotationData read(CompoundNBT compound) {
			return new RotationData(compound.getDouble("Rotation"), compound.getDouble("Rota"), compound.getLong("LastUpdateTick"));
		}
		
		public static CompoundNBT write(RotationData data) {
			CompoundNBT tag = new CompoundNBT();
			tag.putDouble("Rotation", data.rotation);
			tag.putDouble("Rota", data.rota);
			tag.putLong("LastUpdateTick", data.lastUpdateTick);
			return tag;
		}
	}
	
	static class VoodooData {
		public final int timesUsed;
		public final int timer;
		
		public VoodooData(int timesUsed, int timer) {
			this.timer = timer;
			this.timesUsed = timesUsed;
		}
		
		public static VoodooData read(CompoundNBT compound) {
			return new VoodooData(compound.getInt("TimesUsed"), compound.getInt("Timer"));
		}
		
		public static CompoundNBT write(VoodooData data) {
			CompoundNBT tag = new CompoundNBT();
			tag.putInt("TimesUsed", data.timesUsed);
			tag.putInt("Timer", data.timer);
			return tag;
		}
	}
}