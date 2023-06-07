import * as React from 'react';
import TextField from '@mui/material/TextField';
import PropTypes from 'prop-types';
import LinearProgress from '@mui/material/LinearProgress';
import Typography from '@mui/material/Typography';
import Box from '@mui/material/Box';

/**
 * This component is used to build a step of the idawi link
 * @returns the StepElement component
 * @param props - the props of the component
 * @param props.title - the title of the step
 * @param props.choices - the choices of the step
 * @param props.idawilink - the idawilink to get suggestions from
 * @param props.index - the index of the step to update
*/
export default function ComponentProgrss(props) {

  const [progress, setProgress] = React.useState(10);


  function LinearProgressWithLabel(props) {
    return (
      <Box sx={{ display: 'flex', alignItems: 'center' }}>
        <Box sx={{ width: '100%', mr: 1 }}>
          <LinearProgress variant="determinate" {...props} />
        </Box>
        <Box sx={{ minWidth: 35 }}>
          <Typography variant="body2" color="text.secondary">{`${Math.round(
            props.value,
          )}%`}</Typography>
        </Box>
      </Box>
    );
  }

  LinearProgressWithLabel.propTypes = {
    /**
     * The value of the progress indicator for the determinate and buffer variants.
     * Value between 0 and 100.
     */
    value: PropTypes.number.isRequired,
  };

  React.useEffect(() => {
    const timer = setInterval(() => {
      setProgress((prevProgress) => (prevProgress >= 100 ? 10 : prevProgress + 10));
    }, 800);
    return () => {
      clearInterval(timer);
    };
  }, []);

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
        <Box sx={{ width: '100%' }}>
            <LinearProgressWithLabel value={progress} />
        </Box>
    </div>
  );
}