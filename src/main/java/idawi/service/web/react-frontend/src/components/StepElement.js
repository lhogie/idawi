import * as React from 'react';
import TextField from '@mui/material/TextField';
import Autocomplete from '@mui/material/Autocomplete';
import { UrlContext } from '../components/UrlBuilder';

export default function StepElement(props) {

  const [choices, setChoices] = React.useState([]);
  const [value, setValue] = React.useState(choices[0]);

  var idawilink = React.useContext(UrlContext).idawilink;
  
  React.useEffect(() => {
    console.log("idawilink: " + idawilink)
    var idawiListener = new EventSource(idawilink, { withCredentials: true });

    idawiListener.onmessage = function (event) {
        var s = event.data;
        var lines = s.split(/\r?\n/);
        var headerraw = lines.shift();
        var data = lines.join('');
        var payload = JSON.parse(data);
        if(payload['#class'] != 'idawi.messaging.EOT' && payload.content.elements){
            setChoices(Array.from(payload.content.elements));
            console.log(choices)
        
        }
    }
  });
    
  return (
    <div>
        {props.title}
        <div>
            <div>{`value: ${value !== null ? `'${value}'` : 'null'}`}</div>
            <br />
            <Autocomplete
                value={value}
                onChange={(event, newValue) => {
                setValue(newValue);
                }}
                options={choices}
                sx={{ width: 300 }}
                renderInput={(params) => <TextField {...params} label="Controllable" />}
            />
        </div>
    </div>
  );
}