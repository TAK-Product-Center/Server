

package com.bbn.marti.remote;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;

public class QueueMetric implements Serializable {
	private static final long serialVersionUID = -3693914778574548283L;
	
	public AtomicLong currentSize = new AtomicLong(0);
	public AtomicLong capacity = new AtomicLong(0);
}
