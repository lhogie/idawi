import React from "react";
import ReactDOM from "react-dom";
import Graph from "react-vis-network-graph";

export default function GraphComponent() {

  const graph = {
    nodes: [
      { id: 1, label: "1", title: "node 1 tootip text", color: "blue" },
      { id: 2, label: "2", title: "node 2 tootip text" },
      { id: 3, label: "3", title: "node 3 tootip text" },
      { id: 4, label: "4", title: "node 4 tootip text" },
      { id: 5, label: "5", title: "node 5 tootip text" },
      { id: 6, label: "6", title: "node 6 tootip text" }
    ],
    edges: [
      { from: 1, to: 2, dashes: true},
      { from: 1, to: 3 },
      { from: 2, to: 4 },
      { from: 2, to: 5 },
      { from: 2, to: 6 },
      { from: 6, to: 1 },
      { from: 5, to: 6 }
    ]
  };

  const options = {
    layout: {
      hierarchical: false
    },
    edges: {
      color: "red"
    },
    height: "500px"
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
