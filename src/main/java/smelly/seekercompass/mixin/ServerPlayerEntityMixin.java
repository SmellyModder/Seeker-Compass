package smelly.seekercompass.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import smelly.seekercompass.Stalker;

import javax.annotation.Nullable;

@Mixin(ServerPlayerEntity.class)
public final class ServerPlayerEntityMixin implements Stalker {
    private LivingEntity stalkingEntity;
    private boolean hasUnstalked;

    @Override
    public void setStalkingEntity(@Nullable LivingEntity stalkingEntity) {
        this.stalkingEntity = stalkingEntity;
        if (stalkingEntity != null) {
            this.hasUnstalked = false;
        } else {
            this.hasUnstalked = true;
        }
    }

    @Nullable
    @Override
    public LivingEntity getStalkingEntity() {
        return this.stalkingEntity;
    }

    @Override
    public boolean wasHidingGUI() {
        return false;
    }

    @Override
    public void setUnstalked(boolean unstalked) {
        this.hasUnstalked = unstalked;
    }

    @Override
    public boolean hasUnstalked() {
        return this.hasUnstalked;
    }
}