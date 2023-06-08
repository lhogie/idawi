import * as React from 'react';
import { Chart } from '../models/Chart';
import ChartComponent from './ChartComponent';
import GraphComponent from './GraphComponent';
import ImageComponent from './ImageComponent';
import VideoComponent from './VideoComponent';
import PrimitiveComponent from './PrimitiveComponent';
import { Stack } from '@mui/material';

/**
 * This component is used to build an idawi link
 * @returns the UrlBuilder component
 */
export default function ComponentContent() {

  const [idawilink, setIdawiLink] = React.useState("http://localhost:8081/api/idawi.routing.ForceBroadcasting//gw/idawi.service.DemoService/SendImage");
  //const graphLink = "http://localhost:8081/api/idawi.routing.ForceBroadcasting//gw/idawi.service.DemoService/SendGraph"
 // const imageLink = "http://localhost:8081/api/idawi.routing.ForceBroadcasting//gw/idawi.service.DemoService/SendImage"
  const [steps, setSteps] = React.useState([{title: "Routing", choices: []}, {title: "Parameters", choices: []}, {title: "Components", choices: []}, {title: "Services", choices: []}, {title: "Operations", choices: []}]);
  const [componentName, setComponentName] = React.useState("");
  const [chart, setChart] = React.useState([])
  const [image, setImage] = React.useState("")
  const [video, setVideo] = React.useState("")
  const [primitiveValue, setPrimitiveValue] = React.useState("")


  const updateIdawiLink = (newValue) => {
    //setIdawiLink(newValue);
    setIdawiLink(newValue)
  };


  function isNotArrayOrJSON(variable) {
    return !Array.isArray(variable) && typeof variable !== 'object';
  }


  /**
   * Get suggestions for the next step
   * @param idawilink - the idawilink to get suggestions from
   */
  const getSuggestions = (idawilink) => {
    setIdawiLink(idawilink);
    console.log("idawilink :", idawilink);
    var idawiListener = new EventSource(idawilink, { withCredentials: true });

    idawiListener.onmessage = function (event) {
        var s = event.data;
        var lines = s.split(/\r?\n/);
        var headerraw = lines.shift();
        var data = lines.join('');
        if(data !== "EOT"){
          var payload = JSON.parse(data);
          if(payload['#class'] === "idawi.messaging.Message" && payload.content['#class'] !== "idawi.messaging.EOT"){
            if(payload.content['#class'] === "idawi.service.web.chart.Function"){
              setComponentName(payload.route.elements[0])
              var elements = Array.from(payload.content.functions.elements)
              elements = [...new Set(elements)];      
              if(elements.length > 0){
                var chart = new Chart([], [])
                elements.forEach(element => {
                  if(element['#class'] === "idawi.service.web.chart.Function"){
                      var points = element.points.elements
                      points.forEach(point => {
                          chart.listY.push(point.y);
                          chart.listX.push(point.x);
                      });
                  }
                });
              }
              setChart(chart)
            } else if(payload.content['#class'] === "idawi.service.web.Image"){
              var base64 = payload.content.base64
              //console.log("base64 :", base64);
              setImage(base64)
            } else if(payload.content['#class'] === "idawi.service.web.Video"){
              var base64 = payload.content.base64
              setVideo(base64)
            }
          } else if(payload['#class'] === "idawi.messaging.Message" && isNotArrayOrJSON(payload.content) ){
             setPrimitiveValue(payload.content)
          }
        }
    }
  };

  /**
   * Get suggestions for the next step
   * @param idawilink - the idawilink to get suggestions from
   */
  // const getGraph = (graphLink) => {
  //   console.log("graphLink :", graphLink);
  //   var idawiListener = new EventSource(graphLink, { withCredentials: true });

  //   idawiListener.onmessage = function (event) {
  //       var s = event.data;
  //       var lines = s.split(/\r?\n/);
  //       var headerraw = lines.shift();
  //       var data = lines.join('');
  //       if(data != "EOT"){
  //         var payload = JSON.parse(data);
  //         if(payload['#class'] == "idawi.messaging.Message" && payload.content['#class'] != "idawi.messaging.EOT"){
  //           setComponentName(payload.route.elements[0])
  //           var elements = Array.from(payload.content.links.elements)
  //           elements = [...new Set(elements)];      
  //           if(elements.length > 0){
  //             var chart = new Chart([], [])
  //             elements.forEach(element => {
  //               if(element['#class'] == "idawi.service.web.Graph$Link"){
  //                   var points = element.points.elements
  //                   points.forEach(point => {
  //                       chart.listY.push(point.y);
  //                       chart.listX.push(point.x);
  //                   });
  //               }    
  //             });
  //           }
  //           setChart(chart)
  //         }
  //       }
  //   }
  // };

  /**
   * Get suggestions for the first step
   */
  React.useEffect(() => {
    getSuggestions(idawilink);
    //getGraph(graphLink);
  }, [idawilink])

  
  return (
    <React.Fragment>
      <div> Component : {componentName}</div>
      <Stack spacing={2}>
        <ChartComponent xList={chart.listX} yList={chart.listY} />
        <GraphComponent />
        <PrimitiveComponent data={primitiveValue} />
        <ImageComponent data ={image} height={180} width={180} />
        <VideoComponent data={video}  width={640} height={360} />
      </Stack>
    </React.Fragment>
  );
}