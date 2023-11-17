import asyncio
import logging
import multiprocessing
import random
import ssl
from collections import defaultdict

from multiprocessing import Process, Pool
import threading
import requests
from concurrent.futures import ThreadPoolExecutor
from functools import partial

import zmq
import zmq.asyncio as azmq

import sys
import time

from create_cot import CotMessage
from create_proto import CotProtoMessage, get_msg_size
from mission_api import MissionApiPyTAKHelper
from utils import p12_to_pem

import stats

class PyTAKStreamingProto:

    def __init__(self,
                 address=None,
                 certfile=None,
                 password=None,
                 uid=None,
                 self_sa_delta=1.0,
                 mission_config=None,
                 data_dict=None,
                 ping=False,
                 ping_interval=1000, # in ms
                 arr=None,
                 debug=False):

        self.certfile = certfile
        self.password = password
        self.address = address

        self.uid = uid or CotProtoMessage().uid
        self.self_sa_delta = self_sa_delta
        self.last_sa_write = time.time() + self.self_sa_delta

        self.ping = ping
        self.ping_interval = ping_interval # in ms
        self.last_ping_write = time.time()*1000 + self.ping_interval # in ms
        self.arr = arr

        if mission_config is not None:
            self.mission_config = mission_config
            self.mission_cot_count = defaultdict(int)
            self.mission_locations = dict()
            self.mission_cot_delta = self.mission_config.get("mission_write_interval")
            self.mission_cot_prob = self.mission_config.get("send_mission_cot_probability")
            self.last_mission_write = time.time()
            self.next_mission_write = self.last_mission_write + random.uniform(0, self.mission_cot_delta)
            self.did_mission_write = False
        else:
            self.mission_config = dict()
            self.mission_config['address'] = self.address

        mission_api_host = self.mission_config.get('address')[0]
        mission_api_port = self.mission_config.get('address')[1]
        self.data_sync_sess = MissionApiPyTAKHelper(mission_api_host, mission_api_port,
                                                    certfile=self.certfile,
                                                    password=self.password,
                                                    uid=self.uid,
                                                    mission_config=mission_config)

        self.ssl_context = ssl.SSLContext(ssl.PROTOCOL_TLS)

        with p12_to_pem(self.certfile, self.password) as cert:
            self.ssl_context.load_cert_chain(cert)

        self.location = (random.uniform(-90, 90), random.uniform(-180, 180))
        self.data_dict = data_dict if data_dict is not None else dict()
        self.data_dict[self.uid] = {'write': 0, 'read': 0, 'bytes': 0, 'connected': False}

        self.logger = logging.getLogger(self.uid)
        if debug:
            self.logger.setLevel(logging.DEBUG)

        if sys.version_info >= (3, 8):
            formatter = logging.Formatter('%(asctime)s - %(name)s - %(levelname)s:\t %(message)s')
            handler = logging.StreamHandler()
            handler.setFormatter(formatter)
            handler.setStream(sys.stdout)
            handler.setLevel(logging.INFO)
            self.logger.addHandler(handler)

        self.negotiated = False
        self.buffer = None

        self.pool = ThreadPoolExecutor(max_workers=2)

        self.read_socket: azmq.Socket = None
        self.read_pool: Pool = None

    async def negotiate_protocol(self, reader, writer):
        self.negotiated = False
        self.logger.debug("negotiating protocol")
        sent_request = False
        while not self.negotiated:
            try:
                data = await reader.readuntil(b'</event>')
            except asyncio.LimitOverrunError as e:
                if sent_request:
                    data = await reader.read(reader._limit)
                    if data[0].to_bytes(1, byteorder='big') == b'\xbf':
                        self.buffer = data
                        self.negotiated = True
                        return
                raise e

            self.logger.debug(data)

            msg = CotMessage(msg=data)
            if msg.server_protocol_version_support() == '1':
                response = CotMessage(uid=msg.uid)
                response.protocol_response_message(version='1')
                writer.write(response.to_string())
                await writer.drain()
                sent_request = True

            elif msg.server_protocol_negotiation_handshake():
                self.negotiated = True

        self.logger.debug("done negotiating")

    async def read_handler(self, reader):
        leftovers = None
        msg_size = 0
        if self.buffer is not None:
            data = self.buffer
        else:
            data = await reader.read(reader._limit)
        while True:
            
            if leftovers is None:
                fullbuffer = data
            else:
                fullbuffer = leftovers + data
                leftovers = None
            while len(fullbuffer) > 0:
                if msg_size == 0:  # get the message size
                    first_byte = fullbuffer[0].to_bytes(1, byteorder="big")
                    if first_byte != b'\xbf':
                        raise Exception("Magic byte is wrong: " + str(first_byte))
                    msg_size = get_msg_size(fullbuffer[1:])
                    if not msg_size:
                        leftovers = fullbuffer
                        break
                if len(fullbuffer) < msg_size:
                    leftovers = fullbuffer
                    break

                next_msg = fullbuffer[:msg_size]
                fullbuffer = fullbuffer[msg_size:]

                if self.arr is not None:
                    self.arr[stats.MESSAGES_RECEIVED_INDEX] += 1
                # deal with pong messages here, not send it to read_socket
                proto_message = CotProtoMessage(msg=next_msg)
                if proto_message.is_pong():
                    # print("~~~Streaming proto: Received a pong message")
                    self.arr[stats.MESSAGES_PONG_COUNT_INDEX] += 1
                    self.arr[stats.TIME_BETWEEN_PING_PONG] += round(time.time()*1000 - self.last_ping_write) # in ms
                else:
                    if self.mission_config.get("react_to_change_message", False):
                        await self.read_socket.send(next_msg)

                connection_data = self.data_dict[self.uid]
                connection_data['read'] += 1
                #connection_data['bytes'] += len(data)
                self.data_dict[self.uid] = connection_data

                leftovers = None
                msg_size = 0

            data = await reader.read(reader._limit)
            if data == b'':
                await asyncio.sleep(0.0001)


    async def send_self_sa(self, writer):
        self.location = (random.uniform(-90, 90), random.uniform(-180, 180))
        self_sa_message = CotProtoMessage(uid=self.uid, lat=str(self.location[0]), lon=str(self.location[1]))
        self_sa_message.add_callsign_detail(group_name="Red", platform="PyTAKStreamingProto")
        writer.write(self_sa_message.serialize())
        await writer.drain()
        self.last_sa_write = time.time()

    async def send_mission_cot(self, writer):
        for mission in self.data_sync_sess.missions_subscribed:
            if self.mission_cot_prob < random.uniform(0, 1):
                continue
            mission_loc = self.mission_locations.get(mission)
            if mission_loc is None:
                mission_loc = (str(random.uniform(-90, 90)), str(random.uniform(-180, 180)))
                self.mission_locations[mission] = mission_loc
            else:
                lat = float(mission_loc[0]) + random.choice([-1, 0, 1])
                if lat < -90:
                    lat = -90
                elif lat > 90:
                    lat = 90
                lon = float(mission_loc[1]) + random.choice([-1, 0, 1])
                if lon < -180:
                    lon = 180
                elif lon > 180:
                    lon = -180
                mission_loc = (str(lat), str(lon))
                self.mission_locations[mission] = mission_loc

            send_only_new_tracks = self.mission_config.get("send_only_new_tracks", False)
            mission_cot_uid = self.uid + "_mission_" + mission
            if send_only_new_tracks:
                mission_cot_uid += "_" + str(self.mission_cot_count[mission])
            mission_cot_proto = CotProtoMessage(uid=mission_cot_uid, lat=mission_loc[0],
                                                lon=mission_loc[1])
            if self.mission_cot_count[mission] == 0 or send_only_new_tracks:
                dest_attr = {"mission": mission}
                mission_cot_proto.add_sub_detail("marti", "dest", dest_attr)
            writer.write(mission_cot_proto.serialize())
            await writer.drain()
            self.mission_cot_count[mission] += 1
        self.did_mission_write = True

    async def send_ping(self, writer):
        ping_message = CotProtoMessage(uid=self.uid, lat=str(self.location[0]), lon=str(self.location[1]), type="t-x-c-t")
        writer.write(ping_message.serialize())
        # print(f"sending Ping: {ping_message.serialize()}")
        await writer.drain()
        self.last_ping_write = time.time()*1000 # in ms

    async def time_to_write(self):
        while True:
            now = time.time()
            if now - self.last_sa_write > self.self_sa_delta:
                return "self_sa"
            if self.mission_config.get("send_mission_cot", False):
                if (not self.did_mission_write) and (now > self.next_mission_write):
                    return "mission_cot"
                if now - self.last_mission_write > self.mission_cot_delta:
                    self.last_mission_write = now
                    self.next_mission_write = self.last_mission_write + random.uniform(0, self.mission_cot_delta)
                    self.did_mission_write = False
            # if self.data_sync_sess.ready_to_request():
            #     return "data_sync"
            if (now * 1000 - self.last_ping_write) > self.ping_interval: # in ms
                return "ping"
            await asyncio.sleep(0.1)

    def data_sync_write_target(self, event):
        while True:
            if event.is_set():
                return
            if self.data_sync_sess.ready_to_request():
                try:
                    self.data_sync_sess.make_requests()
                except requests.Timeout as e:
                    self.logger.error(str(type(e)) + ": " + str(e))
                    raise e
            time.sleep(0.1)

    async def write_handler(self, writer):
        try:
            data_sync_event = threading.Event()
            data_sync_thread = threading.Thread(target=self.data_sync_write_target, args=(data_sync_event,))
            data_sync_thread.start()
            self.last_sa_write = time.time() - self.self_sa_delta
            self.last_ping_write = time.time()*1000 - self.ping_interval
            while True:
                write_action = await self.time_to_write()

                if write_action == "ping":
                    await self.send_ping(writer)
                    # print("~~~Streaming proto: Sent a ping message")
                    if self.arr is not None:
                        self.arr[stats.MESSAGES_PING_COUNT_INDEX] += 1
                        self.arr[stats.MESSAGES_SENT_INDEX] += 1

                elif write_action == "self_sa":
                    await self.send_self_sa(writer)
                    # print("~~~Streaming proto: Sent send_self_sa")
                    if self.arr is not None:
                        self.arr[stats.MESSAGES_SENT_INDEX] += 1

                elif write_action == "mission_cot":
                    await self.send_mission_cot(writer)
                    # print("~~~Streaming proto: Sent send_mission_cot")
                    if self.arr is not None:
                        self.arr[stats.MESSAGES_SENT_INDEX] += 1

                # elif write_action == "data_sync":
                #     await asyncio.get_event_loop().run_in_executor(self.pool, self.data_sync_sess.make_requests)

                await asyncio.get_event_loop().run_in_executor(self.pool, partial(data_sync_thread.join, timeout=0.1))
                if not data_sync_thread.is_alive():
                    raise RuntimeError("There was a mission api exception")
                await asyncio.sleep(0.01)
                connection_data = self.data_dict[self.uid]
                connection_data['write'] += 1
                self.data_dict[self.uid] = connection_data
        except asyncio.CancelledError:
            data_sync_event.set()
            data_sync_thread.join()
            raise


    async def connect_socket(self):
        times_connected = 0
        while True:
            try:
                self.logger.debug('trying')
                reader, writer = await asyncio.open_connection(self.address[0], self.address[1], ssl=self.ssl_context)
                if self.arr is not None:
                    self.arr[stats.CONNECT_EVENT_COUNT_INDEX] += 1 

                times_connected += 1
                await self.negotiate_protocol(reader, writer)
                connection_data = self.data_dict[self.uid]
                connection_data['connected'] = True
                self.data_dict[self.uid] = connection_data

                # if we dont react to change messages, then we dont need to parse any data
                if self.mission_config.get("react_to_change_message", False):
                    context = azmq.Context()
                    self.read_socket = context.socket(zmq.PUSH)
                    port = self.read_socket.bind_to_random_port('tcp://*')
                    data_sync_port = self.data_sync_sess.port
                    POOL_SIZE = 3
                    self.read_pool = Pool(POOL_SIZE)

                    for i in range(POOL_SIZE):
                        self.read_pool.apply_async(read_thread_zmq, args=(port, data_sync_port))
                    await asyncio.sleep(1)

                # get list of recent client connect / disconnect events
                self.data_sync_sess.get_client_endpoints()

                read_task = asyncio.ensure_future(self.read_handler(reader))
                write_task = asyncio.ensure_future(self.write_handler(writer))

                done, pending = await asyncio.wait([read_task, write_task], return_when=asyncio.FIRST_COMPLETED)
                for task in pending:
                    self.logger.error("read/write task in pending: {}".format(task))
                    task.cancel()
                for task in done:
                    self.logger.error("read/write task done:      {}".format(task))
                    task.exception()
            except Exception as e:
                self.logger.error(str(type(e)) + ": " + str(e))


            finally:
                self.logger.error("closing connection in order to restart")
                if self.mission_config.get("react_to_change_message", False):
                    self.read_socket.close()
                    self.read_pool.terminate()
                    self.read_pool.join()

                if writer is not None:
                    writer.close()
                    writer.transport.abort()

                if self.arr is not None:
                    self.arr[stats.DISCONNECT_EVENT_COUNT_INDEX] += 1

                connection_data = self.data_dict[self.uid]
                connection_data['connected'] = False
                self.data_dict[self.uid] = connection_data
                self.logger.error("retry # %d" % times_connected)
                await asyncio.sleep(1)

