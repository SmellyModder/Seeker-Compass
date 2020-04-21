package smelly.seekercompass;

import java.awt.Color;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import com.mojang.datafixers.util.Pair;

import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.IItemPropertyGetter;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceContext.FluidMode;
import net.minecraft.util.math.RayTraceResult.Type;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.PacketDistributor;
import smelly.seekercompass.enchants.SCEnchants;

public class SeekerCompassItem extends Item {

	public SeekerCompassItem(Properties properties) {
		super(properties);
		this.addPropertyOverride(new ResourceLocation("angle"), new IItemPropertyGetter() {
			@OnlyIn(Dist.CLIENT)
			private double rotation;
			@OnlyIn(Dist.CLIENT)
			private double rota;
			@OnlyIn(Dist.CLIENT)
			private long lastUpdateTick;
			
			@OnlyIn(Dist.CLIENT)
			public float call(ItemStack stack, @Nullable World world, @Nullable LivingEntity livingEntity) {
				if(!isNotBroken(stack)) {
					return 0.0F;
				} else {
					if(livingEntity == null && !stack.isOnItemFrame()) {
						return 0.484375F;
					} else {
						boolean flag = livingEntity != null;
						Entity entity = (Entity)(flag ? livingEntity : stack.getItemFrame());
						if(world == null) {
							world = entity.world;
						}
						
						if(stack.hasTag() && stack.getTag().contains("Rotations") && stack.getTag().contains("EntityStatus") && !stack.isOnItemFrame()) {
							return (float) MathHelper.positiveModulo(SeekerCompassItem.getRotationData(stack).rotation, 1.0F);
						} else {
							double randRotation = Math.random();
							
							if(flag) {
								randRotation = this.wobble(world, randRotation);
							}

							return MathHelper.positiveModulo((float) randRotation, 1.0F);
						}
					}
				}
			}
			
			@OnlyIn(Dist.CLIENT)
			private double wobble(World world, double rotation) {
				if(world.getGameTime() != this.lastUpdateTick) {
					this.lastUpdateTick = world.getGameTime();
					double d0 = rotation - this.rotation;
					d0 = MathHelper.positiveModulo(d0 + 0.5D, 1.0D) - 0.5D;
					this.rota += d0 * 0.1D;
					this.rota *= 0.8D;
					this.rotation = MathHelper.positiveModulo(this.rotation + this.rota, 1.0D);
				}

				return this.rotation;
			}
		});
		
		this.addPropertyOverride(new ResourceLocation("broken"), (stack, world, entity) -> {
			return isNotBroken(stack) ? 0.0F : 1.0F;
		});
	}
	
	public static boolean isNotBroken(ItemStack stack) {
		return stack.getDamage() < stack.getMaxDamage() - 1;
	}
	
	private Entity getEntity(ServerWorld world, ItemStack stack) {
		return world.getEntityByUuid(NBTUtil.readUniqueId((CompoundNBT) stack.getTag().get("TrackingEntity")));
	}
	
	private static EntityStatusData getEntityStatus(ItemStack stack) {
		return EntityStatusData.read((CompoundNBT) stack.getTag().get("EntityStatus"));
	}
	
	private static RotationData getRotationData(ItemStack stack) {
		return RotationData.read(stack.getTag().getCompound("Rotations"));
	}
	
	private double getAngleToTrackedEntity(ItemStack stack, Entity entity) {
		EntityStatusData data = EntityStatusData.read((CompoundNBT) stack.getTag().get("EntityStatus"));
		BlockPos pos = data.pos;
		return Math.atan2((double) pos.getZ() - entity.getPosZ(), (double) pos.getX() - entity.getPosX());
	}
	
	private Pair<Long, double[]> wobble(World world, double angle, long lastUpdateTickIn, double rotationIn, double rotaIn) {
		long lastUpdateTick = lastUpdateTickIn;
		double rotation = rotationIn;
		double rota = rotaIn;
		
		if(world.getGameTime() != lastUpdateTick) {
			lastUpdateTick = world.getGameTime();
			double d0 = angle - rotation;
			d0 = MathHelper.positiveModulo(d0 + 0.5D, 1.0D) - 0.5D;
			rota += d0 * 0.1D;
			rota *= 0.8D;
			rotation = MathHelper.positiveModulo(rotation + rota, 1.0D);
		}

		return Pair.of(lastUpdateTick, new double[] {rotation, rota});
	}
	
