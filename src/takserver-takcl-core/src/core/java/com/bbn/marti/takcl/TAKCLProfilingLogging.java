package com.bbn.marti.takcl;

import com.bbn.marti.test.shared.data.protocols.ProtocolProfiles;
import com.bbn.marti.test.shared.data.users.AbstractUser;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TAKCLProfilingLogging {

	public enum Category {
		__NONE,
		NETWORK;

		@Override
		public String toString() {
			return this == __NONE ? "" : this.name();
		}
	}

	public enum Direction {
		__NONE,
		SEND,
		RECEIVE,
		CONNECT,
		LISTEN,
		AUTH;

		@Override
		public String toString() {
			return this == __NONE ? "" : this.name();
		}
	}

	public enum Data {
		__NONE,
		MESSAGE;

		@Override
		public String toString() {
			return this == __NONE ? "" : this.name();
		}
	}

	public enum Protocol {
		__NONE,
		TCP,
		UDP,
		STCP,
		TLS,
		SSL,
		MCAST,
		SUB_TCP,
		SUB_UDP,
		SUB_STCP,
		SUB_TLS,
		SUB_SSL,
		SUB_MCAST,
		WSS;

		@Override
		public String toString() {
			return this == __NONE ? "" : this.name();
		}
	}

	public enum Source {
		CLIENT,
		SERVER
	}

	public interface LogActivityInterface {
		String startMessage();

		String finishMessage();
	}

	public enum LogActivity implements LogActivityInterface {
		connect(Category.NETWORK, Direction.CONNECT, Protocol.__NONE, Data.__NONE),
		listen(Category.NETWORK, Direction.LISTEN, Protocol.__NONE, Data.__NONE),

		auth(Category.NETWORK, Direction.AUTH, Protocol.__NONE, Data.__NONE),

		sendTcp(Category.NETWORK, Direction.SEND, Protocol.TCP, Data.MESSAGE),
		sendUdp(Category.NETWORK, Direction.SEND, Protocol.UDP, Data.MESSAGE),
		sendStcp(Category.NETWORK, Direction.SEND, Protocol.STCP, Data.MESSAGE),
		sendTls(Category.NETWORK, Direction.SEND, Protocol.TLS, Data.MESSAGE),
		sendSsl(Category.NETWORK, Direction.SEND, Protocol.SSL, Data.MESSAGE),
		sendMcast(Category.NETWORK, Direction.SEND, Protocol.MCAST, Data.MESSAGE),
		sendWss(Category.NETWORK, Direction.SEND, Protocol.WSS, Data.MESSAGE),

		receiveStcp(Category.NETWORK, Direction.RECEIVE, Protocol.STCP, Data.MESSAGE),
		receiveSsl(Category.NETWORK, Direction.RECEIVE, Protocol.SSL, Data.MESSAGE),
		receiveTls(Category.NETWORK, Direction.RECEIVE, Protocol.TLS, Data.MESSAGE),
		receiveWss(Category.NETWORK, Direction.RECEIVE, Protocol.WSS, Data.MESSAGE),

		subReceiveTcp(Category.NETWORK, Direction.RECEIVE, Protocol.TCP, Data.MESSAGE),
		subReceiveUdp(Category.NETWORK, Direction.RECEIVE, Protocol.UDP, Data.MESSAGE),
		subReceiveStcp(Category.NETWORK, Direction.RECEIVE, Protocol.STCP, Data.MESSAGE),
		subReceiveTls(Category.NETWORK, Direction.RECEIVE, Protocol.TLS, Data.MESSAGE),
		subReceiveSsl(Category.NETWORK, Direction.RECEIVE, Protocol.SSL, Data.MESSAGE),
		subReceiveMcast(Category.NETWORK, Direction.RECEIVE, Protocol.MCAST, Data.MESSAGE);

		public final Category category;
		public final Direction direction;
		public final Protocol protocol;
		public final Data data;

		LogActivity(@NotNull Category category, @NotNull Direction direction, @NotNull Protocol protocol, @NotNull Data data) {
			this.category = category;
			this.direction = direction;
			this.protocol = protocol;
			this.data = data;
		}

		public final String startMessage() {
			return "-- " + String.join(" ", category.toString(), direction.toString(), protocol.toString(), data.toString(), "Begin --");
		}

		public final String finishMessage() {
			return "-- " + String.join(" ", category.toString(), direction.toString(), protocol.toString(), data.toString(), "Completed --");
		}

		@Override
		public final String toString() {
			return "(" + String.join("/", category.toString(), direction.toString(), protocol.toString(), data.toString()) + ")";
		}

		public static LogActivity getSend(AbstractUser user) {
			ProtocolProfiles protocol = user.getConnection().getProtocol();

			switch (protocol) {
				case INPUT_TCP:
				case DATAFEED_TCP:
					assert (protocol.canSend());
					return sendTcp;
				case INPUT_UDP:
				case DATAFEED_UDP:
					assert (protocol.canSend());
					return sendUdp;
				case INPUT_MCAST:
				case DATAFEED_MCAST:
					assert (protocol.canSend());
					return sendMcast;
				case INPUT_STCP:
				case DATAFEED_STCP:
					assert (protocol.canSend());
					return sendStcp;
				case INPUT_TLS:
				case DATAFEED_TLS:
					assert (protocol.canSend());
					return sendTls;
				case INPUT_SSL:
				case DATAFEED_SSL:
					assert (protocol.canSend());
					return sendSsl;
				default:
					assert (!protocol.canSend());
					throw new RuntimeException("Cannot send from protocol " + protocol + "!");
			}
		}

		public static LogActivity getReceive(AbstractUser user) {
			ProtocolProfiles protocol = user.getConnection().getProtocol();

			switch (protocol) {
				case INPUT_STCP:
				case DATAFEED_STCP:
					assert (protocol.canListen());
					return receiveStcp;
				case INPUT_SSL:
				case DATAFEED_SSL:
					assert (protocol.canListen());
					return receiveSsl;
				case INPUT_TLS:
				case DATAFEED_TLS:
					assert (protocol.canListen());
					return receiveTls;
				case SUBSCRIPTION_TCP:
					assert (protocol.canListen());
					return subReceiveTcp;
				case SUBSCRIPTION_UDP:
					assert (protocol.canListen());
					return subReceiveUdp;
				case SUBSCRIPTION_MCAST:
					assert (protocol.canListen());
					return subReceiveMcast;
				case SUBSCRIPTION_STCP:
					assert (protocol.canListen());
					return subReceiveStcp;
				case SUBSCRIPTION_SSL:
					assert (protocol.canListen());
					return subReceiveSsl;
				case SUBSCRIPTION_TLS:
					assert (protocol.canListen());
					return subReceiveTls;
				default:
					assert (!protocol.canListen());
					throw new RuntimeException("Cannot receive from protocol " + protocol + "!");
			}
		}

	}

	public static class PythonGenerator {

		private static void toPythonEnum(@NotNull String label, @NotNull Enum<?>[] enumValues, @NotNull StringBuilder sb) {
			sb.append("class ").append(label).append("(Enum):\n");
			for (Enum<?> enumValue : enumValues) {
				String value;
				if (enumValue.name().equals("__NONE")) {
					value = "\tNONE = None\n";
				} else {
					value = "\t" + enumValue.name() + " = '" + enumValue.name() + "'\n";
				}
				sb.append(value);
			}
			sb.append("\n\n");
		}

		public PythonGenerator() {
		}

		private void addBaseEvent(@NotNull StringBuilder sb) {
			sb.append("class BaseEvent(Enum):\n")
					.append("\tdef __init__(self, category: Category, source: Source, direction: Direction, protocol: Protocol, match: str = None, regex: str = None):\n")
					.append("\t\tself.category = category\n").append("\t\tself.source = source\n").append("\t\tself.direction = direction\n")
					.append("\t\tself.protocol = protocol\n").append("\t\tself.match = match\n").append("\t\tself.regex = regex\n")
					.append("\n\t@property\n\tdef label(self) -> str:\n\t\treturn self.category.name + ': ' + self.source.name + '-' + self.direction.name + '(' + self.protocol.name + ')'\n")
					.append("\n\tdef __str__(self):\n\t\treturn self.label\n\n\n");
		}

		public void generate(@NotNull Path targetFile) throws IOException {
			StringBuilder sb = new StringBuilder("from enum import Enum\n\n\n");

			toPythonEnum("Category", Category.values(), sb);
			toPythonEnum("Direction", Direction.values(), sb);
			toPythonEnum("Data", Data.values(), sb);
			toPythonEnum("Protocol", Protocol.values(), sb);
			toPythonEnum("Source", Source.values(), sb);

			addBaseEvent(sb);

			sb.append("class ClientEvent(BaseEvent):\n");
			sb.append("\tdef __init__(self, category: Category, direction: Direction, protocol: Protocol, match: str):\n");
			sb.append("\t\tsuper().__init__(category, Source.CLIENT, direction, protocol, match)\n");
			sb.append("\n");

			for (LogActivity entity : LogActivity.values()) {
				String cat = entity.category.name().equals("__NONE") ? "NONE" : entity.category.name();
				String dir = entity.direction.name().equals("__NONE") ? "NONE" : entity.direction.name();
				String proto = entity.protocol.name().equals("__NONE") ? "NONE" : entity.protocol.name();

				String value = "\t" + entity.name() + " = (Category." + cat + ", Direction." + dir
						+ ", Protocol." + proto + ", '" + entity.startMessage() + "')\n";
				sb.append(value);
			}

			Files.write(targetFile, sb.toString().getBytes());
		}
	}

	public static class DurationLogger {
		final String entityLabel;
		final Logger logger;
		final Map<LogActivityInterface, Long> startTimeMap = new ConcurrentHashMap<>();

		public DurationLogger(@NotNull String entityLabel, @NotNull Logger logger) {
			this.entityLabel = entityLabel;
			this.logger = logger;
		}

		public void begin(@NotNull LogActivityInterface logEntity) {
			if (startTimeMap.containsKey(logEntity)) {
				throw new RuntimeException("Activity '" + logEntity + "' has not ended!");
			}
			startTimeMap.put(logEntity, System.currentTimeMillis());
			logger.info("[" + entityLabel + "] - " + logEntity.startMessage());
		}

		public void end(@NotNull LogActivityInterface logEntity) {
			logger.info("[" + entityLabel + "] - " + logEntity.finishMessage() + " in " +
					(System.currentTimeMillis() - startTimeMap.remove(logEntity)) + "ms.");
		}
	}
}
