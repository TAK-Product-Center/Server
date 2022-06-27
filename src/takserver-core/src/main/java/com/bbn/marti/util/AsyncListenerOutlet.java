

package com.bbn.marti.util;

import com.google.common.util.concurrent.ListenableFuture;

public interface AsyncListenerOutlet<E> extends ListenerOutlet<E> {
	public ListenableFuture<Boolean> asyncBroadcast(E input);
}