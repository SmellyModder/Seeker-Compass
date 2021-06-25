package net.smelly.seekercompass;

import net.minecraft.entity.EntityType;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.ITag.INamedTag;

public class SCTags {
	
	public static class EntityTags {
		public static final INamedTag<EntityType<?>> SUMMONABLES = createTag("summonables");
		
		public static INamedTag<EntityType<?>> createTag(String name) {
			return EntityTypeTags.bind(SeekerCompass.MOD_ID + name);
		}
	}

}