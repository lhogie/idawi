import * as React from 'react';
import TextField from '@mui/material/TextField';
import Autocomplete from '@mui/material/Autocomplete';

export default function StepElement(props) {
  const [value, setValue] = React.useState(null);
  const [inputValue, setInputValue] = React.useState('');

  const handleChange = (newValue) => {
    setValue(newValue);
    // get suggestions for the next step
    props.getSuggestions(props.idawilink + newValue + "/", props.index + 1);
  };

  const handleInputChange = (newInputValue) => {
    setInputValue(newInputValue);
  };
    
  return (
    <div>
        {props.title}
        <div>
            <div>{`value: ${value !== null ? `'${value}'` : 'null'}`}</div>
            <div>{`inputValue: '${inputValue}'`}</div>
            <br />
            <Autocomplete
                value={value}
                onChange={(event, newValue) => {
                    handleChange(newValue);
                }}
                options={props.choices}
                inputValue={inputValue}
                onInputChange={(event, newInputValue) => {
                  setInputValue(newInputValue);
                }}
                sx={{ width: 300 }}
                renderInput={(params) => <TextField {...params} label="Controllable" />}
            />
        </div>
    </div>
  );
}