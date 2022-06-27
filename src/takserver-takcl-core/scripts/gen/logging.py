from enum import Enum


class Category(Enum):
	NONE = None
	NETWORK = 'NETWORK'


class Direction(Enum):
	NONE = None
	SEND = 'SEND'
	RECEIVE = 'RECEIVE'
	CONNECT = 'CONNECT'
	AUTH = 'AUTH'


class Data(Enum):
	NONE = None
	MESSAGE = 'MESSAGE'


class Protocol(Enum):
	NONE = None
	TCP = 'TCP'
	UDP = 'UDP'
	STCP = 'STCP'
	TLS = 'TLS'
	SSL = 'SSL'
	MCAST = 'MCAST'
	SUB_TCP = 'SUB_TCP'
	SUB_UDP = 'SUB_UDP'
	SUB_STCP = 'SUB_STCP'
	SUB_TLS = 'SUB_TLS'
	SUB_SSL = 'SUB_SSL'
	SUB_MCAST = 'SUB_MCAST'
	WSS = 'WSS'


class Source(Enum):
	CLIENT = 'CLIENT'
	SERVER = 'SERVER'


class BaseEvent(Enum):
	def __init__(self, category: Category, source: Source, direction: Direction, protocol: Protocol, match: str = None, regex: str = None):
		self.category = category
		self.source = source
		self.direction = direction
		self.protocol = protocol
		self.match = match
		self.regex = regex

	@property
	def label(self) -> str:
		return self.category.name + ': ' + self.source.name + '-' + self.direction.name + '(' + self.protocol.name + ')'

	def __str__(self):
		return self.label


class ClientEvent(BaseEvent):
	def __init__(self, category: Category, direction: Direction, protocol: Protocol, match: str):
		super().__init__(category, Source.CLIENT, direction, protocol, match)

	connect = (Category.NETWORK, Direction.CONNECT, Protocol.NONE, '-- NETWORK CONNECT   Begin --')
	auth = (Category.NETWORK, Direction.AUTH, Protocol.NONE, '-- NETWORK AUTH   Begin --')
	sendTcp = (Category.NETWORK, Direction.SEND, Protocol.TCP, '-- NETWORK SEND TCP MESSAGE Begin --')
	sendUdp = (Category.NETWORK, Direction.SEND, Protocol.UDP, '-- NETWORK SEND UDP MESSAGE Begin --')
	sendStcp = (Category.NETWORK, Direction.SEND, Protocol.STCP, '-- NETWORK SEND STCP MESSAGE Begin --')
	sendTls = (Category.NETWORK, Direction.SEND, Protocol.TLS, '-- NETWORK SEND TLS MESSAGE Begin --')
	sendSsl = (Category.NETWORK, Direction.SEND, Protocol.SSL, '-- NETWORK SEND SSL MESSAGE Begin --')
	sendMcast = (Category.NETWORK, Direction.SEND, Protocol.MCAST, '-- NETWORK SEND MCAST MESSAGE Begin --')
	sendWss = (Category.NETWORK, Direction.SEND, Protocol.WSS, '-- NETWORK SEND WSS MESSAGE Begin --')
	receiveStcp = (Category.NETWORK, Direction.RECEIVE, Protocol.STCP, '-- NETWORK RECEIVE STCP MESSAGE Begin --')
	receiveSsl = (Category.NETWORK, Direction.RECEIVE, Protocol.SSL, '-- NETWORK RECEIVE SSL MESSAGE Begin --')
	receiveTls = (Category.NETWORK, Direction.RECEIVE, Protocol.TLS, '-- NETWORK RECEIVE TLS MESSAGE Begin --')
	receiveWss = (Category.NETWORK, Direction.RECEIVE, Protocol.WSS, '-- NETWORK RECEIVE WSS MESSAGE Begin --')
	subReceiveTcp = (Category.NETWORK, Direction.RECEIVE, Protocol.TCP, '-- NETWORK RECEIVE TCP MESSAGE Begin --')
	subReceiveUdp = (Category.NETWORK, Direction.RECEIVE, Protocol.UDP, '-- NETWORK RECEIVE UDP MESSAGE Begin --')
	subReceiveStcp = (Category.NETWORK, Direction.RECEIVE, Protocol.STCP, '-- NETWORK RECEIVE STCP MESSAGE Begin --')
	subReceiveTls = (Category.NETWORK, Direction.RECEIVE, Protocol.TLS, '-- NETWORK RECEIVE TLS MESSAGE Begin --')
	subReceiveSsl = (Category.NETWORK, Direction.RECEIVE, Protocol.SSL, '-- NETWORK RECEIVE SSL MESSAGE Begin --')
	subReceiveMcast = (Category.NETWORK, Direction.RECEIVE, Protocol.MCAST, '-- NETWORK RECEIVE MCAST MESSAGE Begin --')
