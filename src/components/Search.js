import "../Style/Search.css";
import { useState, useEffect, useRef } from "react";
import { Button, TextField, CircularProgress } from "@mui/material";
import JSONViewer from "./JSONViewer";
import { ReactComponent as Arrow } from "../assets/arrow.svg";
function Search() {
  const payload = useRef();
  const [allItems, setallItems] = useState([]);
  const [URL, setURL] = useState("http://localhost:8081/api?");
  const [tabSuggestion, settabSuggestion] = useState([]);
  const [tabMessage, settabMessage] = useState([]);
  const [historique, sethistorique] = useState([]);
  function fetch_to_search(URL) {
    payload.current.innerHTML = "";

    sethistorique((oldArray) => [
      ...oldArray,
      [new Date().toLocaleString(), URL],
    ]);
    var idawiListener = new EventSource(URL);
    idawiListener.onmessage = function (event) {
      var s = event.data;
      var lines = s.split(/\r?\n/);
      var headerraw = lines.shift();
      var header = JSON.parse(headerraw);
      if (header["encodings"] === "plain") {
        idawiListener.close();
      } else {
        var data = lines.join("");
        var data = JSON.parse(data);
        if (data["#class"] == "suggestion") {
          settabSuggestion((oldArray) => [...oldArray, data]);
        } else if (data["#class"] == "message") {
          let content =
            data.content.base64 ||
            data.content.bytes ||
            data.content.progress ||
            data.content.msg ||
            data.content;
          let contentType = data.content["#class"] || "String";
          settabMessage([content, contentType, data]);
        }
      }
    };
  }
  function HistoriqueFunc(URL) {
    setURL(URL);
    fetch_to_search(URL);
  }
  function check(e, message) {
    if (
      e.target.parentElement.children[2].style.height == "0px" ||
      e.target.parentElement.children[2].style.height == ""
    ) {
      e.target.parentElement.children[2].style.height = "200px";
      e.target.parentElement.style.maxHeight = "800px";
      e.target.parentElement.children[1].style.borderRadius = "0px 0px 0px 0px";
    } else {
      e.target.parentElement.children[2].style.height = "0px";
      e.target.parentElement.style.maxHeight = "140px";
      e.target.parentElement.children[1].style.borderRadius =
        "0px 0px 10px 10px";
    }
  }
  useEffect(() => {
    if (tabMessage.length > 0) {
      setallItems((oldArray) => [
        ...oldArray,
        [tabMessage[0], tabMessage[1], tabMessage[2]],
      ]);
    }
  }, [tabMessage]);

  return (
    <div className="SearchPage">
      <div className="Navos">
        <div className="NavUp">
          <h2>Idawi : Front Visualizer</h2>
        </div>
        <div className="NavLeft">
          <h4>Historique</h4>
          <div>
            {historique.map((e) => {
              return <div onClick={() => HistoriqueFunc(e[1])}>{e[0]}</div>;
            })}
          </div>
        </div>
        <div className="SearchSection">
          <div className="Search">
            <TextField
              label="Enter URL"
              variant="standard"
              defaultValue={URL}
              value={URL}
              sx={{ m: 1, mt: 3, width: "50ch" }}
              onChange={(e) => {
                setURL(e.target.value);
              }}
            />
            <Button variant="contained" onClick={() => fetch_to_search(URL)}>
              <svg
                xmlns="http://www.w3.org/2000/svg"
                class="icon icon-tabler icon-tabler-arrow-badge-right-filled"
                width="24"
                height="24"
                viewBox="0 0 24 24"
                stroke-width="1.5"
                stroke="#000000"
                fill="none"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <path stroke="none" d="M0 0h24v24H0z" fill="none" />
                <path
                  d="M7 6l-.112 .006a1 1 0 0 0 -.669 1.619l3.501 4.375l-3.5 4.375a1 1 0 0 0 .78 1.625h6a1 1 0 0 0 .78 -.375l4 -5a1 1 0 0 0 0 -1.25l-4 -5a1 1 0 0 0 -.78 -.375h-6z"
                  stroke-width="0"
                  fill="currentColor"
                />
              </svg>
            </Button>
          </div>
          <div className="showPayload" ref={payload}>
            {allItems.map((e) => {
              return (
                <div className="data_content slide-in-fwd-center">
                  {e[1] == "String" || e[1] == "progress_message" ? (
                    <p> {e[0]}</p>
                  ) : e[1] == "raw data" || e[1] == "image" ? (
                    <img src={"data:image/png;base64," + e[0]} />
                  ) : e[1] == "progress_ratio" ? (
                    <CircularProgress variant="determinate" value={e[0]} />
                  ) : (
                    ""
                  )}

                  <div
                    className="data_complete"
                    onClick={(el) => check(el, e[2])}
                  >
                    <Arrow />
                  </div>
                  <JSONViewer data={e[2]} />
                </div>
              );
            })}
          </div>
        </div>
      </div>
    </div>
  );
}

export default Search;
