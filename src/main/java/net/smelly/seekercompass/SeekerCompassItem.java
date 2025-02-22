package net.smelly.seekercompass;

import java.util.List;

import javax.annotation.Nullable;

import com.teamabnormals.blueprint.core.util.NetworkUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.smelly.seekercompass.advancements.SCCriteriaTriggers;
import net.smelly.seekercompass.enchants.SCEnchants;
import net.smelly.seekercompass.interfaces.Stalker;
import net.smelly.seekercompass.particles.SCParticles;

/**
 * @author SmellyModder (Luke Tonon)
 */
public class SeekerCompassItem extends Item {
	private static final String VOODOO_TAG = "Voodoo";
	public static final String TRACKING_TAG = "TrackingEntity";
	public static final String ENTITY_TAG = "EntityStatus";
	public static final String TRACKING_ONLY = "TrackingOnly";
	public static final String DISABLED_USE_FOR_TICK = "DisabledUseForTick";

	public SeekerCompassItem(Properties properties) {
		super(properties);
	}

	@Override
	public void inventoryTick(ItemStack stack, Level world, Entity entity, int itemSlot, boolean isSelected) {
		if (!world.isClientSide && isNotBroken(stack)) {
			CompoundTag tag = stack.getTag();
			if (tag != null) {
				if (tag.contains(DISABLED_USE_FOR_TICK)) tag.remove(DISABLED_USE_FOR_TICK);

				if (tag.contains(VOODOO_TAG, 10)) {
					VoodooData data = getVoodooData(tag);
					if (data.timer > 0) {
						tag.put(VOODOO_TAG, VoodooData.write(new VoodooData(data.timesUsed, data.timer - 1)));
					} else if (data.timesUsed >= 9) {
						tag.put(VOODOO_TAG, VoodooData.write(new VoodooData(0, 0)));
					}
				}

				if (tag.contains(TRACKING_TAG)) {
					Entity trackingEntity = this.getEntity((ServerLevel) world, stack);
					if (trackingEntity != null) {
						if (entity instanceof ServerPlayer) {
							int damage = 1;
							Stalker stalker = (Stalker) entity;
							if (EnchantmentHelper.getTagEnchantmentLevel(SCEnchants.STALKING.get(), stack) > 0) {
								if (stalker.getStalkingEntity() == trackingEntity) {
									stalker.setShouldBeStalking(true);
									damage = 10;
								}
							}
							if (world.getGameTime() % 20 == 0 && stack.isDamageableItem()) {
								ServerPlayer player = (ServerPlayer) entity;
								if (!player.getAbilities().instabuild) {
									int maxDamage = stack.getMaxDamage() - 1;
									stack.hurt(damage, player.getRandom(), player);
									stack.setDamageValue(Mth.clamp(stack.getDamageValue(), 0, maxDamage));
									if (stack.getDamageValue() == maxDamage) {
										player.playNotifySound(SoundEvents.SHIELD_BREAK, SoundSource.PLAYERS, 0.5F, 1.5F);
									}
								}
							}
						}

						tag.put(ENTITY_TAG, EntityStatusData.write(trackingEntity));

						CompoundTag persistantData = trackingEntity.getPersistentData();
						persistantData.putBoolean(SCEvents.TAG_CHUNK_UPDATE, true);
						persistantData.putInt(SCEvents.TAG_CHUNK_TIMER, 20);

						if (EnchantmentHelper.getTagEnchantmentLevel(SCEnchants.PERSISTENCE.get(), stack) > 0 && trackingEntity instanceof Mob) {
							((Mob) trackingEntity).setPersistenceRequired();
						}
					} else if (tag.contains(ENTITY_TAG, 10)) {
						EntityStatusData data = EntityStatusData.read(tag.getCompound(ENTITY_TAG));
						ChunkPos chunkpos = new ChunkPos(data.pos);
						if (!SCEvents.isChunkForced((ServerLevel) world, chunkpos)) {
							world.getChunkSource().updateChunkForced(chunkpos, false);
						}

						tag.put(ENTITY_TAG, EntityStatusData.writeMissingEntity(data));
					}
				}
			}
		}
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> components, TooltipFlag tooltipFlag) {
		CompoundTag tag = stack.getTag();
		if (SeekerCompassItem.isNotBroken(stack) && tag != null && !tag.getBoolean(TRACKING_ONLY) && tag.contains(TRACKING_TAG) && tag.contains(ENTITY_TAG, 10)) {
			EntityStatusData status = EntityStatusData.read(tag.getCompound(ENTITY_TAG));

			components.add(Component.translatable("tooltip.seeker_compass.tracking_entity"));

			components.add((Component.translatable("tooltip.seeker_compass.entity_type").withStyle(ChatFormatting.GRAY)).append(Component.translatable(status.entityType)));
			components.add((Component.translatable("tooltip.seeker_compass.entity_name").withStyle(ChatFormatting.GRAY)).append(Component.literal(status.entityName)));

			boolean alive = status.isAlive;
			ChatFormatting color = alive ? ChatFormatting.GREEN : ChatFormatting.RED;
			String aliveString = String.valueOf(alive);
			aliveString = aliveString.substring(0, 1).toUpperCase() + aliveString.substring(1).toLowerCase();

			components.add((Component.translatable("tooltip.seeker_compass.alive").withStyle(ChatFormatting.GRAY)).append(Component.literal(aliveString).withStyle(color)));

			components.add(Component.translatable("tooltip.seeker_compass.health").withStyle(ChatFormatting.GRAY).append(Component.literal(String.valueOf(status.health)).withStyle(ChatFormatting.GREEN)));

			if (EnchantmentHelper.getTagEnchantmentLevel(SCEnchants.TRACKING.get(), stack) > 0) {
				components.add((Component.translatable("tooltip.seeker_compass.blockpos").withStyle(ChatFormatting.GRAY)).append(Component.literal(status.pos.toShortString())));
				if (level != null) {
					components.add((Component.translatable("tooltip.seeker_compass.standing_on").withStyle(ChatFormatting.GRAY)).append(Component.translatable(level.getBlockState(status.pos.below()).getBlock().getDescriptionId())));
					var biomeKey = level.getBiome(status.pos).unwrapKey();
					components.add(Component.translatable("tooltip.seeker_compass.biome").withStyle(ChatFormatting.GRAY).append(Component.translatable(biomeKey.map(biomeResourceKey -> "biome." + biomeResourceKey.location().getNamespace() + "." + biomeResourceKey.location().getPath()).orElse("Unknown"))));
				}
			}
		}
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		CompoundTag tag = stack.getTag();
		boolean hasTag = tag != null;
		if (hasTag && (tag.getBoolean(TRACKING_ONLY) || tag.getBoolean(DISABLED_USE_FOR_TICK))) return InteractionResultHolder.fail(stack);
		if (isNotBroken(stack) && hasTag && tag.contains(TRACKING_TAG)) {
			int voodooLevel = EnchantmentHelper.getTagEnchantmentLevel(SCEnchants.VOODOO.get(), stack);
			if (voodooLevel > 0) {
				if (tag.contains(VOODOO_TAG, 10) && getVoodooData(tag).timer > 0 && !player.isCreative()) {
					if (!level.isClientSide) {
						player.sendSystemMessage(Component.translatable("message.seeker_compass.voodoo_cooldown").append(Component.literal(String.valueOf(getVoodooData(tag).timer)).withStyle(ChatFormatting.GOLD)));
					}
					return InteractionResultHolder.fail(stack);
				}

				if (level instanceof ServerLevel) {
					Entity entity = this.getEntity((ServerLevel) level, stack);
					if (entity != null && entity.hurt(level.damageSources().indirectMagic(player, null), 1.5F + voodooLevel)) {
						SCCriteriaTriggers.VOODOO_MAGIC.trigger((ServerPlayer) player);

						SCEvents.spawnSeekerParticles(entity.position(), player.getRandom(), entity, level.dimension(), false);

						if (!player.isCreative()) {
							int damage = Mth.clamp(stack.getDamageValue() + 400, 0, stack.getMaxDamage() - 1);
							stack.setDamageValue(damage);

							if (damage == stack.getMaxDamage() - 1) {
								player.playNotifySound(SoundEvents.SHIELD_BREAK, SoundSource.PLAYERS, 0.5F, 1.5F);
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
				return InteractionResultHolder.fail(stack);
			} else if (EnchantmentHelper.getTagEnchantmentLevel(SCEnchants.WARPING.get(), stack) > 0) {
				if (level instanceof ServerLevel) {
					Entity entity = this.getEntity((ServerLevel) level, stack);

					if (entity != null) {
						Vec3 pos = entity.position();
						double x = pos.x();
						double y = pos.y();
						double z = pos.z();

						if (player.randomTeleport(x, y, z, false)) {
							player.fallDistance = 0.0F;
							level.playSound(null, x, y, z, SoundEvents.SHULKER_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
							NetworkUtil.spawnParticle(SCParticles.SEEKER_WARP.getId().toString(), player.getX(), player.getY(), player.getZ(), 0.0F, 0.0F, 0.0F);

							if (!player.isCreative()) {
								if (player.getRandom().nextFloat() < 0.25F) {
									stack.shrink(1);
								}
								stack.setDamageValue(stack.getMaxDamage());
							}

							return InteractionResultHolder.success(stack);
						}
					}
				}
			} else if (EnchantmentHelper.getTagEnchantmentLevel(SCEnchants.STALKING.get(), stack) > 0) {
				if (level instanceof ServerLevel serverLevel) {
					Entity entity = this.getEntity(serverLevel, stack);
					if (entity instanceof LivingEntity) {
						Stalker stalker = (Stalker) player;
						if (stalker.isStalking()) {
							stalker.setStalkingEntity(null);
						} else {
							stalker.setStalkingEntity((LivingEntity) entity);
						}
					}
				}
			}
		}
		return super.use(level, player, hand);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		ItemStack stack = context.getItemInHand();
		CompoundTag tag = stack.getTag();
		boolean hasTag = tag != null;
		if (hasTag && tag.getBoolean(TRACKING_ONLY)) return InteractionResult.FAIL;
		Player player = context.getPlayer();
		if (player == null) return InteractionResult.FAIL;
		Level level = context.getLevel();
		BlockPos placingPos = context.getClickedPos().above();
		if (isNotBroken(stack) && EnchantmentHelper.getTagEnchantmentLevel(SCEnchants.SUMMONING.get(), stack) > 0 && hasTag && tag.contains(TRACKING_TAG)) {
			if (level instanceof ServerLevel) {
				Entity trackedEntity = this.getEntity((ServerLevel) level, stack);
				if (trackedEntity instanceof TamableAnimal || trackedEntity.getType().is(SCTags.EntityTypeTags.SUMMONABLES)) {
					if (((LivingEntity) trackedEntity).randomTeleport(placingPos.getX() + 0.5F, placingPos.getY(), placingPos.getZ() + 0.5F, false)) {
						level.playSound(null, placingPos.getX(), placingPos.getY(), placingPos.getZ(), SoundEvents.SHULKER_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
						NetworkUtil.spawnParticle("seeker_compass:seeker_warp", trackedEntity.getX(), trackedEntity.getY(), trackedEntity.getZ(), 0.0F, 0.0F, 0.0F);
						if (!player.isCreative()) {
							int damage = Mth.clamp(stack.getDamageValue() + 300, 0, stack.getMaxDamage() - 1);
							stack.setDamageValue(damage);

							if (damage == stack.getMaxDamage() - 1) {
								player.playNotifySound(SoundEvents.SHIELD_BREAK, SoundSource.PLAYERS, 0.5F, 1.5F);
							}
						}
						return InteractionResult.SUCCESS;
					}
				}
			}
		} else {
			if (tag == null || !tag.contains(TRACKING_TAG)) {
				boolean creative = player.isCreative();
				if (level.getBlockState(placingPos.below()).is(Blocks.CRYING_OBSIDIAN) && (player.experienceLevel >= 10 || creative)) {
					if (!creative) {
						player.experienceLevel -= 10;
					}

					level.playSound(null, placingPos.getX(), placingPos.getY(), placingPos.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.75F, 1.0F);

					if (level instanceof ServerLevel serverLevel) {
						SCCriteriaTriggers.JOHN_CENA.trigger((ServerPlayer) player);
						for (ServerPlayer players : serverLevel.players()) {
							var inventory = players.getInventory();
							boolean unbinded = false;
							for (int i = 0; i < inventory.getContainerSize(); i++) {
								ItemStack itemstack = inventory.getItem(i);
								if (itemstack.getItem() != this) continue;
								CompoundTag otherTag = itemstack.getTag();
								if (otherTag == null || !otherTag.contains(TRACKING_TAG)) continue;
								if (player == this.getEntity(serverLevel, itemstack)) {
									otherTag.remove(TRACKING_TAG);
									otherTag.remove(ENTITY_TAG);
									unbinded = true;
								}
							}
							if (unbinded) {
								SCEvents.spawnSeekerParticles(players.position(), players.getRandom(), players, level.dimension(), true);
								level.playSound(null, players.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.75F, 1.5F);
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

	public static boolean isNotBroken(ItemStack stack) {
		return stack.getDamageValue() < stack.getMaxDamage() - 1;
	}

	private Entity getEntity(ServerLevel level, ItemStack stack) {
		return level.getEntity(stack.getOrCreateTag().getUUID(TRACKING_TAG));
	}

	private static VoodooData getVoodooData(CompoundTag tag) {
		return VoodooData.read(tag.getCompound(VOODOO_TAG));
	}

	record EntityStatusData(boolean isAlive, float health, String entityType, String entityName, BlockPos pos) {
		public static EntityStatusData read(CompoundTag compound) {
			return new EntityStatusData(compound.getBoolean("Alive"), compound.getFloat("Health"), compound.getString("EntityType"), compound.getString("EntityName"), NbtUtils.readBlockPos(compound.getCompound("Pos")));
		}

		public static CompoundTag write(Entity trackingEntity) {
			CompoundTag tag = new CompoundTag();
			tag.putBoolean("Alive", trackingEntity.isAlive());
			tag.putString("EntityType", trackingEntity.getType().getDescriptionId());
			tag.putString("EntityName", trackingEntity.getName().getString());

			if (trackingEntity instanceof LivingEntity) {
				tag.putFloat("Health", ((LivingEntity) trackingEntity).getHealth());
			}
			tag.put("Pos", NbtUtils.writeBlockPos(trackingEntity.blockPosition()));
			return tag;
		}

		public static CompoundTag writeMissingEntity(EntityStatusData status) {
			CompoundTag tag = new CompoundTag();
			tag.putBoolean("Alive", false);
			tag.putString("EntityType", status.entityType);
			tag.putString("EntityName", status.entityName);
			tag.putFloat("Health", 0.0F);
			tag.put("Pos", NbtUtils.writeBlockPos(status.pos));
			return tag;
		}
	}

	record VoodooData(int timesUsed, int timer) {
		public static VoodooData read(CompoundTag compound) {
			return new VoodooData(compound.getInt("TimesUsed"), compound.getInt("Timer"));
		}

		public static CompoundTag write(VoodooData data) {
			CompoundTag tag = new CompoundTag();
			tag.putInt("TimesUsed", data.timesUsed);
			tag.putInt("Timer", data.timer);
			return tag;
		}
	}
}