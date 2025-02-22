package net.smelly.seekercompass.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.PacketDistributor;
import net.smelly.seekercompass.SeekerCompass;
import net.smelly.seekercompass.interfaces.Stalkable;
import net.smelly.seekercompass.interfaces.Stalker;
import net.smelly.seekercompass.network.S2CUpdateStalkedMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;
import java.util.Set;

@Mixin(LivingEntity.class)
public final class LivingEntityMixin implements Stalkable {
	private final Set<Player> stalkers = new HashSet<>();
	private boolean dirty;

	@Inject(at = @At("HEAD"), method = "tick")
	private void tickStalking(CallbackInfo info) {
		LivingEntity livingEntity = ((LivingEntity) (Object) this);
		if (!livingEntity.level().isClientSide) {
			Set<Player> stalkers = this.stalkers;
			int prevSize = stalkers.size();
			stalkers.removeIf(player -> !player.isAlive() || ((Stalker) player).getStalkingEntity() != livingEntity);
			if (prevSize != stalkers.size()) {
				this.setDirty(true);
			}
			if (this.isDirty()) {
				PacketDistributor.PacketTarget packetTarget = livingEntity instanceof ServerPlayer serverPlayer ? PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> serverPlayer) : PacketDistributor.TRACKING_ENTITY.with(() -> livingEntity);
				SeekerCompass.CHANNEL.send(packetTarget, new S2CUpdateStalkedMessage(livingEntity.getId(), !this.stalkers.isEmpty()));
			}
		}
	}

	@Override
	public void addStalker(Player player) {
		if (this.stalkers.add(player)) {
			this.setDirty(true);
		}
	}

	@Override
	public void removeStalker(Player player) {
		if (this.stalkers.remove(player)) {
			this.setDirty(true);
		}
	}

	@Override
	public boolean isBeingStalkedBy(Player player) {
		return this.stalkers.contains(player);
	}

	@Override
	public void setDirty(boolean dirty) {
		this.dirty = dirty;
	}

	@Override
	public boolean isDirty() {
		return this.dirty;
	}
}
