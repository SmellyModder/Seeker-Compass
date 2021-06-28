package net.smelly.seekercompass.mixin.client;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(EntityRenderer.class)
public interface EntityRendererInvokerMixin<T extends Entity> {
	@Invoker
	boolean callShouldShowName(T entity);
}
