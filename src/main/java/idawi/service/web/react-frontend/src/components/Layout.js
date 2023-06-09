import Column from "./Column";
import React, { useEffect, useState } from "react";
import ResizablePanels from "./ResizablePanels";
import { AppBar, Button, Container, Stack, TextField, Toolbar, Typography } from "@mui/material";

const Layout = () => {
  const [idawilink, setIdawiLink] = useState(
    "http://localhost:8081/api/idawi.routing.ForceBroadcasting///idawi.service.DemoService/SendGraph"
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

  const updateIdawiLink = (value) => {
    setIdawiLink(value)
  }

  useEffect(() => {
    getMessages();
  }, []);

  return (
    <div>
      <div> IDAWI </div>
      <Stack spacing={2} direction={"row"} mt={2} ml={5} mb={5} mr={5}>
          <TextField 
            id="outlined-basic" 
            label="URL" 
            variant="outlined" 
            value={idawilink}
            onChange={(event, newValue) => {
              updateIdawiLink(newValue);
            }}
            fullWidth 
          />
          <Button variant="contained" onClick={getMessages}>
            Valider
          </Button>
      </Stack>
      <div className="container" style={{ display: "flex" }}>
        <ResizablePanels>
          {Object.keys(components).map((key) => (
            <div>
              <Column key={key} name={key} messages={components[key]} />
            </div>
          ))}
        </ResizablePanels>
      </div>
    </div>
  );
};

export default Layout;
