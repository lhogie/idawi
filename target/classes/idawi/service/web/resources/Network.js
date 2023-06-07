class Node {
    constructor (id, label, params) {
        this.id = id;
        this.label = label;
        this.messageLabel = label;
        this.params = params;
        this.contacts = [];
        this.colorbg = this.calcRandomColorByID ();
        this.colorborder = "#648FC9";
        this.listPropertiesToDraw = [];
        this.dftColorbg = "#97c2fc";
        this.dftColorborder = "#648FC9";
    }

    calcRandomColorByID () {
        let hashCode = function(s) {
            var h = 0, i = s.length;
            while (i >= 0)
                h = (h << 5) - h + (s.charCodeAt(--i) | 0);
            return h;
        };
        let hashId = hashCode(this.id.toString() + this.label);

        let c = {
            r: (hashId & 255).toString(16),
            g: ((hashId >> 8) & 255).toString(16),
            b: ((hashId >> 16) & 255).toString(16),
            getColor: function () {
                return "#" + (this.r.length == 1 ? "0" + this.r : this.r) +
                    (this.g.length == 1 ? "0" + this.g : this.g) +
                    (this.b.length == 1 ? "0" + this.b : this.b);
            }
        }

        return c.getColor ();
    }

    setMessageLabel (property) {
        this.messageLabel = this.params[property];
    }

    setColor (visnetwork, colorbg, colorborder) {
        //this.colorbg = colorbg;
        //this.colorborder = colorborder;
       // console.log("hellloooo" , this.id, colorbg, colorborder);
        //setColorNodeNetwork (visnetwork, this.id, this.colorbg, this.colorborder);
        this.setBackgroundColor(visnetwork, colorbg);
        this.setBorderColor(visnetwork, colorborder);
    }

    setDefaultColor (visnetwork) {
        this.colorbg = this.calcRandomColorByID ();
        this.colorborder = "#648FC9";

        this.setBackgroundColor(visnetwork, this.colorbg);
        this.setBorderColor(visnetwork, this.colorborder);
    }

    setDefaultBackgroundColor (visnetwork) {
        this.colorbg = this.calcRandomColorByID();
        visnetwork.body.data.nodes.updateOnly({
            id: this.id,
            color: {
                background: this.colorbg
            }
        });
    }

    setDefaultBorderColor (visnetwork) {
        this.colorborder = this.dftColorborder;
        visnetwork.body.data.nodes.updateOnly({
            id: this.id,
            color: {
                border: this.colorborder
            }
        });
    }

    setBackgroundColor (visnetwork, color) {
        this.colorbg = color;
        visnetwork.body.data.nodes.updateOnly({
            id: this.id,
            color: {
                background: color
            }
        });
    }

    setBorderColor (visnetwork, color) {
        this.colorborder = color;
        visnetwork.body.data.nodes.updateOnly({
            id: this.id,
            color: {
                border: color
            }
        });
    }

    linkTo (node) {
        if (this.contacts.includes(node)) {
            return false;
        }
        this.contacts.push(node);
    }

    getNode () {
        return {
            id: this.id,
            label: this.label,
            value: 20
        }
    }

    getContacts () {
        let res = [];
        this.contacts.forEach ((contact) => {
            res.push ({
                from: this.id,
                to: contact.id,
                arrows: {
                    to: {
                        type: "arrow",
                        enabled: true
                    }
                }
            });
        });
        return res;
    }

    drawInformation (ctx, x, y, scale, color="black") {
        ctx.save ();
        ctx.fillStyle = color;
        var visibleFontSize = 16*scale;

       // console.log(scale, visibleFontSize);
        (visibleFontSize > 25) ? visibleFontSize = 25/scale : visibleFontSize = 16;
        ctx.font = visibleFontSize + "px Arial";

        ctx.textBaseline = 'middle';
        ctx.textAlign = "center";
        ctx.fillText(this.messageLabel, x, y);

        this.listPropertiesToDraw.forEach((property) => {
            ctx.font = "italic" + visibleFontSize * 0.7 + "px Arial";
        });
        ctx.restore ();
    }
}

class Link {
    constructor (nfrom, nto) {
        this.from = nfrom;
        this.to = nto;
    }
}

class Network {
    constructor () {
        this.listNodes = [];
        this.visEdges = null;
        this.visNodes = null;
    }

