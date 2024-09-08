import cgi
import json
import multiprocessing
import os
import random
import requests
import time

import zmq

from pprint import pprint
from tempfile import NamedTemporaryFile

from requests import Session
from requests.adapters import HTTPAdapter

from OpenSSL import SSL

from utils import p12_to_pem


class IgnoreHostNameAdapter(HTTPAdapter):
    def init_poolmanager(self, *args, **kwargs):
        kwargs['assert_hostname'] = False
        return super(IgnoreHostNameAdapter, self).init_poolmanager(*args, **kwargs)


class MissionApiSession:

    def __init__(self, host, port, certfile=None, password=None, uid=None):
        self.base_url = "https://{host}:{port}/Marti/".format(host=host, port=port)
        self.base_mission_api = self.base_url + "api/missions/"
        self.base_sync_api = self.base_url + "sync/"

        self.session = Session()

        self.session.mount("https://", IgnoreHostNameAdapter())

        self.certfile = certfile
        self.password = password

        self.all_missions = dict()
        self.missions_created = dict()

        self.file_hash_map = dict()

        self.uid = uid

    def request(self, *args, **kwargs):
        with p12_to_pem(self.certfile, self.password) as cert_file:
            self.session.cert = cert_file
            self.session.verify = cert_file

            if "timeout" not in kwargs:
                kwargs['timeout'] = 60

            sslErrorRetry = 0
            requestExceptionRetry = 0
            exc = None
            while (sslErrorRetry < 10):
                try:
                    return self.session.request(*args, **kwargs)
                except SSL.Error as e: # not really sure why this happens, but it seems to be maybe that the temp file isn't yet available?
                    # print(e)
                    # print(self.uid, "SSL error, I think its a timing thing?", args, kwargs)
                    # print("trying again")
                    exc = e
                    sslErrorRetry += 1
                    time.sleep(0.1)
                except requests.Timeout as e:
                    exc = e
                    requestExceptionRetry += 1
                    time.sleep(0.1)
                except requests.RequestException as e:
                    exc = e
                    requestExceptionRetry += 1
                    time.sleep(0.1)
                if requestExceptionRetry > 2:
                    r = requests.Response()
                    r.status_code = 400
                    r._content = str(e).encode()
                    return r

            #print("failed to request:", args, kwargs)
            raise Exception("Failed to request: " + str(args) + str(kwargs))

    def get(self, *args, **kwargs):
        return self.request("get", *args, **kwargs)

    def post(self, *args, **kwargs):
        return self.request("post", *args, **kwargs)

    def put(self, *args, **kwargs):
        return self.request("put", *args, **kwargs)

    def delete(self, *args, **kwargs):
        return self.request("delete", *args, **kwargs)

    ############### Mission Information/Subscription #####################
    def get_all_missions(self):

        response = self.get(self.base_mission_api)

        if response.status_code != 200:
            print("ERROR: ", response.text)
            return

        mission_data = response.json()['data']

        for mission in mission_data:
            self.all_missions[mission['name']] = mission

    def get_mission(self, mission):

        response = self.get(self.base_mission_api + mission)

        if response.status_code != 200:
            print(("ERROR: ", response.text))
            return

        mission_data = response.json()['data']
        self.all_missions[mission] = mission_data

    def get_mission_cot(self, mission):

        response = self.get(self.base_mission_api + mission + "/cot")

        if response.status_code != 200:
            print(("ERROR: ", response.text))
            return
            
    def get_client_endpoints(self):
        
        cep_url = self.base_url + "api/clientEndPoints"
        
        #print("client endpoints url: " + cep_url)

        response = self.get(cep_url)
        
        #print(("clientEndPoints response: ", response.text))

        if response.status_code != 200:
            print(("ERROR: ", response.text))
            return

    def create_mission(self, mission_name,
                       creator_uid=None,
                       group=None,
                       tool=None,
                       description=None):

        ret_value = True

        creator_uid = creator_uid or self.uid or None
        if not creator_uid:
            print("ERROR: missing creatorUID need to create mission")
            return

        if not group or not isinstance(group, str):
            group = "__ANON__"

        query_params = {'creatorUid': creator_uid,
                        'group': group}

        if tool is not None:
            query_params['tool'] = tool
        if description is not None:
            query_params['description'] = description

        response = self.put(self.base_mission_api + mission_name, params=query_params)
        #pprint(query_params)
        if response.status_code == 201:
            self.all_missions[mission_name] = response.json()['data']
            self.missions_created[mission_name] = response.json()['data']

        elif 300 > response.status_code >= 200:
            #print(response.status_code, "Mission already exists")
            ret_value = False
            self.all_missions[mission_name] = response.json()['data']

        elif response.status_code >= 400:
            print("Failed to create mission", response.status_code)
            ret_value = False

        return ret_value

    def delete_mission(self, mission_name):

        response = self.delete(self.base_mission_api + mission_name)

        print(("Delete successful:", response.ok))

        return response

    def add_mission_content(self, mission_name, content_hash=None, content_uid=None):
        if not content_hash and not content_uid:
            return

        hashes = [content_hash] if content_hash is not None else []
        uids = [content_uid] if content_uid is not None else []

        data = json.dumps({"hashes": hashes,
                           "uids": uids})

        headers = {"Content-Type": "application/json"}

        response = self.put(self.base_mission_api + mission_name + "/contents", data=data, headers=headers)

        return response

    def subscribe_to_mission(self, mission_name, subscriber_uid=None):
        subscriber_uid = subscriber_uid or self.uid or None
        if not subscriber_uid:
            print("ERROR: Missing a subscriber uid. Who is subscribing to the mission?!?")
            return

        subscribe_url = self.base_mission_api + mission_name + "/subscription"
        params = {'uid': subscriber_uid}

        response = self.put(subscribe_url, params=params)
        return response

    def unsubscribe_from_mission(self, mission_name, subscriber_uid=None):
        subscriber_uid = subscriber_uid or self.uid
        if not subscriber_uid:
            print("ERROR: Missing a subscriber uid. Who is unsubscibing from the mission?!?")
            return

        unsubscribe_url = self.base_mission_api + mission_name + "/subscription"
        params = {'uid': subscriber_uid}

        response = self.delete(unsubscribe_url, params=params)
        return response

    def add_datafeed_to_mission(self, mission_name, datafeed_uuid, creator_uid):
        my_url = self.base_mission_api + mission_name + "/feed"
        params = {'creatorUid': creator_uid, 'dataFeedUid': datafeed_uuid}

        response = self.post(my_url, params=params)
        return response

    ########### End Mission Information/Subscription #####################

    ############## Enterprise Sync #############################################
    def download_file(self, file_hash, filename=None, save=False):
        params = {"hash": file_hash}
        response = self.get(self.base_sync_api + "content", params=params)

        if filename is None:
            value, params = cgi.parse_header(response.headers['Content-Disposition'])

            filename = params['filename']

        self.file_hash_map[filename] = file_hash

        if save:
            with open(filename, 'wb') as down:
                down.write(response.content)

        return response

    def upload_file(self,
                    file_name,
                    file_object,
                    content_type,
                    upload_file_name=None,
                    creatorUid=None,
                    uid=None,
                    latitude=None,
                    longitude=None,
                    altitude=None,
                    keywords=None):

        if upload_file_name is None:
            upload_file_name = file_name

        files = {"file": file_object}
        params = {"name": upload_file_name}
        if creatorUid:
            params["creatorUid"] = creatorUid
        if uid:
            params["uid"] = uid
        if latitude:
            params["latitude"] = latitude
        if longitude:
            params["longitude"] = longitude
        if altitude:
            params["altitude"] = altitude
        params['keywords'] = ['pyTakLoadTest']
        if keywords:
            params["keywords"].extend(keywords)

        headers = {"Content-Type": content_type}

        try:
            #pprint(params)
            #pprint(headers)
            response = self.post(self.base_sync_api + "upload", params=params, files=files, headers=headers)
            #print(response.status_code, response.request.url, response.content)
        except requests.exceptions.ConnectionError:
            "File {} too big to upload. Limit is 200mb".format(upload_file_name)
            return
        else:
            if response.status_code == 200:
                self.file_hash_map[upload_file_name] = str(response.json()['Hash'])

        #print(self.file_hash_map)
        return response

    def upload_size_file(self,
                         file_name,
                         size,
                         creatorUid="size-file-creator"):

        with NamedTemporaryFile() as f_object:
            f_file = f_object.file
            f_file.write(os.urandom(size))
            #print((f_file.tell() / 1000000))
            f_file.seek(0)
            keywords = ["size_file"]
            self.upload_file(file_name,
                             f_file,
                             "application/txt",
                             creatorUid=creatorUid,
                             keywords=keywords)

    def delete_file(self, file_name=None, file_hash=None):
        file_hash = file_hash or self.file_hash_map.get(file_name)
        if not file_hash:
            return False

        params = {"hash": file_hash}
        response = self.get(self.base_sync_api + "delete", params=params)
        if response.status_code == 200:
            try:
                self.file_hash_map.pop(file_name)
            except KeyError:
                pass
            return True

        return False

    def delete_all_files(self):
        for file_name, file_hash in list(self.file_hash_map.items()):
            success = self.delete_file(file_name=file_name, file_hash=file_hash)
            if not success:
                return False

        return True

    def search_files(self, keywords=[]):
        params = dict()
        if keywords:
            params['keywords'].extend(keywords)
        res = self.get(self.base_sync_api + "search", params=params)
        if res.status_code >= 400:
            return []
        files = json.loads(res.content).get('results')
        return files

    def delete_all_test_files(self):
        files = self.search_files()
        if files is None:
            return
        for f in files:
            self.delete_file(file_name=f['Name'], file_hash=f['Hash'])


