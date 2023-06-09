import React from "react";
import ReactDOM from "react-dom";
import Graph from "react-vis-network-graph";

export default function GraphComponent({graph}) {

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
  const events = {
    select: function (event) {
      var { nodes, edges } = event;
      console.log(edges);
      console.log(nodes);
    }
  };
  return (
    <Graph
        graph={graph}
        options={options}
        events={events}
        getNetwork={(network) => {
          //  if you want access to vis.js network api you can set the state in a parent component using this property
        }}
    />
  );
}
