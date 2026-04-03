from utils import extract_mission_change, extract_fileshare

import random
import string
import time

from datetime import datetime, timedelta
import xml.etree.ElementTree as ET
import xml.dom.minidom

from utils import extract_mission_change, extract_fileshare

def id_gen(size=10):
    chars = string.ascii_letters + string.digits
    return "".join(random.choice(chars) for s in range(size))

def cot_time_string(now=None):
    if now is None:
        now = datetime.utcnow()

    cur_time = str(now.year).zfill(4)+"-"+str(now.month).zfill(2)+"-"+str(now.day).zfill(2) + \
        "T"+str(now.hour).zfill(2)+":"+str(now.minute).zfill(2)+":"+str(now.second).zfill(2)+"."+str(int(now.microsecond/1000)).zfill(3)+"Z"

    return cur_time


class CotMessage:

    def __init__(self,
                 uid=None,
                 how="h-g-i-g-o",
                 type="a-f-G-U-C-I",
                 version="2.0",
                 lat="0", lon="0",
                 msg=None):

        self.detail = None

        if msg is not None:
            self.deserialize(msg)
            return


        if uid == None:
            uid = "PyTAK-" + id_gen()

        self.uid = uid

        self.event = ET.Element(
            "event",
            {
                "how": how,
                "stale": cot_time_string(datetime.utcnow() + timedelta(minutes=1)),
                "start": cot_time_string(),
                "time": cot_time_string(),
                "type": type,
                "uid": self.uid,
                "version": version,
            }
        )

        self.point = ET.SubElement(self.event, "point")
        self.point.attrib.update({
            "ce": "500",
            "hae": "262.0",
            "lat": lat,
            "le": "500",
            "lon": lon
        })
        self.detail = ET.SubElement(self.event, "detail")


    def add_callsign_detail(self,
                            callsign=None,
                            endpoint="*:-1:stcp",
                            group_name="Black",
                            group_role="Team Member",
                            platform="PyTAK",
                            version="0.0.1"):

        if callsign is None:
            callsign = self.uid

        contact_attrib = {"callsign": callsign,
                          "endpoint": endpoint}
        self.add_detail("contact", contact_attrib)

        __group_attrib = {"name": group_name,
                          "role": group_role}
        self.add_detail("__group", __group_attrib)

        takv_attrib = {"platform": platform,
                            "version": version}
        self.add_detail("takv", takv_attrib)

        return True

    def add_detail(self, detail_name, detail_attributes, detail_text=None, duplicate=False, edit=False):
        if detail_name in (c.tag for c in self.detail) and not duplicate:
            return False

        if edit:
            for c in self.detail:
                if c.tag == detail_name:
                    sub_detail = c
            else:
                return False # we are trying to edit a tag that doesn't exist

        else:
            sub_detail = ET.SubElement(self.detail, detail_name)

        sub_detail.attrib.update(detail_attributes)

        if detail_text is not None:
            sub_detail.text = detail_text

        return True

    def add_sub_detail(self, detail_name, sub_detail_name, sub_detail_attributes, sub_detail_text=None):
        for c in self.detail:
            if detail_name == c.tag:
                detail = c

        else:
            detail = ET.SubElement(self.detail, detail_name)

        sub_detail = ET.SubElement(detail, sub_detail_name)

        sub_detail.attrib.update(sub_detail_attributes)
        if sub_detail_text is not None:
            sub_detail.text = sub_detail_text

        return True

    def is_sa(self):
        if self.detail is None: # Pong messages might not have a detail tag
            return False
        for c in self.detail:
            if c.tag == "contact":
                return True
        return False

    def is_pong(self):
        if self.event.attrib.get("type") == "t-x-c-t-r":
            return True
        return False

    def mission_change(self):
        return extract_mission_change(self.detail, is_proto=False)

    def fileshare(self):
        return extract_fileshare(self.detail, is_proto=False)


    def to_string(self, pretty_print=False):
        rough_string = ET.tostring(self.event, encoding='utf-8')
        reparsed = xml.dom.minidom.parseString(rough_string)
        if pretty_print == True:
            return reparsed.toprettyxml(indent="  ")
        else:
            return reparsed.toxml()

    def deserialize(self, msg):
        self.event = ET.fromstring(msg)
        self.uid = self.event.attrib.get("uid")
        for elem in self.event:
            if elem.tag == 'detail':
                self.detail = elem
            elif elem.tag == 'point':
                self.point = elem

    def server_protocol_version_support(self):
        tak_control = self.detail.find("TakControl")
        if tak_control is not None:
            protocol_message = tak_control.find("TakProtocolSupport")
            if protocol_message is not None:
                return protocol_message.attrib.get('version')
        return False

    def server_protocol_negotiation_handshake(self):
        tak_control = self.detail.find("TakControl")
        if tak_control is not None:
            tak_response = tak_control.find("TakResponse")
            if tak_response is not None and tak_response.attrib.get("status") == "true":
                return True
        return False

    def protocol_response_message(self, version="1"):
        self.point.attrib.update({"lat": "0.0",
                                  "lon": "0.0",
                                  "hae": "0.0",
                                  "ce": "999999",
                                  "le": "999999"})
        self.event.attrib.update({"type": "t-x-takp-q",
                                  "how": "m-g"})
        self.add_sub_detail("TakControl", "TakRequest", sub_detail_attributes={"version": version})

    def __str__(self):
        return ET.tostring(self.event, encoding='utf-8').decode()

if __name__ == "__main__":
    event = CotMessage()

    event.add_callsign_detail()