	@Override
	public void inventoryTick(ItemStack stack, World world, Entity entity, int itemSlot, boolean isSelected) {
		if(!world.isRemote && isNotBroken(stack)) {
			if(world.getGameTime() % 20 == 0 && entity instanceof PlayerEntity) {
				PlayerEntity player = (PlayerEntity) entity;
				stack.damageItem(1, player, (living) -> {
					living.sendBreakAnimation(player.getActiveHand());
				});
			}
			
			CompoundNBT tag = stack.getTag();
			if(tag != null && tag.contains("TrackingEntity")) {
				Entity trackingEntity = this.getEntity((ServerWorld) world, stack);
				if(trackingEntity != null) {
					tag.put("EntityStatus", EntityStatusData.write(trackingEntity));
				} else if(tag.contains("EntityStatus")) {
					tag.put("EntityStatus", EntityStatusData.writeMissingEntity(EntityStatusData.read((CompoundNBT) tag.get("EntityStatus"))));
				}
				
				if(tag.contains("Rotations")) {
					RotationData rotations = RotationData.read(tag.getCompound("Rotations"));
					
					double turn;
					if(stack.hasTag() && stack.getTag().contains("EntityStatus") && SeekerCompassItem.getEntityStatus(stack).canTrackEntity(entity.dimension)) {
						double yaw = (double) entity.rotationYaw;
						yaw = MathHelper.positiveModulo(yaw / 360.0D, 1.0D);
						double angle = this.getAngleToTrackedEntity(stack, entity) / (double) ((float)Math.PI * 2F);
						turn = 0.5D - (yaw - 0.25D - angle);
					} else {
						turn = Math.random();
					}
					
					Pair<Long, double[]> rotationData = this.wobble(world, turn, rotations.lastUpdateTick, rotations.rotation, rotations.rota);
					rotations = new RotationData(rotationData.getSecond()[0], rotationData.getSecond()[1], rotationData.getFirst());
					
					tag.put("Rotations", RotationData.write(rotations));
				} else {
					RotationData rotations = new RotationData(0.0F, 0.0F, 0L);
					
					double turn;
					if(stack.hasTag() && stack.getTag().contains("EntityStatus") && SeekerCompassItem.getEntityStatus(stack).canTrackEntity(entity.dimension)) {
						double yaw = (double) entity.rotationYaw;
						yaw = MathHelper.positiveModulo(yaw / 360.0D, 1.0D);
						double angle = this.getAngleToTrackedEntity(stack, entity) / (double) ((float)Math.PI * 2F);
						turn = 0.5D - (yaw - 0.25D - angle);
					} else {
						turn = Math.random();
					}
					
					Pair<Long, double[]> rotationData = this.wobble(world, turn, rotations.lastUpdateTick, rotations.rotation, rotations.rota);
					rotations = new RotationData(rotationData.getSecond()[0], rotationData.getSecond()[1], rotationData.getFirst());
					
					tag.put("Rotations", RotationData.write(rotations));
				}
			}
		}
	}
	
	@Override
	public void addInformation(ItemStack stack, World world, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
		CompoundNBT tag = stack.getTag();
		if(SeekerCompassItem.isNotBroken(stack) && tag != null && tag.contains("TrackingEntity") && tag.contains("EntityStatus")) {
			EntityStatusData status = EntityStatusData.read(tag.getCompound("EntityStatus"));
			
			tooltip.add(new TranslationTextComponent("tooltip.seeker_compass.tracking_entity"));
			
			tooltip.add((new TranslationTextComponent("tooltip.seeker_compass.entity_type").applyTextStyle(TextFormatting.GRAY)).appendSibling(new StringTextComponent(status.entityType)));
			tooltip.add((new TranslationTextComponent("tooltip.seeker_compass.entity_name").applyTextStyle(TextFormatting.GRAY)).appendSibling(new StringTextComponent(status.entityName)));
			
			boolean alive = status.isAlive;
			TextFormatting color = alive ? TextFormatting.GREEN : TextFormatting.RED;
			String aliveString = String.valueOf(alive); 
			aliveString = aliveString.substring(0,1).toUpperCase() + aliveString.substring(1).toLowerCase();
			
			tooltip.add((new TranslationTextComponent("tooltip.seeker_compass.alive").applyTextStyle(TextFormatting.GRAY)).appendSibling((new StringTextComponent(aliveString).applyTextStyle(color))));
			
			tooltip.add((new TranslationTextComponent("tooltip.seeker_compass.health").applyTextStyle(TextFormatting.GRAY)).appendSibling(new StringTextComponent(String.valueOf(status.health)).applyTextStyle(TextFormatting.GREEN)));
		
			if(EnchantmentHelper.getEnchantmentLevel(SCEnchants.TRACKING.get(), stack) > 0) {
				tooltip.add((new TranslationTextComponent("tooltip.seeker_compass.blockpos").applyTextStyle(TextFormatting.GRAY)).appendSibling(new StringTextComponent(status.pos.toString())));
				tooltip.add((new TranslationTextComponent("tooltip.seeker_compass.standing_on").applyTextStyle(TextFormatting.GRAY)).appendSibling(new StringTextComponent(I18n.format(world.getBlockState(status.pos.down()).getBlock().getTranslationKey()))));
				tooltip.add((new TranslationTextComponent("tooltip.seeker_compass.biome").applyTextStyle(TextFormatting.GRAY)).appendSibling(new StringTextComponent(I18n.format(world.getBiome(status.pos).getTranslationKey()))));
			}
		}
	}
	
