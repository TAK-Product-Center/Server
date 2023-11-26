import './UploadFile.css';

import React, { useEffect } from 'react';
import Button from '@mui/material/Button';
import TextField from '@mui/material/TextField';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogContentText from '@mui/material/DialogContentText';
import DialogTitle from '@mui/material/DialogTitle';
import { useTheme } from '@mui/material/styles';
import ListItemButton from '@mui/material/ListItemButton';
import Box from '@mui/material/Box';
import OutlinedInput from '@mui/material/OutlinedInput';
import MenuItem from '@mui/material/MenuItem';
import Select from '@mui/material/Select';
import Chip from '@mui/material/Chip';
import Switch from '@mui/material/Switch';
import Collapse from '@mui/material/Collapse';
import ExpandLess from '@mui/icons-material/ExpandLess';
import ExpandMore from '@mui/icons-material/ExpandMore';
import Typography from '@mui/material/Typography';
import InputLabel from '@mui/material/InputLabel';

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

function UploadFile({openHook, setOpenHook, deleted, setDeleted, fileProp, setFileProp, selectedMission}) {
  // Hooks
    const [missions, setMissions] = React.useState([]);
    const [checked, setChecked] = React.useState([]);
    const [resourceName, setResourceName] = React.useState("");
    const [downloadPath, setDownloadPath] = React.useState("");
    const [lat, setLat] = React.useState("");
    const [lon, setLon] = React.useState("");
    const [openDetails, setOpenDetails] = React.useState(false);
    const [copChecked, setCopChecked] = React.useState(false);

    
    const theme = useTheme();

    // Nedded for hook set timing
    function timeout(delay) {
      return new Promise( res => setTimeout(res, delay) );
    }

    useEffect(() => {
      var selectArray = []
      if(selectedMission != null){
        selectArray.push(selectedMission)
        setChecked(selectArray)
      }
    },[selectedMission])

    useEffect(() => {
      fetch('/Marti/api/missions?passwordProtected=true&defaultRole=true')
          .then(response => response.json())
          .then(data => {
              var missionList = [];
              data.data.forEach(function (missionData) {
                missionList.push(missionData.name);
                allMissions.push(missionData.name);
                if (missionData.tool === "vbm"){
                  copMissions.push(missionData.name);
                }
              });
              setMissions(missionList);
          });
    },[openHook])

    async function handleUploadAndClose()  {
      submitForm();
      await timeout(100);
      clearData();
      handleClose();
    };

    // Needed for dropzone
    function getStyles(name, personName, theme) {
      return {
        fontWeight:
          personName.indexOf(name) === -1
            ? theme.typography.fontWeightRegular
            : theme.typography.fontWeightMedium,
      };
    }

    const handleClose = () => {
      setOpenHook(false);
      clearData();
      setCopChecked(false);
    };

    const toggleDetails = () => {
      setOpenDetails(!openDetails)
    }

    function clearData(){
      if(selectedMission != null){
        setChecked([selectedMission])
      } else {
        setChecked([]);
      }
      setFileProp("");
      setResourceName("");
      setDownloadPath("");
      setLat("");
      setLon("");
      setOpenDetails(false);
    }

    const handleLeaveOpen = () => {
      submitForm();
      clearData();
    };

    // Upload function for added file
    function submitForm() {
      const formData = new FormData();
      formData.append('assetfile', fileProp);
      formData.append('Name', resourceName);
      formData.append('DownloadPath', downloadPath);
      formData.append('Latitude', lat);
      formData.append('Longitude', lon);
      fetch('/Marti/sync/upload', {
        method: "POST",
        body: formData
      }).then(function (res) {
        if (res.ok) {
          res.json().then(data => {
            const missionsLength = checked.length;
              for (var i = 0; i < missionsLength; i++) {
                addMissionContents(data.Hash,checked[i], data.SubmissionUser);
              }
               
          });
        }
      }, function (e) {
        alert("Error submitting form!");
      });

    };

    // Called when user adds file to mission on upload
    function addMissionContents(hash, mission, creatorUid) {
      const data = "{ \"hashes\":[ \"" + hash + "\" ] }";
      var url = '/Marti/api/missions/' + mission + '/contents?creatorUid=' + creatorUid
      fetch(url, { 
        method: 'PUT',
        headers: {'Content-Type':'application/json'},
        body: data
      }).then((function (res) {
        if(!res.ok){
          console.log(res);
          }
        if(res.ok){
          console.log("Upload successful")
          setDeleted(!deleted)
        }
      }));
    }


    const handleChange = (event) => {
      const {
        target: { value },
      } = event;
      setChecked(
        // On autofill we get a stringified value.
        typeof value === 'string' ? value.split(',') : value,
      );
    };


    return(
    <Dialog open={openHook}
        fullWidth
        maxWidth="sm">
        <DialogContent>
        <DialogTitle sx={{pl: 0}}>File Upload</DialogTitle>
        
          <DialogContentText>
            {fileProp === "" ? "No File Selected, drop a file on dialog to upload": fileProp.name} 
            <br/>
          </DialogContentText>
          <DialogTitle sx={{pl: 0}}>Mission</DialogTitle>
          
          <DialogContentText>You may optionally associate the file with a mission.</DialogContentText>
          <div><Switch className='copSwitch'
                checked={copChecked}
                onChange={
                    (e) => {
                      setCopChecked(e.target.checked)
                        if(e.target.checked === false){
                            setMissions(allMissions)
                        } else {
                            setMissions(copMissions)
                        }
                    }
                }
                inputProps={{ 'aria-label': 'controlled' }}
            />
            <Typography className='copSwitch' variant="body1" style={{ marginRight: 16 }}>Show Only COPs</Typography>
          </div>
          {missions.length > 0 ? (
            <div>
            <InputLabel id="mission-chip-label">Mission</InputLabel>
            <Select style={{width: 350}}
              labelId="mission-chip-label"
              id="mission-multiple-chip"
              multiple
              value={checked}
              onChange={handleChange}
              input={<OutlinedInput id="select-multiple-chip" label="Chip" />}
              renderValue={(selected) => (
                <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
                  {selected.map((value) => (
                    <Chip key={value} label={value} />
                  ))}
                </Box>
              )}
              MenuProps={MenuProps}
            >
              {missions.map((mission) => (
                <MenuItem
                  key={mission}
                  value={mission}
                  style={getStyles(mission, missions, theme)}
                >
                  {mission}
                </MenuItem>
              ))}
            </Select>
            </div>
          ) : (
            <DialogContentText>No missions available.</DialogContentText>
          )}
          <ListItemButton sx={{pl: 0}} onClick={toggleDetails}>
            Optional Settings {openDetails ? <ExpandLess /> : <ExpandMore />}
          </ListItemButton>
          <Collapse in={openDetails}>
            <TextField
              margin="dense"
              id="name"
              value={resourceName}
              onChange={event => setResourceName(event.target.value)}
              label="Resource Name (Optional)"
              fullWidth
              variant="standard"
            />
            <TextField
              margin="dense"
              id="name"
              value={downloadPath}
              onChange={event => setDownloadPath(event.target.value)}
              label="Download Path (Optional)"
              fullWidth
              variant="standard"
            />
              
            <DialogTitle sx={{pl: 0}}>Resource Location</DialogTitle>
                <DialogContentText>
                You may enter an optional location to associate with the uploaded resource. For example, if the resource is a photo, the location could represent the place where the photo was taken.
                </DialogContentText>
                <DialogContentText>
                Location is optional but ATAK can only retrieve resources whose location metadata is defined.
                </DialogContentText>
                <TextField
                id="lat"
                margin="dense"
                step="any"
                value={lat}
                onChange={event => setLat(event.target.value)}
                error={lat !==  "" && (lat < -90 || lat > 90)}
                helperText={lat !==  "" && (lat < -90 || lat > 90) ? 'Latitude must be between -90 and 90':''}
                label="Latitude (decimal degrees)"
                variant="standard"
              />
              <TextField
                margin="dense"
                step="any"
                value={lon}
                onChange={event => setLon(event.target.value)}
                error={lon !==  "" && (lon < -180 || lon > 180)}
                helperText={lon !==  "" && (lon < -180 || lon > 180) ? 'Longitude must be between -180 and 180':''}
                id="name"
                label="Longitude (decimal degrees)"
                variant="standard"
              />
          </Collapse>
        </DialogContent>
        
        <DialogActions>
          <Button onClick={handleClose}>Close</Button>
          <Button onClick={handleUploadAndClose} disabled={fileProp === ""}>Upload and Close</Button>
          <Button onClick={handleLeaveOpen} disabled={fileProp === ""}>Upload and Leave Open</Button>
        </DialogActions>
    </Dialog>
    );
}

export default UploadFile;