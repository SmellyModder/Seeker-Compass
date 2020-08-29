package smelly.seekercompass;

import net.minecraft.entity.LivingEntity;

import javax.annotation.Nullable;

public interface Stalker {
    void setStalkingEntity(@Nullable LivingEntity stalkingEntity);

    @Nullable
    LivingEntity getStalkingEntity();
}