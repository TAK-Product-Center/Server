import React, { useEffect } from 'react';
import Grid from '@mui/material/Grid';
import List from '@mui/material/List';
import Card from '@mui/material/Card';
import CardHeader from '@mui/material/CardHeader';
import ListItem from '@mui/material/ListItem';
import ListItemText from '@mui/material/ListItemText';
import ListItemIcon from '@mui/material/ListItemIcon';
import Checkbox from '@mui/material/Checkbox';
import Button from '@mui/material/Button';
import Divider from '@mui/material/Divider';
import Typography from '@mui/material/Typography';
import AirIcon from '@mui/icons-material/Air';
import PowerIcon from '@mui/icons-material/Power';
import GroupIcon from '@mui/icons-material/Group';
import DataObjectIcon from '@mui/icons-material/DataObject';
import { createTheme } from '@mui/material/styles';
import grey from '@mui/material/colors/grey';

function not(a, b) {
  return a.filter((value) => b.indexOf(value) === -1);
}

function intersection(a, b) {
  return a.filter((value) => b.indexOf(value) !== -1);
}

function union(a, b) {
  return [...a, ...not(b, a)];
}

//const available = [{id: 0, type:'stream'},{id: 1, type:'predicate'},{id: 2, type:'plugin'},{id: 3, type:'federate'},
//{id: 4, type:'federate'}]

