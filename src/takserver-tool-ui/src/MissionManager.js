import './MissionManager.css'
import SendInviteMission from './SendInviteMission';
import TransferList from './TranferList';
import React, { useEffect, useRef } from 'react';
import { styled, useTheme } from '@mui/material/styles';
import { DataGrid, GridToolbarContainer, getGridStringOperators} from '@mui/x-data-grid';
import Box from '@mui/material/Box';
import Drawer from '@mui/material/Drawer';
import CssBaseline from '@mui/material/CssBaseline';
import MuiAppBar, { AppBarProps as MuiAppBarProps } from '@mui/material/AppBar';
import PropTypes from 'prop-types';
import Typography from '@mui/material/Typography';
import Tabs from '@mui/material/Tabs';
import Tab from '@mui/material/Tab';
import DownloadIcon from '@mui/icons-material/Download';
import DeleteIcon from '@mui/icons-material/Delete';
import { Button, IconButton, ListItemText } from '@mui/material';
import List from '@mui/material/List';
import ListItem from '@mui/material/ListItem';
import ListItemButton from '@mui/material/ListItemButton';
import AddIcon from '@mui/icons-material/Add';
import EditIcon from '@mui/icons-material/Edit';
import RssFeedIcon from '@mui/icons-material/RssFeed';
import SendIcon from '@mui/icons-material/Send';
import TextField from '@mui/material/TextField';
import MailIcon from '@mui/icons-material/Mail';
import Select from '@mui/material/Select';
import InputLabel from '@mui/material/InputLabel';
import MenuItem from '@mui/material/MenuItem';
import FormControl from '@mui/material/FormControl';
import Chip from '@mui/material/Chip';
import OutlinedInput from '@mui/material/OutlinedInput';
import moment from 'moment';
import Backdrop from '@mui/material/Backdrop';
import CircularProgress from '@mui/material/CircularProgress';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogContentText from '@mui/material/DialogContentText';
import DialogTitle from '@mui/material/DialogTitle';
import {useDropzone} from 'react-dropzone';
import UploadFile from './UploadFile';
import ArchiveIcon from '@mui/icons-material/Archive';
import Switch from '@mui/material/Switch';
import DataObjectIcon from '@mui/icons-material/DataObject';
import CodeIcon from '@mui/icons-material/Code';
import Autocomplete from '@mui/material/Autocomplete';
import Snackbar from '@mui/material/Snackbar';
import MuiAlert from '@mui/material/Alert';
import Tooltip from '@mui/material/Tooltip';
import { SubscriptRounded } from '@mui/icons-material';

const drawerWidth = 600;

const mockedRows = [{ id: '1', password: "hh", groups: ["__ANON__"], defaultRole: "__ANON__", type: 'public', chatRoom: "A", name: 'Row1', description: 'fdf', contents: ['file.txt'], keywords: 'dfd', uid: ['uid1'],
              dataFeeds: [{id: 0, type:'stream'}]},
              { id: '2', password: "", groups: [], type: 'vbm', chatRoom: "", hash: 'ID', name: 'Row1', description: 'fdf', contents: ['dfd', 'fdf', '1', '6a3d784d-a17f-44f1-b160-337852e8ae3b'], keywords: 'dfd', uid: ['dfd', 'fdf'],
              dataFeeds: [{id: 1, type:'predicate'}] }]

const available = [{name: 'Hello', id: "0", type:'stream'},{name: 'World', id:"1", type:'predicate'},{name: 'Test', id:"2", type:'plugin'},{name: 'Stream', id: "3", type:'federate'},
{name: 'Sweet', id:"4", type:'federate'}]

const Alert = React.forwardRef(function Alert(props, ref) {
  return <MuiAlert elevation={6} ref={ref} variant="filled" {...props} />;
});


              // Reset object needed for componets that access the selected row hook
const emptyRow = {name: "", hash: ""};
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

function not(a, b) {
  return a.filter((value) => b.indexOf(value) === -1);
}

const Main = styled('main', { shouldForwardProp: (prop) => prop !== 'open' })(
  ({ theme, open }) => ({
    flexGrow: 1,
    padding: theme.spacing(3),
    transition: theme.transitions.create('margin', {
      easing: theme.transitions.easing.sharp,
      duration: theme.transitions.duration.leavingScreen,
    }),
    marginLeft: `-${drawerWidth}px`,
    ...(open && {
      transition: theme.transitions.create('margin', {
        easing: theme.transitions.easing.easeOut,
        duration: theme.transitions.duration.enteringScreen,
      }),
      marginLeft: 0,
    }),
  }),
);

function TabPanel(props) {
  const { children, value, index, ...other } = props;

  return (
    <div
      role="tabpanel"
      hidden={value !== index}
      id={`simple-tabpanel-${index}`}
      aria-labelledby={`simple-tab-${index}`}
      {...other}
    >
      {value === index && (
        <Box sx={{ p: 3 , minHeight: '90vh'}}>
          <Typography>{children}</Typography>
        </Box>
      )}
    </div>
  );
}

TabPanel.propTypes = {
  children: PropTypes.node,
  index: PropTypes.number.isRequired,
  value: PropTypes.number.isRequired,
};

function a11yProps(index) {
  return {
    id: `simple-tab-${index}`,
    'aria-controls': `simple-tabpanel-${index}`,
  };
}

