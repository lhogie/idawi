
class TokensPlotly {

    constructor(machine, token) {
        this.machine = machine;
        this.token = token;
        console.log({
            machine: machine,
            token: token.name
        });
        $.post(url_server + "/ajax/getAllData", {
            machine: machine,
            token: token.name
        }).done((data) => {
            console.log(data);
            this.plotly = new PlotlyGeneration(machine, token.name, JSON.parse(data));
        });
    }

    generateMain () {
        this.plotly.generateMain();
    }
}


class PlotlyGeneration {

    constructor (machine, title, json1) {
        this.machine = machine;
        this.title = title;
        this.courbe1 = this.parseJson (json1, "lines");
        // this.courbe2 = this.parseJson (json2, "lines");
        this.container = $("<div></div>");
        this.generatePlotly(this.courbe1);

        this.generateMain();
        //this.addPoint (x, y);
    }

    addPoint (date, value) {
        Plotly.extendTraces(this.container.get(0), {
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
        let t = Plotly.newPlot(this.container.get(0), this.currentDatas, {
            title: this.title,
            width: 600
        });
        let fin = Date.now();
        console.log(fin - debut + " millisecondes de chargement...");

        return t;
    }

    generateHeader () {
        let reduce = false;
        this.main.append($("<table></table>")
            .addClass("initialize-plotly")
            .append($("<tr></tr>")
            // machine name
            .append($("<td></td>")
                .text(this.machine)
            )
            // button reduction
            .append($("<td></td>")
                .text("-")
                .css({
                    "min-width": "50px"
                })
                .click ((ev) => {
                    reduce = !reduce;
                    console.log(this.container);
                    if (reduce) {
                        this.container.css({display: "none"})
                    } else {
                        this.container.css({display: "inherit"})
                    }
                })
            )
            // button suppression
            .append($("<td></td>")
                .text("x")
                .click ((ev) => {
                    this.mainContainer.remove();
                })
            )
        ))
    }

    generateMain () {
        this.mainContainer = $("<div></div>");
        this.main = $("<div></div>");
        //this.container = $("<div></div>");

        this.generateHeader ();
        //this.generatePlotly (this.courbe1);
        this.mainContainer.append(this.main).append(this.container);
        $("#plotly-visualization").append(this.mainContainer);
    }
}