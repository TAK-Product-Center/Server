package com.bbn.marti.takcl.cursedtak;

import com.bbn.marti.test.shared.TestConnectivityState;
import org.jetbrains.annotations.NotNull;

/**
 * Created on 3/13/18.
 */
interface CursedTakControllerListener {
	void modeSwitched(@NotNull CursedTakMode newMode);

	void messageSent(@NotNull String message);

	void messageReceived(@NotNull String message);

	void connectivityStateChanged(TestConnectivityState connectivityState);
}
