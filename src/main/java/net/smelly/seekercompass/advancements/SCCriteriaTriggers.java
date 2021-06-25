package net.smelly.seekercompass.advancements;

import com.minecraftabnormals.abnormals_core.common.advancement.EmptyTrigger;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.smelly.seekercompass.SeekerCompass;

@EventBusSubscriber(modid = SeekerCompass.MOD_ID)
public class SCCriteriaTriggers {
	public static final EmptyTrigger JOHN_CENA = CriteriaTriggers.register(new EmptyTrigger(prefix("john_cena")));
	public static final EmptyTrigger VOODOO_MAGIC = CriteriaTriggers.register(new EmptyTrigger(prefix("voodoo_magic")));
	
	private static ResourceLocation prefix(String name) {
		return new ResourceLocation(SeekerCompass.MOD_ID, name);
	}
}