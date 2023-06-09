import React, { useState } from "react";
import JSONViewer from "./JSONViewer"


const Card = ({ title, content }) => {
  const [isPopupOpen, setIsPopupOpen] = useState(false);

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
    setIsPopupOpen(true);
  };

  const closePopup = () => {
    setIsPopupOpen(false);
  };

  return (
    <div>
      <div style={cardStyle} onClick={openPopup}>
        <h3 style={titleStyle}>{title}</h3>
        <JSONViewer data={content} />
      </div>
      {isPopupOpen && (
        <div className="popup-overlay">
          <div className="popup-content">
            <h3 style={titleStyle}>{title}</h3>
            <button onClick={closePopup}>Close</button>
          </div>
        </div>
      )}
    </div>
  );
};
export default Card;