export default function TransferList({available, selected, setSelected, resetList}) {


  const theme = createTheme({
  palette: {
      primary: {
          main: grey[900],
      }
  },
  });
  const [checked, setChecked] = React.useState([]);
  const [left, setLeft] = React.useState(not(available, selected));
  const [streamVariant, setStreamVariant] = React.useState("outlined");
  const [predicateVariant, setPredicateVariant] = React.useState("outlined");
  const [pluginVariant, setPluginVariant] = React.useState("outlined");
  const [federateVariant, setFederateVariant] = React.useState("outlined");
  const [filter, setFilter]  = React.useState("");
  const leftChecked = intersection(checked, left);
  const rightChecked = intersection(checked, selected);
  const [resetLeft, setResetLeft] = React.useState(not(available, selected));

  useEffect(() => {
    console.log(selected)
    handleDataFeedChange()
  },[resetList])

  function handleFilter(type){
    setStreamVariant("outlined")
    setPredicateVariant("outlined")
    setPluginVariant("outlined")
    setFederateVariant("outlined")
    if(type === 'Streaming' && filter !== "Streaming"){
      setStreamVariant("contained")
      setLeft(resetLeft.filter(function(el){return el.type === 'Streaming'}))
      setFilter("Streaming")
    }
    else if(type === 'Streaming'){
      setLeft(resetLeft);
      setFilter("");
    }
    if(type === 'Predicate' && filter !== "Predicate"){
      setPredicateVariant("contained")
      setLeft(resetLeft.filter(function(el){return el.type === 'Predicate'}))
      setFilter("Predicate")
    }
    else if(type === 'Predicate'){
      setLeft(resetLeft);
      setFilter("");
    }
    if(type === 'Plugin' && filter !== "Plugin"){
      setPluginVariant("contained")
      setLeft(resetLeft.filter(function(el){return el.type === 'Plugin'}))
      setFilter("Plugin")
    }
    else if(type === 'Plugin'){
      setLeft(resetLeft);
      setFilter("");
    }
    if(type === 'Federate' && filter !== "Federate"){
      setFederateVariant("contained")
      setLeft(resetLeft.filter(function(el){return el.type === 'Federate'}))
      setFilter("Federate")
    }
    else if(type === 'Federate'){
      setLeft(resetLeft);
      setFilter("");
    }
  }

  const handleToggle = (value) => () => {
    const currentIndex = checked.indexOf(value);
    const newChecked = [...checked];

    if (currentIndex === -1) {
      newChecked.push(value);
    } else {
      newChecked.splice(currentIndex, 1);
    }

    setChecked(newChecked);
  };

  const numberOfChecked = (items) => intersection(checked, items).length;

  const handleToggleAll = (items) => () => {
    if (numberOfChecked(items) === items.length) {
      setChecked(not(checked, items));
    } else {
      setChecked(union(checked, items));
    }
  };

  const handleCheckedRight = () => {
    setSelected(selected.concat(leftChecked));
    setLeft(not(left, leftChecked));
    setResetLeft(not(resetLeft, leftChecked));
    setChecked(not(checked, leftChecked));
  };

  const handleCheckedLeft = () => {
    setLeft(left.concat(rightChecked));
    setResetLeft(resetLeft.concat(rightChecked));
    if(filter !== ""){
      // extra concat is needed set the hook set as aync and we can't count on it being updated in time
      setLeft(resetLeft.concat(rightChecked).filter(function(el){return el.type === filter}))
    } 
    setSelected(not(selected, rightChecked));
    setChecked(not(checked, rightChecked));
  };

  function handleDataFeedChange(){
    setLeft(not(available, selected));
    setResetLeft(not(available, selected));
    setChecked([]);
  };

  const customList = (title, items) => (
    <Card>
      <CardHeader
        sx={{ px: 2, py: 1 }}
        avatar={
          <Checkbox
            onClick={handleToggleAll(items)}
            checked={numberOfChecked(items) === items.length && items.length !== 0}
            indeterminate={
              numberOfChecked(items) !== items.length && numberOfChecked(items) !== 0
            }
            disabled={items.length === 0}
            inputProps={{
              'aria-label': 'all items selected',
            }}
          />
        }
        title={title}
        subheader={`${numberOfChecked(items)}/${items.length} selected`}
      />
      <Divider />
      <List
        sx={{
          width: 200,
          height: 530,
          bgcolor: 'background.paper',
          overflow: 'auto',
        }}
        dense
        component="div"
        role="list"
      >
        {items.map((value) => {
          if(value === undefined) {return (<></>)}
          const labelId = `transfer-list-all-item-${value.name}-label`;
          return (
            <ListItem
              key={value.id}
              role="listitem"
              button
              onClick={handleToggle(value)}
            >
              <ListItemIcon>
                <Checkbox
                  checked={checked.indexOf(value) !== -1}
                  tabIndex={-1}
                  disableRipple
                  inputProps={{
                    'aria-labelledby': labelId,
                  }}
                />
                {value.type === 'Streaming' ? <AirIcon sx={{mt:1, mr:0.5}}/> : <></>}
                {value.type === 'Predicate' ? <DataObjectIcon sx={{mt:1, mr:0.5}}/> : <></>}
                {value.type === 'Plugin' ? <PowerIcon sx={{mt:1, mr:0.5}}/> : <></>}
                {value.type === 'Federate' ? <GroupIcon sx={{mt:1, mr:0.5}}/> : <></>}
              </ListItemIcon>
              <ListItemText id={labelId} primary={`${value.name}`} />
            </ListItem>
          );
        })}
      </List>
    </Card>
  );

  return (
    <>
     <Typography sx={{pl:5}} variant="h5">Filter Available Feeds:</Typography>
     <Button sx={{ml:5}} className="Toolbar" 
     color='primary' theme={theme}  onClick={() => {handleFilter("Streaming")}} variant={streamVariant}
     startIcon={<AirIcon />}>Stream</Button>
     <Button sx={{ml:1, mr: 13}} className="Toolbar" 
     color='primary' theme={theme} onClick={() => {handleFilter("Predicate")}} variant={predicateVariant}
      startIcon={<DataObjectIcon />}>Predicate</Button>
     <Button sx={{ml:5, mt: 2}} className="Toolbar" 
     color='primary' theme={theme} onClick={() => {handleFilter("Plugin")}} variant={pluginVariant}
      startIcon={<PowerIcon />}>Plugin</Button>
     <Button sx={{ml:2, mt: 2}} className="Toolbar" 
     color='primary' theme={theme} onClick={() => {handleFilter("Federate")}} variant={federateVariant}
      startIcon={<GroupIcon />}>Federate</Button>
    <Grid sx={{marginTop:1}} container spacing={2} justifyContent="center" alignItems="center">
      <Grid item>{customList('Available', left)}</Grid>
      <Grid item>
        <Grid container direction="column" alignItems="center">
          <Button
            sx={{ my: 0.5 }}
            variant="outlined"
            size="small"
            onClick={handleCheckedRight}
            disabled={leftChecked.length === 0}
            aria-label="move selected right"
          >
            &gt;
          </Button>
          <Button
            sx={{ my: 0.5 }}
            variant="outlined"
            size="small"
            onClick={handleCheckedLeft}
            disabled={rightChecked.length === 0}
            aria-label="move selected left"
          >
            &lt;
          </Button>
        </Grid>
      </Grid>
      <Grid item>{customList('Current', selected)}</Grid>
    </Grid>
    </>
  );
}