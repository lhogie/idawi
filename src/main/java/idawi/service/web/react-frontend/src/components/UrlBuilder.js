import * as React from 'react';
import Box from '@mui/material/Box';
import StepElement from './StepElement';

export default function UrlBuilder() {
  const [idawilink, setIdawiLink] = React.useState("http://localhost:8081/api/");

  const [steps, setSteps] = React.useState([{title: "step1", choices: []}, {title: "step2", choices: []}]);

  const updateIdawiLink = (newValue) => {
    setIdawiLink(newValue);
  };

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
            var elements = Array.from(payload.content.elements);
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
              else{
                var newSteps = [...steps]
                newSteps[index].choices = Array.from(payload.content.elements);
                setSteps(newSteps);
              }
            }
          }
        }
    }
  };

  React.useEffect(() => {
    getSuggestions(idawilink, 0);
  }, [idawilink]);

  return (
    <Box sx={{ width: '100%' }}>
        <React.Fragment>
          <div>{idawilink}</div>
          <div>{steps.map((step, index) => <StepElement index={index} getSuggestions={getSuggestions} idawilink={idawilink} choices={step.choices} title={step.title} />)}</div>
        </React.Fragment>
    </Box>
  );
}