########### End Enterprise Sync ############################################


#################### Initialization Classes ###############################
class MissionApiSetup(MissionApiSession):

    def initialize_takserver(self, config_dict):

        creatorUid = config_dict.get("creatorUid")
        group = config_dict.get("group")
        tool = config_dict.get("tool")

        files = config_dict.get("files", {})
        size_files = config_dict.get("size_files", {})

        for f in files:
            name, attrs = list(f.items())[0]
            if "content_type" not in attrs:
                print("ERROR: need to specify a content type for each file")
                exit()
            print("uploading file", name)
            try:
                file_object = open(name, "rb")
            except (FileExistsError, FileNotFoundError):
                print(("couldn't open file: {}".format(name)))
                continue

            self.upload_file(name,
                             file_object,
                             attrs.get("content_type"),
                             creatorUid=attrs.get("creatorUid", creatorUid),
                             uid=attrs.get("uid"),
                             latitude=attrs.get("latitude"),
                             longitude=attrs.get("longitude"),
                             altitude=attrs.get("altitude"),
                             keywords=attrs.get("keywords"))
            print()
            print(name, "uploaded")
            print()

        for size_file in size_files:
            print(size_file.items())
            f_name, f_size = list(size_file.items())[0]
            print(("uploading size_file {}".format(f_name)))
            self.upload_size_file(f_name, f_size)
            print()
            print(f_name, "uploaded")
            print()

        # pull existing missions
        get_all_missions = self.get(self.base_mission_api)
        existing_missions = {}
        for mission_entry in get_all_missions.json()['data']:
            existing_missions[mission_entry['name']] = mission_entry

        missions = config_dict.get("missions", {})
        pprint(missions)
        print(type(missions))

        for m in missions:
            # skip mission init if it exists
            name = ''
            if not type(m) == dict:
                name = m
            else:
                name, attrs = list(m.items())[0]

            if name in existing_missions:
                self.all_missions[name] = existing_missions[name]
                self.missions_created[name] = existing_missions[name]
                print('\nexisting mission, skipping create', name, '\n')
                continue


            print("creating mission", m)
            if not type(m) == dict:
                self.create_mission(m, creator_uid=creatorUid, group=group)
                continue

            name, attrs = list(m.items())[0]  # because there is only one mission per mission dict
            if attrs is None:
                attrs = {}
            self.create_mission(name,
                                creator_uid=attrs.get("creatorUid", creatorUid),
                                group=attrs.get("group", group))

            self.create_mission(name,
                                creator_uid=attrs.get("creatorUid", creatorUid),
                                group=attrs.get("group", group),
                                tool=attrs.get("tool", tool),
                                description=attrs.get("description"))
            self.unsubscribe_from_mission(name, subscriber_uid=attrs.get("creatorUid", creatorUid))
            print()
            print("mission created")
            print()

            if "files" in attrs:
                for f in attrs.get("files"):
                    self.add_mission_content(name, content_hash=self.file_hash_map.get(f))

            # Add datafeed to mission:
            if "datafeed_uuids" in attrs:
                print("Adding datafeeds to mission " + name)
                for datafeed_uuid in attrs.get("datafeed_uuids"):
                    self.add_datafeed_to_mission(name, datafeed_uuid = datafeed_uuid, creator_uid=attrs.get("creatorUid", creatorUid))
                    print("Added datafeed " + datafeed_uuid + " to mission " + name)
                print("Done adding datafeeds to mission " + name)
                print()
            if "datafeed_uuid_pattern" in attrs:
                pattern = attrs.get("datafeed_uuid_pattern").get("pattern")
                start_i = attrs.get("datafeed_uuid_pattern").get("start_i")
                end_i = attrs.get("datafeed_uuid_pattern").get("end_i")
                if (pattern is None or start_i is None or end_i is None):
                    print("ERROR: missing params in datafeed_uuid_pattern")
                    exit(1)
                print("Adding datafeeds with pattern " + pattern + " to mission " + name)
                for i in range(int(start_i), int(end_i)+1):
                    datafeed_uuid = pattern.replace("[i]", str(i))
                    self.add_datafeed_to_mission(name, datafeed_uuid = datafeed_uuid, creator_uid=attrs.get("creatorUid", creatorUid))
                    print("Added datafeed " + datafeed_uuid + " to mission " + name)
                print("Done adding datafeeds with pattern " + pattern + " to mission" + name)


    def cleanup(self):
        print("cleaning up initialization data on server")
        self.delete_all_test_files()
        for m in self.all_missions:
            self.delete_mission(m)


