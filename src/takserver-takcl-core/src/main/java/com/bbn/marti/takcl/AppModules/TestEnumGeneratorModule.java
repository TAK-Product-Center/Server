package com.bbn.marti.takcl.AppModules;

import com.bbn.marti.takcl.AppModules.generic.AppModuleInterface;
import com.bbn.marti.takcl.cli.simple.Command;
import com.bbn.marti.takcl.config.common.TakclRunMode;
import com.bbn.marti.test.shared.data.GenerationHelper;
import org.jetbrains.annotations.NotNull;

/**
 */
public class TestEnumGeneratorModule implements AppModuleInterface {

	@NotNull
	@Override
	public TakclRunMode[] getRunModes() {
		return new TakclRunMode[]{TakclRunMode.LOCAL_SOURCE_INTERACTION};
	}

	@Override
	public ServerState getRequiredServerState() {
		return ServerState.NOT_APPLICABLE;
	}

	@Override
	public String getCommandDescription() {
		return "Used to generate test enum definitions.";
	}

	@Override
	public void init() {
	}

	@Command
	public void generateTestData() {
		GenerationHelper helper = new GenerationHelper();
		helper.generateTestData();
	}

}
