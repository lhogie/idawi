import Column from "./Column";
import React, { useEffect, useState } from "react";
import ResizablePanels from "./ResizablePanels";
import { AppBar, Button, Container, Stack, TextField, Toolbar, Typography } from "@mui/material";

const Layout = () => {
  const [idawilink, setIdawiLink] = useState(
    "http://localhost:8081/api/idawi.routing.ForceBroadcasting///idawi.service.DemoService/SendGraph"
  );
  const [newLink, setNewLink] = useState("")

  const [components, setComponents] = useState({});
  const [openedEventSources, setOpenedEventSources] = useState([])



  const getMessages = () => {
    openedEventSources.forEach((eventSource) => {
      eventSource.close()
    })
    let idawiListener = new EventSource(idawilink, { withCredentials: true });
    setOpenedEventSources(openedEventSources => [...openedEventSources, idawiListener])

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

  const updateIdawiLink = (event) => {
    setNewLink(event.target.value)
  }


  useEffect(() => {
    getMessages();
  }, [idawilink]);

  return (
    <div>
      <div> IDAWI </div>
      <Stack spacing={2} direction={"row"} mt={2} ml={5} mb={5} mr={5}>
          <TextField 
            id="outlined-basic" 
            label="URL" 
            variant="outlined" 
            value={newLink}
            onChange={(event) => {
              updateIdawiLink(event);
            }}
            fullWidth 
          />
          <Button variant="contained" onClick={()=>{
            setComponents({})            
            setIdawiLink(newLink)
          }}>
            Valider
          </Button>
      </Stack>
      <div >
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