// Custom Toolbar for data grid
function MissionManagerToolbar({rowsSelected, setMultiDeleteOpen, handleAdd,
                             handleEdit, handleDataFeeds, handleSendInvite, handleSendPackage,
                            handleArchivedMissionsOpen}) 
{
    return (
        <GridToolbarContainer>
            <Typography variant="h5" className="FileManagerLabel">Mission (COP) Manager</Typography>
            <Button className="Toolbar" color='error' startIcon={<DeleteIcon />}
            disabled={rowsSelected.length===0} onClick={
            (e) => {
                setMultiDeleteOpen(true);
            }  
            }>Delete</Button>
            <Button className="Toolbar" color='primary' startIcon={<AddIcon />}
            onClick={handleAdd}>Add </Button>
            <Button className="Toolbar" color='primary' startIcon={<DownloadIcon />}
             href={rowsSelected.length===1 ? 'api/missions/'+ rowsSelected[0].name + '/archive' : ""} 
             disabled={rowsSelected.length!==1}>Download</Button>
            <Button className="Toolbar" color='primary' startIcon={<EditIcon />}
            onClick={handleEdit} disabled={rowsSelected.length!==1}>Edit</Button>
            <Button className="Toolbar" color='primary' startIcon={<RssFeedIcon />}
            onClick={handleDataFeeds} disabled={rowsSelected.length!==1}>Data Feeds</Button>
            <Button className="Toolbar" color='primary' startIcon={<SendIcon />}
            onClick={handleSendPackage} disabled={rowsSelected.length!==1}>Send</Button>
            <Button className="Toolbar" color='primary' startIcon={<MailIcon />}
            onClick={handleSendInvite} disabled={rowsSelected.length!==1}>Invite</Button>
            <Button className="Toolbar" color='primary' startIcon={<CodeIcon />}
             href={rowsSelected.length===1 ? 'api/missions/'+ rowsSelected[0].name + '/kml' : ""} 
             disabled={rowsSelected.length!==1}>Kml Network Link</Button>
             <Button className="Toolbar" color='primary' startIcon={<DownloadIcon />}
             href={rowsSelected.length===1 ? 'api/missions/'+ rowsSelected[0].name + '/kml?download=true' : ""} 
             disabled={rowsSelected.length!==1}>Download KML</Button>
             <Button className="Toolbar" color='primary' startIcon={<ArchiveIcon />}
            onClick={handleArchivedMissionsOpen}>Archived Missions </Button>
        </GridToolbarContainer>
    );
}


const AppBar = styled(MuiAppBar, {
  shouldForwardProp: (prop) => prop !== 'open',
})(({ theme, open }) => ({
  transition: theme.transitions.create(['margin', 'width'], {
    easing: theme.transitions.easing.sharp,
    duration: theme.transitions.duration.leavingScreen,
  }),
  ...(open && {
    width: `calc(100% - ${drawerWidth}px)`,
    marginLeft: `${drawerWidth}px`,
    transition: theme.transitions.create(['margin', 'width'], {
      easing: theme.transitions.easing.easeOut,
      duration: theme.transitions.duration.enteringScreen,
    }),
  }),
}));
const DrawerHeader = styled('div')(({ theme }) => ({
  display: 'flex',
  alignItems: 'center',
  padding: theme.spacing(0, 1),
  // necessary for content to be below app bar
  ...theme.mixins.toolbar,
  justifyContent: 'flex-end',
}));

const stringOperators = getGridStringOperators().filter((op => ['contains'].includes(op.value)));

const columns = [
  { field: 'id', headerName: 'ID', width: 1 },
  { field: 'hash', headerName: 'hash', width: 1 },
  { field: 'name', headerName: 'Name', width: 305, filterOperators: stringOperators,
  renderCell: (params) => {
      return (
          <div>
            <Tooltip title="View Json">
                <Button href={'api/missions/'+ params.value} 
                onClick={ (e) => {e.stopPropagation();}}
                aria-label="Download" style={{textTransform: 'none'}}>
                {params.value}
                </Button>
              </Tooltip>
              
          </div>
      );
    } },
  { field: 'description', headerName: 'Description', width: 185, sortable: false, filterable: false },
  { field: 'contents', headerName: 'Contents', width: 265, sortable: false, filterable: false,
  renderCell: (params) => {
    return (
      <List dense sx={{maxHeight: 100, overflow: 'auto', pl: 0}}>
        {params.value.map((value) =>(
          <ListItem sx={{pl: 0}}
          key={value.name}>
            <ListItemButton sx={{ height: 20, pl: 0 }} href={'/Marti/api/files/'+ value.hash}  
            onClick={ (e) => {e.stopPropagation();}} >
            <ListItemText sx={{ color: '#1769aa' }}
             primary={value.name}/>
            </ListItemButton>
          </ListItem>
        ))}
      </List>
    )
  } },
  { field: 'dataFeeds', headerName: 'Data Feeds', width: 115, sortable: false, filterable: false,
   renderCell: (params) => {
    return (
      <List dense sx={{maxHeight: 100, overflow: 'auto', pl: 0}}>
        {params.value.map((value) =>(
          <ListItem sx={{pl: 0}}
          key={value.name}>
            <ListItemButton 
            sx={{ height: 20, pl: 0 }} href={'/Marti/inputs/index.html#!/modifyPluginDataFeed/'+ value.name}  
            onClick={ (e) => {e.stopPropagation();}} >
            <ListItemText sx={{ color: '#1769aa' }}
             primary={value.name}/>
            </ListItemButton>
          </ListItem>
        ))}
      </List>
    )
  } },
  { field: 'groups', headerName: 'Groups', width: 180, sortable: false, filterable: false,
  renderCell: (params) => {
    return (
      <List dense sx={{maxHeight: 100, overflow: 'auto', pl: 0}}>
        {params.value.map((value) =>(
          <ListItem sx={{pl: 0}}
          key={value}>
            <ListItemText sx={{ color: '#1769aa' }}
             primary={value}/>
          </ListItem>
        ))}
      </List>
    )
  } },
  { field: 'tool', headerName: 'Tool', width: 155, filterOperators: stringOperators },
  { field: 'create_time', headerName: 'Create Time', width: 180, filterable: false,
  renderCell: (params) => {
      return (
          <TextField variant="standard"
          value={moment(params.value).format("YYYY-MM-DDThh:mm")}
          InputProps={{
              disableUnderline: true,
            }}
          />
      ) }
  },
  { field: 'uid', headerName: 'UID', width: 210, sortable: false, filterOperators: stringOperators,
  renderCell: (params) => {
    return (
      <List dense sx={{maxHeight: 100, overflow: 'auto', pl: 0}}>
        {params.value.map((value) =>(
          <ListItem sx={{pl: 0}}
          key={value}>
            <ListItemButton sx={{ height: 20, pl: 0 }} href={'/Marti/api/cot/xml/'+ value}  
            onClick={ (e) => {e.stopPropagation();}} >
            <ListItemText sx={{ color: '#1769aa' }}
             primary={value}/>
            </ListItemButton>
          </ListItem>
        ))}
      </List>
    )
  }},
  { field: 'keywords', headerName: 'Keywords', width: 215, sortable: false, filterable: false },
  { field: 'creatorUid', headerName: 'Creator Uid', width: 120, sortable: false, filterable: false }
];

