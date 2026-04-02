#!/usr/local/bin/python

import argparse
import logging
import socket
import time
import random
import sys
from multiprocessing import Lock, Manager, Pool
from pprint import pformat

import yaml
from requests import Session

from create_cot import CotMessage, id_gen
from mission_api import MissionApiSetupProcess
from pyTak import TLSClientProcess
if sys.version_info >= (3, 6):
    from pyTakWebsocket import PyTAKWebsocketProcess
    from pyTakStreamingProto import PyTAKStreamingProtoProcess
    from pyTakStreamingCot import PyTAKStreamingCotProcess
from utils import IgnoreHostNameAdapter, p12_to_pem, print_data_dict
from stats import Stats
from cloud_watch import cloud_watch_process, print_info_without_sending_to_cloud_watch_process
from multiprocessing import Process, Value, Array
from create_proto import CotProtoMessage, get_msg_size

logger = logging.getLogger("tak_tester")

def test_mission_api(host, port, cert, password, verbose=True):
    mission_api = "https://{host}:{port}/Marti/api/".format(host=host, port=port)
    enterprise_sync = "https://{host}:{port}/Marti/sync/".format(host=host, port=port)


    with p12_to_pem(cert, password) as cert_file:
        sess = Session()
        sess.mount("https://", IgnoreHostNameAdapter())
        sess.cert = cert_file
        sess.verify = cert_file

        logger.info("subcribing to a mission?...")
        files = {'file': open('Mission-API.pdf', 'rb')}
        response = sess.post(enterprise_sync + "upload", params={"name": "test_file.pdf", "creatorUid": "test"}, files=files, headers={"Content-Type": "application/pdf"})


        logger.info(pformat(response))
        logger.info(pformat(response.request.url))
        logger.info(pformat(response.request.headers))
        logger.info(pformat(response.json()))

    return True


def test_tls(host, port, cert, password,
             clients=1,
             self_sa_delta=5.0,
             offset=1.0,
             sequential_uids=True,
             mission_config=None,
             mission_port=None,
             mission_api_config=None):

    if sequential_uids:
        uids = ["PyTAK-"+str(i) for i in range(clients)]
    else:
        uids = [None for i in range(clients)]

    procs = list()

    if mission_config:
        if not mission_port:
            print("Need to specify https port if you want to use mission api")
            exit(1)
        print("Initializing the server with files and missions")
        lock = Lock()
        initializer = MissionApiSetupProcess(address=(host, mission_port),
                                             cert=cert, password=password,
                                             config_dict=mission_config,
                                             lock=lock)

        procs.append(initializer)
        initializer.start()

        lock.acquire()
        print("Initializing complete")
        lock.release()


    for i in range(clients):
        try:
            p = TLSClientProcess(address=(host, port),
                                 cert=cert, password=password,
                                 uid=uids[i],
                                 self_sa_delta=self_sa_delta,
                                 mission_config=mission_api_config)
            procs.append(p)
            p.start()
            time.sleep(offset)
        except KeyboardInterrupt:
            print("trying to kill {} processes".format(len(procs)))
            for p in procs:
                p.join()
                print(str(p.uid), "is dead")
            exit(0)

    while True:
        try:
            pass
        except KeyboardInterrupt:
            print("trying to kill {} processes".format(len(procs)))
            for p in procs:
                p.join()
                print(str(p.uid), "is dead")
            exit(0)

