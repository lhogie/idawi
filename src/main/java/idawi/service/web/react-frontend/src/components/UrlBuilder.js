import * as React from 'react';
import Box from '@mui/material/Box';
import Stack from '@mui/material/Stack';
import StepElement from './StepElement';
import JSONViewer from './JSONViewer';
import AppBar from '@mui/material/AppBar';
import Toolbar from '@mui/material/Toolbar';
import Typography from '@mui/material/Typography';
import Container from '@mui/material/Container';
import ExceptionComponent from './ExceptionComponent'
import ImageComponent from './ImageComponent';

/**
 * This component is used to build an idawi link
 * @returns the UrlBuilder component
 */
export default function UrlBuilder() {

  const [idawilink, setIdawiLink] = React.useState("http://localhost:8081/api/");
  const [steps, setSteps] = React.useState([{title: "Routing", choices: []}, {title: "Parameters", choices: []}, {title: "Components", choices: []}, {title: "Services", choices: []}, {title: "Operations", choices: []}]);
  const [content, setContent] = React.useState([]);
  const [exceptionMessage, setExceptionMessage] = React.useState("")
  const [exceptionStackTrace, setExceptionStackTrace] = React.useState({})


  const updateIdawiLink = (newValue) => {
    setIdawiLink(newValue);
  };

  /**
   * Get suggestions for the next step
   * @param idawilink - the idawilink to get suggestions from
   * @param index - the index of the step to update
   */
  const getSuggestions = (idawilink, index) => {
    setIdawiLink(idawilink);
    console.log("idawilink :", idawilink);
    var idawiListener = new EventSource(idawilink, { withCredentials: true });

    idawiListener.onmessage = function (event) {
        var s = event.data;
        var lines = s.split(/\r?\n/);
        var headerraw = lines.shift();
        var data = lines.join('');
        if(data != "EOT"){
          var payload = JSON.parse(data);
          if(payload['#class'] == "idawi.messaging.Message" && payload.content['#class'] != "idawi.messaging.EOT"){
            var elements = Array.from(payload.content.elements).concat(Array.from(payload.route.elements));
            elements = [...new Set(elements)];      
            if(elements.length > 0){
              if(elements[0]['#class'] == "idawi.Component"){
                var newSteps = [...steps]
                newSteps[index].choices = elements.map((element) => element.ref);
                setSteps(newSteps);
              }
              else if(elements[0]['#class'] == "idawi.routing.EmptyRoutingParms"){
                var newSteps = [...steps]
                newSteps[index].choices = elements.map((element) => element['#class']);
                setSteps(newSteps);
              }
              else if(elements[0]['#class']?.includes("Exception")){
                setExceptionMessage(payload.content.message)
                setExceptionStackTrace(payload.content.stack_trace)
              }
              else if(index < steps.length){
                var newSteps = [...steps]
                newSteps[index].choices =  elements;
                setSteps(newSteps);
              }
              if(index === 5){
                setContent(payload.content.elements);
              }
            }
          }
        }
    }
  };

  /**
   * Get suggestions for the first step
   */
  React.useEffect(() => {
    getSuggestions(idawilink, 0);
  }, [idawilink]);
  
  return (
    <React.Fragment>
      <Box sx={{ width: '100%' }}>
        <AppBar position="static">
          <Container maxWidth="xl">
            <Toolbar disableGutters>
              <Typography
                variant="h6"
                href="/"
                sx={{
                  fontFamily: 'monospace',
                  fontWeight: 700,
                  letterSpacing: '.3rem',
                  color: 'inherit',
                  textDecoration: 'none',
                }}
              >
                IDAWI
              </Typography>
              </Toolbar>
          </Container>
      </AppBar>
            <Stack direction="row" spacing={20} marginLeft={10} marginTop={10}>
            <div>{steps.map((step, index) => <StepElement index={index} getSuggestions={getSuggestions} idawilink={idawilink} choices={step.choices} title={step.title} />)}</div>
            <div>{idawilink}</div>
            {content.length > 0 && <JSONViewer data={content} />}
            {exceptionMessage.length > 0 && <ExceptionComponent data={exceptionStackTrace} message={exceptionMessage} />}
          </Stack>
      </Box>
    </React.Fragment>
  );
}