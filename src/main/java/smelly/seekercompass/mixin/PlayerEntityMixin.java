package smelly.seekercompass.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import smelly.seekercompass.Stalker;

import javax.annotation.Nullable;

@Mixin(PlayerEntity.class)
public final class PlayerEntityMixin implements Stalker {
    private LivingEntity stalkingEntity;

    @Override
    public void setStalkingEntity(@Nullable LivingEntity stalkingEntity) {
        this.stalkingEntity = stalkingEntity;
    }

    @Nullable
    @Override
    public LivingEntity getStalkingEntity() {
        return this.stalkingEntity;
    }
}