def read_thread_zmq(port: int, data_sync_port):
    context = zmq.Context()
    socket = context.socket(zmq.PULL)
    data_sync_sock = context.socket(zmq.PUSH)
    addr = 'tcp://localhost:' + str(port)
    data_sync_addr = 'tcp://localhost:' + str(data_sync_port)
    socket.connect(addr)
    data_sync_sock.connect(data_sync_addr)

    try:
        while True:
            data = socket.recv()
            proto_message = CotProtoMessage(msg=data)

            if proto_message.is_sa():
                continue

            change_type, change_data = proto_message.mission_change()
            if change_type:
                send_data = {'req_type': change_type, 'req_data': change_data}
                data_sync_sock.send_json(send_data, flags=zmq.NOBLOCK)

            fileshare_data = proto_message.fileshare()
            if fileshare_data:
                send_data = {'req_type': 'download_file', 'req_data': fileshare_data}
                data_sync_sock.send_json(send_data, flags=zmq.NOBLOCK)

    except Exception as e:
        print("read_thread error: " + str(type(e)) + " -> " + str(e.args))
        raise e


class PyTAKStreamingProtoProcess(multiprocessing.Process):
    def __init__(self, address=None, uid=None, cert=None, password=None, self_sa_delta=5.0,
                 mission_config=None, data_dict=None,
                 ping=False, ping_interval=1000, arr=None):
        multiprocessing.Process.__init__(self)
        self.address = address
        self.uid = uid
        self.cert = cert
        self.password = password
        self.self_sa_delta = self_sa_delta
        self.mission_config = mission_config
        self.data_dict = data_dict
        self.agent = None
        self.ping = ping
        self.ping_interval = ping_interval
        self.arr = arr # multiprocessing.Array to store metric data for this client

    def run(self):
        try:
            self.agent = PyTAKStreamingProto(address=self.address,
                                             uid=self.uid,
                                             certfile=self.cert,
                                             password=self.password,
                                             self_sa_delta=self.self_sa_delta,
                                             mission_config=self.mission_config,
                                             data_dict=self.data_dict,
                                             ping=self.ping,
                                             ping_interval=self.ping_interval,
                                             arr = self.arr
                                             )
            if self.uid is None:
                self.uid = self.agent.uid
            asyncio.get_event_loop().run_until_complete(self.agent.connect_socket())

        except KeyboardInterrupt:
            pass
        finally:
            if self.agent.read_pool is not None:
                self.agent.read_pool.terminate()
                self.agent.read_pool.join()
            return


if __name__ == "__main__":
    logging.basicConfig(level=logging.ERROR,
                        format='%(asctime)s - %(name)s - %(levelname)s:\t %(message)s',
                        stream=sys.stdout)
    log = logging.getLogger('main')
    log.setLevel(logging.INFO)

    socket = PyTAKStreamingProto(address=("3.82.2.223", 8089),
                                 certfile="certs/takserver.p12", password="atakatak",
                                 debug=True)

    asyncio.get_event_loop().run_until_complete(socket.connect_socket())
