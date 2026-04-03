import contextlib
import tempfile

import math
from cryptography.hazmat.primitives.serialization import pkcs12, Encoding, PrivateFormat, NoEncryption
from cryptography.hazmat.primitives import serialization
import tempfile
from contextlib import contextmanager
import xml.etree.ElementTree as ET
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
    """Decrypts the .p12 file and yields a .pem file path."""
    with open(p12_path, 'rb') as f:
        p12_data = f.read()

    private_key, certificate, additional_certs = pkcs12.load_key_and_certificates(
        p12_data,
        p12_password.encode() if p12_password else None,
    )

    with tempfile.NamedTemporaryFile(suffix='.pem', delete=False) as t_pem:
        if private_key:
            t_pem.write(
                private_key.private_bytes(
                    encoding=Encoding.PEM,
                    format=PrivateFormat.TraditionalOpenSSL,
                    encryption_algorithm=NoEncryption()
                )
            )

        if certificate:
            t_pem.write(
                certificate.public_bytes(Encoding.PEM)
            )

        if additional_certs:
            for cert in additional_certs:
                t_pem.write(cert.public_bytes(Encoding.PEM))

        t_pem.flush()
        yield t_pem.name

########## End PKCS#12 helpers ####################################

def mission_change(message):
    root = ET.fromstring(message)
    details = root.find("detail")

    if details is None:
        return False, False

    for c in details:
        if c.tag == "mission":
            change_type = c.attrib.get("type")
            mission_name = c.attrib.get("name")
            file_hashes = []

            for mission_changes in c:
                for mission_change in mission_changes:
                    for detail in mission_change:
                        if detail.tag == "contentResource" and detail.find("filename") is not None:
                            hash_elem = detail.find("hash")
                            if hash_elem is not None and hash_elem.text:
                                file_hashes.append(hash_elem.text)

            if file_hashes:
                return "download_mission_files", file_hashes

            if change_type in {"CHANGE", "CREATE", "INVITE", "DELETE"}:
                return change_type, mission_name

    return False, False


def is_sa_message(message):
    root = ET.fromstring(message)
    details = root.find("detail")
    if details is not None:
        for c in details:
            if c.tag == "contact":
                return True
    return False


def extract_mission_change(detail_obj, is_proto: bool = False):
    """
    Unified logic to detect mission change events.
    Returns tuple: (action_type: str or False, data: Any or False)
    """
    if is_proto:
        # Proto path (already using xmlDetail string)
        try:
            xml_details = ET.fromstring("<detail>" + detail_obj.xmlDetail + "</detail>")
        except Exception:
            return False, False
    else:
        # XML path
        xml_details = detail_obj  # already an Element

    for elem in xml_details:
        if elem.tag == "_flow-tags_":
            continue

        if elem.tag == "mission":
            change_type = elem.attrib.get("type")
            name = elem.attrib.get("name")
            file_hashes = []

            mission_changes = elem.find("MissionChanges")
            if mission_changes is not None:
                for change in mission_changes.findall("MissionChange"):
                    for res in change.findall("contentResource"):
                        hash_elem = res.find("hash")
                        if hash_elem is not None and hash_elem.text:
                            file_hashes.append(hash_elem.text)

            if file_hashes:
                return "download_mission_files", file_hashes

            if change_type in {"CHANGE", "CREATE", "INVITE", "DELETE"}:
                return change_type, name

    return False, False

def extract_fileshare(detail_obj, is_proto: bool = False):
    """
    Unified fileshare detection.
    Returns (hash: str or False, filename: str or False)
    """
    if is_proto:
        try:
            root = ET.fromstring("<details>" + detail_obj.xmlDetail + "</details>")
        except Exception:
            return False, False
    else:
        root = detail_obj  # XML detail element

    for elem in root:
        if elem.tag == "fileshare":
            return (
                elem.attrib.get("sha256"),
                elem.attrib.get("filename")
            )
    return False, False


prev_data = {'read': 0, 'write': 0, 'bytes': 0}
def print_data_dict(connection_data):
    total_clients = len(connection_data)
    connected_clients = 0
    num_writes = 0
    num_reads = 0
    #num_bytes = 0
    for client_data in connection_data.values():
        if client_data['connected']:
            connected_clients += 1
        num_reads += client_data['read']
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

    diff_reads = num_reads - prev_data['read']
    prev_data['read'] = num_reads 
    
    diff_writes = num_writes - prev_data['write']
    prev_data['write'] = num_writes 

    # ; num_reads/client: {reads:>10}; num_bytes: {bytes:>10}" \
    return "clients connected: {conn:>3} / {tot:>3}; num_writes: {writes:>10}; num_reads: {reads:>10} " \
        .format(conn=connected_clients,
                tot=total_clients,
                writes=diff_writes,
                reads=diff_reads) #,
                #bytes=diff_bytes)