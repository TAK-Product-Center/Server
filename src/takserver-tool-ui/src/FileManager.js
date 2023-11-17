import './FileManager.css';
import React, { useEffect } from 'react';
import PropTypes from 'prop-types';
import { DataGrid, GridToolbarContainer, gridPageSizeSelector, gridFilteredTopLevelRowCountSelector,
    useGridRootProps, GridPagination, useGridSelector, useGridApiContext, GridCellModes} from '@mui/x-data-grid';
import DownloadIcon from '@mui/icons-material/Download';
import FileUploadIcon from '@mui/icons-material/FileUpload';
import DeleteIcon from '@mui/icons-material/Delete';
import FilterAltIcon from '@mui/icons-material/FilterAlt';
import { Button } from '@mui/material';
import TextField from '@mui/material/TextField';
import moment from 'moment';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogContentText from '@mui/material/DialogContentText';
import DialogTitle from '@mui/material/DialogTitle';
import Typography from '@mui/material/Typography';
import UploadFile from './UploadFile';
import {useDropzone} from 'react-dropzone';
import MenuItem from '@mui/material/MenuItem';
import Select from '@mui/material/Select';
import Switch from '@mui/material/Switch';
import Pagination from '@mui/material/Pagination';
import Footer from './Footer';


// Reset object needed for componets that access the selected row hook
const emptyRow = {name: "", hash: ""};

// Structures for missions
var allMissions = [];
var copMissions = [];

const getPageCount = (rowCount, pageSize) => {
    if (pageSize > 0 && rowCount > 0) {
      return Math.ceil(rowCount / pageSize);
    }
  
    return 0;
  };

// Component needed for Delete column rendering
function DeleteButton({setOpen, row, setRow, value})
{
return (
    <Button variant="outlined" color='error' startIcon={<DeleteIcon />}
                    onClick={ (e) => {
                        e.stopPropagation();
                        setRow(row);
                        setOpen(true);
                    }}
                    >
                    {value}
    </Button>
)
}

// Custom Pagination Controller for Grid
function ActionPagination({ page, onPageChange, className }) {
    const apiRef = useGridApiContext();
    const rootProps = useGridRootProps();
    const pageSize = useGridSelector(apiRef, gridPageSizeSelector);
    const [searchPage, setSearchPage] = React.useState({event: null, value: 1});
    const visibleTopLevelRowCount = useGridSelector(
        apiRef,
        gridFilteredTopLevelRowCountSelector
      );
    const pageCount = getPageCount(
        rootProps.rowCount ?? visibleTopLevelRowCount,
        pageSize
    );

    useEffect(() => {
        const delayDebounceFn = setTimeout(() => {
            onPageChange(searchPage.event, searchPage.value-1);
        }, 1000)
    
        return () => clearTimeout(delayDebounceFn)
    }, [searchPage])

    return (
        <Pagination
        color="primary"
        className={className}
        count={pageCount}
        page={page + 1}
        showFirstButton showLastButton
        onChange={(event, newPage) => {
            onPageChange(event, newPage - 1);
        }}
    />
    );
}

ActionPagination.propTypes = {
    className: PropTypes.string,
    /**
     * Callback fired when the page is changed.
     *
     * @param {React.MouseEvent<HTMLButtonElement> | null} event The event source of the callback.
     * @param {number} page The page selected.
     */
    onPageChange: PropTypes.func.isRequired,
    /**
     * The zero-based index of the current page.
     */
    page: PropTypes.number.isRequired,
  };

function CustomPagination(props) {
    return <GridPagination ActionsComponent={ActionPagination} {...props} />;
}

// Custom Toolbar for data grid
function FileManagerToolbar({rowsSelected, setMultiDeleteOpen, setUploadOpen, setFilter, 
                             filterPackage, setFilterPackage,
                            missions, setMissions, mission, setMission, copChecked, setCopChecked}) 
{
    return (
        <GridToolbarContainer>
            <Typography variant="h5" className="FileManagerLabel">File Manager</Typography>
            <Button className="Toolbar" color='error' startIcon={<DeleteIcon />}
            disabled={rowsSelected.length===0} onClick={
            (e) => {
                setMultiDeleteOpen(true);
            }  
            }>Delete</Button>
            <Button className="Toolbar" color='primary' startIcon={<FilterAltIcon />}
            style={{ marginRight: 16 }} selected={filterPackage} onClick={
                (e) => {
                    if(!filterPackage){
                        setFilter("missionPackage=true")
                        
                    } else {
                        setFilter("")
                        setMission("")
                    }
                    setFilterPackage(!filterPackage)
                }}>{filterPackage ? "Remove Filters" : "Mission Package"} </Button>
            <Typography variant="body1" className="MissionFilter">Mission: </Typography>
            <Select style={{width: 250, height: 25}}
                value={mission}
                label="Mission"
                onChange={
                    (e) => {
                        setMission(e.target.value)
                        setFilter("mission=" + e.target.value)
                        setFilterPackage(true)
                    }
                }
            >
            {missions.map((m) => (
                <MenuItem
                key={m}
                value={m}
                >
                    {m}
                </MenuItem>
            ))}
            </Select>
            <Switch
                checked={copChecked}
                onChange={
                    (e) => {
                        if(e.target.checked === false){
                            setMissions(allMissions)
                        } else {
                            setMissions(copMissions)
                        }
                        setCopChecked(e.target.checked)
                        if(mission !== ""){
                            setFilter("")
                            setMission("")
                            setFilterPackage(false)
                        }
                    }
                }
                inputProps={{ 'aria-label': 'controlled' }}
            />
            <Typography variant="body1" style={{ marginRight: 16 }}>Show Only COPs</Typography>
            <Button className="Toolbar" color='primary' startIcon={<FileUploadIcon />}
            style={{ marginRight: 16 }} onClick={
                (e) => {
                    setUploadOpen(true);
                }}>Upload</Button>
            
        </GridToolbarContainer>
    );
} 

