//observeur pour le résultat de la requete
class myIdawiObserver extends IdawiObserver {
    constructor() {
        super()
        this.components = []
    }

    //message d'erreur si Idawi a un problème
    error() {
        document.getElementById("resultats").innerHTML += '<h1 style="text-align: center;margin-left: 38%;">Connection error with idawi.<br>Try again</h1>'
    }

    //verifie si le composant qui envoit le message existe déjà ou pas pour éviter de recreer l'objet associé
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
    next(header, data) {
        if (header['semantic'] !== 'EOT') {
            var data = JSON.parse(data);
            this.parseData(data)
            this.draw()
            this.addOnClick()
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
    parseData(data) {
        var comps = data.route.elements
        var message = data.content
        var res
        if (!this.IsJsonString(message)) {
            res = this.checkJson(message)
        }
        else res = { "content": message }

        var name = comps[0].componentName
        var component = this.checkIfExists(name)

        //date d'émission à convertir à partir de secondes depuis d'EPOC
        var epoch = parseFloat(comps[0].emissionDate)
        res["emissionDate"] = new Date(epoch * 1000).toLocaleString();

        //si res est erreur
        if (res.hasOwnProperty('error'))
            component.errors.push(res)

        //si res est progression
        else if (res.hasOwnProperty('progress')) {
            component.progress = res.progress
            component.progressTarget = res.target
        }

        //si res est EOT
        else if (res.content == "EOT") {
            component.eot = true;
            component.eotdate = res.emissionDate;
        }

        //tout le reste
        else component.messages.push(res)
    }

    //check si c'est un json ou pas
    IsJsonString(str) {
        try {
            JSON.parse(str);
        } catch (e) {
            return false;
        }
        return true;
    }

    //retourne le contenu du message par rapport au type de la classe
    //à modifier sous forme de switch?
    checkJson(data) {
        var res
        //si c'est un EOT
        if (data["#class"] == 'idawi.EOT') {
            return { "content": "EOT" }
        }

        //si c'est une erreur
        if (data["#class"].includes("Exception")) {
            res = { "error": data["message"] }
        }

        //si c'est une barre de progression
        else if (data["#class"] == "idawi.ProgressRatio") {
            res = { "progress": data["progress"], "target": data["target"] }
        }

        //tout le reste sera juste un json classique
        else res = data
        return res
    }
}


//observeur pour la partie formulaire
class myFormObserver extends IdawiObserver {
    constructor() {
        super()
        this.components = []
        this.services = []
        this.currentservice;
    }

    //message d'erreur si Idawi a un problème
    error() {
        document.getElementById("resultats").innerHTML += '<h1 style="text-align: center;margin-left: 38%;">Connection error with idawi.<br>Try again</h1>'
    }

    next(header, data) {
        if (header['semantic'] !== 'EOT') {
            var data = JSON.parse(data);
            this.parseData(data)
        }
        else
            this.draw()
    }

    draw() {
        var instance = this
        var fields = document.getElementById("urlformfields")
        var res = '<div class="field">'
        var first = true
        if (this.components.length > 0) {

            res += '<label>Component(s)</label>'
            for (let i = 0; i < this.components.length; i++) {
                res += '<div class="ui checkbox"><input class="urlcheckbox" type="checkbox" id="form' + this.components[i] + '" name="' + this.components[i] + '"'
                if (first) {
                    res += 'checked'
                    first = false
                }
                res += '><label for="form' + this.components[i] + '">' + this.components[i] + '&nbsp;&nbsp;&nbsp;</label></div>'
            }
        }
        res += '</div>'
        if (this.services.length > 0) {
            this.currentservice = this.services[0];

            res += '<div class="field" id="servicefield">'
            res += '<label>Service</label><select id="serviceoption">'
            for (let i = 0; i < this.services.length; i++) {
                res += '<option value="' + this.services[i]["service"] + '">' + this.services[i]["service"] + '</option>'
            }
            res += '</select>'
            res += '</div>'

            res += '<div class="field" id="operationfield">'
            var ops = this.services[0]["operations"]
            if (ops.length > 0) {
                res += '<label>Operation</label><select id="operationoption">'
                for (let i = 0; i < ops.length; i++) {
                    res += '<option value="' + ops[i]["name"] + '">' + ops[i]["name"] + '</option>'
                }
                res += '</select>'
            }
            res += '</div>'
            res += '<div class="field" id="descriptionfield">'
            if (ops.length > 0) {
                res += '<label>Operation description</label>'
                res += '<p style="padding-top:5px">' + ops[0]["description"] + '</p>'
            }
            res += '</div>'
            res += '<div class="field">'
            if (ops.length > 0) {
                res += '<label>Operation parameters</label>'
                res += '<input id="paramfield" type="text" placeholder="parameter=value">'
            }
            res += '</div>'

            res += '<div style="padding-top:1.5em" class="field"><button class="ui secondary button" type="submit">Create URL</button></div>'
        }

        fields.innerHTML = res
        document.getElementById("serviceoption").addEventListener('change', function () {
            var ops = instance.findService(this.value)["operations"]
            var res = ""
            if (ops.length > 0) {
                res += '<label>Operation</label><select id="operationoption">'
                for (let i = 0; i < ops.length; i++) {
                    res += '<option value="' + ops[i]["name"] + '">' + ops[i]["name"] + '</option>'
                }
                res += '</select>'
                document.getElementById("operationfield").innerHTML = res

                res = ""
                res += '<label>Operation description</label>'
                res += '<p style="padding-top:5px">' + ops[0]["name"] + '</p>'
                document.getElementById("descriptionfield").innerHTML = res

                document.getElementById("operationoption").addEventListener('change', function () {
                    var res = ""
                    res += '<label>Operation description</label>'
                    res += '<p style="padding-top:5px">' + instance.findOperationDescription(this.value) + '</p>'

                    document.getElementById("descriptionfield").innerHTML = res

                });
            }


        });
        if (ops.length > 0) {

            document.getElementById("operationoption").addEventListener('change', function () {
                var res = ""
                res += '<label>Operation description</label>'
                res += '<p style="padding-top:5px">' + instance.findOperationDescription(this.value) + '</p>'

                document.getElementById("descriptionfield").innerHTML = res

            });
        }
    }

    findService(nom) {
        for (let i = 0; i < this.services.length; i++) {
            if (this.services[i]["service"] == nom) {
                this.currentservice = this.services[i]
                return this.services[i]
            }
        }
        return null
    }

    findOperationDescription(nom) {
        for (let i = 0; i < this.currentservice["operations"].length; i++) {
            if (this.currentservice["operations"][i]["name"] == nom) {
                if (this.currentservice["operations"][i]["description"] == null)
                    return "no description"
                return this.currentservice["operations"][i]["description"]
            }
        }
        return "no description"
    }


    parseData(data) {
        var content = data.content
        if (content["#class"] !== 'idawi.EOT') {
            this.components.push(content["name"])
            var servicez = content["services"]["elements"]
            for (let i = 0; i < servicez.length; i++) {
                if (!this.hasService(servicez[i]["name"]))
                    this.services.push({ "service": servicez[i]["name"], "operations": servicez[i]["operations"]["elements"] })
            }
        }
    }

    hasService(name) {
        for (let i = 0; i < this.services.length; i++) {
            if (this.services[i]["service"] == name)
                return true
        }
        return false
    }

}

//classe corréspondante à un composant issue d'une requete Idawi
class myComponent {
    constructor(name) {
        this.name = name
        this.progress = -1
        this.progressTarget = -1
        this.messages = []
        this.errors = []
        this.eot = false
        this.eotdate;
        this.displaymessage = "none"
        this.displayerror = "none"
    }

    //fonction recursive de création d'une arborescence sous forme de liste d'un json
    tree(res, data) {
        if (typeof (data) == 'object') {
            res += '<ul>';
            for (var i in data) {
                res += '<li>' + i;
                res = this.tree(res, data[i]);
                res += "</li>"
            }
            res += '</ul>';
        } else {
            res += ' => ' + data;
        }
        return res
    }

    draw() {
        var sizecolumn = "four"
        if (this.progress == -1)
            sizecolumn = "eight"
        var res = '<div id="component' + this.name + '" class="column">'

        //title
        res += '<h3><i class="component-icon"></i> Component ' + this.name + '</h3>'

        //nombre de messages
        res += '<div class="ui grid"><div id="message' + this.name + '" class="messagecomp ' + sizecolumn + ' wide column"><i class="message-icon"></i> &#160;<b>messages(' + this.messages.length + ')</b></div>'

        //nombre d'errors
        res += '<div id="error' + this.name + '" class="errorcomp ' + sizecolumn + ' wide column"> <i class="bug-icon"></i>&#160;<b> errors (' + this.errors.length + ')</b></div>'

        //affichage barre de progression
        if (this.progress != -1)
            res += '<div class="seven wide column"><progress class="progcomp" value="' + this.progress + '" max="' + this.progressTarget + '"></progress><p>' + this.progress + ' of ' + this.progressTarget + '</p></div>'
        res += '</div>'

        //affichage des messages
        res += '<div id="messageblock' + this.name + '" style="display:' + this.displaymessage + '">'
        res += '<br><h3>Messages received</h3>'
        for (let i = this.messages.length - 1; i >= 0; i--) {
            res += '<p style="text-align:justify"><b> message n°' + (i + 1) + '</b></p>'
            var treeres = ""
            treeres = this.tree(treeres, this.messages[i])
            res += treeres
        }
        res += '</div>'

        //affichage des erreurs
        res += '<div id="errorblock' + this.name + '" style="display:' + this.displayerror + '">'
        res += '<br><h3>Errors received</h3>'
        for (let i = this.errors.length - 1; i >= 0; i--) {
            res += '<p style="text-align:justify"><b> Error n°' + (i + 1) + '</b></p>'
            var treeres = ""
            treeres = this.tree(treeres, this.errors[i])
            res += treeres
        }
        res += '</div>'

        //si eot alors affiche message de fin de transmission
        if (this.eot)
            res += '<br><h3>Transmission ended the ' + this.eotdate + '</h3>'
        res += '</div>'
        return res
    }
}