	@Override
	public ActionResult<ItemStack> onItemRightClick(World world, PlayerEntity player, Hand hand) {
		ItemStack stack = player.getHeldItem(hand);
		if(isNotBroken(stack) && stack.hasTag() && stack.getTag().contains("TrackingEntity") && player.isShiftKeyDown() && rayTrace(world, player, FluidMode.NONE).getType() != Type.ENTITY) {
			int level = EnchantmentHelper.getEnchantmentLevel(SCEnchants.VOODOO.get(), stack);
			if(level > 0 && !getTargetEntity(player, 8).isPresent()) {
				if(world instanceof ServerWorld) {
					Entity entity = this.getEntity((ServerWorld) world, stack);
					if(entity != null) {
						entity.attackEntityFrom(DamageSource.causePlayerDamage(player).setDamageBypassesArmor().setMagicDamage(), 1 + level);
				
						Random rand = player.getRNG();
						for(int i = 0; i < 8; i++) {
							Vec3d targetPosition = entity.getPositionVec();
							Vec3d position = targetPosition.add(rand.nextBoolean() ? -rand.nextFloat() : rand.nextFloat() * 1.25F, entity.getEyeHeight(), rand.nextBoolean() ? -rand.nextFloat() : rand.nextFloat() * 1.25F);
							Vec3d motion = position.subtract(targetPosition.add(0.0F, entity.getEyeHeight() * 0.35F, 0.0F)).scale(-0.5F);
						
							SeekerCompass.CHANNEL.send(PacketDistributor.ALL.with(() -> null), new MessageS2CParticle("seeker_compass:seeker_eyes", position.getX(), position.getY(), position.getZ(), motion.getX(), motion.getY(), motion.getZ()));
						}
						if(!player.isCreative()) {
							int damage = MathHelper.clamp(stack.getDamage() + 400, 0, stack.getMaxDamage() - 1);
							stack.setDamage(damage);
							
							if(damage == stack.getMaxDamage() - 1) {
								player.playSound(SoundEvents.ITEM_SHIELD_BREAK, SoundCategory.PLAYERS, 0.5F, 1.5F);
							}
						}
					}
				}
				return ActionResult.resultSuccess(stack);
			}
		}
		return super.onItemRightClick(world, player, hand);
	}
	
