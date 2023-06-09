import * as React from 'react';
import { Chart } from '../models/Chart';
import ChartComponent from './ChartComponent';
import GraphComponent from './GraphComponent';
import ImageComponent from './ImageComponent';
import VideoComponent from './VideoComponent';
import PrimitiveComponent from './PrimitiveComponent';
import { Stack } from '@mui/material';
import Graph from "react-vis-network-graph";


/**
 * This component is used to build an idawi link
 * @returns the UrlBuilder component
 */
export default function ComponentContent({content}) {

  const [idawilink, setIdawiLink] = React.useState("http://localhost:8081/api/idawi.routing.ForceBroadcasting//gw/idawi.service.DemoService/SendImage");
  //const graphLink = "http://localhost:8081/api/idawi.routing.ForceBroadcasting//gw/idawi.service.DemoService/SendGraph"
 // const imageLink = "http://localhost:8081/api/idawi.routing.ForceBroadcasting//gw/idawi.service.DemoService/SendImage"
  const [steps, setSteps] = React.useState([{title: "Routing", choices: []}, {title: "Parameters", choices: []}, {title: "Components", choices: []}, {title: "Services", choices: []}, {title: "Operations", choices: []}]);
  //const [idawilink, setIdawiLink] = React.useState("http://localhost:8081/api/idawi.routing.ForceBroadcasting//gw/idawi.service.DemoService/SendChart");
  const graphUrl = "http://localhost:8081/api/idawi.routing.ForceBroadcasting//gw/idawi.service.DemoService/SendGraph"
  
  // make this graph dynamic
 

  const [nodes, setNodes] = React.useState([
    ]);

  const [edges, setEdges] = React.useState([{from: 1, to: 2, dashes: true}]);

  const [graph, setGraph] = React.useState({
    nodes: nodes,
    edges: edges
  });

  const [contentType, setContentType] = React.useState("")

  const [componentName, setComponentName] = React.useState("");
  const [chart, setChart] = React.useState([])
  const [image, setImage] = React.useState("")
  const [video, setVideo] = React.useState("")
  const [primitiveValue, setPrimitiveValue] = React.useState("")


  const updateIdawiLink = (newValue) => {
    //setIdawiLink(newValue);
    setIdawiLink(newValue)
  };
  
  const options = {
    layout: {
      improvedLayout: true,
      clusterThreshold: 1,
      hierarchical: false
    },
    edges: {
      color: "red"
    },
  };


  function isNotArrayOrJSON(variable) {
    return !Array.isArray(variable) && typeof variable !== 'object';
  }

  const parsePayload = (payload) => {
    if(payload['#class'] === "idawi.messaging.Message" && payload.content['#class'] !== "idawi.messaging.EOT"){
      const contentType = payload.content['#class'].split(".").pop();
      setContentType(contentType)
      console.log("dazdadad :", contentType);
      if(payload.content['#class'] === "idawi.service.web.chart.Chart"){
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
      } else if(payload.content['#class'] === "idawi.service.web.Graph"){
        var links = payload.content.links.elements
        if ( links !== undefined && links.length > 0){
           var elements = Array.from(payload.content.links.elements)
        }
        elements = [...new Set(elements)];      
        elements.forEach(graphLink => {              
          if(graph.nodes.find(node => node.id === graphLink.a.label) === undefined && graph.nodes.find(node => node.id === graphLink.b.label) === undefined && graph.edges.find(edge => edge.from === graphLink.a.label && edge.to === graphLink.b.label) === undefined){              

            if(graphLink.a.color !== undefined && graphLink.b.color !== undefined && graphLink.style !== undefined){
                
                var newNodeA = {id: graphLink.a.label, label: (graphLink.a.label).toString(), color: graphLink.a.color}
                var newNodeB = {id: graphLink.b.label, label: (graphLink.b.label).toString(), color: graphLink.b.color}
                var newEdge = {from: graphLink.a.label, to: graphLink.b.label, dashes: graphLink.style === "dotted"}
                
                setGraph(graph => ({ ...graph, nodes: [...new Set([...graph.nodes, newNodeA, newNodeB])]}));
                setGraph(graph => ({ ...graph, edges: [...new Set([...graph.edges, newEdge])]}));
            }
            else if(graphLink.a.color !== undefined && graphLink.b.color === undefined){
                
                var newNodeA = {id: parseInt(graphLink.a.label), label: (graphLink.a.label).toString(), color: graphLink.a.color}
                var newEdge = {from: parseInt(graphLink.a.label), to: parseInt(getToNodeId(graphLink.b, elements)), dashes: graphLink.style === "dotted"}
                
                setGraph(graph => ({ ...graph, nodes: [...new Set([...graph.nodes, newNodeA])]}));
                setGraph(graph => ({ ...graph, edges: [...new Set([...graph.edges, newEdge])]}));
            }
            else if(graphLink.a.color === undefined && graphLink.b.color !== undefined){
                
                var newNodeB = {id: parseInt(graphLink.b.label), label: (graphLink.b.label).toString(), color: graphLink.b.color}
                var newEdge = {from: parseInt(getToNodeId(graphLink.a, elements)), to: parseInt(graphLink.b.label), dashes: graphLink.style === "dotted"}
                
                setGraph(graph => ({ ...graph, nodes: [...new Set([...graph.nodes, newNodeB])]}));
                setGraph(graph => ({ ...graph, edges: [...new Set([...graph.edges, newEdge])]}));
            }
            else {
                var newEdge = createEdgeFromLink(graphLink, elements)
                setGraph(graph => ({ ...graph, edges: [...new Set([...graph.edges, newEdge])]}));
            }
          }
        });
      }
    } else if(payload['#class'] === "idawi.messaging.Message" && isNotArrayOrJSON(payload.content) ){
       setPrimitiveValue(payload.content)
    }
  };

  /**
   * Get suggestions for the next step
   * @param idawilink - the idawilink to get suggestions from
   */
  // const getSuggestions = (idawilink) => {
  //   setIdawiLink(idawilink);
  //   console.log("idawilink :", idawilink);
  //   var idawiListener = new EventSource(idawilink, { withCredentials: true });

  //   idawiListener.onmessage = function (event) {
  //       var s = event.data;
  //       var lines = s.split(/\r?\n/);
  //       var headerraw = lines.shift();
  //       var data = lines.join('');
  //       if(data !== "EOT"){
  //         var payload = JSON.parse(data);
  //         if(payload['#class'] === "idawi.messaging.Message" && payload.content['#class'] !== "idawi.messaging.EOT"){
  //           if(payload.content['#class'] === "idawi.service.web.chart.Function"){
  //             setComponentName(payload.route.elements[0])
  //             var elements = Array.from(payload.content.functions.elements)
  //             elements = [...new Set(elements)];      
  //             if(elements.length > 0){
  //               var chart = new Chart([], [])
  //               elements.forEach(element => {
  //                 if(element['#class'] === "idawi.service.web.chart.Function"){
  //                     var points = element.points.elements
  //                     points.forEach(point => {
  //                         chart.listY.push(point.y);
  //                         chart.listX.push(point.x);
  //                     });
  //                 }
  //               });
  //             }
  //             setChart(chart)
  //           } else if(payload.content['#class'] === "idawi.service.web.Image"){
  //             var base64 = payload.content.base64
  //             //console.log("base64 :", base64);
  //             setImage(base64)
  //           } else if(payload.content['#class'] === "idawi.service.web.Video"){
  //             var base64 = payload.content.base64
  //             setVideo(base64)
  //           }
  //         } else if(payload['#class'] === "idawi.messaging.Message" && isNotArrayOrJSON(payload.content) ){
  //            setPrimitiveValue(payload.content)
  //         }
  //       }
  //   }
  // };

  /**
   * Get graph data
   * @param graphUrl - the graphUrl to get graph payload
   */
  const getGraph = (graphUrl) => {
    var idawiListener = new EventSource(graphUrl, { withCredentials: true });
    idawiListener.onmessage = function (event) {
        var s = event.data;
        var lines = s.split(/\r?\n/);
        var headerraw = lines.shift();
        var data = lines.join('');
        if(data !== "EOT"){
          var payload = JSON.parse(data);
          if(payload['#class'] === "idawi.messaging.Message" && payload.content['#class'] !== "idawi.messaging.EOT"){
            setComponentName(payload.route.elements[0])
           
          }
        }
    }
  };

  const getToNodeId = (node, elements) => {
    var toNodeIndex = node['#link_to'].split(".")[4]
    var toNodeElement = node['#link_to'].split(".")[5]
    return elements[toNodeIndex][toNodeElement].label
  }

  const createEdgeFromLink = (graphLink, elements) => {
    if(graphLink.a['#link_to'] !== undefined || graphLink.b['#link_to'] !== undefined){
      var from_index = graphLink.a['#link_to'].split(".")[4]
      var from_element = graphLink.a['#link_to'].split(".")[5]
      var to_index = graphLink.b['#link_to'].split(".")[4]
      var to_element = graphLink.b['#link_to'].split(".")[5]
      return {from: parseInt(elements[from_index][from_element].label), to: parseInt(elements[to_index][to_element].label), dashes: graphLink.style === "dotted"}
    }
  }

  const events = {
    select: function (event) {
      var { nodes, edges } = event;
      console.log(edges);
      console.log(nodes);
    }
  };

  const displayComponent = (ComponentType) => {
    switch(ComponentType) {
      case "Chart":
        return <ChartComponent xList={chart.listX} yList={chart.listY} />
      case "Graph":
        return <GraphComponent graph={graph} options={options} events={events} />
      case "Image":
        return <ImageComponent data ={image} height={180} width={180} />
      case "Video":
        return <VideoComponent data={video}  width={640} height={360} />
      case "Primitive":
        return <PrimitiveComponent data={primitiveValue} />
      default:
        return <div> No component to display </div>
    }
  }


  /**
   * Get suggestions for the first step
   */
  React.useEffect(() => {
    parsePayload(content);
    // getGraph(graphUrl);
  }, [content])

  
  return (
    <React.Fragment>
      {displayComponent(contentType)}
      {/* <Stack spacing={2}>
        <ChartComponent xList={chart.listX} yList={chart.listY} />
        <GraphComponent />
        <Graph
        graph={graph}
        options={options}
        events={events}
        getNetwork={(network) => {
          //  if you want access to vis.js network api you can set the state in a parent component using this property
        }}/>
        <PrimitiveComponent data={primitiveValue} />
        <ImageComponent data ={image} height={180} width={180} />
        <VideoComponent data={video}  width={640} height={360} />
      </Stack> */}
    </React.Fragment>
  );
}