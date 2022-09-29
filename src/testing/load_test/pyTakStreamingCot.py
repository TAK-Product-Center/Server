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
from mission_api import MissionApiPyTAKHelper
from utils import p12_to_pem


class PyTAKStreamingCot:

    def __init__(self,
                 address=None,
                 certfile=None,
                 password=None,
                 uid=None,
                 self_sa_delta=1.0,
                 mission_config=None,
                 data_dict=None,
                 debug=False):

        self.certfile = certfile
        self.password = password
        self.address = address

        self.uid = uid or CotMessage().uid
        self.self_sa_delta = self_sa_delta
        self.last_sa_write = time.time() + self.self_sa_delta

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

        self.pool = ThreadPoolExecutor(max_workers=2)

        self.read_thread: Process = None
        self.read_socket: zmq.Socket = None
        self.read_pool: Pool = None

    async def read_handler(self, reader):
        while True:
            data = await reader.readuntil(b'</event>')
            self.logger.debug("Client receives an event message")
            if self.mission_config.get("react_to_change_message", False):
                await self.read_socket.send(data)

            connection_data = self.data_dict[self.uid]
            connection_data['read'] += 1
            #connection_data['bytes'] += len(data)
            self.data_dict[self.uid] = connection_data
                
    async def send_self_sa(self, writer):
        self.logger.debug("Client sends self_sa message")
        self_sa_message = CotMessage(uid=self.uid, lat=str(self.location[0]), lon=str(self.location[1]))
        self_sa_message.add_callsign_detail(group_name="Red", platform="PyTAKStreamingCot")
        writer.write(self_sa_message.to_string())
        await writer.drain()
        self.last_sa_write = time.time()

    async def send_mission_cot(self, writer):
        self.logger.debug("Client sending mission_cot")
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
            mission_cot = CotMessage(uid=mission_cot_uid, lat=mission_loc[0], lon=mission_loc[1])
            if self.mission_cot_count[mission] == 0 or send_only_new_tracks:
                dest_attr = {"mission": mission}
                mission_cot.add_sub_detail("marti", "dest", dest_attr)
            writer.write(mission_cot.to_string())
            await writer.drain()
            self.mission_cot_count[mission] += 1
        self.did_mission_write = True

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
            while True:
                write_action = await self.time_to_write()
                if write_action == "self_sa":
                    await self.send_self_sa(writer)
                elif write_action == "mission_cot":
                    await self.send_mission_cot(writer)
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
                reader, writer = await asyncio.open_connection(self.address[0], self.address[1], ssl=self.ssl_context)
                times_connected += 1
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
                    self.logger.error("finishing task: {}".format(task))
                    task.cancel()
                for task in done:
                    self.logger.error("done task:      {}".format(task))
                    task.exception()
            except Exception as e:
                self.logger.error(str(type(e)) + ": " + str(e))

            finally:
                if self.mission_config.get("react_to_change_message", False):
                    self.read_socket.close()
                    self.read_pool.terminate()
                    self.read_pool.join()

                if writer is not None:
                    writer.close()
                    writer.transport.abort()

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
            cot_message = CotMessage(msg=data)
            if cot_message.is_sa():
                continue

            change_type, change_data = cot_message.mission_change()
            if change_type:
                send_data = {'req_type': change_type, 'req_data': change_data}
                data_sync_sock.send_json(send_data, flags=zmq.NOBLOCK)

            fileshare_data = cot_message.fileshare()
            if fileshare_data:
                send_data = {'req_type': 'download_file', 'req_data': fileshare_data}
                data_sync_sock.send_json(send_data, flags=zmq.NOBLOCK)

    except Exception as e:
        print("read_thread error: " + str(type(e)) + " -> " + str(e.args))
        raise e


class PyTAKStreamingCotProcess(multiprocessing.Process):
    def __init__(self, address=None, uid=None, cert=None, password=None, self_sa_delta=5.0,
                 mission_config=None, data_dict=None, debug=None):
        multiprocessing.Process.__init__(self)
        self.address = address
        self.uid = uid
        self.cert = cert
        self.password = password
        self.self_sa_delta = self_sa_delta
        self.mission_config = mission_config
        self.data_dict = data_dict
        self.debug = debug
        self.agent = None

    def run(self):
        try:
            self.agent = PyTAKStreamingCot(address=self.address,
                                           uid=self.uid,
                                           certfile=self.cert,
                                           password=self.password,
                                           self_sa_delta=self.self_sa_delta,
                                           mission_config=self.mission_config,
                                           data_dict=self.data_dict,
                                           debug=self.debug)
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

    socket = PyTAKStreamingCot(address=("3.82.2.223", 8089),
                               certfile="certs/takserver.p12", password="atakatak",
                               debug=True)

    asyncio.get_event_loop().run_until_complete(socket.connect_socket())
