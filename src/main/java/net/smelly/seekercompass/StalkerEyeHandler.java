package net.smelly.seekercompass;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.smelly.seekercompass.interfaces.ClientStalkable;
import net.smelly.seekercompass.interfaces.Stalkable;
import net.smelly.seekercompass.mixin.client.EntityRendererInvokerMixin;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

@Mod.EventBusSubscriber(modid = SeekerCompass.MOD_ID)
public final class StalkerEyeHandler {
	private static final ResourceLocation STALKER_EYE = new ResourceLocation(SeekerCompass.MOD_ID, "textures/entity/stalker_eye.png");

	@OnlyIn(Dist.CLIENT)
	@SubscribeEvent
	public static void onLivingRender(RenderLivingEvent.Post<?, ?> event) {
		if (SCConfig.CLIENT.stalkingEyeProcedure.rendersAboveEntity) {
			LivingEntity entity = event.getEntity();
			if (((ClientStalkable) entity).isBeingStalked()) {
				LivingEntityRenderer<? extends LivingEntity, ?> livingRenderer = event.getRenderer();
				PoseStack matrixStack = event.getPoseStack();
				matrixStack.pushPose();
				var invoker = ((EntityRendererInvokerMixin<LivingEntity>) livingRenderer);
				EntityRenderDispatcher dispatcher = invoker.getEntityRenderDispatcher();
				float offset = invoker.callShouldShowName(entity) && ForgeHooksClient.isNameplateInRenderDistance(entity, dispatcher.distanceToSqr(entity)) ? 0.5F : 0.0F;
				matrixStack.translate(0.0D, entity.getBbHeight() + 0.5F + offset, 0.0D);
				matrixStack.mulPose(dispatcher.cameraOrientation());
				matrixStack.scale(-0.025F * 2.0F, -0.025F * 2.0F, 0.025F * 2.0F);

				RenderSystem.setShader(GameRenderer::getPositionTexShader);
				RenderSystem.setShaderTexture(0, STALKER_EYE);
				RenderSystem.enableBlend();
				RenderSystem.enableDepthTest();
				RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
				Matrix4f matrix4f = matrixStack.last().pose();
				Tesselator tessellator = Tesselator.getInstance();
				BufferBuilder builder = tessellator.getBuilder();
				builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
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
	public static void renderStalkingEye(RenderGuiOverlayEvent.Post event) {
		if (SCConfig.CLIENT.stalkingEyeProcedure.rendersInGUI && event.getOverlay().id().equals(VanillaGuiOverlay.EXPERIENCE_BAR.id())) {
			Minecraft minecraft = Minecraft.getInstance();
			if (!minecraft.isPaused()) {
				Entity cameraEntity = minecraft.getCameraEntity();
				if (cameraEntity instanceof Stalkable && ((ClientStalkable) cameraEntity).isBeingStalked() || isHoveringOverStalkedEntity(minecraft.hitResult)) {
					PoseStack stack = event.getGuiGraphics().pose();
					stack.pushPose();
					RenderSystem.enableBlend();

					RenderSystem.setShaderTexture(0, STALKER_EYE);

					Window mainWindow = minecraft.getWindow();
					int scaledWidth = mainWindow.getGuiScaledWidth();
					int scaledHeight = mainWindow.getGuiScaledHeight();
					float middle = scaledWidth / 2.0F;
					ForgeGui forgeGui = (ForgeGui) minecraft.gui;
					int bottom = scaledHeight - forgeGui.rightHeight;
					int top = bottom + 5;

					Tesselator tesselator = Tesselator.getInstance();
					BufferBuilder bufferbuilder = tesselator.getBuilder();
					bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
					float left = middle - 3.5F;
					float right = middle + 3.5F;
					bufferbuilder.vertex(left, top, 0).uv(0, 1).endVertex();
					bufferbuilder.vertex(right, top, 0).uv(1, 1).endVertex();
					bufferbuilder.vertex(right, bottom, 0).uv(1, 0).endVertex();
					bufferbuilder.vertex(left, bottom, 0).uv(0, 0).endVertex();
					tesselator.end();

					RenderSystem.disableBlend();
					stack.popPose();

					forgeGui.rightHeight += 11;
				}
			}
		}
	}

	private static boolean isHoveringOverStalkedEntity(HitResult hitResult) {
		return hitResult instanceof EntityHitResult entityHitResult && entityHitResult.getEntity() instanceof ClientStalkable stalkable && stalkable.isBeingStalked();
	}
}
