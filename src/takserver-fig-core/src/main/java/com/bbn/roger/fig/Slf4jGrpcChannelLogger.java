package com.bbn.roger.fig;

import io.grpc.ChannelLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;


public class Slf4jGrpcChannelLogger extends ChannelLogger {

	private final ChannelLogger channelLogger;
	private final Logger logger;
	private final String label;

	public Slf4jGrpcChannelLogger(ChannelLogger channelLogger, String label) {
		this.channelLogger = channelLogger;
		this.logger = LoggerFactory.getLogger(Slf4jGrpcChannelLogger.class);
		this.label = label;
	}

	private void innerLog(ChannelLogLevel level, String message) {
		switch (level) {
			case DEBUG:
				logger.debug(label + ":" + message);
				break;
			case INFO:
				logger.info(label + ":" + message);
				break;
			case WARNING:
				logger.warn(label + ":" + message);
				break;
			case ERROR:
				logger.error(label + ":" + message);
				break;
		}
	}

	@Override
	public void log(ChannelLogLevel level, String message) {
		innerLog(level, message);
		channelLogger.log(level, message);
	}

	@Override
	public void log(ChannelLogLevel level, String messageFormat, Object... args) {
		String message = MessageFormat.format(messageFormat, args);
		innerLog(level, message);
		channelLogger.log(level, messageFormat, args);
	}
}