def test_websocket_proto(host, port, websocket_path, cert, password,
                         clients=1,
                         self_sa_delta=5.0,
                         offset=1.0,
                         sequential_uids=True,
                         mission_config=None,
                         mission_port=None,
                         mission_api_config=None):
    if sequential_uids:
        uids = ["PyTAK-%04d" % i for i in range(clients)]
    else:
        uids = [None for i in range(clients)]

    procs = list()

    if mission_config:
        if not mission_port:
            logger.error("Need to specify https port if you want to use mission api")
            exit(1)
        logger.info("Initializing the server with files and missions")
        lock = Lock()
        initializer = MissionApiSetupProcess(address=(host, mission_port),
                                             cert=cert, password=password,
                                             config_dict=mission_config,
                                             lock=lock)

        procs.append(initializer)
        initializer.start()

        lock.acquire()
        logger.info("Initializing complete")
        lock.release()

    data_dict = Manager().dict()
    for i in range(clients):
        try:
            p = PyTAKWebsocketProcess(address=(host, port),
                                      websocket_path=websocket_path,
                                      cert=cert, password=password,
                                      uid=uids[i],
                                      self_sa_delta=self_sa_delta,
                                      mission_config=mission_api_config,
                                      data_dict=data_dict)
            procs.append(p)
            p.start()
            time.sleep(offset)
            logger.info(print_data_dict(data_dict))
        except KeyboardInterrupt:
            logger.info("trying to kill {} processes".format(len(procs)))
            for p in procs:
                p.join()
            exit(0)
    logger.info("Done with setting up " + str(clients) + " clients")

    while True:
        try:
            logger.info(print_data_dict(data_dict))
            time.sleep(1)
        except KeyboardInterrupt:
            logger.info("trying to kill {} processes".format(len(procs)))
            for p in procs:
                p.join()
            exit(0)

def test_streaming_proto(host, port, cert, password,
                         clients=1,
                         self_sa_delta=5.0,
                         offset=1.0,
                         sequential_uids=True,
                         mission_config=None,
                         mission_port=None,
                         mission_api_config=None,
                         ping=False,
                         ping_interval=1000,
                         send_metrics=False,
                         send_metrics_interval=60,
                         cloudwatch_namespace="pyTAK-test",
                         track_file=None,
                         track_start_delay=0):
    
    if sequential_uids:
        uids = ["PyTAK-%04d" % i for i in range(clients)]
    else:
        uids = [None for i in range(clients)]

    procs = list()

    if mission_config:
        if not mission_port:
            logger.error("Need to specify https port if you want to use mission api")
            exit(1)
        logger.info("Initializing the server with files and missions")
        lock = Lock()
        initializer = MissionApiSetupProcess(address=(host, mission_port),
                                             cert=cert, password=password,
                                             config_dict=mission_config,
                                             lock=lock)

        procs.append(initializer)
        initializer.start()

        lock.acquire()
        logger.info("Initializing complete")
        lock.release()
    data_dict = Manager().dict()

    if mission_api_config is not None:
        if mission_port is None:
            logger.error("Need to specify https port if you want to use the mission port")
            exit(1)
        mission_api_config['address'] = (host, mission_port)
    for i in range(clients):
        try:

            #stats_uid = "proto_client_" + str(i)
            uid = ("proto_client_" + str(i)) if uids[i] is None else uids[i]
            arr = None

            global stats
            stats.init_uid(uid)
            arr = stats.get_uid(uid)

            p = PyTAKStreamingProtoProcess(address=(host, port),
                                           cert=cert, password=password,
                                           uid=uids[i],
                                           self_sa_delta=self_sa_delta,
                                           track_write_delta=track_write_delta,
                                           num_tracks_per_delta=num_tracks_per_delta,
                                           mission_config=mission_api_config,
                                           data_dict=data_dict,
                                           ping=ping,
                                           ping_interval=ping_interval,
                                           arr = arr,
                                           track_file=track_file,
                                           track_start_delay=track_start_delay)
            procs.append(p)
            p.start()

            if send_metrics:
                p_cloud_watch = Process(target=cloud_watch_process, args=(uid, arr,cloudwatch_namespace, send_metrics_interval))
                procs.append(p_cloud_watch)
                p_cloud_watch.start()
            else:
                p_info_without_sending_to_cloud_watch_process= Process(target=print_info_without_sending_to_cloud_watch_process, args=(uid, arr, send_metrics_interval))
                procs.append(p_info_without_sending_to_cloud_watch_process)
                p_info_without_sending_to_cloud_watch_process.start()
                
            time.sleep(offset)
            logger.info(print_data_dict(data_dict))

        except KeyboardInterrupt:
            logger.info("trying to kill {} processes".format(len(procs)))
            for p in procs:
                p.join()
            exit(0)

    while True:
        try:
            logger.info(print_data_dict(data_dict))
            time.sleep(1)
        except KeyboardInterrupt:
            logger.info("trying to kill {} processes".format(len(procs)))
            for p in procs:
                p.join()
            exit(0)


