#!/usr/local/bin/python

import argparse
import logging
import socket
import time
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

global logger

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
        response = sess.post(enterprise_sync + "upload", params={"name": "test_file.pdf", "creatorUid": "test_User"}, files=files, headers={"Content-Type": "application/pdf"})


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

    if mission_api_config is not None:
        if mission_port is None:
            logger.error("Need to specify https port if you want to use the mission port")
            exit(1)
        mission_api_config['address'] = (host, mission_port)
    for i in range(clients):
        try:
            p = PyTAKStreamingProtoProcess(address=(host, port),
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

    if mission_api_config is not None:
        if mission_port is None:
            logger.error("Need to specify https port if you want to use the mission port")
            exit(1)
        mission_api_config['address'] = (host, mission_port)

    data_dict = Manager().dict()
    for i in range(clients):
        try:
            p = PyTAKStreamingCotProcess(address=(host, port),
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

def test_udp(host, port, interval=0.25, track_uid=None):

    sock = socket.socket( socket.AF_INET, socket.SOCK_DGRAM )

    sock.settimeout(1.0)

    if not track_uid:
        track_uid = id_gen()

    lat = 40
    lon = -65

    logger.debug("------------- Testing UDP ---------------------")
    logger.debug(track_uid + " is going to be trying to send things on udp port")
    while True:
        try:
            m = CotMessage(uid=track_uid, lat=str(lat), lon=str(lon))
            sock.sendto(m.to_string(), (host, port))


            time.sleep(interval)
        except KeyboardInterrupt:
            return



if __name__ == "__main__":

    parser = argparse.ArgumentParser()



    parser.add_argument("--https", help="specify https port for REST calls")
    parser.add_argument("--test-websocket-proto", help="test the websocket protocol buffer client type", action="store_true")


    parser.add_argument("--test-pytak", help="test using the pytak client (you can also use the PyTAK program alone)", action="store_true")
    parser.add_argument("--test-pytak-proto", help="test the streaming connection with protocol buffers instead of cot xml", action="store_true")
    parser.add_argument("--tls", help="specify tls port", type=int)

    parser.add_argument("--udp", help="specify udp port", type=int)
    parser.add_argument("--test-udp", help="perform the udp test", action="store_true")


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
    handler.setLevel(logging.INFO)
    logger = logging.getLogger("tak_tester")
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

            offset = args.offset or pytak_conf.get('offset', 1.0)
            pytak_clients = args.clients or pytak_conf.get('clients', 1)

            mission_api_config = config['PyTAK'].get('missions', None)
            if mission_api_config is None and not args.skip_mission_api:
                logger.warning("No mission api config set for the load test client")

        udp_conf = config.get("UDPTest")
        if udp_conf:
            udp_interval = args.interval or udp_conf.get("interval", 0.1)
            udp_clients = args.clients or udp_conf.get("clients", 1)


        logger.debug("The current test config '{}' is:".format(config_file))
        logger.debug("\n"+pformat(config))


    else:
        cert = args.cert
        if cert is None:
            print("-c/--cert CERT required")
            exit(1)
        password = args.password or "atakatak"

        host = args.host

        https = args.https
        tls = args.tls
        udp = args.udp


        interval = args.interval
        self_sa_delta = udp_interval = interval or 5

        offset = args.offset

        pytak_clients = udp_clients = args.clients or 1

        config = {}

    if args.aws:
        with open("TAKServerPrivateIP.txt") as f:
            host = f.readline().strip()

    if args.skip_mission_api:
        config["Missions"] = None
        mission_api_config = None

    if args.skip_mission_init:
        config["Missions"] = None

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
                           mission_api_config=mission_api_config)

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
                             mission_api_config=mission_api_config)

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
        args_and_kwargs = lambda x: ((host, udp), {"track_uid": str(x), "interval": udp_interval})
        worker_data = [args_and_kwargs(i) for i in range(udp_clients)]
        p = Pool(udp_clients)
        try:
            res = p.map(test_udp_wrapper, worker_data)
        except KeyboardInterrupt:
            exit()
