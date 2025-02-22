package net.smelly.seekercompass;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

public class SCTags {
	public static class EntityTypeTags {
		public static final TagKey<EntityType<?>> SUMMONABLES = create("summonables");

		private static TagKey<EntityType<?>> create(String name) {
			return TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation(SeekerCompass.MOD_ID, name));
		}
	}
}