    addNode (label, params) {
        let ID;
        this.listNodes.length > 0 ? ID = this.listNodes[this.listNodes.length -1].id + 1 : ID = 0;
        let newNode = new Node (ID, label, params);
        this.listNodes.push(newNode);
        return newNode;
    }

    linkNode (from, to) {
        from.linkTo (to);
    }

    getNode (ID) {
        let res = null;
        this.listNodes.forEach ((node) => {
            if (node.id == ID) {
                res = node;
                return;
            }
        })
        return res;
    }

    getNodeFromLabel (label) {
        let res = null;
        this.listNodes.forEach ((node) => {
            if (node.label == label) {
                res = node;
                return;
            }
        })
        return res;
    }

    getIdFromLabel (name) {
        let res = null;
        this.listNodes.forEach ((node) => {
            if (node.label == name) {
                res = node;
                return;
            }
        });
        return res;
    }

    getData () {
        let data = [];
        let link = [];
        this.listNodes.forEach ((node) => {
            data.push(node.getNode ());
            if (node.getContacts ().length > 0) {
                link = link.concat(node.getContacts ());
            }
        });

        this.visNodes = new vis.DataSet (data);
        this.visEdges = new vis.DataSet (link);
        return {
            nodes: this.visNodes,
            edges: this.visEdges
        }
    }

    getListNodes () {
        return this.listNodes;
    }

    getNodes () {
        return this.visNodes;
    }

    getEdges () {
        return this.visEdges;
    }
}

class EventVisnetwork {
    constructor(network, visnetwork, currentNode) {
        this.network = network;
        this.visnetwork = visnetwork;
        this.currentNode = currentNode;
        this.addEventSelectNode();
        this.addEventSelectLink();
/*
        this.addEvent ("deselectNode", () => {

        });

        this.addEvent("deselectEdge", () => {

        });
 */
    }

    addEventSelectNode () {
        this.addEvent ("selectNode", (params) => {
            this.currentNode.evSelectNode (params);
        });
    }

    addEventSelectLink () {
        this.addEvent("selectEdge", (params) => {

        });
    }

    addEvent (nameEvent, todo) {
        this.visnetwork.addEventListener (nameEvent, (params) => {
            console.log("EVENT " + nameEvent, params);
            todo (params);
        });
    }
}

function setColorNodeNetwork (network, idNode, backgroundColor, borderColor) {
    try {
        let node = network.body.nodes[idNode];
        let lastColorBorder = node.options.color.border;
        let lastColorBackground = node.options.color.background;
        node.options.color = {
            border: lastColorBorder,            // ces deux paramètres gèrent les couleurs des liens dépendants du noeud
            background: lastColorBackground,

            highlight: {
                border: borderColor,
                background: backgroundColor,
            },
            hover: {}
        }
    } catch (e) { console.log(e); }
}

function generateNetwork (nodes) {
    let network = new Network();
    // création du graphique network à partir du JSON
    nodes.knownComponents.forEach((n) => {
        network.addNode (n.friendlyName, n);
    });

    nodes.knownComponents.forEach((n) => {
        n.neighbors.forEach((voisin) => {
            network.linkNode (network.getIdFromLabel (n.friendlyName), network.getIdFromLabel (voisin));
        });
    });

    return network;
}

// attention, main doit être un composant html natif, pas un composant jQuery
function createNetwork (container, network, options={}, width=600, height=600) {
    let main = document.createElement ("div");
    main.style.width = width+"px";
    main.style.height = height+"px";
    container.appendChild (main);

    let visnetwork = new vis.Network (main, network.getData(), options);

    visnetwork.on("afterDrawing", function (ctx) {
        network.getListNodes().forEach((node) => {
            let nodePosition = visnetwork.getPositions([node.id]);
            let colorGenerator = {
                r: parseInt(node.colorbg.substr(1,2), 16) > 120 ? "00": "ff",
                g: parseInt(node.colorbg.substr(3, 2), 16) > 120 ? "00": "ff",
                b: parseInt(node.colorbg.substr(5, 2), 16) > 120 ? "00": "ff",
                
                compute: function () {
                    if (this.r == "00" || this.g == "00" || this.b == "00") { return "#000000"; }
                    return "#ffffff";
                }
            }

            node.drawInformation (ctx, nodePosition[node.id].x, nodePosition[node.id].y, visnetwork.getScale(), colorGenerator.compute ());
        });
    });

    return visnetwork;
}