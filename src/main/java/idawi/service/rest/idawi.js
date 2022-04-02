/**
 * 
 */

function idawiObservable(url, observer) {
    var idawiListener = new EventSource(url, { withCredentials: true });
    if (idawiListener.readyState === EventSource.CLOSED) {
        // Traitement en cas de perte de connexion définitive avec le serveur /
        throw "connection failure"
    }
    idawiListener.onmessage = function (event) {
        var s = event.data;
        var a = s.split(/\r?\n/);
        var headerraw = a.shift();
        var data = a.join('');
        var header = JSON.parse(headerraw);
        if (header['semantic'] === 'EOT') {
            idawiListener.close()
        }
        observer.next(header, data)
    }
}

class IdawiObserver {
    //appelé quand l'observeur reçoit un {header,data} de l'observable
    next(header,data) {
        throw "must be implemented by extending class"
    }
}