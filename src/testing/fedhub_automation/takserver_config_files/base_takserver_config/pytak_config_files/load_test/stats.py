
# global stats_cot
# stats_cot = {
#         'messages_sent': 0,
#         'messages_received': 0,
#         'connect_count': 0,
#         'disconnect_count': 0,
#         'ping_count': 0,
#         'pong_count': 0,
# }

# global stats_cot_proto
# stats_cot_proto = {
#         'messages_sent': 0,
#         'messages_received': 0,
#         'connect_count': 0,
#         'disconnect_count': 0,
#         'ping_count': 0,
#         'pong_count': 0,
# }
# def reset_stats(stats):
#     stats['messages_sent'] = 0
#     stats['messages_received'] = 0
#     stats['connect_count'] = 0
#     stats['disconnect_count'] = 0
#     stats['ping_count'] = 0
#     stats['pong_count'] = 0

import threading
from multiprocessing import Process, Value, Array

# indices in metric arrays
MESSAGES_SENT_INDEX = 0
MESSAGES_RECEIVED_INDEX = 1
CONNECT_EVENT_COUNT_INDEX = 2
DISCONNECT_EVENT_COUNT_INDEX = 3
MESSAGES_PING_COUNT_INDEX = 4
MESSAGES_PONG_COUNT_INDEX = 5
TIME_BETWEEN_PING_PONG = 6 # in ms

class Stats:

    def __init__(self):
        self.data = {} # key is uid, value is a dictionary
        self.threadLock = threading.Lock()

    def init_uid(self, uid):
        try:
            self.threadLock.acquire()
            self.data[uid] = Array('i', range(7))
            for i in range(len(self.data[uid])):
                self.data[uid][i] = 0
            return self.data[uid]
        finally:
            self.threadLock.release()

    def get_uid(self, uid):
        try:
            self.threadLock.acquire()
            return self.data.get(uid)
        finally:
            self.threadLock.release()


