var connection = new WebSocket('ws://127.0.0.1:8001');

var plotly;
connection.onopen = function () {
    console.log('Connected!');
    //connection.send('Ping'); // Send the message 'Ping' to the server
};

// Log errors
connection.onerror = function (error) {
    console.log('WebSocket Error ', error);
};

// Log messages from the server
connection.onmessage = function (e) {
    console.log('Server: ', e, e.data);
    let message = JSON.parse(e.data);

    switch (message.name) {
        case "getAllData":
            plotly = new PlotlyGeneration (message.data);
            break;
        case "addData":
            try {
                plotly.addPoint(message.data[0], message.data[1]);
            } catch (e) {
                console.log(e);
            }
            break;
    }

};

window.onload = () => {
    console.log("GRAPHS");
}

class PlotlyGeneration {

    constructor (json1) {
        this.courbe1 = this.parseJson (json1, "lines");
       // this.courbe2 = this.parseJson (json2, "lines");
        this.generatePlotly (this.courbe1);
        //this.addPoint (x, y);
    }

    addPoint (date, value) {
        Plotly.extendTraces('plotly-visualization', {
            x: [[date]],
            y: [[value]]
        }, [0]);

    }

    parseJson (json, mode) {
        let res = {
            x: [],
            y: [],
            //fill: 'tonexty',
            fill: 'tozeroy',
            mode: mode,
            name: mode
        }
        json.forEach ((tuple) => {
            res.y.push(tuple[1]);
            res.x.push(tuple[0]);
        });
        return res;
    }

    generatePlotly (tuple1) {
        this.currentDatas = [tuple1];
        let debut = Date.now();
        let t = Plotly.newPlot("plotly-visualization", this.currentDatas, {
            title:"Affichage de courbes randomizées =D"
        });
        let fin = Date.now();
        console.log(fin - debut + " millisecondes de chargement...");
        return t;
    }
}

function randint (min, max) {
    return (Math.random() * (max - min)) | 0 + min;
}