class MissionApiSetupProcess(multiprocessing.Process):

    def __init__(self, address, cert, password, config_dict, lock=None):
        multiprocessing.Process.__init__(self)
        self.setup_session = MissionApiSetup(address[0],
                                             address[1],
                                             cert, password)

        self.lock = None
        if lock is not None:
            self.lock = lock
            self.lock.acquire()
        self.config_dict = config_dict
        self.uid = "Init Process"

    def run(self):
        print("starting init")
        try:
            self.setup_session.initialize_takserver(self.config_dict)
            print("init started")
            if self.lock:
                self.lock.release()
            while True:
                time.sleep(1)
        except KeyboardInterrupt:
            if self.lock:
                try:
                    self.lock.release()
                except:
                    pass
            self.setup_session.cleanup()

###################### End Initialization Classes ##################################


class MissionApiPyTAKHelper(MissionApiSession):

    def __init__(self, host, port, certfile=None, password=None, uid=None,
                 mission_config=None):

        MissionApiSession.__init__(self, host, port, certfile, password, uid)

        if mission_config is not None:
            self.write_delta = mission_config.get("mission_write_interval", 1.0)
            self.last_write = time.time() - 2 * self.write_delta
            self.do_write = False

            self.upload_config = mission_config.get("uploads", {})
            self.upload_probability = self.upload_config.get("probability", 0)
            self.upload_size = self.upload_config.get("size", 0)
            self.upload_interval = self.upload_config.get("interval", 0)
            self.do_upload = False

            self.last_upload = time.time()
            self.next_upload = random.uniform(0, self.upload_interval) + self.last_upload
            self.did_upload = False
            self.upload_count = {}

            self.missions_subscribed = set()
            self.missions_to_subscribe = mission_config.get("subscribe", 0)
            self.select_random = mission_config.get("random", False)

            download_mission_content = mission_config.get("download_mission_content", 1.0)
            self.download_mission_content = max(0, min(1, download_mission_content))

            self.download_existing_content = mission_config.get("download_existing_content", False)

            self.num_missions_created = 0

            context = zmq.Context()
            self.request_queue = context.socket(zmq.PULL)
            self.port = self.request_queue.bind_to_random_port('tcp://*')
            self.poll = False

        else:
            self.write_delta = None
            self.poll = False
            self.do_write = False
            self.do_upload = False
            self.upload_interval = 0


    def ready_to_request(self):
        if (self.write_delta is None):
            return False
        self.poll = self.request_queue.poll(1) == zmq.POLLIN
        now = time.time()
        self.do_write = (now - self.last_write > self.write_delta)
        if self.upload_interval > 0:
            self.do_upload = (not self.did_upload) and (now > self.next_upload)
            if now - self.last_upload > self.upload_interval:
                self.last_upload = now
                self.next_upload = random.uniform(0, self.upload_interval) + self.last_upload
                self.did_upload = False
        return self.do_write or self.do_upload or self.poll



    def get_and_subscribe_to_mission(self, mission_name):
        self.get_mission(mission_name)
        self.get_mission_cot(mission_name)
        if self.download_existing_content:
            for c in self.all_missions[mission_name][0].get('contents', {}):
                file_data = c['data']
                if file_data['hash'] == self.file_hash_map.get(file_data['name']):
                    continue
                self.download_file(c['data']['hash'])
                c['data']['downloaded'] = True

    def make_requests(self):
        
        if self.do_write:
            if len(self.all_missions) == 0:
                self.get_all_missions()
                for mission in self.all_missions.keys():
                    self.unsubscribe_from_mission(mission, subscriber_uid=self.uid)

            if self.select_random and len(self.missions_subscribed) < self.missions_to_subscribe:
                if len(self.missions_subscribed) == len(self.all_missions):
                    remaining = 0
                elif self.missions_to_subscribe > len(self.all_missions):
                    remaining = len(self.all_missions) - len(self.missions_subscribed)
                else:
                    remaining = self.missions_to_subscribe - len(self.missions_subscribed)

                missions_to_sub = random.sample(set(self.all_missions.keys()).difference(self.missions_subscribed),
                                                remaining)
                for m in missions_to_sub:
                    if not self.subscribe_to_mission(m):
                        self.missions_to_subscribe.remove(m)
                        continue
                    self.get_and_subscribe_to_mission(m)

            elif (not self.select_random) and len(set(self.missions_to_subscribe).difference(self.missions_subscribed)) > 0:
                remaining = True
                if len(self.missions_subscribed) == len(self.all_missions):
                    remaining = False
                    self.missions_to_subscribe = self.missions_subscribed

                if remaining:
                    missions_to_sub = set(self.missions_to_subscribe).difference(self.missions_subscribed)
                    # print("missions left to sub", missions_to_sub)
                    for m in missions_to_sub:
                        if not self.subscribe_to_mission(m):
                            self.missions_to_subscribe.remove(m)
                            continue
                        self.get_and_subscribe_to_mission(m)
            self.last_write = time.time()
            self.do_write = False

        if self.do_upload:
            for mission in self.missions_subscribed:
                if self.upload_probability > random.uniform(0, 1):
                    mission_upload_count = self.upload_count.get(mission, 0)
                    file_name = self.uid + "_mission_" + mission + "_" + str(mission_upload_count)
                    self.upload_size_file(file_name, self.upload_size, creatorUid=self.uid)
                    file_hash = self.file_hash_map.get(file_name)
                    if file_hash is not None:
                        self.add_mission_content(mission, content_hash=file_hash)
                        if mission_upload_count == 0:
                            self.upload_count[mission] = 1
                        else:
                            self.upload_count[mission] += 1
            self.did_upload = True
            self.do_upload = False

        if self.poll:
            data = self.request_queue.recv_json()
            req_type = data['req_type']
            req_data = data['req_data']

            if req_type == "CREATE":
                self.get_mission(req_data)
            elif req_type == "INVITE":
                if not req_data in self.all_missions:
                    self.get_mission(req_data)
                success = self.subscribe_to_mission(req_data)
                if success:
                    self.get_mission_cot(req_data)
            elif req_type == "DELETE":
                try:
                    self.all_missions.pop(req_data, None)
                    self.missions_subscribed.remove(req_data)
                    self.missions_created.pop(req_data, None)
                except Exception as e:
                    print(e)

            elif req_type == "download_mission_files":
                if self.download_mission_content > random.uniform(0, 1):
                    for file_hash in req_data:
                        if file_hash not in self.file_hash_map.values():
                            self.download_file(file_hash=file_hash)

            elif req_type == "download_file":
                file_hash, filename = req_data
                self.download_file(file_hash=file_hash, filename=filename)

    def create_dynamic_mission(self):
        mission_name = self.uid + "_mission_" + str(self.num_missions_created)
        self.num_missions_created += 1
        if not self.create_mission(mission_name,
                                   creator_uid=self.uid,
                                   group="__ANON__"):
            self.delete_mission(mission_name)
        else:
            self.create_mission(mission_name,
                                creator_uid=self.uid,
                                group="__ANON__",
                                tool="PyTAK",
                                description="dynamically created mission by " + self.uid)

    def subscribe_to_mission(self, mission_name, subscriber_uid=None):
        response = MissionApiSession.subscribe_to_mission(self, mission_name, subscriber_uid)
        if response.status_code >= 300:
            print(self.uid, "failed to subscribe to mission {}:".format(mission_name), response.status_code, response.text)
            return False
        self.missions_subscribed.add(mission_name)

        return True

