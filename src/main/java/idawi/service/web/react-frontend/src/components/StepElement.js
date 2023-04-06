import * as React from 'react';
import TextField from '@mui/material/TextField';
import Autocomplete from '@mui/material/Autocomplete';
import { UrlContext } from '../IdawiLinkContext';

export default function StepElement(props) {

  const [choices, setChoices] = React.useState([]);
  const [value, setValue] = React.useState(choices[0]);

  //var idawilink = React.useContext(UrlContext).idawilink;

  const {idawilink, updateIdawiLink} = React.useContext(UrlContext);
  
  React.useEffect(() => {
    console.log("idawilink :", idawilink);
    var idawiListener = new EventSource(idawilink, { withCredentials: true });

    idawiListener.onmessage = function (event) {
        var s = event.data;
        var lines = s.split(/\r?\n/);
        var headerraw = lines.shift();
        var data = lines.join('');
        var payload = JSON.parse(data);
        console.log("payload :", payload);
        if(payload['#class'] == "idawi.messaging.Message" && payload.content['#class'] != "idawi.messaging.EOT"){
          var elements = Array.from(payload.content.elements);
          if(elements.length > 0){
            if(elements[0]['#class'] == "idawi.Component"){
              setChoices(elements.map((element) => element.ref));
            }
            else if(elements[0]['#class'] == "idawi.routing.EmptyRoutingParms"){
              setChoices(elements.map((element) => element['#class']));
            }
            else{
              setChoices(Array.from(payload.content.elements));
            }
          }
        }
    }
  });

  const handleChange = (newValue) => {
    setValue(newValue);
    // change the value in context
    updateIdawiLink(idawilink + newValue + "/");
  };
    
  return (
    <div>
        {props.title}
        <div>
            <div>{`value: ${value !== null ? `'${value}'` : 'null'}`}</div>
            <br />
            <Autocomplete
                value={value}
                onChange={(event, newValue) => {
                    handleChange(newValue);
                }}
                options={choices}
                sx={{ width: 300 }}
                renderInput={(params) => <TextField {...params} label="Controllable" />}
            />
        </div>
    </div>
  );
}