function FileManager() {
    // Dialog open hooks
    const [open, setOpen] = React.useState(false);
    const [multiDeleteOpen, setMultiDeleteOpen] = React.useState(false);
    const [uploadOpen, setUploadOpen] = React.useState(false);
    // Selected Row hooks
    const [rowToDelete, setRow] = React.useState(emptyRow);
    const [rowsSelected, setRowsSelected] = React.useState([]);
    // Fetched data hook
    const [rows, setRows] = React.useState([]);
    // Hook used to in useEffect to trigger data hydration
    const [deleted, setSomethingDeleted] = React.useState(false);
    // Pagination system for grid
    const [page, setPage] = React.useState(0);
    //const [pageSize, setPageSize] = React.useState(12);
    const [rowCount, setRowCount] = React.useState(100);
    const [loading, setLoading] = React.useState(false);
    const [paginationModel, setPaginationModel] = React.useState({
        pageSize: 12,
        page: 0,
      });

    // Hooks for filters
    const [filter, setFilter] = React.useState("");
    const [filterPackage, setFilterPackage] = React.useState(false);
    const [missions, setMissions] = React.useState([]);
    const [mission, setMission] = React.useState([]);
    const [copChecked, setCopChecked] = React.useState(false);
    const [sort, setSort] = React.useState("");

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


    // Dropzone state management
    const [fileProp, setFileProp] = React.useState("");
    const {getRootProps} = useDropzone({
        // Note how this callback is never invoked if drop occurs on the inner dropzone
        noClick: true,
        onDrop: files => { 
            setFileProp(files[0]);
            setUploadOpen(true);
        }
    });

    // Only get missions once on page load since they cannot be changed on this page
    useEffect(() => {
        fetch('/Marti/api/missions')
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
      },[])

    useEffect(() => {
        setLoading(true);
        const pageSize = paginationModel.pageSize
        const url = '/Marti/api/files/metadata?' + filter + '&page=' 
        + page + '&limit=' + pageSize + sort
        // Grab the page first since we start on page 0
        fetch(url)
            .then(response => response.json())
            .then(data => {
                var rows = [];
                data.data.forEach(function (rowData, i) {
                    var row = {
                        id: (page * pageSize)  + i, 
                        hash: rowData.Hash,
                        name: rowData.Name,
                        submitter: rowData.User,    
                        keywords: rowData.Keywords,
                        groups: rowData.Groups,
                        size: rowData.Size,
                        updateTime: rowData.Time,
                        type: rowData.MimeType,
                        expiration: rowData.Expiration,
                        actions: 'Delete'
                    }
                    rows.push(row);
                });
                setRows(rows);
                setLoading(false);
            });
        // Get the row count for page indexing
        fetch('/Marti/api/files/metadata/count?' + filter)
            .then(response => response.json())
            .then(data => {
                setRowCount(data.data);
            });
        
    }, [page, deleted, paginationModel, sort, filter]);

    // Render definition for Data Grid
    const columns = [
        { field: 'id', headerName: 'ID', width: 1 },
        { field: 'hash', headerName: 'hash', width: 1 },
        { field: 'name', headerName: 'Name', width: 330,
        renderCell: (params) => {
            return (
                <div>
                    <Button href={'api/files/'+ params.row.hash} 
                    onClick={ (e) => {e.stopPropagation();}}
                    startIcon={<DownloadIcon />} aria-label="Download" style={{textTransform: 'none'}}>
                    {params.value}
                    </Button>
                    
                </div>
            );
          } },
        { field: 'submitter', headerName: 'Submitter', width: 150, sortable: false },
        { field: 'keywords', headerName: 'Keywords', width: 250, sortable: false },
        { field: 'groups', headerName: 'Groups', width: 150, sortable: false },
        { field: 'size', headerName: 'Size (Approx)', width: 120 },
        { field: 'updateTime', headerName: 'Update Time', width: 180,
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
        { field: 'type', headerName: 'Type', width: 200, sortable: false },
        { field: 'expiration', headerName: 'Expiration', width: 300, sortable: false,
        renderCell: (params) => {
            return (
                <TextField
                    id="datetime-local"
                    type="datetime-local"
                    defaultValue={moment(params.value).format('YYYY-MM-DDThh:mm')}
                    onClick={ (e) => {e.stopPropagation();}}
                    onChange={(e) => {
                        setExpiration(params.row.hash, moment.utc(e.target.value).unix())
                    }}
                    InputLabelProps={{
                        shrink: true,
                    }}
                />  
            );
          }
        },
        { field: 'actions', headerName: 'Actions', width: 150, sortable: false, 
        renderCell: (params) => {
            return (
                <DeleteButton setOpen={setOpen} row={params.row} 
                setRow={setRow} value={params.value} />
        );
        }}
        
    ];

    //Used for setting expiration
    function setExpiration(hash,newDate){
        var date;
        if(isNaN(newDate)){
            date = -1
        } else{
            date = newDate
        }
        var url = '/Marti/api/sync/metadata/' + hash + "/expiration?expiration=" + date
        fetch(url, { method: 'PUT' })
            .then(() => {
                setSomethingDeleted(!deleted);
            });
    }

    // Used in Dialog
    const handleClose = () => {
        setOpen(false);
        setMultiDeleteOpen(false);
        setRow(emptyRow);
    };

    // Delete functions for row button and toolbar button
    function deleteFile(){
        var url = '/Marti/api/files/' + rowToDelete.hash
        fetch(url, { method: 'DELETE' })
            .then(() => {
                setSomethingDeleted(!deleted);
                handleClose();
            });
    }

    function deleteSelectedFiles(){
        var urls = [];
        rowsSelected.forEach(function (rowData) {
            var url = '/Marti/api/files/' + rowData.hash
            urls.push(url);
        })
        Promise.all(urls.map(url =>
            fetch(url, { method: 'DELETE' }).then()
        )).then(() => {
            setSomethingDeleted(!deleted);
            handleClose();
        })
    }
      

    return (
        <div {...getRootProps({className: 'dropzone'})} >
        <header className="DataTable">
        <DataGrid
            rows={rows}
            columns={columns}
            columnVisibilityModel={{
                id: false,
                hash: false
            }}
            pagination
            //autoPageSize
            checkboxSelection
            /*onPageSizeChange={(newPageSize) => {
                console.log("New page size: "+newPageSize)
                setPageSize(newPageSize)
            }}*/
            pageSizeOptions={[12, 25, 50, 100]}
            page={page}
            onPageChange={(newPage) => setPage(newPage)}
            rowCount={rowCount}
            paginationMode="server"
            paginationModel={paginationModel}
            onPaginationModelChange={(pgModel) => {
                setPaginationModel(pgModel)
                setPage(pgModel.page)
                }
            }
            onSelectionModelChange={(ids) => {
                const selectedRowData = ids.map((id) => 
                    rows.find((row) => row.id === id
                ));
                setRowsSelected(selectedRowData);
            }}
            components={{
                Toolbar: FileManagerToolbar,
                Pagination: CustomPagination,
            }}
            componentsProps={{ toolbar: {rowsSelected, setMultiDeleteOpen, setUploadOpen, setFilter, 
                filterPackage, setFilterPackage,missions, setMissions, mission, 
                setMission, copChecked, setCopChecked}}}
            rowsSelected={rowsSelected}
            setMultiDeleteOpen={setMultiDeleteOpen}
            setFilter={setFilter}
            setSomethingDeleted={setSomethingDeleted}
            deleted={deleted}
            loading={loading}
            sortingMode="server"
            onSortModelChange={handleSortModelChange}
            />
        </header>
        <UploadFile openHook={uploadOpen} setOpenHook={setUploadOpen}
                    deleted={deleted} setDeleted={setSomethingDeleted}
                    fileProp={fileProp} setFileProp={setFileProp}/>
        <Dialog open={open} onClose={handleClose} >
            <DialogTitle id="alert-dialog-title">
            {"Delete File " + rowToDelete.name + "?"}
            </DialogTitle>
            <DialogContent>
            <DialogContentText>
                This file will be removed from the database
            </DialogContentText>
            </DialogContent>
            <DialogActions>
            <Button onClick={handleClose}>Cancel</Button>
            <Button onClick={deleteFile} autoFocus>
                Delete
            </Button>
            </DialogActions>
        </Dialog>
        <Dialog open={multiDeleteOpen} onClose={handleClose}>
            <DialogTitle id="alert-dialog-title">
            {"Delete " + rowsSelected.length + " Files ?"}
            </DialogTitle>
            <DialogContent>
            <DialogContentText>
                The selected files will be removed from the database
            </DialogContentText>
            </DialogContent>
            <DialogActions>
            <Button onClick={handleClose}>Cancel</Button>
            <Button onClick={deleteSelectedFiles} autoFocus>
                Delete
            </Button>
            </DialogActions>
        </Dialog>
        <Footer />
        </div>
    );
}

export default FileManager;