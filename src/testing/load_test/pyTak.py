import asyncio
import multiprocessing
import ssl
import random
import time

from collections import defaultdict
from create_cot import CotMessage
from mission_api import MissionApiPyTAKHelper
from utils import p12_to_pem, is_sa_message, mission_change


class TLSClientProtocol(asyncio.Protocol):
    def __init__(self, uid, certfile, password, address, mission_config, data_dict):
        self.uid = uid
        self.certfile = certfile
        self.password = password
        self.address = address
        self.mission_config = mission_config or {}
        self.data_dict = data_dict if data_dict is not None else {}
        self.data_dict[self.uid] = {'write': 0, 'read': 0, 'connected': False}

        self.transport = None
        self.handshake_happened = False

        self.last_self_sa = time.time() - 5.0  # example
        self.self_sa_delta = 5.0
        self.write_mission_cot = lambda: False
        if self.mission_config.get('send_mission_cot', False):
            self.last_mission_delta = self.mission_config['mission_write_interval']
            self.last_mission_write = time.time() + self.last_mission_delta
            self.write_mission_cot = lambda: (time.time() - self.last_mission_write > self.last_mission_delta)

        mission_api_host, mission_api_port = self.mission_config.get('address', (self.address[0], 8443))
        self.data_sync_sess = MissionApiPyTAKHelper(
            mission_api_host, mission_api_port,
            certfile=self.certfile,
            password=self.password,
            uid=self.uid,
            mission_config=mission_config
        )

        self.location = (random.uniform(-90, 90), random.uniform(-180, 180))
        self.mission_locations = dict()
        self.mission_cot_count = defaultdict(int)

        self.really_close = False

    def connection_made(self, transport):
        print(f"Connection made: {self.uid}")
        self.transport = transport
        self.handshake_happened = True
        self.data_dict[self.uid]['connected'] = True

    def data_received(self, data):
        # Called whenever data is received from server
        try:
            if is_sa_message(data):
                return
            if not self.mission_config.get("react_to_change_message", False):
                return
            action, mission_name = mission_change(data)
            if action:
                self.data_sync_sess.request_queue.append((action, mission_name))
        except Exception as e:
            print(f"Error processing data_received: {e}")

    def connection_lost(self, exc):
        print(f"Connection lost: {self.uid}, exc={exc}")
        self.data_dict[self.uid]['connected'] = False
        # Could schedule reconnect here, or handle from outside

    def writable(self):
        now = time.time()
        if (now - self.last_self_sa) > self.self_sa_delta:
            return True
        if self.data_sync_sess.ready_to_request():
            return True
        if self.write_mission_cot():
            return True
        return False

    def send_self_sa(self):
        self_sa = CotMessage(uid=self.uid, lat=str(self.location[0]), lon=str(self.location[1]))
        self_sa.add_callsign_detail()
        try:
            self.transport.write(self_sa.to_string())
            self.last_self_sa = time.time()
        except Exception as e:
            print(f"Error sending self_sa: {e}")

    def send_mission_cots(self):
        for mission in self.data_sync_sess.missions_subscribed:
            mission_loc = self.mission_locations.get(mission)
            if mission_loc is None:
                mission_loc = (str(random.uniform(-90, 90)), str(random.uniform(-180, 180)))
                self.mission_locations[mission] = mission_loc
            else:
                lat = float(mission_loc[0]) + random.choice([-1, 0, 1])
                lat = min(max(lat, -90), 90)
                lon = float(mission_loc[1]) + random.choice([-1, 0, 1])
                lon = (lon + 180) % 360 - 180  # wrap longitude correctly
                mission_loc = (str(lat), str(lon))
                self.mission_locations[mission] = mission_loc

            send_only_new_tracks = self.mission_config.get("send_only_new_tracks", False)
            mission_cot_uid = f"{self.uid}_mission_{mission}"
            if send_only_new_tracks:
                mission_cot_uid += f"_{self.mission_cot_count[mission]}"
            mission_cot = CotMessage(uid=mission_cot_uid, lat=mission_loc[0], lon=mission_loc[1])
            if self.mission_cot_count[mission] == 0 or send_only_new_tracks:
                mission_cot.add_sub_detail("marti", "dest", {"mission": mission})
            self.mission_cot_count[mission] += 1
            try:
                self.transport.write(mission_cot.to_string())
            except Exception as e:
                print(f"Error sending mission_cot: {e}")
        self.last_mission_write = time.time()

    async def run_loop(self):
        while True:
            await asyncio.sleep(0.1)  # Adjust frequency as needed

            if self.writable():
                if (time.time() - self.last_self_sa) > self.self_sa_delta:
                    self.send_self_sa()

                if self.write_mission_cot():
                    self.send_mission_cots()

                if self.data_sync_sess.ready_to_request():
                    self.data_sync_sess.make_requests()


class TLSClientProcess(multiprocessing.Process):
    def __init__(self, address=None, uid=None, cert=None, password=None, self_sa_delta=5.0, mission_config=None):
        multiprocessing.Process.__init__(self)
        self.address = address
        self.uid = uid
        self.cert = cert
        self.password = password
        self.self_sa_delta = self_sa_delta
        self.mission_config = mission_config

    def run(self):
        asyncio.run(self.main())

    async def main(self):
        ssl_context = ssl.create_default_context(ssl.Purpose.SERVER_AUTH)
        if self.cert:
            # Assuming you have PEM cert here, if p12, you'll have to convert before
            ssl_context.load_cert_chain(certfile=self.cert, password=self.password)

        while True:
            try:
                loop = asyncio.get_running_loop()
                transport, protocol = await loop.create_connection(
                    lambda: TLSClientProtocol(
                        uid=self.uid,
                        certfile=self.cert,
                        password=self.password,
                        address=self.address,
                        mission_config=self.mission_config,
                        data_dict={}
                    ),
                    host=self.address[0],
                    port=self.address[1],
                    ssl=ssl_context
                )
                await protocol.run_loop()
            except KeyboardInterrupt:
                print("Interrupted, shutting down")
                if transport:
                    transport.close()
                break
            except Exception as e:
                print(f"Connection error: {e}, reconnecting in 5 seconds")
                await asyncio.sleep(5)