def test_streaming_cot(host, port, cert, password,
                       clients=1,
                       self_sa_delta=5.0,
                       offset=1.0,
                       sequential_uids=True,
                       mission_config=None,
                       mission_port=None,
                       mission_api_config=None,
                       debug=False,
                       ping=False,
                       ping_interval=1000,
                       send_metrics=False,
                       send_metrics_interval=60,
                       cloudwatch_namespace="pyTAK-test"
                       ):
    if sequential_uids:
        uids = ["PyTAK-%04d" % i for i in range(clients)]
    else:
        uids = [None for i in range(clients)]

    procs = list()

    if mission_config:
        if not mission_port:
            logger.error("Need to specify https port if you want to use mission api")
            exit(1)
        logger.info("Initializing the server with files and missions")
        lock = Lock()
        initializer = MissionApiSetupProcess(address=(host, mission_port),
                                             cert=cert, password=password,
                                             config_dict=mission_config,
                                             lock=lock)

        procs.append(initializer)
        initializer.start()

        lock.acquire()
        logger.info("Initializing complete")
        lock.release()

    if mission_api_config is not None:
        if mission_port is None:
            logger.error("Need to specify https port if you want to use the mission port")
            exit(1)
        mission_api_config['address'] = (host, mission_port)

    data_dict = Manager().dict()
    for i in range(clients):
        try:
            # stats_uid = "cot_client_" + str(i)
            uid = ("cot_client_" + str(i)) if uids[i] is None else uids[i]
            arr = None
            
            global stats
            stats.init_uid(uid)
            arr = stats.get_uid(uid)

            p = PyTAKStreamingCotProcess(address=(host, port),
                                         cert=cert, password=password,
                                         uid=uids[i],
                                         self_sa_delta=self_sa_delta,
                                         mission_config=mission_api_config,
                                         data_dict=data_dict,
                                         debug = debug,
                                         ping=ping,
                                         ping_interval=ping_interval,
                                         arr = arr
                                         )
            procs.append(p)
            p.start()

            if send_metrics:
                p_cloud_watch = Process(target=cloud_watch_process, args=(uid, arr,cloudwatch_namespace, send_metrics_interval))
                procs.append(p_cloud_watch)
                p_cloud_watch.start()
            else:
                p_info_without_sending_to_cloud_watch_process= Process(target=print_info_without_sending_to_cloud_watch_process, args=(uid, arr, send_metrics_interval))
                procs.append(p_info_without_sending_to_cloud_watch_process)
                p_info_without_sending_to_cloud_watch_process.start()

            time.sleep(offset)
            logger.info(print_data_dict(data_dict))
        except KeyboardInterrupt:
            logger.info("trying to kill {} processes".format(len(procs)))
            for p in procs:
                p.join()
            exit(0)

    while True:
        try:
            logger.info(print_data_dict(data_dict))
            time.sleep(1)
        except KeyboardInterrupt:
            logger.info("trying to kill {} processes".format(len(procs)))
            for p in procs:
                p.join()
            exit(0)

def test_udp_wrapper(args_and_kwargs):

    args, kwargs = args_and_kwargs
    return test_udp(*args, **kwargs)

def test_tcp_wrapper(args_and_kwargs):
    
    args, kwargs = args_and_kwargs
    return test_tcp(*args, **kwargs)

