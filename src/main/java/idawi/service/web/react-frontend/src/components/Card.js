import React, { useState } from "react";
import JSONViewer from "./JSONViewer"
import ComponentContent from "./ComponentContent";
import FormGroup from '@mui/material/FormGroup';
import FormControlLabel from '@mui/material/FormControlLabel';
import { Switch } from "@mui/material";

const Card = ({ title, content }) => {
  const [isPopupOpen, setIsPopupOpen] = useState(false);
  const [checked, setChecked] = React.useState(false);

  const cardStyle = {
    backgroundColor: "#ffffff",
    padding: "10px",
    margin: "10px",
    boxShadow: "0px 2px 4px rgba(0, 0, 0, 0.1)",
    cursor: "pointer",
  };

  const titleStyle = {
    fontWeight: "bold",
    marginBottom: "5px",
  };

  const openPopup = () => {
    setIsPopupOpen(!isPopupOpen);
  };

  const handleChange = (event) => {
    setChecked(event.target.checked);
  };

  return (
    <div>
      <div style={cardStyle} onClick={openPopup}>
        <h3 style={titleStyle}>{title}</h3>
        <FormGroup>
          <FormControlLabel value={checked} control={<Switch  onChange={handleChange}/>} label="Afficher Payload" />
      </FormGroup>
      </div>
      {checked &&  (
        <JSONViewer data={content} />
      )}
      {isPopupOpen && !checked && (
        <div className="popup-overlay">
          <div className="popup-content">
            <ComponentContent content={content} />
          </div>
        </div>
      )}
    </div>
  );
};
export default Card;
