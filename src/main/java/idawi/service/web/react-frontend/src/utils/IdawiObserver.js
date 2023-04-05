export default class IdawiObserver {
    //appelé quand l'observeur reçoit un {header,data} de l'observable
    next(header,data) {
        throw "must be implemented by extending class"
    }
    //appelé quand l'observable reçoit une erreur de connexion
    error(){
        throw "must be implemented by extending class"
    }
}