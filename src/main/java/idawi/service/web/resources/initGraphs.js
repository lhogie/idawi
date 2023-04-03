//var connection = new WebSocket('ws://127.0.0.1:8000/serversocket');
var url_server = "http://127.0.0.1:8000";
var plotly;
var key_machine = "Machine";

/*connection.onopen = function () {
    console.log('Connected!');
    connection.send('Ping,Hello,World'); // Send the message 'Ping' to the server
};*/

// Log errors
/*connection.onerror = function (error) {
    console.log('WebSocket Error ', error);
};*/

// Log messages from the server
/*connection.onmessage = function (e) {
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

};*/

$.ajax({
    url: url_server + "/ajax/getAllTokens"
}).done((data) => {
    let jdata = JSON.parse(data);
    console.log(jdata);
    constructionListTokens (jdata);
})

/*$.ajax({
    url: url_server + "/ajax/getAllData"
}).done ((data) => {
    let jdata = JSON.parse(data);
    console.log(jdata);
    plotly = new PlotlyGeneration (jdata);
});*/

/*setInterval(() => {
    $.ajax({
        url: url_server + "/ajax/getLastData"
    }).done ((data) => {
        let jdata = JSON.parse(data);
        plotly.addPoint(jdata[0], jdata[1]);
    }); }, 1000);*/

window.onload = () => {
    console.log("GRAPHS");
}

var ALL_PLOTS = {};
function constructionListTokens (jdata) {
    let getAllKeys = (jdata) => {
        let keys = [];
        keys.push (key_machine);
        Object.keys(jdata).forEach((machine) => {
           jdata[machine].forEach((propertie) => {
               if (!keys.includes(propertie.name)) {
                   keys.push(propertie.name);
               }
           })
        });
        return keys;
    }

    let table = $("<table></table>");
    let tr = $("<tr></tr>");
    console.log(getAllKeys (jdata));
    getAllKeys (jdata).forEach((key) => {
        tr.append(
            $("<th></th>")
                .text(key)
        );
    });
    table.append(tr);

    let add_point = false;
    Object.keys(jdata).forEach((machine) => {
        let tr = $("<tr></tr>");
        let text = "";
        let bgcolor = "inherit";
        console.log(getAllKeys(jdata));
        getAllKeys (jdata).forEach((key) => {
            text = "";
            bgcolor = "inherit";

            add_point = false;
            let my_tuple = null;
            jdata[machine].forEach((tuple) => {
                if (!add_point && tuple.name == key) {
                    add_point = true;
                    my_tuple = tuple;
                }
            });

            if (add_point) {
                //text = "X";
                bgcolor = "green";
            }
            if (key == key_machine) {
                text = machine;
            }
            let new_td = $("<td></td>").text(text).css({"background-color": bgcolor, "min-width": "20px"});
            if (add_point) {
                let tmp = null;
                new_td.click((ev) => {
                    if (tmp == null) { tmp = new TokensPlotly(machine, my_tuple); }
                    else {
                        console.log(tmp);
                        tmp.generateMain ();
                    }
                });
            }
            tr.append(new_td);
        });
        /*
        jdata[machine].forEach ((token) => {
            let ctp = new TokensPlotly(token);
            ALL_PLOTS[token.name] = ctp;
            table.append(
                $("<tr></tr>")
                    .append($("<td></td>")
                        .text(token.name))
                    .append($("<td></td>")
                        .text(token.unit))
                    .append($("<td></td>")
                        .text(token.range_min))
                    .append($("<td></td>")
                        .text(token.range_max))
                    .click ((ev) => {
                        console.log(ctp);
                    })
            );
        });*/
        table.append(tr);
    });

    $("#list-tokens").append(table);
}

function randint (min, max) {
    return (Math.random() * (max - min)) | 0 + min;
}