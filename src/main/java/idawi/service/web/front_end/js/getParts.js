const json_s = '{"signature": "json+gzip+base64"}';
const json_s2 = '{"signature": "xml+gzip+base64"}';
const json_s3 = '{"signature": "txt+gzip+base64"}';

    
function gzip(inputReadableStream) {
    return inputReadableStream.pipeThrough(
        new CompressionStream("gzip"),
      );
}

async function DecompressBlob(blob) {
    const ds = new DecompressionStream("gzip");
    const decompressedStream = blob.stream().pipeThrough(ds);
    return await new Response(decompressedStream).blob();
}

function json() {
    console.log("json");
}
function gzip() {
    console.log("gzip");
}

function xml() {
    console.log("xml");
}
function base64() {
    console.log("base64");
}

function txt() {
    console.log("txt");
}

function getSignaturePartsAndHanle(server_data) {
    const data = JSON.parse(server_data);
    const signature = data.signature;
    const parts = signature.split("+");
    for (let i = parts.length - 1; i >=0; i--) {
        eval(parts[i])();
    }
}

getSignaturePartsAndHanle(json_s);
getSignaturePartsAndHanle(json_s2);
getSignaturePartsAndHanle(json_s3);
console.log("----------------------");

const encodedData = btoa("Hello, world"); // encode a string
console.log(encodedData);

const decodedData = atob(encodedData); // decode the string
console.log(decodedData);