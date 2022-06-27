"""
SSL/TLS negotiation.
"""

import asyncore
import multiprocessing
import random
import socket
import ssl


from collections import defaultdict

import time

from create_cot import CotMessage
from mission_api import MissionApiPyTAKHelper
from utils import p12_to_pem, is_sa_message, mission_change


class TLSHandshake(asyncore.dispatcher):
    """
    Negotiates a SSL/TLS connection before handing itself spawning a
    dispatcher that can deal with the overlying protocol as soon as the
    handshake has been completed.
    `address` is a tuple consisting of hostname/address and port to connect to
    if nothing is passed in `sock`, which can take an already-connected socket.
    `certfile` can take a path to a certificate bundle, and `server_side`
    indicates whether the socket is intended to be a server-side or client-side
    socket.
    """

    want_read = want_write = True

    def __init__(self,
                 address=None,
                 sock=None,
                 certfile=None,
                 password=None,
                 uid=None,
                 self_sa_delta=5.0,
                 mission_config=None,
                 data_dict=None):

        asyncore.dispatcher.__init__(self, sock)
        self.certfile = certfile
        self.password = password
        self.address = address

        self.uid = uid or CotMessage().uid
        self.self_sa_delta = self_sa_delta
        self.last_self_sa = time.time() - self.self_sa_delta
        self.write_self_sa = lambda: (time.time() - self.last_self_sa > self.self_sa_delta)

        if mission_config is not None:
            self.mission_config = mission_config
            self.last_mission_delta = self.mission_config['mission_write_interval']
            self.last_mission_write = time.time() + self.last_mission_delta
        else:
            self.mission_config = dict()

        if self.mission_config.get('send_mission_cot', False):
            self.write_mission_cot = lambda: (time.time() - self.last_mission_write > self.last_mission_delta)
        else:
            self.write_mission_cot = lambda: False

        self.really_close = False

        mission_api_host = self.mission_config.get('address', (self.address[0], 8443))[0]
        mission_api_port = self.mission_config.get('address', (self.address[0], 8443))[1]
        self.data_sync_sess = MissionApiPyTAKHelper(mission_api_host, mission_api_port,
                                                    certfile=self.certfile,
                                                    password=self.password,
                                                    uid=self.uid,
                                                    mission_config=mission_config)

        self.reconnect(sock=sock)
        self.handshake_happened = False

        self.location = (random.uniform(-90, 90), random.uniform(-180, 180))
        self.mission_locations = dict()
        self.mission_cot_count = defaultdict(int)

        self.data_dict = data_dict if data_dict is not None else {}
        self.data_dict[self.uid] = {'write': 0, 'read': 0, 'connected': False}

    def reconnect(self, sock=None):
        self.handshake_happened = False
        self.want_read = self.want_write = True
        if sock is None:
            self.create_socket(socket.AF_INET, socket.SOCK_STREAM)
            print(('Connecting to {}:{}   {}'.format(self.address[0], self.address[1], self.uid)))
            tries = 0
            self.connected = False
            while not self.connected:
                tries += 1
                print("attempt number {} for uid:{}".format(tries, self.uid))
                try:
                    self.connect(self.address)
                except socket.error:
                    pass
                time.sleep(2)
            print("finally connected?", self.uid)

        elif self.connected:
            # Initiate the handshake for an already-connected socket.
            self.handle_connect()

    def handle_connect(self):
        # Once the connection has been established, it's safe to wrap the
        # socket.
        with p12_to_pem(self.certfile, self.password) as certfile:
            self.socket = ssl.wrap_socket(self.socket,
                                          certfile=certfile,
                                          ca_certs=certfile,
                                          cert_reqs=ssl.CERT_REQUIRED,
                                          do_handshake_on_connect=False)

    def writable(self):
        if self.want_write:
            return self.write_self_sa() or self.data_sync_sess.ready_to_request() or self.write_mission_cot()
        return False

    def readable(self):
        return self.want_read

    def _handshake(self):
        """
        Perform the handshake.
        """
        try:
            self.socket.do_handshake()
            print("handshake success", self.want_read, self.want_write, self.uid)
        except ssl.SSLError as err:
            self.want_read = self.want_write = False
            if err.args[0] == ssl.SSL_ERROR_WANT_READ:
                self.want_read = True
            elif err.args[0] == ssl.SSL_ERROR_WANT_WRITE:
                self.want_write = True
            else:
                print(err)
                raise
        else:
            self.handshake_happened = True
            self.want_read = self.want_write = True
            connection_data = self.data_dict[self.uid]
            connection_data['connected'] = True
            self.data_dict[self.uid] = connection_data

    def handle_read(self):
        if not self.handshake_happened:
            try:
                self._handshake()
            except Exception as e:
                print("haha", self.uid)
                print(e)
                self.really_close = True
                self.handle_close()

        try:
            data = self.recv(81920)
        except Exception:
            pass
        else:
            if is_sa_message(data):
                return
            if not self.mission_config.get("react_to_change_message", False):
                return
            action, mission_name = mission_change(data)
            if action:
                self.data_sync_sess.request_queue.append((action, mission_name))

    def handle_write(self):
        if not self.handshake_happened:
            self._handshake()

        if self.write_self_sa():
            self_sa = CotMessage(uid=self.uid, lat=str(self.location[0]), lon=str(self.location[1]))
            self_sa.add_callsign_detail()
            try:
                self.send(self_sa.to_string())
            except Exception as e:
                print("write error", self.uid)
                print(e)
            else:
                self.last_self_sa = time.time()

        if self.write_mission_cot():
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
                mission_cot = CotMessage(uid=mission_cot_uid, lat=mission_loc[0],
                                         lon=mission_loc[1])
                if self.mission_cot_count[mission] == 0 or send_only_new_tracks:
                    dest_attr = {"mission": mission}
                    mission_cot.add_sub_detail("marti", "dest", dest_attr)
                self.mission_cot_count[mission] += 1
                self.send(mission_cot.to_string())
            self.last_mission_write = time.time()

        if self.data_sync_sess.ready_to_request():
            self.data_sync_sess.make_requests()

    def handle_close(self):
        print("----------------------------------------------------------------")
        print("------------------ {} trying to close --------------------".format(self.uid))
        print("----------------------------------------------------------------")
        self.close()
        connection_data = self.data_dict[self.uid]
        connection_data['connected'] = False
        if not self.really_close:
            print("trying again", self.uid)
            time.sleep(1)
            self.reconnect()
        else:
            print(self.uid, "not really closing", self.uid)
            self.really_close = False
            time.sleep(10)
            print(self.uid, "ok, now lets try again")
            self.reconnect()


class TLSClientProcess(multiprocessing.Process):

    async_tls = None
    def __init__(self, address=None, uid=None, cert=None, password=None, self_sa_delta=5.0, mission_config=None):

        multiprocessing.Process.__init__(self)
        self.address = address
        self.uid = uid
        self.cert = cert
        self.password = password
        self.self_sa_delta = self_sa_delta
        self.mission_config = mission_config

    def run(self):
        try:
            print("starting", self.name, "id:", self.uid)

            self.async_tls = TLSHandshake(address=self.address,
                                          certfile=self.cert,
                                          password=self.password,
                                          uid=self.uid,
                                          self_sa_delta=self.self_sa_delta,
                                          mission_config=self.mission_config)

            asyncore.loop(0)

        except KeyboardInterrupt:
            # if we close all connections, then the asycnore loop for this process will end
            print("trying to close")
            asyncore.close_all()
