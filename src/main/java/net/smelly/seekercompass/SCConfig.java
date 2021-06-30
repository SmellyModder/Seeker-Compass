package net.smelly.seekercompass;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public final class SCConfig {
	public static final ForgeConfigSpec COMMON_SPEC;
	public static final Common COMMON;
	public static final ForgeConfigSpec CLIENT_SPEC;
	public static final Client CLIENT;

	static {
		Pair<Common, ForgeConfigSpec> commonSpecPair = new ForgeConfigSpec.Builder().configure(Common::new);
		COMMON_SPEC = commonSpecPair.getRight();
		COMMON = commonSpecPair.getLeft();

		Pair<Client, ForgeConfigSpec> clientSpecPair = new ForgeConfigSpec.Builder().configure(Client::new);
		CLIENT_SPEC = clientSpecPair.getRight();
		CLIENT = clientSpecPair.getLeft();
	}

	public static final class Common {
		private final ForgeConfigSpec.DoubleValue zombifiedPiglinCompassChanceValue;
		public double zombifiedPiglinCompassChance;

		Common(ForgeConfigSpec.Builder builder) {
			builder.comment("Common settings for Seeker Compass.").push("common");
			this.zombifiedPiglinCompassChanceValue = builder
					.comment("Chance for Zombified Piglins to naturally spawn holding a Seeker Compass. Default: 0.02")
					.translation(makeTranslation("zombified_piglin_compass_chance"))
					.defineInRange("zombifiedPiglinCompassChance", 0.02F, 0.0F, 1.0F);
			builder.pop();
		}

		public void load() {
			this.zombifiedPiglinCompassChance = this.zombifiedPiglinCompassChanceValue.get();
		}
	}

	public static final class Client {
		private final ForgeConfigSpec.EnumValue<StalkingEyeProcedure> stalkingEyeProcedureEnumValue;
		public StalkingEyeProcedure stalkingEyeProcedure;
		private final ForgeConfigSpec.BooleanValue enableStalkingShaderValue;
		public boolean enableStalkingShader;

		Client(ForgeConfigSpec.Builder builder) {
			builder.comment("Client only settings for Seeker Compass.").push("client");
			this.stalkingEyeProcedureEnumValue = builder
					.comment("Determines the way the stalking eye is rendered for entities being stalked. Default: BOTH\nBOTH: GUI and ENTITY.\nENTITY: Renders the eye above the entity.\nGUI: Renders the eye at the top of the GUI.\nDISABLED: The eye will not render at all.")
					.translation(makeTranslation("stalking_eye_procedure"))
					.defineEnum("stalkingEyeProcedure", StalkingEyeProcedure.BOTH);
			this.enableStalkingShaderValue = builder
					.comment("If the post-effect shader should be applied when stalking an entity.")
					.translation(makeTranslation("enable_stalking_shader"))
					.define("enableStalkingShader", true);
			builder.pop();
		}

		public void load() {
			this.stalkingEyeProcedure = this.stalkingEyeProcedureEnumValue.get();
			this.enableStalkingShader = this.enableStalkingShaderValue.get();
		}
	}

	private static String makeTranslation(String name) {
		return "seeker_compass.config." + name;
	}

	enum StalkingEyeProcedure {
		BOTH(true, true),
		ENTITY(true, false),
		GUI(false, true),
		DISABLED(false, false);

		public final boolean rendersAboveEntity, rendersInGUI;

		StalkingEyeProcedure(boolean rendersAboveEntity, boolean rendersInGUI) {
			this.rendersAboveEntity = rendersAboveEntity;
			this.rendersInGUI = rendersInGUI;
		}
	}
}
