import React, { useEffect } from 'react';
import Button from '@mui/material/Button';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogTitle from '@mui/material/DialogTitle';
import Box from '@mui/material/Box';
import OutlinedInput from '@mui/material/OutlinedInput';
import MenuItem from '@mui/material/MenuItem';
import Select from '@mui/material/Select';
import Chip from '@mui/material/Chip';

// Constants to use throughout the component
const ITEM_HEIGHT = 48;
const ITEM_PADDING_TOP = 8;
const MenuProps = {
  PaperProps: {
    style: {
      maxHeight: ITEM_HEIGHT * 4.5 + ITEM_PADDING_TOP,
      width: 350,
    },
  },
};

// Structures for missions
var allMissions = [];
var copMissions = [];

function SendInviteMission({openHook, setOpenHook, sendBool, missionSelected}) {
  // Hooks
    const [contacts, setContacts] = React.useState([]);
    const [checked, setChecked] = React.useState([]);

    useEffect(() => {
      fetch('/Marti/api/contacts/all')
      .then(response => response.json())
      .then(data => {
          var contactsArray = []
          data.forEach(function (rowData) {
            if (rowData.uid !== ""){
              var struct = {
                name: rowData.callsign,
                uid: rowData.uid
              }
              contactsArray.push(struct);
            }
          })
          setContacts(contactsArray);
      });
      setChecked([]);
    },[openHook])

    const handleContacts = (event) => {
      const {
        target: { value },
      } = event;
      setChecked(
        // On autofill we get a stringified value.
        typeof value === 'string' ? value.split(',') : value,
      );
    };
  

    const sendOrInviteMission = () => {
      var url = ""
      if(sendBool){
        url = "/Marti/api/missions/" + missionSelected[0].name + "/send"
      } else {
        url = "/Marti/api/missions/" + missionSelected[0].name + "/invite"
      }

      var formBody = [];
      checked.forEach(function (arrayItem) {
        formBody.push("contacts=" + arrayItem.uid)
      })
      formBody = formBody.join("&");
      fetch(url, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8'
        },
        body: formBody
      }).then(function (res) {
        if (res.ok) {setOpenHook(false)}
      })
    }

    return(
    <Dialog open={openHook}
        fullWidth
        maxWidth="sm">
        <DialogContent>
        <DialogTitle sx={{pl: 0}}>{sendBool ? "Send Mission Package" : "Send Mission Invite"}</DialogTitle>
          <DialogContent>{missionSelected.length > 0  ? "Mission Name: " + missionSelected[0].name : ""} </DialogContent>
        
          <DialogTitle sx={{pl: 0}}>Send Mission To</DialogTitle>
          
            <div>
            <Select style={{width: 350}}
              labelId="mission-chip-label"
              id="mission-multiple-chip"
              multiple
              value={checked}
              onChange={handleContacts}
              input={<OutlinedInput id="select-multiple-chip" label="Chip" />}
              renderValue={(selected) => (
                <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
                  {selected.map((value) => (
                    <Chip key={value.name} label={value.name} />
                  ))}
                </Box>
              )}
              MenuProps={MenuProps}
            >
              {contacts.map((contact) => (
                <MenuItem
                  key={contact.name}
                  value={contact}
                >
                  {contact.name}
                </MenuItem>
              ))}
            </Select>
            </div>
        </DialogContent>
        
        <DialogActions>
          <Button onClick={() => {setOpenHook(false)}}>Close</Button>
          <Button onClick={sendOrInviteMission} >Send</Button>
        </DialogActions>
    </Dialog>
    );
}

export default SendInviteMission;