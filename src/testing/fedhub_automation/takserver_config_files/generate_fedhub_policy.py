import json
import os
import subprocess
import sys
import uuid

def extract_ca_info(pem_file_path):
    try:
        # Run the OpenSSL command to get the fingerprint
        fingerprint_output = subprocess.check_output(["openssl", "x509", "-in", pem_file_path, "-noout", "-fingerprint", "-sha256"]).decode("utf-8")

        # Extract the fingerprint from the output
        fingerprint = fingerprint_output.split("sha256 Fingerprint=")[1].strip()

        # Remove colons and spaces, and convert to lowercase
        cleaned_fingerprint = fingerprint.replace(":", "").replace(" ", "").lower()

        # Run the OpenSSL command to get the subject
        subject_output = subprocess.check_output(["openssl", "x509", "-noout", "-subject", "-in", pem_file_path]).decode("utf-8")
        
        # Extract the subject from the output
        subject = subject_output.split("subject=")[1].strip()

        # Split the subject into individual fields
        fields = subject.split(", ")

        # Reorder and remove spaces from the fields
        formatted_fields = [f.replace(" ", "") for f in fields[::-1]]

        # Join the fields back together with commas
        formatted_subject = ",".join(formatted_fields)

        # Combine formatted_subject and cleaned_fingerprint with a hyphen
        result = f"{formatted_subject}-{cleaned_fingerprint}"

        return result
    except subprocess.CalledProcessError as e:
        print(f"Error processing {pem_file_path}: {e}")
        return None

def read_pem_file(pem_file_path):
    try:
        with open(pem_file_path, 'r') as file:
            lines = file.readlines()
            content = ''.join(line.strip() for line in lines[1:-1])  # Exclude first and last line
            return content
    except Exception as e:
        print(f"Error reading {pem_file_path}: {e}")
        return None
                        
def generate_ui_policy(ca_contents, connection_index):
    ui_policy = {
        "name": "Testing",
        "federate_edges": [],
        "groups": [],
        "additionalData": {
            "uiData": {
                "name": "Testing",
                "version": "",
                "type": "",
                "description": "Testing",
                "thumbnail": None,
                "cells": [],
                "diagramType": "Federation"
            }
        }
    }

    z = 0
    index = 0

    ca_group_cell_uuids = []

    # Add groups and cells
    for ca_name,pem_contents in ca_contents:
        group = {
            "uid": ca_name,
            "interconnected": False
        }
        ui_policy["groups"].append(group)

        node_name = "BBN" + str(index)

        ca_group_cell_uuids.append(str(uuid.uuid4()))
        
        cell = {
            "graphType": "GroupCell",
            "id": ca_group_cell_uuids[index],
            "attrs": {
                ".body": {"fill": "white", "opacity": "0.35"},
                ".inner": {"visibility": "hidden"},
                "path": {"ref": ".outer"},
                "image": {"ref": ".outer", "ref-dy": "", "ref-y": 5, "xlink:href": "data:image/png;base64," + pem_contents},
                "text": {"ref-y": 0.5},
                ".content": {"text": node_name},
                ".fobj": {"width": 200, "height": 150},
                "div": {"style": {"width": 200, "height": 150}},
                ".fobj div": {"style": {"verticalAlign": "middle", "paddingTop": 0}},
                ".outer": {"stroke-width": 1, "stroke-dasharray": "none"},
                ".sub-process": {"visibility": "hidden", "data-sub-process": ""}
            },
            "type": "bpmn.Activity",
            "roger_federation": {
                "name": ca_name,
                "id": None,
                "description": node_name,
                "attributes": [],
                "interconnected": False,
                "groupFilters": [],
                "type": "Group",
                "stringId": node_name
            },
            "size": {"width": 200, "height": 150},
            "activeConnections": [],
            "icon": "circle",
            "angle": 0,
            "z": str(z),
            "position": {"x": 1064, "y": 1346},
            "activityType": "task",
            "embeds": "",
            "content": node_name
        }
        ui_policy["additionalData"]["uiData"]["cells"].append(cell)

        z += 1
        index += 1

    # Add edges between each pair of federates

    connection_index_int = int(connection_index)

    for i in range(len(ca_contents)):

        num_connections = 0
        
        for j in range(i + 1, i + 1 + len(ca_contents)):

            if j >= len(ca_contents):
                j = j - len(ca_contents)
            
            if i == j:
                continue

            # take connection_index into account as well 
            if num_connections >= connection_index_int:
                continue
            
            edge1 = {
                "source": ca_contents[i][0],
                "destination": ca_contents[j][0],
                "groupsFilterType": "ALL"
            }
            edge2 = {
                "source": ca_contents[j][0],
                "destination": ca_contents[i][0],
                "groupsFilterType": "ALL"
            }
            ui_policy["federate_edges"].extend([edge1, edge2])
            
            name = "BBN" + str(i) + " -> BBN" + str(j)

            cell = {
                "graphType": "EdgeCell",
                "id": str(uuid.uuid4()),
                "attrs": {},
                "type": "bpmn.Flow",
                "roger_federation": {
                    "name": name,
                    "allowedGroups": [],
                    "disallowedGroups": [],
                    "groupsFilterType": "allGroups",
                    "edgeFilters": [],
                    "type": "Federate Policy"
                },
                "router": {
                    "name": "metro"
                },
                "connector": {
                    "name": "rounded"
                },
                "z": str(z),
                "source": {
                    "id": ca_group_cell_uuids[i]
                },
                "embeds": "",
                "flowType": "normal",
                "target": {
                    "id": ca_group_cell_uuids[j]
                },
                "labels": [
                    {
                        "position": 0.5,
                        "attrs": {
                            "text": {
                                "text": name
                            }
                        }
                    }
                ]
            }
            ui_policy["additionalData"]["uiData"]["cells"].append(cell)
        
            z += 1

            num_connections += 1

    return json.dumps(ui_policy, indent=4)

def main(directory, connection_index):
    ca_contents = []
    
    for subdir in os.listdir(directory):
        if subdir.startswith("takserver_config_"):
            
            pem_file_path = os.path.join(directory, subdir, "files", "ca.pem")
            if os.path.exists(pem_file_path):
                
                result = extract_ca_info(pem_file_path)
                pem_contents = read_pem_file(pem_file_path)

                if result and pem_contents:
                    ca_contents.append(tuple((result, pem_contents)))
                    
    if ca_contents:
        json_policy = generate_ui_policy(ca_contents, connection_index)
        print(json_policy)
    else:
        print("No valid CA information found.")

if __name__ == "__main__":
    if len(sys.argv) != 3:
        print("Usage: python script.py <path_to_directory> <connection_index>")
        sys.exit(1)
    directory_path = sys.argv[1]
    connection_index = sys.argv[2]
    main(directory_path, connection_index)
