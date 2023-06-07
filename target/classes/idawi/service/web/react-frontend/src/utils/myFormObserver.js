/**
 * Observeur pour la partie formulaire.
 * @author : Saad el din AHMED
 */
export default class myFormObserver extends IdawiObserver {
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