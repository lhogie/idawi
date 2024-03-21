import React from 'react';
import { JSONTree } from 'react-json-tree';

/**
 * This component is used to display a JSON object
 * @returns the JSONViewer component
 * @param data - the data to display
*/
function JSONViewer({ data }) {
  const theme = {
    scheme: 'monokai',
    author: 'wimer hazenberg',
    base00: '#272822',
    base01: '#383830',
    base02: '#49483e',
    base03: '#75715e',
    base04: '#a59f85',
    base05: '#f8f8f2',
    base06: '#f5f4f1',
    base07: '#f9f8f5',
    base08: '#f92672',
    base09: '#fd971f',
    base0A: '#f4bf75',
    base0B: '#a6e22e',
    base0C: '#a1efe4',
    base0D: '#66d9ef',
    base0E: '#ae81ff',
    base0F: '#cc6633'
  };

  const iconStyle = {
    marginLeft: '2px',
    marginRight: '5px'
  };

  return (
    <div style={{ display: 'flex' }}>
      <div style={{ width: '1000px', marginRight: '20px', textAlign: 'left' }}>
        <JSONTree data={data} theme={theme} iconStyle={iconStyle} />
      </div>
      <div style={{ flex: 1 }}>
        {/* Le reste de votre contenu ici */}
      </div>
    </div>
  );
}

export default JSONViewer;