def test_udp(host, port, track_write_delta=1.0, num_tracks_per_delta=1):

    sock = socket.socket( socket.AF_INET, socket.SOCK_DGRAM )

    sock.settimeout(10.0)

    track_uids = [CotProtoMessage().uid for _ in range(num_tracks_per_delta)]

    logger.info("------------- Testing UDP ---------------------")
    logger.info("Sending " + str(num_tracks_per_delta) + " tracks at interval " + str(track_write_delta))
    while True:
        try:
            for i in range(num_tracks_per_delta):
                track_uid = track_uids[i]
                track_location = (random.uniform(-70, 70), random.uniform(-130, 130))
                track_message = CotMessage(uid=track_uid, lat=str(track_location[0]), lon=str(track_location[1]))
                track_message.add_callsign_detail(group_name="Blue", platform="PyTAKStreamingCot")
                # track_message = CotProtoMessage(uid=track_uid, lat=str(track_location[0]), lon=str(track_location[1]))
                # track_message.add_callsign_detail(group_name="Blue", platform="PyTAKStreamingProto")
                logger.info("sending track " + str(track_message.to_string()))
                sock.sendto(track_message.to_string(), (host, port))
                # sock.sendto(track_message.serialize(), (host, port))
                logger.info("Sent track with id " + str(track_uid))

            logger.info("Waiting for " + str(track_write_delta) + " until sending more tracks")
            time.sleep(track_write_delta)
        except KeyboardInterrupt:
            return

def test_tcp(host, port, track_write_delta=1.0, num_tracks_per_delta=1, track_file=None, track_start_delay=0):
    logger.info("------------- Testing TCP ---------------------")
    logger.info(f"Connecting to {host}:{port} and sending {num_tracks_per_delta} tracks every {track_write_delta}s")

    track_points = []
    if track_file:
        logger.info(f"Loading track points from {track_file}")
        with open(track_file, "r") as f:
            lines = f.read().strip().splitlines()
            
        if not lines:
            logger.error(f"Track file {track_file} is empty!")
            return

        header = lines[0].strip().split(",")
        if len(header) < 2:
            logger.error(f"Track file first line must contain callsign,uid")
            return

        callsign_str, uid_str = lines[0].strip().split(",", 1)
        replay_callsign = callsign_str.strip()
        replay_uid = uid_str.strip()
        
        parsed_points = []
        for idx, line in enumerate(lines[1:]):  # preserve index in file (line 0 is header)
            try:
                delay, lat, lon = map(float, line.strip().split(",")[:3])
                if delay >= track_start_delay:
                    parsed_points.append((idx, delay, lat, lon))
            except ValueError:
                logger.warning(f"Skipping malformed line: {line}")
                
        # Normalize delay so that first retained track starts at 0.0
        track_points = [(idx, delay - track_start_delay, lat, lon) for (idx, delay, lat, lon) in parsed_points]
                                                                    
        logger.info(f"Loaded {len(track_points)} track points from file.")
    
    try:
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.connect((host, port))

        track_uids = [CotProtoMessage().uid for _ in range(num_tracks_per_delta)]
        replay_index = 0
        replay_start_time = time.time()

        while True:
            if track_points:
                now = time.time()
                if replay_index < len(track_points):
                    original_idx, delay, lat, lon = track_points[replay_index]                
                    current_delay_since_start = now - replay_start_time
                    logger.info("current delay since start: " + str(current_delay_since_start))
                    if (now - replay_start_time) >= delay:
                        track_message = CotMessage(uid=replay_uid, lat=str(lat), lon=str(lon))
                        track_message.add_callsign_detail(group_name="Replay", platform="PyTAKStreamingTCP", callsign=replay_callsign)
                        sock.sendall(track_message.to_string())
                        logger.info(f"Sent replayed track #{original_idx} at lat={lat}, lon={lon}")                
                        replay_index += 1
                    else:
                        logger.debug(f"Waiting to send replay track, time since start={now - replay_start_time:.2f}s, next delay={delay}s")
                        time.sleep(1.0)
                        continue
                else:
                    logger.info("All replay tracks sent. Stopping.")
                    return

            else:
                for i in range(num_tracks_per_delta):
                    # fallback to random track
                    track_location = (random.uniform(-70, 70), random.uniform(-130, 130))
                    track_uid = track_uids[i]
                    track_message = CotMessage(uid=track_uid, lat=str(track_location[0]), lon=str(track_location[1]))
                    track_message.add_callsign_detail(group_name="Blue", platform="PyTAKStreamingTCP")
                    sock.sendall(track_message.to_string())
                    logger.info(f"Sent random track with id {track_uid}")
                    
                logger.info(f"Waiting for {track_write_delta}s until sending more tracks")
                time.sleep(track_write_delta)
                
    except KeyboardInterrupt:
        logger.info("TCP test interrupted by user.")
    except Exception as e:
        logger.error(f"TCP connection failed: {e}")
    finally:
        sock.close()
                                                                                                                                                                                                    
