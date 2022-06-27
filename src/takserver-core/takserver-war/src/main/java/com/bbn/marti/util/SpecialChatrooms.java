package com.bbn.marti.util;

/**
 * Enum to hold all the special chatrooms we are supporting for WebTAK
 */
public enum SpecialChatrooms {
   ALL_STREAMING("All Streaming"),
   ALL_CHAT("All Chat Rooms");
   public final String name;

   SpecialChatrooms(String name) { this.name = name; }
}
