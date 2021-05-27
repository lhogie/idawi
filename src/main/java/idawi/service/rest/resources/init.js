$.getJSON("/api", (json) => {
    console.log(json);
    todoAfterReceiveJSON(json);
});

var TYPESELECTION = "simple";
var WINDOWWIDTH = window.innerWidth * 0.65;
var WINDOWHEIGHT = window.innerHeight

//le code lancé une fois le json récupéré
function randint (min, max) {
    return Math.floor(Math.random() * (max - min) + min);
}

function todoAfterReceiveJSON (json) {
    // nodes -> le json envoyé par l'appli Idawi
    let nodes = json.value;

    // options pour l'initialisation de visnetwork
    let options = {
        nodes: {
            shape: 'dot',
            scaling: {
                label: {
                    enabled: true
                },
                customScalingFunction: function (min,max,total,value) {
                    return 0.05 * value;
                }
            },
            //title: htmlTitle("<ul><li>Hello World</li></ul>")
        },
        autoResize: true,
        physics: {
            // stabilization: true,
            // adaptiveTimestep: true,
            // timestep: true,
            barnesHut: {
                springLength: 200,
            }
        }
    }

    let props = {
        "CPU": {
            "name": "Nombre de CPU",
            "valeur_min": 1,
            "valeur_max": 512
        },
        "load": {
            "name": "load ratio",
            "valeur_min": 0,
            "valeur_max": 1
        },
        "RAM": {
            "name": "RAM",
            "valeur_min": 1,
            "valeur_max": 64
        },
        "DNS": {
            "name": "DNS",
            "valeur_min": 1,
            "valeur_max": 3
        }
    };

    let attributes = {
        "Node color": {
            type: "color",
            functionApply: (key, fx) => {
                if (key != "") {
                network.getListNodes().forEach((node) => {
                    node.setBackgroundColor (visnetwork, fx.getcolor (node.params[key]));
                });
                } else {
                    network.getListNodes().forEach((node) => {
                        node.setDefaultBackgroundColor(visnetwork);
                    });
                }
                visnetwork.redraw ();
            }
        },
        "Node size" : {
            type: "function",
            function: "20",
            dftfunction: "20",
            functionApply: (key, fx) => {
                console.log(key, fx);
                var newFX = new Function ("x", "return " + fx);
                network.getListNodes().forEach((node) => {
                    let newSize = newFX (node.params[key]);

                    visnetwork.body.data.nodes.updateOnly({
                        id: node.id,
                        value: newSize
                    });
                });
                visnetwork.redraw ();
            }
        },
        "Border color": {
            type: "color",
            functionApply: (key, fx) => {
                if (key != "") {
                    network.getListNodes().forEach((node) => {
                        node.setBorderColor(visnetwork, fx.getcolor(node.params[key]))
                    });
                } else {
                    network.getListNodes().forEach((node) => {
                        node.setDefaultBorderColor(visnetwork);
                    });
                }
                visnetwork.redraw ();
            }
        },
        "Border size": {
            type: "function",
            function: "1",
            dftfunction : "1",
            functionApply: (key, fx) => {
                var newFX = new Function ("x", "return " + fx);
                network.getListNodes().forEach((node) => {
                    let newSize = newFX (node.params[key]);

                    visnetwork.body.data.nodes.updateOnly({
                        id: node.id,
                        borderWidth: newSize
                    });
                });
                visnetwork.redraw ();
            }
        }
    };
/*
    nodes.knownComponents.forEach((component) => {
        $.each(props, (key, value) => {
            component[key] = randint (value.valeur_min, value.valeur_max)
        });
    });
  */

    // generate our own network object
    let network = generateNetwork (nodes);
    
    // that we use to create the VIS network
    let visnetwork = createNetwork (
        document.querySelector ("#reseau-machine"),
        network,
        options,
        WINDOWWIDTH, WINDOWHEIGHT);

    network.getListNodes().forEach((node) => {
        node.setDefaultColor(visnetwork);
    });

    // variable implémentant le "noeud" courant
    let currentNode = new SelectionNodes(nodes, network, visnetwork);
    currentNode.node = nodes.localComponent;

    // la sélection d'un noeud sélectionne aussi tous les liens qui lui sont dépendants
    let eventVisnetwork = new EventVisnetwork(network, visnetwork, currentNode);

    // CREATION DU MENU DE CHOIX
    hotbar = new Hotbar();
    hotbar.addPanelLinkPropertiesToFunction(attributes, props);
    hotbar.addPanelChangeLabel(network, visnetwork, Object.keys(nodes.knownComponents[0]));
    hotbar.addEntry("Sélection Simple", (e) => {
        TYPESELECTION = "simple";
    });
    hotbar.addEntry("Sélection Multiple", (e) => {
        TYPESELECTION = "multiple";
    });

    $("#information-network").append(hotbar.generateHTML ());
}

function setContainerWidth () {
    $("#container-network").css({
        width: window.innerWidth * 0.65
    });
    $("#container-nodes").css({
        width: window.innerWidth * 0.3
    });
}

window.addEventListener ("load", () => {
    setContainerWidth();
});

window.addEventListener("resize", () => {
    setContainerWidth();
});

