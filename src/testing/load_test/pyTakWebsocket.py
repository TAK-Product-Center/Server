import asyncio
import multiprocessing
import random
import ssl
import time
from collections import defaultdict

import websockets
from requests.exceptions import ConnectionError, ConnectTimeout

from create_proto import CotProtoMessage
from mission_api import MissionApiPyTAKHelper
from utils import p12_to_pem

import zmq
import zmq.asyncio as azmq

from multiprocessing import Process, Pool


class PyTAKWebsocket:

    def __init__(self,
                 address=None,
                 websocket_path=None,
                 certfile=None,
                 password=None,
                 uid=None,
                 self_sa_delta=None,
                 mission_config=None,
                 data_dict=None):

        self.certfile = certfile
        self.password = password
        self.address = address
        self.websocket_path = websocket_path
        self.uri = "wss://{host}:{port}/{path}".format(host=self.address[0],
                                                       port=self.address[1],
                                                       path=self.websocket_path)

        self.uid = uid or CotProtoMessage().uid
        self.self_sa_delta = self_sa_delta
        self.last_sa_write = time.time() - self.self_sa_delta

        if mission_config is not None:
            self.mission_config = mission_config
            self.mission_cot_count = defaultdict(int)
            self.mission_locations = dict()
            self.mission_cot_delta = self.mission_config.get("mission_write_interval")
            self.last_mission_write = time.time() + self.mission_cot_delta
        else:
            self.mission_config = dict()

        self.data_sync_sess = MissionApiPyTAKHelper(self.address[0], self.address[1],
                                                    certfile=self.certfile,
                                                    password=self.password,
                                                    uid=self.uid,
                                                    mission_config=mission_config)

        self.ssl_context = ssl.SSLContext(ssl.PROTOCOL_TLSv1_2)

        with p12_to_pem(self.certfile, self.password) as cert:
            self.ssl_context.load_cert_chain(cert)

        self.location = (random.uniform(-90, 90), random.uniform(-180, 180))
        self.data_dict = data_dict if data_dict is not None else {}
        self.data_dict[self.uid] = {'write': 0, 'read': 0, 'connected': False}

        self.read_socket: azmq.Socket = None
        self.read_pool: Pool = None


    async def read_handler(self, websocket):
        async for message in websocket:
            if self.mission_config.get("react_to_change_message", False):
                await self.read_socket.send(message)
            # connection_data = self.data_dict[self.uid]
            # connection_data['read'] += 1
            # self.data_dict[self.uid] = connection_data

    async def send_self_sa(self, websocket):
        self_sa_message = CotProtoMessage(uid=self.uid, lat=str(self.location[0]), lon=str(self.location[1]))
        self_sa_message.add_callsign_detail(group_name="Cyan", platform="PyTAKWebsocketProto")
        await websocket.send(self_sa_message.serialize())
        self.last_sa_write = time.time()

    async def send_mission_cot(self, websocket):
        for mission in self.data_sync_sess.missions_subscribed:
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
            await websocket.send(mission_cot_proto.serialize())
            self.mission_cot_count[mission] += 1
        self.last_mission_write = time.time()

    async def time_to_write(self):
        while True:
            now = time.time()
            if now - self.last_sa_write > self.self_sa_delta:
                return "self_sa"
            if self.mission_config.get("send_mission_cot", False) and (
                    now - self.last_mission_write > self.mission_cot_delta):
                return "mission_cot"
            if self.data_sync_sess.ready_to_request():
                return "data_sync"
            await asyncio.sleep(0.5)

    async def write_handler(self, websocket):
        while True:
            write_action = await self.time_to_write()
            if write_action == "self_sa":
                await self.send_self_sa(websocket)
            elif write_action == "mission_cot":
                await self.send_mission_cot(websocket)
            # elif write_action == "data_sync":
            #     self.data_sync_sess.make_requests()
            await asyncio.sleep(0.1)
            connection_data = self.data_dict[self.uid]
            connection_data['write'] += 1
            self.data_dict[self.uid] = connection_data

    async def connect_websocket(self):
        times_connected = 0
        while True:
            try:
                self.data_sync_sess.get("https://{host}:{port}/".format(host=self.address[0], port=self.address[1]))
                session_cookie = "JSESSIONID=" + dict(self.data_sync_sess.session.cookies).get("JSESSIONID")
                async with websockets.connect(self.uri, ssl=self.ssl_context,
                                              extra_headers={"cookie": session_cookie}) as ws:
                    connection_data = self.data_dict[self.uid]
                    connection_data['connected'] = True
                    self.data_dict[self.uid] = connection_data
                    times_connected += 1

                    if self.mission_config.get('react_to_change_message', False):
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

                    read_task = asyncio.ensure_future(self.read_handler(ws))
                    write_task = asyncio.ensure_future(self.write_handler(ws))
                    done, pending = await asyncio.wait([read_task, write_task], return_when=asyncio.FIRST_COMPLETED)
                    for task in pending:
                        print(self.uid, "finishing?", task)
                        task.cancel()
                    for task in done:
                        print(self.uid, "done?", task)

            except ConnectionRefusedError as e:
                print(self.uid, "connection refused", e)
            except websockets.ConnectionClosed as e:
                print(self.uid, "connection closed?", e)
            except ConnectTimeout as e:
                print(self.uid, "ConnectTimeout", e)
            except ConnectionError as e:
                print(self.uid, "ConnectionError: are you sure that", e.request.url, "is the correct address?")
            except Exception as e:
                print(self.uid, type(e), e)
            connection_data = self.data_dict[self.uid]
            connection_data['connected'] = False
            self.data_dict[self.uid] = connection_data
            print(self.uid, "retry #", times_connected)
            await asyncio.sleep(0.5)


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


class PyTAKWebsocketProcess(multiprocessing.Process):
    def __init__(self, address=None, websocket_path="takproto/1", uid=None, cert=None, password=None,
                 self_sa_delta=5.0, mission_config=None, data_dict=None):
        multiprocessing.Process.__init__(self)
        self.address = address
        self.websocket_path = websocket_path
        self.uid = uid
        self.cert = cert
        self.password = password
        self.self_sa_delta = self_sa_delta
        self.mission_config = mission_config
        self.data_dict = data_dict
        self.agent = None

    def run(self):
        try:
            self.agent = PyTAKWebsocket(address=self.address,
                                        websocket_path=self.websocket_path,
                                        uid=self.uid,
                                        certfile=self.cert,
                                        password=self.password,
                                        self_sa_delta=self.self_sa_delta,
                                        mission_config=self.mission_config,
                                        data_dict=self.data_dict)
            asyncio.get_event_loop().run_until_complete(self.agent.connect_websocket())

        except KeyboardInterrupt:
            pass
        finally:
            return