if __name__ == "__main__":

    parser = argparse.ArgumentParser()



    parser.add_argument("--https", help="specify https port for REST calls")
    parser.add_argument("--test-websocket-proto", help="test the websocket protocol buffer client type", action="store_true")


    parser.add_argument("--test-pytak", help="test using the pytak client (you can also use the PyTAK program alone)", action="store_true")
    parser.add_argument("--test-pytak-proto", help="test the streaming connection with protocol buffers instead of cot xml", action="store_true")
    parser.add_argument("--tls", help="specify tls port", type=int)

    parser.add_argument("--udp", help="specify udp port", type=int)
    parser.add_argument("--test-udp", help="perform the udp test", action="store_true")

    parser.add_argument("--tcp", help="specify tcp port", type=int)
    parser.add_argument("--test-tcp", help="perform the tcp test", action="store_true")


    parser.add_argument("-v", "--verbose", action="store_true")
    parser.add_argument("-c", "--cert",
                        help="path to p12 client certificate for TAK server")
    parser.add_argument("-p", "--password",
                        default="atakatak",
                        help="password for p12 certificate")

    parser.add_argument("--host",
                        help="address of the TAK server (default 128.89.77.154)")

    parser.add_argument("--port",
                        default=8089,
                        help="tls port on the TAK server (default 8089)",
                        type=int)

    parser.add_argument("-i", "--interval",
                        help="how many seconds between self-sa messages (default 5 seconds)",
                        type=int)

    parser.add_argument("--track-interval",
                        help="Time interval for writing track data (default 1 second)",
                        type=float)
    
    parser.add_argument("--num-tracks-per-delta",
                        help="Number of tracks per track write interval",
                        type=int)        

    parser.add_argument("--offset",
                        help="how many seconds between starting each new client (default 1 second)",
                        default=1.0,
                        type=float)

    parser.add_argument("--config", help="use a config file to specify host:port and other settings")

    parser.add_argument("--clients",
                        help="how many clients should we create",
                        type=int)

    parser.add_argument("--sequential-uids",
                        action="store_true",
                        help="instead of random uids for each client, name them seqeuntially, ie PyTAK-0000, PyTAK-0001")

    parser.add_argument("--skip-mission-api",
                        action="store_true",
                        help="dont test mission api capabilities")

    parser.add_argument("--skip-mission-init",
                        action="store_true",
                        help="dont create and upload files and missions to the server at the beginning of the test")

    parser.add_argument("--aws", help="running this test on AWS, so replace host ip with the server ip", action="store_true")

    parser.add_argument("--ping", help="sending ping messages to server", type=bool)

    parser.add_argument("--ping-interval", help="ping interval in MILLISECOND", type=int)

    parser.add_argument("--send-metrics", help="sending client metrics to server", type=bool)

    parser.add_argument("--send-metrics-interval", help="sending metrics interval (in seconds)", type=int)

    parser.add_argument("--cloudwatch-namespace", help="CloudWatch namespace to send metrics to")

    parser.add_argument("--track-file", help="Path to track replay file")
    
    parser.add_argument("--track-start-delay", type=float, default=0.0, help="Initial delay in seconds before first replay point")

    args = parser.parse_args()

    if args.verbose:
        level = logging.DEBUG
    else:
        level = logging.INFO
    logging.basicConfig(level=level,
                        format='%(asctime)s - %(name)s - %(levelname)s:\t %(message)s',
                        stream=sys.stdout)
    formatter = logging.Formatter('%(asctime)s - %(name)s - %(levelname)s:\t %(message)s')
    handler = logging.FileHandler(filename="loadTest.log")
    handler.setFormatter(formatter)
    handler.setLevel(level)
    logger.addHandler(handler)

    config_file = args.config
    if config_file:
        with open(config_file) as stream:
            try:
                if sys.version_info.major < 3:
                    config = yaml.load(stream)
                elif sys.version_info.major >= 3:
                    config = yaml.load(stream, Loader=yaml.FullLoader)
            except yaml.YAMLError as e:
                logger.error(e)
                exit(1)

        conn_config = config['connection']
        host = args.host or conn_config.get('host')
        if host is None:
            print("Missing host address in config (host:<address> in connection section)")
            exit(1)
        tls = args.tls or conn_config.get('tls')
        udp = args.udp or conn_config.get('udp')
        https = args.https or conn_config.get('https')

        websocket_path = config['PyTAK'].get('websocket_path', 'takproto/1')

        auth_conf = config.get('authentication', {})
        cert = args.cert or auth_conf.get('cert')
        password = args.password or auth_conf.get('password')
        if cert is None or password is None:
            print("Missing cert or password for TAK server")


        pytak_conf = config.get('PyTAK')
        if pytak_conf:
            self_sa_delta = args.interval or pytak_conf.get('self_sa_delta', 5.0)
            track_write_delta = args.track_interval or pytak_conf.get('track_write_delta', 1.0)
            num_tracks_per_delta = args.num_tracks_per_delta or pytak_conf.get('num_tracks_per_delta', 0.0)

            offset = args.offset or pytak_conf.get('offset', 1.0)
            pytak_clients = args.clients or pytak_conf.get('clients', 1)

            mission_api_config = config['PyTAK'].get('missions', None)
            if mission_api_config is None and not args.skip_mission_api:
                logger.warning("No mission api config set for the load test client")

            ping = args.ping or pytak_conf.get('ping', False)
            ping_interval = args.ping_interval or pytak_conf.get('ping_interval', 1000)
            send_metrics = args.send_metrics or pytak_conf.get('send_metrics', False)
            send_metrics_interval = args.send_metrics_interval or pytak_conf.get('send_metrics_interval', 60)
            cloudwatch_namespace = args.cloudwatch_namespace or pytak_conf.get('cloudwatch_namespace', 'pyTAK-test')

        udp_conf = config.get("UDPTest")
        if udp_conf:
            udp_interval = args.interval or udp_conf.get("interval", 0.1)
            udp_clients = args.clients or udp_conf.get("clients", 1)


        logger.debug("The current test config '{}' is:".format(config_file))
        logger.debug("\n"+pformat(config))


    else: # no config file

        cert = args.cert
        if cert is None:
            print("-c/--cert CERT required")
            exit(1)
        password = args.password or "atakatak"

        host = args.host

        https = args.https
        tls = args.tls
        udp = args.udp
        tcp = args.tcp


        interval = args.interval
        self_sa_delta = udp_interval = interval or 5

        offset = args.offset

        pytak_clients = tcp_clients = udp_clients = args.clients or 1

        ping = args.ping or False
        ping_interval = args.ping_interval or 1000
        send_metrics = args.send_metrics or False
        track_write_delta = args.track_interval
        num_tracks_per_delta = getattr(args, "num_tracks_per_delta", 0) or 0
        send_metrics_interval = args.send_metrics_interval or 60
        cloudwatch_namespace = args.cloudwatch_namespace
        track_file = args.track_file
        track_start_delay = args.track_start_delay
        websocket_path = 'takproto/1'
        config = {}

    if args.aws:
        print ("Open TAKServerPrivateIP.txt")
        with open("TAKServerPrivateIP.txt") as f:
            host = f.readline().strip()

    if args.skip_mission_api:
        config["Missions"] = None
        mission_api_config = None

    if args.skip_mission_init:
        config["Missions"] = None

    global stats
    stats = Stats()

    if args.test_pytak:
        logger.info("")
        logger.info("---------- testing streaming with cot xml ----------")
        logger.info("testing host: {}:{}".format(host, tls))
        test_streaming_cot(host, tls, cert, password,
                           clients=pytak_clients,
                           self_sa_delta=self_sa_delta,
                           offset=offset,
                           sequential_uids=args.sequential_uids,
                           mission_config=config.get("Missions"),
                           mission_port=https,
                           mission_api_config=mission_api_config,
                           debug = args.verbose,
                           ping=ping,
                           ping_interval=ping_interval,
                           send_metrics=send_metrics,
                           send_metrics_interval=send_metrics_interval,
                           cloudwatch_namespace=cloudwatch_namespace
                           )

    if args.test_pytak_proto:
        logger.info("")
        logger.info("---------- testing streaming with protocol buffer ----------")
        logger.info("testing host: {}:{}".format(host, tls))

        test_streaming_proto(host, tls, cert, password,
                             clients=pytak_clients,
                             self_sa_delta=self_sa_delta,
                             offset=offset,
                             sequential_uids=args.sequential_uids,
                             mission_config=config.get("Missions"),
                             mission_port=https,
                             mission_api_config=mission_api_config,
                             ping=ping,
                             ping_interval=ping_interval,
                             send_metrics=send_metrics,
                             send_metrics_interval=send_metrics_interval,
                             cloudwatch_namespace=cloudwatch_namespace,
                             track_file=track_file,
                             track_start_delay=track_start_delay
                            )

    if args.test_websocket_proto:
        logger.info("")
        logger.info("---------- testing websockets with protocol buffers -----------")
        logger.info("testing host: {}:{}/{}".format(host, https, websocket_path))
        test_websocket_proto(host, https, websocket_path, cert, password,
                              clients=pytak_clients,
                              self_sa_delta=self_sa_delta,
                              offset=offset,
                              sequential_uids=args.sequential_uids,
                              mission_config=config.get("Missions"),
                              mission_port=https,
                              mission_api_config=mission_api_config)


    if args.test_udp:
        logger.info("")
        logger.info("----------- testing the udp transport -------------------")
        logger.info("testing host: {}:{}".format(host, udp))
        args_and_kwargs = lambda x: ((host, udp), {"track_write_delta": track_write_delta, "num_tracks_per_delta": num_tracks_per_delta})
        worker_data = [args_and_kwargs(i) for i in range(udp_clients)]
        p = Pool(udp_clients)
        try:
            res = p.map(test_udp_wrapper, worker_data)
        except KeyboardInterrupt:
            exit()

    if args.test_tcp:
        logger.info("")
        logger.info("----------- testing the tcp transport -------------------")
        logger.info("testing host: {}:{}".format(host, tcp))
        args_and_kwargs = lambda x: ((host, tcp), {"track_write_delta": track_write_delta, "num_tracks_per_delta": num_tracks_per_delta, "track_file": track_file, "track_start_delay": track_start_delay})
        worker_data = [args_and_kwargs(i) for i in range(tcp_clients)]
        p = Pool(tcp_clients)
        try:
            res = p.map(test_tcp_wrapper, worker_data)
        except KeyboardInterrupt:
            exit()
