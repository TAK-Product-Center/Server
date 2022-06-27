package tak.server.plugins;

import atakmap.commoncommo.protobuf.v1.MessageOuterClass.Message;

public interface MessageSenderReceiver extends Sender<Message>, Receiver<Message> { }