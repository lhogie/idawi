import Column from "./Column";
import * as React from "react";


const Layout = () => {
  const [idawilink, setIdawiLink] = React.useState(
    "http://localhost:8081/api/idawi.routing.ForceBroadcasting//gw/idawi.service.DemoService/SendGraph"
  );
  const [columnNames, setColumnNames] = React.useState([]);

    //create dictionary 
    //key: component name
    //value: array of messages
    // [{
    //   gw, 
    //   messages [
    //     paload, 
    //     payload
    //   ]
    // }]
    const [components, setComponents] = React.useState({});


    

  const getMessages = () => {
    var idawiListener = new EventSource(idawilink, { withCredentials: true });
    idawiListener.onmessage = function (event) {
      var s = event.data;
      var lines = s.split(/\r?\n/);
      var headerraw = lines.shift();
      var data = lines.join("");
      if (data != "EOT") {
        var payload = JSON.parse(data);
        if (
          payload["#class"] === "idawi.messaging.Message" &&
          payload.content["#class"] !== "idawi.messaging.EOT"
        ) {
          let componentName = payload.route.elements[0];
          console.log(componentName)
          // if (!columnNames.includes(componentName)) {
          //   const newColumnNames = [...columnNames, componentName];
          //   setColumnNames(newColumnNames);
          // } else {
          //   console.log("Already exists");
          //   console.log(columnNames);
          // }
          // console.log(payload.content);
          if(components[componentName] !== undefined){
            //component already exists
            //add message to the array
            let componentMessages = components[componentName].push(payload)
            setComponents({...components, [componentName]: componentMessages})
            //console.log(components)
          }else{
            //component doesn't exist
            //create new array
            components[componentName] = [payload]
            setComponents({...componentName})
            //console.log(components)
          }
          
        }
      }
    };
  };


  React.useEffect(() => {
    getMessages();
  }, []);


  return (
    <div className="container" style={{ height: "100vh", display: "flex" }}>
      {Object.keys(components).forEach((values, key) => (
        <Column name={key} width={20} messages={values} />
      ))}
    </div>
  );
};
export default Layout;