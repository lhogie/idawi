import Column from "./Column";
import React, { useEffect, useState } from "react";

const Layout = () => {
  const [idawilink, setIdawiLink] = useState(
    "http://localhost:8081/api/idawi.routing.ForceBroadcasting//gw/idawi.service.DemoService/SendGraph"
  );
  const [columnNames, setColumnNames] = useState([]);
  const [components, setComponents] = useState({});

  const getMessages = () => {
    var idawiListener = new EventSource(idawilink, { withCredentials: true });
    idawiListener.onmessage = function (event) {
      var s = event.data;
      var lines = s.split(/\r?\n/);
      var headerraw = lines.shift();
      var data = lines.join("");
      if (data !== "EOT") {
        var payload = JSON.parse(data);
        if (
          payload["#class"] === "idawi.messaging.Message" &&
          payload.content["#class"] !== "idawi.messaging.EOT"
        ) {
          let componentName = payload.route.elements[0];
          if (components[componentName] !== undefined) {
            components[componentName].push(payload);
            setComponents({ ...components});
          } else {
            components[componentName] = [];
            components[componentName].push(payload);  
            setComponents({ ...components });
          }
        }
      }
    };
  };

  useEffect(() => {
    getMessages();
  }, []);

  return (
    <div>
      <div className="container" style={{ height: "100vh", display: "flex" }}>
        {Object.keys(components).map((key) => (
          <Column key={key} name={key} width={20} messages={components[key]} />
        ))}
      </div>
    </div>
  );
};

export default Layout;