	@Override
	public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
		return false;
	}
	
	@Override
	public boolean getIsRepairable(ItemStack toRepair, ItemStack repair) {
		return repair.getItem() == Items.MAGMA_CREAM;
	}
	
	@Override
	public int getMaxDamage(ItemStack stack) {
		return 1200;
	}
	
	@Override
	public int getRGBDurabilityForDisplay(ItemStack stack) {
		Color color = new Color(16743936);
		return color.getRGB();
	}
	
	public static Optional<Entity> getTargetEntity(@Nullable Entity entityIn, int distance) {
		if(entityIn == null) {
			return Optional.empty();
		} else {
			Vec3d vec3d = entityIn.getEyePosition(1.0F);
			Vec3d vec3d1 = entityIn.getLook(1.0F).scale((double)distance);
			Vec3d vec3d2 = vec3d.add(vec3d1);
			AxisAlignedBB axisalignedbb = entityIn.getBoundingBox().expand(vec3d1).grow(1.0D);
			int i = distance * distance;
			Predicate<Entity> predicate = (p_217727_0_) -> {
				return !p_217727_0_.isSpectator() && p_217727_0_.canBeCollidedWith();
			};
			EntityRayTraceResult entityraytraceresult = rayTraceEntities(entityIn, vec3d, vec3d2, axisalignedbb, predicate, (double)i);
			if (entityraytraceresult == null) {
				return Optional.empty();
			} else {
				return vec3d.squareDistanceTo(entityraytraceresult.getHitVec()) > (double)i ? Optional.empty() : Optional.of(entityraytraceresult.getEntity());
			}
		}
	}
	
	@Nullable
	public static EntityRayTraceResult rayTraceEntities(Entity p_221273_0_, Vec3d p_221273_1_, Vec3d p_221273_2_, AxisAlignedBB p_221273_3_, Predicate<Entity> p_221273_4_, double p_221273_5_) {
		World world = p_221273_0_.world;
		double d0 = p_221273_5_;
		Entity entity = null;
		Vec3d vec3d = null;

		for(Entity entity1 : world.getEntitiesInAABBexcluding(p_221273_0_, p_221273_3_, p_221273_4_)) {
			AxisAlignedBB axisalignedbb = entity1.getBoundingBox().grow((double)entity1.getCollisionBorderSize());
			Optional<Vec3d> optional = axisalignedbb.rayTrace(p_221273_1_, p_221273_2_);
			if (axisalignedbb.contains(p_221273_1_)) {
				if (d0 >= 0.0D) {
					entity = entity1;
					vec3d = optional.orElse(p_221273_1_);
					d0 = 0.0D;
				}
			} else if (optional.isPresent()) {
				Vec3d vec3d1 = optional.get();
				double d1 = p_221273_1_.squareDistanceTo(vec3d1);
				if (d1 < d0 || d0 == 0.0D) {
					if (entity1.getLowestRidingEntity() == p_221273_0_.getLowestRidingEntity()) {
						if (d0 == 0.0D) {
							entity = entity1;
							vec3d = vec3d1;
						}
					} else {
						entity = entity1;
						vec3d = vec3d1;
						d0 = d1;
					}
				}
			}
		}

		if (entity == null) {
			return null;
		} else {
			return new EntityRayTraceResult(entity, vec3d);
		}
	}

	static class EntityStatusData {
		public final boolean isAlive;
		public final float health;
		public final String entityType;
		public final String entityName;
		public final BlockPos pos;
		public final int dimensionId;
		
		public EntityStatusData(boolean isAlive, float health, String entityType, String entityName, BlockPos pos, int dimensionId) {
			this.isAlive = isAlive;
			this.health = health;
			this.entityType = entityType;
			this.entityName = entityName;
			this.pos = pos;
			this.dimensionId = dimensionId;
		}
		
		public boolean canTrackEntity(DimensionType type) {
			return DimensionType.getById(this.dimensionId) == type && this.isAlive;
		}
		
		public static EntityStatusData read(CompoundNBT compound) {
			return new EntityStatusData(compound.getBoolean("Alive"), compound.getFloat("Health"), compound.getString("EntityType"), compound.getString("EntityName"), NBTUtil.readBlockPos((CompoundNBT) compound.get("Pos")), compound.getInt("DimensionId"));
		}
		
		public static CompoundNBT write(Entity trackingEntity) {
			CompoundNBT tag = new CompoundNBT();
			tag.putBoolean("Alive", trackingEntity.isAlive());
			tag.putString("EntityType", I18n.format(trackingEntity.getType().getTranslationKey()));
			tag.putString("EntityName", trackingEntity.getName().getString());
			
			if(trackingEntity instanceof LivingEntity) {
				tag.putFloat("Health", ((LivingEntity) trackingEntity).getHealth());
			}
			tag.put("Pos", NBTUtil.writeBlockPos(trackingEntity.getPosition()));
			tag.putInt("DimensionId", trackingEntity.dimension.getId());
			return tag;
		}
		
		public static CompoundNBT writeMissingEntity(EntityStatusData status) {
			CompoundNBT tag = new CompoundNBT();
			tag.putBoolean("Alive", false);
			tag.putString("EntityType", status.entityType);
			tag.putString("EntityName", status.entityName);
			tag.putFloat("Health", 0.0F);
			tag.put("Pos", NBTUtil.writeBlockPos(status.pos));
			tag.putInt("DimensionId", status.dimensionId);
			return tag;
		}
	}
	
	static class RotationData {
		private final double rotation;
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
}