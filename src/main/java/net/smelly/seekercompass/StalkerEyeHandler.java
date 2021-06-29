package net.smelly.seekercompass;

import com.minecraftabnormals.abnormals_core.client.ClientInfo;
import com.minecraftabnormals.abnormals_core.core.events.EntityTrackingEvent;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.LivingRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.gui.ForgeIngameGui;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.network.PacketDistributor;
import net.smelly.seekercompass.interfaces.ClientStalkable;
import net.smelly.seekercompass.interfaces.Stalkable;
import net.smelly.seekercompass.mixin.client.EntityRendererInvokerMixin;
import net.smelly.seekercompass.network.S2CUpdateStalkedMessage;
import org.lwjgl.opengl.GL11;

@Mod.EventBusSubscriber(modid = SeekerCompass.MOD_ID)
public final class StalkerEyeHandler {
	private static final ResourceLocation STALKER_EYE = new ResourceLocation(SeekerCompass.MOD_ID, "textures/entity/stalker_eye.png");

	@SubscribeEvent
	public static void onEntityTracking(EntityTrackingEvent event) {
		if (!event.isUpdating()) {
			Entity entity = event.getEntity();
			if (entity instanceof Stalkable) {
				Stalkable stalkable = (Stalkable) entity;
				if (stalkable.isDirty()) {
					PacketDistributor.PacketTarget packetTarget = entity instanceof ServerPlayerEntity ? PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity) : PacketDistributor.TRACKING_ENTITY.with(() -> entity);
					SeekerCompass.CHANNEL.send(packetTarget, new S2CUpdateStalkedMessage(entity.getId(), stalkable.hasStalkers()));
				}
			}
		}
	}

	@SubscribeEvent
	public static void onStartTrackingEntity(PlayerEvent.StartTracking event) {
		Entity target = event.getTarget();
		if (target instanceof LivingEntity) {
			PlayerEntity player = event.getPlayer();
			if (player instanceof ServerPlayerEntity) {
				SeekerCompass.CHANNEL.send(PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) player), new S2CUpdateStalkedMessage(target.getId(), ((Stalkable) target).hasStalkers()));
			}
		}
	}

	@OnlyIn(Dist.CLIENT)
	@SubscribeEvent
	public static void onLivingRender(RenderLivingEvent.Post<?, ?> event) {
		if (SCConfig.CLIENT.stalkingEyeProcedure.rendersAboveEntity) {
			LivingEntity entity = event.getEntity();
			if (((ClientStalkable) entity).isBeingStalked()) {
				LivingRenderer<? extends LivingEntity, ?> livingRenderer = event.getRenderer();
				MatrixStack matrixStack = event.getMatrixStack();
				matrixStack.pushPose();
				EntityRendererManager dispatcher = livingRenderer.getDispatcher();
				float offset = ((EntityRendererInvokerMixin<LivingEntity>) livingRenderer).callShouldShowName(entity) && ForgeHooksClient.isNameplateInRenderDistance(entity, dispatcher.distanceToSqr(entity)) ? 0.5F : 0.0F;
				matrixStack.translate(0.0D, entity.getBbHeight() + 0.5F + offset, 0.0D);
				matrixStack.mulPose(dispatcher.cameraOrientation());
				matrixStack.scale(-0.025F * 2.0F, -0.025F * 2.0F, 0.025F * 2.0F);
				RenderSystem.enableBlend();
				RenderSystem.enableDepthTest();
				RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
				Matrix4f matrix4f = matrixStack.last().pose();
				Tessellator tessellator = Tessellator.getInstance();
				BufferBuilder builder = tessellator.getBuilder();
				ClientInfo.MINECRAFT.getTextureManager().bind(STALKER_EYE);
				builder.begin(7, DefaultVertexFormats.POSITION_TEX);
				builder.vertex(matrix4f, -3.5F, 5.0F, 0.0F).uv(0, 1).endVertex();
				builder.vertex(matrix4f, 3.5F, 5.0F, 0.0F).uv(1, 1).endVertex();
				builder.vertex(matrix4f, 3.5F, 0.0F, 0.0F).uv(1, 0).endVertex();
				builder.vertex(matrix4f, -3.5F, 0.0F, 0.0F).uv(0, 0).endVertex();
				tessellator.end();

				RenderSystem.disableBlend();
				matrixStack.popPose();
			}
		}
	}

	@OnlyIn(Dist.CLIENT)
	@SubscribeEvent
	public static void renderStalkingEye(RenderGameOverlayEvent.Post event) {
		if (SCConfig.CLIENT.stalkingEyeProcedure.rendersInGUI && event.getType() == RenderGameOverlayEvent.ElementType.ALL) {
			Minecraft minecraft = Minecraft.getInstance();
			if (!minecraft.isPaused()) {
				Entity cameraEntity = minecraft.getCameraEntity();
				if (cameraEntity instanceof Stalkable && ((ClientStalkable) cameraEntity).isBeingStalked() || isHoveringOverStalkedEntity(minecraft.hitResult)) {
					MatrixStack stack = event.getMatrixStack();
					stack.pushPose();
					RenderSystem.enableBlend();

					minecraft.getTextureManager().bind(STALKER_EYE);

					MainWindow mainWindow = minecraft.getWindow();
					int scaledWidth = mainWindow.getGuiScaledWidth();
					int scaledHeight = mainWindow.getGuiScaledHeight();
					float middle = scaledWidth / 2.0F;
					int bottom = scaledHeight - ForgeIngameGui.right_height;
					int top = bottom + 10;

					Tessellator tessellator = Tessellator.getInstance();
					BufferBuilder bufferbuilder = tessellator.getBuilder();
					bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX);
					float left = middle - 7;
					float right = middle + 7;
					bufferbuilder.vertex(left, top, 0).uv(0, 1).endVertex();
					bufferbuilder.vertex(right, top, 0).uv(1, 1).endVertex();
					bufferbuilder.vertex(right, bottom, 0).uv(1, 0).endVertex();
					bufferbuilder.vertex(left, bottom, 0).uv(0, 0).endVertex();
					tessellator.end();

					RenderSystem.disableBlend();
					stack.popPose();

					ForgeIngameGui.right_height += 11;
				}
			}
		}
	}

	private static boolean isHoveringOverStalkedEntity(RayTraceResult rayTraceResult) {
		if (rayTraceResult instanceof EntityRayTraceResult) {
			Entity entity = ((EntityRayTraceResult) rayTraceResult).getEntity();
			return entity instanceof ClientStalkable && ((ClientStalkable) entity).isBeingStalked();
		}
		return false;
	}

}
