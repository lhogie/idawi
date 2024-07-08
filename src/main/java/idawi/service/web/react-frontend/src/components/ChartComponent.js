import React from 'react';
import Plot from 'react-plotly.js';

export default function ChartComponent({xList, yList}){
    return (
        <Plot
          data={[
            {
              x:xList,
              y:yList,
              type: 'lines',
              mode: 'lines+markers',
              marker: {color: 'red'},
            },          ]}
          layout={ {width: 320, height: 240, title: 'A Fancy Plot'} }
        />
      );
}