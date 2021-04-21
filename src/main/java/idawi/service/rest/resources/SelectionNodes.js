
// utilisation :
// currentNode.node = "c6"
// la fonction set va convertir le c6 en la variable node correspondante
class SelectionNodes {
    constructor (nodes, network, visnetwork) {
        this.colorBackground = "red";
        this.colorBorder = "red";


        // la liste des tous les noeuds du réseau sous format json
        this.nodes = nodes;
        // la classe network, créant à partir du json un réseau de noeud interconnectés les uns aux autres
        this.network = network;
        // l'objet vis créé à partir de this.network
        this.visnetwork = visnetwork;

        // une liste contenant le ou les noeuds actuellement sélectionnés
        this.nInternal = [];
        // un pointeur indiquant, en cas de plusieurs noeuds sélectionné, celui qui a le focus
        this.indexShow = 0;
        this.nListener = () => {
            this.fillCurrentNode (this.nInternal[this.indexShow]);
        }
    }

    // le coeur de la classe
    // appelé lors du set d'un noeud
    // son comportement :
    // cette fonction permet, à l'initialisation de node, de réaffecter le noeud correctement
    // en cas de sélection simple, on réinitialise le précédant noeud courant, puis on écrase l'ancien noeud par le nouveau
    // en cas de sélection multiple, le nouveau noeud n'écrase pas les précédantes sélection mais se rajoute
    set node (val) {
        // on remet à 0 tous les noeuds appartenant à la sélection courante
    /*    this.nInternal.forEach((node) => {
    //        this.network.getNodeFromLabel (node.friendlyName).setDefaultColor (this.visnetwork);
        });*/

        // deux cas de figure possible
        // sélection "simple"
        // sélection "multiple"

        // dans le cas d'une sélection simple, on vide la précédante sélection et on remet à 0 le pointeur
        if (TYPESELECTION == "simple") {
            this.indexShow = 0;
            this.nInternal = [];
        }

        // test vérifiant si le noeud a déjà été sélectionné (évitons d'enregistrer deux fois le même noeud)
        if (!this.isAlreadySelected(val)) {
            // le noeud n'était pas présent dans notre sélection courante, on le rajoute !
            this.addNode(val);
        } else {
            // aha le noeud a été sélectionné ! bon bah on le déselectionne dans ce cas :(
            this.removeNode (val);
        }

        // maintenant que le.s noeud.s sont correctement set, on leur applique la couleur spécifique à la sélection
    /*    this.nInternal.forEach((node) => {
    //        this.network.getNodeFromLabel (node.friendlyName).setColor (this.visnetwork, this.colorBackground, this.colorBorder);
        });*/
    }

    get node () {
        return this.nInternal;
    }

    // fonction ajouter un noeud val aux noeuds déjà sélectionnés
    addNode (val) {
        this.nodes.knownComponents.forEach((n) => {
            // le noeud dont le friendlyname vaut val est ajouter à la liste
            if (n.friendlyName == val) {
                this.nInternal.push(n);
            }
        });
        this.nListener();
    }

    removeNode (val) {
        for (let i = 0; i<this.nInternal.length; i++) {
            if (this.nInternal[i].friendlyName == val) {
                this.nInternal.splice(i, 1);
            }
        }
        this.nListener();
    }

    getActualNodePointed () {
        return this.nInternal[this.indexShow];
    }

    isAlreadySelected (val) {
        let isSelect = false;
        this.nInternal.forEach((node) => {
            if (val == node.friendlyName) {
                isSelect = true;
                return isSelect;
            }
        });
        return isSelect;
    }

    /* Fonctions Events */

    evSelectNode (params) {
        this.node = this.network.getNode(params.nodes[0]).label;
    }

    /* Fonction manipulant le DOM */

    // fonction retournant un élément jQuery contenant un sélect possédant tous les noeuds sélectionnés
    generateHTML () {
        let html = $("<select></select>")
            .addClass("form-control")
            .change((e) => {
                this.indexShow = $(e.currentTarget).val();
                this.fillHTMLDescriptor(this.nInternal[this.indexShow]);
            });
        this.nInternal.forEach ((value, index) => {
            html.append($("<option></option>")
                .text(value.friendlyName)
                .attr({
                    value: index
                })
                .addClass("form-control")
            )
        });
        return $("<div></div>")
            .addClass("input-group")
            .addClass("col-som-5")
            .append($("<span></span>")
                .addClass("input-group-addon")
                .text("Machines")
            )
            .append(html);
    }

    fillCurrentNode (node) {
        $("#container-nodes")
            .empty()
            .append(this.generateHTML ())
            .append(new InformationNode(node).generateHTMLDescriptor());
    }

    fillHTMLDescriptor (node) {
        $("#node-descriptor").remove();
        $("#container-nodes").append(new InformationNode(node).generateHTMLDescriptor());
    }

}