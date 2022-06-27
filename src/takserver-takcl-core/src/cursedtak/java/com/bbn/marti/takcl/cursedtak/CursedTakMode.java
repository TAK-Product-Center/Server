package com.bbn.marti.takcl.cursedtak;

import org.jetbrains.annotations.NotNull;

enum CursedTakMode {
	COT_SEND("Show Sent", "Sent CoT Messages"),
	COT_RECEIVE("Show Received", "Received CoT Messages");

	public final String label;
	public final String modeTitle;

	CursedTakMode(@NotNull String label, @NotNull String modeTitle) {
		this.label = label;
		this.modeTitle = modeTitle;
	}

	public CursedTakMode nextMode() {
		switch (this) {
			case COT_SEND:
				return COT_RECEIVE;

			case COT_RECEIVE:
				return COT_SEND;
			default:
				throw new RuntimeException("Unaccounted for mode '" + this.label + "'!");
		}
	}
}
