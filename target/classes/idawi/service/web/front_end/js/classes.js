/**
 * Observeur pour le résultat de la requête.
 * @author : Saad el din AHMED
 * @Refactoring : Eliel WOTOBE
 * @email : elielwotobe@gmail.com
 */
class myIdawiObserver extends IdawiObserver {
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


/**
 * Observeur pour la partie formulaire.
 * @author : Saad el din AHMED
 */
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

    newMessage(header, data) {
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

/**
 * Classe correspondant à un composant issu d'une requête.
 * @author : Saad el din AHMED
 * @Refactoring : Eliel WOTOBE
 * @email : elielwotobe@gmail.com
 */
class myComponent {
    constructor(name) {
        this.name = name
        this.progress = -1
        this.progressTarget = -1
        this.messages = []
        this.message
        this.errors = []
        this.eot = false
        this.eotdate;
        this.displaymessage = "none"
        this.displayerror = "none"
    }
    /**
     * Fonction récursive de création d'une arborescence sous
     * forme de liste d'un json.
     * @param  res  résultat construit au fur et à mesure de l'exécution de tree
     * @param  data  message avec lequel on construit le résultat
     * @return res
     */
     tree(res, data) {
        if (typeof (data) == 'object' && data != null) {
             res += '<ul>';
             for (var i in data) {
                 res += '<li data-collapsed="true">' + i;
                 res = this.tree(res, data[i]);
                 res += "</li>"
             }
             res += '</ul>'
        } else {
            res += ' => ' + data;
        }
        return res
    }

    /**
     * Fonction permettant de créer la partie html du message/
     * @return void
     */
    draw() {
        var sizecolumn = "four"
        if (this.progress == -1)
            sizecolumn = "eight"
        var messages = this.messages
        var progr_bar =""
        var title = ""
        var nb_errors = this.countNbErrors();
        var actual_msgs_numbers = this.messages.length - (this.countNbOfEOTMsgs() + nb_errors);
        var res = '<div id="component' + this.name + '" class="column">'
        //title

            if (nb_errors) {
                res += title = '<h3><i class="component-icon"></i> Component ' + this.name + ' (' + actual_msgs_numbers + '/' + '<span style="color: red">' + nb_errors + '</span>' + ')' + '</h3>'
            } else
                res += title = '<h3><i class="component-icon"></i> Component ' + this.name + ' (' + actual_msgs_numbers + '/' + nb_errors + ')' + '</h3>'

        /*for(let i =0 ; i < this.messages.length; i++) {

            if(this.messages[i].content["#class"] === "idawi.ProgressRatio"){
                progr_bar = '<div class="seven wide column"><progress style="width: 100%" value="' + this.messages[i].content["progress"] + '" max="' + this.messages[i].content["target"] + '"></progress><p>' + this.messages[i].content["progress"] + ' of ' + this.messages[i].content["target"] + '</p></div>'
            }else if(this.messages[i].content["#class"] == 'idawi.EOT' && this.messages.length == 2) {
                progr_bar = '<div class="seven wide column"><progress style="width: 100%" value="' + 100 + '" max="' + 100 + '"></progress><p>' + 100 + ' of ' + 100 + '</p></div>'
            }

        }
        res += progr_bar*/





        /*if(this.errors.length === 0)
            res += '<h3><i class="component-icon"></i> Component ' + this.name + ' ('+ this.messages.length  +'/' + this.errors.length +')' + '</h3>'
        else
        res += '<h3><i class="component-icon"></i> Component ' + this.name + ' ('+ this.messages.length  +'/' + '<span style="color: red">' +this.errors.length+'</span>' +')' + '</h3>'
*/
        //nombre de messages
        //res += '<div class="ui grid"><div id="message' + this.name + '" class="messagecomp ' + sizecolumn + ' wide column"><i class="message-icon"></i> &#160;<b>messages(' + this.messages.length + ')</b></div>'
        //var progr_bar = ""
        //nombre d'errors
        //progr_bar = '<div class="seven wide column"><progress class="progcomp" value="'+ 0 +'" max="'+ 100+ '"></progress><p>' + 0 + ' of ' + 100 + '</p></div>'


        //affichage barre de progression
        //res +='<div><div></div>'

        /*if (this.progress != -1)
            progr_bar = '<div class="seven wide column"><progress class="progcomp" value="' + this.progress + '" max="' + this.progressTarget + '"></progress><p>' + this.progress + ' of ' + this.progressTarget + '</p></div>'
        else if (this.eot === true){
            progr_bar+= '<div class="seven wide column"><progress style="width: 100%" value="' + 100 + '" max="' + 100 + '"></progress><p>' + 100 + ' of ' + 100 + '</p></div>'
        }*/
        //res+= progr_bar
        //res+= '<div></div></div>'
        //console.log(messages)
        //affichage des messages et des erreurs
        res += '<div ="messageblock' + this.name +'">'
        res+= '<table>'

        for(let i = 0; i < this.messages.length; i++){
            //var a = i+1
            //var b = i+2
            //var content = JSON.stringify(this.messages[i].content);
            //console.log(content)
            if(this.messages[i].content["#class"] !== 'idawi.EOT' && this.messages[i].content["#class"] !=='idawi.RemoteException'){
                //res += '<p style="text-align:justify"><b>' + i + '</b></p>'
                //res += '<b>Message n°' + i + '</b>'
                //res+= '<div class="root"></div>'
                //const tree = jsonview.renderJSON(content,document.querySelector('.root'))
                //jsonview.collapse(tree)
                //     console.log(i)
                var treeres = ""
                treeres = this.tree(treeres, this.messages[i])

                res+= '<tr>'
                res+= '<td colspan="2"><b>'+i+'</b></td>'
                res+='<td>'
                res+= this.messages[i].content["#class"]!== undefined ? '<ul data-role = "treeview" id="parent"'+i+'><li data-collapsed="true"><b>'+ this.messages[i].content["#class"]+'</b>' : '<ul data-role = "treeview" ><li data-collapsed="true"><b>'+this.messages[i].content+'</b>'
                res+=treeres
                res+="</li></ul>"
                res+= '</td>'


                // res+='<ul data-role = "treeview" ><li data-collapsed="true"><b>Message n°'+i+'</b>'


            }

            if(this.messages[i].content["#class"] === "idawi.RemoteException"){
                /*res += '<p style="text-align:justify"><b>' + i+ '</b></p>'
                var tree_res_errors = ""
                tree_res_errors = this.tree(tree_res_errors,this.messages[i])
                res+= tree_res_errors*/
                var tree_res_errors = ""
                tree_res_errors = this.tree(tree_res_errors,this.messages[i])
                res+= '<tr>'
                res+= '<td colspan="2"><b style="color: red">'+i+'</b></td>'
                res+='<td>'
                res+= this.messages[i].content["#class"]!== undefined ? '<ul data-role = "treeview" ><li data-collapsed="true"><b>'+ this.messages[i].content["#class"]+'</b>' : '<ul data-role = "treeview" ><li data-collapsed="true"><b>'+this.messages[i].content+'</b>'
                res+=tree_res_errors
                res+="</li></ul>"
                res+= '</td>'
            }


        }

        res+= '</table>'
        res += '</div>'

        /*let j = 1;
        for (let i = this.messages.length - 1; i >= 0; i--) {

            res += '<p style="text-align:justify"><b>' + j + '</b></p>'
            var treeres = ""
            treeres = this.tree(treeres, this.messages[i])
            res += treeres
            j++
        }
        res += '</div>'*/

        //affichage des erreurs
        /*res += '<div id="errorblock' + this.name + '" style="display:' + this.displayerror + '">'
        res += '<br><h3>Errors received</h3>'
        for (let i = this.errors.length - 1; i >= 0; i--) {
            res += '<p style="text-align:justify"><b> Error n°' + (i + 1) + '</b></p>'
            var treeres = ""
            treeres = this.tree(treeres, this.errors[i])
            res += treeres
        }
        res += '</div>'*/

        //si eot alors affiche message de fin de transmission
        //if (this.eot)
            /*res += '<div ><h3 >Transmission ended the ' + this.eotdate + '</h3>' +
                '<div></div>' +
                '</div>'*/
        res += '</div>'
        return res
    }


    createArborescenceJScon(tree_res,counter, html_building){
        tree_res = this.tree(tree_res,this.messages[counter])
        html_building+= '<tr>'
        html_building+= '<td colspan="2"><b>'+counter+'</b></td>'
        html_building+='<td>'
        html_building+= this.messages[counter].content["#class"]!== undefined ? '<ul data-role = "treeview" ><li data-collapsed="true"><b>'+ this.messages[counter].content["#class"]+'</b>' : '<ul data-role = "treeview" ><li data-collapsed="true"><b>'+this.messages[counter].content+'</b>'
        html_building+=tree_res
        html_building+="</li></ul>"
        html_building+= '</td>'


    }
    countNbOfEOTMsgs(){
        var nbOfEOTAndErrors = 0
        for(let i = 0; i < this.messages.length; i++){
        if(this.messages[i].content["#class"] === "idawi.EOT") {
            nbOfEOTAndErrors++
        }
        }
        return nbOfEOTAndErrors
    }

    countNbErrors(){
        var nbErrors = 0;
        for(let i = 0; i < this.messages.length; i++){
            if(this.messages[i].content["#class"] === "idawi.RemoteException") {
                nbErrors++
            }
        }
        return nbErrors
    }

    countNbActualMsgs(){
        var nbActualMsgs = 0;
        for(let i = 0; i < this.messages.length; i++){
            if(this.messages[i].content["#class"] !== "idawi.RemoteException" && this.messages[i].content["#class"] !== "idawi.EOT") {
                nbActualMsgs++
            }
        }
        return nbActualMsgs
    }

    /**
     * Fonction permettant de savoir si un message est déplié ou non
     */
    isDeplied(i, messages, res, component){
        b = document.body
        for(let i = 0; i < component.messages.length; i++){
        parentUl= document.getElementById("parent"+id);
        document.getElementById("parent" + i).addEventListener('click', function (e) {
            firstChildLi = parentUl.firstChild;
            if(component.messages[i].deplied == false ){
                tmp = document.createElement('li')
                if(component.messages[i].content["#class"]!== undefined){
                tmp.innerHTML = '<b>'+ messages[i].content["#class"]+'</b>'
                }else
                    tmp.innerHTML = '<b>'+ messages[i].content+'</b>'
                b.replace(tmp,firstChildLi)
                messages[i].deplied = true
            }else
                res+= messages[i].content["#class"]!== undefined ? '<ul data-role = "treeview" id="parent"'+i+'><li><b>'+ messages[i].content["#class"]+'</b>' : '<ul data-role = "treeview" ><li><b>'+messages[i].content+'</b>'
        });
        }
    }
}
