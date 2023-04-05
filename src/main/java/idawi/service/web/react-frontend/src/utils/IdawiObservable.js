export default function idawiObservable(url, observer) {
    var idawiListener = new EventSource(url, { withCredentials: true });

    idawiListener.onmessage = function (event) {
        //console.log(event.data)
        var s = event.data;
        var lines = s.split(/\r?\n/);
        //console.log(lines)
        var headerraw = lines.shift();
        //console.log(headerraw)

        var data = lines.join('');
        //console.log(data)
        var header = JSON.parse(headerraw);
        //console.log(header)
        if (header['semantic'] === 'EOT') {
            idawiListener.close()
        }
        observer.newMessage(header, data)
    }
    idawiListener.onerror = function (){
        observer.error()
        idawiListener.close()
    }
    return idawiListener;
}