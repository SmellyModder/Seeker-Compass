package smelly.seekercompass;

import net.minecraft.entity.EntityType;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.Tag;
import net.minecraft.util.ResourceLocation;

public class SCTags {
	
	public static class EntityTags {
		public static final Tag<EntityType<?>> SUMMONABLES = createTag("summonables");
		
		public static Tag<EntityType<?>> createTag(String name) {
			return new EntityTypeTags.Wrapper(new ResourceLocation(SeekerCompass.MOD_ID, name));
		}
	}

}