import * as React from 'react';
import TextField from '@mui/material/TextField';
import Autocomplete from '@mui/material/Autocomplete';

/**
 * This component is used to build a step of the idawi link
 * @returns the StepElement component
 * @param props - the props of the component
 * @param props.title - the title of the step
 * @param props.choices - the choices of the step
 * @param props.idawilink - the idawilink to get suggestions from
 * @param props.index - the index of the step to update
*/
export default function StepElement(props) {
  const [value, setValue] = React.useState(null);
  const [inputValue, setInputValue] = React.useState('');

  /**
   * Handle the change of the step
   * @param newValue - the new value of the step 
   */
  const handleChange = (newValue) => {
    setValue(newValue);
    // get suggestions for the next step
    props.getSuggestions(props.idawilink + newValue + "/", props.index + 1);
  };

  /**
   * Handle the change of the input value
   * @param newInputValue - the new input value
   * @returns the new input value
   */
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