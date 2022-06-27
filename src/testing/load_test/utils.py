import contextlib
import tempfile

import math
from OpenSSL import crypto
from lxml import etree
from requests import Session
from requests.adapters import HTTPAdapter


###### This allows us to verify the host without needing to match the hostname ###############
#### use these methods to make requests #####################
class IgnoreHostNameAdapter(HTTPAdapter):
    def init_poolmanager(self, *args, **kwargs):
        kwargs['assert_hostname'] = False
        return super(IgnoreHostNameAdapter, self).init_poolmanager(*args, **kwargs)

def request(*args, **kwargs):
    with Session() as sess:
        adapter = IgnoreHostNameAdapter()
        sess.mount("https://", adapter)
        return sess.request(*args, **kwargs)

def get(*args, **kwargs):
    return request('get', *args, **kwargs)

def post(*args, **kwargs):
    return request('post', *args, **kwargs)

def put(*args, **kwargs):
    return request('put', *args, **kwargs)

def delete(*args, **kwargs):
    return request('delete', *args, **kwargs)
######### End HTTPS Request helpers ########################################################


##### This allows us to use PKCS#12 cert files #####################
@contextlib.contextmanager
def p12_to_pem(p12_path, p12_password):
    """ Decrypts the .p12 file to be used with requests. """
    with tempfile.NamedTemporaryFile(suffix='.pem') as t_pem:
        f_pem = open(t_pem.name, 'wb')
        p12_data = open(p12_path, 'rb').read()
        p12 = crypto.load_pkcs12(p12_data, p12_password)
        f_pem.write(crypto.dump_privatekey(crypto.FILETYPE_PEM, p12.get_privatekey()))
        f_pem.write(crypto.dump_certificate(crypto.FILETYPE_PEM, p12.get_certificate()))
        ca = p12.get_ca_certificates()
        if ca is not None:
            for cert in ca:
                f_pem.write(crypto.dump_certificate(crypto.FILETYPE_PEM, cert))
        f_pem.close()
        yield t_pem.name

########## End PKCS#12 helpers ####################################


def is_sa_message(message):
    root = etree.fromstring(message)
    details = root.find("detail")
    for c in details.getchildren():
        if c.tag == "contact":
            return True
    return False


def mission_change(message):
    root = etree.fromstring(message)
    details = root.find("detail")
    for c in details.getchildren():
        if c.tag == "mission":
            change_type = c.attrib.get("type")
            mission_name = c.attrib.get("name")
            file_hashes = list()
            for mission_changes in c.getchildren():
                for mission_change in mission_changes.getchildren():
                    for detail in mission_change.getchildren():
                        if detail.tag == "contentResource" and detail.find("filename") is not None:
                            file_hashes.append(detail.find("hash").text)
            if file_hashes:
                return "download_mission_files", file_hashes

            if change_type in {"CHANGE", "CREATE", "INVITE", "DELETE"}:
                return change_type, mission_name

    return False, False


prev_data = {'read': 0, 'write': 0, 'bytes': 0}
def print_data_dict(connection_data):
    total_clients = len(connection_data)
    connected_clients = 0
    num_writes = 0
    #num_reads = 0
    #num_bytes = 0
    for client_data in connection_data.values():
        if client_data['connected']:
            connected_clients += 1
        #num_reads += client_data['read']
        num_writes += client_data['write']
        #num_bytes += client_data['bytes']
    # try:
    #     diff_reads = math.ceil((num_reads - prev_data['read']) / connected_clients)
    # except ZeroDivisionError:
    #     diff_reads = num_reads - prev_data['read']
    # prev_data['read'] = num_reads
    #
    # diff_bytes = num_bytes - prev_data['bytes']
    # if diff_bytes > 1000000000: # GB
    #     diff_bytes = "{:.3f} GB".format((diff_bytes / 1000000000))
    # elif diff_bytes > 1000000:
    #     diff_bytes = "{:.3f} MB".format((diff_bytes / 1000000))
    # else:
    #     diff_bytes = "{:.3f} KB".format((diff_bytes / 1000))

    # prev_data['bytes'] = num_bytes

    diff_writes = num_writes - prev_data['write']
    prev_data['write'] = num_writes

    # ; num_reads/client: {reads:>10}; num_bytes: {bytes:>10}" \
    return "clients connected: {conn:>3} / {tot:>3}; num_writes: {writes:>10}" \
        .format(conn=connected_clients,
                tot=total_clients,
                writes=diff_writes)#,
                #reads=diff_reads,
                #bytes=diff_bytes)