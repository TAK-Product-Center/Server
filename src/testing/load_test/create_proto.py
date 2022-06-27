from datetime import datetime
from protobuf_msg.takmessage_pb2 import TakMessage
from lxml import etree

from create_cot import id_gen

MAGIC_BYTE = b'\xbf'


class CotProtoMessage:

    def __init__(self,
                 uid=None,
                 how="h-g-i-g-o",
                 type="a-f-G-U-C-I",
                 version="2.0",
                 lat="42", lon="-71",
                 msg=None):
        self.message = TakMessage()

        if msg is not None:
            self.deserialize_message(msg)
            self.uid = self.message.cotEvent.uid
            return

        if uid == None:
            uid = "PyTAK-" + id_gen()

        self.uid = uid

        # <event>
        event = self.message.cotEvent
        event.type = type
        now = int(datetime.now().timestamp()) * 1000
        event.staleTime = (now + 60) * 1000 # timestamp in seconds, so this adds a minute
        event.startTime = (now) * 1000
        event.sendTime = now
        event.how = how
        event.uid = self.uid

        # <point>
        event.lat = float(lat)
        event.lon = float(lon)
        event.hae = 262.0
        event.ce = 500
        event.le = 500

    def add_callsign_detail(self,
                            callsign=None,
                            endpoint="*:-1:stcp",
                            group_name="Black",
                            group_role="Team Member",
                            platform="PyTAKProto",
                            version="0.0.2"):

        if callsign is None:
            callsign = self.uid

        detail = self.message.cotEvent.detail

        contact = detail.contact
        contact.callsign = callsign
        contact.endpoint = endpoint

        group = detail.group
        group.name = group_name
        group.role = group_role

        takv = detail.takv
        takv.platform = platform
        takv.version = version

        return True

    def add_sub_detail(self, detail_name, sub_detail_name, sub_detail_attributes, sub_detail_text=None):
        details = etree.fromstring("<detail>" + self.message.cotEvent.detail.xmlDetail + "</detail>")
        for c in details:
            if detail_name == c.tag:
                detail = c
        else:
            detail = etree.SubElement(details, detail_name)

        sub_detail = etree.SubElement(detail, sub_detail_name)

        sub_detail.attrib.update(sub_detail_attributes)
        if sub_detail_text is not None:
            sub_detail.text = sub_detail_text

        xml_details = list()
        for elem in details.getchildren():
            xml_details.append(etree.tostring(elem).decode('utf-8'))
        self.message.cotEvent.detail.xmlDetail = "".join(xml_details)


    def deserialize_message(self, msg):
        if not type(msg) == bytes:
            raise TypeError("msg needs to be of type bytes")

        first_byte = msg[0]
        if first_byte.to_bytes(1, byteorder="big") != MAGIC_BYTE:
            raise Exception("Magic byte is wrong: " + str(first_byte))

        msg_size, msg = get_size_and_truncate(msg[1:])
        self.message.ParseFromString(msg)

    def serialize(self):
        msg = self.message.SerializeToString()
        size_bytes = get_size_bytes(msg)
        return MAGIC_BYTE + size_bytes + msg

    def is_sa(self):
        return self.message.cotEvent.detail.HasField("contact")

    def mission_change(self):
        xml_details = etree.fromstring("<detail>"+self.message.cotEvent.detail.xmlDetail+"</detail>")
        for xml_detail in xml_details.getchildren():
            if xml_detail.tag == "_flow-tags_":
                continue
            if xml_detail.tag == "mission":
                change_type = xml_detail.attrib.get("type")
                name = xml_detail.attrib.get("name")
                file_hashes = list()

                for node in xml_details.findall('.//mission/MissionChanges/MissionChange/contentResource/hash'):
                    file_hashes.append(node.text)

                if file_hashes:
                    return "download_mission_files", file_hashes

                if change_type in {"INVITE", "CREATE", "DELETE"}:
                    return change_type, name

        return False, False

    def fileshare(self):
        xml_details = etree.fromstring("<details>"+self.message.cotEvent.detail.xmlDetail+"</details>")
        for xml_detail in xml_details:
            if xml_detail.tag == "fileshare":
                file_hash = xml_detail.attrib.get("sha256")
                filename = xml_detail.attrib.get("filename")
                return file_hash, filename

        return False

def get_size_and_truncate(msg):
    size = 0
    shift = 0
    while True:
        byte = msg[0]
        if byte & 128 == 0:
            size = size | (byte << shift)
            return size, msg[1:]
        else:
            size = size | ((byte & 127) << shift)
            shift = shift + 7
            msg = msg[1:]

def get_msg_size(msg):
    size = 0
    shift = 0
    hdr_size = 2
    while len(msg) > 0:
        byte = msg[0]
        if byte & 128 == 0:
            size = size | (byte << shift)
            return size + hdr_size
        else:
            size = size | ((byte & 127) << shift)
            shift = shift + 7
            msg = msg[1:]
            hdr_size += 1
    return False

def get_size_bytes(msg):
    size_bytes = bytearray()
    msg_size = len(msg)
    while True:
        if msg_size & ~127 == 0:
            size_bytes.append(msg_size)
            return bytes(size_bytes)
        else:
            size_byte = (msg_size & 127) | 128
            size_bytes.append(size_byte)
            msg_size = msg_size >> 7




