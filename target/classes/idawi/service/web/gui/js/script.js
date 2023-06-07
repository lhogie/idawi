

var listeneridawi
var idawilink = "http://localhost:8081/api/" // lien de l'api, à modifier pour prendre en compte le lien http actuel de la GUI 
window.onload = function () {
    var formobserver = new myFormObserver()
    formidawi = idawiObservable(idawilink, formobserver)

    //evennement liée bouton d'envoi requete vers idawi
    document.getElementById("requestform").onsubmit = function (e) {
        e.preventDefault()
        var input = document.getElementById("requestfield")
        var url = input.value
        var observer = new myIdawiObserver()
        if (listeneridawi != undefined)
            listeneridawi.close()
        listeneridawi = idawiObservable(url, observer)
    }

    //evennement liée au bouton d'envoi requete vers idawi
    document.getElementById("urlform").onsubmit = function (e) {
        var baselink = idawilink
        var components = ""
        var service = ""
        var operation = ""
        var params = ""

        e.preventDefault()
        var checkboxes = document.getElementsByClassName("urlcheckbox")
        for (let i = 0; i < checkboxes.length; i++) {
            if (checkboxes[i].checked)
                components += checkboxes[i].name + ","
        }
        if (components != "")
            components = components.slice(0, -1)

        service = document.getElementById("serviceoption").value
        operation = document.getElementById("operationoption").value
        params = document.getElementById("paramfield").value


        var finallink = baselink + components + "/" + service + "/" + operation
        if (params != "")
            finallink += "?" + params
        document.getElementById("requestfield").value = finallink

    }
}
