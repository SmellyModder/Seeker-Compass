package smelly.seekercompass;

import java.awt.Color;
import java.util.List;

import javax.annotation.Nullable;

import com.mojang.datafixers.util.Pair;

import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.IItemPropertyGetter;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
	
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
		}
	}
	
	@Override
	public ActionResultType onItemUse(ItemUseContext context) {
		
		return ActionResultType.PASS;
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

	public static class EntityStatusData {
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
	
	public static class RotationData {
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