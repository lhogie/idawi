import Card from "./Card";
import React from 'react'

const Column = ({ name, width, messages }) => {
  
  
  const columnStyle = {
    width: `${width}%`,
    height: "100%",
    backgroundColor: "#f2f2f2",
    margin: "0 5px",
  };

  React.useEffect(()=>{
    console.log("updated")
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
        <Card key={index} title="Card 1" content={message.content} />
      ))}
    </div>
  );
};

export default Column;
