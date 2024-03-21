import Card from "./Card";
import React from 'react'
import JSONViewer from "./JSONViewer"

const Column = ({ name, messages }) => {
  
  
  const columnStyle = {
    height: "100%",
    backgroundColor: "#f2f2f2",
    margin: "0 5px",
  };

  React.useEffect(()=>{
  }, [messages])

  const headerStyle = {
    fontWeight: "bold",
    textAlign: "center",
    top: "0",
  };

  return (
    <div style={columnStyle}>
      <h2 style={headerStyle}>{name}</h2>
      {messages.map((message, index) => (
        <div>
            <Card key={index} title={message.content['#class'].split('.').pop()} content={message} />
        </div>
      ))}
    </div>
  );
};

export default Column;