// Structures for missions
var allMissions = [];
var copMissions = [];

function MissionManager() {

  // Dialog open hooks
  const [multiDeleteOpen, setMultiDeleteOpen] = React.useState(false);
  const [uploadOpen, setUploadOpen] = React.useState(false);
  // Selected Row hooks
  const [rowsSelected, setRowsSelected] = React.useState([]);
  const [rows, setRows] = React.useState(mockedRows);

  // Hooks for filters
  const [expiration, setExpiration] = React.useState("");
  const [missionChanged, setMissionChanged] = React.useState(false);
  const [value, setValue] = React.useState(0);
  const [age, setAge] = React.useState("");

  const [checked, setChecked] = React.useState([]);

  // Pagination system for grid
  const [page, setPage] = React.useState(0);
  const [paginationModel, setPaginationModel] = React.useState({
    pageSize: 6,
    page: 0,
  });
  const [rowCount, setRowCount] = React.useState(100);
  const [loading, setLoading] = React.useState(false);
  const [apiSent, setApiSent] = React.useState(false);

  // Form hooks for Add
  const [name, setName] = React.useState("");
  const [description, setDescription] = React.useState("");
  const [chatRoom, setChatRoom] = React.useState("");
  const [password, setPassword] = React.useState("");
  const [defaultRole, setDefaultRole] = React.useState("");
  const [groups, setGroups] = React.useState([]);
  const [allGroups, setAllGroups] = React.useState([]);
  const [disableEditAndFeeds, setDisableEditAndFeeds] = React.useState(true);
  const [disableAdd, setDisableAdd] = React.useState(false);
  const [copChecked, setCopChecked] = React.useState(false);
  const [tool, setTool] = React.useState("");

  // Extra hooks for Edit
  const [contents, setContents] = React.useState([]);
  const [uid, setUIDs] = React.useState([]);
  const [uidToAdd, setUidToAdd] = React.useState("");
  const [uidOptions, setUidOptions] = React.useState("");
  const [uidOriginal, setUidOriginal] = React.useState([]);
  const [contentsOriginal, setContentsOriginal] = React.useState([]);
  const [keywordToAdd, setKeywordToAdd] = React.useState("");
  const [keywords, setKeywords] = React.useState([]);
  const [keywordOriginal, setKeywordOriginal] = React.useState([]);
  const [keywordShrink, setKeywordShrink] = React.useState(false);
  const [uidShrink, setUidSkrink] = React.useState(false);

  // Data feeds hooks for page
  const [dataFeedsSelected, setDataFeedsSelected] = React.useState([]);
  const [resetDataFeeds, setResetDataFeeds] = React.useState(false);
  const [dataFeedsAvailable, setDataFeedsAvailable] = React.useState(available);
  const [openSendInvite, setOpenSetInvite] = React.useState(false);
  const [sendBool, setSendBool] = React.useState(false);
  const [archivedMissions, setArchivedMissions] = React.useState([]);

  //VBM mode toast
  const [vbmMode, setVbmMode] = React.useState(false);
  const [vbmToastOpen, setVbmToastOpen] = React.useState(false);
  const [vbmSeverity, setVbmSeverity] = React.useState("");

  // Sorting via paged missions api
  const [sort, setSort] = React.useState("");
  const [filter, setFilter] = React.useState("");

  const [archivedMissionOpen, setArchivedMissionOpen] = React.useState(false);

  const handleSortModelChange = (newModel) => {
    if(newModel.length === 0){
        setSort("");
    } else{
        if(newModel[0].sort === "asc"){
            setSort("&sort="+newModel[0].field+"&ascending=true")
        } 
        if(newModel[0].sort === "desc"){
            setSort("&sort="+newModel[0].field+"&ascending=false")
        }
    }
  };

  const handleFilterModelChange = (newModel) => {
    if(newModel.items !== null && newModel.items.length === 0){
      setFilter("")
    } else {
      // Model value can be null if set by user
      if(newModel.items[0].value){
        if (newModel.items[0].field === "name"){
          setFilter("&nameFilter="+newModel.items[0].value)
        }
        if (newModel.items[0].field === "tool"){
          setFilter("&tool="+newModel.items[0].value)
        }
        if (newModel.items[0].field === "uid"){
          setFilter("&uidFilter="+newModel.items[0].value)
        }
      } else {
        setFilter("")
      }
    }
    console.log(filter)
  };

  // Dropzone state management
  const [fileProp, setFileProp] = React.useState("");
  const {getRootProps, getInputProps, open} = useDropzone({
      // Note how this callback is never invoked if drop occurs on the inner dropzone
      noClick: true,
      onDrop: files => { 
          setFileProp(files[0]);
          setUploadOpen(true);
      }
  });

  function addOrEditMission(setValue, value, setApiSent, setMissionChanged, missionChanged, closeAfter) {
    const requestOptions = {
      method : 'PUT'
    }
    var url = '/Marti/api/missions/'+ name + '?';
    if(description !== ""){
      url += '&description=' + description;
    }
    if(chatRoom !== ""){
      url += '&chatRoom=' + chatRoom;
    }
    if(password !== "" && password !== "fakePassword"){
      url += '&password=' + password;
    }
    if(groups.lenth !== 0){
      groups.forEach(function (group) {
        url += "&group=" + group;
      })
      url += "&allowGroupChange=true";
    }
    if(tool.length !== 0){
      url += "&tool=" + tool;
    }
    if(defaultRole !== "" && defaultRole != null){
      url += "&defaultRole=" + defaultRole;
    }
    if(! isNaN(expiration) && expiration != null){
      url += "&expiration=" + -1;
    } else {
      url += "&expiration=" + expiration;
    }
    setApiSent(true);
    // delete log once complete
    console.log("making request "+ url)
    fetch(url, requestOptions)
        .then(response => {
          if(!response.ok) console.log("response failed")
          if(response.ok) {
            console.log("response OK")
            handleAddUIDs();
            if(keywords !== []){
              handleSetKeywords();
            }
            deleteUIDs();
            deleteContents();
            setApiSent(false);
            setMissionChanged(!missionChanged);
            setValue(value);
          };
        })
  }

  const handleChange = (event, newValue) => {
    setValue(newValue);
  };

  const handleChip = (event) => {
    const {
      target: { value },
    } = event;
    setGroups(
      // On autofill we get a stringified value.
      typeof value === 'string' ? value.split(',') : value,
    );
  };

  const handleContents = (event) => {
    const {
      target: { value },
    } = event;
    setContents(
      // On autofill we get a stringified value.
      typeof value === 'string' ? value.split(',') : value,
    );
  };

  const handleUid = (event) => {
    const {
      target: { value },
    } = event;
    if (value.length === 0){
      setUidSkrink(false);
    } else{
      setUidSkrink(true);
    }
    setUIDs(
      // On autofill we get a stringified value.
      typeof value === 'string' ? value.split(',') : value,
    );
  };

  const handleKeywordsChip = (event) => {
    const {
      target: { value },
    } = event;
    if (value.length === 0){
      setKeywordShrink(false);
    } else{
      setKeywordShrink(true);
    }
    setKeywords(
      // On autofill we get a stringified value.
      typeof value === 'string' ? value.split(',') : value,
    );
  };

    
    useEffect(() => {
      setLoading(true);
      const pageSize = paginationModel.pageSize
      var url = '/Marti/api/pagedmissions?passwordProtected=true&defaultRole=true&page='
      +page+'&pagesize='+ pageSize + sort + filter
        fetch(url)
            .then(response => {
              if(!response.ok) throw new Error(response.status);
              else return response.json();
            })
            .then(data => {
              var rows = [];
              data.data.forEach(function (rowData, i) {
                var role = rowData.defaultRole;
                  var row = {
                      id: (page * pageSize)  + i, 
                      password: "",
                      groups: rowData.groups,
                      name: rowData.name,
                      defaultRole: role?.type,    
                      keywords: rowData.keywords,
                      tool: rowData.tool,
                      chatRoom: rowData.chatRoom,
                      description: rowData.description,
                      expiration: rowData.expiration,
                      create_time: rowData.createTime,
                      creatorUid: rowData.creatorUid,
                      uid: [],
                      dataFeeds: [],
                      contents: []
                  }
                  if(rowData.passwordProtected === true ){
                    row.password = "fakePassword"
                  }
                  rowData.uids.forEach(function (uid){
                    row.uid.push(uid.data)
                  })
                  rowData.feeds.forEach(function (feed, i){
                    row.dataFeeds.push({id: feed.dataFeedUid, name:feed.name})
                  })
                  rowData.contents.forEach(function (file){
                    var struct = {
                      name: file.data.filename,
                      hash: file.data.hash
                    }
                    row.contents.push(struct)
                  })
                  rows.push(row);
              });
              setRows(rows);
              // If we are on edit (content was uploaded), update the edit panel
              if (rowsSelected.length === 1) {
                rows.forEach(function (row){
                  if (row.create_time === rowsSelected[0].create_time &&  row.name === rowsSelected[0].name){
                    setRowsSelected([row]);
                    if(value === 1){
                      setContents(row.contents)
                      setContentsOriginal(row.contents)
                    }
                  }
                })
              }
              if(value === 1 && rowsSelected.length === 0){
                rows.forEach(function (row){
                  if (row.name === name){
                    setContents(row.contents)
                    setContentsOriginal(row.contents)
                  }
                })
              }
              setLoading(false);
            });  
      },[page, paginationModel, missionChanged, sort, filter])

      useEffect(() => {
        // Get the groups
        fetch('/Marti/api/groups/all?useCache=false&sendLatestSA=false')
        .then(response => response.json())
        .then(data => {
            var groupArray = []
            data.data.forEach(function (rowData) {
              groupArray.push(rowData.name);
            })
            setAllGroups(groupArray);
        });

        fetch('/Marti/api/sync/search?keyword=ARCHIVED_MISSION')
        .then(response => response.json())
        .then(data => {
            var archivedMissionArray = []
            data.data.forEach(function (rowData) {
              var struct = {
                name: rowData.name,
                hash: rowData.hash
              }
              archivedMissionArray.push(struct);
            })
            setArchivedMissions(archivedMissionArray);
        });

        // Get groups for page
        fetch('/Marti/api/missioncount?passwordProtected=true&defaultRole=true')
        .then(response => response.json())
        .then(data => {
            setRowCount(data.data);
        });

        // Get the available data feeds
      fetch('/Marti/api/datafeeds')
      .then(response => response.json())
      .then(data => {
          var feedArray = []
          data.data.forEach(function (rowData) {
            feedArray.push({id: rowData.uuid, name: rowData.name, type:rowData.type});
          })
          setDataFeedsAvailable(feedArray);
      });
      }, [missionChanged])

      useEffect(() => {
        var url = '/Marti/api/cot/matchUid'
        if(uidToAdd !== "" && uidToAdd != null){
          url = url + "?search=" + uidToAdd;
        }
        fetch(url)
            .then(response => {
              if(!response.ok) {
                setUidOptions([]);
                throw new Error(response.status);
              }
              else return response.json();
            }).then(data => {
              var uidOptionsArray = []
              data.forEach(function (rowData) {
                uidOptionsArray.push(rowData);
              })
              setUidOptions(uidOptionsArray);
          });
      }, [uidToAdd])

      useEffect (() => {
        var url = '/vbm/api/config';
        fetch(url)
            .then(response => {
              if(!response.ok) {
                setVbmMode(false);
                setVbmSeverity("info")
                setVbmToastOpen(true)
                throw new Error(response.status);
              }
              else return response.json();
            }).then(data => {
              console.log(data)
              if(data.vbmEnabled === true) {
                setVbmMode(true);
                setVbmSeverity("success")
                setVbmToastOpen(true)
              } else{
                setVbmMode(false);
                setVbmSeverity("info")
                setVbmToastOpen(true)
              }
            })
      }, [])

      const theme = useTheme();
      const [drawerOpen, setDrawerOpen] = React.useState(false);

      const handleNextOrBack = () => {
        if(value === 0){
          setContents([])
          addOrEditMission(setValue, 1, setApiSent, setMissionChanged, missionChanged);
          setDisableEditAndFeeds(false);
          setDisableAdd(true);
        } else if(value === 1){
          setResetDataFeeds(!resetDataFeeds);
          addOrEditMission(setValue, 2, setApiSent, setMissionChanged, missionChanged);
        } else {
          handlePutDatafeeds();
          setValue(1)
        }
        
      }
      
      const handleDrawerClose = () => {
        setDrawerOpen(false);
      };

      const handleSaveAndClose = () => {
        if(value === 2){
          handlePutDatafeeds();
        } else if(value === 1){
          setResetDataFeeds(!resetDataFeeds);
          addOrEditMission(setValue, 2, setApiSent, setMissionChanged, missionChanged);
        }
        setDrawerOpen(false);
      } 

      const handleSendInvite = () => {
        setSendBool(false);
        setOpenSetInvite(true);
      }

      const handleArchivedMissionsClose = () => {
        setArchivedMissionOpen(false)
      }

      const handleArchivedMissionsOpen = () => {
        setArchivedMissionOpen(true)
      }


      const handleSendPackage = () => {
        setSendBool(true);
        setOpenSetInvite(true);
      }

      const handleAdd = () => {
        var emptyArrUid = [];
        var emptyArrKeyword = [];
        setRowsSelected([]);
        setDisableEditAndFeeds(true);
        setDisableAdd(false);
        setName("");
        setDescription("");
        setChatRoom("");
        setCopChecked(false);
        setDefaultRole([]);
        setPassword("");
        setTool("public")
        setUIDs(emptyArrUid);
        setUidOriginal(emptyArrUid);
        setKeywords(emptyArrKeyword);
        setKeywordOriginal(emptyArrKeyword);
        setContents([]);
        setGroups([]);
        setDrawerOpen(true);
        setValue(0);
      }

      const handleEdit = () => {
        setName(rowsSelected[0].name)
        setDescription(rowsSelected[0].description)
        setContents(rowsSelected[0].contents)
        setContentsOriginal(rowsSelected[0].contents)
        setUIDs(rowsSelected[0].uid)
        if(rowsSelected[0].uid.length !== 0){
          setUidSkrink(true)
        } else {
          setUidSkrink(false)
        }
        setUidOriginal(rowsSelected[0].uid)
        setKeywords(rowsSelected[0].keywords)
        if(rowsSelected[0].keywords.length !== 0){
          setKeywordShrink(true)
        } else {
          setKeywordShrink(false)
        }
        setKeywordOriginal(rowsSelected[0].keywords)
        setDefaultRole(rowsSelected[0].defaultRole)
        setChatRoom(rowsSelected[0].chatRoom)
        setGroups(rowsSelected[0].groups)
        setExpiration(rowsSelected[0].expiration)
        setPassword(rowsSelected[0].password)
        setDataFeeds(rowsSelected[0].dataFeeds)
        if(rowsSelected[0].tool === "vbm"){
          setCopChecked(true);
        } else{
          setCopChecked(false);
        }
        setTool(rowsSelected[0].tool)
        setResetDataFeeds(!resetDataFeeds);
        setDisableEditAndFeeds(false);
        
        setDisableAdd(true);
        setDrawerOpen(true);
        setValue(1);
      }

      function handlePutDatafeeds(){
        var feedStructArray = []
        dataFeedsSelected.forEach(function (arrayItem) {
          var struct = {
            dataFeedUid: arrayItem.id,
            name: arrayItem.name
          }
          feedStructArray.push(struct);
        })
        const data = "{ \"feeds\":" + JSON.stringify(feedStructArray) + "}";
        console.log(data);
        var url = '/Marti/api/missions/'+ name
        fetch(url, {
          method: "PUT",
          headers: {'Content-Type':'application/json'},
          body: data
        }).then(function (res) {
          if (!res.ok) {console.log("data feeds not set")}
          if (res.ok) {setMissionChanged(!missionChanged)
            setDrawerOpen(false);}
        })

      }

      function handleAddUIDs(){
        var uidStructArray = []
        if(uid.length > 0){
          uid.forEach(function (arrayItem) {
            uidStructArray.push(arrayItem);
          })
          const data = "{ \"uids\":" + JSON.stringify(uidStructArray) + "}";
          console.log(data);
          var url = '/Marti/api/missions/'+ name + '/contents'
          fetch(url, {
            method: "PUT",
            headers: {'Content-Type':'application/json'},
            body: data
          }).then(function (res) {
            if (!res.ok) {console.log("uids not set")}
            if (res.ok) {setMissionChanged(!missionChanged)}
          })
        }
      }

      function deleteUIDs(){
        var arrayToDel = uidOriginal.filter(function(val) {
          return uid.indexOf(val) == -1;
        });
        var urls = [];
        if(arrayToDel.length > 0){
          arrayToDel.forEach(function (rowData) {
              var url = '/Marti/api/missions/'+ name + '/contents?uid=' + rowData
              urls.push(url);
          })
          Promise.all(urls.map(url =>
              fetch(url, { method: 'DELETE' }).then()
          )).then(() => {
            setMissionChanged(!missionChanged)
          })
        }
    }

    function deleteContents(){
      var arrayToDel = contentsOriginal.filter(function(val) {
        return contents.indexOf(val) == -1;
      });
      var urls = [];
      if(arrayToDel.length > 0){
        arrayToDel.forEach(function (rowData) {
            var url = '/Marti/api/missions/'+ name + '/contents?hash=' + rowData.hash
            urls.push(url);
        })
        Promise.all(urls.map(url =>
            fetch(url, { method: 'DELETE' }).then()
        )).then(() => {
          setMissionChanged(!missionChanged)
        })
      }
    }

      const handleSetRemovePasswords = () => {
        if(password !== "fakePassword"){
          var url = '/Marti/api/missions/'+ name + '/password?password=' + password
          fetch(url, {
            method: "PUT"
          }).then(function (res) {
            if (res.ok) {setPassword("fakePassword")}
          })
        } else {
          var url = '/Marti/api/missions/'+ name + '/password'
          fetch(url, {
            method: "DELETE"
          }).then(function (res) {
            if (res.ok) {setPassword("")}
          })
        }
      }

      const handleSetKeywords = () => {
        var url = '/Marti/api/missions/'+ name + '/keywords'
        console.log("setting:" + url);
        var keywordsBody = JSON.stringify(keywords)
        fetch(url, {
          method: "PUT",
          headers: {
            'Accept': 'application/json',
            'Content-Type': 'application/json',
          },
          body: keywordsBody
        }).then(function (res) {
          if (!res.ok && rowsSelected.length > 0) {setKeywords(rowsSelected[0].keywords)}
        })
      }

      const handleDataFeeds = () => {
        handleEdit()
        setValue(2);
      }

      const handleVbmClose = (event, reason) => {
        if (reason === 'clickaway') {
          return;
        }
    
        setVbmToastOpen(false);
      };

      const handleDeleteMissions = () => {
        var urls = [];
        if(rowsSelected.length > 0){
          rowsSelected.forEach(function (rowData) {
              var url = '/Marti/api/missions/'+ rowData.name
              urls.push(url);
          })
          Promise.all(urls.map(url =>
              fetch(url, { method: 'DELETE' }).then()
          )).then(() => {
            setMissionChanged(!missionChanged)
            setMultiDeleteOpen(false);
          })
        }
        
      }

      function setDataFeeds(selected){
        var finalSelect = [];
        selected.forEach(function (arrayItem) {
          finalSelect.push(dataFeedsAvailable.find(item => item.id === arrayItem.id));
        })
        setDataFeedsSelected(finalSelect);
      }
    

    return (
      
        <Box sx={{ display: 'flex' }}>
          <CssBaseline />
          <Drawer
            sx={{
              width: drawerWidth,
              flexShrink: 0,
              '& .MuiDrawer-paper': {
                width: drawerWidth,
                boxSizing: 'border-box',
              },
            }}
            variant="persistent"
            anchor="left"
            open={drawerOpen}
          >
            <Box sx={{ borderBottom: 1, borderColor: 'divider' }}>
            <Tabs value={value} onChange={handleChange} variant="fullWidth" aria-label="basic tabs example">
              <Tab label="Add" disabled={disableAdd} {...a11yProps(0)} />
              <Tab label="Edit" disabled={disableEditAndFeeds} {...a11yProps(1)} />
              <Tab label="Data Feeds" disabled={disableEditAndFeeds} {...a11yProps(2)} />
            </Tabs>
          </Box>
          <Backdrop
              sx={{ color: '#fff', zIndex: (theme) => theme.zIndex.drawer + 1 }}
              open={apiSent}
            >
              <CircularProgress color="inherit" />
            </Backdrop>
          <TabPanel value={value} index={0} >
          <TextField
          sx={{width: .8}}
              margin="dense"
              id="name"
              value={name}
              onChange={(event) => {
                setName(event.target.value);
              }}
              label="Name (Required)"
              fullWidth
              variant="standard"
            />
            <TextField
            sx={{width: .8}}
              margin="dense"
              id="description"
              value={description}
              onChange={(event) => {
                setDescription(event.target.value);
              }}
              label="Description (Optional)"
              fullWidth
              variant="standard"
            />
            <TextField
            sx={{width: .8}}
              margin="dense"
              id="chatRoom"
              value={chatRoom}
              onChange={(event) => {
                setChatRoom(event.target.value);
              }}
              label="Chat Room (Optional)"
              fullWidth
              variant="standard"
            />
            <TextField
            sx={{width: .8}}
              margin="dense"
              id="password"
              value={password}
              onChange={(event) => {
                setPassword(event.target.value);
              }}
              label="Password (Optional)"
              type="password"
              fullWidth
              variant="standard"
            />
            <div>
            <Typography className='copSwitch' variant="body1">Mark as COP</Typography>
            <Switch className='copSwitch'
                checked={copChecked}
                onChange={(e) => {setCopChecked(e.target.checked)
                                  if(e.target.checked === true){
                                    setTool("vbm")
                                  } else {
                                    setTool("public")
                                  }}}
                inputProps={{ 'aria-label': 'controlled' }}
            />
            <TextField
            sx={{width: .8}}
              margin="dense"
              id="tool"
              value={tool}
              onChange={(event) => {
                setTool(event.target.value);
              }}
              label="Tool"
              fullWidth
              variant="standard"
            />
            </div>
            <TextField 
                sx={{width: .8, paddingBottom:3}}
                id="defaultRole"
                label="defaultRole"
                select
                value={defaultRole}
                onChange={
                    (e) => {
                        setDefaultRole(e.target.value)
                    }
                }
            ><MenuItem value={"MISSION_SUBSCRIBER"}>MISSION_SUBSCRIBER</MenuItem>
            <MenuItem value={"MISSION_OWNER"}>MISSION_OWNER</MenuItem>
            <MenuItem value={"MISSION_READONLY_SUBSCRIBER"}>MISSION_READONLY_SUBSCRIBER</MenuItem>
            </TextField>
            <FormControl>
            <InputLabel id="groups-label">Groups</InputLabel>
            <Select style={{width: 440}}
              label="Groups"
              labelId="groups-label"
              id="mission-multiple-chip"
              multiple
              value={groups}
              onChange={handleChip}
              input={<OutlinedInput id="select-multiple-chip" label="Chip" />}
              renderValue={(selected) => (
                <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
                  {selected.map((value) => (
                    <Chip key={value} label={value} />
                  ))}
                </Box>
              )}
              MenuProps={MenuProps}
            >{allGroups.map((group) => (
              <MenuItem
                key={group}
                value={group}
              >
                {group}
              </MenuItem>
            ))}
            </Select>
            </FormControl>
          </TabPanel>
          <TabPanel value={value} index={1}>
          <div {...getRootProps({className: 'dropzone'})} >
          <TextField
          sx={{width: .8}}
              margin="dense"
              id="name"
              value={name}
              onChange={(event) => {
                setName(event.target.value);
              }}
              label="Name (Required)"
              fullWidth
              variant="standard"
            />
            <TextField
            sx={{width: .8}}
              margin="dense"
              id="description"
              value={description}
              onChange={(event) => {
                setDescription(event.target.value);
              }}
              label="Description (Optional)"
              fullWidth
              variant="standard"
            />
            <TextField
            sx={{width: .8}}
              margin="dense"
              id="chatRoom"
              value={chatRoom}
              onChange={(event) => {
                setChatRoom(event.target.value);
              }}
              label="Chat Room (Optional)"
              fullWidth
              variant="standard"
            />
            <div>
            <Typography className='copSwitch' variant="body1">Mark as COP</Typography>
            <Switch className='copSwitch'
                checked={copChecked}
                onChange={(e) => {setCopChecked(e.target.checked)
                                  if(e.target.checked === true){
                                    setTool("vbm")
                                  } else {
                                    setTool("public")
                                  }}}
                inputProps={{ 'aria-label': 'controlled' }}
            />
            </div>
            <TextField
            sx={{width: .8}}
              margin="dense"
              id="tool"
              value={tool}
              onChange={(event) => {
                setTool(event.target.value);
              }}
              label="Tool"
              fullWidth
              variant="standard"
            />
            <TextField 
                sx={{width: .8, paddingBottom:3}}
                id="defaultRole"
                label="defaultRole"
                select
                value={defaultRole}
                onChange={
                    (e) => {
                        setDefaultRole(e.target.value)
                    }
                }
            ><MenuItem value={"MISSION_SUBSCRIBER"}>MISSION_SUBSCRIBER</MenuItem>
            <MenuItem value={"MISSION_OWNER"}>MISSION_OWNER</MenuItem>
            <MenuItem value={"MISSION_READONLY_SUBSCRIBER"}>MISSION_READONLY_SUBSCRIBER</MenuItem>
            </TextField>
            <FormControl sx={{paddingBottom:3}}>
            <InputLabel id="groups-label">Groups</InputLabel>
            <Select style={{width: 440}}
              label="Groups"
              labelId="groups-label"
              id="mission-multiple-chip"
              multiple
              labelWidth={ "text".length * 9}
              value={groups}
              onChange={handleChip}
              input={<OutlinedInput id="select-multiple-chip" label="Chip" />}
              renderValue={(selected) => (
                <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
                  {selected.map((value) => (
                    <Chip key={value} label={value} />
                  ))}
                </Box>
              )}
              MenuProps={MenuProps}
            >{allGroups.map((group) => (
              <MenuItem
                key={group}
                value={group}
              >
                {group}
              </MenuItem>
            ))}
            </Select>
            </FormControl>
            <TextField  sx={{width: .8, paddingBottom:3}}
                    label="Expiration"
                    id="datetime-local"
                    type="datetime-local"
                    value={expiration === -1 || expiration === "" ? "" : moment.unix(expiration).format('YYYY-MM-DDThh:mm')}
                    onClick={ (e) => {e.stopPropagation();}}
                    onChange={(e) => {
                        setExpiration(moment.utc(e.target.value).unix())
                    }}
                    InputLabelProps={{
                        shrink: true,
                    }}
                />
            <FormControl sx={{paddingBottom:3}}>
            <InputLabel id="contents-label">Contents</InputLabel>
            <Select style={{width: 335}}
              label="Contents"
              labelId="contents-label"
              id="content-multiple-chip"
              multiple
              labelWidth={ "text".length * 9}
              value={contents}
              onChange={handleContents}
              input={<OutlinedInput id="select-multiple-chip" label="Chip" />}
              renderValue={(selected) => (
                <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
                  {selected.map((value) => (
                    <Chip key={value.name} label={value.name} />
                  ))}
                </Box>
              )}
              MenuProps={MenuProps}
            >{contentsOriginal.map((el) => (
              <MenuItem
                key={el.name}
                value={el}
              >
                {el.name}
              </MenuItem>
            ))}
            </Select>
            </FormControl>
            <input {...getInputProps()} />
            <Button variant="text" sx={{width:.2, paddingTop:2}} onClick={open}>Add File</Button>
            <FormControl>
            <InputLabel id="uid-label" shrink={uidShrink}>UIDs</InputLabel>
            <Select style={{width: 335}}
              label="UIDs"
              labelId="uid-label"
              id="uid-multiple-chip"
              multiple
              labelWidth={ "text".length * 9}
              value={uid}
              onChange={handleUid}
              input={<OutlinedInput id="select-multiple-chip" label="Chip" />}
              renderValue={(selected) => (
                <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
                  {selected.map((value) => (
                    <Chip key={value} label={value} />
                  ))}
                </Box>
              )}
              MenuProps={MenuProps}
            >{uidOriginal.map((el) => (
              <MenuItem
                key={el}
                value={el}
              >
                {el}
              </MenuItem>
            ))}
            </Select>
            </FormControl>
            <FormControl>
            <InputLabel id="uidToAdd-label"></InputLabel>
            <Autocomplete
              style={{width: 335,  paddingTop:20}}
              labelId="uidToAdd-label"
              inputValue={uidToAdd}
              onInputChange={(event, newInputValue) => {
                setUidToAdd(newInputValue);
              }}
              id="uid-add"
              options={uidOptions}
              renderInput={(params) => <TextField style={{width: 335}} {...params} label="UID" />}
            />
            </FormControl>
            <Button variant="text" sx={{width:.2, paddingTop:4, paddingBottom:5}} onClick={() =>{
              uidOriginal.push(uidToAdd);
              setUidOriginal(uidOriginal);
              setUidSkrink(true);
              setUidToAdd("");
            }}>Add</Button>
            <FormControl>
            <InputLabel id="keywords-label" shrink={keywordShrink}>Keywords</InputLabel>
            <Select style={{width: 335}}
              label="Keywords"
              labelId="keyword-label"
              id="keyword-multiple-chip"
              multiple
              labelWidth={ "text".length * 9}
              value={keywords}
              onChange={handleKeywordsChip}
              input={<OutlinedInput id="select-multiple-chip" label="Chip" />}
              renderValue={(selected) => (
                <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
                  {selected.map((value) => (
                    <Chip key={value} label={value} />
                  ))}
                </Box>
              )}
              MenuProps={MenuProps}
            >{keywordOriginal.map((el) => (
              <MenuItem
                key={el}
                value={el}
              >
                {el}
              </MenuItem>
            ))}
            </Select>
            </FormControl>
            <TextField
            sx={{width: .6}}
              margin="dense"
              id="keywords-add"
              label="Keywords"
              value={keywordToAdd}
              onChange={(event) => {
                setKeywordToAdd(event.target.value)
              }}
              fullWidth
              variant="standard"
            />
            <Button variant="text" sx={{width:.2, paddingTop:2}} onClick={() =>{
              keywordOriginal.push(keywordToAdd);
              setKeywordOriginal(keywordOriginal);
              setKeywordShrink(true)
              setKeywordToAdd("");
            }}>Add</Button>
            <TextField
            sx={{width: .6}}
            margin="dense"
            id="password"
            value={password}
            disabled={password === "fakePassword"}
            onChange={(event) => {
              setPassword(event.target.value);
            }}
            label="Password (Optional)"
            type="password"
            fullWidth
            variant="standard"
            />
            <Button variant="text" sx={{width:.2, paddingTop:2}}
            disabled={password === ""} 
            onClick={handleSetRemovePasswords}>
              {password === "fakePassword" ? "Remove" : "Set"}
            </Button>
          </div>
          </TabPanel>
          <TabPanel value={value} index={2} >
          <TransferList available={dataFeedsAvailable} selected={dataFeedsSelected} setSelected={setDataFeedsSelected}
          resetList={resetDataFeeds}/>
          </TabPanel>
          <Box sx={{ borderBottom: 1, borderColor: 'divider', display: 'flex', justifyContent: 'space-evenly' }}>
          <Button variant="text" disabled={name === "" && value === 0} onClick={handleNextOrBack}>{value === 2 ? "Save & Go Back" : "Save & Next"}</Button>
          <Button variant="text" disabled={name === ""} onClick={handleSaveAndClose}>Save & Close</Button>
          <Button variant="text" onClick={handleDrawerClose}>Cancel & Close</Button>
          </Box>
          </Drawer>
          <Main open={drawerOpen} className="DataTable">
          <DataGrid 
            rows={rows}
            columns={columns}
            columnVisibilityModel={{
                id: false,
                hash: false
            }}
            rowsSelected={rowsSelected}
            rowHeight={100}
            onRowSelectionModelChange={(ids) => {
              const selectedRowData = ids.map((id) => 
                  rows.find((row) => row.id === id
              ));
              setRowsSelected(selectedRowData);
            }}
            pagination
            autoPageSize
            sortingMode="server"
            onSortModelChange={handleSortModelChange}
            filterMode="server"
            onFilterModelChange={handleFilterModelChange}
            checkboxSelection
            page={page}
            onPageChange={(newPage) => {
              setPage(newPage)
            }}
            rowCount={rowCount}
            paginationMode="server"
            paginationModel={paginationModel}
            onPaginationModelChange={(pgModel) => {
                setPaginationModel(pgModel)
                setPage(pgModel.page)
                }
            }
            components={{
              Toolbar: MissionManagerToolbar
            }}
            componentsProps={{ toolbar: {rowsSelected, setMultiDeleteOpen, handleAdd, 
              handleEdit, handleDataFeeds, handleSendInvite, handleSendPackage, handleArchivedMissionsOpen}}}
            />
            <Snackbar open={vbmToastOpen} autoHideDuration={6000} onClose={handleVbmClose}>
              <Alert onClose={handleVbmClose} severity={vbmSeverity} sx={{ width: '100%' }}>
                {vbmMode === true ? "VBM Mode enabled" :"VBM Mode not enabled" } 
              </Alert>
            </Snackbar>
          </Main>
          <UploadFile openHook={uploadOpen} setOpenHook={setUploadOpen}
                    deleted={missionChanged} setDeleted={setMissionChanged}
                    fileProp={fileProp} setFileProp={setFileProp} selectedMission={name}/>
          <SendInviteMission openHook={openSendInvite} setOpenHook={setOpenSetInvite} 
          sendBool={sendBool} missionSelected={rowsSelected}/>
          <Dialog open={multiDeleteOpen}>
            <DialogTitle id="alert-dialog-title">
            {"Delete " + rowsSelected.length + " Missions ?"}
            </DialogTitle>
            <DialogContent>
            <DialogContentText>
                The selected missions will be removed from the database
            </DialogContentText>
            </DialogContent>
            <DialogActions>
            <Button onClick={() => setMultiDeleteOpen(false)}>Cancel</Button>
            <Button onClick={handleDeleteMissions} autoFocus>
                Delete
            </Button>
            </DialogActions>
          </Dialog>
          <Dialog open={archivedMissionOpen} onClose={handleArchivedMissionsClose}
                  fullWidth={true}
                  scroll={"paper"}
                  maxWidth={"xl"}>
            <DialogTitle textAlign="center">Archived Missions</DialogTitle>
            <DialogContent dividers={true}>
            <List sx={{textAlign: 'center'}}>
            {archivedMissions.map((m) => (
              <ListItem sx={{textAlign: 'center'}} component="a" href={"/Marti/sync/content?hash=" + m.hash}>
              <ListItemText 
                primary={"" + m.name}
              />
            </ListItem>
            ))}
          </List>
          </DialogContent>
          <DialogActions>
            <Button onClick={handleArchivedMissionsClose}>Close</Button>
          </DialogActions>
          </Dialog>
        </Box>
      );
    
}

export default MissionManager;