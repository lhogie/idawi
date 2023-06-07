class Hotbar {

    constructor() {
        this.mainContainer = $("<div></div>")
            .attr({
                id: "vis-network-hotbar"
            });
        this.main = $("<div></div>");

        this.addPanelDragAndResize();
        // this.addPanelSortedNodes(props);
        // this.addPanelFonctionNodes (props);
        this.nbEntries = 0;
    }

    addPanelDragAndResize () {
        let container = $("<div></div>");

        // creation du drag
        let draggerCreation = () => {
            let canMove = false;
            let dragger = $("<span></span>");
            let mouse = {}
            let pos = {}
            let draggingPanel = () => {
                if (canMove) {
                    this.mainContainer.css({
                        top: mouse.top - pos.top - 10,
                        left: mouse.left - pos.left - 10
                    });
                    requestAnimationFrame(draggingPanel);
                }
            }
            dragger
                .css({
                    display: "inline-block",
                    padding: "10px",
                    "background-color": "red",
                    width: "20px",
                    height: "20px"
                }).mousedown((ev) => {
                canMove = true;
                mouse.top = ev.clientY;
                mouse.left = ev.clientX;
                requestAnimationFrame(draggingPanel);
            });

            $(window).mousemove((ev) => {
                mouse.top = ev.clientY;
                mouse.left = ev.clientX;
            }).mouseup ((ev) => {
                canMove = false;
            });
            this.mainContainer.mousedown((ev) => {
                pos.top = ev.offsetY;
                pos.left = ev.offsetX;
            });
            return dragger
        }

        let resizerCreation = () => {
            let isClick = false;
            let resizer = $("<div></div>").css({
                display: "inline-block",
                padding: "10px",
                "background-color": "green",
                width: "20px",
                height: "20px",
                float: "right"
            }).mouseup ((ev) => {
                if (isClick = !isClick) {
                    this.main.css({
                        display: "none"
                    })
                } else {
                    this.main.css({
                        display: "inherit"
                    })
                }
            })
            return resizer;
        }
    //    container.append(draggerCreation());
        container.append(resizerCreation());
        this.mainContainer.append (container);
    }

    addPanelLinkPropertiesToFunction (attributes, properties) {
        function applyColorModification (attribut, myselect, inputmin, inputmax, input1, input2) {
            let min = parseInt(inputmin.val()), max = parseInt(inputmax.val());
            if (!isNaN(min) && !isNaN(max)) {
                let c1 = hexToRgb(input1.val()), c2 = hexToRgb(input2.val());

                let aff = (x1, y1, x2, y2) => {
                    let a = (y2 - y1) / (x2 - x1);
                    return {
                        a: (y2 - y1) / (x2 - x1),
                        b: y1 - a * x1,
                        calc: function (x) { return this.a * x + this.b; }
                    }
                }

                let colorRegression = {
                    r: aff(min, c1.r, max, c2.r),
                    g: aff(min, c1.g, max, c2.g),
                    b: aff(min, c1.b, max, c2.b),
                    parse: (x) => {
                        let val = (x | 0).toString(16);
                        (val.length == 1) ? val = "0" + val : val = val;
                        return val
                    },
                    getcolor: function (x) {
                        return "#" + this.parse(this.r.calc(x)) + this.parse(this.g.calc(x)) + this.parse(this.b.calc(x))
                    }
                }
                attribut.functionApply (myselect.currentValue, colorRegression);
            }
        }

        function createSelect (properties, inputmin, inputmax, inputfx, input1, input2, attribut) {
            let myselect = $("<select></select>")
                .addClass("select-propertie-value")
                .change (function (ev) {
                    let valeur = this.value;
                    if (myselect.currentValue != undefined) {
                        $(".select-propertie-value").each ((index, select) => {
                            $(select.options).each ((index, option) => {
                                if (option.value == myselect.currentValue) {
                                    $(option).css({display: "inherit"});
                                }
                            });
                        });
                    }

                    $(".select-propertie-value").each ((index, select) => {
                        if (this.value != "") {
                            // on supprime l'option pour les autres listes
                            $(select.options).each ((index, option) => {
                                if (option.value == valeur) {
                                    $(option).css({display: "none"});
                                }
                            });
                        }
                    });
                    myselect.currentValue = valeur;

                    if (valeur != "") {
                        inputmin.val(properties[valeur].valeur_min);
                        inputmax.val(properties[valeur].valeur_max);
                    } else {
                        inputmin.val("");
                        inputmax.val("");

                        if (attributes[attribut].type == "function" && valeur == "") {
                            attributes[attribut].functionApply (myselect.currentValue, new Function(attributes[attribut].dftfunction));
                        }
                        if (attributes[attribut].type == "color" && valeur == "") {
                            attributes[attribut].functionApply (myselect.currentValue);
                        }
                    }
                });

            if (attributes[attribut].type == "color") {
                inputfx.append($("<button></button>")
                    .text("Appliquer")
                    .click (() => {
                        applyColorModification (attributes[attribut], myselect, inputmin, inputmax, input1, input2);
                    })
                )
            }

            if (inputfx != undefined) {
                inputfx.keypress((ev) => {
                    if (ev.keyCode == 13) {
                        attributes[attribut].functionApply (myselect.currentValue, inputfx.val());
                    }
                });
            }

            myselect.append($("<option></option>").text(""));
            $.each(properties, (key, value) => {
                myselect.append($("<option></option>")
                    .text(key)
                    .attr({
                        value: key
                    })
                )
            });
            return myselect;
        }

        let table = $("<table></table>").append(
            $("<tr></tr>")
                .append($("<th></th>")
                    .text("Propriété"))
                .append($("<th></th>")
                    .text("Champs"))
                .append($("<th></th>")
                    .text("Bornes"))
                .append($("<th></th>")
                    .text("Fonction"))
        );

        Object.keys(attributes).forEach ((attribut) => {
            let inputmin = $("<input>").attr({readonly: false, class: "parameters-values-borders"});
            let inputmax = $("<input>").attr({readonly: false, class: "parameters-values-borders"});

            let input1 = $("<input>").attr({type: "color"});
            let input2 = $("<input>").attr({type: "color"});

            let inputfunction;
            switch (attributes[attribut].type) {
                case "function" :
                    inputfunction = $("<input>");
                    break;
                case "color":
                    let color = "black";
                    switch (attribut) {
                        case "Node color":
                            color = new Node(0, "", {}).dftColorbg;
                            break;
                        case "Border color":
                            color = new Node(0, "", {}).dftColorborder;
                            break;
                        default:
                    }
                    input1.attr({value: color});
                    input2.attr({value: color});

                    inputfunction = $("<div></div>")
                        .append(input1)
                        .append(input2);
                    break;
                case "label" :
                    inputfunction = $("<input>");
                    break;
            }

            if (attributes[attribut].type == "function" || attributes[attribut].type == "label") {
                inputfunction.val(attributes[attribut].function);
            }

            let line = $("<tr></tr>")
                .append($("<td></td>")
                .text(attribut))
                .append($("<td></td>")
                    .append(createSelect(properties, inputmin, inputmax, inputfunction, input1, input2, attribut))
                )
                .append($("<td></td>")
                    .append($("<table></table>")
                        .append($("<tr></tr>")
                            .append($("<td></td>").text("min"))
                            .append($("<td></td>").append(inputmin))

                            .append($("<td></td>").text("max"))
                            .append($("<td></td>").append(inputmax))
                        )
                ))
                .append($("<td></td>")
                    .append(inputfunction));

            table.append (line);
        });

        this.main.append (table);
    }

    addPanelChangeLabel (network, visnetwork, listProperties) {
        let conteneur = $("<div></div>");
        let select = $("<select></select>").change ((ev) => {
            network.getListNodes().forEach((node) => {
               node.setMessageLabel (select.val());
            });
            visnetwork.redraw();
        });
        listProperties.forEach ((element) => {
            select.append($("<option></option>").text(element));
        });
        select.val("friendlyName");

        conteneur.append($("<table></table>")
            .append($("<tr></tr>")
                .append($("<td></td>").text("Label"))
                .append($("<td></td>").append(select))
            )
        );
        this.main.append(conteneur);
    }

    addPanelFonctionNodes (properties, toDoFunctionCallback) {
        function textbox (id, text, value, name) {
            return $("<div></div>")
                .append(
                    $("<label></label>")
                        .attr ({
                            "id": "label-" + id,
                            "for": name
                        })
                        .text (text)
                )
                .append(
                    $("<input>").attr({
                        id: id,
                        name: "name",
                        value: value
                    })
                );
        }
        let fonction_sort_node = "20";

        let select = $("<select></select>").change (function (ev) {
            console.log(ev);
            console.log(this.value);
            $("#valeur-min-select").val(properties[this.value].valeur_min);
            $("#valeur-max-select").val(properties[this.value].valeur_max);
            $("#fonction-select").val(properties[this.value].fonction);
        });

        let container = $("<div></div>");
        container.append(textbox("valeur-min-select", "Valeur Minimale", properties[Object.keys(properties)[0]].valeur_min, "valeur_min"));
        container.append(textbox("valeur-max-select", "Valeur Maximale", properties[Object.keys(properties)[0]].valeur_max, "valeur_max"));
        container.append($("<div></div>")
            .append(
                $("<label></label>")
                    .attr ({
                        "id": "label-fonction-select"
                    })
                    .text ("Fonction d'affichage")
            )
            .append($("<input>")
                .attr({
                    type: "text",
                    id: "fonction-select",
                    value: fonction_sort_node
                })
            ).append($("<button></button>")
                .text("Valider")
                .click ((ev) => {
                    properties[select.val()].fonction = $("#fonction-select").val();
                    console.log(select, properties);
                    toDoFunctionCallback (
                        select.val(),
                        properties[select.val()].fonction);
                })
            )
        );

        $.each(properties, (key, value) => {
            properties[key].fonction = fonction_sort_node;
            select.append($("<option></option>")
                .text(key)
            )
        });

        this.main.append($("<div></div>")
            .append(select)
            .append(container)
        );
    }

    // fonction rajoutant un panel permettant de faire une sélection d'une propriété
    // et fonction de cette propriété, réajuste le graphique
    addPanelSortedNodes (properties) {
        function createTextBox (id, min, max, dft, name, textLabel, todo) {
            if (min >= max) {
                min = max - 1;
            }
            let span = $("<span></span>")
                .text(dft)
                .attr({
                    "id": "span-"+id
                });
            let input = $("<input>").attr({
                "type": "range",
                "min": min,
                "max": max,
                "step": 1,
                "value": dft,
                "name": name,
                "id": id
            }).on("input", (ev) => {
                span.text(input.val());
                todo (input.val());
            });
            return $("<div></div>")
                .append(
                    $("<label></label>")
                        .text(textLabel)
                        .attr({"for": name})
                )
                .append(input)
                .append(span);

        }

        console.log(properties);
        let select = $("<select></select>");
        $.each(properties, (key, value) => {
            select.append($("<option></option>")
                .text(value.name)
            )
        });

        this.main.append($("<div></div>")
            .append(select)
            .append(createTextBox("min-selection-idawi", 0, 100, 0, "minSelect", "Valeur minimale", (val) => {
                $("#max-selection-idawi").attr({
                    min: val
                });
                $("#span-max-selection-idawi").text($("#max-selection-idawi").val());
            }))
            .append(createTextBox("max-selection-idawi", 0, 100, 10, "maxSelect", "Valeur maximale", (val) => {

            }))
        );
    }

    addEntry (label, todo) {
        this.main.append ($("<div></div>")
            .append($("<input>")
                .attr({
                    type: "radio",
                    id: "radio-hotbar-" + this.nbEntries,
                    name: "hotbar",
                    value: "single"
                })
                .prop ({
                    checked: (this.nbEntries == 0)
                })
            )
            .append ($("<label></label>")
                .text(label)
                .attr({
                    for: "radio-hotbar-" + this.nbEntries
                })
            )
            .change((e) => {
                todo(e);
            })
        )
        this.nbEntries++;
    }

    addSelectorBackgroundColor () {
        let inputColor = $("<input>")
            .attr({
                type: "color",
                value: "#f0f0f0"
            })
            .change(function () {
                $("div.vis-network").css({
                    "background-color": this.value
                });
            });
        let selectorBgColor = $("<div></div>")
            .append($("<label></label>")
                .text("Sélection de la couleur de background")
            )
            .append(inputColor);
        this.main.append(selectorBgColor);
    }

    generateHTML () {
        return this.mainContainer.append(this.main);
    }

}

function hexToRgb(hex) {
    var result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex);
    return {
        r: parseInt(result[1], 16),
        g: parseInt(result[2], 16),
        b: parseInt(result[3], 16)
    };
}