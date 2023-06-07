/**
 * Observeur pour le résultat de la requête.
 * @author : Saad el din AHMED
 * @Refactoring : Eliel WOTOBE
 * @email : elielwotobe@gmail.com
 */
export default class myIdawiObserver extends IdawiObserver {
    constructor() {
        super()
        this.components = []
    }

    //message d'erreur si Idawi a un problème
    error() {
        document.getElementById("resultats").innerHTML += '<h1 style="text-align: center;margin-left: 38%;">Connection error with idawi.<br>Try again</h1>'
    }

    //verifie si le composant qui envoie le message existe déjà ou pas pour éviter de recreer l'objet associé
    checkIfExists(name) {
        for (let i = 0; i < this.components.length; i++) {
            if (this.components[i].name == name)
                return this.components[i];
        }
        let newcomp = new myComponent(name)
        this.components.push(newcomp)
        return newcomp;
    }

    // implementation de next qui sera appelé par l'observable à chaque nouveau message
    newMessage(header, data) {
        if (header['semantic'] !== 'EOT') {
            var message = JSON.parse(data);
            //console.log(message)
            //this.processData(data)
            var name = message.route.elements[0].componentName
            var component = this.checkIfExists(name)
            message.deplied = false
            component.messages.push(message)
            //console.log(component)
            this.draw()
            //this.addOnClick()
        }
        else { console.log("EOT") }
    }

    //dessine le résultat de la requete
    draw() {
        var results = document.getElementById("resultats")
        var res = '<div class="center aligned row">'
        for (let i = 0; i < this.components.length; i++) {
            res += this.components[i].draw()
        }
        res += '</div>'
        results.innerHTML = res
    }

    //met en place le fait d'afficher ou non les messages si on clique sur l'icone, pareil pour erreurs
    addOnClick() {
        var instance = this
        for (let i = 0; i < this.components.length; i++) {
            document.getElementById("message" + this.components[i].name).addEventListener('click', function (e) {
                var name
                if (e.target.id.includes("message")) {
                    name = e.target.id.replace("message", "")
                }
                else name = e.target.parentNode.id.replace("message", "")
                var comp = instance.checkIfExists(name)
                if (comp.displaymessage == "none")
                    comp.displaymessage = "block"
                else comp.displaymessage = "none"
                document.getElementById("messageblock" + comp.name).style.display = comp.displaymessage
            });
            document.getElementById("error" + this.components[i].name).addEventListener('click', function (e) {
                var name
                if (e.target.id.includes("error")) {
                    name = e.target.id.replace("error", "")
                }
                else name = e.target.parentNode.id.replace("error", "")
                var comp = instance.checkIfExists(name)
                if (comp.displayerror == "none")
                    comp.displayerror = "block"
                else comp.displayerror = "none"
                document.getElementById("errorblock" + comp.name).style.display = comp.displayerror
            });
        }
    }

    //traite les données reçus dans le data
    processData(message) {

        /*var name = message.route.elements[0].componentName
        var component = this.checkIfExists(name)
        component.messages.push(message)*/

        //si data est erreur
       /* if (message.content["#class"] === "idawi.RemoteException")
            component.errors.push(message.content)*/

        //si res est progression
        /*else if (message.content["#class"] === "idawi.ProgressRatio") {
            component.progress = message["progress"]
            component.progressTarget = message["target"]
        }*/

        //si res est EOT
        /*if (message.content["#class"] == 'idawi.EOT') {
            component.eot = true;
            //date d'émission à convertir à partir de secondes depuis d'EPOC
            var epoch = parseFloat(message.route.elements[0].emissionDate)
            console.log(epoch)
            component.eotdate = new Date(epoch * 1000).toLocaleString();
        }*/

        //tout le reste
        //console.log(this.components[0].messages)
    }

}